package com.lody.virtual.custom.models;

public class IntegrityInfo {
    private String packageName;
    private SignatureInfo signatureInfo;
    private String classesDexSha256;

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public SignatureInfo getSignatureInfo() {
        return signatureInfo;
    }

    public void setSignatureInfo(SignatureInfo signatureInfo) {
        this.signatureInfo = signatureInfo;
    }

    public String getClassesDexSha256() {
        return classesDexSha256;
    }

    public void setClassesDexSha256(String classesDexSha256) {
        this.classesDexSha256 = classesDexSha256;
    }

    public static class SignatureInfo {
        private String md5;
        private String sha1;
        private String sha256;

        public String getMd5() {
            return md5;
        }

        public void setMd5(String md5) {
            this.md5 = md5;
        }

        public String getSha1() {
            return sha1;
        }

        public void setSha1(String sha1) {
            this.sha1 = sha1;
        }

        public String getSha256() {
            return sha256;
        }

        public void setSha256(String sha256) {
            this.sha256 = sha256;
        }
    }

}
