package com.signkorea.cloud.sample.viewModels;

import android.annotation.SuppressLint;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModel;

import java.util.HashMap;
import java.util.Map;

public class InterFragmentStore extends ViewModel {
    public static final String MO_ACTION_CONFIRM = "MO AUTH confirm action";
    public static final String MO_ACTION_CANCEL = "MO AUTH cancel action";
    public static final String MO_API_TRANSACTION = "MO API transaction";
    public static final String MO_API_CANCEL = "MO API cancel";
    public static final String FALLBACK_FRAGMENT_ID = "Fallback Fragment ID";   // 네비게이션에 정의된 흐름 외 화면(MO인증 등)에서 취소 발생 시 복귀할 화면 관리

    Map<String, Object> store = new HashMap<>();

    @SuppressLint("DefaultLocale")
    private String getKey(@IdRes int resId, String key) {
        return String.format("%d.%s", resId, key);
    }

    public String getKey(String key) {
        return "common." + key;
    }

    public void entrust(@IdRes int resId, String key, Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null.");
        }

        store.put(getKey(resId, key), value);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T accept(@IdRes int resId, String key) {
        return (T)store.remove(getKey(resId, key));
    }

    public void entrust(String key, Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null.");
        }

        store.put(getKey(key), value);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T peek(String key) {
        return (T)store.get(getKey(key));
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T remove(String key) {
        return (T)store.remove(getKey(key));
    }
}
