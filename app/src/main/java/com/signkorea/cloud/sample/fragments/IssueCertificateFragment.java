package com.signkorea.cloud.sample.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import com.lumensoft.ks.KSException;
import com.signkorea.certmanager.BillActivity;
import com.signkorea.certmanager.BillParam;
import com.yettiesoft.cloud.Client;
import com.yettiesoft.cloud.InvalidLicenseException;
import com.yettiesoft.cloud.InvalidPinException;
import com.signkorea.cloud.sample.ViewModelFragment;
import com.signkorea.cloud.sample.databinding.AlertPasswordBinding;
import com.signkorea.cloud.sample.databinding.FragmentIssueCertificateBinding;
import com.signkorea.cloud.sample.enums.CertificateOperation;
import com.signkorea.cloud.sample.viewModels.IssueCertificateFragmentViewModel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.Observable;
import androidx.databinding.ObservableField;

import java.util.Hashtable;
import java.util.function.Consumer;

public class IssueCertificateFragment extends ViewModelFragment<FragmentIssueCertificateBinding, IssueCertificateFragmentViewModel> {

    private String opp = null;
    private CertificateOperation operation = CertificateOperation.issue;
    private boolean withBilling = false;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        withBilling = CertificateListFragmentArgs.fromBundle(getArguments()).getBill();

        try {
            getViewModel()
                .init(requireContext().getApplicationContext())
                .setClientDelegate((Client.Delegate) requireActivity());
        } catch (InvalidLicenseException e) {
            e.printStackTrace();
        }

        getBinding().issue.setOnClickListener(view1 -> issue());
        getBinding().savePhone.setOnClickListener(view1 -> savePhone());
        getBinding().saveCloud.setOnClickListener(view1 -> saveCloud());
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
                getViewModel().issue(completion, withBilling);
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

    private void issue() {
        Consumer<Hashtable<String, Object>> completion = (ret) -> {
            if (((String)ret.get("CODE")).equalsIgnoreCase("-4100") &&
                ((String)ret.get("MESSAGE")).startsWith("SKM_CA_0001")) {

                // cloud 용 opp code 값 setting
                if(((String)ret.get("MESSAGE")).contains("SK_MC")) {
                    opp = "SK_MC";
                }

                requireActivity().runOnUiThread(() -> new AlertDialog.Builder(requireActivity())
                    .setMessage("CODE : " + "-4100" + "\n" + "MSG : 공동인증서(구 공인인증서) 비용을 납부하셔야 합니다. 진행하시겠습니까?")
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        showBillingActivity(getViewModel().refNum.get());
                    })
                    .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                        dialog.cancel();// Action for 'NO' Button
                        // 인증서 발급 취소됨. 어디로 갈건지 고객사에서 설정하세요.
                    })
                    .show());
            } else {
                requireActivity().runOnUiThread(() -> new AlertDialog.Builder(requireActivity())
                    .setTitle((String)ret.get("CODE"))
                    .setMessage((String)ret.get("MESSAGE") + " - " + getViewModel().getIssuedCertDN())
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {})
                    .setNegativeButton(android.R.string.cancel, (dialog, which) -> {})
                    .show());
            }
        };

        getViewModel().issue(completion, withBilling);

    }

    private void savePhone () {
        boolean ret = getViewModel().savePhone();
        new AlertDialog.Builder(requireContext())
            .setTitle(ret ? "휴대폰 저장 성공": "휴대폰 저장 실패")
            .setPositiveButton(android.R.string.ok, (dialog, which) -> {})
            .show();

    }

    private void saveCloud () {
        Runnable completion = () -> new AlertDialog.Builder(requireContext())
            .setTitle("Cloud 저장 성공")
            .setPositiveButton(android.R.string.ok, (dialog, which) -> {})
            .show();

        Consumer<Exception> onError = (e) -> {
            final String errorMessage;

            if (e instanceof InvalidPinException) {
                switch (((InvalidPinException) e).getCode()) {
                    case 10001: errorMessage = "PIN 길이 제한 (6자리 가능)"; break;
                    case 10002: errorMessage = "같은 숫자가 3개 이상 발생"; break;
                    case 10003: errorMessage = "연속된 숫자가 3개 이상 발생"; break;
                    default: errorMessage = "PIN 검증 오류"; break;
                }
            } else {
                errorMessage = e.toString();
            }

            new AlertDialog.Builder(requireContext())
                .setTitle("Cloud 저장 실패")
                .setMessage(errorMessage)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                })
                .show();
        };

        acquirePassword(requireContext(), "인증서 발급 저장",
            true, false, "", pin -> {
                getViewModel().saveCloud(pin, completion, onError);
            });
    }

    private void showBillingActivity(String reference) {
        Intent intent = new Intent(this.getContext(), BillActivity.class);
        intent.putExtra(BillActivity.IS_MAIN_SERVER, false); // 메인 서버인 경우에는  true로 설정
        intent.putExtra(BillActivity.OPERATION, BillActivity.ISSUE); // 갱신인 경우에는 BillActivity.UPDATE 적용
        //intent.putExtra(BillActivity.REFERENCE, reference); // 갱신인 경우에는 인증서 일련번호 스트링 입력
        // billing 시 mReference를 가공 후 전달해 주는 로직으로 변경
        intent.putExtra(BillActivity.REFERENCE, BillParam.makeBillParam(reference)); // 갱신인 경우에는 인증서 일련번호 스트링 입력
        // opp code값이 result message를 통해 넘어온 경우에 처리되어야 함
        if (opp != null) intent.putExtra(BillActivity.OPP, opp);
        startActivityForResult(intent, BillActivity.ID); // 결과는 onActivityResult에서 확인
    }

    private void acquirePassword(Context context, String title, boolean pinMode, boolean confirmPassword, String initialPassword, Consumer<String> completion) {
        ObservableField<String> pwd1 = new ObservableField<String>();
        ObservableField<String> pwd2 = new ObservableField<String>();

        AlertPasswordBinding binding =
            AlertPasswordBinding.inflate(LayoutInflater.from(context));

        binding.setPassword1(pwd1);
        binding.setPassword2(pwd2);
        binding.setConfirmPassword(confirmPassword);
        binding.setPinMode(pinMode);

        AlertDialog alert = new AlertDialog.Builder(context)
            .setTitle(title)
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
}
