package com.signkorea.cloud.sample.views.base;

import static com.signkorea.cloud.sample.viewModels.InterFragmentStore.BILL_ACTION_CANCEL;
import static com.signkorea.cloud.sample.viewModels.InterFragmentStore.BILL_ACTION_COMPLETE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.Observable;
import androidx.databinding.ObservableField;
import androidx.databinding.ViewDataBinding;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;

import com.lumensoft.ks.KSException;
import com.signkorea.certmanager.BillActivity;
import com.signkorea.cloud.BillInfo;
import com.signkorea.cloud.KSCertificateManagerExt;
import com.signkorea.cloud.sample.databinding.AlertPasswordBinding;
import com.signkorea.cloud.sample.viewModels.InterFragmentStore;
import com.signkorea.cloud.sample.views.MainActivity;
import com.signkorea.securedata.ProtectedData;
import com.signkorea.securedata.SecureData;
import com.yettiesoft.cloud.CancelException;
import com.yettiesoft.cloud.CloudAPIException;
import com.yettiesoft.cloud.IncorrectPasscodeException;
import com.yettiesoft.cloud.InvalidLicenseException;
import com.yettiesoft.cloud.InvalidPinException;
import com.yettiesoft.cloud.NonmemberException;
import com.yettiesoft.cloud.NotCachedCertificateException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import lombok.val;

public class DataBindingFragment<BindingT extends ViewDataBinding> extends Fragment implements KSCertificateManagerExt.Delegate {
    protected final String TAG = getClass().getSimpleName();
    private BindingT binding;
    private InterFragmentStore interFragmentStore;

    @SuppressWarnings({"unchecked", "ConstantConditions"})
    public Class<BindingT> getBindingClass() {
        return (Class<BindingT>)((ParameterizedType)getClass().getGenericSuperclass())
            .getActualTypeArguments()[0];
    }

    @SuppressWarnings("unchecked")
    private BindingT inflate(LayoutInflater inflater, ViewGroup container) {
        try {
            Method inflateMethod = getBindingClass()
                .getMethod("inflate", LayoutInflater.class, ViewGroup.class, boolean.class);

            return (BindingT)inflateMethod.invoke(null, inflater, container, false);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @NonNull
    public BindingT getBinding() {
        return binding;
    }

    @NonNull
    public InterFragmentStore getInterFragmentStore() {
        if (interFragmentStore == null) {
            interFragmentStore = new ViewModelProvider(requireActivity()).get(InterFragmentStore.class);
        }

        return interFragmentStore;
    }

    @Nullable
    @Override
    public View onCreateView(
        @NonNull LayoutInflater inflater,
        @Nullable ViewGroup container,
        @Nullable Bundle savedInstanceState)
    {
        Log.e(TAG, "====onCreateView====");
        binding = inflate(inflater, container);

        return binding.getRoot();
    }

    private int myDestinationId;

    protected NavController getNavController() {
        return ((MainActivity)requireActivity()).getNavController();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //noinspection ConstantConditions
        myDestinationId = getNavController().getCurrentDestination().getId();
    }

    @SuppressLint("NonConstantResourceId")
    protected void alertException(@NonNull Exception exception, @Nullable String title, boolean popBackStack, @Nullable Runnable completion) {
        dismissLoading();
        DialogInterface.OnClickListener onClickListener = (dialog, which) -> {
            getNavController().popBackStack(myDestinationId, popBackStack);

            getInterFragmentStore().remove(InterFragmentStore.MO_ACTION_CANCEL);

            if (completion != null) {
                completion.run();
            }
        };

        val message = exceptionMapper.apply(exception);
        val builder = new AlertDialog.Builder(requireContext())
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, onClickListener);

        if (title != null) {
            builder.setTitle(title);
        }

        builder.show();
    }

    protected void alertException(@NonNull Exception exception, @NonNull String title, boolean popBackStack) {
        alertException(exception, title, popBackStack, null);
    }

    protected void alertException(@NonNull Exception exception, boolean popBackStack) {
        alertException(exception, null, popBackStack, null);
    }

    protected void alertException(@NonNull Exception exception, @NonNull String title) {
        alertException(exception, title, false, null);
    }

    protected void alertException(@NonNull Exception exception) {
        alertException(exception, null, false, null);
    }

    static class ExceptionMapper<R> {
        private final List<Pair<Class<?>, Function<Object, R>>> chain = new LinkedList<>();
        private Function<Exception, R> orElse;

        @SuppressWarnings("unchecked")
        public <T extends Exception> ExceptionMapper<R> is(@NonNull Class<T> cls, @NonNull Function<T, R> fn) {
            chain.add(new Pair<>(cls, o -> fn.apply((T)o)));
            return this;
        }

        public ExceptionMapper<R> orElse(@NonNull Function<Exception, R> orElse) {
            this.orElse = orElse;
            return this;
        }

        @NonNull
        public R apply(@NonNull Exception exception) {
            Class<?> cls = exception.getClass();

            for (val pair: chain) {
                if (pair.first.isAssignableFrom(cls)) {
                    return pair.second.apply(exception);
                }
            }

            return orElse.apply(exception);
        }
    }

    @SuppressLint("DefaultLocale")
    private static final ExceptionMapper<String> exceptionMapper = new ExceptionMapper<String>()
        .is(IncorrectPasscodeException.class, exception -> {
            val failCount = Arrays.stream(exception.getPinFailCounts())
                .findFirst()
//              .map(PinFailCount::getFailed)
                .map(c -> {
                    if (c.isLock()) {
                        return "인증서가 잠겼습니다.";
                    } else {
                        if (c.getFailed() == c.getMaxErrorCount())
                            return "캐시 인증서 PIN 오류로 인해 해당 캐시 인증서 사용이 불가합니다. 클라우드 인증서를 이용해 서명해주세요.";
                        else
                            return String.format("인증서 PIN이 일치하지 않습니다. [%d/%d]", c.getFailed(), c.getMaxErrorCount());
                    }
                })
                .orElse("");

            return failCount;
        })
        .is(NonmemberException.class, ignored -> "공동인증 서비스 회원이 아닙니다.")
        .is(CancelException.class, ignored -> "클라우드 서비스 연결 후 이용해주세요.")
        .is(CloudAPIException.class, exception -> {
            switch (exception.getCode()) {
                case CloudAPIException.PhoneNumberProofIntervalPolicyViolation:
                    return "매크로 방지를 위해 잠시후 다시 시도해 주세요.";
                case CloudAPIException.OldCertificate:
                    return "최신 인증서가 이미 등록되어 있습니다.";
                case CloudAPIException.PinLock:
                    return "핀 5회 오류로 인해 인증서가 잠겼습니다.";
                case CloudAPIException.NotAllowCert:
                    return "Cloud에 등록할 수 없는 인증서입니다.";
                case CloudAPIException.NotAllowTime:
                    return "Cloud 사용 제한 시간입니다.";
                case CloudAPIException.StartEndTimeIsSame:
                    return "보안설정 시작 시간과 종료 시간이 같습니다.";
                case CloudAPIException.NotAllowAge:
                    return "만 14세 이하는 서비스 이용이 불가합니다.";
                case CloudAPIException.NotAllowLocation:
                    return "Cloud에 사용 제한 지역입니다.";
                case CloudAPIException.NotAllowExport:
                    return "Cloud에서 내려받을 수 없는 인증서입니다.";
                case CloudAPIException.FDSDetect:
                case CloudAPIException.FDSDetect + 1:
                case CloudAPIException.FDSDetect + 2:
                case CloudAPIException.FDSDetect + 3:
                case CloudAPIException.FDSDetect + 4:
                case CloudAPIException.FDSDetect + 5:
                case CloudAPIException.FDSDetect + 6:
                case CloudAPIException.FDSDetect + 7:
                case CloudAPIException.FDSDetect + 8:
                case CloudAPIException.FDSDetect + 9:
                    return String.format("Cloud에서 이상거래가 탐지되었습니다.(%d)", exception.getCode());
                default:
                    return String.format("서비스 장애가 발생하였습니다.[%d:%s]", exception.getCode(), exception.getMessage());
            }
        })
        .is(InvalidLicenseException.class, ignored -> "클라우드 라이선스 정보가 유효하지 않습니다.")
        .is(InvalidPinException.class, exception -> {
            switch (exception.getCode()) {
                case InvalidPinException.INVALID_PIN_LENGTH: return "PIN 길이 제한 (6자리)";
                case InvalidPinException.INVALID_PWD_REPEATED_SAME_CHARS: return "같은 숫자 3개이상 존재";
                case InvalidPinException.INVALID_PWD_CONSECUTIVE_LETTERS: return "연속된 숫자가 3개 이상 존재";
                default: return "제한된 PIN 입력 형식";
            }
        })
        .is(NotCachedCertificateException.class, ignored -> "인증서를 캐시에서 찾을 수 없습니다.")
        .is(KSException.class, exception -> {
            switch (exception.errorCode) {
                case KSException.FAILED_CLOUD_NO_CACHED_CERTIFICATE: return "cache된 인증서가 없습니다.";
                case KSException.FAILED_CLOUD_INVALID_CLOUD_CERT: return "cloud에 저장된 인증서가 아닙니다.";
                default: return exception.getMessage();
            }
        })
        .is(SocketTimeoutException.class, ignored -> "서버에서 응답이 없습니다.")
        .orElse(ignored -> "알 수 없는 오류가 발생하였습니다.");

    public void showLoading() {
        ((MainActivity)requireActivity()).showLoading();
    }

    public void dismissLoading() {
        ((MainActivity)requireActivity()).dismissLoading();
    }

    protected void acquirePassword(Context context,
                                   String title,
                                   boolean pinMode,
                                   boolean confirmPassword,
                                   String initialPassword,
                                   Consumer<String> completion,
                                   @Nullable Runnable cancel) {
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
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                    if(cancel != null)
                        cancel.run();
                })
                .setOnCancelListener(dialog -> {
                    if(cancel != null)
                        cancel.run();
                })
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

        // TODO DEBUG
        if(pinMode)
            initialPassword = "121212";
        else
            initialPassword = "1q2w3e4r!!";

        pwd1.set(initialPassword);
        pwd2.set(initialPassword);

    }

    protected void acquirePassword(Context context,
                                   String title,
                                   boolean pinMode,
                                   boolean confirmPassword,
                                   String initialPassword,
                                   Consumer<String> completion) {
        acquirePassword(context, title, pinMode, confirmPassword, initialPassword, completion, () -> dismissLoading());
    }

    // region KSCertificateManagerExt.Delegate 구현부
    // 처리 도중 사용자로부터 비밀번호/PIN을 입력받아야 하는 경우 이벤트 발생
    @Override
    public void requireCode(@NonNull KSCertificateManagerExt.CodeType type,
                            @NonNull Consumer<ProtectedData> code,
                            @NonNull Runnable cancel) {
        boolean isPin;
        String title;
        if(type == KSCertificateManagerExt.CodeType.NUMBER) {
            isPin = true;
            title = "인증서 PIN 입력";
        }
        else {
            isPin = false;
            title = "인증서 비밀번호 입력";
        }

        acquirePassword(requireContext(),
                title,
                isPin,
                true,
                "",
                secret -> {
                    code.accept(new SecureData(secret.getBytes()));
                },
                () -> {
                    dismissLoading();
                    cancel.run();
                });
    }

    // 처리 도중 빌링 처리가 진행되어야 하는 경우 이벤트 발생
    @Override
    public void doBill(@NonNull BillInfo billInfo,
                       @NonNull Runnable complete,
                       @NonNull Runnable cancel) {
        getInterFragmentStore().put(
                BILL_ACTION_COMPLETE,
                complete);

        getInterFragmentStore().put(
                BILL_ACTION_CANCEL,
                cancel);

        Intent intent = new Intent(requireContext(), BillActivity.class);
        intent.putExtra(BillActivity.IS_MAIN_SERVER, false);         // 메인 서버인 경우에는 true로 설정
        intent.putExtra(BillActivity.OPERATION, billInfo.getOperation());   // 갱신인 경우에는 BillActivity.UPDATE 적용

        String arg;
        if (billInfo.getOperation().equalsIgnoreCase(BillActivity.ISSUE))
            arg = BillActivity.REFERENCE;
        else
            arg = BillActivity.SERIAL;

        intent.putExtra(arg, billInfo.getBillArgument());
        startActivityForResult(intent, BillActivity.ID);
    }

    @Override
    public void showStatus(@NonNull KSCertificateManagerExt.Status status,
                           @NonNull String message,
                           @NonNull Runnable complete,
                           @NonNull Runnable cancel) {
        String title;
        switch(status) {
            case SWITCH_ISSUE:
                title = "인증서 발급";
                break;

            case SWITCH_UPDATE:
                title = "인증서 갱신";
                break;

            default:
                title = "";
                assert false: "정의되지 않은 인증서 전환 발급/갱신 프로세스입니다.";
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> complete.run()) // 전환 발급/갱신을 진행
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> cancel.run()) // 전환 발급/갱신을 취소
                .show();

    }
    // endregion
}
