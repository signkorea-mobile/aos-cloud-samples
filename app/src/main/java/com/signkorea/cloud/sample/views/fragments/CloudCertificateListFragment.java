package com.signkorea.cloud.sample.views.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.signkorea.certmanager.BillActivity;
import com.signkorea.certmanager.BillParam;
import com.signkorea.cloud.KSCertificateExt;
import com.signkorea.cloud.sample.databinding.FragmentCloudCertificateListBinding;
import com.signkorea.cloud.sample.databinding.ItemCertificateBinding;
import com.signkorea.cloud.sample.enums.CertificateOperation;
import com.signkorea.cloud.sample.enums.SignMenuType;
import com.signkorea.cloud.sample.utils.OnceRunnable;
import com.signkorea.cloud.sample.viewModels.CertificateListFragmentViewModel;
import com.signkorea.cloud.sample.viewModels.InterFragmentStore;
import com.signkorea.cloud.sample.views.base.ViewModelFragment;
import com.yettiesoft.cloud.Client;
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

public class CloudCertificateListFragment extends ViewModelFragment<FragmentCloudCertificateListBinding, CertificateListFragmentViewModel> {

    private String opp = null;
    private CertificateOperation operation = CertificateOperation.get;

    private SignMenuType menuType;

    private final Adapter adapter = new Adapter();

    private int updatePosition = -1;
    private String updatePin = null;

    private String code = null;
    private String message = null;

    private final OnceRunnable loadData = new OnceRunnable(() -> {
        try {
            getViewModel().init(requireContext().getApplicationContext(),
                    (Client.Delegate)requireActivity(),
                    this);
        } catch (Exception exception) {
            alertException(exception, operation.getLabel(), true);
            return;
        }

        Consumer<Exception> onError = exception -> alertException(exception, operation.getLabel(), true);

        @SuppressLint("NotifyDataSetChanged")
        Runnable completion = () -> {
            dismissLoading();
            adapter.notifyDataSetChanged();

            if (adapter.getItemCount() == 0) {
                getBinding().noCertText.setVisibility(View.VISIBLE);
                getBinding().registCert.setVisibility(View.VISIBLE);
            } else
            {
                getBinding().noCertText.setVisibility(View.GONE);
                getBinding().registCert.setVisibility(View.GONE);
            }
        };

        Predicate<KSCertificateExt> certificateFilter;
        if(operation == CertificateOperation.unlock)
            certificateFilter = KSCertificateExt::isLock;
        else
            certificateFilter = null;

        showLoading();
        getViewModel().setDataSource(CertificateListFragmentViewModel.DataSource.remote)
                .loadData(completion, onError, certificateFilter);
    });

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        operation = CloudCertificateListFragmentArgs.fromBundle(getArguments()).getOperation();
        menuType = CloudCertificateListFragmentArgs.fromBundle(getArguments()).getSignMenuType();

        getBinding().recyclerView.setAdapter(adapter);

        NavDirections direction;

        // 등록 (클라우드 내 인증서가 없는 경우)
        direction = CloudCertificateListFragmentDirections
                .actionCloudCertificateListFragmentToLocalCertificateListFragment()
                .setOperation(CertificateOperation.register);
        getBinding().registCert.setOnClickListener(
                Navigation.createNavigateOnClickListener(direction));
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData.run();
    }

    // 클라우드 내 인증서 update() 처리 중 BillActivity 화면 전환 후 결과 처리
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

    private void onItemClick(int position) {
        switch (operation) {
            case get:
                getCertificateIndex(position);
                break;

            case export:
                exportCertificate(position);
                break;

            case changePin:
                changeCertificatePin(position);
                break;

            case delete:
                deleteCertificate(position);
                break;

            case updateCloud:
                updateCertificate(position);
                break;

            case unlock:
                unlockCertificate(position);
                break;

            default:
                assert false : "정의되지 않은 동작: " + operation.name();
                break;
        }
    }

    @Override
    protected void alertException(@NonNull Exception exception) {
        super.alertException(exception, operation.getLabel());
    }

    private void getCertificateIndex(int position) {
        NavDirections directions = CloudCertificateListFragmentDirections.actionCloudCertificateListFragmentToLoginFragment()
                .setCertificateIndex(position)
                .setOperation(operation)
                .setSignMenuType(menuType);
        getNavController().navigate(directions);
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

        acquirePassword(requireContext(),
                operation.getLabel(),
                true,
                false,
                "",
                pin -> acquirePassword(requireContext(),
                        operation.getLabel(),
                        false,
                        true,
                        "",
                        secret -> onPasswordAcquired.accept(pin, secret)));
    }

    private void changeCertificatePin(int position) {
        BiConsumer<String, String> onPinAcquired = (oldPin, newPin) -> {
            Runnable completion = () -> new AlertDialog.Builder(requireContext())
                    .setTitle(operation.getLabel() + " 성공")
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {})
                    .show();

            getViewModel().changeCertificatePin(position, oldPin, newPin, completion, this::alertException);
        };

        acquirePassword(requireContext(),
                operation.getLabel(),
                true,
                false,
                "",
                oldPin -> acquirePassword(requireContext(),
                        operation.getLabel(),
                        true,
                        true,
                        "",
                        newPin -> onPinAcquired.accept(oldPin, newPin)));
    }

    private void deleteCertificate(int position) {
        Runnable completion = () -> {
            Toast.makeText(requireContext(), "인증서를 삭제하였습니다.", Toast.LENGTH_SHORT).show();
            adapter.notifyItemRemoved(position);
        };

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
            dismissLoading();

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
                        if(code.equalsIgnoreCase("CMP201")) {   // CMP201: 인증서 갱신 성공
                            // 인증서 목록 갱신
                            showLoading();
                            Consumer<Exception> onLoadingError = exception -> alertException(exception, operation.getLabel(), true);

                            Runnable onLoadingComplete = () -> {
                                dismissLoading();
                                adapter.notifyDataSetChanged();
                            };
                            getViewModel().setDataSource(CertificateListFragmentViewModel.DataSource.remote)
                                    .loadData(onLoadingComplete, onLoadingError, null);
                        }
                    })
                    .show());
        };

        Consumer<String> onPasswordAcquired = (pin) -> {
            updatePosition = position;
            updatePin = pin;
            getViewModel().updateCertificate(position, pin, completion);
        };

        acquirePassword(requireContext(),
                operation.getLabel(),
                true,
                false,
                "",
                pin -> onPasswordAcquired.accept(pin));
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
        intent.putExtra(BillActivity.IS_MAIN_SERVER, false); // 메인 서버인 경우에는  true로 설정
        intent.putExtra(BillActivity.OPERATION, BillActivity.UPDATE);
        intent.putExtra(BillActivity.SERIAL, BillParam.makeBillParam(serial));
        if (opp != null)
            intent.putExtra(BillActivity.OPP, opp);
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

        public static CloudCertificateListFragment.ItemView create(ViewGroup parent) {
            ItemCertificateBinding binding = ItemCertificateBinding.inflate(
                    LayoutInflater.from(parent.getContext()),
                    parent,
                    false);

            return new CloudCertificateListFragment.ItemView(binding);
        }
    }
}