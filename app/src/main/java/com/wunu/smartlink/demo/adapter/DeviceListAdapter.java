package com.wunu.smartlink.demo.adapter;

import android.graphics.Color;
import android.provider.CalendarContract;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.RecyclerView;

import com.clj.fastble.data.BleDevice;
import com.wunu.smartlink.sdk.utils.Hex;

import java.util.*;

public class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.ViewHolder> {

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView line1;
        TextView line2;

        ViewHolder(View itemView) {
            super(itemView);
            line1 = itemView.findViewById(android.R.id.text1);
            line2 = itemView.findViewById(android.R.id.text2);
        }
    }

    interface OnAdapterItemClickListener {

        void onAdapterViewClick(View view);
    }

    private static final Comparator<BleDevice> SORTING_COMPARATOR = (lhs, rhs) ->
            rhs.getRssi() - lhs.getRssi();
    private final List<BleDevice> data = new ArrayList<>();
    private OnAdapterItemClickListener onAdapterItemClickListener;
    private final View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if (onAdapterItemClickListener != null) {
                onAdapterItemClickListener.onAdapterViewClick(v);
            }
        }
    };

    public void addScanResult(BleDevice device) {
        // Not the best way to ensure distinct devices, just for sake on the demo.

        for (int i = 0; i < data.size(); i++) {

            if (TextUtils.equals(data.get(i).getMac(), device.getMac())) {
                data.set(i, device);
                notifyItemChanged(i);
                return;
            }
        }

        data.add(device);
        Collections.sort(data, SORTING_COMPARATOR);
        notifyDataSetChanged();
    }

    public void clearScanResults() {
        data.clear();
        notifyDataSetChanged();
    }

    public BleDevice getItemAtPosition(int childAdapterPosition) {
        return data.get(childAdapterPosition);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final BleDevice itemModel = data.get(position);
        holder.line1.setText(itemModel.getName());
        holder.line2.setText(String.format(Locale.getDefault(), "MAC: %s \t\t\t SSID: %d", itemModel.getMac(), itemModel.getRssi()));
        holder.line2.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        LinearLayout.LayoutParams lp1 = (LinearLayout.LayoutParams) holder.line1.getLayoutParams();
        lp1.setMargins(30, 36, 0, 12);
        holder.line1.setLayoutParams(lp1);
        LinearLayout.LayoutParams lp2 = (LinearLayout.LayoutParams) holder.line2.getLayoutParams();
        lp2.setMargins(30, 0, 0, 24);
        holder.line2.setLayoutParams(lp2);

        boolean isBind = false;
        boolean isLightOn = false;

        String featureHex = null;
        if(itemModel.getName().startsWith("WSL_U") || itemModel.getName().startsWith("WSL_N")|| itemModel.getName().startsWith("WSL_M")){
            featureHex = Hex.encodeHex(itemModel.getScanRecord()).substring(10, 14);
        }else if(itemModel.getName().startsWith("WSL_O") || itemModel.getName().startsWith("WSL_F")
                || itemModel.getName().startsWith("WSL_C")|| itemModel.getName().startsWith("WSL_D")){
            featureHex = Hex.encodeHex(itemModel.getScanRecord()).substring(16, 20);
        }

        if(featureHex != null) {
            isLightOn = TextUtils.equals("1", featureHex.substring(1, 2));
            isBind = TextUtils.equals("1", featureHex.substring(3, 4));
        }

        if(isBind){
            holder.line1.setTextColor(Color.RED);
        }else{
            holder.line1.setTextColor(isLightOn ? Color.GREEN : Color.BLACK);
        }

    }

    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View itemView = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.two_line_list_item, parent, false);
        itemView.setOnClickListener(onClickListener);
        return new ViewHolder(itemView);
    }

    public void setOnAdapterItemClickListener(OnAdapterItemClickListener onAdapterItemClickListener) {
        this.onAdapterItemClickListener = onAdapterItemClickListener;
    }
}
