package com.signkorea.cloud.sample.views;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
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

import com.signkorea.cloud.sample.R;
import com.signkorea.cloud.sample.databinding.ActivityMainBinding;
import com.signkorea.cloud.sample.enums.CertificateOperation;
import com.signkorea.cloud.sample.repository.CertificateRepo;
import com.signkorea.cloud.sample.views.fragments.CloudCertificateListFragmentArgs;
import com.signkorea.cloud.sample.views.fragments.LocalCertificateListFragmentArgs;
import com.signkorea.cloud.sample.viewModels.InterFragmentStore;
import com.signkorea.cloud.sample.views.base.DataBindingActivity;
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
                    getInterFragmentStore().<Runnable>peek(InterFragmentStore.MO_ACTION_CANCEL).run();
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

    @SuppressWarnings("ConstantConditions")
    private Runnable registerCancelAction() {
        int destFragId = getNavController().getCurrentDestination().getId();
        Runnable cancelAction = () -> {
            Integer nextFragId = null;//getInterFragmentStore().remove(InterFragmentStore.FALLBACK_FRAGMENT_ID);   // TODO 필요한지 확인
            if (nextFragId == null)
                getNavController().popBackStack(destFragId, false);
            else
                getNavController().popBackStack(nextFragId, false);
            getInterFragmentStore().remove(InterFragmentStore.MO_ACTION_CANCEL);
            getSupportActionBar().show();
        };

        getInterFragmentStore().entrust(InterFragmentStore.MO_ACTION_CANCEL, cancelAction);

        return cancelAction;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void acquireUserInfo(@NonNull Client.UserInfoAcceptor acceptor, @NonNull Runnable cancel) {
        registerCancelAction();

        getInterFragmentStore().entrust(R.id.userInfoFormFragment, InterFragmentStore.FALLBACK_FRAGMENT_ID,acceptor);
        getInterFragmentStore().entrust(R.id.userInfoFormFragment, InterFragmentStore.MO_API_CANCEL, cancel);

        getNavController().navigate(R.id.userInfoFormFragment);
        getSupportActionBar().hide();
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void acknowledgeConditionsOfUse(@Nullable AcquiredUserInfo userInfo,
                                           AcknowledgeConditionsOfUseReason reason,
                                           @NonNull BiConsumer<String, String> agree,
                                           @NonNull Runnable cancel) {
        BiConsumer<String, String> confirmAction;
        // TODO
        Log.e(TAG, "destination = " + getNavController().getCurrentDestination().getId());

        if (getNavController().getCurrentDestination().getId() == R.id.userInfoFormFragment) {
            // 이용자 가입 과정에서 이벤트 발생 시
            confirmAction = agree;
        } else {
            // TODO 약관 업데이트 처리 시 정상 동작하는지 확인해볼 것: 동의하든 취소하든 destination 화면으로 이동하게 되는 듯
            // 약관 업데이트로 인한 이벤트 발생 시 (현재 화면 != 사용자 정보 입력 화면)
            val cancelAction = registerCancelAction();

            confirmAction = (conditionsOfUseVer, privacyPolicyVer) -> {
                agree.accept(conditionsOfUseVer, privacyPolicyVer);
                cancelAction.run();     // 완료 후 약관 동의 화면 종료 처리
            };
        }

        Runnable acknowledgeConditionsOfUse = () -> {
            getInterFragmentStore().entrust(R.id.conditionsOfUseFragment, InterFragmentStore.FALLBACK_FRAGMENT_ID, confirmAction);
            getInterFragmentStore().entrust(R.id.conditionsOfUseFragment, InterFragmentStore.MO_API_CANCEL, cancel);

            Bundle bundle = new Bundle();
            bundle.putSerializable("reason", reason);
            getNavController().navigate(R.id.conditionsOfUseFragment, bundle);
        };

        if (reason == AcknowledgeConditionsOfUseReason.duplicatedUserInfo) {
            // 중복 가입 정보 처리 (생년월일+전화번호가 동일하고 이름이 다른 경우)
            new AlertDialog.Builder(this)
                .setTitle("다른 이름으로 가입된 정보가 있습니다.")
                .setMessage("재가입하시겠습니까? 저장된 인증서는 모두 삭제됩니다.")
                .setPositiveButton(android.R.string.ok, (dialog, which) -> acknowledgeConditionsOfUse.run())
                .setNegativeButton(android.R.string.cancel,(dialog, which) -> getInterFragmentStore().<Runnable>peek(InterFragmentStore.MO_ACTION_CANCEL).run())
                .show();
        } else {
            // 약관이 업데이트된 경우 재동의가 필요함을 안내
            if(reason == AcknowledgeConditionsOfUseReason.updated)
                Toast.makeText(this, "클라우드 서비스 약관이 변경되었습니다. 약관을 확인해주세요.", Toast.LENGTH_SHORT).show();
            acknowledgeConditionsOfUse.run();
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onPhoneNumberProofTransactionStart(@NonNull PhoneNumberProofTransaction transaction,
                                                   @NonNull Runnable cancel) {
        val cancelAction = Optional.<Runnable>ofNullable(getInterFragmentStore().peek(InterFragmentStore.MO_ACTION_CANCEL))
            .orElseGet(() -> {
                int destination = getNavController().getCurrentDestination().getId();

                return () -> {
                    getNavController().popBackStack(destination, false);
                    getInterFragmentStore().remove(InterFragmentStore.MO_ACTION_CANCEL);
                    getSupportActionBar().show();
                };
            });

        getInterFragmentStore().entrust(InterFragmentStore.MO_ACTION_CANCEL, (Runnable)() -> {
            transaction.cancel();
            cancelAction.run();
        });

        int fragId;
        if (transaction.getProofMethod() == PhoneNumberProofMethod.Mobile) {
            fragId = R.id.phoneNumberAuthenticationV2Fragment;
        } else {
            fragId = R.id.phoneNumberAuthenticationV1Fragment;
        }

        getInterFragmentStore().entrust(fragId, InterFragmentStore.FALLBACK_FRAGMENT_ID, (Runnable)() -> {
            transaction.commit();
            cancelAction.run();
        });

        getInterFragmentStore().entrust(fragId, InterFragmentStore.FALLBACK_FRAGMENT_ID, transaction);
        getInterFragmentStore().entrust(fragId, InterFragmentStore.MO_API_CANCEL, cancel);

        getNavController().navigate(fragId);
    }
}