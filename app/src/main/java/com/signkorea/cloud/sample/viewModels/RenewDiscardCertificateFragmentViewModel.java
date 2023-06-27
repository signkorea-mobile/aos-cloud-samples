package com.signkorea.cloud.sample.viewModels;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;

import com.yettiesoft.cloud.IncorrectPasscodeException;
import com.signkorea.cloud.sample.databinding.AlertPasswordBinding;
import com.yettiesoft.cloud.models.ExportedCertificate;
import com.yettiesoft.cloud.models.PinFailCount;

import java.util.function.Consumer;
import java.util.Arrays;

import androidx.databinding.Observable;
import androidx.databinding.ObservableField;
import androidx.lifecycle.ViewModel;

import lombok.val;

public class RenewDiscardCertificateFragmentViewModel extends ViewModel {
    public final ObservableField<String> ip = new ObservableField<>();
    public final ObservableField<String> port = new ObservableField<>();
    public final ObservableField<String> caNum = new ObservableField<>();
    private String certificateId = null;

    public void init(String certificateId) {
        // 선택된 인증서 id
        this.certificateId = certificateId;
    }

    public void renewal(Context context) {
        // 갱신 클릭시 호출
        getPassword(context, "인증서 갱신");
    }

    public void discard(Context context) {
        // 폐기 클릭시 호출
        getPassword(context, "인증서 폐기");
    }

    public void getPassword(Context context, String title) {
        acquirePassword(context, title, true, false, "", pin -> {
            Consumer<ExportedCertificate[]> completion = certificates -> new AlertDialog.Builder(context)
                    .setTitle(title)
                    .setMessage(certificates[0].getId())
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {})
                    .show();

            // 인증서 갱신 또는 폐기 관련 함수 호출
        });
    }

    private void alertError(Context context, String title, Exception exception) {
        final String errorMessage;

        if (exception instanceof IncorrectPasscodeException) {
            val failCount = Arrays.stream(((IncorrectPasscodeException) exception).getPinFailCounts())
                .findFirst()
                .map(PinFailCount::getFailed)
                .map(c -> String.format(" [%d/5]", c))
                .orElse("");

            errorMessage = "인증서 PIN이 일치하지 않습니다." + failCount;
        } else {
            errorMessage = exception.toString();
        }

        new AlertDialog.Builder(context)
                .setTitle(title + " 실패")
                .setMessage(errorMessage)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {})
                .show();
    }

    private void acquirePassword(Context context, String title, boolean pinMode, boolean confirmPassword, String initialPassword, Consumer<String> completion) {
        ObservableField<String> pwd1 = new ObservableField<String>();
        ObservableField<String> pwd2 = new ObservableField<String>();

        AlertPasswordBinding binding =
                AlertPasswordBinding.inflate(LayoutInflater.from(context));

        binding.setPassword1(pwd1);
        binding.setPassword2(pwd2);
        binding.setConfirmPassword(confirmPassword);
        binding.setPinMode(pinMode);

        AlertDialog alert = new AlertDialog.Builder(context)
                .setTitle(title)
                .setView(binding.getRoot())
                .setPositiveButton(android.R.string.ok, (dialog, which) -> completion.accept(pwd1.get()))
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> {})
                .create();

        if (confirmPassword) {
            Observable.OnPropertyChangedCallback onPwdChanged = new Observable.OnPropertyChangedCallback() {
                @Override
                public void onPropertyChanged(Observable sender, int propertyId) {
                    String pwd = pwd1.get();
                    boolean ok = pwd != null && pwd.length() > 0 && pwd.equals(pwd2.get());
                    alert.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(ok);
                }
            };
            pwd1.addOnPropertyChangedCallback(onPwdChanged);
            pwd2.addOnPropertyChangedCallback(onPwdChanged);
        } else {
            Observable.OnPropertyChangedCallback onPwdChanged = new Observable.OnPropertyChangedCallback() {
                @Override
                public void onPropertyChanged(Observable sender, int propertyId) {
                    String pwd = pwd1.get();
                    boolean ok = pwd != null && pwd.length() > 0;
                    alert.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(ok);
                }
            };
            pwd1.addOnPropertyChangedCallback(onPwdChanged);
        }

        alert.show();

        pwd1.set(initialPassword);
        pwd2.set(initialPassword);
    }
}
