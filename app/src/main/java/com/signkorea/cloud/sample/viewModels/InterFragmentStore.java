package com.signkorea.cloud.sample.viewModels;

import android.annotation.SuppressLint;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModel;

import java.util.HashMap;
import java.util.Map;

public class InterFragmentStore extends ViewModel {
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

    public void remove(String key) {
        store.remove(getKey(key));
    }
}
