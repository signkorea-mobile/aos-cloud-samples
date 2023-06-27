package com.signkorea.cloud.sample.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;

import com.yettiesoft.cloud.Client;
import com.yettiesoft.cloud.InvalidLicenseException;
import com.yettiesoft.cloud.NonmemberException;
import com.signkorea.cloud.sample.DataBindingFragment;
import com.signkorea.cloud.sample.MainActivity;
import com.signkorea.cloud.sample.databinding.FragmentAccountManagementBinding;
import com.signkorea.cloud.sample.utils.OnceRunnable;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import lombok.val;

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
                .setMessage("Cloud NPKI 라이센스 정보가 유효하지 않습니다.")
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {})
                .show();
        }
    });

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        HomeFragment.getInstance().setfid(getNavController().getCurrentDestination().getId());

        NavDirections direction;

        direction = AccountManagementFragmentDirections.actionAccountManagementFragmentToAutoConnectionDevicesFragment();
        getBinding().autoConnectDevicesButton.setOnClickListener(Navigation.createNavigateOnClickListener(direction));

        direction = AccountManagementFragmentDirections.actionAccountManagementFragmentToSecureSettingFragment();
        getBinding().secureSettingButton.setOnClickListener(Navigation.createNavigateOnClickListener(direction));

        getBinding().closeAccountButton.setOnClickListener(this::deleteAccount);

        // 연결 끊기
        getBinding().disconnectButton.setOnClickListener(this::onDisconnect);



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
                .setPositiveButton(android.R.string.ok, (dialog, which) ->
                    Navigation.findNavController(requireView()).popBackStack()
                )
                .show();

        Consumer<Exception> onError = exception -> {
            if (exception instanceof NonmemberException) {
                new AlertDialog.Builder(requireContext())
                        .setTitle("회원 탈퇴 실패")
                        .setMessage("공동 인증 서비스 회원이 아닙니다.")
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> Optional
                                .ofNullable(getInterFragmentStore().<Runnable>peek(MainActivity.MoAuthCancelAction))
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
                        client.deleteAccount(onAccountClosed, onError);
                    }
                })
                .setNegativeButton(android.R.string.cancel,(dialog, which) -> {})
                .show();
    }

    private void fetchUserInfo(View view) {
        Consumer<Exception> onError = exception -> alertException(exception, "사용자 정보 조회");

        Objects.requireNonNull(client).getUserInfo((userInfo) -> {
            val message = Optional.ofNullable(userInfo)
                .map(ui -> String.format("이름 : %s\n전화번호 : %s", ui.getName(), ui.getPhoneNumber()))
                .orElse("서버와 연결되어 있지 않습니다.");

            new AlertDialog.Builder(requireContext())
                .setTitle("사용자 정보 조회")
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
        }, onError);
    }

    private void onDisconnect(View view) {
        if (client == null) {
            return;
        }

        Runnable completion = () -> new AlertDialog.Builder(requireContext())
                .setMessage("연결 끊기 성공")
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {})
                .show();

        Consumer<Exception> onError = exception -> alertException(exception, "연결 끊기");

        new AlertDialog.Builder(requireContext())
                .setMessage("연결을 끊습니다.")
                .setPositiveButton(android.R.string.ok, (dialog, which) ->
                        client.disconnect(completion, onError)
                )
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> {})
                .show();
    }

    private void onCheckAutoConnect(View view) {
        if (client == null) {
            return;
        }

        Consumer<Boolean> completion = connected -> new AlertDialog.Builder(requireContext())
                .setTitle("연결 확인")
                .setMessage(connected ? "서버와 연결되어 있습니다." : "서버와 연결되어 있지 않습니다.")
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {})
                .show();

        Consumer<Exception> onError = exception -> alertException(exception, "연결 확인");

        client.checkConnect(completion, onError);
    }

    private void getCurrentDeviceId(View view) {
        if (client == null) {
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("deviceID")
                .setMessage(client.getCurrentDeviceId())
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {})
                .show();
    }
}
