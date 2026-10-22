package com.doubleangels.nextdnsmanagement;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.doubleangels.nextdnsmanagement.sentry.SentryManager;
import com.doubleangels.nextdnsmanagement.utils.ExternalLinkHandler;
import com.doubleangels.nextdnsmanagement.utils.InsetsHelper;

/**
 * Activity displaying information about the author, including links to GitHub
 * and email.
 */
public class AuthorActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_author);
        setupInsets();

        try {
            setupPersonalLinks();
        } catch (Exception e) {
            SentryManager.captureException(e);
        }
    }

    private void setupInsets() {
        View root = findViewById(R.id.root);
        InsetsHelper.installOnRoot(root);
        InsetsHelper.applySystemBarPadding(root);
    }

    private void setupPersonalLinks() {
        ImageView githubButton = findViewById(R.id.githubImageView);
        ImageView emailButton = findViewById(R.id.emailImageView);
        if (githubButton == null || emailButton == null) {
            return;
        }

        githubButton.setOnClickListener(view -> ExternalLinkHandler.openExternalLink(this,
                Uri.parse(getString(R.string.github_profile_url))));

        emailButton.setOnClickListener(view -> {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:jzgm2llm@addy.io"));
            try {
                startActivity(Intent.createChooser(emailIntent, getString(R.string.send_email)));
            } catch (android.content.ActivityNotFoundException e) {
                Toast.makeText(this, R.string.no_email_client_found, Toast.LENGTH_LONG).show();
            } catch (SecurityException e) {
                Toast.makeText(this, R.string.email_open_security_error,
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}
