package com.signkorea.cloud.sample.fragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.lumensoft.ks.KSException;
import com.yettiesoft.cloud.Client;
import com.yettiesoft.cloud.InvalidLicenseException;
import com.signkorea.cloud.sample.MainActivity;
import com.signkorea.cloud.sample.SimpleSharedPreferences;
import com.signkorea.cloud.sample.ViewModelFragment;
import com.signkorea.cloud.sample.databinding.FragmentCertificateHistoryBinding;
import com.signkorea.cloud.sample.databinding.FragmentSecureSettingBinding;
import com.signkorea.cloud.sample.databinding.ItemCertificateHistoryBinding;
import com.signkorea.cloud.sample.utils.OnceRunnable;
import com.signkorea.cloud.sample.viewModels.CertificateHistoryViewModel;
import com.signkorea.cloud.sample.viewModels.SecureSettingViewModel;
import com.yettiesoft.cloud.models.CertificateHistory;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;

import lombok.val;

public class SecureSettingFragment extends ViewModelFragment<FragmentSecureSettingBinding, SecureSettingViewModel> {

    private final OnceRunnable loadData = new OnceRunnable(() -> {
        try {
            getViewModel().init(requireContext().getApplicationContext(), (Client.Delegate)requireActivity());
        } catch (InvalidLicenseException exception) {
            alertException(exception, true);
        }

        getViewModel().loadData(exception -> alertException(exception, true));
    });

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getBinding().startTimePicker.setOnClickListener(v -> {
            TimePickerDialog.OnTimeSetListener listener = (view1, hh, mm) ->
                getViewModel().setServiceStartTime(hh, mm);

            TimePickerDialog timePickerDialog = new TimePickerDialog(
                getActivity(),
                android.R.style.Theme_Holo_Light_Dialog_NoActionBar,
                listener,
                getViewModel().getHour(getViewModel().startTime.get()),
                getViewModel().getMinute(getViewModel().startTime.get()),
                true);
            timePickerDialog.setTitle("이용 시작 시간");
            timePickerDialog.show();
        });

        getBinding().endTimePicker.setOnClickListener(v -> {
            TimePickerDialog.OnTimeSetListener listener = (view1, hh, mm) ->
                getViewModel().setServiceEndTime(hh, mm);

            TimePickerDialog timePickerDialog = new TimePickerDialog(
                getActivity(),
                android.R.style.Theme_Holo_Light_Dialog_NoActionBar,
                listener,
                getViewModel().getHour(getViewModel().endTime.get()),
                getViewModel().getMinute(getViewModel().endTime.get()),
                true);
            timePickerDialog.setTitle("이용 종료 시간");
            timePickerDialog.show();
        });

        Runnable completion = () -> new AlertDialog.Builder(requireContext())
            .setMessage("보안 설정을 저장했습니다.")
            .show();

        getBinding().confirmButton.setOnClickListener(button -> {
            getViewModel().setSecureSetting(completion, this::alertException);
        });
    }

    @Override
    public void onAuthenticationError(int i, CharSequence charSequence) {
        String message = null;
        if (i == KSException.FAILED_CLOUD_BIO_INVALID_PIN) {
            message = charSequence.toString();
        } else {
            message = i + " : " + charSequence;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("생체 인증 실패")
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                })
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();

        loadData.run();
    }

    @Override
    protected void alertException(@NonNull Exception exception) {
        super.alertException(exception);
    }
}
