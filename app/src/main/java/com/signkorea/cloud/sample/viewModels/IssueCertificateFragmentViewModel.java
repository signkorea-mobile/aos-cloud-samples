package com.signkorea.cloud.sample.viewModels;

import androidx.databinding.ObservableField;
import androidx.lifecycle.ViewModel;

import com.signkorea.cloud.sample.models.CloudRepository;
import com.signkorea.securedata.SecureData;

import java.util.Hashtable;
import java.util.function.Consumer;

public class IssueCertificateFragmentViewModel extends ViewModel {
    public final ObservableField<String> refNum = new ObservableField<>("");  // 참조 번호
    public final ObservableField<String> code = new ObservableField<>("");    // 인가 코드
    public final ObservableField<String> pwd = new ObservableField<>("");     // 인증서 암호

    public IssueCertificateFragmentViewModel init() {
        refNum.set("");
        code.set("");
        pwd.set("1q2w3e4r!!");
        return this;
    }

    public void issue(Consumer<Hashtable<String, Object>> completion) {
        CloudRepository.getInstance().issueCertificate(refNum.get(), code.get(), completion);
    }

    public boolean savePhone() {
        return CloudRepository.getInstance().saveCertificateLocal(new SecureData(pwd.get().getBytes()));
    }

    public void saveCloud(String pin, Runnable completion, Consumer<Exception> onError) {
        CloudRepository.getInstance().saveCertificateCloud(new SecureData(pin.getBytes()), completion, onError);
    }
}
