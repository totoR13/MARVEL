# MARVELoid Example

We show an example of how the MARVELoid protection works on [br.com.cjdinfo.puzzle](https://apkpure.com/it/puzzle/br.com.cjdinfo.puzzle) Android app.

## Prerequisites

To reproduce the following example, you need all [MARVELoid](../Sources/MARVELoid/README.md) and [TC](../Sources/TrustedContainerVirtualApp/README.md) prerequisites:
* Android SDK;
* Android device or emulator;
* Java 8 or newer (we suggest openjdk 11).


In addition, you need:
* *adb* tool. It is part of the official Android SDK (on an Ubuntu machine, it is available at `~/Android/Sdk/platform-tools/adb`). <br>
We suggest to follow the official Android documentation for [adb](https://developer.android.com/studio/command-line/adb?gclid=CjwKCAjwybyJBhBwEiwAvz4G79AMFHFp4eF3ZGCEZAlnvCwgtde1nNAMimEF-zTrGIDIeeSqyEFCrhoCLqQQAvD_BwE&gclsrc=aw.ds);

* *jarsigner* tool. It is part of the java JDK.

## Example

Below are the steps to reproduce the MARVELoid protection process and how to execute the protected app in an Android emulator/device (Android API 26+).

1. Run MARVELoid to protect the puzzle app
```console
$ cd </path/to/MARVELoid>/Example
$ java -jar ../Binaries/MARVELoid-1.0.jar -o ${PWD}/marveloid_output -re true -ri true -rp true -a </path/to/android_jar/platforms> -en 10 -ex 10 -in 10 -i ./br.com.cjdinfo.puzzle.apk -j </path/to/jarsigner> -k ./my-release-key.keystore 
[...]
Output apk signed correctly with SHA1withRSA
Output apk signed correctly with SHA256withRSA
Protection statistic: 
-) Number of extracted methods: 4;
-) Number of encrypted methods: 5;
-) Number of injected AT controls: 3;
```

2. Install the TC app and grant the needed permission to the TC app (read and write from external storage)
```console
$ adb install ../Binaries/trusted-container.apk
Performing Streamed Install
Success
$ adb shell pm grant com.example.trustedcontainervirtualapp android.permission.READ_EXTERNAL_STORAGE
$ adb shell pm grant com.example.trustedcontainervirtualapp android.permission.WRITE_EXTERNAL_STORAGE
```

3. Setup of the plugin app metadata
```console
$ adb shell mkdir /sdcard/br.com.cjdinfo.puzzle
$ adb push marveloid_output/* /sdcard/br.com.cjdinfo.puzzle/
marveloid_output/226a567711094a13b61707e7ec715686/: 1 file pushed. 0.0 MB/s (744 bytes in 0.036s)
marveloid_output/96ffa9ad807242e39e9f9567bba6b09b/: 1 file pushed. 0.0 MB/s (788 bytes in 0.027s)
marveloid_output/b268270f972846c497c82de535f73158/: 1 file pushed. 0.1 MB/s (1388 bytes in 0.024s)
marveloid_output/br.com.cjdinfo.puzzle.apk: 1 file pushed. 67.2 MB/s (2219137 bytes in 0.031s)
marveloid_output/d6b0e76f38674d2b976cef233d84db83/: 1 file pushed. 0.0 MB/s (1116 bytes in 0.032s)
marveloid_output/injectorDetails: 1 file pushed. 0.1 MB/s (438 bytes in 0.005s)
marveloid_output/integrityCheck: 1 file pushed. 0.1 MB/s (406 bytes in 0.005s)
marveloid_output/protectorDetails: 1 file pushed. 0.1 MB/s (485 bytes in 0.007s)
marveloid_output/signatures: 1 file pushed. 0.2 MB/s (916 bytes in 0.004s)
9 files pushed. 11.7 MB/s (2225418 bytes in 0.181s)
$ adb push marveloid_output/br.com.cjdinfo.puzzle.apk /sdcard/plugin.apk
marveloid_output/br.com.cjdinfo.puzzle.apk: 1 file pushed. 46.5 MB/s (2219137 bytes in 0.046s)
$ adb push marveloid_output/integrityCheck /sdcard/integrityCheck
marveloid_output/integrityCheck: 1 file pushed. 0.1 MB/s (406 bytes in 0.003s)
$ adb push marveloid_output/injectorDetails /sdcard/injectorDetails
marveloid_output/injectorDetails: 1 file pushed. 0.2 MB/s (438 bytes in 0.003s)
$ adb push marveloid_output/protectorDetails /sdcard/protectorDetails
marveloid_output/protectorDetails: 1 file pushed. 0.2 MB/s (485 bytes in 0.003s)
$
```

4. Excute the TC app from the emulator/device and enjoy the protected puzzle app <br> *Note*: Due to the virtualization (i.e., VirtualApp framework), the first execution of the TC could take several seconds.