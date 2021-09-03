package com.lody.virtual.custom.models;

import java.security.SignatureException;

public abstract class CustomException extends RuntimeException {

    public CustomException(String message) {
        super(message);
    }

    // Signature Exceptions
    public static class SignatureException extends CustomException {
        public SignatureException(String message) {
            super(message);
        }
    }

    public static class InvalidSignatureException extends SignatureException {
        private final SignatureInfo signatureInfo;

        public InvalidSignatureException(String message, SignatureInfo signatureInfo) {
            super(message);
            this.signatureInfo = signatureInfo;
        }

        public SignatureInfo getSignatureInfo() {
            return signatureInfo;
        }
    }

    // Integrity Exceptions
    public static class ClassesDexIntegrityException extends CustomException {

        public ClassesDexIntegrityException(String message) {
            super(message);
        }
    }


}
