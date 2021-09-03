# TrustedContainerVirtualApp

This folder contains the Trusted Container (TC) used during the PoC of the MARVEL schema.
The container app leverages the [VirtualApp](https://github.com/asLody/VirtualApp) framework to implement the virtualization in the Android ecosystem.

> **Note**: We leverage the github version of the VirtualApp framework. Unfortunately, to access the latest version of the VirtualApp source code (with the full support of the latest Android OS versions), a commercial license is required. **Due to this, we suggest to execute the TC in an Android emulator/device running Android 8 (API 26 or 27).**

## Implementation highlight
In this section, we briefly recap the main modification performed on the VirtualApp framework to implement the MARVEL schema.

* The `com.lody.virtual.server.pm.VAppManagerService.installPackage` method is responsible to install a plugin app. We update this method to verify the trustfulness of the plugin app that will be installed (e.g., its signature);
* The `com.lody.virtual.client.VClientImpl.bindApplicationNoCheck` method is responsible to create the process and the class loader of the plugin app. We modified this method to inject a custom class loader;
* We modified some classes in `com.lody.virtual.client.hook.proxies.pm.MethodProxies` to implement the IATs TC-side;
* The package `com.lody.virtual.custom` contains all the MARVELoid classes. In particular, we highlight the `CustomDexClassLoader` and the logic to perform the ART instrumentation, and the `Checker` to perform the integrity check on the plugin apps.

The container app leverages the ART instrumentation to replace the correct body of the splitted methods of the plugin app. To implement the ART instrumentation, we leverage the [YAHFA](https://github.com/PAGalaxyLab/YAHFA) framework.

## Prerequisites

* *IDE*: To build the Trusted Container Android app, you need the [Android SDK](https://developer.android.com/studio?gclid=CjwKCAjwybyJBhBwEiwAvz4G73ajtfmlbQR5KTuTtNv0qc0nwcE3aN_w7izyhD1ryYqv3YYyuhgI2hoCZo8QAvD_BwE&gclsrc=aw.ds) for Android API 26 or newer. 
Moreover, we suggest to download the Android Studio IDE. <br>
We use Android Studio 4.2.2 - Build #AI-202.7660.26.42.7486908, built on June 24, 2021. 

* *Android Device/Emulator*: To execute the TC app, you need a real Android device, or an emulator. <br>
To create an emulator, we suggest the AVD Manager integrated in Android Studio (at `Available devices > AVD Manager`), or [Genymotion](https://www.genymotion.com/). <br>
The TC is built on the github version of the VirtualApp framework. Unfortunately, to access the latest version of the VirtualApp source code (with the full support of the latest Android OS versions), a commercial license is required. **Due to this, we suggest to execute the TC in an Android emulator/device running Android 8 (API 26 or 27).**

## Building

You can open the TC project directly from the Android Studio.

To build a signed apk from Android Studio: 

1. `Build > Generate Signed Bundle or APK`;
2. Select `APK`;
3. Insert an existing keystore or Create a new one;
4. Specify the destination folder, the build variant (e.g., release), and the signature version (e.g., V2 (Full APK Signature)). 

A latest version of the TC Android app is available in the [Binaries folder](../../Binaries).

## Runtime Setup
In the current implementation, the TC reads the metadata files (output of the MARVELoid tool) from the `/sdcard` folder of the emulator/device.
> **Note** In a real scenario, the TC may download such information from a remote, trusted server in a secure way.

In order to run a new protected application, follow these steps:

* Install the TC Android app into the emulator;
* Copy the `integrityCheck`, `injectorDetails`, `protectorDetails` files into the `/sdcard/` folder of the emulator/device;
* Create a folder in the `/sdcard` folder of the emulator/device, named with the package name of the plugin app (i.e., `/sdcard/<package_name_plugin_app>`);
* Copy the `signatures` file and the folders with the extracted methods into the `/sdcard/<package_name_plugin_app>` folder of the emulator/device;
* Copy the protected apk into `/sdcard/pugin.apk` file of the emulator/device;
* Run the TC app and enjoy the protected plugin app!

See the [Example](../../Example/README.md) for a complete use-case.

