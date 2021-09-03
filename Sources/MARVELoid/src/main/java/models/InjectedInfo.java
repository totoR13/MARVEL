package models;

public class InjectedInfo {
    private String packageNameValue;
    private byte[] expectedValue;

    public InjectedInfo(String packageNameValue, byte[] expectedValue) {
        this.packageNameValue = packageNameValue;
        this.expectedValue = expectedValue;
    }

    public String getPackageNameValue() {
        return packageNameValue;
    }

    public void setPackageNameValue(String packageNameValue) {
        this.packageNameValue = packageNameValue;
    }

    public byte[] getExpectedValue() {
        return expectedValue;
    }

    public void setExpectedValue(byte[] expectedValue) {
        this.expectedValue = expectedValue;
    }
}
