package com.signkorea.cloud.sample.viewModels;

import android.content.Context;

import androidx.databinding.ObservableField;
import androidx.lifecycle.ViewModel;

import com.lumensoft.ks.KSException;
import com.lumensoft.ks.KSX509Util;
import com.signkorea.cloud.KSCertificateManagerExt;
import com.signkorea.securedata.SecureData;
import com.yettiesoft.cloud.Client;
import com.yettiesoft.cloud.InvalidLicenseException;

import java.util.Hashtable;
import java.util.function.Consumer;

public class IssueCertificateFragmentViewModel extends ViewModel {
    public final ObservableField<String> refNum = new ObservableField<>();  // 참조 번호
    public final ObservableField<String> code = new ObservableField<>();    // 인가 코드
    public final ObservableField<String> pwd = new ObservableField<>();     // 인증서 암호

    private KSCertificateManagerExt certMgr = new KSCertificateManagerExt();

    public IssueCertificateFragmentViewModel init(Context context) throws InvalidLicenseException {
        refNum.set("");
        code.set("");
        pwd.set("1q2w3e4r!!");

        certMgr.init(context);
        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
    public IssueCertificateFragmentViewModel setClientDelegate(Client.Delegate delegate) {
        certMgr.setClientDelegate(delegate);
        return this;
    }

    public void issue(Consumer<Hashtable<String, Object>> completion) {
        certMgr.issue(refNum.get(), code.get(), 256, true, completion);
    }

    public boolean savePhone() {
        return certMgr.saveCertLocal(new SecureData(pwd.get().getBytes()));
    }

    public void saveCloud(String pin, Runnable completion, Consumer<Exception> onError) {
        certMgr.saveCloud(new SecureData(pin.getBytes()),
            completion, onError);
    }

    public String getIssuedCertDN () {
        try {
            byte[] bCert = certMgr.getIssuedCert();
            KSX509Util cert = new KSX509Util(bCert);
            return cert.getSubjectDn();
        } catch (KSException e) {
            return null;
        }
    }
}
