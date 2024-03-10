package com.doubleangels.nextdnsmanagement;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

import com.doubleangels.nextdnsmanagement.geckoruntime.GeckoRuntimeSingleton;
import com.doubleangels.nextdnsmanagement.protocoltest.VisualIndicator;

import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoRuntimeSettings;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoView;
import org.mozilla.geckoview.WebExtension;

import java.util.Locale;
import java.util.Objects;

import io.sentry.ITransaction;
import io.sentry.Sentry;

public class MainActivity extends AppCompatActivity {

    private static GeckoRuntime runtime;
    private GeckoSession geckoSession;
    private Boolean darkMode;

    @SuppressLint("WrongThread")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ITransaction mainActivityCreateTransaction = Sentry.startTransaction("MainActivity_onCreate()", "MainActivity");
        try {
            setupToolbar();
            String appLocale = setupLanguage();
            setupDarkMode();
            setupVisualIndicator();
            GeckoView geckoView = findViewById(R.id.geckoView);
            geckoSession = new GeckoSession();
            geckoSession.setContentDelegate(new GeckoSession.ContentDelegate() {});
            if (runtime == null) {
                runtime = GeckoRuntime.create(this);
                GeckoRuntimeSingleton.setInstance(runtime);
            }
            runtime.getSettings().setAllowInsecureConnections(GeckoRuntimeSettings.HTTPS_ONLY);
            runtime.getSettings().setAutomaticFontSizeAdjustment(true);
            runtime.getSettings().setLocales(new String[] {appLocale});
            geckoSession.open(runtime);
            geckoView.setSession(geckoSession);

            if (darkMode) {
                runtime.getWebExtensionController()
                        .ensureBuiltIn("resource://android/assets/darkmode/", "nextdns@doubleangels.com")
                        .accept(
                                extension -> Sentry.addBreadcrumb("WebExtension installed successfully!"),
                                e -> Sentry.addBreadcrumb("Error installing WebExtension: " + e)
                        );
            } else {
                String extensionId = "nextdns@doubleangels.com";
                runtime.getWebExtensionController().list().then(extensions -> {
                    boolean found = false;
                    assert extensions != null;
                    for (WebExtension extension : extensions) {
                        if (extension.id.equals(extensionId)) {
                            found = true;
                            runtime.getWebExtensionController().uninstall(extension)
                                    .then(result -> {
                                        Sentry.addBreadcrumb("WebExtension uninstalled successfully!");
                                        return null;
                                    })
                                    .exceptionally(throwable -> {
                                        Sentry.addBreadcrumb("Error uninstalling WebExtension: " + throwable);
                                        return null;
                                    });
                            break;
                        }
                    }
                    if (!found) {
                        Log.i("NextDNSManager", "WebExtension not found.");
                    }
                    return null;
                });
            }
            geckoSession.getSettings().setAllowJavascript(true);
            geckoSession.getSettings().setUseTrackingProtection(true);
            geckoSession.loadUri(getString(R.string.main_url));
        } catch (Exception e) {
            Sentry.captureException(e);
        } finally {
            mainActivityCreateTransaction.finish();
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
    }

    private String setupLanguage() {
        String appLocaleString = getResources().getConfiguration().getLocales().get(0).toString();
        String appLocaleStringResult = appLocaleString.split("_")[0];
        Locale appLocale = Locale.forLanguageTag(appLocaleStringResult);
        Locale.setDefault(appLocale);
        Configuration appConfig = new Configuration();
        appConfig.locale = appLocale;
        getResources().updateConfiguration(appConfig, getResources().getDisplayMetrics());
        return appLocaleStringResult;
    }

    private void setupDarkMode() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        darkMode = currentNightMode == Configuration.UI_MODE_NIGHT_YES;
    }

    private void setupVisualIndicator() {
        try {
            VisualIndicator visualIndicator = new VisualIndicator();
            visualIndicator.initiateVisualIndicator(this, getApplicationContext());
        } catch (Exception e) {
            Sentry.captureException(e);
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
