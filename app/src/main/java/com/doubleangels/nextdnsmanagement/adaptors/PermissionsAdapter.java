package com.doubleangels.nextdnsmanagement.adaptors;

import android.annotation.SuppressLint;
import android.content.pm.PermissionInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.doubleangels.nextdnsmanagement.R;

import java.util.List;

public class PermissionsAdapter extends RecyclerView.Adapter<PermissionsAdapter.PermissionViewHolder> {

    private final List<PermissionInfo> permissions;

    public PermissionsAdapter(List<PermissionInfo> permissions) {
        this.permissions = permissions;
    }

    @NonNull
    @Override
    public PermissionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.permission_item, parent, false);
        return new PermissionViewHolder(itemView);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull PermissionViewHolder holder, int position) {
        PermissionInfo permissionInfo = permissions.get(position);
        holder.permissionName.setText(permissionInfo.loadLabel(holder.itemView.getContext().getPackageManager()).toString().toUpperCase());
        CharSequence description = permissionInfo.loadDescription(holder.itemView.getContext().getPackageManager());
        if (description != null && !description.toString().endsWith(".")) {
            holder.permissionDescription.setText((description + ".").toUpperCase());
        } else {
            holder.permissionDescription.setText(description);
        }
    }

    @Override
    public int getItemCount() {
        return permissions.size();
    }

    public static class PermissionViewHolder extends RecyclerView.ViewHolder {
        TextView permissionName;
        TextView permissionDescription;

        public PermissionViewHolder(View itemView) {
            super(itemView);
            permissionName = itemView.findViewById(R.id.permissionName);
            permissionDescription = itemView.findViewById(R.id.permissionDescription);
        }
    }
}