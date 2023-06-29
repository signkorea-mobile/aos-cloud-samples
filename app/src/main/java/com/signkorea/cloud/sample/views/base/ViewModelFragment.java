package com.signkorea.cloud.sample.views.base;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.ViewDataBinding;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.signkorea.cloud.sample.views.base.DataBindingFragment;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;

public abstract class ViewModelFragment<BindingT extends ViewDataBinding, ViewModelT extends ViewModel>
        extends DataBindingFragment<BindingT>
{
    private ViewModelT viewModel;

    public ViewModelT getViewModel() {
        return viewModel;
    }

    @SuppressWarnings({"unchecked", "ConstantConditions"})
    public Class<ViewModelT> getViewModelClass() {
        return (Class<ViewModelT>)((ParameterizedType)getClass().getGenericSuperclass())
                .getActualTypeArguments()[1];
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Class<ViewModelT> viewModelType = getViewModelClass();

        viewModel = new ViewModelProvider(this).get(viewModelType);

        try {
            Method modelSetter = getBindingClass().getMethod("setViewModel", viewModelType);
            modelSetter.invoke(getBinding(), viewModel);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
        }
    }
}
