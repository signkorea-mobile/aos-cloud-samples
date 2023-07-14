package com.signkorea.cloud.sample.views.fragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.signkorea.cloud.sample.R;
import com.signkorea.cloud.sample.databinding.FragmentPhoneNumberAuthenticationV2Binding;
import com.signkorea.cloud.sample.viewModels.InterFragmentStore;
import com.signkorea.cloud.sample.views.base.DataBindingFragment;
import com.yettiesoft.cloud.PhoneNumberProofTransaction;

import java.util.Timer;
import java.util.TimerTask;

import lombok.val;

public class PhoneNumberAuthenticationV2Fragment extends DataBindingFragment<FragmentPhoneNumberAuthenticationV2Binding> {
    private PhoneNumberProofTransaction transaction;
    private Runnable onCancel;
    private Timer timer = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                cancel();
            }
        });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        transaction = getInterFragmentStore().remove(
            R.id.phoneNumberAuthenticationV2Fragment,
            InterFragmentStore.MO_API_EXECUTOR);

        onCancel = getInterFragmentStore().remove(
                R.id.phoneNumberAuthenticationV2Fragment,
                InterFragmentStore.MO_API_CANCEL);

        // 확인
        getBinding().confirmButton.setEnabled(false);
        getBinding().confirmButton.setBackgroundColor(0xff505050);

        // 취소
        getBinding().cancelButton.setOnClickListener(button -> cancel());

        // 인증 메시지 보내기
        getBinding().sendButton.setOnClickListener(button -> sendMessage());

        // 인증 방식 변경
        getBinding().fallbackButton.setOnClickListener(button -> transaction.fallback());
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        getInterFragmentStore().remove(R.id.phoneNumberAuthenticationV2Fragment, InterFragmentStore.MO_API_CANCEL);
    }

    private void confirm() {
        transaction.commit();
        navigateToReturnView();
    }

    @SuppressWarnings("ConstantConditions")
    private void cancel() {
        transaction.cancel();
        onCancel.run();
        navigateToReturnView();      // MO에 진입하기 전 화면으로 이동
    }

    private void sendMessage() {
        val smsBody = String.format("클라우드 인증: 인증문자 보내기\n[%s]\n이전단계로 돌아가 주세요", transaction.getAuthCode());
        val intent = new Intent(Intent.ACTION_SENDTO)
            .setData(Uri.fromParts("smsto", transaction.getFeedbackNumber(), null))
            .putExtra("sms_body", smsBody);

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException ignored) {
            new AlertDialog.Builder(requireContext())
                .setMessage("인증 메시지를 보낼 수 없습니다.")
                .setPositiveButton(android.R.string.ok, null)
                .show();
            return;
        }

        val toastMessage = String.format("SMS에 생성된 인증번호 전송 후 \"%s\"(으)로 돌아가 주세요.", getString(R.string.app_name));
        Toast.makeText(requireContext().getApplicationContext(), toastMessage, Toast.LENGTH_LONG).show();

        schedule();
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
                // MO인증이 완료된 경우 후속 진행 처리
                confirm();
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
        switch (status) {
            case InProgress:
                return true;

            case AuthCodeDoesNotMatch: {
                requireActivity().runOnUiThread(() -> {
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
                });
            } break;

            case Timeout: {
                requireActivity().runOnUiThread(() -> {
                    new AlertDialog.Builder(requireContext())
                            .setMessage("인증 시간이 초과되었습니다.")
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> cancel())
                            .show();
                });
            } break;

            case AuthCodeInvalidated: {
                requireActivity().runOnUiThread(() -> {
                    new AlertDialog.Builder(requireContext())
                            .setMessage("인증코드 전송 횟수를 초과하였습니다.")
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> cancel())
                            .show();
                });
            } break;

            default:
                break;
        }

        return false;
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

                requireActivity().runOnUiThread(() -> updateTime(status));

                if (!checkStatus(status, mismatchCount))
                    unschedule();
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
