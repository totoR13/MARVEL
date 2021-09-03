package com.lody.virtual.custom;

import android.util.Log;
import java.lang.reflect.Method;
import lab.galaxy.yahfa.HookMain;

public class CustomHookingReplacingMethods {
    private static final String TAG = CustomHookingReplacingMethods.class.getSimpleName();
    public static final Class myClass;


    static {
        myClass = CustomHookingReplacingMethods.class;
    }

    public static boolean performHooking(Class<?> targetClass, String targetMethodName, String targetMethodSignature,
                                         String hookClassName, String hookMethodName, ClassLoader classLoader) {
        // Check if we already hooked
        String key = targetClass + " <" + targetMethodSignature + " " + targetMethodName + ">";

        try {
            Method hook = null;
            if ((hook = CustomHookingReplacingMethods.getMethod(key)) == null) {
                // Load declared method
                Method[] declaredMethods = classLoader.loadClass(hookClassName).getDeclaredMethods();

                // NB: We suppose that this class contains only one method with name targetMethodName -> Ignore method signature
                // -> This is due to the Extractor module of Transformer project
                for (Method declaredMethod : declaredMethods) {
                    if (declaredMethod.getName().equals(hookMethodName)) {
                        hook = declaredMethod;
                        break;
                    }
                }

                CustomHookingReplacingMethods.addMethod(key, hook);
            }

            if (hook == null) {
                Log.e(TAG, "Cannot find method '" + hookMethodName + "' in class '" + hookClassName + "'.");
                throw new NoSuchMethodException("Cannot find method '" + hookMethodName + "' in class '" + hookClassName + "'.");
            }

            Log.e(TAG, "Hook method : " + hook.hashCode() + " - backup method  is null");

            HookMain.findAndBackupAndHook(targetClass, targetMethodName, targetMethodSignature, hook, null);
            return true;
        } catch (ClassNotFoundException | NoSuchMethodException e) { // | NoSuchFieldException | IllegalAccessException e) { // NB: multiple exception required minimum Android version 19
            Log.e(TAG, "Error hooking the target method : " + targetMethodName + " -> Message : " + e.getMessage());
            return false;
        }
    }

    public static Method getMethod(String methodName) {
        Log.d(TAG, "Get method : " + methodName);
        return CustomDexClassLoader.usedMethods.get(methodName);
    }

    public static void addMethod(String methodName, Method method) {
        if (!CustomDexClassLoader.usedMethods.containsKey(methodName)) {
            Log.d(TAG, "Add method : " + methodName);
            CustomDexClassLoader.usedMethods.put(methodName, method);
        }
    }

}
