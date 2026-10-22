package com.doubleangels.nextdnsmanagement.adaptors;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.doubleangels.nextdnsmanagement.R;
import com.doubleangels.nextdnsmanagement.sentry.SentryManager;

import java.util.List;

/**
 * A RecyclerView Adapter for displaying a list of PermissionInfo objects.
 * Each item shows the permission name and a short description, along with
 * whether the permission is currently granted or not (when applicable).
 */
public class PermissionsAdapter extends RecyclerView.Adapter<PermissionsAdapter.PermissionViewHolder> {

    private List<PermissionInfo> permissions;

    /**
     * Constructor for the PermissionsAdapter.
     *
     * @param permissions A list of PermissionInfo objects (from PackageManager)
     *                    that should be displayed in the RecyclerView.
     * @throws IllegalArgumentException if the permissions list is null.
     */
    public PermissionsAdapter(List<PermissionInfo> permissions) {
        if (permissions == null) {
            throw new IllegalArgumentException("Permissions list cannot be null");
        }
        this.permissions = permissions;
    }

    public void updatePermissions(List<PermissionInfo> updatedPermissions) {
        if (updatedPermissions == null) {
            throw new IllegalArgumentException("Permissions list cannot be null");
        }
        this.permissions = updatedPermissions;
        notifyDataSetChanged();
    }

    /**
     * Called when the RecyclerView needs a new ViewHolder. Inflates the layout
     * resource (permission_item) for each item in the list.
     *
     * @param parent   The parent ViewGroup into which the new view will be added.
     * @param viewType The view type of the new View. Not used here, but required by
     *                 the API.
     * @return A new PermissionViewHolder holding the inflated layout.
     */
    @NonNull
    @Override
    public PermissionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.permission_item, parent, false);
        return new PermissionViewHolder(itemView);
    }

    /**
     * Binds the data from the PermissionInfo object to the views in the
     * PermissionViewHolder.
     * Also checks if the permission is granted (for relevant permissions, such as
     * POST_NOTIFICATIONS).
     *
     * @param holder   The ViewHolder to be updated with new data.
     * @param position The position in the data set of the item being bound.
     */
    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull PermissionViewHolder holder, int position) {
        // Get the PermissionInfo at the current position
        PermissionInfo permissionInfo = permissions.get(position);

        // Determine if the permission has been granted (runtime permissions only)
        boolean isGranted = true;
        boolean isRuntimePermission = permissionInfo.name.equals(android.Manifest.permission.POST_NOTIFICATIONS)
                || permissionInfo.name.equals(android.Manifest.permission.ACCESS_NOTIFICATION_POLICY);
        if (isRuntimePermission && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                try {
                    isGranted = holder.itemView.getContext()
                            .checkSelfPermission(permissionInfo.name) == PackageManager.PERMISSION_GRANTED;
                } catch (Exception e) {
                    SentryManager.captureException(e);
                    isGranted = false;
                }
        }

        // Load and set the permission name label from system resources, then make it
        // uppercase
        String permissionLabel;
        try {
            CharSequence label = permissionInfo.loadLabel(holder.itemView.getContext().getPackageManager());
            permissionLabel = label.toString().toUpperCase();
        } catch (Exception e) {
            SentryManager.captureException(e);
            permissionLabel = permissionInfo.name.toUpperCase();
        }
        holder.permissionName.setText(permissionLabel);

        // Attempt to load the description of the permission from system resources
        String displayText;
        try {
            CharSequence description = permissionInfo.loadDescription(holder.itemView.getContext().getPackageManager());
            if (description == null || description.toString().trim().isEmpty()) {
                displayText = isRuntimePermission
                        ? (isGranted ? "(GRANTED)" : "(NOT GRANTED)")
                        : "(DECLARED)";
            } else {
                displayText = description.toString();
                if (!displayText.endsWith(".")) {
                    displayText += ".";
                }
                if (isRuntimePermission) {
                    displayText += isGranted ? " (GRANTED)" : " (NOT GRANTED)";
                }
            }
        } catch (Exception e) {
            SentryManager.captureException(e);
            displayText = isRuntimePermission
                    ? (isGranted ? "(GRANTED)" : "(NOT GRANTED)")
                    : "(DECLARED)";
        }
        holder.permissionDescription.setText(displayText);
    }

    /**
     * Returns the total number of PermissionInfo items in this adapter.
     *
     * @return The size of the permissions list.
     */
    @Override
    public int getItemCount() {
        return permissions.size();
    }

    /**
     * Inner ViewHolder class that holds references to each view in a single item
     * layout.
     * This improves performance by avoiding repeated calls to findViewById.
     */
    public static class PermissionViewHolder extends RecyclerView.ViewHolder {

        // UI elements in each list item
        TextView permissionName;
        TextView permissionDescription;

        /**
         * Constructor that assigns references to the TextViews for the permission name
         * and description.
         *
         * @param itemView The inflated layout view for this item.
         */
        public PermissionViewHolder(View itemView) {
            super(itemView);
            permissionName = itemView.findViewById(R.id.permissionName);
            permissionDescription = itemView.findViewById(R.id.permissionDescription);
        }
    }
}
