package com.lody.virtual.custom;

import android.content.Context;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;

public class Helper {

    /**
     * Read the byte from an asset resource
     *
     * @param context The context
     * @param assetName The target asset name
     *
     * @return the byte array if the resource exists, otherwise null
     */
    public static byte[] readBytesFromAsset(Context context, String assetName) {
        InputStream inputStream = null;
        try {
            inputStream = context.getAssets().open(assetName);

            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes);

            return bytes;
        } catch (IOException e) {
            // No asset with this name
            return null;
        }
    }

    /**
     * Read the byte from a file
     *
     * @param path The absolute path of a file
     *
     * @return the byte array if the resource exists, otherwise null
     */
    public static byte[] readBytesFromPath(String path) {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(new File(path));

            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes);

            return bytes;
        } catch (IOException e) {
            // No asset with this name
            return null;
        }
    }

    /**
     * This method read write the body bytes into a file with name filename in the private folder
     * [path_to_this_package]/files/pluginApps/
     *
     * @param context The context of the host app
     * @param fileName The target file name
     * @param body The bytes to be write in the target file
     *
     * @return The file path of the created app, otherwise null if some errors occurs
     */
    public static String writeFileOnInternalStorage(Context context, String fileName, String folderName, byte[] body){
        if (body == null)
            return null;

        File dir = new File(context.getFilesDir(), folderName);
        if(!dir.exists()){
            dir.mkdir();
        }

        String path = null;
        try {
            File file = new File(dir, fileName);

            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
            bos.write(body);
            bos.flush();
            bos.close();

            path = file.getAbsolutePath();
        } catch (Exception e){
            e.printStackTrace();
            path = null;
        }

        return path;
    }

    public static boolean removeFileOnInternalStorage(String path) {
        File file = new File(path);
        return file.delete();
    }

    public static String bytesToString(byte[] bytes) {
        StringBuilder md5StrBuff = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            if (Integer.toHexString(0xFF & bytes[i]).length() == 1) {
                md5StrBuff.append("0").append(Integer.toHexString(0xFF & bytes[i]));
            } else {
                md5StrBuff.append(Integer.toHexString(0xFF & bytes[i]));
            }
            if (bytes.length - 1 != i) {
                md5StrBuff.append(":");
            }
        }
        return md5StrBuff.toString();
    }

}
