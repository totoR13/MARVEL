# MARVELoid

MARVELoid is responsible to transform (i.e., protect) the input apk file which could be executed in the Trusted Container App (i.e., the [TrustedContainerVirtualApp](../TrustedContainerVirtualApp/README.md) project)

## Implementation details

The source code is organized as follow:

* `src/main/java/embedded`: the Java file that will be injected into the plugin app;
* `src/main/java/main`: the project's entry point;
* `src/main/java/models`: the models of the Java object used during the transformation;
* `src/main/java/transformers`: the objects responsible for the transformation. In particular:
    * `Extractor.java` responsible for implementing the code-splitting technique;
    * `Injector.java` responsible for adding the Base IATs;
    * `Protector.java` responsible for adding the IATs with encryption.
* `src/main/java/utils`: the helpers and support functions.

## Prerequisites

* To build the MARVELoid project, the Android SDK with an Android API 26 or newer is required. <br>
You can download it from the [Android SDK](https://developer.android.com/studio?gclid=CjwKCAjwybyJBhBwEiwAvz4G73ajtfmlbQR5KTuTtNv0qc0nwcE3aN_w7izyhD1ryYqv3YYyuhgI2hoCZo8QAvD_BwE&gclsrc=aw.ds) website. Alternatively, you can download specific android jar files from the Sable [android-platforms](https://github.com/Sable/android-platforms) GitHub repository. 

* Java 8 or newer. <br>
We tested MARVELoid in an Ubuntu machine (18.04 or 20.04) with Java 11.
```console
$ java --version
openjdk 11.0.11 2021-04-20
OpenJDK Runtime Environment (build 11.0.11+9-Ubuntu-0ubuntu2.18.04)
OpenJDK 64-Bit Server VM (build 11.0.11+9-Ubuntu-0ubuntu2.18.04, mixed mode, sharing)
```

> Note: Due to MARVELoid leverage Soot framework, we suggest to build the output jar file with Java 8  

## Building

> **Note** Before building the project, update the dependency into the `build.gradle` file related to the Android sdk. 
At the moment, we suppose that the android sdk is located at `~/Android/Sdk/platforms/android-26/android.jar`.

You can open the MARVELoid project directly from the Intellij IDEA.
Once it is open with Intellij IDEA, it shows a popup (at the bottom-right) with `IntelliJ IDEA found a Gradle build script` title. Then, you should click the `Import Gradle Project` link in this popup.
Alternatively, you can right-click on the `build.gradle` file (listed in the Project view) and select `Import Gradle Project`.

Now, you can run the `MARVELoid[fatJar]` task (in the `build.gradle` file) to build the jar file of MARVELoid.
The executable output is located at `<path/to/MARVELoid>/Sources/MARVELoid/build/libs/MARVELoid-1.0.jar`.

You can also build the jar file directly from the command line.  <br>
From an Ubuntu OS machine:
```console
$ cd <path/to/MARVELoid>/Sources/MARVELoid
$
$ # Grant the needed permission to gradlew file
$ chmod +x ./gradlew
$ ./gradlew compileJava processResources classes fatJar
$
$ ls <path/to/MARVELoid>/Sources/MARVELoid/build/libs/
MARVELoid-1.0.jar [...]
```

A latest version of the jar file is available in the [Binaries folder](../../Binaries).

We built the MARVELoid jar file with Java 8:
```console
$ java -version
openjdk version "1.8.0_292"
OpenJDK Runtime Environment (build 1.8.0_292-8u292-b10-0ubuntu1~18.04-b10)
OpenJDK 64-Bit Server VM (build 25.292-b10, mixed mode)
```

## Usage

Input parameters from *Help* :
```
usage: MARVELoid
 -a,--android-jars <arg>         The path to the android jars. The default
                                 is '$HOME/Android/Sdk/platforms'
 -c,--soot-classpath <arg>       The path to the wanted soot.jar file.
 -en,--encryption-chance <arg>   The percentage to encrypt a method. The
                                 default value is 20%
 -ex,--extractor-chance <arg>    The percentage to extract a method. The
                                 default value is 20%
 -i,--apk-path <arg>             The path to the target apk file.
 -in,--injection-chance <arg>    The percentage to inject an
                                 anti-tampering control. The default value
                                 is 40%
 -j,--jarsigner-path <arg>       The absolute path of the jarsigner. The
                                 default value is 'C:\Program
                                 Files\Java\jdk1.8.0_191\bin\jarsigner.exe
                                 '
 -k,--keystore-path <arg>        The absolute path of the keystore. The
                                 default value is
                                 './src/main/resources/keystore/my-release
                                 -key.keystore'
 -kp,--keystore-pass <arg>       The password of the keystore. The default
                                 value is 'test123'
 -n,--alias-name <arg>           The password of the keystore. The default
                                 value is 'alias_name'
 -o,--output-path <arg>          The output folder. The default value is
                                 in ${apk-path}/sootOutput
 -p,--package-name <arg>         The package name where to insert bombs.
                                 "none" to include all classes (libraries
                                 included). Default value is taken from
                                 Manifest file
 -re,--run-extractor <arg>       Flag that enable the extractor module.
                                 The default value is true
 -ri,--run-injector <arg>        Flag that enable the injector module. The
                                 default value is false
 -rp,--run-protector <arg>       Flag that enable the protector module.
                                 The default value is false
```

MARVELoid output (in `output-path`) involves:

* the protected *apk* (already signed with the keystore in input);
* a folder for each extracted method with the corresponding *classes.dex* file;
* a file (*signatures*) with the mapping between extracted method and output folder. This file contains the informations of the code-splitting;
* a file (*integrityCheck*) with the integrity check info needed by the TC. At the installation time, the TC verifies if the plugin app matches some metadata (e.g., the apk signature, the apk sha256);
* two files (*injectorDetails*, and *protectorDetails*) with the info of the injected IAT controls (for base IATs and IATs with encryption, respectively). The trusted container is responsible to read this file and reply the correct challenge to the plugin app.

<!-- >> **Note**: In a real scenario, the container may download such informations from a remote, trusted server in a secure way.-->

See the [Example](../../Example/README.md) for a complete use-case.

### Notes

1) If the apk to protect has a target `minSdkVersion` less than 22, the Soot framework throws the exception below.
In order to fix this exception, the minSdkVersion of the original apk should be set to (at least) 22.
```
Exception in thread "main" Exception in thread "Thread-17" java.lang.RuntimeException: Dex file overflow. Splitting not support for pre Lollipop Android (Api 22).
at soot.toDex.MultiDexBuilder.hasOverflowed(MultiDexBuilder.java:96)
at soot.toDex.MultiDexBuilder.internClass(MultiDexBuilder.java:58)
at soot.toDex.DexPrinter.addAsClassDefItem(DexPrinter.java:670)
at soot.toDex.DexPrinter.add(DexPrinter.java:1677)
at soot.PackManager.writeClass(PackManager.java:1096)
at soot.PackManager.lambda$writeOutput$1(PackManager.java:699)
at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)
at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)
at java.lang.Thread.run(Thread.java:748)
```

2) The Protector module of MARVELoid leverages the [InMemoryDexClassLoader](https://developer.android.com/reference/dalvik/system/InMemoryDexClassLoader), which is available from Android API 26, and java.util.Base64, which is available from Java 8. Due to this, to test a protected app with Protector module enabled, an emulator with at least API 26 is required.
