package main;

import models.CmdOption;
import models.Stats;
import org.apache.commons.cli.*;
import soot.*;
import soot.options.Options;
import transformers.AppTransformer;
import transformers.Extractor;
import transformers.Injector;
import transformers.Protector;
import utils.ManifestHelper;

import java.io.File;
import java.util.*;

// Soot survive guide : https://www.brics.dk/SootGuide/sootsurvivorsguide.pdf
// Soot javadoc : https://javadoc.io/doc/ca.mcgill.sable/soot/latest/index.html

public class Main {
    public static final String[] CMD_ARGUMENTS = {"android-jars", "soot-classpath", "package-name", "apk-path", "output-path", "run-extractor", "extractor-chance", "run-protector", "encryption-chance", "run-injector", "injection-chance", "jarsigner-path", "keystore-path", "keystore-pass", "alias-name"};
    public static final Stats stats = new Stats();

    public static void main (String[] args) {
        Options.v().set_force_overwrite(true);
        Options.v().set_allow_phantom_refs(true);       // allow soot to create phantom ref for unknown classes
        Options.v().set_prepend_classpath(true);        // prepend the VM's classpath to Soot's own classpath
        Options.v().set_validate(true);
        Options.v().set_include_all(true);
        Options.v().set_whole_program(true);

        // parse arguments
        Map<String, String> arguments = parseArgs(args);

        // set class path -> not need with gradle
        Options.v().set_soot_classpath(arguments.get(CMD_ARGUMENTS[1]));

        // prefer Android APK files// -src-prec apk
        Options.v().set_src_prec(Options.src_prec_apk);
        Options.v().set_process_multiple_dex(true);

        //output as APK, too//-f J
        Options.v().set_output_format(Options.output_format_dex);
        String outputFolder = arguments.get(CMD_ARGUMENTS[4]);
        Options.v().set_output_dir(outputFolder);

        // set android jar location -> https://github.com/Sable/android-platforms
        Options.v().set_android_jars(arguments.get(CMD_ARGUMENTS[0]));

        // set apk file path -> from command line
        Options.v().set_process_dir(Collections.singletonList(arguments.get(CMD_ARGUMENTS[3])));

        // Check if we want to run the protector module
        boolean runProtector = Boolean.parseBoolean(arguments.get(CMD_ARGUMENTS[7]));
        AppTransformer appTransformer = new AppTransformer(arguments.get(CMD_ARGUMENTS[3]));
        appTransformer.preliminaryActivities();

        /*
        // NB: instead of create the application class, we can leverage the trusted container or the java reflection
        // to retrieve the application context
        if (runProtector) {
            PackManager.v().getPack("wjtp").add(new Transform("wjtp.appChecker", appTransformer));
        }
         */

        // Add the transformation roles to the apk
        // note of Pack and Transformer: https://github.com/soot-oss/soot/wiki/Packs-and-phases-in-Soot
        String packageName = arguments.get(CMD_ARGUMENTS[2]);

        // Inject some AT controls
        boolean runInjector = Boolean.parseBoolean(arguments.get(CMD_ARGUMENTS[9]));
        if (runInjector) {
            int chanceInjection = Integer.parseInt(arguments.get(CMD_ARGUMENTS[10]));
            Injector injector = new Injector(packageName, outputFolder, chanceInjection);
            PackManager.v().getPack("jtp").add(new Transform("jtp.injectControls", injector));
        }

        // Check if we want to run the protector module
        if (Boolean.parseBoolean(arguments.get(CMD_ARGUMENTS[5]))) {
            int chanceExtraction = Integer.parseInt(arguments.get(CMD_ARGUMENTS[6]));
            Extractor extractor = new Extractor(packageName, outputFolder, chanceExtraction);
            PackManager.v().getPack("jtp").add(new Transform("jtp.methodExtractor", extractor));
        }

        if (runProtector) {
            int chanceEncryption = Integer.parseInt(arguments.get(CMD_ARGUMENTS[8]));
            Protector protector = new Protector(packageName, outputFolder, chanceEncryption);
            PackManager.v().getPack("jtp").add(new Transform("jtp.methodProtector", protector));
        }

        // Run modification and write outputs
        PackManager.v().runPacks();
        PackManager.v().writeOutput();

        // Perform post operation after transformation
        File apkFile = new File(outputFolder + "/" + (new File(arguments.get(CMD_ARGUMENTS[3]))).getName());
        if (!apkFile.exists()) {
            System.out.println("Error: the protected apk is not present in the output folder " + outputFolder);
            System.exit(1);
        }

        appTransformer.postActivities(false, apkFile,  arguments.get(CMD_ARGUMENTS[11]),
                arguments.get(CMD_ARGUMENTS[12]), arguments.get(CMD_ARGUMENTS[13]), arguments.get(CMD_ARGUMENTS[14]));

        stats.printStats();
    }

    /**
     * Return a map for the args (for each CMD_ARGUMENTS) array passed in input
     *
     * @param args String[]
     *
     * @return A map of the args array
     */
    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> arguments = new HashMap<>();

        org.apache.commons.cli.Options options = getOptions();
        CommandLineParser parser = new DefaultParser();

        // check for help
        if(Arrays.stream(args).anyMatch("--help"::equals) || Arrays.stream(args).anyMatch("-h"::equals)) {
            printHelp(options);
            System.exit(0);
        }

        try {
            CommandLine cmdLine = parser.parse(options, args);
            for(org.apache.commons.cli.Option opt : options.getOptions()) {
                if(opt.isRequired())
                    arguments.put(opt.getLongOpt(), cmdLine.getOptionValue(opt.getOpt()));
                else
                    arguments.put(opt.getLongOpt(), cmdLine.getOptionValue(opt.getOpt(), ((CmdOption) opt).getDefaultValue()));
            }
        } catch (ParseException e) {
            System.err.println("Error on parsing options: " + e.getMessage());
            printHelp(options);
            System.exit(1);
        }

        // check defaults
        arguments.put(CMD_ARGUMENTS[2], getCorrectPackageName(arguments.get(CMD_ARGUMENTS[2]), arguments.get(CMD_ARGUMENTS[3]))); //"br.com.cjdinfo.puzzle";
        arguments.put(CMD_ARGUMENTS[4], getCorrectOutputFolder(arguments.get(CMD_ARGUMENTS[4]), arguments.get(CMD_ARGUMENTS[3]))); //"C:\\Users\\Antonio\\Documents\\GitHub\\PhD\\Random\\TestAndroguard\\apks\\sootOutput";

        return arguments;
    }

    /**
     * Return the package name where perform the modification, based on the value passed from command line
     *
     * @param packageName The argument package name
     * @param inputPath The argument path of input apk
     *
     * @return the package name
     */
    private static String getCorrectPackageName(String packageName, String inputPath) {
        if(packageName == null || packageName.equals("")){
            return ManifestHelper.getPackageName(inputPath);
        } else if (packageName.equals("none")) {
            return "";
        }
        return packageName;
    }

    /**
     * Return the output folder where save the output files, based on the value passed from command line
     *
     * @param outputFolder The argument output folder
     * @param inputPath The argument path of input apk
     *
     * @return the output folder path
     */
    private static String getCorrectOutputFolder(String outputFolder, String inputPath) {
        if (outputFolder == null || outputFolder.equals("")) {
            int lastIndex = inputPath.lastIndexOf('/');
            if (lastIndex == -1) {
                lastIndex = inputPath.lastIndexOf('\\');
            }
            outputFolder = inputPath.substring(0, lastIndex) + "/sootOutput";
        }
        // check if not exists and create folder
        File directory = new File(outputFolder);
        if (!directory.exists()){
            directory.mkdirs();
        }

        return outputFolder;
    }

    /**
     * Print the help given a list of options
     *
     * @param options list of options of the program
     */
    private static void printHelp(org.apache.commons.cli.Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("MARVELoid", options);
    }

    /**
     * Build the Options object
     *
     * @return the Option Object
     */
    private static org.apache.commons.cli.Options getOptions() {
        Option optionAndroidJar = new CmdOption("a","android-jars",true,
                "The path to the android jars. The default is '$HOME/Android/Sdk/platforms'",
                System.getenv("HOME") + "/Android/Sdk/platforms/");

        Option optionClasspath = new CmdOption("c","soot-classpath",true,
                "The path to the wanted soot.jar file.",
                Scene.v().defaultClassPath() + File.pathSeparator + Main.class.getProtectionDomain().getCodeSource().getLocation().getPath() + File.pathSeparator);

        Option optionPackageName = new CmdOption("p", "package-name", true, "The package name where to insert bombs. " +
                "\"none\" to include all classes (libraries included). Default value is taken from Manifest file", null);

        Option optionApkPath = new CmdOption("i","apk-path",true, true,
                "The path to the target apk file.");

        Option optionOutputPath = new CmdOption("o","output-path",true,
                "The output folder. The default value is in ${apk-path}/sootOutput", null);

        Option optionRunExtractor = new CmdOption("re", "run-extractor", true,
                "Flag that enable the extractor module. The default value is true", "true");

        Option optionExtractorChance = new CmdOption("ex","extractor-chance",true,
                "The percentage to extract a method. The default value is 20%", "20");

        Option optionRunProtector = new CmdOption("rp", "run-protector", true,
                "Flag that enable the protector module. The default value is false", "false");

        Option optionEncryptChance = new CmdOption("en","encryption-chance",true,
                "The percentage to encrypt a method. The default value is 20%", "20");

        Option optionRunInjection = new CmdOption("ri", "run-injector", true,
                "Flag that enable the injector module. The default value is false", "false");

        Option optionInjectionChance = new CmdOption("in","injection-chance",true,
                "The percentage to inject an anti-tampering control. The default value is 40%", "40");

        Option optionJarsignerPath = new CmdOption("j", "jarsigner-path", true,
                "The absolute path of the jarsigner. The default value is 'C:\\Program Files\\Java\\jdk1.8.0_191\\bin\\jarsigner.exe'",
                "C:\\Program Files\\Java\\jdk1.8.0_191\\bin\\jarsigner.exe");

        Option optionKeystorePath = new CmdOption("k", "keystore-path", true,
                "The absolute path of the keystore. The default value is './src/main/resources/keystore/my-release-key.keystore'",
                "./src/main/resources/keystore/my-release-key.keystore");

        Option optionKeystorePass = new CmdOption("kp", "keystore-pass", true,
                "The password of the keystore. The default value is 'test123'",
                "test123");

        Option optionAliasName = new CmdOption("n", "alias-name", true,
                "The password of the keystore. The default value is 'alias_name'",
                "alias_name");

        org.apache.commons.cli.Options options = new org.apache.commons.cli.Options();

        options.addOption(optionAndroidJar);
        options.addOption(optionClasspath);
        options.addOption(optionApkPath);
        options.addOption(optionOutputPath);
        options.addOption(optionPackageName);
        options.addOption(optionRunExtractor);
        options.addOption(optionExtractorChance);
        options.addOption(optionRunProtector);
        options.addOption(optionEncryptChance);
        options.addOption(optionRunInjection);
        options.addOption(optionInjectionChance);
        options.addOption(optionJarsignerPath);
        options.addOption(optionKeystorePath);
        options.addOption(optionKeystorePass);
        options.addOption(optionAliasName);

        return options;
    }

}
