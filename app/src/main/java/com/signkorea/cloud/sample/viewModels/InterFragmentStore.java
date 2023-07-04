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
    public static final String BILL_ACTION_COMPLETE = "Bill complete action";
    public static final String BILL_ACTION_CANCEL = "Bill cancel action";

    Map<String, Object> store = new HashMap<>();

    @SuppressLint("DefaultLocale")
    private String getKey(@IdRes int resId, String key) {
        return String.format("%d.%s", resId, key);
    }

    public String getKey(String key) {
        return "common." + key;
    }

    public void put(@IdRes int resId, String key, Object value) {       // entrust
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null.");
        }

        store.put(getKey(resId, key), value);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T remove(@IdRes int resId, String key) {             // accept
        return (T)store.remove(getKey(resId, key));
    }

    public void put(String key, Object value) {         // entrust
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null.");
        }

        store.put(getKey(key), value);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T get(String key) {         // peek
        return (T)store.get(getKey(key));
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T remove(String key) {       // remove
        return (T)store.remove(getKey(key));
    }
}
