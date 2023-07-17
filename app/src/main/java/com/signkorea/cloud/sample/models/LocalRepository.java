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
        isBusy.set(true);
        certMgr.getUserCertificateListLocal(certs -> {
            isBusy.set(false);
            this.certificates = certs;
            onComplete.run();
        }, e -> {
            isBusy.set(false);
            onError.accept(e);
        });
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
        isBusy.set(true);
        certMgr.importCertificate(certificate, key, kmCertificate, kmKey, secret, pin, () -> {
            isBusy.set(false);
            completion.run();
        }, e -> {
            isBusy.set(false);
            onError.accept(e);
        });
    }

    public void updateCertificateLocal(KSCertificateExt cert,
                                       @NonNull ProtectedData pwd,
                                       @NonNull Consumer<Hashtable<String, Object>> completion) {
        isBusy.set(true);
        certMgr.updateLocal(cert.getCertificate(),
                cert.getKey(),
                pwd,
                256,
                true,       // 테스트서버: true, 가동서버: false
                c -> {
                    isBusy.set(false);
                    completion.accept(c);
                });
    }

    public static LocalRepository getInstance() {
        return Singleton.INSTANCE;
    }

    private static class Singleton {
        private static final LocalRepository INSTANCE = new LocalRepository();
        private Singleton() {}
    }
}
