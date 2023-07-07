package com.signkorea.cloud.sample.views;

import static com.signkorea.cloud.sample.viewModels.InterFragmentStore.BILL_ACTION_CANCEL;
import static com.signkorea.cloud.sample.viewModels.InterFragmentStore.BILL_ACTION_COMPLETE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.signkorea.certmanager.BillActivity;
import com.signkorea.cloud.BillInfo;
import com.signkorea.cloud.KSCertificateManagerExt;
import com.signkorea.cloud.sample.BuildConfig;
import com.signkorea.cloud.sample.R;
import com.signkorea.cloud.sample.databinding.ActivityMainBinding;
import com.signkorea.cloud.sample.enums.CertificateOperation;
import com.signkorea.cloud.sample.models.CloudRepository;
import com.signkorea.cloud.sample.utils.PasswordDialog;
import com.signkorea.cloud.sample.viewModels.InterFragmentStore;
import com.signkorea.cloud.sample.views.base.DataBindingActivity;
import com.signkorea.cloud.sample.views.fragments.CloudCertificateListFragmentArgs;
import com.signkorea.cloud.sample.views.fragments.LocalCertificateListFragmentArgs;
import com.signkorea.securedata.ProtectedData;
import com.signkorea.securedata.SecureData;
import com.yettiesoft.cloud.AcknowledgeConditionsOfUseReason;
import com.yettiesoft.cloud.AcquiredUserInfo;
import com.yettiesoft.cloud.Client;
import com.yettiesoft.cloud.PhoneNumberProofMethod;
import com.yettiesoft.cloud.PhoneNumberProofTransaction;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import lombok.Getter;
import lombok.val;

public class MainActivity extends DataBindingActivity<ActivityMainBinding> implements Client.Delegate, KSCertificateManagerExt.Delegate {
    private InterFragmentStore interFragmentStore;
    private AppBarConfiguration appBarConfiguration;
    private Dialog loadingPopup;

    @Getter
    private CloudRepository cloudRepository;

    @Override
    @SuppressLint("NonConstantResourceId")
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        interFragmentStore = new ViewModelProvider(this).get(InterFragmentStore.class);

        try {
            CloudRepository.getInstance().init(this, this, this);
        }
        catch(Exception e) {
            new AlertDialog.Builder(this)
                    .setTitle("라이선스 오류")
                    .setMessage("라이선스를 확인 후 재시도해주세요.\n앱을 종료합니다.")
                    .setPositiveButton("확인", (d, i) -> finish())
                    .setCancelable(false)
                    .show();
            return;
        }

        Fragment navHostFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        @SuppressWarnings("ConstantConditions")
        NavController navController = ((NavHostFragment)navHostFragment).getNavController();
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (destination.getId() == R.id.homeFragment || destination.getId() == R.id.loginFragment ||
                    destination.getId() == R.id.certificateManagementFragment || destination.getId() == R.id.accountManagementFragment) {
                getSupportActionBar().show();
            }

            if (destination.getId() == R.id.localCertificateListFragment) {
                Optional.ofNullable(getSupportActionBar()).ifPresent(actionBar -> {
                    CertificateOperation operation = LocalCertificateListFragmentArgs.fromBundle(arguments).getOperation();
                    actionBar.setTitle(operation.getLabel());
                });
            }
            else if (destination.getId() == R.id.cloudCertificateListFragment) {
                Optional.ofNullable(getSupportActionBar()).ifPresent(actionBar -> {
                    CertificateOperation operation = CloudCertificateListFragmentArgs.fromBundle(arguments).getOperation();
                    actionBar.setTitle(operation.getLabel());
                });
            }
        });

        loadingPopup = new Dialog(this);
        loadingPopup.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        loadingPopup.setContentView(new ProgressBar(this));
        loadingPopup.setCanceledOnTouchOutside(false);
        loadingPopup.setOnCancelListener(null);

        getSupportActionBar().setSubtitle("v" + BuildConfig.VERSION_NAME);
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(getNavController(), appBarConfiguration) || super.onSupportNavigateUp();
    }

    @SuppressWarnings("ConstantConditions")
    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            switch (getNavController().getCurrentDestination().getId()) {
                case R.id.userInfoFormFragment:
                case R.id.conditionsOfUseFragment:
                case R.id.phoneNumberAuthenticationV1Fragment:
                case R.id.phoneNumberAuthenticationV2Fragment:
                    getInterFragmentStore().<Runnable>remove(InterFragmentStore.MO_ACTION_CANCEL).run();
                    return true;
                default:
                    break;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    public NavController getNavController() {
        return Navigation.findNavController(this, R.id.nav_host_fragment);
    }

    public InterFragmentStore getInterFragmentStore() {
        return interFragmentStore;
    }

    // region Client.Delegate 구현부
    // 클라우드 사용자 가입 정보가 요구되는 이벤트 발생
    @SuppressWarnings("ConstantConditions")
    @Override
    public void acquireUserInfo(@NonNull Client.UserInfoAcceptor acceptor, @NonNull Runnable cancel) {
        registerCancelAction();

        getInterFragmentStore().put(R.id.userInfoFormFragment, InterFragmentStore.MO_ACTION_CONFIRM, acceptor);
        getInterFragmentStore().put(R.id.userInfoFormFragment, InterFragmentStore.MO_API_CANCEL, cancel);

        getNavController().navigate(R.id.userInfoFormFragment);
        getSupportActionBar().hide();
    }

    // 사용자의 약관 동의가 요구되는 이벤트 발생
    @SuppressWarnings("ConstantConditions")
    @Override
    public void acknowledgeConditionsOfUse(@Nullable AcquiredUserInfo userInfo,
                                           AcknowledgeConditionsOfUseReason reason,
                                           @NonNull BiConsumer<String, String> agree,
                                           @NonNull Runnable cancel) {
        BiConsumer<String, String> confirmAction;
        if (getNavController().getCurrentDestination().getId() == R.id.userInfoFormFragment) {
            // 이용자 가입 과정에서 이벤트 발생 시
            confirmAction = agree;
        } else {
            // 약관 업데이트로 인한 이벤트 발생 시 (현재 화면 != 사용자 정보 입력 화면)
            val cancelAction = registerCancelAction();

            confirmAction = (conditionsOfUseVer, privacyPolicyVer) -> {
                agree.accept(conditionsOfUseVer, privacyPolicyVer);
                cancelAction.run();     // 완료 후 약관 동의 화면 종료 처리
            };
        }

        Runnable acknowledgeConditionsOfUse = () -> {
            getInterFragmentStore().put(R.id.conditionsOfUseFragment, InterFragmentStore.MO_ACTION_CONFIRM, confirmAction);
            getInterFragmentStore().put(R.id.conditionsOfUseFragment, InterFragmentStore.MO_API_CANCEL, cancel);

            Bundle bundle = new Bundle();
            bundle.putSerializable("reason", reason);
            getNavController().navigate(R.id.conditionsOfUseFragment, bundle);
        };

        if (reason == AcknowledgeConditionsOfUseReason.duplicatedUserInfo) {
            // 중복 가입 정보 처리 (생년월일+전화번호가 동일하고 이름이 다른 경우)
            new AlertDialog.Builder(this)
                .setTitle("재가입 안내")
                .setMessage("다른 이름으로 가입된 정보가 있습니다. 재가입하시겠습니까?\n클라우드에 저장되어 있던 인증서는 모두 삭제됩니다.")
                .setPositiveButton(android.R.string.ok, (dialog, which) -> acknowledgeConditionsOfUse.run())
                .setNegativeButton(android.R.string.cancel,(dialog, which) -> getInterFragmentStore().<Runnable>remove(InterFragmentStore.MO_ACTION_CANCEL).run())
                .show();
        } else {
            // 약관이 업데이트된 경우 재동의가 필요함을 안내
            if(reason == AcknowledgeConditionsOfUseReason.updated)
                Toast.makeText(this, "클라우드 서비스 약관이 변경되었습니다. 약관을 확인해주세요.", Toast.LENGTH_SHORT).show();
            acknowledgeConditionsOfUse.run();
        }
    }

    // 사용자로부터 MO 인증이 요구되는 이벤트 발생
    @SuppressWarnings("ConstantConditions")
    @Override
    public void onPhoneNumberProofTransactionStart(@NonNull PhoneNumberProofTransaction transaction,
                                                   @NonNull Runnable cancel) {
        val cancelAction = Optional.<Runnable>ofNullable(getInterFragmentStore().remove(InterFragmentStore.MO_ACTION_CANCEL))
            .orElseGet(() -> {
                int destination = getNavController().getCurrentDestination().getId();

                return () -> {
                    getNavController().popBackStack(destination, false);
                    getInterFragmentStore().remove(InterFragmentStore.MO_ACTION_CANCEL);
                    getSupportActionBar().show();
                };
            });

        getInterFragmentStore().put(InterFragmentStore.MO_ACTION_CANCEL, (Runnable)() -> {
            transaction.cancel();
            cancelAction.run();
        });

        int fragId;
        if (transaction.getProofMethod() == PhoneNumberProofMethod.Mobile) {
            fragId = R.id.phoneNumberAuthenticationV2Fragment;
        } else {
            fragId = R.id.phoneNumberAuthenticationV1Fragment;
        }

        getInterFragmentStore().put(fragId, InterFragmentStore.MO_ACTION_CONFIRM, (Runnable)() -> {
            transaction.commit();
            cancelAction.run();
        });

        getInterFragmentStore().put(fragId, InterFragmentStore.MO_API_TRANSACTION, transaction);
        getInterFragmentStore().put(fragId, InterFragmentStore.MO_API_CANCEL, cancel);

        getNavController().navigate(fragId);
    }

    @SuppressWarnings("ConstantConditions")
    private Runnable registerCancelAction() {
        int destFragId = getNavController().getCurrentDestination().getId();
        Runnable cancelAction = () -> {
            getNavController().popBackStack(destFragId, false);
            getInterFragmentStore().remove(InterFragmentStore.MO_ACTION_CANCEL);
            getSupportActionBar().show();
        };

        getInterFragmentStore().put(InterFragmentStore.MO_ACTION_CANCEL, cancelAction);

        return cancelAction;
    }
    // endregion



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

        PasswordDialog.show(this,
                title,
                isPin,
                true,
                "",
                secret -> code.accept(new SecureData(secret.getBytes())),
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

        Intent intent = new Intent(this, BillActivity.class);
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

    // 전환 발급/갱신이 중 PIN 입력을 취소하는 경우 이벤트 발생
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
                assert false: "정의되지 않은 클라우드 전환 발급/갱신 프로세스입니다.";
        }

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> complete.run()) // 클라우드 저장 없이 로컬 발급/갱신 진행
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> cancel.run()) // 발급/갱신 취소
                .show();

    }
    // endregion

    // region   전 화면 공통 로딩 UI (Activity에 attach)
    public void showLoading() {
        loadingPopup.show();
    }

    public void dismissLoading() {
        loadingPopup.dismiss();
    }
    // endregion

    // 클라우드 내 인증서 update() 처리 중 BillActivity 화면 결과 처리
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == BillActivity.ID) {
            String useAction;
            String discardAction;
            if(resultCode == Activity.RESULT_OK){
                useAction = InterFragmentStore.BILL_ACTION_COMPLETE;
                discardAction = InterFragmentStore.BILL_ACTION_CANCEL;
            }
            else {
                useAction = InterFragmentStore.BILL_ACTION_CANCEL;
                discardAction = InterFragmentStore.BILL_ACTION_COMPLETE;
            }
            getInterFragmentStore().<Runnable>remove(useAction).run();
            getInterFragmentStore().remove(discardAction);
        }
        else
            super.onActivityResult(requestCode, resultCode, data);
    }
}