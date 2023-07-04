package com.signkorea.cloud.sample.views.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.signkorea.cloud.sample.R;
import com.signkorea.cloud.sample.databinding.FragmentUserInfoFormBinding;
import com.signkorea.cloud.sample.utils.SimpleSharedPreferences;
import com.signkorea.cloud.sample.viewModels.InterFragmentStore;
import com.signkorea.cloud.sample.viewModels.UserInfoFormFragmentViewModel;
import com.signkorea.cloud.sample.views.base.ViewModelFragment;
import com.yettiesoft.cloud.Client;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class UserInfoFormFragment extends ViewModelFragment<FragmentUserInfoFormBinding, UserInfoFormFragmentViewModel> {
    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
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

        dismissLoading();
        getViewModel().init(requireContext());

        Client.UserInfoAcceptor userInfoAcceptor = getInterFragmentStore().remove(
                R.id.userInfoFormFragment,
                InterFragmentStore.MO_ACTION_CONFIRM);

        // 확인
        getBinding().confirmButton.setOnClickListener(button -> {
            String userName = getViewModel().getRefinedUserName();
            String phoneNumber = getViewModel().getRefinedPhoneNumber();
            String sbirthday = getBinding().birthdayField.getText().toString();
            Date birthday;

            try {
                birthday = new SimpleDateFormat("yyyyMMdd").parse(sbirthday);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
            boolean autoConnect = getViewModel().autoConnection.get();

            String message;
            if (userName.isEmpty()) {
                message = "이름이 올바르지 않습니다.";
            } else if (phoneNumber == null || phoneNumber.length() < 11) {
                message = "전화번호가 올바르지 않습니다.";
            }
            else if (sbirthday == null || sbirthday.length() < 8) {
                    message = "생년월일이 올바르지 않습니다.";
            } else {
                message = null;
            }

            if (message != null) {
                new AlertDialog.Builder(requireContext())
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
                return;
            }

            SimpleSharedPreferences
                .getInstance(requireContext().getApplicationContext())
                .edit()
                .userName(userName)
                .phoneNumber(phoneNumber)
                .birthday(birthday)
                .authConnect(autoConnect)
                .commit();

            //noinspection ConstantConditions
            userInfoAcceptor.consume(userName, phoneNumber, birthday, autoConnect);
        });

        // 취소
        getBinding().cancelButton.setOnClickListener(button -> {
            cancel();
        });
    }

    private void cancel() {
        getInterFragmentStore().<Runnable>remove(R.id.userInfoFormFragment, InterFragmentStore.MO_API_CANCEL).run();
        getInterFragmentStore().<Runnable>remove(InterFragmentStore.MO_ACTION_CANCEL).run();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getInterFragmentStore().remove(R.id.userInfoFormFragment, InterFragmentStore.MO_API_CANCEL);
    }
}
