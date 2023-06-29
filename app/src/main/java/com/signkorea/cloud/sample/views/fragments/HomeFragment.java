package com.signkorea.cloud.sample.views.fragments;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;

import com.signkorea.cloud.sample.views.base.DataBindingFragment;
import com.signkorea.cloud.sample.databinding.FragmentHomeBinding;
import com.signkorea.cloud.sample.enums.SignMenuType;

public class HomeFragment extends DataBindingFragment<FragmentHomeBinding> {
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        NavDirections directions;

        // 계정 관리
        directions = HomeFragmentDirections.actionHomeFragmentToAccountManagementFragment();
        getBinding().manageAccountButton.setOnClickListener(
            Navigation.createNavigateOnClickListener(directions));

        // 인증서 관리
        directions = HomeFragmentDirections.actionHomeFragmentToCertificateManagementFragment();
        getBinding().manageCertificateButton.setOnClickListener(
            Navigation.createNavigateOnClickListener(directions));

        // 로그인
        directions = HomeFragmentDirections.actionHomeFragmentToLoginFragment()
                .setSignMenuType(SignMenuType.LOGIN);
        getBinding().loginButton.setOnClickListener(
                Navigation.createNavigateOnClickListener(directions));

        //주문
        directions = HomeFragmentDirections.actionHomeFragmentToLoginFragment()
                .setSignMenuType(SignMenuType.ORDER);
        getBinding().briefButton.setOnClickListener(
                Navigation.createNavigateOnClickListener(directions));

        //타기관 인증서 등록
        directions = HomeFragmentDirections.actionHomeFragmentToLoginFragment()
                .setSignMenuType(SignMenuType.REGISTER);
        getBinding().regiButton.setOnClickListener(
                Navigation.createNavigateOnClickListener(directions));
    }
}

