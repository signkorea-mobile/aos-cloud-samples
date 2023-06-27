package com.signkorea.cloud.sample.fragments;

import android.annotation.SuppressLint;
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
import com.signkorea.cloud.sample.databinding.FragmentUserHistoryBinding;
import com.signkorea.cloud.sample.databinding.ItemUserHistoryBinding;
import com.signkorea.cloud.sample.utils.OnceRunnable;
import com.signkorea.cloud.sample.viewModels.UserHistoryViewModel;
import com.yettiesoft.cloud.models.UserHistory;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Optional;

import lombok.val;

public class UserHistoryFragment extends ViewModelFragment<FragmentUserHistoryBinding, UserHistoryViewModel> {
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
        private final ItemUserHistoryBinding binding;

        public ItemView(ItemUserHistoryBinding binding) {
            super(binding.getRoot());

            this.binding = binding;
        }

        public void bind(@NonNull UserHistory data) {
            @SuppressLint("SimpleDateFormat")
            val dt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(data.getDate());

            binding.setDate(dt);
            binding.setDevice(data.getDevice());
            binding.setCustomer(data.getCustomerName());
            binding.setService(data.getServiceName());
            binding.setAction(data.getAction());
            binding.setResult(data.getResult());
        }

        public static ItemView create(ViewGroup parent) {
            val binding = ItemUserHistoryBinding.inflate(
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
            holder.bind(getViewModel().getUserHistory().get(position));
        }

        @Override
        public int getItemCount() {
            return getViewModel().getUserHistory().size();
        }
    }
}
