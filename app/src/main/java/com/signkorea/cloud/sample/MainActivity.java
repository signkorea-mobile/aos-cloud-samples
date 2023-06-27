package com.signkorea.cloud.sample;

import android.Manifest;
import android.app.AlertDialog;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.yettiesoft.cloud.AcknowledgeConditionsOfUseReason;
import com.yettiesoft.cloud.AcquiredUserInfo;
import com.yettiesoft.cloud.Client;
import com.yettiesoft.cloud.PhoneNumberProofMethod;
import com.yettiesoft.cloud.PhoneNumberProofTransaction;
import com.signkorea.cloud.sample.databinding.ActivityMainBinding;
import com.signkorea.cloud.sample.enums.CertificateOperation;
import com.signkorea.cloud.sample.fragments.CertificateListFragmentArgs;
import com.signkorea.cloud.sample.viewModels.InterFragmentStore;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import lombok.val;

public class MainActivity extends DataBindingActivity<ActivityMainBinding> implements Client.Delegate {
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
            if (destination.getId() == R.id.certificateListFragment) {
                Optional.ofNullable(getSupportActionBar()).ifPresent(actionBar -> {
                    CertificateOperation operation = CertificateListFragmentArgs.fromBundle(arguments).getOperation();
                    actionBar.setTitle(operation.getLabel());
                });
            }

            if (destination.getId() == R.id.homeFragment || destination.getId() == R.id.LoginFragment ||
                    destination.getId() == R.id.certificateManagementFragment || destination.getId() == R.id.accountManagementFragment) {
                getSupportActionBar().show();
            }
        });

        if (ContextCompat.checkSelfPermission(
            this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    1);
            } else {
                ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    1);
            }
        }
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
                    getInterFragmentStore().<Runnable>peek(MainActivity.MoAuthCancelAction).run();
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

    public static final String MoAuthConfirmAction = "MO AUTH confirm action";
    public static final String MoAuthCancelAction = "MO AUTH cancel action";
    public static final String MoAuthTransaction = "MO AUTH transaction";

    @SuppressWarnings("ConstantConditions")
    private Runnable registerCancelAction() {
        int destination = getNavController().getCurrentDestination().getId();

        Runnable cancelAction = () -> {
            getNavController().popBackStack(destination, false);

            getInterFragmentStore().remove(MoAuthCancelAction);

            getSupportActionBar().show();
        };

        getInterFragmentStore().entrust(MoAuthCancelAction, cancelAction);

        return cancelAction;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void acquireUserInfo(@NonNull Client.UserInfoAcceptor acceptor) {
        registerCancelAction();

        getInterFragmentStore().entrust(
                R.id.userInfoFormFragment,
                MoAuthConfirmAction,
                acceptor);

        getNavController().navigate(R.id.userInfoFormFragment);

        getSupportActionBar().hide();
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void acknowledgeConditionsOfUse(@Nullable AcquiredUserInfo userInfo, AcknowledgeConditionsOfUseReason reason, @NonNull BiConsumer<String, String> agree) {
        BiConsumer<String, String> confirmAction;

        if (getNavController().getCurrentDestination().getId() == R.id.userInfoFormFragment) {
            confirmAction = agree;
        } else {
            // 약관동의 독립화면 (현재 화면 != 사용자 정보 입력 화면) -> 취소 핸들러 등록
            val cancelAction = registerCancelAction();

            confirmAction = (conditionsOfUseVer, privacyPolicyVer) -> {
                agree.accept(conditionsOfUseVer, privacyPolicyVer);

                // 약관동의 독립화면일 경우 약관동의 화면 닫기
                cancelAction.run();
            };
        }

        Runnable acknowledgeConditionsOfUse = () -> {
            getInterFragmentStore().entrust(
                R.id.conditionsOfUseFragment,
                MoAuthConfirmAction,
                confirmAction);

            Bundle bundle = new Bundle();
            bundle.putSerializable("reason", reason);

            getNavController().navigate(R.id.conditionsOfUseFragment, bundle);
        };

        // 생년월일 및 전화번호가 같은 사용자 있음
        if (reason == AcknowledgeConditionsOfUseReason.duplicatedUserInfo) {
            new AlertDialog.Builder(this)
                .setTitle("다른 이름으로 가입된 정보가 있습니다.")
                .setMessage("재가입하시겠습니까? 저장된 인증서는 모두 삭제됩니다.")
                .setPositiveButton(android.R.string.ok, (dialog, which) -> acknowledgeConditionsOfUse.run())
                .setNegativeButton(android.R.string.cancel,(dialog, which) -> getInterFragmentStore().<Runnable>peek(MoAuthCancelAction).run())
                .show();
        } else {
            acknowledgeConditionsOfUse.run();
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onPhoneNumberProofTransactionStart(@NonNull PhoneNumberProofTransaction transaction) {
        val cancelAction = Optional.<Runnable>ofNullable(getInterFragmentStore().peek(MoAuthCancelAction))
            .orElseGet(() -> {
                int destination = getNavController().getCurrentDestination().getId();

                return () -> {
                    getNavController().popBackStack(destination, false);

                    getInterFragmentStore().remove(MoAuthCancelAction);

                    getSupportActionBar().show();
                };
            });

        getInterFragmentStore().entrust(MoAuthCancelAction, (Runnable)() -> {
            transaction.cancel();

            cancelAction.run();
        });

        int fragId;
        if (transaction.getProofMethod() == PhoneNumberProofMethod.Mobile) {
            fragId = R.id.phoneNumberAuthenticationV2Fragment;
        } else {
            fragId = R.id.phoneNumberAuthenticationV1Fragment;
        }

        getInterFragmentStore().entrust(fragId, MoAuthConfirmAction, (Runnable)() -> {
            transaction.commit();

            cancelAction.run();
        });

        getInterFragmentStore().entrust(fragId, MoAuthTransaction, transaction);

        getNavController().navigate(fragId);
    }
}