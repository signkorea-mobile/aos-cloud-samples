package com.signkorea.cloud.sample.views.fragments;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;

import com.signkorea.cloud.sample.views.base.DataBindingFragment;
import com.signkorea.cloud.sample.databinding.FragmentCertificateManagementBinding;
import com.signkorea.cloud.sample.enums.CertificateOperation;

public class CertificateManagementFragment extends DataBindingFragment<FragmentCertificateManagementBinding> {
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        NavDirections direction;

        // 등록 (로컬 전용)
        direction = CertificateManagementFragmentDirections
            .actionCertificateManagementFragmentToLocalCertificateListFragment()
            .setOperation(CertificateOperation.register);
        getBinding().registerCertificateButton.setOnClickListener(
            Navigation.createNavigateOnClickListener(direction));

        // 내보내기 (클라우드 전용)
        direction = CertificateManagementFragmentDirections
            .actionCertificateManagementFragmentToCloudCertificateListFragment()
            .setOperation(CertificateOperation.export);
        getBinding().exportCertificateButton.setOnClickListener(
            Navigation.createNavigateOnClickListener(direction));

        // PIN 변경
        direction = CertificateManagementFragmentDirections
            .actionCertificateManagementFragmentToCloudCertificateListFragment()
            .setOperation(CertificateOperation.changePin);
        getBinding().changeCertificatePinButton.setOnClickListener(
            Navigation.createNavigateOnClickListener(direction));

        // 삭제
        direction = CertificateManagementFragmentDirections
            .actionCertificateManagementFragmentToCloudCertificateListFragment()
            .setOperation(CertificateOperation.delete);
        getBinding().deleteCertificateButton.setOnClickListener(
            Navigation.createNavigateOnClickListener(direction));

        // 발급
        getBinding().issueCertificateButton.setOnClickListener(view1 -> issue());

        // 갱신 (클라우드)
        getBinding().renewCertificateCloudButton.setOnClickListener(view1 -> update(CertificateOperation.updateCloud));

        // 갱신 (로컬)
        getBinding().renewCertificateLocalButton.setOnClickListener(view1 -> update(CertificateOperation.updateLocal));

        // 잠금 해제
        direction = CertificateManagementFragmentDirections
            .actionCertificateManagementFragmentToCloudCertificateListFragment()
            .setOperation(CertificateOperation.unlock);
        getBinding().unlockCertificateButton.setOnClickListener(
            Navigation.createNavigateOnClickListener(direction));
    }

    private void issue() {
        NavDirections direction = CertificateManagementFragmentDirections
            .actionCertificateManagementFragmentToIssueCertificateFragment();
        getNavController().navigate(direction);
    }

    private void update(CertificateOperation operation) {
        NavDirections direction;

        if(operation == CertificateOperation.updateLocal) {
            direction = CertificateManagementFragmentDirections
                    .actionCertificateManagementFragmentToLocalCertificateListFragment()
                    .setOperation(operation);
        }
        else {
            direction = CertificateManagementFragmentDirections
                    .actionCertificateManagementFragmentToCloudCertificateListFragment()
                    .setOperation(operation);
        }

        getNavController().navigate(direction);
    }
}
