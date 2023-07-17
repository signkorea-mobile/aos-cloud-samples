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
        isBusy.set(true);
        certMgr.getUserCertificateListCloud(certs -> {
            isBusy.set(false);
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
            isBusy.set(false);
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
        isBusy.set(true);
        certMgr.exportCertificate(id,
                pin,
                secret,
                (certificates, fromCache) -> {
                    isBusy.set(false);
                    completion.accept(certificates[0], fromCache);
                }, e -> {
                    isBusy.set(false);
                    onError.accept(e);
                });
    }

    public void changeCertificatePin(
            String id,
            @NonNull ProtectedData oldPin,
            @NonNull ProtectedData newPin,
            @NonNull Runnable completion,
            @NonNull Consumer<Exception> onError) {
        isBusy.set(true);
        certMgr.changePwd(id, oldPin, newPin, () -> {
            isBusy.set(false);
            completion.run();
        }, e -> {
            isBusy.set(false);
            onError.accept(e);
        });
    }

    public void deleteCertificate(
            String id,
            @NonNull Runnable completion,
            @NonNull Consumer<Exception> onError) {
        isBusy.set(true);
        certMgr.deleteCert(id,
                () -> {
                    isBusy.set(false);
                    certificates.removeIf(cert -> Objects.equals(cert.getId(), id));
                    completion.run();
                }, e -> {
                    isBusy.set(false);
                    onError.accept(e);
                });
    }

    public void issueCertificate(String refNum, String authCode, Consumer<Hashtable<String, Object>> completion) {
        isBusy.set(true);
        certMgr.issue(refNum, authCode, 256, true, c -> {
            isBusy.set(false);
            completion.accept(c);
        });
    }

    public boolean saveCertificateLocal(ProtectedData pwd) {
        return certMgr.saveCertLocal(pwd);
    }

    public void saveCertificateCloud(ProtectedData pin, Runnable completion, Consumer<Exception> onError) {
        isBusy.set(true);
        certMgr.saveCloud(pin, () -> {
            isBusy.set(false);
            completion.run();
        }, e -> {
            isBusy.set(false);
            onError.accept(e);
        });
    }

    public void updateCertificateCloud(KSCertificateExt cert,
                                       @NonNull ProtectedData pin,
                                       @NonNull Consumer<Hashtable<String, Object>> completion) {
        isBusy.set(true);
        certMgr.updateCloud(cert.getId(),
                pin,
                256,
                true,       // 테스트서버: true, 가동서버: false
                c -> {
                    isBusy.set(false);
                    completion.accept(c);
                });
    }

    public void unlockCertificate(
            KSCertificateExt cert,
            @NonNull Runnable completion,
            @NonNull Consumer<Exception> onError)
    {
        isBusy.set(true);
        certMgr.unlockCertificate(cert.getId(),
                () -> {
                    isBusy.set(false);
                    loadCertificates(completion, onError);
                },
                e -> {
                    isBusy.set(false);
                    onError.accept(e);
                });
    }

    public List<KSCertificateExt> getLockedCertificates() {
        return this.certificates.stream().filter(KSCertificateExt::isLock).collect(Collectors.toList());
    }

    public void deleteAccount(Runnable onComplete, Consumer<Exception> onError) {
        isBusy.set(true);
        certMgr.client.deleteAccount(() -> {
            isBusy.set(false);
            onComplete.run();
        }, e -> {
            isBusy.set(false);
            onError.accept(e);
        });
    }

    public void disconnect(Runnable onComplete, Consumer<Exception> onError) {
        isBusy.set(true);
        certMgr.client.checkConnect(connected -> {
                if(connected) {
                    certMgr.client.disconnect(() -> {
                        isBusy.set(false);
                        certificates = null;
                        onComplete.run();
                    }, e -> {
                        isBusy.set(false);
                        onError.accept(e);
                    });
                }
                else {
                    isBusy.set(false);
                    onError.accept(new RuntimeException("클라우드에 연결되어 있지 않습니다."));
                }
            }, e -> {
                isBusy.set(false);
                onError.accept(e);
        });
    }

    public void getAutoConnectDevices(Consumer<List<AutoConnectDevice>> onComplete, Consumer<Exception> onError) {
        isBusy.set(true);
        certMgr.client.getAutoConnectInfo(devices -> {
            isBusy.set(false);
            onComplete.accept(Arrays.asList(devices));
        }, e -> {
            isBusy.set(false);
            onError.accept(e);
        });
    }

    public void deleteAutoConnectDevice(String deviceId, Consumer<Boolean> completion, Consumer<Exception> onError) {
        isBusy.set(true);
        certMgr.client.deleteAutoConnect(deviceId, b -> {
            isBusy.set(false);
            completion.accept(b);
        }, e -> {
            isBusy.set(false);
            onError.accept(e);
        });
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
