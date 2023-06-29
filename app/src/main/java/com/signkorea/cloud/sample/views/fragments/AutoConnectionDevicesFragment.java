package com.signkorea.cloud.sample.views.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.signkorea.cloud.sample.views.base.ViewModelFragment;
import com.signkorea.cloud.sample.databinding.FragmentAutoConnectionDevicesBinding;
import com.signkorea.cloud.sample.databinding.ItemAutoConnectDeviceBinding;
import com.signkorea.cloud.sample.utils.OnceRunnable;
import com.signkorea.cloud.sample.viewModels.AutoConnectionDevicesFragmentViewModel;
import com.yettiesoft.cloud.Client;
import com.yettiesoft.cloud.InvalidLicenseException;
import com.yettiesoft.cloud.models.AutoConnectDevice;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Date;
import java.util.Optional;
import java.util.function.Consumer;

import lombok.var;

public class AutoConnectionDevicesFragment extends
    ViewModelFragment<FragmentAutoConnectionDevicesBinding, AutoConnectionDevicesFragmentViewModel>
{
    private final Adapter adapter = new Adapter();

    private final OnceRunnable loadData = new OnceRunnable(() -> {
        try {
            getViewModel().init(requireContext().getApplicationContext(), (Client.Delegate)requireActivity());
        } catch (InvalidLicenseException exception) {
            alertException(exception, true);
            return;
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

    private void onItemClick(int position) {
        new AlertDialog.Builder(requireContext())
            .setTitle("자동 연결 매체 삭제")
            .setMessage("선택한 자동 연결 매체를 삭제 하시겠습니까?")
            .setPositiveButton(android.R.string.ok, (dialog, which) -> removeItem(position))
            .setNegativeButton(android.R.string.cancel,(dialog, which) -> {})
            .show();
    }

    private void removeItem(int position) {
        Consumer<Boolean> onItemDeleted = isCurrentDevice -> {
            dismissLoading();
            if (isCurrentDevice) {
                // 현재 디바이스 삭제 -> 뒤로 가기
                getNavController().popBackStack();
            } else {
                adapter.notifyItemRemoved(position);
            }
        };

        Consumer<Exception> onError = exception -> alertException(exception, "자동 연결 삭제");

        showLoading();
        getViewModel().removeItem(position, onItemDeleted, onError);
    }

    static class ItemView extends RecyclerView.ViewHolder {
        private final ItemAutoConnectDeviceBinding binding;

        public ItemView(ItemAutoConnectDeviceBinding binding) {
            super(binding.getRoot());

            this.binding = binding;
        }

        public void bind(@NonNull AutoConnectDevice device) {
            DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL);

            String registeredAt = Optional.ofNullable(device.getRegisteredAt())
                .map(Date::toInstant)
                .map(date -> date.atZone(ZoneId.systemDefault()))
                .map(ZonedDateTime::toLocalDate)
                .map(formatter::format)
                .orElse(null);
            String lastLoggedInAt = Optional.ofNullable(device.getLastLoggedInAt())
                .map(Date::toInstant)
                .map(date -> date.atZone(ZoneId.systemDefault()))
                .map(ZonedDateTime::toLocalDate)
                .map(formatter::format)
                .orElse(null);

            binding.setRegisterDate(registeredAt);
            binding.setLastLoginDate(lastLoggedInAt);
            binding.setIpAddress(device.getIp());
            binding.setOs(device.getOs());
            binding.setHwModel(device.getHwModel());
            binding.setPlatformVersion(device.getPlatformVersion());
            binding.setNickName(device.getNickName());
            binding.setServiceName(device.getServiceName());
            binding.setDeviceInfo(device.getDeviceInfo());
        }

        public static ItemView create(ViewGroup parent) {
            ItemAutoConnectDeviceBinding binding = ItemAutoConnectDeviceBinding.inflate(
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
            var itemView = ItemView.create(parent);

            itemView.binding.getRoot().setOnClickListener(view -> {
                int pos = itemView.getAdapterPosition();

                if (pos == RecyclerView.NO_POSITION) {
                    return;
                }

                AutoConnectionDevicesFragment.this.onItemClick(pos);
            });

            return itemView;
        }

        @Override
        public void onBindViewHolder(@NonNull ItemView holder, int position) {
            holder.bind(getViewModel().getDevices().get(position));
        }

        @Override
        public int getItemCount() {
            return getViewModel().getDevices().size();
        }
    }
}
