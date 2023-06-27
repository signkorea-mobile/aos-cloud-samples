package com.signkorea.cloud.sample;

import android.os.Bundle;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.ViewDataBinding;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;

public class DataBindingActivity<BindingT extends ViewDataBinding> extends AppCompatActivity {
    private BindingT binding;

    @SuppressWarnings({"unchecked", "ConstantConditions"})
    public Class<BindingT> getBindingClass() {
        return (Class<BindingT>)((ParameterizedType)getClass().getGenericSuperclass())
                .getActualTypeArguments()[0];
    }

    @SuppressWarnings("unchecked")
    private BindingT inflate(LayoutInflater inflater) {
        try {
            Method inflateMethod = getBindingClass()
                    .getMethod("inflate", LayoutInflater.class);

            return (BindingT)inflateMethod.invoke(null, inflater);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @NonNull
    protected BindingT getBinding() {
        return binding;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = inflate(getLayoutInflater());

        setContentView(binding.getRoot());
    }
}
