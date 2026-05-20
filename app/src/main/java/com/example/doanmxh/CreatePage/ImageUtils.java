package com.example.doanmxh.CreatePage;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImageUtils {

    public static File uriToFile(Context context, Uri uri) throws IOException {

        InputStream inputStream =
                context.getContentResolver().openInputStream(uri);

        File file = new File(
                context.getCacheDir(),
                "imgbb_" + System.currentTimeMillis() + ".jpg"
        );

        FileOutputStream outputStream = new FileOutputStream(file);

        byte[] buffer = new byte[4096];
        int len;

        while ((len = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, len);
        }

        outputStream.close();
        inputStream.close();

        return file;
    }
}