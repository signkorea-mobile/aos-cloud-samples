package com.signkorea.cloud.sample.viewModels;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModel;

import com.signkorea.cloud.KSCertificateExt;
import com.signkorea.cloud.KSCertificateManagerExt;
import com.signkorea.securedata.ProtectedData;
import com.signkorea.securedata.SecureData;
import com.yettiesoft.cloud.Client;
import com.yettiesoft.cloud.InvalidLicenseException;
import com.yettiesoft.cloud.models.ExportedCertificate;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import lombok.val;

public class CertificateListFragmentViewModel extends ViewModel {
    public enum DataSource {
        remote,
            local
    }

    private KSCertificateManagerExt certMgr = new KSCertificateManagerExt();

    @Getter
    private List<KSCertificateExt> certificates = new ArrayList<>();

    @Setter @Accessors(chain = true)
    private DataSource dataSource = DataSource.remote;

    public CertificateListFragmentViewModel init(Context context,
                                                 Client.Delegate delegate,
                                                 KSCertificateManagerExt.Delegate cmpDelegate) throws InvalidLicenseException {
        try {
            certMgr.init(context)
                    .setClientDelegate(delegate)
                    .setCMPDelegate(cmpDelegate);
        }
        catch(Exception e) {
            certMgr = null;
            throw e;
        }
        return this;
    }

    public boolean hasValidLicense() {
        return certMgr != null;
    }

    @SneakyThrows
    public void loadData(@NonNull Runnable completion,
                         @NonNull Consumer<Exception> onError,
                         Predicate<KSCertificateExt> certificateFilter) {
        if (!hasValidLicense()) {
            return;
        }

        final Predicate<KSCertificateExt> filter = (certificateFilter != null) ? certificateFilter : certInfo -> true;
        if (dataSource == DataSource.remote) {
            certMgr.getUserCertificateListCloud((certificates) -> {
                this.certificates = certificates.stream().filter(filter).collect(Collectors.toList());
                completion.run();
            }, onError);
        } else if (dataSource == DataSource.local){
            certMgr.getUserCertificateListLocal(certificates -> {
                this.certificates = certificates.stream().filter(filter).collect(Collectors.toList());
                completion.run();
            }, onError);
        } else {
            assert false:"정의되지 않은 인증서 데이터 소스입니다.";
        }
    }

    public void registerCertificate(
        @NonNull byte[] certificate,
        @NonNull byte[] key,
        @Nullable byte[] kmCertificate,
        @Nullable byte[] kmKey,
        @NonNull String secret,
        @NonNull String pin,
        @NonNull Runnable completion,
        @NonNull Consumer<Exception> onError)
    {
        if (!hasValidLicense()) {
            return;
        }

        assert dataSource == DataSource.local;

        ProtectedData encryptedSecret = new SecureData(secret.getBytes());
        ProtectedData encryptedpin = new SecureData(pin.getBytes());

        Runnable innerCompletion = () -> {
            encryptedSecret.clear();
            encryptedpin.clear();
            completion.run();
        };

        Consumer<Exception> innerError = e -> {
            encryptedSecret.clear();
            encryptedpin.clear();
            onError.accept(e);
        };

        certMgr.importCertificate(certificate, key, kmCertificate, kmKey, encryptedSecret, encryptedpin, innerCompletion, innerError);
    }

    public void exportCertificate(
        int index,
        @NonNull String pin,
        @NonNull String secret,
        @NonNull BiConsumer<ExportedCertificate, Boolean> completion,
        @NonNull Consumer<Exception> onError)
    {
        if (!hasValidLicense()) {
            return;
        }

        KSCertificateExt cert = certificates.get(index);

        ProtectedData encryptedSecret = new SecureData(secret.getBytes());
        ProtectedData encryptedpin = new SecureData(pin.getBytes());

        BiConsumer<ExportedCertificate[], Boolean> innerCompletion = (certificates, fromCache) -> {
            encryptedSecret.clear();
            encryptedpin.clear();
            completion.accept(certificates[0], fromCache);
        };

        Consumer<Exception> innerError = e -> {
            encryptedSecret.clear();
            encryptedpin.clear();
            onError.accept(e);
        };

        certMgr.exportCertificate(cert.getId(), encryptedpin, encryptedSecret, innerCompletion, innerError);
    }

    public void changeCertificatePin(
        int index,
        @NonNull String oldPin,
        @NonNull String newPin,
        @NonNull Runnable completion,
        @NonNull Consumer<Exception> onError)
    {
        if (!hasValidLicense()) {
            return;
        }

        KSCertificateExt cert = certificates.get(index);

        ProtectedData encryptedOldPin = new SecureData(oldPin.getBytes());
        ProtectedData encryptedNewPin = new SecureData(newPin.getBytes());

        Runnable innerCompletion = () -> {
            encryptedOldPin.clear();
            encryptedNewPin.clear();
            completion.run();
        };

        Consumer<Exception> innerError = e -> {
            encryptedOldPin.clear();
            encryptedNewPin.clear();
            onError.accept(e);
        };

        certMgr.changePwd(cert.getId(), encryptedOldPin, encryptedNewPin, innerCompletion, innerError);
    }

    public void deleteCertificate(
        int index,
        @NonNull Runnable completion,
        @NonNull Consumer<Exception> onError)
    {
        if (!hasValidLicense()) {
            return;
        }

        KSCertificateExt cert = certificates.get(index);

        certMgr.deleteCert(cert.getId(), () -> {
            certificates.remove(index);
            completion.run();
        }, onError);
    }

    public void updateCertificate (
        int index,
        @NonNull String secret,
        @NonNull Consumer<Hashtable<String, Object>> completion)
    {
        if (!hasValidLicense()) {
            return;
        }

        if(dataSource == DataSource.remote) {       // 클라우드 인증서 갱신
            ProtectedData encryptedPin = new SecureData(secret.getBytes());
            KSCertificateExt cert = certificates.get(index);

            new Thread() {
                public void run() {
                    Consumer<Hashtable<String, Object>> innerCompletion = table -> {
                        encryptedPin.clear();
                        completion.accept(table);
                    };

                    certMgr.updateCloud(cert.getId(),
                        encryptedPin,
                        256,
                        true,
                        innerCompletion);
                }
            }.start();
        }
        else {      // 로컬 인증서 갱신
            ProtectedData encryptedPwd = new SecureData(secret.getBytes());
            KSCertificateExt cert = certificates.get(index);

            new Thread() {
                Consumer<Hashtable<String, Object>> innerCompletion = table -> {
                    encryptedPwd.clear();
                    completion.accept(table);
                };

                public void run() {
                    certMgr.updateLocal(cert.getCertificate(),
                            cert.getKey(),
                            encryptedPwd,
                            256,
                            true,       // 테스트서버: true, 가동서버: false
                            innerCompletion);
                }
            }.start();
        }
    }

    public void unlockCertificate(
        int index,
        @NonNull Runnable completion,
        @NonNull Consumer<Exception> onError)
    {
        if (!hasValidLicense()) {
            return;
        }

        val cert = certificates.get(index);

        certMgr.unlockCertificate(cert.getId(), () -> {
            certificates.remove(index);
            completion.run();
        }, onError);
    }
}
