package com.signkorea.cloud.sample.models;

import android.content.Context;

import com.signkorea.cloud.KSCertificateExt;
import com.signkorea.cloud.KSCertificateManagerExt;
import com.yettiesoft.cloud.Client;
import com.yettiesoft.cloud.InvalidLicenseException;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public abstract class Repository {
    protected Context context;
    protected KSCertificateManagerExt certMgr;
    protected List<KSCertificateExt> certificates;
    protected AtomicBoolean isBusy = new AtomicBoolean(false);

    public void init(Context context,
                     Client.Delegate clientDelegate,
                     KSCertificateManagerExt.Delegate cmpDelegate) throws InvalidLicenseException {
        this.context = context;
        certMgr = new KSCertificateManagerExt()
                .init(context)
                .setClientDelegate(clientDelegate)
                .setCMPDelegate(cmpDelegate);
    }

    public abstract void loadCertificates(Runnable onComplete, Consumer<Exception> onError);

    public void setViewContext(Context context) {
        this.context = context;
    }

    // region Getters
    public List<KSCertificateExt> getCertificates() { return certificates; }
    public KSCertificateManagerExt getCertMgr() { return certMgr; }
    public boolean isBusy() { return isBusy.get(); }
    // endregion
}
