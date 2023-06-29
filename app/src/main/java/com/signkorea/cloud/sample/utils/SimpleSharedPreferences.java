package com.signkorea.cloud.sample.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

public class SimpleSharedPreferences {
    public static final String SharedPrefUserName = "userName";
    public static final String SharedPrefPhoneNumber = "phoneNumber";
    public static final String SharedPrefBirthday = "birthday";
    public static final String SharedPrefAuthConnect = "autoConnect";

    private SharedPreferences sharedPref;

    private SimpleSharedPreferences(Context appContext) {
        sharedPref = appContext.getSharedPreferences(
                getClass().getName(),
                Context.MODE_PRIVATE);
    }

    public String getUserName() {
        return sharedPref.getString(SharedPrefUserName, null);
    }

    public String getPhoneNumber() {
        return sharedPref.getString(SharedPrefPhoneNumber, null);
    }

    @SuppressLint("SimpleDateFormat")
    public Date getBirthday() {
        return Optional.ofNullable(sharedPref.getString(SharedPrefBirthday, null))
                .flatMap(str -> {
                    try {
                        return Optional.ofNullable(new SimpleDateFormat("yyyyMMdd").parse(str));
                    } catch (ParseException e) {
                        return Optional.empty();
                    }
                })
                .orElse(null);
    }

    public boolean getAutoConnect() {
        return sharedPref.getBoolean(SharedPrefAuthConnect, true);
    }

    public Editor edit() {
        return new Editor();
    }

    private static SimpleSharedPreferences theInstance;

    public static SimpleSharedPreferences getInstance(Context appContext) {
        if (theInstance == null) {
            synchronized (SimpleSharedPreferences.class) {
                if (theInstance == null) {
                    theInstance = new SimpleSharedPreferences(appContext);
                }
            }
        }
        return theInstance;
    }

    public class Editor {
        private final SharedPreferences.Editor editor;

        @SuppressLint("CommitPrefEdits")
        private Editor() {
            editor = sharedPref.edit();
        }

        public Editor userName(String userName) {
            editor.putString(SharedPrefUserName, userName);
            return this;
        }

        public Editor phoneNumber(String phoneNumber) {
            editor.putString(SharedPrefPhoneNumber, phoneNumber);
            return this;
        }

        public Editor birthday(Date birthday) {
            String birthdayStr = new SimpleDateFormat("yyyyMMdd").format(birthday);
            editor.putString(SharedPrefBirthday, birthdayStr);
            return this;
        }

        public Editor authConnect(boolean autoConnect) {
            editor.putBoolean(SharedPrefAuthConnect, autoConnect);
            return this;
        }

        public void commit() {
            editor.commit();
        }
    }
}
