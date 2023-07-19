package com.signkorea.cloud.sample.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.signkorea.cloud.KSCertificateExt;
import com.signkorea.securedata.ProtectedData;

import java.util.Hashtable;
import java.util.function.Consumer;

public class LocalRepository extends Repository {
    @Override
    public void loadCertificates(Runnable onComplete, Consumer<Exception> onError) {
        certMgr.getUserCertificateListLocal(certs -> {
            this.certificates = certs;
            onComplete.run();
        }, onError);
    }

    public void importCertificate(
            @NonNull byte[] certificate,
            @NonNull byte[] key,
            @Nullable byte[] kmCertificate,
            @Nullable byte[] kmKey,
            @NonNull ProtectedData secret,
            @NonNull ProtectedData pin,
            @NonNull Runnable completion,
            @NonNull Consumer<Exception> onError) {
        certMgr.importCertificate(
                certificate, key,
                kmCertificate, kmKey,
                secret, pin,
                completion, onError);
    }

    public void updateCertificateLocal(KSCertificateExt cert,
                                       @NonNull ProtectedData pwd,
                                       @NonNull Consumer<Hashtable<String, Object>> completion) {
        certMgr.updateLocal(cert.getCertificate(),
                cert.getKey(),
                pwd,
                256,
                true,       // 테스트서버: true, 가동서버: false
                completion);
    }

    public static LocalRepository getInstance() {
        return Singleton.INSTANCE;
    }

    private static class Singleton {
        private static final LocalRepository INSTANCE = new LocalRepository();
        private Singleton() {}
    }
}
