package com.blackberrykeyboard.dict;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;

/**
 * Extracts open-source compressed dictionaries from assets on first run.
 * Sources: FrequencyWords (MIT) for es/en, bundled names list.
 */
public final class DictionaryLoader {
    private static final String TAG = "DictionaryLoader";
    private static final String[] ASSETS = {"es.txt.gz", "en.txt.gz", "names.txt.gz"};

    private DictionaryLoader() {
    }

    public static File ensureDictionaries(Context context) {
        File dictDir = new File(context.getFilesDir(), "dict");
        if (!dictDir.exists() && !dictDir.mkdirs()) {
            Log.w(TAG, "Could not create dict directory");
        }

        for (String asset : ASSETS) {
            File out = new File(dictDir, asset.replace(".gz", ""));
            if (!out.exists() || out.length() == 0) {
                extractGzipAsset(context, "dict/" + asset, out);
            }
        }
        return dictDir;
    }

    private static void extractGzipAsset(Context context, String assetPath, File outFile) {
        try (InputStream raw = context.getAssets().open(assetPath);
             GZIPInputStream gzip = new GZIPInputStream(raw);
             OutputStream out = new FileOutputStream(outFile)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = gzip.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            Log.i(TAG, "Extracted " + assetPath + " -> " + outFile.length() + " bytes");
        } catch (IOException e) {
            Log.e(TAG, "Failed to extract " + assetPath, e);
            if (outFile.exists()) {
                outFile.delete();
            }
        }
    }
}
