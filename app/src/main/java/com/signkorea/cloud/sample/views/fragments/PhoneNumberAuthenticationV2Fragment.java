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

import com.signkorea.cloud.sample.viewModels.InterFragmentStore;
import com.yettiesoft.cloud.PhoneNumberProofTransaction;
import com.signkorea.cloud.sample.views.base.DataBindingFragment;
import com.signkorea.cloud.sample.R;
import com.signkorea.cloud.sample.databinding.FragmentPhoneNumberAuthenticationV2Binding;

import java.util.Timer;
import java.util.TimerTask;

import lombok.val;

public class PhoneNumberAuthenticationV2Fragment extends DataBindingFragment<FragmentPhoneNumberAuthenticationV2Binding> {
    private PhoneNumberProofTransaction transaction;
    private Timer timer;

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

        transaction = getInterFragmentStore().accept(
            R.id.phoneNumberAuthenticationV2Fragment,
            InterFragmentStore.MO_API_TRANSACTION);

        Runnable confirmAction = getInterFragmentStore().accept(
            R.id.phoneNumberAuthenticationV2Fragment,
            InterFragmentStore.MO_ACTION_CONFIRM);

        // 확인
        getBinding().confirmButton.setEnabled(false);
        getBinding().confirmButton.setBackgroundColor(0xff505050);
        getBinding().confirmButton.setOnClickListener(button -> {
            //noinspection ConstantConditions
            confirmAction.run();
        });

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

    @SuppressWarnings("ConstantConditions")
    private void cancel() {
        getInterFragmentStore().<Runnable>peek(InterFragmentStore.MO_ACTION_CANCEL).run();
        getInterFragmentStore().<Runnable>accept(R.id.phoneNumberAuthenticationV2Fragment, InterFragmentStore.MO_ACTION_CANCEL).run();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getInterFragmentStore().accept(R.id.phoneNumberAuthenticationV2Fragment, InterFragmentStore.MO_ACTION_CANCEL);
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
                // TODO
//                getBinding().confirmButton.setEnabled(true);
//                getBinding().confirmButton.setBackgroundColor(0xffe65619);
//                getBinding().confirmButton.setText("확인");
                // SMS인증이 확인되면 다음 단계로 진행
                getInterFragmentStore().remove(InterFragmentStore.FALLBACK_FRAGMENT_ID);
                getBinding().confirmButton.callOnClick();
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
