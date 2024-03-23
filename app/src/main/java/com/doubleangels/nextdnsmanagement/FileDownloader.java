package com.doubleangels.nextdnsmanagement;

import android.os.Environment;
import android.widget.Toast;
import android.content.Context;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class FileDownloader {

    private Context context;

    public FileDownloader(Context context) {
        this.context = context;
    }

    public void downloadFileFromInputStream(InputStream inputStream) {
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File file = new File(dir, "NextDNS.mobileconfig");
        OutputStream outputStream = null;

        try {
            outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                // Convert bytes to string, removing non-printable characters
                String data = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8)
                        .replaceAll("[^\\x20-\\x7e]", ""); // Remove non-printable characters
                byte[] printableBytes = data.getBytes(StandardCharsets.UTF_8);
                outputStream.write(printableBytes, 0, printableBytes.length);
            }
            Toast.makeText(context, "Downloaded file to downloads!", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Error downloading file!", Toast.LENGTH_LONG).show();
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
