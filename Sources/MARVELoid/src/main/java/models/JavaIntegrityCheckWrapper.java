package models;

import utils.Random;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class JavaIntegrityCheckWrapper {
    private static final List<JavaIntegrityCheckWrapper> integrityChecks;
    static {
        integrityChecks = new ArrayList<>();

        // check virtual environment
        integrityChecks.add(new JavaIntegrityCheckWrapper("void checkVirtualEnvironment()", false, false, null));

        Argument packageName = new Argument();
        integrityChecks.add(
                new JavaIntegrityCheckWrapper("void checkAppSignature(android.content.Context,byte[],java.lang.String)", true, null,true, packageName));
    }

    public static JavaIntegrityCheckWrapper getRandomIntegrityCheck() {
        if (integrityChecks.size() <= 0)
            return null;

        int index = Random.getRandomFromTo(0, integrityChecks.size());
        return integrityChecks.get(index);
    }

    private final String signature;
    private final boolean useContext;
    private final boolean hasExpectedValue;
    private final boolean writeToFile;
    private final ExpectedValue expectedValue;
    private final Argument packageName;
    //private final Argument[] args;

    public JavaIntegrityCheckWrapper(String signature, boolean useContext, boolean writeToFile, Argument packageName) {
        this.signature = signature;
        this.useContext = useContext;
        this.hasExpectedValue = false;
        this.expectedValue = null;
        this.writeToFile = writeToFile;
        this.packageName = packageName;
    }

    public JavaIntegrityCheckWrapper(String signature, boolean useContext, ExpectedValue expectedValue, boolean writeToFile, Argument packageName) { // if expectedValue is null use random expected value
        this.signature = signature;
        this.useContext = useContext;
        this.hasExpectedValue = true;
        this.expectedValue = expectedValue;
        this.writeToFile = writeToFile;
        this.packageName = packageName;
    }

    public String getSignature() {
        return signature;
    }

    public boolean isUseContext() {
        return useContext;
    }

    public ExpectedValue getExpectedValue() {
        if (expectedValue == null) {
            return new ExpectedValue(Random.getRandomString().getBytes());
        }
        return expectedValue;
    }

    public Argument getPackageName() {
        return packageName;
    }

    public boolean isHasExpectedValue() {
        return hasExpectedValue;
    }

    public boolean isWriteToFile() {
        return writeToFile;
    }

    public static class Argument { // TODO: At the moment we support only string argument -> Update this class
        private final String className;
        private final String value;

        public Argument() {
            this.value = null;
            this.className = "java.lang.String";
        }

        public Argument(String value) {
            if (value == null)
                throw new IllegalArgumentException("This Argument constructor cannot be invoked with a null object");

            this.value = value;
            this.className = value.getClass().getName();
        }

        public String getClassName() {
            return className;
        }

        public String getValue() {
            if (this.value == null) {
                return Random.getRandomString();
            }

            return this.value;
        }
    }

    public static class ExpectedValue {
        public final byte[] value;
        public byte[] hash;

        public ExpectedValue(byte[] value) {
            this.value = value;

            MessageDigest md = null;
            try {
                md = MessageDigest.getInstance("SHA");
                md.update(value);
                this.hash = md.digest();
            } catch (NoSuchAlgorithmException e) {
                System.err.println("Error creating the random expected value");
                this.hash = null;
            }
        }
    }
}
