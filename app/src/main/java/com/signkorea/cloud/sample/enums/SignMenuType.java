package com.signkorea.cloud.sample.enums;

import androidx.annotation.NonNull;

public enum SignMenuType {
    LOGIN,
    ORDER,
    REGISTER;

    public String getLabel() {
        switch (this) {
            case LOGIN:
                return "로그인";
            case ORDER:
                return "주문";
            case REGISTER:
                return "타기관 인증서 등록";
        }
        return "";
    }

    @NonNull
    @Override
    public String toString() {
        return getLabel();
    }
}
