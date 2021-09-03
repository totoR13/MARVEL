package com.example.trustedcontainervirtualapp;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.lody.virtual.client.core.InstallStrategy;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.custom.Helper;
import com.lody.virtual.custom.models.CustomException;
import com.lody.virtual.remote.InstallResult;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String apkPath = "/sdcard/plugin.apk";
        System.out.println("Load file in path : " + apkPath);

        // Install and launch
        InstallResult result = VirtualCore.get().installPackage(apkPath, InstallStrategy.IGNORE_NEW_VERSION); // InstallStrategy.UPDATE_IF_EXIST

        // VirtualApp support multi-user-mode which can run multiple instances of a same app.
        // if you don't need this feature, just set `{userId}` to 0.
        Intent intent = VirtualCore.get().getLaunchIntent(result.packageName, 0);
        VActivityManager.get().startActivity(intent, 0);

        /* if (apkPath == null) {
            System.err.println("Error writing to the internal storage");
        } else {
            System.out.println("Load file in path : " + apkPath);

            // Install and launch
            try {
                InstallResult result = VirtualCore.get().installPackage(apkPath, InstallStrategy.IGNORE_NEW_VERSION); // InstallStrategy.UPDATE_IF_EXIST

                Log.d(TAG, "Install " + result.packageName + " result = " + result.isSuccess);

                if (result.isSuccess) {
                    Toast.makeText(this, "Install " + result.packageName + " installed correctly", Toast.LENGTH_LONG).show();

                    //VirtualApp support multi-user-mode which can run multiple instances of a same app.
                    //if you don't need this feature, just set `{userId}` to 0.
                    Intent intent = VirtualCore.get().getLaunchIntent(result.packageName, 0);
                    VActivityManager.get().startActivity(intent, 0);
                } else {
                    Toast.makeText(this, "Install " + result.packageName + " failed", Toast.LENGTH_LONG).show();
                }

            } catch (CustomException.SignatureException e) {
                Toast.makeText(this, "Target application do not match requirements constraints -> " + e.getMessage(), Toast.LENGTH_LONG).show();
            }

        }*/

    }

}