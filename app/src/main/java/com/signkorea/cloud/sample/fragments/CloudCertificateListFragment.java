package com.signkorea.cloud.sample.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.Observable;
import androidx.databinding.ObservableField;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.lumensoft.ks.KSException;
import com.signkorea.certmanager.BillActivity;
import com.signkorea.certmanager.BillParam;
import com.signkorea.cloud.Bio;
import com.signkorea.cloud.KSCertificateExt;
import com.yettiesoft.cloud.Client;
import com.yettiesoft.cloud.IncorrectPasscodeException;
import com.signkorea.cloud.sample.MainActivity;
import com.signkorea.cloud.sample.R;
import com.signkorea.cloud.sample.ViewModelFragment;
import com.signkorea.cloud.sample.databinding.AlertPasswordBinding;
import com.signkorea.cloud.sample.databinding.FragmentCloudCertificateListBinding;
import com.signkorea.cloud.sample.databinding.ItemCertificateBinding;
import com.signkorea.cloud.sample.enums.CertificateOperation;
import com.signkorea.cloud.sample.utils.OnceRunnable;
import com.signkorea.cloud.sample.viewModels.CloudCertificateListFragmentViewModel;
import com.yettiesoft.cloud.models.ExportedCertificate;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Hashtable;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

import lombok.val;

public class CloudCertificateListFragment extends ViewModelFragment<FragmentCloudCertificateListBinding, CloudCertificateListFragmentViewModel> {

    private String opp = null;
    private CertificateOperation operation = CertificateOperation.get;
    private boolean withBilling = false;

    private int signtype = 0;

    private final Adapter adapter = new Adapter();

    private int updatePosition = -1;
    private String updatePin = null;

    private String code = null;
    private String message = null;

    private final OnceRunnable loadData = new OnceRunnable(() -> {
        try {
            getViewModel().init(requireContext().getApplicationContext(), (Client.Delegate)requireActivity());
        } catch (Exception exception) {
            alertException(exception, operation.getLabel(), true);
            return;
        }

        CloudCertificateListFragmentViewModel.DataSource source;

        if (operation == CertificateOperation.register) {
            source = CloudCertificateListFragmentViewModel.DataSource.local;
        } else if (operation == CertificateOperation.cache) {
            source = CloudCertificateListFragmentViewModel.DataSource.cache;
        } else if (operation == CertificateOperation.all) {
            source = CloudCertificateListFragmentViewModel.DataSource.all;
        } else {
            source = CloudCertificateListFragmentViewModel.DataSource.remote;
        }

        Consumer<Exception> onError = exception -> alertException(exception, operation.getLabel(), true);

        @SuppressLint("NotifyDataSetChanged")
        Runnable completion = () -> {
            adapter.notifyDataSetChanged();

            if (adapter.getItemCount() == 0) {
                getBinding().nocertText.setVisibility(View.VISIBLE);
                getBinding().registcert.setVisibility(View.VISIBLE);
            } else
            {
                getBinding().nocertText.setVisibility(View.GONE);
                getBinding().registcert.setVisibility(View.GONE);
            }
        };

        Predicate<KSCertificateExt> certificateFilter;
        switch (operation) {
            case register:
            case delete: {
                certificateFilter = certInfo -> true;
            } break;

            case unlock: {
                certificateFilter = KSCertificateExt::isLock;
            } break;

            default: {
                certificateFilter = certInfo -> true;
            } break;
        }

        getViewModel().setDataSource(source).loadData(requireContext(), completion, onError, certificateFilter);
    });

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        operation = CloudCertificateListFragmentArgs.fromBundle(getArguments()).getOperation();

        withBilling = CloudCertificateListFragmentArgs.fromBundle(getArguments()).getBill();

        signtype = CloudCertificateListFragmentArgs.fromBundle(getArguments()).getSigntype();

        getBinding().recyclerView.setAdapter(adapter);

        NavDirections direction;

        // 등록
        direction = CloudCertificateListFragmentDirections
                .actionCloudCertificateListFragmentToCertificateListFragment()
                .setOperation(CertificateOperation.register);
        getBinding().registcert.setOnClickListener(
                Navigation.createNavigateOnClickListener(direction));
    }

    @Override
    public void onAuthenticationError(int i, CharSequence charSequence) {
        String message = null;
        if (i == KSException.FAILED_CLOUD_BIO_INVALID_PIN) {
            message = charSequence.toString();
        } else {
            message = i + " : " + charSequence;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("생체 인증 실패")
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                })
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData.run();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == BillActivity.ID) {
            // 수행을 제대로 한 경우
            if(resultCode == Activity.RESULT_OK && data != null)
            {
                Consumer<Hashtable<String, Object>> completion = (ret) -> {
                    requireActivity().runOnUiThread(() -> new AlertDialog.Builder(requireActivity())
                            .setTitle((String)ret.get("CODE"))
                            .setMessage((String)ret.get("MESSAGE"))
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> {})
                            .setNegativeButton(android.R.string.cancel, (dialog, which) -> {})
                            .show());
                };

                getViewModel().updateCertificate(updatePosition, withBilling, updatePin, completion);
            }
            // 수행을 제대로 하지 못한 경우
            else if(resultCode == Activity.RESULT_CANCELED)
            {
                String reason = "빌링취소 : ";
                if(data != null)
                    reason += data.getStringExtra(BillActivity.REASON);

                String finalReason = reason;
                requireActivity().runOnUiThread(() -> new AlertDialog.Builder(requireActivity())
                        .setTitle(finalReason)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {})
                        .setNegativeButton(android.R.string.cancel, (dialog, which) -> {})
                        .show());
            }
        }
    }

    private void onItemClick(int position) {
        switch (operation) {
            case register: {
                registerCertificate(position);
            } break;

            case get:
            case cache:
            case all: {
                getCertificateIndex(position);
            } break;

            case export: {
                exportCertificate(position);
            } break;

            case changePin: {
                changeCertificatePin(position);
            } break;

            case delete: {
                deleteCertificate(position);
            } break;

            case update: {
                updateCertificate(position);
            } break;

            case revoke: {
                revokeCertificate(position);
            } break;

            case unlock: {
                unlockCertificate(position);
            } break;

            default: {
                assert false;
            } break;
        }
    }

    private void acquirePassword(boolean pinMode, boolean confirmPassword, String initialPassword, Consumer<String> completion) {
        ObservableField<String> pwd1 = new ObservableField<String>();
        ObservableField<String> pwd2 = new ObservableField<String>();

        AlertPasswordBinding binding =
                AlertPasswordBinding.inflate(LayoutInflater.from(requireContext()));

        binding.setPassword1(pwd1);
        binding.setPassword2(pwd2);
        binding.setConfirmPassword(confirmPassword);
        binding.setPinMode(pinMode);

        AlertDialog alert = new AlertDialog.Builder(requireContext())
                .setTitle(operation.getLabel())
                .setView(binding.getRoot())
                .setPositiveButton(android.R.string.ok, (dialog, which) -> completion.accept(pwd1.get()))
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> {})
                .create();

        if (confirmPassword) {
            Observable.OnPropertyChangedCallback onPwdChanged = new Observable.OnPropertyChangedCallback() {
                @Override
                public void onPropertyChanged(Observable sender, int propertyId) {
                    String pwd = pwd1.get();
                    boolean ok = pwd != null && pwd.length() > 0 && pwd.equals(pwd2.get());
                    alert.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(ok);
                }
            };
            pwd1.addOnPropertyChangedCallback(onPwdChanged);
            pwd2.addOnPropertyChangedCallback(onPwdChanged);
        } else {
            Observable.OnPropertyChangedCallback onPwdChanged = new Observable.OnPropertyChangedCallback() {
                @Override
                public void onPropertyChanged(Observable sender, int propertyId) {
                    String pwd = pwd1.get();
                    boolean ok = pwd != null && pwd.length() > 0;
                    alert.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(ok);
                }
            };
            pwd1.addOnPropertyChangedCallback(onPwdChanged);
        }

        alert.show();

        pwd1.set(initialPassword);
        pwd2.set(initialPassword);
    }

    public void registerCertificate(int position) {
        Consumer<KSCertificateExt> completion = cert -> new AlertDialog.Builder(requireContext())
                .setTitle(operation.getLabel() + " 성공")
                .setMessage(cert.getSubject())
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {})
                .show();

        Consumer<Exception> onError = exception -> {
            final String errorMessage;

            if (exception instanceof IncorrectPasscodeException) {
                new AlertDialog.Builder(requireContext())
                        .setTitle(operation.getLabel())
                        .setMessage("인증서 비밀번호가 일치하지 않습니다.")
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {})
                        .show();
            } else {
                alertException(exception, operation.getLabel());
            }
        };

        BiConsumer<String, String> onPasswordAcquired = (password, pin) -> {
            val cert = (KSCertificateExt)getViewModel().getCertificates().get(position);

            getViewModel().registerCertificate(
                    cert.getCertificate(),
                    cert.getKey(),
                    cert.getKmCertificate(),
                    cert.getKmKey(),
                    password,
                    pin,
                    () -> completion.accept(cert),
                    onError);
        };

        acquirePassword(false, false, "", password ->
                acquirePassword(true, true, "", pin ->
                        onPasswordAcquired.accept(password, pin)));
    }

    @Override
    protected void alertException(@NonNull Exception exception) {
        super.alertException(exception, operation.getLabel());
    }

    private void getCertificateIndex(int position) {

        HomeFragment.getInstance().setindex(position);

        NavDirections directions = CloudCertificateListFragmentDirections.actionCloudCertificateListFragmentToLoginFragment()
                .setCertificateIndex(position)
                .setOperation(operation)
                .setSigntype(signtype);
        Navigation.findNavController(getView()).navigate(directions);
    }

    private void exportCertificate(int position) {
        BiConsumer<String, String> onPasswordAcquired = (pin, secret) -> {
            BiConsumer<ExportedCertificate, Boolean> completion = (certificate, fromCache) -> {
                val title = operation.getLabel() + " 성공 ";
                val source = fromCache ? "[Cached]" : "[Downloaded]";

                new AlertDialog.Builder(requireContext())
                        .setTitle(title + source)
                        .setMessage(certificate.getId())
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {})
                        .show();
            };

            getViewModel().exportCertificate(position, pin, secret, completion, this::alertException);
        };

        acquirePassword(true, false, "", pin ->
                acquirePassword(false, true, "", secret ->
                        onPasswordAcquired.accept(pin, secret)));
    }

    private void changeCertificatePin(int position) {
        BiConsumer<String, String> onPinAcquired = (oldPin, newPin) -> {
            Runnable completion = () -> new AlertDialog.Builder(requireContext())
                    .setTitle(operation.getLabel() + " 성공")
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {})
                    .show();

            getViewModel().changeCertificatePin(position, oldPin, newPin, completion, this::alertException);
        };

        acquirePassword(true, false, "", oldPin ->
                acquirePassword(true, true, "", newPin ->
                        onPinAcquired.accept(oldPin, newPin)));
    }

    private void deleteCertificate(int position) {
        Runnable completion = () -> adapter.notifyItemRemoved(position);

        Consumer<Exception> onError = exception -> new AlertDialog.Builder(requireContext())
                .setTitle(operation.getLabel() + " 실패")
                .setMessage(exception.toString())
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {})
                .show();

        val cert = getViewModel().getCertificates().get(position);

        new AlertDialog.Builder(requireActivity())
                .setMessage(String.format("'%s' 인증서를 삭제합니다.", cert.getSubject()))
                .setPositiveButton(android.R.string.ok, (dialog, which) ->
                        getViewModel().deleteCertificate(position, completion, onError)
                )
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> {})
                .show();
    }

    private void updateCertificate(int position) {
        Consumer<Hashtable<String, Object>> completion = (ret) -> {
            if (((String)ret.get("CODE")).equalsIgnoreCase("-4100") &&
                    ((String)ret.get("MESSAGE")).startsWith("SKM_CA_0002")) {

                // cloud 용 opp code 값 setting
                if(((String)ret.get("MESSAGE")).contains("SK_MC")) {
                    opp = "SK_MC";
                }

                requireActivity().runOnUiThread(() -> new AlertDialog.Builder(requireActivity())
                        .setMessage("CODE : " + "-4100" + "\n" + "MSG : 공동인증서(구 공인인증서) 비용을 납부하셔야 합니다. 진행하시겠습니까?")
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            KSCertificateExt cert = getViewModel().getCertificates().get(position);
                            showBillingActivity(cert.getSerialInt());
                        })
                        .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                            dialog.cancel();// Action for 'NO' Button
                            // 인증서 발급 취소됨. 어디로 갈건지 고객사에서 설정하세요.
                        })
                        .show());
            } else {
                code = (String) ret.get("CODE");

                if (code.equalsIgnoreCase("NL709")) {
                    message = "클라우드에 없는 인증서입니다.";
                } else if (code.equalsIgnoreCase("NL711")) {
                    message = "pin 번호가 틀렸습니다.";
                } else if (code.equalsIgnoreCase("NL715")) {
                    message = "pin 번호 5회 오류로 인증서가 잠겼습니다.";
                } else if (code.equalsIgnoreCase("NL716") || code.equalsIgnoreCase("NL717")) {
                    message = "클라우드 인증 서비스에 오류가 발생했습니다.";
                } else{
                    message = (String) ret.get("MESSAGE");
                }

                requireActivity().runOnUiThread(() -> new AlertDialog.Builder(requireActivity())
                        .setTitle(code)
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        })
                        .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                        })
                        .show());
            }
        };

        Consumer<String> onPasswordAcquired = (pin) -> {
            updatePosition = position;
            updatePin = pin;
            getViewModel().updateCertificate(position, withBilling, pin, completion);
        };

        acquirePassword(true, false, "", pin ->
                onPasswordAcquired.accept(pin));
    }

    private void revokeCertificate(int position) {
        Consumer<Hashtable<String, Object>> completion = (ret) -> {
            new AlertDialog.Builder(requireActivity())
                    .setTitle((String)ret.get("CODE"))
                    .setMessage((String)ret.get("MESSAGE"))
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {})
                    .setNegativeButton(android.R.string.cancel, (dialog, which) -> {})
                    .show();
        };

        getViewModel().revokeCertificate(position, completion);
    }

    private void unlockCertificate(int position) {
        Runnable completion = () -> new AlertDialog.Builder(requireContext())
                .setMessage("인증서 잠금을 해제 하였습니다.")
                .setPositiveButton(android.R.string.ok, ((dialog, which) -> {
                    adapter.notifyItemRemoved(position);
                }))
                .show();

        getViewModel().unlockCertificate(position, completion, this::alertException);
    }



    private void showBillingActivity(String serial) {
        Intent intent = new Intent(this.getContext(), BillActivity.class);
        intent.putExtra(BillActivity.IS_MAIN_SERVER, false);
        intent.putExtra(BillActivity.OPERATION, BillActivity.UPDATE);
        //intent.putExtra(BillActivity.SERIAL, serial);
        // billing 시 mSerial을 가공 후 전달해 주는 로직으로 변경
        intent.putExtra(BillActivity.SERIAL, BillParam.makeBillParam(serial));
        // opp code값이 result message를 통해 넘어온 경우에 처리되어야 함
        if (opp != null) intent.putExtra(BillActivity.OPP, opp);
        startActivityForResult(intent, BillActivity.ID);
    }

    class Adapter extends RecyclerView.Adapter<ItemView> {
        @NonNull
        @Override
        public ItemView onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            CloudCertificateListFragment.ItemView itemView = ItemView.create(parent);


            itemView.binding.getRoot().setOnClickListener(view -> {
                int pos = itemView.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    onItemClick(pos);
                }
            });

            return itemView;
        }

        @Override
        public void onBindViewHolder(@NonNull CloudCertificateListFragment.ItemView holder, int position) {
            holder.bind(getViewModel().getCertificates().get(position));
        }

        @Override
        public int getItemCount() {
            return getViewModel().getCertificates().size();
        }
    }

    static class ItemView extends RecyclerView.ViewHolder {
        private final ItemCertificateBinding binding;

        public ItemView(@NonNull ItemCertificateBinding binding) {
            super(binding.getRoot());

            this.binding = binding;
        }

        public void bind(@NonNull KSCertificateExt cert) {
            LocalDate dt = cert.getNotAfter()
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();

            String notAfter = DateTimeFormatter
                    .ofLocalizedDate(FormatStyle.FULL)
                    .format(dt);

            binding.setSubject(cert.getSubject());
            binding.setNotAfter(notAfter);
            binding.setSerial(cert.getSerial());
            StringBuffer sb = new StringBuffer();
            if (cert.isCloud()) {
                sb.append("Cloud-");
                if (cert.isCache()) sb.append("Cache");
                else sb.append("Server");

                if (cert.isBio()) sb.append("-Bio");
            } else sb.append("Local");

            binding.setType(sb.toString());
            binding.setLocked(cert.isLock());
        }

        public static CloudCertificateListFragment.ItemView create(ViewGroup parent) {
            ItemCertificateBinding binding = ItemCertificateBinding.inflate(
                    LayoutInflater.from(parent.getContext()),
                    parent,
                    false);

            return new CloudCertificateListFragment.ItemView(binding);
        }
    }
}