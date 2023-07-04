package com.signkorea.cloud.sample.views.fragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.signkorea.cloud.sample.views.base.DataBindingFragment;
import com.signkorea.cloud.sample.R;
import com.signkorea.cloud.sample.databinding.FragmentConditionsOfUseBinding;
import com.signkorea.cloud.sample.utils.OnceRunnable;
import com.signkorea.cloud.sample.viewModels.InterFragmentStore;
import com.yettiesoft.cloud.AcknowledgeConditionsOfUseReason;

import java.util.function.BiConsumer;

import lombok.val;

public class ConditionsOfUseFragment extends DataBindingFragment<FragmentConditionsOfUseBinding> {
    private BiConsumer<String, String> onAgree;
    private AcknowledgeConditionsOfUseReason reason;

    private final OnceRunnable loadPage = new OnceRunnable(() -> getBinding().webView.loadUrl(getUrl()));

    @Nullable
    private String getUrl() {
        val ctx = requireContext();
        val appId = ctx.getPackageName();

        try {
            val ai = ctx.getPackageManager().getApplicationInfo(appId, PackageManager.GET_META_DATA);
            return ai.metaData.getString("Cloud NPKI Conditions of Use URL");
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                cancel();
            }
        });
    }

    @SuppressLint({"JavascriptInterface", "SetJavaScriptEnabled"})
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getBinding().webView.getSettings().setJavaScriptEnabled(true);
        getBinding().webView.addJavascriptInterface(new Bridge(), "conditionsOfUse");
        getBinding().webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                new AlertDialog.Builder(requireContext())
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> result.confirm())
                    .setCancelable(false)
                    .create()
                    .show();

                return true;
            }

            @Override
            public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
                new AlertDialog.Builder(requireContext())
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> result.confirm())
                    .setNegativeButton(android.R.string.cancel, (dialog, which) -> result.cancel())
                    .setCancelable(false)
                    .create()
                    .show();

                return true;
            }
        });

        onAgree = getInterFragmentStore().remove(
            R.id.conditionsOfUseFragment,
            InterFragmentStore.MO_ACTION_CONFIRM);

        reason = ConditionsOfUseFragmentArgs.fromBundle(getArguments()).getReason();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadPage.run();
    }

    class Bridge {
        @JavascriptInterface
        public void agree(@NonNull String termsOfUseVer, @NonNull String privacyPolicyVer) {
            new Handler(Looper.getMainLooper()).post(() -> onAgree.accept(termsOfUseVer, privacyPolicyVer));
        }

        @SuppressWarnings("ConstantConditions")
        @JavascriptInterface
        public void cancel() {
            new Handler(Looper.getMainLooper()).post(() -> {
                if (reason == AcknowledgeConditionsOfUseReason.updated) {
                    new AlertDialog.Builder(requireContext())
                        .setTitle("약관 개정 안내")
                        .setMessage("개정된 약관에 동의하신 후 서비스를 이용하실 수 있습니다.")
                        .setPositiveButton(android.R.string.ok, null)
                        .setNegativeButton(android.R.string.cancel, (dialog, which) -> cancel())
                        .show();
                } else {
                    cancel();
                }
            });
        }
    }

    private void cancel() {
        getInterFragmentStore().<Runnable>remove(R.id.conditionsOfUseFragment, InterFragmentStore.MO_API_CANCEL).run();
        getInterFragmentStore().<Runnable>remove(InterFragmentStore.MO_ACTION_CANCEL).run();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getInterFragmentStore().remove(R.id.conditionsOfUseFragment, InterFragmentStore.MO_API_CANCEL);
    }
}
