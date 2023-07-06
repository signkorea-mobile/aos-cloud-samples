package com.signkorea.cloud.sample.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

public class SimpleSharedPreferences {
    private final String USER_NAME = "userName";
    private final String PHONE_NUMBER = "phoneNumber";
    private final String BIRTHDAY = "birthday";
    private final String CERT_DN = "certDn";

    private SharedPreferences sharedPref;

    private SimpleSharedPreferences(Context appContext) {
        sharedPref = appContext.getSharedPreferences(
                getClass().getName(),
                Context.MODE_PRIVATE);
    }

    public String getUserName() {
        return sharedPref.getString(USER_NAME, null);
    }

    public String getPhoneNumber() {
        return sharedPref.getString(PHONE_NUMBER, null);
    }

    @SuppressLint("SimpleDateFormat")
    public Date getBirthday() {
        return Optional.ofNullable(sharedPref.getString(BIRTHDAY, null))
                .flatMap(str -> {
                    try {
                        return Optional.ofNullable(new SimpleDateFormat("yyyyMMdd").parse(str));
                    } catch (ParseException e) {
                        return Optional.empty();
                    }
                })
                .orElse(null);
    }

    public String getCertDn() {
        return sharedPref.getString(CERT_DN, "");
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
            editor.putString(USER_NAME, userName);
            return this;
        }

        public Editor phoneNumber(String phoneNumber) {
            editor.putString(PHONE_NUMBER, phoneNumber);
            return this;
        }

        public Editor birthday(Date birthday) {
            String birthdayStr = new SimpleDateFormat("yyyyMMdd").format(birthday);
            editor.putString(BIRTHDAY, birthdayStr);
            return this;
        }

        public Editor certDn(String dn) {
            editor.putString(CERT_DN, dn);
            return this;
        }

        public void commit() {
            editor.commit();
        }
    }
}
