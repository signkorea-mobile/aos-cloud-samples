package com.signkorea.cloud.sample.viewModels;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModel;

import com.signkorea.cloud.KSCertificateExt;
import com.signkorea.cloud.sample.models.CloudRepository;
import com.signkorea.securedata.ProtectedData;
import com.signkorea.securedata.SecureData;
import com.yettiesoft.cloud.models.ExportedCertificate;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.val;

public class CertificateListFragmentViewModel extends ViewModel {
    private CloudRepository repo = CloudRepository.getInstance();

    private List<KSCertificateExt> certificates;

    public List<KSCertificateExt> getCertificates() { return certificates; }

    @SneakyThrows
    public void loadData(CloudRepository.DataSource dataSource,
                         Predicate<KSCertificateExt> filter,
                         @NonNull Runnable completion,
                         @NonNull Consumer<Exception> onError) {
        loadData(dataSource, filter, false, completion, onError);
    }

    public void loadData(CloudRepository.DataSource dataSource,
                         Predicate<KSCertificateExt> filter,
                         boolean forceRun,                      // 인증서 목록을 항상 다시 로딩해야 하는 경우 (인증서 갱신 등)
                         @NonNull Runnable completion,
                         @NonNull Consumer<Exception> onError) {
        Runnable innerComplete = () -> {
            if (repo.getCertificates() == null)
                Log.e("AA", "aa");

            certificates = new ArrayList(repo.getCertificates());
            if(filter != null)
                certificates = certificates.stream().filter(filter).collect(Collectors.toList());
            completion.run();
        };

        // 이전 로딩한 인증서 데이터 소스와 다른 경우 재로딩
        if(dataSource == repo.getDataSource() && !forceRun) {
            innerComplete.run();
            return;
        }

        repo.loadCertificates(dataSource, innerComplete, onError);

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
        assert repo.getDataSource() == CloudRepository.DataSource.local;

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

        repo.importCertificate(certificate, key, kmCertificate, kmKey, encryptedSecret, encryptedpin, innerCompletion, innerError);
    }

    public void exportCertificate(
        int index,
        @NonNull String pin,
        @NonNull String secret,
        @NonNull BiConsumer<ExportedCertificate, Boolean> completion,
        @NonNull Consumer<Exception> onError)
    {
        KSCertificateExt cert = certificates.get(index);

        ProtectedData encryptedSecret = new SecureData(secret.getBytes());
        ProtectedData encryptedpin = new SecureData(pin.getBytes());

        BiConsumer<ExportedCertificate, Boolean> innerCompletion = (certificate, fromCache) -> {
            encryptedSecret.clear();
            encryptedpin.clear();
            completion.accept(certificate, fromCache);
        };

        Consumer<Exception> innerError = e -> {
            encryptedSecret.clear();
            encryptedpin.clear();
            onError.accept(e);
        };

        repo.exportCertificate(cert.getId(), encryptedpin, encryptedSecret, innerCompletion, innerError);
    }

    public void changeCertificatePin(
        int index,
        @NonNull String oldPin,
        @NonNull String newPin,
        @NonNull Runnable completion,
        @NonNull Consumer<Exception> onError)
    {
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

        repo.changeCertificatePin(cert.getId(), encryptedOldPin, encryptedNewPin, innerCompletion, innerError);
    }

    public void deleteCertificate(
        int index,
        @NonNull Runnable completion,
        @NonNull Consumer<Exception> onError)
    {
        KSCertificateExt cert = certificates.get(index);

        repo.deleteCertificate(cert.getId(), () -> {
            certificates = repo.getCertificates();      // 삭제된 인증서 리스트 반영
            completion.run();
        }, onError);
    }

    public void updateCertificate (
        int index,
        @NonNull String secret,
        @NonNull Consumer<Hashtable<String, Object>> completion)
    {
        if(repo.getDataSource() == CloudRepository.DataSource.remote) {       // 클라우드 인증서 갱신
            ProtectedData encryptedPin = new SecureData(secret.getBytes());
            KSCertificateExt cert = certificates.get(index);

            new Thread() {
                public void run() {
                    Consumer<Hashtable<String, Object>> innerCompletion = table -> {
                        encryptedPin.clear();
                        completion.accept(table);
                    };

                    repo.updateCertificateCloud(cert,
                        encryptedPin,
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
                    repo.updateCertificateLocal(cert,
                            encryptedPwd,
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
        val cert = certificates.get(index);

        repo.unlockCertificate(cert, () -> {
            certificates = repo.getLockedCertificates();
            completion.run();
        }, onError);
    }
}
