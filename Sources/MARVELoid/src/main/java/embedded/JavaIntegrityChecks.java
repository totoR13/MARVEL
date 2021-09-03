package embedded;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class JavaIntegrityChecks {
    // NB: Injecting this check in plugin app, we suppose that the context is inserted always as first paramenter

    /**
     * Check if the virtual environment is the correct one.
     * At the moment this method checks (it is a PoC):
     * -) if check android.os.Handler.mCallback is instance of HCallbackStub
     * -) if ((Map)android.os.ServiceManager.sCache).get("activity") instance of BinderInvocationStub
     *
     */
    public static void checkVirtualEnvironment() {
        try {
            // Check the mCallback field of ActivityThread
            Class<?> activityThreadClazz = Class.forName("android.app.ActivityThread");
            Field currentActivityThreadField = activityThreadClazz.getDeclaredField("sCurrentActivityThread");
            currentActivityThreadField.setAccessible(true);
            Object currentActivityThread = currentActivityThreadField.get(null);

            Field mHField = activityThreadClazz.getDeclaredField("mH");
            mHField.setAccessible(true);
            Object handler = mHField.get(currentActivityThread);

            Class<?> handlerClazz = Class.forName("android.os.Handler");
            Field mCallbackField = handlerClazz.getDeclaredField("mCallback");
            mCallbackField.setAccessible(true);
            Object instance = mCallbackField.get(handler);

            if (!instance.getClass().getName().equals("com.lody.virtual.client.hook.proxies.am.HCallbackStub"))
                throw new RuntimeException("DEBUG: Proxy class (mCallback) is not the expected one. Class of type: " + handler.getClass().getName());

            // Check the sCache value
            Class<?> serviceManagerClazz = Class.forName("android.os.ServiceManager");
            Field sCacheField = serviceManagerClazz.getDeclaredField("sCache");
            sCacheField.setAccessible(true);
            Method getFromMap = Class.forName("java.util.Map").getMethod("get", Object.class);
            Object activity = getFromMap.invoke(sCacheField.get(null), "activity");

            // TODO: protect with hash functions like for checkAppSignature
            if (!activity.getClass().getName().equals("com.lody.virtual.client.hook.base.BinderInvocationStub"))
                throw new RuntimeException("DEBUG: Proxy class (sCache.get(\"activity\")) is not the expected one. Class of type: " + handler.getClass().getName());

            System.out.println("VirtualEnvironment integrity check performed correctly");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error on checkVirtualEnvironment.", e);
        }
    }

    /**
     * Check if the signature of app is equals to the expected value
     *
     * @param context Context of the app
     * @param expectedValue Expected sha1 of the certificate
     */
    public static void checkAppSignature(Context context, byte[] expectedValue, String packageName) {
        if (context != null) {
            PackageInfo packageInfo = null;
            try {
                packageInfo = context.getPackageManager()
                        .getPackageInfo(packageName, PackageManager.GET_SIGNATURES); // context.getPackageName() + ".my-test-package-name"
            } catch (PackageManager.NameNotFoundException e) {
                packageInfo = null;
            }

            // A Real signaure check
            if (packageInfo.signatures != null && packageInfo.signatures.length > 0) {
                Signature signature = packageInfo.signatures[0];
                byte[] signatureBytes = null;
                MessageDigest md = null;
                try {
                    md = MessageDigest.getInstance("SHA");
                    md.update(signature.toByteArray());
                    signatureBytes = md.digest();
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException("Error creating the sha hash");
                }

                if (signatureBytes.length == expectedValue.length) {
                    for (int i = 0; i < signatureBytes.length; i++) {
                        if (signatureBytes[i] == expectedValue[i])
                            continue;

                        throw new RuntimeException("DEBUG: Signature is not valid (remove this message)");
                    }

                    System.out.println("CheckSignature integrity check performed correctly");
                } else {
                    throw new RuntimeException("DEBUG: Signature is not valid (different length) (remove this message)");
                }
            } else {
                throw new RuntimeException("No signature found");
            }
        }
    }

}
