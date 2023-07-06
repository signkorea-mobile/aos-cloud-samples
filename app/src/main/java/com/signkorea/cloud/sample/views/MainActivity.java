package com.signkorea.cloud.sample.views;

import android.annotation.SuppressLint;
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

import com.signkorea.cloud.sample.BuildConfig;
import com.signkorea.cloud.sample.R;
import com.signkorea.cloud.sample.databinding.ActivityMainBinding;
import com.signkorea.cloud.sample.enums.CertificateOperation;
import com.signkorea.cloud.sample.viewModels.InterFragmentStore;
import com.signkorea.cloud.sample.views.base.DataBindingActivity;
import com.signkorea.cloud.sample.views.fragments.CloudCertificateListFragmentArgs;
import com.signkorea.cloud.sample.views.fragments.LocalCertificateListFragmentArgs;
import com.yettiesoft.cloud.AcknowledgeConditionsOfUseReason;
import com.yettiesoft.cloud.AcquiredUserInfo;
import com.yettiesoft.cloud.Client;
import com.yettiesoft.cloud.PhoneNumberProofMethod;
import com.yettiesoft.cloud.PhoneNumberProofTransaction;

import java.util.Optional;
import java.util.function.BiConsumer;

import lombok.val;

public class MainActivity extends DataBindingActivity<ActivityMainBinding> implements Client.Delegate {
    private final String TAG = getClass().getSimpleName();
    private InterFragmentStore interFragmentStore;
    private AppBarConfiguration appBarConfiguration;
    private Dialog loadingPopup;

    @Override
    @SuppressLint("NonConstantResourceId")
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        interFragmentStore = new ViewModelProvider(this).get(InterFragmentStore.class);

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
            // TODO 약관 업데이트 케이스 동작 확인
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

    // region   전 화면 공통 로딩 UI (Activity에 attach)
    public void showLoading() {
        loadingPopup.show();
    }

    public void dismissLoading() {
        loadingPopup.dismiss();
    }
    // endregion

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
}