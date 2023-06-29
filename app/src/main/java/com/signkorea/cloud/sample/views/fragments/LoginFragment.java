package com.signkorea.cloud.sample.views.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.Observable;
import androidx.databinding.ObservableField;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;

import com.lumensoft.ks.KSCertificate;
import com.lumensoft.ks.KSException;
import com.lumensoft.ks.KSSign;
import com.signkorea.cloud.Bio;
import com.signkorea.cloud.KSCertificateExt;
import com.signkorea.cloud.KSCertificateManagerExt;
import com.signkorea.cloud.sample.R;
import com.signkorea.cloud.sample.databinding.AlertPasswordBinding;
import com.signkorea.cloud.sample.databinding.FragmentLoginBinding;
import com.signkorea.cloud.sample.enums.CertificateOperation;
import com.signkorea.cloud.sample.enums.SignMenuType;
import com.signkorea.cloud.sample.utils.OnceRunnable;
import com.signkorea.cloud.sample.viewModels.InterFragmentStore;
import com.signkorea.cloud.sample.views.base.DataBindingFragment;
import com.signkorea.securedata.ProtectedData;
import com.signkorea.securedata.SecureData;
import com.yettiesoft.cloud.Client;
import com.yettiesoft.cloud.InvalidLicenseException;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class LoginFragment extends DataBindingFragment<FragmentLoginBinding> implements Bio.Callback{
    @Nullable
    private int certificateIndex = -1;    // 선택된 인증서 index

    private CertificateOperation operation = CertificateOperation.get;
    private SignMenuType menuType;

    private List<KSCertificateExt> certificates = new ArrayList<>();

    private final KSCertificateManagerExt certMgr = new KSCertificateManagerExt();

    private Bio bio = null;

    private final OnceRunnable initClient = new OnceRunnable(() -> {
        Consumer<Exception> onError = e -> alertException(e, menuType.getLabel(), true);

        try {
            certMgr.init(requireContext().getApplicationContext());
        } catch (InvalidLicenseException e) {
            alertException(e, "라이선스 안내", true);
        }

        certMgr.setClientDelegate((Client.Delegate) requireActivity());

        Predicate<KSCertificateExt> certificateFilter = certInfo -> true;

        showLoading();
        certMgr.getUserCertificateListCloud((certificates) -> {
            dismissLoading();

            this.certificates = certificates.stream().filter(certificateFilter).collect(Collectors.toList());

            certificateIndex = LoginFragmentArgs.fromBundle(getArguments()).getCertificateIndex();

            if (certificateIndex >= 0) {
                getBinding().selectdnText.setText(this.certificates.get(certificateIndex).getSubject());

                getBinding().authTypeBtnPin.setVisibility(View.VISIBLE);
                getBinding().authTypeBtnFinger.setVisibility(View.VISIBLE);

                menuType = LoginFragmentArgs.fromBundle(getArguments()).getSignMenuType();
                switch(menuType) {
                    case LOGIN:
                        getBinding().koscomCmsSign.setVisibility(View.VISIBLE);
                        getBinding().koscomCmsSign.setText("로그인 서명");
                        break;

                    case ORDER:
                        getBinding().koscomBriefSign.setVisibility(View.VISIBLE);
                        break;

                    case REGISTER:
                        getBinding().koscomCmsSign.setVisibility(View.VISIBLE);
                        getBinding().getRandom.setVisibility(View.VISIBLE);
                        getBinding().koscomCmsSign.setText("서명 데이터 추출");
                        break;
                }

                if (this.certificates.get(certificateIndex).isCloud()) {
                    String id = this.certificates.get(certificateIndex).getCertInfo().getId();
                    if (bio.isBio(id)) {
                        getBinding().deleteBio.setVisibility(View.VISIBLE);
                    }
                }
            }
        }, onError);
    });

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getInterFragmentStore().entrust(InterFragmentStore.FALLBACK_FRAGMENT_ID, R.id.homeFragment);

        NavDirections directions;
        // 인증서 선택
        directions = LoginFragmentDirections.actionLoginFragmentToCloudCertificateListFragment()
                .setOperation(CertificateOperation.get)
                .setSignMenuType(LoginFragmentArgs.fromBundle(getArguments()).getSignMenuType());
        getBinding().certButton.setOnClickListener(
                Navigation.createNavigateOnClickListener(directions));

        getBinding().koscomCmsSign.setOnClickListener(view1 -> sign(Bio.OPERATION.KOSCOMCMSSIGN));
        getBinding().koscomBriefSign.setOnClickListener(view1 -> sign(Bio.OPERATION.KOSCOMBRIEFSIGN));
        getBinding().getRandom.setOnClickListener(view1 -> sign(Bio.OPERATION.GETRANDOM));
        getBinding().deleteBio.setOnClickListener(view1 -> removeBio());

        initClient.run();

        bio = new Bio(requireActivity(), this.certMgr);
        bio.setCallback(this);
    }

    private void sign(Bio.OPERATION type) {
        if (this.certificates.get(certificateIndex).isCloud()) {
            if(getBinding().authTypeBtnFinger.isChecked())
            {
                String id = this.certificates.get(certificateIndex).getCertInfo().getId();
                if (bio.isBio(id)) {
                    showLoading();
                    bio.getBioCloud(id, type);
                } else {
                    new AlertDialog.Builder(requireActivity())
                            .setTitle("생체 인증 등록")
                            .setMessage("등록된 생체정보가 없습니다.\n등록을 진행 하시겠습니까?")
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                                acquirePassword(true, false, "", pin -> {
                                    showLoading();
                                    bio.addBioCloud(id, new SecureData(pin.getBytes()));
                                });
                            })
                            .setNegativeButton(android.R.string.cancel, null)
                            .show();
                }
            }
            else
            {
                acquirePassword(true, false, "", pin -> {
                    cloudSign(type, new SecureData(pin.getBytes()));
                });
            }

        } else {
            acquirePassword(false, false, "", pin -> {
                localSign(type, new SecureData(pin.getBytes()));
            });
        }
    }

    private void cloudSign(Bio.OPERATION type, ProtectedData encryptedPin){
        Consumer<byte[]> completion = signature -> {
            dismissLoading();
            encryptedPin.clear();
            new AlertDialog.Builder(requireContext())
                    .setTitle("클라우드 전자서명 성공")
                    .setMessage(Base64.encodeToString(signature, Base64.NO_WRAP))
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            Log.d(TAG, Base64.encodeToString(signature, Base64.NO_WRAP));
        };

        Consumer<byte[]> getRandomCompletion = random -> {
            dismissLoading();
            encryptedPin.clear();
            new AlertDialog.Builder(requireContext())
                    .setTitle("R 값 획득 성공")
                    .setMessage(Base64.encodeToString(random, Base64.NO_WRAP))
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            Log.d(TAG, Base64.encodeToString(random, Base64.NO_WRAP));
        };

        Consumer<Exception> onError = exception -> {
            encryptedPin.clear();
            alertException(exception, operation.getLabel(), false);
        };

        byte[] plain = "sign plain".getBytes();

        KSCertificateExt cert = certificates.get(certificateIndex);
        showLoading();
        switch (type) {
            case KOSCOMCMSSIGN:
                certMgr.koscomCMSSign(cert.getId(), plain, encryptedPin, completion, onError);
                break;

            case KOSCOMBRIEFSIGN:
                certMgr.koscomBriefSign(cert.getId(), plain, encryptedPin, completion, onError);
                break;

            case GETRANDOM:
                certMgr.getRandom(cert.getId(), encryptedPin, getRandomCompletion, onError);
                break;

            default:
                assert false: "unknown sign type.";
                break;
        }
    }

    private void localSign (Bio.OPERATION type, ProtectedData encryptedPin) {
        byte[] plain = "sign plain".getBytes();

        KSCertificate cert = this.certificates.get(certificateIndex).cert;
        byte[] signature = null;
        try {
            switch (type) {
                case KOSCOMCMSSIGN:
                    signature = KSSign.sign(KSSign.KOSCOM, cert, plain, encryptedPin);
                    break;

                case KOSCOMBRIEFSIGN:
                    signature = KSSign.sign(KSSign.KOSCOM_BRIEF, cert, plain, encryptedPin);
                    break;

                default:
                    assert false: "unknown sign type.";
                    break;
            }

            new AlertDialog.Builder(requireContext())
                    .setTitle("로컬 전자서명 성공")
                    .setMessage(Base64.encodeToString(signature, Base64.NO_WRAP))
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {})
                    .show();
            Log.d(TAG, Base64.encodeToString(signature, Base64.NO_WRAP));

        } catch (KSException e) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("로컬 전자서명 실패")
                    .setMessage(e.getMessage())
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {})
                    .show();
        } finally {
            encryptedPin.clear();
        }
    }

    private void removeBio() {
        String title = "생체 인증 삭제", message = null;
        String id = this.certificates.get(certificateIndex).getCertInfo().getId();
        if (bio.isBio(id)) {
            bio.removeBioCloud(id);
            message = "등록된 생체 인증을 삭제하였습니다.";
        } else {
            message = "등록된 생체 인증이 없습니다.";
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .setOnDismissListener(dialog -> {
                    getBinding().authTypeBtnPin.setSelected(true);
                    getBinding().deleteBio.setVisibility(View.INVISIBLE);
                })
                .show();
    }

    private void acquirePassword(boolean pinMode, boolean confirmPassword, String initialPassword, Consumer<String> completion) {
        ObservableField<String> pwd1 = new ObservableField<String>();
        ObservableField<String> pwd2 = new ObservableField<String>();

        AlertPasswordBinding binding =
                AlertPasswordBinding.inflate(LayoutInflater.from(requireContext()));

        binding.setPassword1(pwd1);
        binding.setPassword2(pwd2);
        binding.setConfirmPassword(confirmPassword);
        binding.setPinMode(pinMode);

        AlertDialog alert = new AlertDialog.Builder(requireContext())
                .setTitle("전자서명")
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

    @Override
    public void onAuthenticationError(int i, CharSequence charSequence) {
        dismissLoading();

        if (i == KSException.FAILED_CLOUD_BIO_INVALID_PIN) {
            new AlertDialog.Builder(requireActivity())
                    .setTitle("생체 인증 실패")
                    .setMessage("인증서의 PIN이 변경되어 등록된 생체인증을 해지합니다.\n확인을 누르시면 재등록을 진행합니다.")
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        acquirePassword(true, false, "", pin -> {
                            showLoading();
                            bio.addBioCloud(certificates.get(certificateIndex).getId(), new SecureData(pin.getBytes()));
                        });
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        } else {
            new AlertDialog.Builder(requireContext())
                    .setTitle("생체 인증 실패")
                    .setMessage(i + " : " + charSequence)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        }
    }

    @Override
    public void onAuthenticationFailed() {
        dismissLoading();
        new AlertDialog.Builder(requireContext())
                .setTitle("생체 인증 실패")
                .setMessage("onAuthenticationFailed")
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    @Override
    public void onAuthenticationSucceeded(Bio.OPERATION operation, int i, ProtectedData protectedData) {
        if (operation == Bio.OPERATION.REGIST) {
            dismissLoading();
            new AlertDialog.Builder(requireContext())
                    .setTitle("생체 인증 등록 성공")
                    .setPositiveButton(android.R.string.ok, null)
                    .setOnDismissListener(dialog -> getBinding().deleteBio.setVisibility(View.VISIBLE))
                    .show();
        } else {
            cloudSign(operation, protectedData);
        }
    }
}