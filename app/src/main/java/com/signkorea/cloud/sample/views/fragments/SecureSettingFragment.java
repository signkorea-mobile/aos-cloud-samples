package com.signkorea.cloud.sample.views.fragments;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.signkorea.cloud.sample.databinding.FragmentSecureSettingBinding;
import com.signkorea.cloud.sample.utils.OnceRunnable;
import com.signkorea.cloud.sample.viewModels.SecureSettingViewModel;
import com.signkorea.cloud.sample.views.base.ViewModelFragment;

public class SecureSettingFragment extends ViewModelFragment<FragmentSecureSettingBinding, SecureSettingViewModel> {

    private final OnceRunnable loadData = new OnceRunnable(() -> {
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

        Runnable completion = () -> {
            dismissLoading();
            new AlertDialog.Builder(requireContext())
                    .setMessage("보안 설정을 저장했습니다.")
                    .show();
        };

        getBinding().confirmButton.setOnClickListener(button -> {
            showLoading();
            getViewModel().setSecureSetting(completion, this::alertException);
        });
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
