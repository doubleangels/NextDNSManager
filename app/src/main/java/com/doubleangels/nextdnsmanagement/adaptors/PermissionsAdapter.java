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

    // List to hold PermissionInfo objects
    private final List<PermissionInfo> permissions;

    // Constructor to initialize the adapter with a list of permissions
    public PermissionsAdapter(List<PermissionInfo> permissions) {
        this.permissions = permissions;
    }

    // Create new views (invoked by the layout manager)
    @NonNull
    @Override
    public PermissionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the permission_item layout
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.permission_item, parent, false);
        // Return a new ViewHolder
        return new PermissionViewHolder(itemView);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull PermissionViewHolder holder, int position) {
        // Get the PermissionInfo object at the given position
        PermissionInfo permissionInfo = permissions.get(position);
        // Set the permission name in the TextView
        holder.permissionName.setText(permissionInfo.loadLabel(holder.itemView.getContext().getPackageManager()).toString().toUpperCase());
        // Set the permission description in the TextView
        CharSequence description = permissionInfo.loadDescription(holder.itemView.getContext().getPackageManager());
        if (description != null && !description.toString().endsWith(".")) {
            holder.permissionDescription.setText((description + ".").toUpperCase());
        } else {
            holder.permissionDescription.setText(description);
        }
    }

    // Return the size of the dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return permissions.size();
    }

    // ViewHolder class to hold the views for each item in the RecyclerView
    public static class PermissionViewHolder extends RecyclerView.ViewHolder {
        // TextViews to display permission name and description
        TextView permissionName;
        TextView permissionDescription;

        // Constructor to initialize the ViewHolder with the item view
        public PermissionViewHolder(View itemView) {
            super(itemView);
            // Find and assign the TextViews
            permissionName = itemView.findViewById(R.id.permissionName);
            permissionDescription = itemView.findViewById(R.id.permissionDescription);
        }
    }
}
