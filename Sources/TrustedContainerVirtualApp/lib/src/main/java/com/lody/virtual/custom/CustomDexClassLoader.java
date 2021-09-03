package com.lody.virtual.custom;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.custom.models.HookInfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;

public class CustomDexClassLoader extends DexClassLoader {
    private static final String TAG = CustomDexClassLoader.class.getSimpleName();
    private final String pluginPackageName;
    private static final List<String> hookedClasses = new ArrayList<>();
    private final List<String> inResolutionClasses = new ArrayList<>();

    public static final Map<String, Method> usedMethods = new HashMap<>();
    public static final List<String> hookedMethods = new ArrayList<>();

    public CustomDexClassLoader(String pluginPackageName, String dexPath, String optimizedDirectory, String librarySearchPath, ClassLoader parent) {
        super(dexPath, optimizedDirectory, librarySearchPath, parent);
        this.pluginPackageName = pluginPackageName;
    }

    @Override
    protected Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {
        Log.d(TAG, "ClassLoader loading class " + className);

        // Perform hooking
        synchronized (inResolutionClasses) {
            inResolutionClasses.add(className);
        }
        Class<?> clazz = super.loadClass(className, resolve);
        synchronized (inResolutionClasses) {
            if (Collections.frequency(inResolutionClasses, className) == 1 && className.contains(pluginPackageName))
                checkIfHook(clazz);

            inResolutionClasses.remove(inResolutionClasses.size()-1);
        }

        return clazz;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        return super.findClass(name);
    }


    // TODO: we can setup a server which expose a rest api responsible to return the corret HookInfo object
    private void checkIfHook(Class<?> clazz) {
        Log.d(TAG, "ClassLoader check if hook class " + clazz.getName());
        synchronized (hookedClasses) {
            hookedClasses.add(clazz.getName());
        }

        BufferedReader br = null; // Upgrade this function in oder to increase the efficiency
        try {
            br = new BufferedReader(new FileReader(new File("/sdcard/" + pluginPackageName + "/signatures")));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        try {
            String line = null;
            while ((line = br.readLine()) != null) {
                // parse line
                HookInfo infos = (new Gson()).fromJson(line, HookInfo.class);

                String className = infos.getClassName();
                Log.i(TAG, "Comparing class " + className + " with " + clazz.getCanonicalName() + " - or is better " + clazz.getName());
                if (className == null || !className.equals(clazz.getName())) {
                    continue;
                }

                String newClassName = infos.getNewClassName();
                String methodName = infos.getMethodName();

                // signature Example : (Ljava/lang/String;)V
                String methodSignature = infos.getMethodSignature();

                // TODO: add a more sophisticate logic
                Context hostContext = VirtualCore.get().getContext();
                String dexPath =
                        Helper.writeFileOnInternalStorage(hostContext, "test.dex", "tmp",
                                Helper.readBytesFromPath("/sdcard/" + pluginPackageName + "/" + infos.getFolder() + "/classes.dex"));

                // There is some error on reading the dex file
                if (dexPath == null) {
                    Log.e(TAG, "Some error occurs reading the dex file from asset");
                    continue;
                }

                ClassLoader classLoader = new PathClassLoader(dexPath, this);

                boolean result = CustomHookingReplacingMethods.performHooking(clazz, methodName,
                        methodSignature, newClassName, "hook", classLoader);

                Log.d(TAG, (result)
                        ? "Hooking of method " + methodName + " performed CORRECTLY"
                        : "An error occurred during the hooking of method " + methodName);

                Helper.removeFileOnInternalStorage(dexPath);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error on hooking or on creating the class loader. Error: " + e.getMessage());
        }
    }
}
