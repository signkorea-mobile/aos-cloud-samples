package com.signkorea.cloud.sample.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.lumensoft.ks.KSException;
import com.yettiesoft.cloud.Client;
import com.yettiesoft.cloud.InvalidLicenseException;
import com.signkorea.cloud.sample.ViewModelFragment;
import com.signkorea.cloud.sample.databinding.FragmentConnectHistoryBinding;
import com.signkorea.cloud.sample.databinding.ItemConnectHistoryBinding;
import com.signkorea.cloud.sample.utils.OnceRunnable;
import com.signkorea.cloud.sample.viewModels.ConnectHistoryFragmentViewModel;

import org.jetbrains.annotations.NotNull;

import java.text.DateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.function.Consumer;

import lombok.val;

public class ConnectHistoryFragment extends ViewModelFragment<FragmentConnectHistoryBinding, ConnectHistoryFragmentViewModel> {
    private final Adapter adapter = new Adapter();

    private final OnceRunnable loadData = new OnceRunnable(() -> {
        try {
            getViewModel().init(requireContext().getApplicationContext(), (Client.Delegate)requireActivity());
        } catch (InvalidLicenseException exception) {
            alertException(exception, true);
        }

        getViewModel().loadData(adapter::notifyDataSetChanged, exception -> alertException(exception, true));
    });

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getBinding().recyclerView.setAdapter(adapter);
    }

    @Override
    public void onAuthenticationError(int i, CharSequence charSequence) {
        String message = null;
        if (i == KSException.FAILED_CLOUD_BIO_INVALID_PIN) {
            message = charSequence.toString();
        } else {
            message = i + " : " + charSequence;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("생체 인증 실패")
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                })
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();

        loadData.run();
    }

    static class ItemView extends RecyclerView.ViewHolder {
        private final ItemConnectHistoryBinding binding;

        public ItemView(ItemConnectHistoryBinding binding) {
            super(binding.getRoot());

            this.binding = binding;
        }

        public void bind(@NonNull Date connectedAt) {
            val dt = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.MEDIUM).format(connectedAt);
            binding.setDateTime(dt);
        }

        public static ItemView create(ViewGroup parent) {
            ItemConnectHistoryBinding binding = ItemConnectHistoryBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false);

            return new ItemView(binding);
        }
    }

    class Adapter extends RecyclerView.Adapter<ItemView> {
        @NonNull
        @Override
        public ItemView onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return ItemView.create(parent);
        }

        @Override
        public void onBindViewHolder(@NonNull ItemView holder, int position) {
            holder.bind(getViewModel().getConnectHistory().get(position));
        }

        @Override
        public int getItemCount() {
            return getViewModel().getConnectHistory().size();
        }
    }}
