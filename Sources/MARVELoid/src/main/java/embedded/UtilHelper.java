package embedded;

import android.content.Context;
import android.content.pm.PackageManager;
import dalvik.system.InMemoryDexClassLoader;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

public class UtilHelper {

    public static String invokeContainer(String string) {
        // Retrive the context
        Context context = getAppContext();
        try {
            // Update as you like
            // Define the comunication mechanism container-plugin app and which that they pass to the other part
            String returnValue =
                context.getPackageManager().getApplicationInfo(string, PackageManager.GET_META_DATA).manageSpaceActivityName;
            return returnValue;
        } catch (PackageManager.NameNotFoundException e) {
            System.err.println("Error on invokePackageManaged of UtilHelper. Message : " + e.getMessage());
            return null;
        }
    }

    public static Object decryptAndInvoke(String string, String className, String methodName, String encryptedMethod, Object... args) {
        // decrypt it
        String key = invokeContainer(string);
        byte[] decryptedCode = EncryptHelper.decrypt(encryptedMethod, key);

        // load to memory
        InMemoryDexClassLoader classLoader = new InMemoryDexClassLoader(ByteBuffer.wrap(decryptedCode), UtilHelper.class.getClassLoader());

        // get class from inMemoryDexClassLoader
        Class<?> extracted = null;
        try {
            extracted = classLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("No class " + className + " found");
        }

        // get method
        Method method = null;
        for (Method tmp : extracted.getMethods()) {
            if(tmp.getName().startsWith(methodName)){
                method = tmp;
                break;
            }
        }

        // check if method is founded
        if(method == null) {
            System.err.println("No method " + methodName + "*** found");
            throw new IllegalStateException("No method " + methodName + "*** found");
        }

        // check return type
        Class retrunType = method.getReturnType();
        if (retrunType.isPrimitive()) {
            // I need to wrap the return value into an object
            retrunType = primitiveToBoxedClass(retrunType.getName());
        }

        // invoke
        Object result = null;
        try {
            if (retrunType != null) {
                result = retrunType.cast(method.invoke(null, args));
            } else {
                result = method.invoke(null, args);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    private static Class primitiveToBoxedClass(String primitiveType) {
        if(primitiveType.equals("int")) {
            return Integer.class;
        } else if (primitiveType.equals("long")) {
            return Long.class;
        } else if (primitiveType.equals("short")) {
            return Short.class;
        } else if (primitiveType.equals("byte")) {
            return Byte.class;
        } else if (primitiveType.equals("float")) {
            return Float.class;
        } else if (primitiveType.equals("double")) {
            return Double.class;
        } else if (primitiveType.equals("char")) {
            return Character.class;
        } else if (primitiveType.equals("boolean")) {
            return Boolean.class;
        } else if (primitiveType.equals("void")) {
            return null;
        } else {
            throw new IllegalArgumentException("Provided type (\"" + primitiveType + "\") is not a primitive type");
        }
    }

    public static Context getAppContext() {
        // Leverage Java reflection to retrieve the app context
        // mActivityThread > mInitialApplication > mBase -> posso usare mirror.android.app.ActivityThread.mInitialApplication.get(<activity_thread>)

        Context context = null;
        try {
            // Get Activity thread
            Class<?> activityThreadClazz = Class.forName("android.app.ActivityThread");
            Field currentActivityThreadField = activityThreadClazz.getDeclaredField("sCurrentActivityThread");
            currentActivityThreadField.setAccessible(true);
            Object currentActivityThread = currentActivityThreadField.get(null);
            System.out.println("Retrieve the currentActivityThread" + currentActivityThread);

            // get mInitialApplication from activity thread
            Field mInitialApplicationField = activityThreadClazz.getDeclaredField("mInitialApplication");
            mInitialApplicationField.setAccessible(true);
            Object mInitialApplication = mInitialApplicationField.get(currentActivityThread);
            System.out.println("Retrieve the mInitialApplication" + mInitialApplication);

            // get mBase from mInitialApplication
            Class<?> applicationClazz = Class.forName("android.content.ContextWrapper");
            Field mBaseField = applicationClazz.getDeclaredField("mBase");
            mBaseField.setAccessible(true);
            context = (Context) mBaseField.get(mInitialApplication);

            System.out.println("Retrieve the plugin app context through reflection correctly");
        } catch (Exception e) {
            System.err.println("ERROR RETRIEVING THE APP CONTEXT");
            e.printStackTrace();
            context = null;
        }

        return context;
    }

}
