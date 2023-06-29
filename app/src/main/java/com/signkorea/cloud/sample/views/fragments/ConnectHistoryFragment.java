package com.signkorea.cloud.sample.views.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.signkorea.cloud.sample.databinding.FragmentConnectHistoryBinding;
import com.signkorea.cloud.sample.databinding.ItemConnectHistoryBinding;
import com.signkorea.cloud.sample.utils.OnceRunnable;
import com.signkorea.cloud.sample.viewModels.ConnectHistoryFragmentViewModel;
import com.signkorea.cloud.sample.views.base.ViewModelFragment;
import com.yettiesoft.cloud.Client;
import com.yettiesoft.cloud.InvalidLicenseException;

import java.text.DateFormat;
import java.util.Date;

import lombok.val;

public class ConnectHistoryFragment extends ViewModelFragment<FragmentConnectHistoryBinding, ConnectHistoryFragmentViewModel> {
    private final Adapter adapter = new Adapter();

    @SuppressLint("NotifyDataSetChanged")
    private final OnceRunnable loadData = new OnceRunnable(() -> {
        try {
            getViewModel().init(requireContext().getApplicationContext(), (Client.Delegate)requireActivity());
        } catch (InvalidLicenseException exception) {
            alertException(exception, true);
        }

        showLoading();
        getViewModel().loadData(() -> {
                    dismissLoading();
                    adapter.notifyDataSetChanged();
                },
                exception -> alertException(exception, true));
    });

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getBinding().recyclerView.setAdapter(adapter);
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
