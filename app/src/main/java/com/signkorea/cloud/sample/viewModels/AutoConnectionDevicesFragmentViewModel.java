package com.signkorea.cloud.sample.viewModels;

import android.content.Context;

import androidx.lifecycle.ViewModel;
import androidx.annotation.Nullable;

import com.yettiesoft.cloud.Client;
import com.yettiesoft.cloud.InvalidLicenseException;
import com.yettiesoft.cloud.models.AutoConnectDevice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import lombok.Getter;
import lombok.NonNull;

public class AutoConnectionDevicesFragmentViewModel extends ViewModel {
    private Client client = null;

    @NonNull
    @Getter
    private List<AutoConnectDevice> devices = new ArrayList<>();

    public AutoConnectionDevicesFragmentViewModel init(Context appContext, Client.Delegate delegate) throws InvalidLicenseException {
        client = new Client().init(appContext).setDelegate(delegate);
        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
    public AutoConnectionDevicesFragmentViewModel loadData(
        Runnable completion,
        Consumer<Exception> onError)
    {
        if (hasValidLicense()) {
            client.getAutoConnectInfo(devices -> {
                this.devices = new ArrayList<>(Arrays.asList(devices));

                completion.run();
            }, onError);
        }

        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
    public AutoConnectionDevicesFragmentViewModel removeItem(
        int index,
        Consumer<Boolean> completion,
        Consumer<Exception> onError)
    {
        if (hasValidLicense()) {
            String deviceId = getDevices().get(index).getDeviceId();

            client.deleteAutoConnect(deviceId, currentDevice -> {
                getDevices().remove(index);
                completion.accept(currentDevice);
            }, onError);
        }

        return this;
    }

    public boolean hasValidLicense() {
        return client != null;
    }
}
