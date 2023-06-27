package com.signkorea.cloud.sample.fragments;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import com.yettiesoft.cloud.PhoneNumberProofTransaction;
import com.signkorea.cloud.sample.DataBindingFragment;
import com.signkorea.cloud.sample.MainActivity;
import com.signkorea.cloud.sample.R;
import com.signkorea.cloud.sample.databinding.FragmentPhoneNumberAuthenticationV1Binding;

import java.util.Timer;
import java.util.TimerTask;

import lombok.val;

public class PhoneNumberAuthenticationV1Fragment extends DataBindingFragment<FragmentPhoneNumberAuthenticationV1Binding> {
    private PhoneNumberProofTransaction transaction;
    private Timer timer = null;

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                cancel();
            }
        });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        transaction = getInterFragmentStore().accept(
            R.id.phoneNumberAuthenticationV1Fragment,
            MainActivity.MoAuthTransaction);

        Runnable confirmAction = getInterFragmentStore().accept(
            R.id.phoneNumberAuthenticationV1Fragment,
            MainActivity.MoAuthConfirmAction);

        // 확인
        getBinding().confirmButton.setEnabled(false);
        getBinding().confirmButton.setOnClickListener(button -> {
            //noinspection ConstantConditions
            confirmAction.run();
        });

        // 취소
        getBinding().cancelButton.setOnClickListener(button -> cancel());

        getBinding().setAuthCode(transaction.getAuthCode());
    }

    @Override
    public void onResume() {
        super.onResume();

        schedule();
    }

    @Override
    public void onPause() {
        super.onPause();

        unschedule();
    }

    @SuppressLint({"SetTextI18n", "DefaultLocale"})
    private void updateTime(PhoneNumberProofTransaction.Status status) {
        switch (status) {
            case AuthCodeDoesNotMatch:
            case InProgress: {
                val totSec = Math.max(transaction.getTimeout().getTime() - System.currentTimeMillis(), 0) / 1000;
                val str = String.format("%02d:%02d", totSec / 60, totSec % 60);
                getBinding().confirmButton.setText(str);
            } break;

            case Complete: {
                getBinding().confirmButton.setBackgroundColor(0xffe65619);
                getBinding().confirmButton.setText("확인");
                getBinding().confirmButton.setEnabled(true);
                transaction.commit();
                Navigation.findNavController(getView()).navigate(R.id.homeFragment);
                //getNavController().navigate(HomeFragment.getInstance().getfid());
            } break;

            case Canceled:
            case Timeout:
            case AuthCodeInvalidated:
            default: {
                getBinding().confirmButton.setText("00:00");
            } break;
        }
    }

    private boolean checkStatus(PhoneNumberProofTransaction.Status status, int mismatchCount) {
        switch (transaction.getStatus()) {
            case InProgress:
                return true;

            case AuthCodeDoesNotMatch: {
                @SuppressLint("DefaultLocale")
                val message = String.format(
                    "인증코드가 일치하지 않습니다. 정확한 인증코드를 전송해 주시기 바랍니다.\n[%d/5]",
                    mismatchCount);

                new AlertDialog.Builder(requireContext())
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        transaction.resume();
                        schedule();
                    })
                    .setNegativeButton(android.R.string.cancel, (dialog, which) -> cancel())
                    .show();
            } break;

            case Timeout: {
                new AlertDialog.Builder(requireContext())
                    .setMessage("인증 시간이 초과되었습니다.")
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> cancel())
                    .show();
            } break;

            case AuthCodeInvalidated: {
                new AlertDialog.Builder(requireContext())
                    .setMessage("인증코드 전송 횟수를 초과하였습니다.")
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> cancel())
                    .show();
            } break;

            default:
                break;
        }

        return false;
    }

    @SuppressWarnings("ConstantConditions")
    private void cancel() {
        getInterFragmentStore().<Runnable>peek(MainActivity.MoAuthCancelAction).run();
    }

    private void schedule() {
        unschedule();

        val status = transaction.getStatus();
        updateTime(status);

        switch (status) {
            case InProgress:
            case AuthCodeDoesNotMatch:
                break;
            default:
                return;
        }

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                val status = transaction.getStatus();
                val mismatchCount = transaction.getAuthCodeMismatchCount();

                requireActivity().runOnUiThread(() -> {
                    updateTime(status);

                    if (!checkStatus(status, mismatchCount)) {
                        unschedule();
                    }
                });
            }
        }, 500, 500);
    }

    private void unschedule() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
}
