package models;

public class HookInfo {
    private String newClassName;
    private String bytecodeSignature;
    private String folder;

    public HookInfo(String newClassName, String bytecodeSignature, String folder) {
        this.newClassName = newClassName;
        this.bytecodeSignature = bytecodeSignature;
        this.folder = folder;
    }

    public String getNewClassName() {
        return newClassName;
    }

    public void setNewClassName(String newClassName) {
        this.newClassName = newClassName;
    }

    public String getBytecodeSignature() {
        return bytecodeSignature;
    }

    public void setBytecodeSignature(String bytecodeSignature) {
        this.bytecodeSignature = bytecodeSignature;
    }

    public String getFolder() {
        return folder;
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }
}
