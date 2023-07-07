package com.signkorea.cloud.sample.views.base;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.ViewDataBinding;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;

import com.lumensoft.ks.KSException;
import com.signkorea.cloud.sample.models.CloudRepository;
import com.signkorea.cloud.sample.viewModels.InterFragmentStore;
import com.signkorea.cloud.sample.views.MainActivity;
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
import java.util.function.Function;

import lombok.val;

public class DataBindingFragment<BindingT extends ViewDataBinding> extends Fragment {
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
        binding = inflate(inflater, container);
        CloudRepository.getInstance().setViewContext(requireContext());

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
        Runnable proceed = () -> {
            getNavController().popBackStack(myDestinationId, popBackStack);

            getInterFragmentStore().remove(InterFragmentStore.MO_ACTION_CANCEL);

            if (completion != null) {
                completion.run();
            }
        };

        val message = exceptionMapper.apply(exception);
        val builder = new AlertDialog.Builder(requireContext())
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> proceed.run())
                .setOnCancelListener(dialog -> proceed.run());

        if (title != null)
            builder.setTitle(title);

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
                    return "PIN 5회 오류로 인해 인증서가 잠겼습니다.";
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
}
