package com.signkorea.cloud.sample.viewModels;

import android.content.Context;

import androidx.lifecycle.ViewModel;

import com.yettiesoft.cloud.Client;
import com.yettiesoft.cloud.InvalidLicenseException;
import com.yettiesoft.cloud.models.UserHistory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import lombok.Getter;
import lombok.NonNull;

public class UserHistoryViewModel extends ViewModel {
    private Client client = null;

    @NonNull
    @Getter
    private List<UserHistory> userHistory = new ArrayList<>();

    public boolean hasValidLicense() {
        return client != null;
    }

    public UserHistoryViewModel init(Context appContext, Client.Delegate delegate) throws InvalidLicenseException {
        client = new Client().init(appContext).setDelegate(delegate);
        return this;
    }

    public void loadData(Runnable completion, Consumer<Exception> onError) {
        if (!hasValidLicense()) {
            return;
        }

        client.getUserHistory(history -> {
            userHistory = Arrays.asList(history);
            completion.run();
        }, onError);
    }
}
