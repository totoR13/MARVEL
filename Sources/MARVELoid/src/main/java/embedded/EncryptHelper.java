package embedded;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class EncryptHelper {

    private static byte[] padding(byte[] key, int size) {
        return Arrays.copyOf(key, size);
    }

    private static byte[] getKey(String secret) {
        MessageDigest sha = null;
        byte[] key;
        try {
            key = secret.getBytes("UTF-8");
            sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16);
            return key;
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String encrypt(byte[] strToEncrypt, Object secret) {
        byte[] key = getKey(secret.toString());
        return encrypt(strToEncrypt, key);
    }

    public static String encrypt(byte[] strToEncrypt, byte[] secret) {
        byte[] key = padding(secret, 16);
        try {
            SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return java.util.Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt));
            //return android.util.Base64.encodeToString(cipher.doFinal(strToEncrypt), android.util.Base64.DEFAULT);
        } catch (Exception e) {
            System.err.println("Error while encrypting: " + e.toString());
        }
        return null;
    }

    public static byte[] decrypt(String strToDecrypt, Object secret) {
        byte[] key = getKey(secret.toString());
        return decrypt(strToDecrypt, key);
    }

    public static byte[] decrypt(String strToDecrypt, byte[] secret) {
        byte[] key = padding(secret, 16);
        try {
            SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return cipher.doFinal(java.util.Base64.getDecoder().decode(strToDecrypt));
        } catch (Exception e) {
            System.err.println("Error while decrypting: " + e.toString());
        }
        return null;
    }

}
