package com.signkorea.cloud.sample.views.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.lumensoft.ks.KSException;
import com.signkorea.certmanager.BillActivity;
import com.signkorea.cloud.Bio;
import com.signkorea.cloud.KSCertificateExt;
import com.signkorea.cloud.sample.databinding.FragmentLocalCertificateListBinding;
import com.signkorea.cloud.sample.databinding.ItemCertificateBinding;
import com.signkorea.cloud.sample.enums.CertificateOperation;
import com.signkorea.cloud.sample.enums.DataSource;
import com.signkorea.cloud.sample.utils.PasswordDialog;
import com.signkorea.cloud.sample.viewModels.CertificateListFragmentViewModel;
import com.signkorea.cloud.sample.viewModels.InterFragmentStore;
import com.signkorea.cloud.sample.views.base.ViewModelFragment;
import com.signkorea.securedata.ProtectedData;
import com.signkorea.securedata.SecureData;
import com.yettiesoft.cloud.IncorrectPasscodeException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Hashtable;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class LocalCertificateListFragment
    extends ViewModelFragment<FragmentLocalCertificateListBinding, CertificateListFragmentViewModel> implements Bio.Callback
{
    private CertificateOperation operation = CertificateOperation.get;

    private final Adapter adapter = new Adapter();

    private String code = null;
    private String message = null;

    private void refresh() {
        Consumer<Exception> onError = exception -> alertException(exception, operation.getLabel(), true);

        @SuppressLint("NotifyDataSetChanged")
        Runnable completion = () -> {
            dismissLoading();
            adapter.notifyDataSetChanged();

            if(adapter.getItemCount() == 0)
            {
                switch(operation) {
                    case register:
                        new AlertDialog.Builder(requireContext())
                                .setTitle(operation.getLabel())
                                .setMessage("로컬 저장소에 내 인증서가 없습니다.")
                                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                                    getNavController().popBackStack();
                                })
                                .show();
                        break;

                    case updateLocal:
                        Toast.makeText(requireContext(), "로컬 저장소에 인증서가 없습니다.", Toast.LENGTH_SHORT).show();
                        getNavController().popBackStack();
                        break;

                    default:
                        assert false: "정의되지 않은 동작: " + operation.name();
                        break;
                }
            }
        };

        // MO에서 복귀한 경우 중복 호출 방지
        // MO에서 복귀한 경우가 아닐 때만 화면/데이터 갱신
        if(getMoReturnDestinationViewId() < 0) {
            showLoading();
            getViewModel().loadData(DataSource.local, null, completion, onError);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        operation = LocalCertificateListFragmentArgs.fromBundle(getArguments()).getOperation();

        getBinding().recyclerView.setAdapter(adapter);
        
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if(operation == CertificateOperation.register) {

                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    private void onItemClick(int position) {
        switch (operation) {
            case register:
                registerCertificate(position);
                break;

            case updateLocal:
                updateCertificate(position);
                break;

            default:
                assert false : "정의되지 않은 동작: " + operation.name();
                break;
        }
    }

    private void registerCertificate(int position) {
        Consumer<KSCertificateExt> completion = cert -> {
            dismissLoading();
            final String dn = cert.getSubject();

            new AlertDialog.Builder(requireContext())
                    .setTitle(operation.getLabel() + " 성공")
                    .setMessage("인증서가 클라우드에 저장 되었습니다." + "\n생체 인증도 함께 사용 하시겠습니까?")
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        try {
                            Toast.makeText(requireContext(), "생체 등록을 진행합니다.", Toast.LENGTH_SHORT).show();

                            PasswordDialog.show(requireContext(),
                                    operation.getLabel() + " - 생체 등록",
                                    true,
                                    false,
                                    "",
                                    pin -> {
                                        String certId = getViewModel().getCertIdFromSubjectDn(dn);
                                        if(!TextUtils.isEmpty(certId))
                                            getViewModel().registerBio(
                                                    requireActivity(),
                                                    certId,
                                                    new SecureData(pin.getBytes()),
                                                    this);
                                        else {
                                            Toast.makeText(requireContext(),
                                                    "클라우드에서 인증서를 찾을 수 없습니다. 인증서를 클라우드에 다시 보관해 주세요.",
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    },
                                    this::dismissLoading);
                        }
                        catch (Exception e)
                        {
                            new AlertDialog.Builder(requireContext())
                                    .setTitle("생체인증 등록 실패")
                                    .setMessage(e.toString())
                                    .setPositiveButton(android.R.string.ok, null)
                                    .show();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        };

        Consumer<Exception> onError = exception -> {
            dismissLoading();
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
            KSCertificateExt cert = getViewModel().getCertificates().get(position);

            showLoading();
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

        PasswordDialog.show(requireContext(),
                operation.getLabel(),
                false,
                false,
                "",
                password -> PasswordDialog.show(requireContext(),
                                                operation.getLabel(),
                                                true,
                                                true,
                                                "",
                                                pin -> onPasswordAcquired.accept(password, pin),
                                                this::dismissLoading),
                this::dismissLoading);
    }

    @Override
    protected void alertException(@NonNull Exception exception) {
        super.alertException(exception, operation.getLabel());
    }

    private void updateCertificate(int position) {
        Consumer<Hashtable<String, Object>> completion = (ret) -> {
            dismissLoading();

            code = (String) ret.get("CODE");
            if (code.equalsIgnoreCase("NL716") || code.equalsIgnoreCase("NL717"))
                message = "클라우드 인증 서비스에 오류가 발생했습니다.";
            else
                message = (String) ret.get("MESSAGE");

            requireActivity().runOnUiThread(() -> new AlertDialog.Builder(requireActivity())
                .setTitle(code)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    if(code.equalsIgnoreCase("CMP201")) {   // CMP201: 인증서 갱신 성공
                        // 인증서 목록 갱신
                        showLoading();
                        Consumer<Exception> onLoadingError = exception -> alertException(exception, operation.getLabel(), true);

                        Runnable onLoadingComplete = () -> {
                            dismissLoading();
                            adapter.notifyDataSetChanged();
                        };

                        getViewModel().loadData(
                                DataSource.local,
                                null,
                                onLoadingComplete,
                                onLoadingError);
                    }
                })
                .show());
        };

        Consumer<String> onPasswordAcquired = (pin) -> {
            getViewModel().updateCertificate(position, pin, completion);
        };

        showLoading();
        PasswordDialog.show(requireContext(),
                operation.getLabel(),
                false,
                false,
                "",
                pwd -> onPasswordAcquired.accept(pwd),
                this::dismissLoading);
    }

    class Adapter extends RecyclerView.Adapter<ItemView> {
        @NonNull
        @Override
        public ItemView onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LocalCertificateListFragment.ItemView itemView = ItemView.create(parent);

            itemView.binding.getRoot().setOnClickListener(view -> {
                int pos = itemView.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    onItemClick(pos);
                }
            });

            return itemView;
        }

        @Override
        public void onBindViewHolder(@NonNull ItemView holder, int position) {
            holder.bind(getViewModel().getCertificates().get(position));
        }

        @Override
        public int getItemCount() {
            return getViewModel().getCertificates() == null ? 0 : getViewModel().getCertificates().size();
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
            binding.setSerial(cert.getSerialInt());
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

        public static ItemView create(ViewGroup parent) {
            ItemCertificateBinding binding = ItemCertificateBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false);

            return new ItemView(binding);
        }
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
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    @Override
    public void onAuthenticationFailed() {
        new AlertDialog.Builder(requireContext())
                .setTitle("생체 인증 실패")
                .setMessage("onAuthenticationFailed")
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    @Override
    public void onAuthenticationSucceeded(Bio.OPERATION operation, int i, ProtectedData protectedData) {
        if (operation == Bio.OPERATION.REGIST) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("생체 인증 등록 성공")
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        } else {
            // 로컬 인증서 화면에서는 등록 처리만 구현되어 있습니다.
            assert false : "정의되지 않은 생체 인증 operation: " + operation.name();
        }
    }

    // 로컬 인증서 updateLocal() 처리 중 BillActivity 화면 전환 후 결과 처리
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == BillActivity.ID) {
            String useAction;
            String discardAction;
            if(resultCode == Activity.RESULT_OK){
                useAction = InterFragmentStore.BILL_ACTION_COMPLETE;
                discardAction = InterFragmentStore.BILL_ACTION_CANCEL;
            }
            else {
                useAction = InterFragmentStore.BILL_ACTION_CANCEL;
                discardAction = InterFragmentStore.BILL_ACTION_COMPLETE;
            }
            getInterFragmentStore().<Runnable>remove(useAction).run();
            getInterFragmentStore().remove(discardAction);
        }
        else
            super.onActivityResult(requestCode, resultCode, data);
    }
}
