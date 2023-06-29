package com.signkorea.cloud.sample.viewModels;

import android.content.Context;
import android.widget.Toast;

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

public class CloudCertificateListFragmentViewModel extends ViewModel {
    public enum DataSource {
        remote,
            local,
            cache,
            all
    }

    private final KSCertificateManagerExt certMgr = new KSCertificateManagerExt();

    @Getter
    private List<KSCertificateExt> certificates = new ArrayList<>();

    @Setter @Accessors(chain = true)
    private DataSource dataSource = DataSource.remote;

    public CloudCertificateListFragmentViewModel init(Context context, Client.Delegate delegate) throws InvalidLicenseException {
        certMgr.init(context).setClientDelegate(delegate);
        return this;
    }

    public boolean hasValidLicense() {
        return certMgr != null;
    }

    @SneakyThrows
    public void loadData(@NonNull Context context, @NonNull Runnable completion, @NonNull Consumer<Exception> onError, @NonNull Predicate<KSCertificateExt> certificateFilter) {
        if (!hasValidLicense()) {
            return;
        }

        if (dataSource == DataSource.remote) {
            certMgr.getUserCertificateListCloud((certificates) -> {
                this.certificates = certificates.stream().filter(certificateFilter).collect(Collectors.toList());

                if (this.certificates.size() == 0)
                {
                    Toast.makeText(context.getApplicationContext(), "클라우드에 저장된 인증서가 없습니다.", Toast.LENGTH_SHORT).show();
                }
                completion.run();
            }, onError);
        } else if (dataSource == DataSource.cache){
            certMgr.getCertificateListInfoCache(false, (certificates, cachedDate) -> {
                this.certificates = certificates.stream().filter(certificateFilter).collect(Collectors.toList());

                if (this.certificates.size() == 0) Toast.makeText(context.getApplicationContext(), "cache에 저장된 인증서가 없습니다.", Toast.LENGTH_SHORT).show();
                else Toast.makeText(context.getApplicationContext(), "cache 일자 : " + cachedDate.toString(), Toast.LENGTH_LONG).show();

                completion.run();
            }, onError);
//            new Handler(Looper.getMainLooper()).post(completion);
        } else if (dataSource == DataSource.local){
            certMgr.getUserCertificateListLocal(certificates -> {
                this.certificates = certificates.stream().filter(certificateFilter).collect(Collectors.toList());

                if (this.certificates.size() == 0) Toast.makeText(context.getApplicationContext(), "로컬에 저장된 인증서가 없습니다.", Toast.LENGTH_SHORT).show();

                completion.run();
            }, onError);
//            new Handler(Looper.getMainLooper()).post(completion);
        } else {
            certMgr.getUserCertificateListAll((certificates) -> {
                this.certificates = certificates.stream().filter(certificateFilter).collect(Collectors.toList());

                if (this.certificates.size() == 0) {
                    Toast.makeText(context.getApplicationContext(), "클라우드 + 로컬에 저장된 인증서가 없습니다.", Toast.LENGTH_SHORT).show();
                }
                completion.run();
            }, onError);
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

//    public void getCertificate(
//            int index,
//            @NonNull String pin,
//            @NonNull Consumer<ExportedCertificate> completion,
//            @NonNull Consumer<Exception> onError)
//    {
//        CertificateInfo cert = certificates.get(index);
//
//        Consumer<ExportedCertificate[]> innerCompletion = certificates ->
//                completion.accept(certificates[0]);
//
//        certMgr.getCertificate(new String[] { cert.getId() }, pin, innerCompletion, onError);
//    }

    public String getCertificateId(int index)
    {
        KSCertificateExt cert = certificates.get(index);
        return cert.getId();
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
        @NonNull String pin,
        @NonNull Consumer<Hashtable<String, Object>> completion)
    {
        if (!hasValidLicense()) {
            return;
        }

        ProtectedData encryptedPin = new SecureData(pin.getBytes());

        KSCertificateExt cert = certificates.get(index);

        Consumer<Hashtable<String, Object>> innerCompletion = table -> {
            encryptedPin.clear();
            completion.accept(table);
        };

        new Thread() {
            public void run() {
                certMgr.update(cert.getId(),
                    encryptedPin,
                    true,
                    innerCompletion);
            }
        }.start();
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
