package com.signkorea.cloud.sample.views.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.signkorea.certmanager.BillActivity;
import com.signkorea.certmanager.BillParam;
import com.signkorea.cloud.sample.databinding.FragmentIssueCertificateBinding;
import com.signkorea.cloud.sample.enums.CertificateOperation;
import com.signkorea.cloud.sample.utils.PasswordDialog;
import com.signkorea.cloud.sample.viewModels.IssueCertificateFragmentViewModel;
import com.signkorea.cloud.sample.views.base.ViewModelFragment;

import java.util.Hashtable;
import java.util.function.Consumer;

public class IssueCertificateFragment extends ViewModelFragment<FragmentIssueCertificateBinding, IssueCertificateFragmentViewModel> {

    private String opp = null;
    private CertificateOperation operation = CertificateOperation.issue;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getViewModel().init();
        getBinding().issue.setOnClickListener(view1 -> issue());
        getBinding().savePhone.setOnClickListener(view1 -> savePhone());
        getBinding().saveCloud.setOnClickListener(view1 -> saveCloud());
    }

    // issue() 처리 중 BillActivity 화면 전환 후 결과 처리
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
                            .setPositiveButton(android.R.string.ok, null)
                            .setNegativeButton(android.R.string.cancel, null)
                            .setOnDismissListener(dialog -> dismissLoading())
                            .show());
                };
                getViewModel().issue(completion);
            }
            // 수행을 제대로 하지 못한 경우
            else if(resultCode == Activity.RESULT_CANCELED)
            {
                String reason = "발급 취소 : ";
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

                if(((String)ret.get("MESSAGE")).contains("SK_MC"))
                    opp = "SK_MC";

                showBillingActivity(getViewModel().refNum.get());
            } else {
                requireActivity().runOnUiThread(() -> new AlertDialog.Builder(requireActivity())
                    .setTitle((String)ret.get("CODE"))
                    .setMessage(ret.get("MESSAGE").toString())
                    .setPositiveButton(android.R.string.ok, null)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setOnDismissListener(dialog -> {
                        dismissLoading();
                    })
                    .show());
            }
        };

        showLoading();
        getViewModel().issue(completion);
    }

    private void savePhone () {
        boolean ret = getViewModel().savePhone();
        new AlertDialog.Builder(requireContext())
            .setTitle(ret ? "휴대폰 저장 성공": "휴대폰 저장 실패")
            .setPositiveButton(android.R.string.ok, null)
            .show();

    }

    private void saveCloud () {
        Runnable completion = () -> {
            dismissLoading();
            new AlertDialog.Builder(requireContext())
                    .setTitle("Cloud 저장 성공")
                    .setPositiveButton(android.R.string.ok, null)
                    .setOnCancelListener(d -> navigateToReturnView(false))
                    .show();
        };

        Consumer<Exception> onError = (e) -> {
            dismissLoading();
            alertException(e, "Cloud 저장 실패");
        };

        PasswordDialog.show(requireContext(),
                "인증서 발급 저장",
                true,
                false,
                "",
                pin -> {
                    showLoading();
                    getViewModel().saveCloud(pin, completion, onError);
                },
                this::dismissLoading);
    }

    private void showBillingActivity(String reference) {
        Intent intent = new Intent(this.getContext(), BillActivity.class);
        intent.putExtra(BillActivity.IS_MAIN_SERVER, false); // 메인 서버인 경우에는  true로 설정
        intent.putExtra(BillActivity.OPERATION, BillActivity.ISSUE);
        intent.putExtra(BillActivity.REFERENCE, BillParam.makeBillParam(reference));
        if (opp != null)
            intent.putExtra(BillActivity.OPP, opp);
        startActivityForResult(intent, BillActivity.ID); // 결과는 onActivityResult 에서 확인
    }
}
