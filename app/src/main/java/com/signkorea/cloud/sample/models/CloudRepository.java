package com.signkorea.cloud.sample.models;

import androidx.annotation.NonNull;

import com.signkorea.cloud.KSCertificateExt;
import com.signkorea.cloud.sample.utils.SimpleSharedPreferences;
import com.signkorea.securedata.ProtectedData;
import com.yettiesoft.cloud.models.AutoConnectDevice;
import com.yettiesoft.cloud.models.ExportedCertificate;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class CloudRepository extends Repository {
    private KSCertificateExt selectedCert;

    @Override
    public void loadCertificates(Runnable onComplete, Consumer<Exception> onError) {
        certMgr.getUserCertificateListCloud(certs -> {
            this.certificates = certs;

            // 이전에 사용했던 인증서 SubjectDn가 로딩한 인증서 목록에서 존재하는지 확인
            String selectedSubjectDn = SimpleSharedPreferences.getInstance(context).getCertDn();
            selectedCert = this.certificates.stream()
                    .filter(cert -> cert.getSubject().equals(selectedSubjectDn))
                    .findFirst()
                    .orElse(null);

            if (selectedCert == null)
                // 이전에 사용한 인증서가 없는 경우 선택 인증서 정보 초기화
                SimpleSharedPreferences.getInstance(context).edit().certDn("");

            onComplete.run();
        }, e -> {
            onError.accept(e);
        });
    }

    public void selectCert(int index) {
        selectedCert = certificates.get(index);
        SimpleSharedPreferences.getInstance(context).edit().certDn(selectedCert.getSubject()).commit();
    }

    public void exportCertificate(
            String id,
            @NonNull ProtectedData pin,
            @NonNull ProtectedData secret,
            @NonNull BiConsumer<ExportedCertificate, Boolean> completion,
            @NonNull Consumer<Exception> onError) {
        certMgr.exportCertificate(id,
                pin,
                secret,
                (certificates, fromCache) -> {
                    completion.accept(certificates[0], fromCache);
                }, e -> {
                    onError.accept(e);
                });
    }

    public void changeCertificatePin(
            String id,
            @NonNull ProtectedData oldPin,
            @NonNull ProtectedData newPin,
            @NonNull Runnable completion,
            @NonNull Consumer<Exception> onError) {
        certMgr.changePwd(id, oldPin, newPin, completion::run, onError::accept);
    }

    public void deleteCertificate(
            String id,
            @NonNull Runnable completion,
            @NonNull Consumer<Exception> onError) {
        certMgr.deleteCert(id,
                () -> {
                    certificates.removeIf(cert -> Objects.equals(cert.getId(), id));
                    completion.run();
                }, e -> onError.accept(e));
    }

    public void issueCertificate(String refNum, String authCode, Consumer<Hashtable<String, Object>> completion) {
        certMgr.issue(refNum, authCode, 256, true, completion);
    }

    public boolean saveCertificateLocal(ProtectedData pwd) {
        return certMgr.saveCertLocal(pwd);
    }

    public void saveCertificateCloud(ProtectedData pin, Runnable completion, Consumer<Exception> onError) {
        certMgr.saveCloud(pin, completion, onError);
    }

    public void updateCertificateCloud(KSCertificateExt cert,
                                       @NonNull ProtectedData pin,
                                       @NonNull Consumer<Hashtable<String, Object>> completion) {
        certMgr.updateCloud(cert.getId(),
                pin,
                256,
                true,       // 테스트서버: true, 가동서버: false
                completion);
    }

    public void unlockCertificate(
            KSCertificateExt cert,
            @NonNull Runnable completion,
            @NonNull Consumer<Exception> onError)
    {
        certMgr.unlockCertificate(cert.getId(),
                () -> loadCertificates(completion, onError),
                onError);
    }

    public List<KSCertificateExt> getLockedCertificates() {
        return this.certificates.stream().filter(KSCertificateExt::isLock).collect(Collectors.toList());
    }

    public void deleteAccount(Runnable onComplete, Consumer<Exception> onError) {
        certMgr.client.deleteAccount(onComplete, onError);
    }

    public void disconnect(Runnable onComplete, Consumer<Exception> onError) {
        certMgr.client.checkConnect(connected -> {
                if(connected) {
                    certMgr.client.disconnect(() -> {
                        certificates = null;
                        onComplete.run();
                    }, onError);
                }
                else {
                    onError.accept(new RuntimeException("클라우드에 연결되어 있지 않습니다."));
                }
            }, onError);
    }

    public void getAutoConnectDevices(Consumer<List<AutoConnectDevice>> onComplete, Consumer<Exception> onError) {
        certMgr.client.getAutoConnectInfo(devices -> onComplete.accept(Arrays.asList(devices)), onError);
    }

    public void deleteAutoConnectDevice(String deviceId, Consumer<Boolean> completion, Consumer<Exception> onError) {
        certMgr.client.deleteAutoConnect(deviceId, completion, onError);
    }

    // region Getters
    public KSCertificateExt getSelectedCert() { return selectedCert; }
    // endregion


    public static CloudRepository getInstance() {
        return Singleton.INSTANCE;
    }

    private static class Singleton {
        private static final CloudRepository INSTANCE = new CloudRepository();
        private Singleton() {}
    }
}
