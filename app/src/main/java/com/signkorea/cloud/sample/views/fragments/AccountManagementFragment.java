package com.signkorea.cloud.sample.views.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;

import com.signkorea.cloud.sample.databinding.FragmentAccountManagementBinding;
import com.signkorea.cloud.sample.utils.OnceRunnable;
import com.signkorea.cloud.sample.viewModels.InterFragmentStore;
import com.signkorea.cloud.sample.views.base.DataBindingFragment;
import com.yettiesoft.cloud.Client;
import com.yettiesoft.cloud.InvalidLicenseException;
import com.yettiesoft.cloud.NonmemberException;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public class AccountManagementFragment extends DataBindingFragment<FragmentAccountManagementBinding> {
    private Client client = new Client();

    private final OnceRunnable initClient = new OnceRunnable(() -> {
        try {
            client.init(requireContext().getApplicationContext());
            client.setDelegate((Client.Delegate)requireActivity());
        } catch (InvalidLicenseException e) {
            client = null;
        }
    });

    private final OnceRunnable checkLicense = new OnceRunnable(() -> {
        if (client == null) {
            new AlertDialog.Builder(requireContext())
                .setTitle("라이선스 안내")
                .setMessage("클라우드 라이선스 정보가 유효하지 않습니다.")
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {})
                .show();
        }
    });

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        NavDirections direction;

        // 자동 연결 기기 조회 및 해제
        direction = AccountManagementFragmentDirections.actionAccountManagementFragmentToAutoConnectionDevicesFragment();
        getBinding().autoConnectDevicesButton.setOnClickListener(Navigation.createNavigateOnClickListener(direction));

        // 보안 설정
        direction = AccountManagementFragmentDirections.actionAccountManagementFragmentToSecureSettingFragment();
        getBinding().secureSettingButton.setOnClickListener(Navigation.createNavigateOnClickListener(direction));

        getBinding().closeAccountButton.setOnClickListener(this::deleteAccount);    // 탈퇴
        getBinding().disconnectButton.setOnClickListener(this::onDisconnect);   // 연결 끊기

        initClient.run();
    }

    @Override
    public void onResume() {
        super.onResume();
        checkLicense.run();
    }

    private void deleteAccount(View view) {
        Runnable onAccountClosed = () -> new AlertDialog.Builder(requireContext())
                .setTitle("회원 탈퇴")
                .setMessage("공동 인증 서비스에서 탈퇴 하였습니다.")
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        dismissLoading();
                        getNavController().popBackStack();
                })
                .show();

        Consumer<Exception> onError = exception -> {
            dismissLoading();
            if (exception instanceof NonmemberException) {
                new AlertDialog.Builder(requireContext())
                        .setTitle("회원 탈퇴 실패")
                        .setMessage("공동 인증 서비스 회원이 아닙니다.")
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> Optional
                                .ofNullable(getInterFragmentStore().<Runnable>remove(InterFragmentStore.MO_ACTION_CANCEL))
                                .ifPresent(Runnable::run)
                        )
                        .show();
            } else {
                new AlertDialog.Builder(requireContext())
                        .setTitle("회원 탈퇴 실패")
                        .setMessage(exception.toString())
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {})
                        .show();
            }
        };

        new AlertDialog.Builder(requireContext())
                .setTitle("회원 탈퇴")
                .setMessage("공동 인증 서비스에서 탈퇴 하시겠습니까?")
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    if (client != null) {
                        showLoading();
                        client.deleteAccount(onAccountClosed, onError);
                    }
                })
                .setNegativeButton(android.R.string.cancel,(dialog, which) -> {})
                .show();
    }

    private void onDisconnect(View view) {
        if (client == null) {
            return;
        }

        Consumer<Exception> onError = exception -> alertException(exception, "연결 끊기");

        Objects.requireNonNull(client).checkConnect(connected -> {
            if(connected) {
                Runnable completion = () -> new AlertDialog.Builder(requireContext())
                        .setMessage("연결 끊기 성공")
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        })
                        .show();

                new AlertDialog.Builder(requireContext())
                        .setMessage("클라우드 서비스 연결을 끊습니다.")
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> client.disconnect(completion, onError))
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            }
            else {
                requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "클라우드 서비스에 연결되어 있지 않습니다.", Toast.LENGTH_SHORT).show());
            }
        }, onError);
    }
}
