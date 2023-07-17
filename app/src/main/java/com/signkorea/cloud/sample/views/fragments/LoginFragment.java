package com.signkorea.cloud.sample.views.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;

import com.lumensoft.ks.KSException;
import com.lumensoft.ks.KSSign;
import com.signkorea.cloud.Bio;
import com.signkorea.cloud.KSCertificateExt;
import com.signkorea.cloud.sample.databinding.FragmentLoginBinding;
import com.signkorea.cloud.sample.enums.CertificateOperation;
import com.signkorea.cloud.sample.enums.SignMenuType;
import com.signkorea.cloud.sample.models.CloudRepository;
import com.signkorea.cloud.sample.utils.PasswordDialog;
import com.signkorea.cloud.sample.views.base.DataBindingFragment;
import com.signkorea.securedata.ProtectedData;
import com.signkorea.securedata.SecureData;

import java.util.function.Consumer;

public class LoginFragment extends DataBindingFragment<FragmentLoginBinding> implements Bio.Callback{
    private CloudRepository cloudRepo = CloudRepository.getInstance();

    @Nullable
    private KSCertificateExt selectedCert = null;

    private CertificateOperation operation = CertificateOperation.get;
    private SignMenuType menuType;

    private Bio bio = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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

        bio = new Bio(requireActivity(), cloudRepo.getCertMgr());
        bio.setCallback(this);

        // MO에서 복귀한 경우 중복 호출 방지
        // MO에서 복귀한 경우가 아닐 때만 화면/데이터 갱신
        if(getMoReturnDestinationViewId() < 0)
            refresh();
    }

    private void refresh() {
        Runnable refreshUI = () -> {
            navigateToReturnView(false);        // MO 처리 후 복귀한 경우 MO완료 후 대상 화면으로 이동
            dismissLoading();

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
                    getBinding().koscomCmsSign.setText("서명 데이터 생성");
                    break;
            }

            if(selectedCert == null) {
                getBinding().authTypeBtnPin.setVisibility(View.INVISIBLE);
                getBinding().deleteBio.setVisibility(View.INVISIBLE);
            } else {
                getBinding().selectdnText.setText(selectedCert.getSubject());
                getBinding().authTypeBtnPin.setVisibility(View.VISIBLE);
                if(selectedCert.isCloud() && bio.isBio(selectedCert.getId())) {
                    getBinding().deleteBio.setVisibility(View.VISIBLE);
                    getBinding().authTypeBtnFinger.setVisibility(View.VISIBLE);
                }
            }
        };

        Consumer<Exception> onError = e -> {
            menuType = LoginFragmentArgs.fromBundle(getArguments()).getSignMenuType();
            alertException(e, menuType.getLabel(), true);
        };

        selectedCert = cloudRepo.getSelectedCert();

        showLoading();
        if(selectedCert == null) {
            refreshUI.run();
        }
        else {
            cloudRepo.loadCertificates(() -> {
                selectedCert = cloudRepo.getSelectedCert();
                refreshUI.run();
            }, onError);
        }
    }

    private void sign(Bio.OPERATION type) {
        if(selectedCert == null) {
            Toast.makeText(requireContext(), "인증서 선택 후 진행해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        else if(selectedCert.isLock()) {
            Toast.makeText(requireContext(), "잠긴 인증서입니다.\n잠금 해제 후 이용해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedCert.isCloud()) {
            if(getBinding().authTypeBtnFinger.isChecked())
            {
                if (bio.isBio(selectedCert.getId())) {
                    showLoading();
                    bio.getBioCloud(selectedCert.getId(), type);
                } else {
                    new AlertDialog.Builder(requireActivity())
                            .setTitle("생체 인증 등록")
                            .setMessage("등록된 생체정보가 없습니다.\n등록을 진행 하시겠습니까?")
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                                PasswordDialog.show(requireContext(),
                                        operation.getLabel(),
                                        true,
                                        false,
                                        "",
                                        pin -> {
                                            showLoading();
                                            bio.addBioCloud(selectedCert.getId(), new SecureData(pin.getBytes()));
                                        },
                                        this::dismissLoading);
                            })
                            .setNegativeButton(android.R.string.cancel, null)
                            .show();
                }
            }
            else
            {
                PasswordDialog.show(requireContext(),
                        operation.getLabel(),
                        true,
                        false,
                        "",
                        pin -> cloudSign(type, new SecureData(pin.getBytes())),
                        this::dismissLoading);
            }
        } else {
            PasswordDialog.show(requireContext(),
                    operation.getLabel(),
                    false,
                    false,
                    "",
                    pin -> localSign(type, new SecureData(pin.getBytes())),
                    this::dismissLoading);
        }
    }

    private void cloudSign(Bio.OPERATION type, ProtectedData encryptedPin){
        if(selectedCert == null) {
            Toast.makeText(requireContext(), "인증서 선택 후 진행해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

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

        showLoading();
        switch (type) {
            case KOSCOMCMSSIGN:
                cloudRepo.getCertMgr().koscomCMSSign(selectedCert.getId(), plain, encryptedPin, completion, onError);
                break;

            case KOSCOMBRIEFSIGN:
                try {
                    // 로그인 후 세션이 유효한 동안은 캐시를 통해 축약서명을 생성합니다.
                    byte[] sign = cloudRepo.getCertMgr().koscomBriefSignCache(selectedCert.getId(), plain, encryptedPin);
                    completion.accept(sign);
                }
                catch(Exception e) {
                    onError.accept(e);
                }
                break;

            case GETRANDOM:
                cloudRepo.getCertMgr().getRandom(selectedCert.getId(), encryptedPin, getRandomCompletion, onError);
                break;

            default:
                assert false: "unknown sign type.";
                break;
        }
    }

    private void localSign (Bio.OPERATION type, ProtectedData encryptedPin) {
        if(selectedCert == null) {
            Toast.makeText(requireContext(), "인증서 선택 후 진행해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        byte[] plain = "sign plain".getBytes();

        byte[] signature = null;
        try {
            switch (type) {
                case KOSCOMCMSSIGN:
                    signature = KSSign.sign(KSSign.KOSCOM, selectedCert.cert, plain, encryptedPin);
                    break;

                case KOSCOMBRIEFSIGN:
                    signature = KSSign.sign(KSSign.KOSCOM_BRIEF, selectedCert.cert, plain, encryptedPin);
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
        if(selectedCert == null) {
            Toast.makeText(requireContext(), "인증서 선택 후 진행해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        String title = "생체 인증 삭제", message = null;
        String id = selectedCert.getCertInfo().getId();
        if (bio.isBio(id)) {
            bio.removeBioCloud(id);
            getBinding().authTypeBtnFinger.setVisibility(View.INVISIBLE);
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

    @Override
    public void onAuthenticationError(int i, CharSequence charSequence) {
        dismissLoading();

        if (i == KSException.FAILED_CLOUD_BIO_INVALID_PIN) {
            new AlertDialog.Builder(requireActivity())
                    .setTitle("생체 인증 실패")
                    .setMessage("인증서의 PIN이 변경되어 등록된 생체인증을 해지합니다.\n확인을 누르시면 재등록을 진행합니다.")
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        PasswordDialog.show(requireContext(),
                                operation.getLabel(),
                                true,
                                false,
                                "",
                                pin -> {
                                    showLoading();
                                    bio.addBioCloud(selectedCert.getId(), new SecureData(pin.getBytes()));
                                },
                                this::dismissLoading);
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