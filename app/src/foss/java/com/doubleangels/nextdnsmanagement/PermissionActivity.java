package com.doubleangels.nextdnsmanagement;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.doubleangels.nextdnsmanagement.adaptors.PermissionsAdapter;
import com.doubleangels.nextdnsmanagement.sentry.SentryManager;
import com.doubleangels.nextdnsmanagement.utils.InsetsHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity that displays the list of permissions requested by the application.
 */
@RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
public class PermissionActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission);
        setupInsets();

        RecyclerView recyclerView = findViewById(R.id.permissionRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new PermissionsAdapter(getPermissionsList()));
    }

    private void setupInsets() {
        View root = findViewById(R.id.root);
        InsetsHelper.installOnRoot(root);
        InsetsHelper.applySystemBarPadding(root);
    }

    private List<PermissionInfo> getPermissionsList() {
        List<PermissionInfo> permissions = new ArrayList<>();
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(
                    getPackageName(),
                    PackageManager.GET_PERMISSIONS);
            if (packageInfo.requestedPermissions != null) {
                for (String permission : packageInfo.requestedPermissions) {
                    try {
                        PermissionInfo permissionInfo = getPackageManager().getPermissionInfo(permission, 0);
                        permissions.add(permissionInfo);
                    } catch (PackageManager.NameNotFoundException ignored) {
                        // Ignore permissions not found on older Android versions
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            SentryManager.captureException(e);
        }
        return permissions;
    }
}
