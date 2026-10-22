package com.doubleangels.nextdnsmanagement.sharedpreferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.doubleangels.nextdnsmanagement.sentry.SentryManager;

import java.io.File;
import java.util.Map;

/**
 * A thread-safe utility class for managing encrypted SharedPreferences.
 * This class provides convenient static methods for reading and writing
 * preference values with encryption at rest.
 * All errors are captured using SentryManager.
 */
public class SharedPreferencesManager {

    // The name of the SharedPreferences file.
    private static final String PREF_NAME = "MyAppPreferences";

    // Key to track if migration from unencrypted to encrypted has been completed.
    private static final String MIGRATION_COMPLETE_KEY = "_encryption_migration_complete";

    // Prefix of the keys EncryptedSharedPreferences uses to store its own keysets.
    private static final String TINK_KEYSET_PREFIX = "__androidx_security_crypto_encrypted_prefs_";

    // Set when encrypted storage could not be initialized.
    private static final String ENCRYPTION_FALLBACK_KEY = "_encryption_fallback_active";

    // A reference to the SharedPreferences object, backed by the application
    // context.
    private static SharedPreferences sharedPreferences;

    // Store only the application context to avoid leaking an Activity context.
    private static Context appContext;

    private static volatile boolean usingEncryptedStorage = false;

    // Tag for logging.
    private static final String TAG = "SharedPreferencesManager";

    /**
     * Private constructor to prevent instantiation.
     * This class only offers static methods.
     */
    private SharedPreferencesManager() {
        throw new UnsupportedOperationException("Cannot instantiate SharedPreferencesManager.");
    }

    public static boolean isInitialized() {
        return sharedPreferences != null;
    }

    public static boolean isUsingEncryptedStorage() {
        return usingEncryptedStorage;
    }

    /**
     * Initializes the encrypted SharedPreferences object using the application
     * context.
     * Must be called once, usually in an Application class or before using any
     * put/get methods.
     * This method will:
     * 1. Create or retrieve an encrypted SharedPreferences instance
     * 2. Migrate existing unencrypted preferences if needed
     * 3. Fall back to unencrypted preferences if encryption fails
     *
     * @param context The context used to create or retrieve the SharedPreferences.
     */
    public static synchronized void init(Context context) {
        if (sharedPreferences == null) {
            appContext = context.getApplicationContext();
            try {
                // Create master key for encryption.
                MasterKey masterKey = new MasterKey.Builder(appContext)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build();

                // Create encrypted SharedPreferences.
                sharedPreferences = EncryptedSharedPreferences.create(
                        appContext,
                        PREF_NAME,
                        masterKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
                usingEncryptedStorage = true;

                // Check if migration from unencrypted to encrypted is needed.
                if (!sharedPreferences.getBoolean(MIGRATION_COMPLETE_KEY, false)) {
                    migrateUnencryptedPreferences(context);
                }
            } catch (Exception e) {
                handleEncryptionFailure(e);
            }
        }
    }

    private static void handleEncryptionFailure(Exception e) {
        Log.e(TAG, "Failed to initialize encrypted SharedPreferences; using plaintext fallback", e);
        SentryManager.captureException(e);

        sharedPreferences = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        usingEncryptedStorage = false;

        // App lock must not rely on plaintext storage; disable it until encryption works.
        sharedPreferences.edit()
                .putBoolean(ENCRYPTION_FALLBACK_KEY, true)
                .putBoolean("app_lock_enable", false)
                .apply();
    }

    /**
     * Migrates existing unencrypted SharedPreferences to encrypted storage.
     * This is a one-time operation that runs on first use of encrypted preferences.
     * <p>
     * EncryptedSharedPreferences stores its entries in the same
     * {@code PREF_NAME}.xml file as the old plaintext preferences, so the
     * plaintext entries are removed key by key. Deleting the file would also wipe
     * the encrypted entries that were just written.
     *
     * @param context The context used to access the unencrypted preferences.
     */
    private static void migrateUnencryptedPreferences(Context context) {
        try {
            File prefsFile = new File(
                    context.getApplicationInfo().dataDir + "/shared_prefs/" + PREF_NAME + ".xml");
            if (!prefsFile.exists()) {
                // No unencrypted preferences to migrate.
                sharedPreferences.edit().putBoolean(MIGRATION_COMPLETE_KEY, true).apply();
                return;
            }

            SharedPreferences unencryptedPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor encryptedEditor = sharedPreferences.edit();
            SharedPreferences.Editor plaintextEditor = unencryptedPrefs.edit();

            for (Map.Entry<String, ?> entry : unencryptedPrefs.getAll().entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (key.startsWith(TINK_KEYSET_PREFIX)) {
                    // The encryption keysets live in this file too; leave them alone.
                    continue;
                }
                if (value instanceof String) {
                    encryptedEditor.putString(key, (String) value);
                } else if (value instanceof Boolean) {
                    encryptedEditor.putBoolean(key, (Boolean) value);
                } else if (value instanceof Integer) {
                    encryptedEditor.putInt(key, (Integer) value);
                } else if (value instanceof Long) {
                    encryptedEditor.putLong(key, (Long) value);
                } else if (value instanceof Float) {
                    encryptedEditor.putFloat(key, (Float) value);
                } else {
                    continue;
                }
                plaintextEditor.remove(key);
            }

            // Write the encrypted copies before dropping the plaintext originals, and
            // mark migration complete only after a successful copy.
            encryptedEditor.putBoolean(MIGRATION_COMPLETE_KEY, true);
            if (encryptedEditor.commit()) {
                plaintextEditor.apply();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during preference migration; will retry on next launch", e);
            SentryManager.captureException(e);
        }
    }

    /**
     * Saves a String value into the SharedPreferences asynchronously.
     * If an error occurs, it is captured via SentryManager.
     *
     * @param key   The preference key under which the value is stored.
     * @param value The string value to store.
     */
    public static void putString(String key, String value) {
        checkInitialization();
        try {
            sharedPreferences.edit().putString(key, value).apply();
        } catch (Exception e) {
            SentryManager.captureException(e);
        }
    }

    /**
     * Retrieves a String value from the SharedPreferences.
     * If the key is not found or an error occurs, returns the provided default
     * value.
     *
     * @param key          The preference key to look for.
     * @param defaultValue The default value to return if not found.
     * @return The stored string value or the default if not found.
     */
    public static String getString(String key, String defaultValue) {
        checkInitialization();
        try {
            return sharedPreferences.getString(key, defaultValue);
        } catch (Exception e) {
            SentryManager.captureException(e);
            return defaultValue;
        }
    }

    /**
     * Saves a boolean value into the SharedPreferences asynchronously.
     * If an error occurs, it is captured via SentryManager.
     *
     * @param key   The preference key under which the value is stored.
     * @param value The boolean value to store.
     */
    public static void putBoolean(String key, boolean value) {
        checkInitialization();
        try {
            sharedPreferences.edit().putBoolean(key, value).apply();
        } catch (Exception e) {
            SentryManager.captureException(e);
        }
    }

    /**
     * Retrieves a boolean value from the SharedPreferences.
     * If the key is not found or an error occurs, returns the provided default
     * value.
     *
     * @param key          The preference key to look for.
     * @param defaultValue The default value to return if not found.
     * @return The stored boolean value or the default if not found.
     */
    public static boolean getBoolean(String key, boolean defaultValue) {
        checkInitialization();
        try {
            return sharedPreferences.getBoolean(key, defaultValue);
        } catch (Exception e) {
            SentryManager.captureException(e);
            return defaultValue;
        }
    }

    public static void putLong(String key, long value) {
        checkInitialization();
        try {
            sharedPreferences.edit().putLong(key, value).apply();
        } catch (Exception e) {
            SentryManager.captureException(e);
        }
    }

    public static long getLong(String key, long defaultValue) {
        checkInitialization();
        try {
            return sharedPreferences.getLong(key, defaultValue);
        } catch (Exception e) {
            SentryManager.captureException(e);
            return defaultValue;
        }
    }

    /**
     * A helper method that ensures the SharedPreferences has been initialized.
     * Throws an exception if init() has not been called beforehand.
     */
    private static void checkInitialization() {
        if (sharedPreferences == null) {
            throw new IllegalStateException(
                    "SharedPreferencesManager is not initialized. Call init() before using it.");
        }
    }
}
