package com.lody.virtual.custom.models;

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

    public String getClassName() {
        return this.bytecodeSignature.split(":")[0];
    }

    public String getMethodName() {
        return this.bytecodeSignature.split(":")[1].split("\\(")[0].trim();
    }

    public String getMethodSignature() {
        return "(" + this.bytecodeSignature.split("\\(")[1].trim();
    }

    public String getFolder() {
        return folder;
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }
}
