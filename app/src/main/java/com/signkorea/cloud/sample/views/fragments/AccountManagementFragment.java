package com.signkorea.cloud.sample.views.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;

import com.signkorea.cloud.sample.databinding.FragmentAccountManagementBinding;
import com.signkorea.cloud.sample.models.CloudRepository;
import com.signkorea.cloud.sample.views.base.DataBindingFragment;
import com.yettiesoft.cloud.NonmemberException;

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class AccountManagementFragment extends DataBindingFragment<FragmentAccountManagementBinding> {
    private CloudRepository cloudRepo = CloudRepository.getInstance();

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        NavDirections direction;

        // 자동 연결 기기 조회 및 해제
        direction = AccountManagementFragmentDirections.actionAccountManagementFragmentToAutoConnectionDevicesFragment();
        getBinding().autoConnectDevicesButton.setOnClickListener(Navigation.createNavigateOnClickListener(direction));

        getBinding().closeAccountButton.setOnClickListener(this::deleteAccount);    // 탈퇴
        getBinding().disconnectButton.setOnClickListener(this::onDisconnect);   // 연결 끊기
    }

    private void deleteAccount(View view) {
        Runnable onAccountClosed = () -> new AlertDialog.Builder(requireContext())
                .setTitle("회원 탈퇴")
                .setMessage("공동 인증 서비스에서 탈퇴 하였습니다.")
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        dismissLoading();
                        navigateToReturnView(false);
                        getNavController().popBackStack();
                })
                .show();

        Consumer<Exception> onError = exception -> {
            removeMoReturnDestinationViewId();
            if (exception instanceof NonmemberException) {
                new AlertDialog.Builder(requireContext())
                        .setTitle("회원 탈퇴 실패")
                        .setMessage("공동 인증 서비스 회원이 아닙니다.")
                        .setPositiveButton(android.R.string.ok, null)   // TODO 회원이 아닐 때 MO가 뜨나? OK에서 별도로 해야하는 것이 있는지 확인
                        .setOnDismissListener(d -> dismissLoading())
                        .show();
            } else {
                new AlertDialog.Builder(requireContext())
                        .setTitle("회원 탈퇴 실패")
                        .setMessage(exception.toString())
                        .setPositiveButton(android.R.string.ok, null)
                        .setOnDismissListener(d -> dismissLoading())
                        .show();
            }
        };

        new AlertDialog.Builder(requireContext())
                .setTitle("회원 탈퇴")
                .setMessage("공동 인증 서비스에서 탈퇴 하시겠습니까?")
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    showLoading();
                    cloudRepo.deleteAccount(onAccountClosed, onError);
                })
                .setNegativeButton(android.R.string.cancel,
                        (d, i) -> dismissLoading())
                .show();
    }

    private void onDisconnect(View view) {
        Consumer<Exception> onError = exception -> alertException(exception, "연결 끊기");
        Runnable completion = () -> new AlertDialog.Builder(requireContext())
                .setMessage("연결 끊기 성공")
                .setPositiveButton(android.R.string.ok, null)
                .setOnDismissListener(v -> {
                    navigateToReturnView(false);
                    dismissLoading();
                })
                .show();

        new AlertDialog.Builder(requireContext())
                .setMessage("클라우드 서비스 연결을 끊습니다.")
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    showLoading();
                    cloudRepo.disconnect(completion, onError);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
