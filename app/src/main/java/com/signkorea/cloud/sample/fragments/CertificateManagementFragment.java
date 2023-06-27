package com.signkorea.cloud.sample.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;

import com.signkorea.cloud.sample.DataBindingFragment;
import com.signkorea.cloud.sample.databinding.FragmentCertificateManagementBinding;
import com.signkorea.cloud.sample.enums.CertificateOperation;

import java.util.Hashtable;
import java.util.function.Consumer;

import lombok.var;

public class CertificateManagementFragment extends DataBindingFragment<FragmentCertificateManagementBinding> {
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        NavDirections direction;
        boolean withCMP = false;

        getBinding().billSwitch.setOnCheckedChangeListener((compoundButton, checked) -> {
            getBinding().billSwitch.setText(checked ? "withBill" : "withOutBill");
        });

        HomeFragment.getInstance().setfid(getNavController().getCurrentDestination().getId());

        // 등록
        direction = CertificateManagementFragmentDirections
            .actionCertificateManagementFragmentToCertificateListFragment()
            .setOperation(CertificateOperation.register);
        getBinding().registerCertificateButton.setOnClickListener(
            Navigation.createNavigateOnClickListener(direction));

        // 내보내기
        direction = CertificateManagementFragmentDirections
            .actionCertificateManagementFragmentToCertificateListFragment()
            .setOperation(CertificateOperation.export);
        getBinding().exportCertificateButton.setOnClickListener(
            Navigation.createNavigateOnClickListener(direction));

        // PIN 변경
        direction = CertificateManagementFragmentDirections
            .actionCertificateManagementFragmentToCertificateListFragment()
            .setOperation(CertificateOperation.changePin);
        getBinding().changeCertificatePinButton.setOnClickListener(
            Navigation.createNavigateOnClickListener(direction));

        // 삭제
        direction = CertificateManagementFragmentDirections
            .actionCertificateManagementFragmentToCertificateListFragment()
            .setOperation(CertificateOperation.delete);
        getBinding().deleteCertificateButton.setOnClickListener(
            Navigation.createNavigateOnClickListener(direction));

        // 폐지
        direction = CertificateManagementFragmentDirections
                .actionCertificateManagementFragmentToCertificateListFragment()
                .setOperation(CertificateOperation.revoke);
        getBinding().revokeCertificateButton.setOnClickListener(
                Navigation.createNavigateOnClickListener(direction));

        // 발급
        getBinding().issueCertificateButton.setOnClickListener(view1 -> issue());

        // 갱신
        getBinding().renewalCertificateButton.setOnClickListener(view1 -> update());

        // 잠금 해제
        direction = CertificateManagementFragmentDirections
            .actionCertificateManagementFragmentToCertificateListFragment()
            .setOperation(CertificateOperation.unlock);
        getBinding().unlockCertificateButton.setOnClickListener(
            Navigation.createNavigateOnClickListener(direction));
    }

    private void issue() {
        boolean selected = getBinding().billSwitch.isSelected();

        NavDirections direction = CertificateManagementFragmentDirections
            .actionCertificateManagementFragmentToIssueCertificateFragment()
            .setBill(getBinding().billSwitch.isChecked());

//        Navigation.createNavigateOnClickListener(direction);
        Navigation.findNavController(getView()).navigate(direction);
    }

    private void update() {
        boolean selected = getBinding().billSwitch.isSelected();

        NavDirections direction = CertificateManagementFragmentDirections
            .actionCertificateManagementFragmentToCertificateListFragment()
            .setOperation(CertificateOperation.update)
            .setBill(getBinding().billSwitch.isChecked());

        Navigation.findNavController(getView()).navigate(direction);
    }
}
