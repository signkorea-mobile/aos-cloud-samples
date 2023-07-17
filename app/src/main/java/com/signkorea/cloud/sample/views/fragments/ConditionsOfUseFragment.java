package com.signkorea.cloud.sample.views.fragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
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

import com.signkorea.cloud.sample.R;
import com.signkorea.cloud.sample.databinding.FragmentConditionsOfUseBinding;
import com.signkorea.cloud.sample.viewModels.InterFragmentStore;
import com.signkorea.cloud.sample.views.base.DataBindingFragment;
import com.yettiesoft.cloud.AcknowledgeConditionsOfUseReason;

import java.util.function.BiConsumer;

public class ConditionsOfUseFragment extends DataBindingFragment<FragmentConditionsOfUseBinding> {
    private BiConsumer<String, String> onAgree;
    private Runnable onCancel;
    private AcknowledgeConditionsOfUseReason reason;

    private final Runnable loadPage = () -> getBinding().webView.loadUrl(getUrl());

    @Nullable
    private String getUrl() {
        Context ctx = requireContext();
        String appId = ctx.getPackageName();

        try {
            ApplicationInfo ai = ctx.getPackageManager().getApplicationInfo(appId, PackageManager.GET_META_DATA);
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
            InterFragmentStore.MO_API_EXECUTOR);

        onCancel = getInterFragmentStore().remove(
                R.id.conditionsOfUseFragment,
                InterFragmentStore.MO_API_CANCEL);

        reason = ConditionsOfUseFragmentArgs.fromBundle(getArguments()).getReason();
        loadPage.run();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    class Bridge {
        @JavascriptInterface
        public void agree(@NonNull String termsOfUseVer, @NonNull String privacyPolicyVer) {
            new Handler(Looper.getMainLooper()).post(() -> onAgree.accept(termsOfUseVer, privacyPolicyVer));
        }

        @SuppressWarnings("ConstantConditions")
        @JavascriptInterface
        public void cancel() {
            new Handler(Looper.getMainLooper()).post(ConditionsOfUseFragment.this::cancel);
        }
    }

    private void cancel() {
        onCancel.run();
    }
}
