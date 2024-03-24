package com.doubleangels.nextdnsmanagement;

import static android.Manifest.permission.POST_NOTIFICATIONS;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.doubleangels.nextdnsmanagement.geckoruntime.GeckoRuntimeSingleton;
import com.doubleangels.nextdnsmanagement.protocoltest.VisualIndicator;
import com.doubleangels.nextdnsmanagement.sentry.SentryInitializer;
import com.doubleangels.nextdnsmanagement.sentry.SentryManager;

import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoRuntimeSettings;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoView;
import org.mozilla.geckoview.WebExtension;
import org.mozilla.geckoview.WebResponse;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static GeckoRuntime runtime;
    private GeckoSession geckoSession;
    private Boolean darkMode;

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @SuppressLint("WrongThread")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SentryManager sentryManager = new SentryManager(this);
        SharedPreferences sharedPreferences = this.getSharedPreferences("preferences", Context.MODE_PRIVATE);
        try {
            if (ContextCompat.checkSelfPermission(this, POST_NOTIFICATIONS) == PackageManager.PERMISSION_DENIED) {
                ActivityCompat.requestPermissions(this, new String[]{POST_NOTIFICATIONS}, 1);
            }
            if (sentryManager.isSentryEnabled()) {
                Log.d("Sentry", "Sentry is enabled for NextDNS Manager.");
                SentryInitializer sentryInitializer = new SentryInitializer();
                sentryInitializer.execute(this);
            }
            setupToolbar();
            String appLocale = setupLanguage();
            setupDarkMode(sharedPreferences);
            setupVisualIndicator(sentryManager);
            GeckoView geckoView = findViewById(R.id.geckoView);
            geckoSession = new GeckoSession();
            geckoSession.setContentDelegate(new GeckoSession.ContentDelegate() {
                @Override
                public void onExternalResponse(@NonNull GeckoSession geckoSession, @NonNull WebResponse webResponse) {
                    GeckoSession.ContentDelegate.super.onExternalResponse(geckoSession, webResponse);
                    if (webResponse.isSecure) {
                        downloadFile(webResponse.uri.replace("?sign=1", ""));
                    }
                }
            });
            if (runtime == null) {
                runtime = GeckoRuntime.create(this);
                GeckoRuntimeSingleton.setInstance(runtime);
                runtime.getSettings()
                        .setAllowInsecureConnections(GeckoRuntimeSettings.HTTPS_ONLY)
                        .setAutomaticFontSizeAdjustment(true);
            }
            runtime.getSettings().setLocales(new String[] {appLocale});
            geckoSession.open(runtime);
            geckoSession.getSettings().setAllowJavascript(true);
            geckoView.setSession(geckoSession);
            if (darkMode) {
                geckoView.coverUntilFirstPaint(getColor(R.color.darkgray));
                runtime.getWebExtensionController()
                        .ensureBuiltIn("resource://android/assets/darkmode/", "nextdns@doubleangels.com");
            } else {
                geckoView.coverUntilFirstPaint(getColor(R.color.white));
                String extensionId = "nextdns@doubleangels.com";
                runtime.getWebExtensionController().list().then(extensions -> {
                    if (extensions != null) {
                        for (WebExtension extension : extensions) {
                            if (extension.id.equals(extensionId)) {
                                runtime.getWebExtensionController().uninstall(extension);
                                return null;
                            }
                        }
                    }
                    return null;
                });
            }
            geckoSession.loadUri(getString(R.string.main_url));
        } catch (Exception e) {
            sentryManager.captureExceptionIfEnabled(e);
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
        }
        ImageView imageView = findViewById(R.id.connectionStatus);
        imageView.setOnClickListener(v -> startActivity(new Intent(this, StatusActivity.class)));
    }

    private String setupLanguage() {
        Configuration config = getResources().getConfiguration();
        Locale appLocale = config.getLocales().get(0);
        Locale.setDefault(appLocale);
        config.setLocale(appLocale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
        return appLocale.getLanguage();
    }

    private void setupDarkMode(SharedPreferences sharedPreferences) {
        String darkModeOverride = sharedPreferences.getString("darkmode_override", "match");
        if (darkModeOverride.contains("match")) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            darkMode = currentNightMode == Configuration.UI_MODE_NIGHT_YES;
        } else if (darkModeOverride.contains("on")) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            darkMode = true;
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            darkMode = false;
        }
    }

    private void setupVisualIndicator(SentryManager sentryManager) {
        try {
            new VisualIndicator(this).initiateVisualIndicator(this, getApplicationContext());
        } catch (Exception e) {
            sentryManager.captureExceptionIfEnabled(e);
        }
    }


    private void downloadFile(String uri) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(uri))
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "NextDNSConfiguration.mobileconfig");
        DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        if (downloadManager != null) {
            downloadManager.enqueue(request);
            Toast.makeText(getApplicationContext(), "Downloading file!", Toast.LENGTH_LONG).show();
        }
    }

    private void startIntent(Class<?> targetClass) {
        Intent intent = new Intent(this, targetClass);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.back -> geckoSession.goBack();
            case R.id.refreshNextDNS -> geckoSession.reload();
            case R.id.pingNextDNS -> startIntent(PingActivity.class);
            case R.id.returnHome -> geckoSession.loadUri(getString(R.string.main_url));
            case R.id.settings -> startIntent(SettingsActivity.class);
        }
        return super.onOptionsItemSelected(item);
    }
}