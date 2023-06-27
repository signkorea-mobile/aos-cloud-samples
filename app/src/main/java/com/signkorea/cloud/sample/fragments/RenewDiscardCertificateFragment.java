package com.signkorea.cloud.sample.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;

import com.lumensoft.ks.KSException;
import com.signkorea.cloud.sample.ViewModelFragment;
import com.signkorea.cloud.sample.databinding.FragmentRenewDiscardCertificateBinding;
import com.signkorea.cloud.sample.viewModels.RenewDiscardCertificateFragmentViewModel;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class RenewDiscardCertificateFragment extends ViewModelFragment<FragmentRenewDiscardCertificateBinding, RenewDiscardCertificateFragmentViewModel> {

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        int certificateIndex = RenewDiscardCertificateFragmentArgs.fromBundle(getArguments()).getCertificateIndex();
//        getViewModel().init(certificateIndex);
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
}
