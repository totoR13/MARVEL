package com.lody.virtual.custom;

import android.util.Log;

import com.google.gson.Gson;
import com.lody.virtual.custom.models.CustomException;
import com.lody.virtual.custom.models.IntegrityInfo;
import com.lody.virtual.custom.models.SignatureInfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Checker {
    private static final String TAG = Checker.class.getSimpleName();
    private static final Map<String, Map<String, List<String>>> validSignatures;
    private static final Map<String, Map<String, List<String>>> validClassesDex;

    static { // TODO: remove values from file and retrieve them from some server or encrypted resource
        validSignatures = new HashMap<>();
        validClassesDex = new HashMap<>();

        // complete validSignature from file
        BufferedReader br = null; // Upgrade this function in oder to increase the efficiency
        try {
            br = new BufferedReader(new FileReader(new File("/sdcard/integrityCheck")));
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            String line = null;
            while ((line = br.readLine()) != null) {
                IntegrityInfo integrityInfo = (new Gson()).fromJson(line, IntegrityInfo.class);

                Map<String, List<String>> mapSignature = new HashMap<>();
                mapSignature.put("md5", Collections.singletonList(integrityInfo.getSignatureInfo().getMd5()));
                mapSignature.put("sha1", Collections.singletonList(integrityInfo.getSignatureInfo().getSha1()));
                mapSignature.put("sha256", Collections.singletonList(integrityInfo.getSignatureInfo().getSha256()));
                validSignatures.put(integrityInfo.getPackageName(), mapSignature);

                Map<String, List<String>> mapClassesDex = new HashMap<>();
                mapClassesDex.put("all", Collections.singletonList(integrityInfo.getClassesDexSha256()));
                validClassesDex.put(integrityInfo.getPackageName(), mapClassesDex);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // load native library
        System.loadLibrary("native-lib");
    }

    private static native byte[] retriveSignatureBytes(String apkPath);

    private static native byte[] retriveClassesDexBytes(String apkPath);

    public static void checkApkSignature(String packageName, String apkPath) {
        Log.d(TAG, "Checking signature of apk: " + apkPath);

        Map<String, List<String>> map = null;
        if ((map = validSignatures.get(packageName)) == null)
            throw new CustomException.SignatureException("No supported package name " + packageName);

        byte[] signature = retriveSignatureBytes(apkPath);
        SignatureInfo signatureInfo = new SignatureInfo(signature);

        for (String algorithm : map.keySet()) { // Update this for loop
            String value;
            try {
                value = (String) SignatureInfo.class.getDeclaredField(algorithm).get(signatureInfo);
            } catch (Exception e) {
                value = null;
            }
            if (value == null || !map.get(algorithm).contains(value))
                throw new CustomException.InvalidSignatureException(packageName + " has signature for algoritm [" + algorithm + "] null or not valid", signatureInfo);
        }
    }

    /**
     * Check the sha256 signature of classes.dex file inside the apk at path apkPath.
     *
     * @param packageName The package name
     * @param apkPath The apk path
     */
    public static void checkClassesDexBytes(String packageName, String apkPath) {
        Log.d(TAG, "Checking signature of apk: " + apkPath);

        Map<String, List<String>> map = null;
        if ((map = validClassesDex.get(packageName)) == null)
            throw new CustomException.ClassesDexIntegrityException("No supported package name " + packageName);

        List<String> functionHashes = map.get("all");
        byte[] classesDexBytes = retrieveBytesOfFile(apkPath, "classes.dex");

        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA256");
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Error on hash algorithm. Message: " + e.getMessage());
            throw new CustomException.ClassesDexIntegrityException("No supported hash function");
        }
        md.update(classesDexBytes);
        String value = Helper.bytesToString(md.digest());

        if (functionHashes == null || !functionHashes.contains(value))
            throw new CustomException.ClassesDexIntegrityException("Error valuating the signature of classes.dex file of " + packageName);

    }

    /**
     * Read the bytes of the file in the apk
     *
     * @param apkPath the path of apk file
     * @param filename the target filename
     *
     * @return the bytes of the target file, otherwise null
     */
    private static byte[] retrieveBytesOfFile(String apkPath, String filename) {
        try {

            ZipFile zipFile = new ZipFile(apkPath);

            byte[] classesDexBytes = null;
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while(entries.hasMoreElements()){
                ZipEntry entry = entries.nextElement();
                if (entry.getName().equals(filename)) {
                    InputStream stream = zipFile.getInputStream(entry);
                    classesDexBytes = new byte[stream.available()];
                    stream.read(classesDexBytes);
                    break;
                }

            }
            return classesDexBytes;
        } catch (Exception e) {
            return null;
        }
    }

}
