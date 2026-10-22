package com.doubleangels.nextdnsmanagement.webview;

import android.net.Uri;

/**
 * Validates WebView-initiated download URLs before handing them to DownloadManager.
 */
public final class WebDownloadHelper {

    private WebDownloadHelper() {
    }

    /**
     * Returns true for HTTPS URLs. Plain HTTP is rejected because DownloadManager
     * runs in a system process and is not bound by this app's cleartext policy.
     */
    public static boolean isDownloadManagerSupportedUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        Uri uri = Uri.parse(url.trim());
        String scheme = uri.getScheme();
        return "https".equalsIgnoreCase(scheme);
    }
}
