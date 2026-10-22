package com.doubleangels.nextdnsmanagement.sharedpreferences;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceDataStore;

/**
 * Backs the settings screen with {@link SharedPreferencesManager}. Without it,
 * PreferenceFragmentCompat persists switch and list values to the default
 * SharedPreferences file in plaintext and shows that copy instead of the
 * encrypted one.
 */
public final class SecurePreferenceDataStore extends PreferenceDataStore {

    @Override
    public void putString(String key, @Nullable String value) {
        SharedPreferencesManager.putString(key, value);
    }

    @Nullable
    @Override
    public String getString(String key, @Nullable String defValue) {
        return SharedPreferencesManager.getString(key, defValue);
    }

    @Override
    public void putBoolean(String key, boolean value) {
        SharedPreferencesManager.putBoolean(key, value);
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        return SharedPreferencesManager.getBoolean(key, defValue);
    }
}
