package com.doubleangels.nextdnsmanagement;

import static android.Manifest.permission.POST_NOTIFICATIONS;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.doubleangels.nextdnsmanagement.geckoruntime.GeckoRuntimeSingleton;
import com.doubleangels.nextdnsmanagement.protocoltest.VisualIndicator;
import com.doubleangels.nextdnsmanagement.sentrymanager.SentryInitializer;
import com.doubleangels.nextdnsmanagement.sentrymanager.SentryManager;

import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoRuntimeSettings;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoView;
import org.mozilla.geckoview.WebExtension;
import org.mozilla.geckoview.WebResponse;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** @noinspection resource*/
public class MainActivity extends AppCompatActivity {


    private static final String TAG = "StreamLogger";

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
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        try {
            if (ContextCompat.checkSelfPermission(this, POST_NOTIFICATIONS) == PackageManager.PERMISSION_DENIED) {
                ActivityCompat.requestPermissions(this, new String[]{POST_NOTIFICATIONS}, 1);
            }
            if (sentryManager.isSentryEnabled()) {
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
                    Log.d("DOWNLOAD", "Ping - " + webResponse.body);
                    try {
                        logInputStream(webResponse.body);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            if (runtime == null) {
                runtime = GeckoRuntime.create(this);
                GeckoRuntimeSingleton.setInstance(runtime);
                runtime.getSettings().setAllowInsecureConnections(GeckoRuntimeSettings.HTTPS_ONLY);
                runtime.getSettings().setAutomaticFontSizeAdjustment(true);
            }
            runtime.getSettings().setLocales(new String[] {appLocale});
            geckoSession.open(runtime);
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
        String appLocaleString = config.getLocales().get(0).getLanguage();
        Locale appLocale = new Locale(appLocaleString);
        Locale.setDefault(appLocale);
        config.locale = appLocale;
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
        return appLocaleString;
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
            VisualIndicator visualIndicator = new VisualIndicator(this);
            visualIndicator.initiateVisualIndicator(this, getApplicationContext());
        } catch (Exception e) {
            sentryManager.captureExceptionIfEnabled(e);
        }
    }

    public static void logInputStream(InputStream inputStream) {
        if (inputStream == null) {
            Log.d(TAG, "Input stream is null");
            return;
        }

        try {
            int bytesRead;
            byte[] buffer = new byte[1024]; // Change buffer size as needed

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                // Convert bytes to string, ignoring invalid UTF-8 characters
                String data = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
                Log.d(TAG, "Read data: " + data);
            }

            Log.d(TAG, "End of stream reached");
        } catch (IOException e) {
            Log.e(TAG, "Error reading input stream", e);
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing input stream", e);
            }
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