package com.signkorea.cloud.sample.fragments;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.Navigation;

import com.lumensoft.ks.KSException;
import com.yettiesoft.cloud.Client;
import com.signkorea.cloud.sample.MainActivity;
import com.signkorea.cloud.sample.R;
import com.signkorea.cloud.sample.SimpleSharedPreferences;
import com.signkorea.cloud.sample.ViewModelFragment;
import com.signkorea.cloud.sample.databinding.FragmentUserInfoFormBinding;
import com.signkorea.cloud.sample.viewModels.UserInfoFormFragmentViewModel;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class UserInfoFormFragment extends ViewModelFragment<FragmentUserInfoFormBinding, UserInfoFormFragmentViewModel> {
    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                getInterFragmentStore().<Runnable>peek(MainActivity.MoAuthCancelAction).run();
            }
        });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getViewModel().init(requireContext());

        // 생년월일
        /*getBinding().birthdayField.setOnClickListener(v -> {
            DatePickerDialog.OnDateSetListener listener = (view1, year, month, dayOfMonth) ->
                    getViewModel().setBirthday(year, month, dayOfMonth);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    getActivity(),
                    listener,
                    getViewModel().birthday.get(Calendar.YEAR),
                    getViewModel().birthday.get(Calendar.MONTH),
                    getViewModel().birthday.get(Calendar.DATE));
            datePickerDialog.show();
        });
*/
        Client.UserInfoAcceptor userInfoAcceptor = getInterFragmentStore().accept(
                R.id.userInfoFormFragment,
                MainActivity.MoAuthConfirmAction);

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
            //noinspection ConstantConditions
            getInterFragmentStore().<Runnable>peek(MainActivity.MoAuthCancelAction).run();

            getNavController().navigate(HomeFragment.getInstance().getfid());
            //Navigation.findNavController(getView()).navigate(R.id.homeFragment);

        });
    }

    @Override
    public void onAuthenticationError(int i, CharSequence charSequence) {
        String message = null;
        if (i == KSException.FAILED_CLOUD_BIO_INVALID_PIN) {
            message = charSequence.toString();
        } else {
            message = i + " : " + charSequence;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("생체 인증 실패")
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                })
                .show();
    }
}
