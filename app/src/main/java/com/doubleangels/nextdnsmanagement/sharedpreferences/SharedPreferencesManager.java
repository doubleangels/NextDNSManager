package com.doubleangels.nextdnsmanagement.sharedpreferences;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.doubleangels.nextdnsmanagement.sentry.SentryManager;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Thread-safe static store for app settings, encrypted at rest.
 * <p>
 * Each value is encrypted with AES-256-GCM using a non-exportable key held in
 * the Android Keystore. The preference key is bound in as associated data, so a
 * ciphertext copied from one entry to another (e.g. a stored {@code false} moved
 * onto {@code app_lock_enable}) fails authentication instead of being accepted.
 * Decrypted values are cached in memory, so reads never touch the Keystore.
 * <p>
 * This replaces the deprecated {@code EncryptedSharedPreferences}. Settings saved
 * by older versions are migrated once on first launch; see
 * {@link #migrateLegacyPreferences}.
 * <p>
 * If the Keystore is unusable, values are kept in a separate plaintext file and
 * app lock is forced off, since it must not rely on unencrypted storage.
 */
public final class SharedPreferencesManager {

    private static final String TAG = "SharedPreferencesManager";

    private static final String SECURE_PREFS_NAME = "secure_preferences";
    private static final String FALLBACK_PREFS_NAME = "fallback_preferences";
    // Plain boolean (not encrypted) marking that legacy settings were migrated.
    private static final String LEGACY_MIGRATED_KEY = "__legacy_migrated";

    private static final String KEYSTORE_PROVIDER = "AndroidKeyStore";
    private static final String KEY_ALIAS = "nextdns_manager_preferences";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    // Values are stored as "<type>:<value>" so a read with the wrong type falls
    // back to the default instead of misparsing.
    private static final char TYPE_STRING = 's';
    private static final char TYPE_BOOLEAN = 'b';
    private static final char TYPE_LONG = 'l';

    private static final String APP_LOCK_KEY = "app_lock_enable";

    // File and keys used by versions that stored settings with
    // EncryptedSharedPreferences, or in plaintext before that.
    private static final String LEGACY_PREFS_NAME = "MyAppPreferences";
    private static final Map<String, Character> LEGACY_KEYS = new LinkedHashMap<>();

    static {
        LEGACY_KEYS.put("dark_mode", TYPE_STRING);
        LEGACY_KEYS.put(APP_LOCK_KEY, TYPE_BOOLEAN);
        LEGACY_KEYS.put("sentry_enable", TYPE_BOOLEAN);
        LEGACY_KEYS.put("last_authenticated_time", TYPE_LONG);
        LEGACY_KEYS.put("last_webview_url", TYPE_STRING);
        LEGACY_KEYS.put("fcmToken", TYPE_STRING);
    }

    // Decrypted "<type>:<value>" strings by preference key.
    private static final Map<String, String> CACHE = new ConcurrentHashMap<>();

    // Volatile because init() runs on a background thread while isInitialized()
    // is polled from the main thread. Set last, once the cache is populated.
    private static volatile SharedPreferences storage;
    // Null when the Keystore is unavailable and the plaintext fallback is in use.
    private static volatile SecretKey secretKey;
    private static volatile boolean usingEncryptedStorage = false;

    private SharedPreferencesManager() {
    }

    public static boolean isInitialized() {
        return storage != null;
    }

    public static boolean isUsingEncryptedStorage() {
        return usingEncryptedStorage;
    }

    /**
     * Opens the store, loads it into memory, and migrates legacy settings on
     * first run. Safe to call repeatedly and from any thread; does Keystore and
     * disk I/O, so avoid calling it on the main thread.
     */
    public static synchronized void init(Context context) {
        if (storage != null) {
            return;
        }
        Context appContext = context.getApplicationContext();
        SharedPreferences prefs;
        try {
            secretKey = getOrCreateKey();
            prefs = appContext.getSharedPreferences(SECURE_PREFS_NAME, Context.MODE_PRIVATE);
            usingEncryptedStorage = true;
        } catch (Exception e) {
            Log.e(TAG, "Android Keystore unavailable; using plaintext fallback", e);
            SentryManager.captureException(e);
            secretKey = null;
            prefs = appContext.getSharedPreferences(FALLBACK_PREFS_NAME, Context.MODE_PRIVATE);
            usingEncryptedStorage = false;
        }

        loadCache(prefs);
        if (!prefs.getBoolean(LEGACY_MIGRATED_KEY, false)) {
            migrateLegacyPreferences(appContext, prefs);
        }
        if (!usingEncryptedStorage) {
            write(prefs, APP_LOCK_KEY, encode(TYPE_BOOLEAN, "false"));
        }
        storage = prefs;
    }

    public static void putString(String key, String value) {
        put(key, value == null ? null : encode(TYPE_STRING, value));
    }

    public static String getString(String key, String defaultValue) {
        String value = get(key, TYPE_STRING);
        return value != null ? value : defaultValue;
    }

    public static void putBoolean(String key, boolean value) {
        put(key, encode(TYPE_BOOLEAN, Boolean.toString(value)));
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = get(key, TYPE_BOOLEAN);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }

    public static void putLong(String key, long value) {
        put(key, encode(TYPE_LONG, Long.toString(value)));
    }

    public static long getLong(String key, long defaultValue) {
        String value = get(key, TYPE_LONG);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static void put(String key, String encoded) {
        SharedPreferences prefs = requireStorage();
        if (encoded == null) {
            CACHE.remove(key);
            prefs.edit().remove(key).apply();
            return;
        }
        if (!usingEncryptedStorage && APP_LOCK_KEY.equals(key)) {
            // App lock must not rely on plaintext storage.
            encoded = encode(TYPE_BOOLEAN, "false");
        }
        write(prefs, key, encoded);
    }

    private static String get(String key, char type) {
        requireStorage();
        String encoded = CACHE.get(key);
        if (encoded == null || encoded.length() < 2 || encoded.charAt(0) != type || encoded.charAt(1) != ':') {
            return null;
        }
        return encoded.substring(2);
    }

    private static SharedPreferences requireStorage() {
        SharedPreferences prefs = storage;
        if (prefs == null) {
            throw new IllegalStateException(
                    "SharedPreferencesManager is not initialized. Call init() before using it.");
        }
        return prefs;
    }

    private static String encode(char type, String value) {
        return type + ":" + value;
    }

    /**
     * Updates the cache and persists the value. Skips the Keystore round trip and
     * disk write when the value is unchanged (e.g. the same URL on every page load).
     */
    private static void write(SharedPreferences prefs, String key, String encoded) {
        if (encoded.equals(CACHE.get(key))) {
            return;
        }
        CACHE.put(key, encoded);
        try {
            prefs.edit().putString(key, seal(key, encoded)).apply();
        } catch (Exception e) {
            Log.e(TAG, "Failed to persist preference " + key, e);
            SentryManager.captureException(e);
        }
    }

    private static void loadCache(SharedPreferences prefs) {
        SharedPreferences.Editor cleanup = null;
        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            if (!(entry.getValue() instanceof String)) {
                continue;
            }
            String key = entry.getKey();
            try {
                CACHE.put(key, open(key, (String) entry.getValue()));
            } catch (Exception e) {
                // Unreadable (e.g. the Keystore key was lost); drop it so the
                // default applies instead of failing on every launch.
                Log.w(TAG, "Dropping unreadable preference " + key, e);
                if (cleanup == null) {
                    cleanup = prefs.edit();
                }
                cleanup.remove(key);
            }
        }
        if (cleanup != null) {
            cleanup.apply();
        }
    }

    private static String seal(String key, String plaintext) throws GeneralSecurityException {
        SecretKey keyToUse = secretKey;
        if (keyToUse == null) {
            return plaintext;
        }
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, keyToUse);
        cipher.updateAAD(key.getBytes(StandardCharsets.UTF_8));
        byte[] iv = cipher.getIV();
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        byte[] combined = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
        return Base64.encodeToString(combined, Base64.NO_WRAP);
    }

    private static String open(String key, String stored) throws GeneralSecurityException {
        SecretKey keyToUse = secretKey;
        if (keyToUse == null) {
            return stored;
        }
        byte[] combined = Base64.decode(stored, Base64.NO_WRAP);
        if (combined.length <= GCM_IV_LENGTH) {
            throw new GeneralSecurityException("Stored value is too short");
        }
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, keyToUse,
                new GCMParameterSpec(GCM_TAG_LENGTH_BITS, combined, 0, GCM_IV_LENGTH));
        cipher.updateAAD(key.getBytes(StandardCharsets.UTF_8));
        byte[] plaintext = cipher.doFinal(combined, GCM_IV_LENGTH, combined.length - GCM_IV_LENGTH);
        return new String(plaintext, StandardCharsets.UTF_8);
    }

    private static SecretKey getOrCreateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
        keyStore.load(null);
        Key existing = keyStore.getKey(KEY_ALIAS, null);
        if (existing instanceof SecretKey) {
            return (SecretKey) existing;
        }
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER);
        generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build());
        return generator.generateKey();
    }

    /**
     * One-time copy of settings saved by older versions, then removal of the old
     * files and key.
     * <p>
     * Older versions used EncryptedSharedPreferences in {@code MyAppPreferences},
     * and before that plain SharedPreferences in the same file. Both are read with
     * typed getters for the known keys only: the file also holds Tink keysets and
     * encrypted key names, so blindly copying {@code getAll()} would import junk.
     * The preference screen also used to mirror its switches into the default
     * SharedPreferences file in plaintext; that copy is deleted, not migrated.
     * <p>
     * Nothing is deleted unless the migrated values were committed, so a failure
     * just retries on the next launch.
     */
    // commit() is deliberate: the old files are deleted only after this is on disk.
    @SuppressLint("ApplySharedPref")
    @SuppressWarnings("deprecation")
    private static void migrateLegacyPreferences(Context appContext, SharedPreferences prefs) {
        try {
            File legacyFile = new File(appContext.getApplicationInfo().dataDir,
                    "shared_prefs/" + LEGACY_PREFS_NAME + ".xml");
            if (legacyFile.exists()) {
                SharedPreferences legacyEncrypted = openLegacyEncryptedPreferences(appContext);
                SharedPreferences legacyPlain = appContext.getSharedPreferences(LEGACY_PREFS_NAME,
                        Context.MODE_PRIVATE);
                for (Map.Entry<String, Character> legacyKey : LEGACY_KEYS.entrySet()) {
                    String key = legacyKey.getKey();
                    if (CACHE.containsKey(key)) {
                        continue;
                    }
                    String encoded = readLegacy(legacyEncrypted, key, legacyKey.getValue());
                    if (encoded == null) {
                        encoded = readLegacy(legacyPlain, key, legacyKey.getValue());
                    }
                    if (encoded != null) {
                        write(prefs, key, encoded);
                    }
                }
            }
            if (!prefs.edit().putBoolean(LEGACY_MIGRATED_KEY, true).commit()) {
                return;
            }
        } catch (Exception e) {
            Log.e(TAG, "Legacy preference migration failed; will retry on next launch", e);
            SentryManager.captureException(e);
            return;
        }

        appContext.deleteSharedPreferences(LEGACY_PREFS_NAME);
        appContext.deleteSharedPreferences(appContext.getPackageName() + "_preferences");
        try {
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
            keyStore.load(null);
            keyStore.deleteEntry(MasterKey.DEFAULT_MASTER_KEY_ALIAS);
        } catch (Exception e) {
            Log.w(TAG, "Failed to delete legacy master key", e);
        }
    }

    @SuppressWarnings("deprecation")
    private static SharedPreferences openLegacyEncryptedPreferences(Context appContext) {
        try {
            MasterKey masterKey = new MasterKey.Builder(appContext)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            return EncryptedSharedPreferences.create(
                    appContext,
                    LEGACY_PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        } catch (Exception e) {
            Log.w(TAG, "Legacy encrypted preferences unreadable; trying plaintext", e);
            return null;
        }
    }

    private static String readLegacy(SharedPreferences source, String key, char type) {
        if (source == null) {
            return null;
        }
        try {
            if (!source.contains(key)) {
                return null;
            }
            switch (type) {
                case TYPE_BOOLEAN:
                    return encode(type, Boolean.toString(source.getBoolean(key, false)));
                case TYPE_LONG:
                    return encode(type, Long.toString(source.getLong(key, 0L)));
                default:
                    String value = source.getString(key, null);
                    return value == null ? null : encode(type, value);
            }
        } catch (Exception e) {
            // Wrong type or undecryptable entry; skip it.
            return null;
        }
    }
}
