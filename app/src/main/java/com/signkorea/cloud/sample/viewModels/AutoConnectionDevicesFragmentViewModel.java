package com.signkorea.cloud.sample.viewModels;

import androidx.lifecycle.ViewModel;

import com.signkorea.cloud.sample.models.CloudRepository;
import com.yettiesoft.cloud.models.AutoConnectDevice;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import lombok.Getter;

public class AutoConnectionDevicesFragmentViewModel extends ViewModel {
    private CloudRepository repo = CloudRepository.getInstance();

    @Getter
    private List<AutoConnectDevice> devices = null;

    @SuppressWarnings("UnusedReturnValue")
    public AutoConnectionDevicesFragmentViewModel loadData(
        Runnable completion,
        Consumer<Exception> onError)
    {
        CloudRepository.getInstance().getAutoConnectDevices(devices -> {
            this.devices = new ArrayList<>(devices);
            completion.run();
        }, onError);

        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
    public AutoConnectionDevicesFragmentViewModel removeItem(
        int index,
        Consumer<Boolean> completion,
        Consumer<Exception> onError)
    {
        String deviceId = devices.get(index).getDeviceId();
        repo.deleteAutoConnectDevice(deviceId, currentDevice -> {
            getDevices().remove(index);
            completion.accept(currentDevice);
        }, onError);

        return this;
    }

    public boolean isCurrentDevice(int index) {
        return Objects.equals(devices.get(index).getDeviceId(), repo.getCertMgr().client.getCurrentDeviceId());
    }
}
