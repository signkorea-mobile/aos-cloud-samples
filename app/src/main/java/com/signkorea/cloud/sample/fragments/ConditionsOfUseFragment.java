package com.signkorea.cloud.sample.fragments;

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

import com.yettiesoft.cloud.AcknowledgeConditionsOfUseReason;
import com.signkorea.cloud.sample.DataBindingFragment;
import com.signkorea.cloud.sample.MainActivity;
import com.signkorea.cloud.sample.R;
import com.signkorea.cloud.sample.databinding.FragmentConditionsOfUseBinding;
import com.signkorea.cloud.sample.utils.OnceRunnable;

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
                getInterFragmentStore().<Runnable>peek(MainActivity.MoAuthCancelAction).run();
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

        onAgree = getInterFragmentStore().accept(
            R.id.conditionsOfUseFragment,
            MainActivity.MoAuthConfirmAction);

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
                        .setTitle("개정 약관 동의")
                        .setMessage("개정된 약관에 동의하지 않으면 서비스를 이용하실 수 없습니다.")
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> getInterFragmentStore().<Runnable>peek(MainActivity.MoAuthCancelAction).run())
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
                } else {
                    getInterFragmentStore().<Runnable>peek(MainActivity.MoAuthCancelAction).run();
                }
            });
        }
    }
}
