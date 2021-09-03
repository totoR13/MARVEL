package models;

import com.google.gson.Gson;
import utils.ManifestHelper;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class IntegrityInfo {
    private final String packageName;
    private final SignatureInfo signatureInfo;
    private final String classesDexSha256;

    public IntegrityInfo(byte[] signatureBytes, byte[] classesDexBytes) throws NoSuchAlgorithmException {
        this.packageName = ManifestHelper.getPackageName();

        this.signatureInfo = new SignatureInfo(signatureBytes);

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(classesDexBytes);
        byte[] byteArray = md.digest();
        this.classesDexSha256 = bytesToString(byteArray);
    }

    public String getPackageName() {
        return packageName;
    }

    public SignatureInfo getSignatureInfo() {
        return signatureInfo;
    }

    public String getClassesDexSha256() {
        return classesDexSha256;
    }

    public String getJson() {
        return new Gson().toJson(this);
    }

    /**
     * Create an hex string from a byte array
     *
     * @param bytes the input byte array
     *
     * @return the hex string
     */
    public static String bytesToString(byte[] bytes) {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            if (Integer.toHexString(0xFF & bytes[i]).length() == 1) {
                str.append("0").append(Integer.toHexString(0xFF & bytes[i]));
            } else {
                str.append(Integer.toHexString(0xFF & bytes[i]));
            }
            if (bytes.length - 1 != i) {
                str.append(":");
            }
        }
        return str.toString();
    }

    public static class SignatureInfo {
        private final String md5;
        private final String sha1;
        private final String sha256;

        public SignatureInfo(byte[] signatureBytes) throws NoSuchAlgorithmException {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(signatureBytes);
            byte[] byteArray = md.digest();
            this.md5 = bytesToString(byteArray);
            md.reset();

            md = MessageDigest.getInstance("SHA-1");
            md.update(signatureBytes);
            byteArray = md.digest();
            this.sha1 = bytesToString(byteArray);
            md.reset();

            md = MessageDigest.getInstance("SHA-256");
            md.update(signatureBytes);
            byteArray = md.digest();
            this.sha256 = bytesToString(byteArray);
        }

        public String getMd5() {
            return md5;
        }

        public String getSha1() {
            return sha1;
        }

        public String getSha256() {
            return sha256;
        }
    }

}
