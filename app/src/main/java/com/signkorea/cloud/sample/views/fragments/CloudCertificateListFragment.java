package com.signkorea.cloud.sample.views.fragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
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

import com.signkorea.cloud.KSCertificateExt;
import com.signkorea.cloud.sample.databinding.FragmentCloudCertificateListBinding;
import com.signkorea.cloud.sample.databinding.ItemCertificateBinding;
import com.signkorea.cloud.sample.enums.CertificateOperation;
import com.signkorea.cloud.sample.enums.SignMenuType;
import com.signkorea.cloud.sample.models.CloudRepository;
import com.signkorea.cloud.sample.utils.PasswordDialog;
import com.signkorea.cloud.sample.viewModels.CertificateListFragmentViewModel;
import com.signkorea.cloud.sample.views.base.ViewModelFragment;
import com.yettiesoft.cloud.models.ExportedCertificate;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Hashtable;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import lombok.val;

public class CloudCertificateListFragment extends ViewModelFragment<FragmentCloudCertificateListBinding, CertificateListFragmentViewModel> {
    private CertificateOperation operation = CertificateOperation.get;

    private SignMenuType menuType;

    private final Adapter adapter = new Adapter();

    private String code = null;
    private String message = null;

    private void refresh() {
        Consumer<Exception> onError = exception -> alertException(exception, operation.getLabel(), true);

        @SuppressLint("NotifyDataSetChanged")
        Runnable completion = () -> {
            dismissLoading();
            adapter.notifyDataSetChanged();

            if (adapter.getItemCount() == 0) {
                getBinding().noCertText.setVisibility(View.VISIBLE);
                getBinding().registCert.setVisibility(View.VISIBLE);
                if(operation == CertificateOperation.unlock) {
                    getBinding().noCertText.setText("잠긴 인증서가 없습니다.");
                    getBinding().registCert.setText("뒤로가기");
                }
            } else
            {
                getBinding().noCertText.setVisibility(View.GONE);
                getBinding().registCert.setVisibility(View.GONE);
            }
        };

        showLoading();
        if(operation == CertificateOperation.unlock) {
            getViewModel().loadData(CloudRepository.DataSource.remote,
                    KSCertificateExt::isLock,
                    true,
                    completion,
                    onError);
        }
        else {
            getViewModel().loadData(CloudRepository.DataSource.remote, null, completion, onError);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        operation = CloudCertificateListFragmentArgs.fromBundle(getArguments()).getOperation();
        menuType = CloudCertificateListFragmentArgs.fromBundle(getArguments()).getSignMenuType();

        getBinding().recyclerView.setAdapter(adapter);

        NavDirections direction;

        // 등록 (클라우드 내 인증서가 없는 경우)
        if(operation == CertificateOperation.unlock)
            getBinding().registCert.setOnClickListener((View.OnClickListener) v -> {
                getNavController().popBackStack();
            });
        else {
            direction = CloudCertificateListFragmentDirections
                    .actionCloudCertificateListFragmentToLocalCertificateListFragment()
                    .setOperation(CertificateOperation.register);
            getBinding().registCert.setOnClickListener(
                    Navigation.createNavigateOnClickListener(direction));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
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
        // 선택된 인증서 정보 저장
        CloudRepository.getInstance().selectCert(position);

        NavDirections directions = CloudCertificateListFragmentDirections.actionCloudCertificateListFragmentToLoginFragment()
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

        PasswordDialog.show(requireContext(),
                operation.getLabel(),
                true,
                false,
                "",
                pin -> PasswordDialog.show(requireContext(),
                        operation.getLabel(),
                        false,
                        true,
                        "",
                        secret -> onPasswordAcquired.accept(pin, secret),
                        this::dismissLoading),
                this::dismissLoading);
    }

    private void changeCertificatePin(int position) {
        BiConsumer<String, String> onPinAcquired = (oldPin, newPin) -> {
            Runnable completion = () -> new AlertDialog.Builder(requireContext())
                    .setTitle(operation.getLabel() + " 성공")
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {})
                    .show();

            getViewModel().changeCertificatePin(position, oldPin, newPin, completion, this::alertException);
        };

        PasswordDialog.show(requireContext(),
                operation.getLabel(),
                true,
                false,
                "",
                oldPin -> PasswordDialog.show(requireContext(),
                        operation.getLabel(),
                        true,
                        true,
                        "",
                        newPin -> onPinAcquired.accept(oldPin, newPin),
                        this::dismissLoading),
                this::dismissLoading);
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

                            getViewModel().loadData(CloudRepository.DataSource.remote, null, true, onLoadingComplete, onLoadingError);
                        }
                    })
                    .show());
        };

        Consumer<String> onPasswordAcquired = (pin) -> {
            getViewModel().updateCertificate(position, pin, completion);
        };

        PasswordDialog.show(requireContext(),
                operation.getLabel(),
                true,
                false,
                "",
                pin -> onPasswordAcquired.accept(pin),
                this::dismissLoading);
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

        public static CloudCertificateListFragment.ItemView create(ViewGroup parent) {
            ItemCertificateBinding binding = ItemCertificateBinding.inflate(
                    LayoutInflater.from(parent.getContext()),
                    parent,
                    false);

            return new CloudCertificateListFragment.ItemView(binding);
        }
    }
}