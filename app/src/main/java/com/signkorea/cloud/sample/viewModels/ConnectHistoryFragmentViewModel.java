package com.signkorea.cloud.sample.viewModels;

import android.content.Context;

import androidx.lifecycle.ViewModel;

import com.yettiesoft.cloud.Client;
import com.yettiesoft.cloud.InvalidLicenseException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

import lombok.Getter;
import lombok.NonNull;

public class ConnectHistoryFragmentViewModel extends ViewModel {
    private Client client = null;

    @NonNull
    @Getter
    private List<Date> connectHistory = new ArrayList<>();

    public boolean hasValidLicense() {
        return client != null;
    }

    public ConnectHistoryFragmentViewModel init(Context appContext, Client.Delegate delegate) throws InvalidLicenseException {
        client = new Client().init(appContext).setDelegate(delegate);
        return this;
    }

    public ConnectHistoryFragmentViewModel setClientDelegate(Client.Delegate delegate) {
        if (hasValidLicense()) {
            client.setDelegate(delegate);
        }

        return this;
    }

    public void loadData(Runnable completion, Consumer<Exception> onError) {
        if (!hasValidLicense()) {
            return;
        }

        client.getConnectHistory(dates -> {
            connectHistory = Arrays.asList(dates);
            completion.run();
        }, onError);
    }
}
