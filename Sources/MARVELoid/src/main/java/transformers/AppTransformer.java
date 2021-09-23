package transformers;

import models.IntegrityInfo;
import soot.*;
import soot.javaToJimple.LocalGenerator;
import soot.jimple.*;
import soot.options.Options;
import utils.ManifestHelper;

import java.io.*;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class AppTransformer extends SceneTransformer {
    public static SootClass applicationClass = null;
    private final String apkPath;

    public AppTransformer(String apkPath) {
        this.apkPath = apkPath;
    }

    @Override
    protected void internalTransform(String phaseName, Map<String, String> options) {
        applicationClass = createApplicationClass();
    }

    /**
     * This method perform all the needed preliminary activities at application level:
     * -) Add basic classes (e.g., PackageManager)
     * -) Inject need classes (e.g., embedded.*) and load them
     *
     */
    public void preliminaryActivities() {
        // Add common classes
        addCommonClasses();

        injectClasses(new String[]{
                "embedded.EncryptHelper",
                "embedded.UtilHelper",
                "embedded.JavaIntegrityChecks"
        });
    }

    /**
     * This method perform all the activities at application level on the output apk (after the packaging):
     * -) Update the activity class in the manifest file if the updateAppInManifest flag is true
     *
     * @param updateAppInManifest true if an update of the android manifest is needed
     * @param apkFile The file object of the protected apk
     * @param jarsignerPath The absolute path of the jarsigner executable
     * @param keystorePath The absolute path of the keystorePath
     * @param keystorePass The password of the keystore
     * @param aliasName The alian name of the keystore
     */
    public void postActivities(boolean updateAppInManifest, File apkFile, String jarsignerPath, String keystorePath, String keystorePass, String aliasName) {

        String apkPath = null;
        try {
            // Update manifest
            if (updateAppInManifest) {
                ManifestHelper.updateApplicationClassName(applicationClass.getName(), apkFile);
            }

            // get output apk path
            // File apkFile = SourceLocator.v().dexClassIndex().values().stream().findFirst().orElseThrow(IllegalStateException::new);
            apkPath = apkFile.getPath(); // Paths.get(SourceLocator.v().getOutputDir(), apkFile.getName()).toString();

            // Sign output apk file
            // Run command -> jarsigner -storepass "key" -sigalg SHA1withRSA -digestalg SHA1 -keystore my-release-key.keystore <path/to/app> my-key-alias
            Map<String, String> algos = new HashMap<>();
            algos.put("SHA1withRSA", "SHA1");
            algos.put("SHA256withRSA", "SHA-256");

            for (String key : algos.keySet()) {
                String cmd = "-storepass KEYSTORE_PASS -sigalg " + key + " -digestalg " + algos.get(key) + " -keystore KEYSTORE_PATH APK_PATH ALIAS_NAME";
                cmd = cmd.replace("KEYSTORE_PASS", keystorePass);
                cmd = cmd.replace("KEYSTORE_PATH", keystorePath);
                cmd = cmd.replace("ALIAS_NAME", aliasName);
                cmd = cmd.replace("APK_PATH", apkPath);

                List<String> cmdArray = new ArrayList<>(Collections.singleton(jarsignerPath));
                cmdArray.addAll(Arrays.asList(cmd.split(" ")));

                // Dev null file
                File NULL_FILE = new File(
                        (System.getProperty("os.name")
                                .startsWith("Windows") ? "NUL" : "/dev/null")
                );

                ProcessBuilder pb = new ProcessBuilder(cmdArray)
                        .redirectInput(ProcessBuilder.Redirect.INHERIT)
                        .redirectOutput(NULL_FILE)
                        .redirectError(NULL_FILE);
                Process p = pb.start();

                // Wait for process end
                while (true) {
                    try {
                        if (p.waitFor() == 0) {
                            System.out.println("Output apk signed correctly with " + key);
                            break;
                        } else {
                            throw new CompilationDeathException("Error from jarsigner");
                        }
                    } catch (InterruptedException e) {
                        // e.printStackTrace();
                    }
                }
            }


        } catch (IllegalStateException e) {
            System.err.println("Cannot found output apk file. Message: " + e.getMessage());
            System.exit(1);
        } catch (IOException | CompilationDeathException e) {
            System.err.println("Cannot run jarsigner command (TODO: check the jarsigner absolute path). Message: " + e.getMessage());
            System.exit(1);
        }

        // Retrieve info from output file
        // Retrieve signature md5, sha1 e sha256 and signature of classes.dex file
        try {
            ZipFile zipFile = new ZipFile(apkPath);

            boolean retrivedSignature = false;
            boolean retrievedClassesDex = false;
            byte[] signatureBytes = null;
            byte[] classesDexBytes = null;
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while(entries.hasMoreElements()){
                ZipEntry entry = entries.nextElement();
                if (entry.getName().startsWith("META-INF/") &&
                        (entry.getName().endsWith(".RSA") || entry.getName().endsWith(".DSA") || entry.getName().endsWith(".EC"))) {
                    InputStream stream = zipFile.getInputStream(entry);

                    CertificateFactory cf = CertificateFactory.getInstance( "X.509" );
                    for (Certificate c : cf.generateCertificates(stream)) {
                        signatureBytes = c.getEncoded();
                        break;
                    }

                    // signatureBytes = new byte[stream.available()];
                    // stream.read(signatureBytes);
                    retrivedSignature = true;
                } else if (entry.getName().equals("classes.dex")) {
                    InputStream stream = zipFile.getInputStream(entry);
                    classesDexBytes = new byte[stream.available()];
                    stream.read(classesDexBytes);
                    retrievedClassesDex = true;
                }

                if (retrivedSignature && retrievedClassesDex)
                    break;
            }

            if (signatureBytes == null)
                throw new IllegalStateException("No signature found for output apk.");

            if (classesDexBytes == null)
                throw new IllegalStateException("No classes.dex found for output apk.");

            IntegrityInfo integrityInfo = new IntegrityInfo(signatureBytes, classesDexBytes);

            // write to file
            String json = integrityInfo.getJson();
            int index = apkPath.lastIndexOf("/");
            if (index <= 0)
                index = apkPath.lastIndexOf("\\");
            String outputFolder = apkPath.substring(0, index);
            File integrityFile = new File(outputFolder + "/integrityCheck");

            BufferedWriter bw = new BufferedWriter(new FileWriter(integrityFile, false));
            bw.write(json);
            bw.flush();
            bw.close();

        } catch (IOException | IllegalStateException | NoSuchAlgorithmException | CertificateException e) {
            System.err.println("Cannot extract signature infos from apk. Message: " + e.getMessage());
            System.exit(1);
        }


    }

    /**
     * Add basic classes, such as PackageInfo, PackageManager, etc..
     */
    private void addCommonClasses() {
        // load necessary classes for soot
        Scene.v().loadClassAndSupport("java.lang.Object");
        Scene.v().loadClassAndSupport("android.app.Application");
        Scene.v().loadClassAndSupport("java.lang.System");
        Scene.v().addBasicClass("java.lang.Exception", SootClass.SIGNATURES);
        Scene.v().addBasicClass("java.lang.IllegalStateException", SootClass.SIGNATURES);
        Scene.v().addBasicClass("android.content.pm.Signature", SootClass.SIGNATURES);
        Scene.v().addBasicClass("android.content.pm.PackageInfo", SootClass.SIGNATURES);
        Scene.v().addBasicClass("android.content.pm.PackageManager", SootClass.SIGNATURES);
        Scene.v().addBasicClass("android.content.pm.ApplicationInfo", SootClass.SIGNATURES);
        Scene.v().addBasicClass("android.os.Build", SootClass.SIGNATURES);
    }

    /**
     * Inject the classes specified in the input array into the apk as application classes
     *
     * @param classes Array of String
     */
    private static void injectClasses(String[] classes) {
        for (String c: classes)
            Scene.v().addBasicClass(c, SootClass.BODIES);

        //Scene.v().loadBasicClasses();
        Scene.v().loadNecessaryClasses();
        for (String c: classes)
            Scene.v().getSootClass(c).setApplicationClass(); // Mark class to be part of output
    }

    /**
     * This method create a new application class that extend the application class specified in the manifest if exists,
     * otherwise android.app.Application
     *
     * @return The new application class
     */
    private SootClass createApplicationClass() {
        String applicationClassName = ManifestHelper.getApplicationClassName();

        // create new ApplicationClass
        String packageName = ManifestHelper.getPackageName();

        // create application activity -> to be inserted in the AndroidManifest file
        String name = packageName + ".MyCustomApplication";
        SootClass appClass = new SootClass(name, Modifier.PUBLIC);

        if (applicationClassName == null) {
            appClass.setSuperclass(Scene.v().getSootClass("android.app.Application"));
        } else {
            appClass.setSuperclass(Scene.v().getSootClass(applicationClassName));
        }

        // create default constructor
        SootMethod defaultConstructor = new SootMethod("<init>", null, VoidType.v(), Modifier.CONSTRUCTOR | Modifier.PUBLIC);
        Body constructorBody = Jimple.v().newBody();
        Local thisLocal = Jimple.v().newLocal("r0", appClass.getType());
        constructorBody.getLocals().addFirst(thisLocal);
        constructorBody.getUnits().addFirst(
                Jimple.v().newIdentityStmt(thisLocal, Jimple.v().newThisRef(RefType.v(appClass))));
        if (applicationClassName == null) {
            constructorBody.getUnits().addLast(
                    Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(thisLocal,
                            Scene.v().getSootClass("android.app.Application").getMethod("void <init>()").makeRef())));
        } else {
            constructorBody.getUnits().addLast(
                    Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(thisLocal,
                            Scene.v().getSootClass(applicationClassName).getMethod("void <init>()").makeRef())));
        }
        constructorBody.getUnits().addLast(Jimple.v().newReturnVoidStmt());
        defaultConstructor.setDeclaringClass(appClass);
        constructorBody.setMethod(defaultConstructor);
        defaultConstructor.setActiveBody(constructorBody);
        appClass.addMethod(defaultConstructor);
        defaultConstructor.setDeclared(true);
        constructorBody.validate();

        // onCreate method
        SootMethod onCreateMethod = new SootMethod("onCreate", Collections.emptyList(), VoidType.v(), Modifier.PUBLIC);
        Body onCreateBody = Jimple.v().newBody();
        thisLocal = Jimple.v().newLocal("r0", appClass.getType());
        onCreateBody.getLocals().addFirst(thisLocal);
        onCreateBody.getUnits().addFirst(
                Jimple.v().newIdentityStmt(thisLocal, Jimple.v().newThisRef(RefType.v(appClass))));

        // Add field to the class
        SootField contextField =
                new SootField("mycontext", Scene.v().getSootClass("android.content.Context").getType(), Modifier.STATIC | Modifier.PUBLIC);
        appClass.addField(contextField);

        // init the field value this contextField
        Local contextLocal = (new LocalGenerator(onCreateBody)).generateLocal(Scene.v().getSootClass("android.content.Context").getType());
        SootMethod function = Scene.v().getSootClass("android.content.ContextWrapper").getMethod("android.content.Context getApplicationContext()");
        AssignStmt assignStmt = Jimple.v().newAssignStmt(contextLocal,
                Jimple.v().newVirtualInvokeExpr(thisLocal, function.makeRef()));
        onCreateBody.getUnits().addLast(assignStmt);
        onCreateBody.getUnits().addLast(Jimple.v().newAssignStmt(Jimple.v().newStaticFieldRef(contextField.makeRef()), contextLocal));

        // add invoke to super onCreate method
        if (applicationClassName == null) {
            onCreateBody.getUnits().addLast(
                    Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(thisLocal,
                            Scene.v().getSootClass("android.app.Application").getMethod("void onCreate()").makeRef())));
        } else {
            onCreateBody.getUnits().addLast(
                    Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(thisLocal,
                            Scene.v().getSootClass(applicationClassName).getMethod("void onCreate()").makeRef())));
        }

        onCreateBody.getUnits().addLast(Jimple.v().newReturnVoidStmt());

        onCreateMethod.setDeclaringClass(appClass);
        onCreateBody.setMethod(onCreateMethod);
        onCreateMethod.setActiveBody(onCreateBody);
        appClass.addMethod(onCreateMethod);
        onCreateMethod.setDeclared(true);

        onCreateBody.validate();

        appClass.setResolvingLevel(SootClass.BODIES);
        synchronized (Options.v()) {
            Scene.v().addClass(appClass);
            Scene.v().getSootClass(name).setApplicationClass();
        }

        return appClass;
    }

}
