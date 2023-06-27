package com.signkorea.cloud.sample.fragments;

import android.app.AlertDialog;
import android.app.Application;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;

import com.signkorea.cloud.KSCertificateExt;
import com.yettiesoft.cloud.Client;
import com.yettiesoft.cloud.InvalidLicenseException;
import com.signkorea.cloud.sample.DataBindingFragment;
import com.signkorea.cloud.sample.databinding.FragmentHomeBinding;
import com.signkorea.cloud.sample.enums.CertificateOperation;
import com.signkorea.cloud.sample.utils.OnceRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class HomeFragment extends DataBindingFragment<FragmentHomeBinding> {
    @Nullable
    private Client client = null;

    private int selectindex = -1;
    private int frgid = -1;

    private final OnceRunnable initClient = new OnceRunnable(() -> {
        try {
            client = new Client()
                .init(requireActivity().getApplicationContext())
                .setDelegate((Client.Delegate)requireActivity());
        } catch (InvalidLicenseException exception) {
            alertException(exception);
        }
    });

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        HomeFragment.getInstance().setfid(getNavController().getCurrentDestination().getId());

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
                .setSigntype(1)
                .setLabeltitle("로그인");

        getBinding().loginButton.setOnClickListener(
                Navigation.createNavigateOnClickListener(directions));

        //주문
        directions = HomeFragmentDirections.actionHomeFragmentToLoginFragment().setSigntype(2).setLabeltitle("주문");
        getBinding().briefButton.setOnClickListener(
                Navigation.createNavigateOnClickListener(directions));

        //타인증서 등록
        directions = HomeFragmentDirections.actionHomeFragmentToLoginFragment().setSigntype(3).setLabeltitle("타인증서 등록");
        getBinding().regiButton.setOnClickListener(
                Navigation.createNavigateOnClickListener(directions));
    }

    @Override
    public void onResume() {
        super.onResume();

        initClient.run();
    }

    public void setindex(int index) {

        this.selectindex = index;
    }

    public int getindex() {

        return selectindex;
    }

    public void setfid(int fid) {

        this.frgid = fid;
    }

    public int getfid() {

        return frgid;
    }

    private static HomeFragment instance = null;

    public static synchronized HomeFragment getInstance(){

        if(null == instance){
            instance = new HomeFragment();
        }
        return instance;
    }
}

