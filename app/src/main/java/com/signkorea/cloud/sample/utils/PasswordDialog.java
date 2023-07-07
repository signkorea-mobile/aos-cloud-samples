package com.signkorea.cloud.sample.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;

import androidx.annotation.Nullable;
import androidx.databinding.Observable;
import androidx.databinding.ObservableField;

import com.signkorea.cloud.sample.databinding.AlertPasswordBinding;

import java.util.function.Consumer;

public class PasswordDialog {
    public static void show(Context context,
                            String title,
                            boolean pinMode,
                            boolean confirmPassword,
                            String initialPassword,
                            Consumer<String> completion,
                            @Nullable Runnable cancel) {
        ObservableField<String> pwd1 = new ObservableField<String>();
        ObservableField<String> pwd2 = new ObservableField<String>();

        AlertPasswordBinding binding =
                AlertPasswordBinding.inflate(LayoutInflater.from(context));

        binding.setPassword1(pwd1);
        binding.setPassword2(pwd2);
        binding.setConfirmPassword(confirmPassword);
        binding.setPinMode(pinMode);

        AlertDialog alert = new AlertDialog.Builder(context)
                .setTitle(title)
                .setView(binding.getRoot())
                .setPositiveButton(android.R.string.ok, (dialog, which) -> completion.accept(pwd1.get()))
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                    if(cancel != null)
                        cancel.run();
                })
                .setOnCancelListener(dialog -> {
                    if(cancel != null)
                        cancel.run();
                })
                .create();

        if (confirmPassword) {
            Observable.OnPropertyChangedCallback onPwdChanged = new Observable.OnPropertyChangedCallback() {
                @Override
                public void onPropertyChanged(Observable sender, int propertyId) {
                    String pwd = pwd1.get();
                    boolean ok = pwd != null && pwd.length() > 0 && pwd.equals(pwd2.get());
                    alert.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(ok);
                }
            };
            pwd1.addOnPropertyChangedCallback(onPwdChanged);
            pwd2.addOnPropertyChangedCallback(onPwdChanged);
        } else {
            Observable.OnPropertyChangedCallback onPwdChanged = new Observable.OnPropertyChangedCallback() {
                @Override
                public void onPropertyChanged(Observable sender, int propertyId) {
                    String pwd = pwd1.get();
                    boolean ok = pwd != null && pwd.length() > 0;
                    alert.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(ok);
                }
            };
            pwd1.addOnPropertyChangedCallback(onPwdChanged);
        }

        alert.show();

        pwd1.set(initialPassword);
        pwd2.set(initialPassword);
    }
}
