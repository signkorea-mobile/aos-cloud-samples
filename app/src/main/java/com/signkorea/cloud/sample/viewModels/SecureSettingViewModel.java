package com.signkorea.cloud.sample.viewModels;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.databinding.ObservableBoolean;
import androidx.databinding.ObservableField;
import androidx.lifecycle.ViewModel;

import com.yettiesoft.cloud.Client;
import com.yettiesoft.cloud.InvalidLicenseException;

import java.util.Optional;
import java.util.function.Consumer;

import lombok.val;

public class SecureSettingViewModel extends ViewModel  {
    private Client client = null;

    public final ObservableBoolean enableServiceTime = new ObservableBoolean();
    public final ObservableField<String> startTime = new ObservableField<>();
    public final ObservableField<String> endTime = new ObservableField<>();
    public final ObservableBoolean enableLocalService = new ObservableBoolean();

    public boolean hasValidLicense() {
        return client != null;
    }

    public SecureSettingViewModel init(Context context, Client.Delegate delegate) throws InvalidLicenseException {
        client = new Client().init(context).setDelegate(delegate);
        return this;
    }

    public void loadData(Consumer<Exception> onError) {
        if (!hasValidLicense()) {
            return;
        }

        client.getSecureSetting(secureSetting -> {
            this.enableServiceTime.set(secureSetting.getServiceTime());
            this.startTime.set(secureSetting.getStartTime());
            this.endTime.set(secureSetting.getEndTime());
            this.enableLocalService.set(secureSetting.getLocalService());
        }, onError);
    }

    public void setServiceStartTime(int startHH, int startMM) {
        this.startTime.set(getRefinedTime(startHH) + ":" + getRefinedTime(startMM));
    }

    public void setServiceEndTime(int endHH, int endMM) {
        this.endTime.set(getRefinedTime(endHH) + ":" + getRefinedTime(endMM));
    }

    @NonNull
    public String getRefinedTime(String time) {
        return Optional.ofNullable(time)
            .map(String::trim)
            .map(s -> s.replaceAll(":", ""))
            .orElse("");
    }

    @NonNull
    public String getRefinedTime(int time) {
        val str = String.valueOf(time);
        if (time < 10) return "0" + str;
        else return str;
    }

    public int getHour(String time) {
        return Integer.parseInt(time.substring(0, 2));
    }

    public int getMinute(String time) {
        return Integer.parseInt(time.substring(3));
    }

    public void setSecureSetting(
        @NonNull Runnable completion,
        @NonNull Consumer<Exception> onError)
    {
        if (!hasValidLicense()) {
            return;
        }

        client.setSecureSetting(
            this.enableLocalService.get(),
            this.enableServiceTime.get(),
            this.getRefinedTime(this.startTime.get()),
            this.getRefinedTime(this.endTime.get()),
            completion,
            onError);
    }
}
