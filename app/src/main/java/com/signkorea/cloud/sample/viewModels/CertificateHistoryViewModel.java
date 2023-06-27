package com.signkorea.cloud.sample.viewModels;

import android.content.Context;

import androidx.lifecycle.ViewModel;

import com.yettiesoft.cloud.Client;
import com.yettiesoft.cloud.InvalidLicenseException;
import com.yettiesoft.cloud.models.CertificateHistory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import lombok.Getter;
import lombok.NonNull;

public class CertificateHistoryViewModel extends ViewModel {
    private Client client = null;

    @NonNull
    @Getter
    private List<CertificateHistory> certificateHistory = new ArrayList<>();

    public boolean hasValidLicense() {
        return client != null;
    }

    public CertificateHistoryViewModel init(Context appContext, Client.Delegate delegate) throws InvalidLicenseException {
        client = new Client().init(appContext).setDelegate(delegate);
        return this;
    }

    public void loadData(Runnable completion, Consumer<Exception> onError) {
        if (!hasValidLicense()) {
            return;
        }

        client.getCertificateHistory(history -> {
            certificateHistory = Arrays.asList(history);
            completion.run();
        }, onError);
    }
}
