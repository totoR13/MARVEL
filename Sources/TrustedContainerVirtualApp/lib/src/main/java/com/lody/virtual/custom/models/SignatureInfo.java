package com.lody.virtual.custom.models;

import com.lody.virtual.custom.Helper;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class SignatureInfo {
    Principal subjectDN;
    Principal issuerDN;
    public final String serialNumber;
    public final String md5;
    public final String sha1;
    public final String sha256;

    public SignatureInfo(byte[] signature) throws IllegalArgumentException {
        if (signature == null)
            throw new IllegalArgumentException("Signature is null");

        InputStream certStream = new ByteArrayInputStream(signature);
        try {
            CertificateFactory certFactory = CertificateFactory.getInstance("X509");
            X509Certificate x509Cert = (X509Certificate) certFactory.generateCertificate(certStream);

            subjectDN = x509Cert.getSubjectDN();
            issuerDN = x509Cert.getIssuerDN();
            serialNumber = x509Cert.getSerialNumber().toString();

            // Retrieve hashes
            MessageDigest md;
            try {
                md = MessageDigest.getInstance("MD5");
                md.update(signature);
                byte[] byteArray = md.digest();
                md5 = Helper.bytesToString(byteArray);
                md.reset();

                md = MessageDigest.getInstance("SHA");
                md.update(signature);
                byteArray = md.digest();
                sha1 = Helper.bytesToString(byteArray);
                md.reset();

                md = MessageDigest.getInstance("SHA256");
                md.update(signature);
                byteArray = md.digest();
                sha256 = Helper.bytesToString(byteArray);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalArgumentException("No valid hash algorithm");
            }

        } catch (CertificateException e) {
            throw new IllegalArgumentException("No valid signature bytes");
        }

    }

}
