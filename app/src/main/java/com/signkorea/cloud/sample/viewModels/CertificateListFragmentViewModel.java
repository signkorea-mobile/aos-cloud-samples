package com.signkorea.cloud.sample.viewModels;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModel;

import com.signkorea.cloud.Bio;
import com.signkorea.cloud.KSCertificateExt;
import com.signkorea.cloud.sample.enums.DataSource;
import com.signkorea.cloud.sample.models.CloudRepository;
import com.signkorea.cloud.sample.models.LocalRepository;
import com.signkorea.cloud.sample.models.Repository;
import com.signkorea.securedata.ProtectedData;
import com.signkorea.securedata.SecureData;
import com.yettiesoft.cloud.models.ExportedCertificate;

import java.util.Hashtable;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class CertificateListFragmentViewModel extends ViewModel {

    private final CloudRepository cloudRepo = CloudRepository.getInstance();
    private final LocalRepository localRepo = LocalRepository.getInstance();
    private DataSource dataSource;

    private List<KSCertificateExt> certificates;

    public List<KSCertificateExt> getCertificates() { return certificates; }

    public void loadData(DataSource dataSource,
                         Predicate<KSCertificateExt> filter,
                         @NonNull Runnable completion,
                         @NonNull Consumer<Exception> onError) {
        this.dataSource = dataSource;
        Repository repo = (dataSource == DataSource.remote) ? cloudRepo : localRepo;

        Runnable innerComplete = () -> {
            certificates = repo.getCertificates();
            if(certificates != null && filter != null)
                certificates = certificates.stream().filter(filter).collect(Collectors.toList());
            completion.run();
        };

        repo.loadCertificates(innerComplete, onError);
    }

    public void registerCertificate(
        @NonNull byte[] certificate,
        @NonNull byte[] key,
        @Nullable byte[] kmCertificate,
        @Nullable byte[] kmKey,
        @NonNull String secret,
        @NonNull String pin,
        @NonNull Runnable onComplete,
        @NonNull Consumer<Exception> onError)
    {
        ProtectedData encryptedSecret = new SecureData(secret.getBytes());
        ProtectedData encryptedpin = new SecureData(pin.getBytes());

        Runnable innerCompletion = () -> {
            encryptedSecret.clear();
            encryptedpin.clear();
            // 클라우드 보관 후 클라우드 목록 갱신
            cloudRepo.loadCertificates(onComplete, onError);
        };

        Consumer<Exception> innerError = e -> {
            encryptedSecret.clear();
            encryptedpin.clear();
            onError.accept(e);
        };

        localRepo.importCertificate(certificate, key,
                kmCertificate, kmKey,
                encryptedSecret, encryptedpin,
                innerCompletion, innerError);
    }

    public void exportCertificate(
        int index,
        @NonNull String pin,
        @NonNull String secret,
        @NonNull BiConsumer<ExportedCertificate, Boolean> onComplete,
        @NonNull Consumer<Exception> onError)
    {
        KSCertificateExt cert = certificates.get(index);

        ProtectedData encryptedSecret = new SecureData(secret.getBytes());
        ProtectedData encryptedpin = new SecureData(pin.getBytes());

        BiConsumer<ExportedCertificate, Boolean> innerCompletion = (certificate, fromCache) -> {
            encryptedSecret.clear();
            encryptedpin.clear();
            onComplete.accept(certificate, fromCache);
        };

        Consumer<Exception> innerError = e -> {
            encryptedSecret.clear();
            encryptedpin.clear();
            onError.accept(e);
        };

        cloudRepo.exportCertificate(cert.getId(),
                encryptedpin, encryptedSecret,
                innerCompletion, innerError);
    }

    public void changeCertificatePin(
        int index,
        @NonNull String oldPin,
        @NonNull String newPin,
        @NonNull Runnable onComplete,
        @NonNull Consumer<Exception> onError)
    {
        KSCertificateExt cert = certificates.get(index);

        ProtectedData encryptedOldPin = new SecureData(oldPin.getBytes());
        ProtectedData encryptedNewPin = new SecureData(newPin.getBytes());

        Runnable innerCompletion = () -> {
            encryptedOldPin.clear();
            encryptedNewPin.clear();
            onComplete.run();
        };

        Consumer<Exception> innerError = e -> {
            encryptedOldPin.clear();
            encryptedNewPin.clear();
            onError.accept(e);
        };

        cloudRepo.changeCertificatePin(cert.getId(),
                encryptedOldPin, encryptedNewPin,
                innerCompletion, innerError);
    }

    public void deleteCertificate(
        int index,
        @NonNull Runnable onComplete,
        @NonNull Consumer<Exception> onError)
    {
        KSCertificateExt cert = certificates.get(index);

        cloudRepo.deleteCertificate(cert.getId(), () -> {
            certificates = cloudRepo.getCertificates();      // 삭제된 인증서 리스트 반영
            onComplete.run();
        }, onError);
    }

    public void updateCertificate (
        int index,
        @NonNull String secret,
        @NonNull Consumer<Hashtable<String, Object>> onComplete)
    {
        if(dataSource == DataSource.remote) {       // 클라우드 인증서 갱신
            ProtectedData encryptedPin = new SecureData(secret.getBytes());
            KSCertificateExt cert = certificates.get(index);

            new Thread() {
                public void run() {
                    Consumer<Hashtable<String, Object>> innerCompletion = table -> {
                        encryptedPin.clear();
                        onComplete.accept(table);
                    };

                    cloudRepo.updateCertificateCloud(cert,
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
                    onComplete.accept(table);
                };

                public void run() {
                    localRepo.updateCertificateLocal(cert,
                            encryptedPwd,
                            innerCompletion);
                }
            }.start();
        }
    }

    public void unlockCertificate(
        int index,
        @NonNull Runnable onComplete,
        @NonNull Consumer<Exception> onError)
    {
        KSCertificateExt cert = certificates.get(index);

        cloudRepo.unlockCertificate(cert, () -> {
            certificates = cloudRepo.getLockedCertificates();
            onComplete.run();
        }, onError);
    }

    public void registerBio(FragmentActivity activity,
                            String id,
                            ProtectedData pin,
                            Bio.Callback bioCallback) {
        Bio bio = new Bio(activity, cloudRepo.getCertMgr());
        bio.setCallback(bioCallback);

        if(bio.isBio(id))
            bio.removeBioCloud(id);     // 기등록된 생체 인증이 있는 경우 삭제 후 진행

        bio.addBioCloud(id, pin);
    }

    public String getCertIdFromSubjectDn(String dn) {
        try {
            int idx = cloudRepo.getCertMgr().getCertIdxBySubjectDN(dn);
            if(idx >= 0)
                return cloudRepo.getCertificates().get(idx).getId();
            else
                return null;

        } catch(Exception e) {
            return null;
        }
    }
}
