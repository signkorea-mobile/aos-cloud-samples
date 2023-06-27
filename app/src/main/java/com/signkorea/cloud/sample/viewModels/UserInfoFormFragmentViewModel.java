package com.signkorea.cloud.sample.viewModels;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.ObservableBoolean;
import androidx.databinding.ObservableField;
import androidx.lifecycle.ViewModel;

import com.signkorea.cloud.sample.SimpleSharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;

import lombok.val;

public class UserInfoFormFragmentViewModel extends ViewModel  {
    public final ObservableField<String> userName = new ObservableField<>();
    public final ObservableField<String> phoneNumber = new ObservableField<>();
    public final ObservableField<String> birthdayText = new ObservableField<>();
    public final ObservableBoolean autoConnection = new ObservableBoolean();

    public Calendar birthday = Calendar.getInstance();

    public void init(Context context) {
        SimpleSharedPreferences sharedPref =
                SimpleSharedPreferences.getInstance(context.getApplicationContext());

        userName.set(sharedPref.getUserName());
        phoneNumber.set(sharedPref.getPhoneNumber());
        autoConnection.set(sharedPref.getAutoConnect());

        setBirthday(sharedPref.getBirthday());
    }

    private void setBirthday(Date birthday) {
        if (birthday != null) {
            this.birthday.setTimeInMillis(birthday.getTime());
            updateBirthdayText();
        }
    }

    public void setBirthday(int year, int month, int date) {
        birthday.set(year, month, date, 0, 0, 0);
        updateBirthdayText();
    }

    private void updateBirthdayText() {
        @SuppressLint("SimpleDateFormat")
        String birthdayStr = new SimpleDateFormat("yyyyMMdd").format(birthday.getTime());

        birthdayText.set(birthdayStr);
    }

    @Nullable
    public String getRefinedPhoneNumber() {
        // 숫자 이외의 문제 제거
        val str = Optional.ofNullable(phoneNumber.get())
            .map(s -> s.replaceAll("\\D", ""))
            .filter(s -> s.length() > 0)
            .orElse( null);

        if (str != null) {
            // 010xxxxxxxx
            if (str.startsWith("010") && str.length() == 11) {
                return str;
            }

            // 8210xxxxxxxx
            if (str.startsWith("8210") && str.length() == 12) {
                return "0" + str.substring(2);
            }

            // 82010xxxxxxxx
            if (str.startsWith("82010") && str.length() == 13) {
                return str.substring(2);
            }
        }

        return null;
    }

    @NonNull
    public String getRefinedUserName() {
        return Optional.ofNullable(userName.get())
            .map(String::trim)
            .orElse("");
    }
}
