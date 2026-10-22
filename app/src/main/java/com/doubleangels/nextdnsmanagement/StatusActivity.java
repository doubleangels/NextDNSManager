package com.doubleangels.nextdnsmanagement;

import android.os.Bundle;
import android.view.View;

import com.doubleangels.nextdnsmanagement.utils.InsetsHelper;

/**
 * Activity explaining the meaning of each connection status indicator color.
 */
public class StatusActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);
        setupInsets();
    }

    private void setupInsets() {
        View root = findViewById(R.id.root);
        InsetsHelper.installOnRoot(root);
        InsetsHelper.applySystemBarPadding(root);
    }
}
