package transformers;

import com.google.gson.Gson;
import main.Main;
import models.HookInfo;
import soot.*;
import soot.javaToJimple.LocalGenerator;
import soot.jimple.*;
import utils.DexPrinter;
import utils.Random;
import utils.UnitHelper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Extractor extends BaseBodyTransformer {
    private final String packageName;
    private final String outputFolder;
    private final File signaturesFile;

    public Extractor(String packageName, String outputFolder, int chanceExtraction) {
        super(chanceExtraction);

        if (chanceExtraction < 0 || chanceExtraction > 100) {
            throw new IllegalArgumentException("The chance of extract a method must be between 0% and 100%");
        }

        this.packageName = packageName;
        this.outputFolder = outputFolder;
        this.signaturesFile = new File(outputFolder + "/signatures");

        // create directory if not exists
        File directory = new File(outputFolder);
        if (!directory.exists()){
            directory.mkdirs();
        } else {
            // overwrite content of signature file
            try {
                BufferedWriter bw = new BufferedWriter(new FileWriter(signaturesFile, false));
                bw.write("");
                bw.flush();
                bw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void internalTransform(Body b, String phaseName, Map<String, String> options) {
        if (!checkMethod(b.getMethod())) {
            return;
        }

        if (Random.checkChance(chance)) {
            SootMethod originMethod = b.getMethod();
            System.out.println("Transforming method " + originMethod.getName() + " of class " + originMethod.getDeclaringClass().getName());

            // String newClassName = getNewClassName(originMethod);
            String newClassName = getNewClassNameRandom();
            String folder = UUID.randomUUID().toString().replace("-", "");

            // Update method -> make public all private/protected functions/fields
            try {
                makeAllPublic(b.getUnits().snapshotIterator(), packageName);
            } catch (IllegalArgumentException e) {
                return;
            }

            // Extract body and print it to a dex file
            SootClass extractedClass = (extractMethodBody(b, newClassName, "hook", false)).getMethod().getDeclaringClass();

            // Add the 'myClass' static field on extractedClass
            SootField myClassField =
                    new SootField("myClass", Scene.v().getSootClass("java.lang.Class").getType(), Modifier.STATIC | Modifier.PUBLIC);
            extractedClass.addField(myClassField);

            // Init static field -> add a static constructor
            SootMethod classInitializer = new SootMethod("<clinit>", Collections.emptyList(), VoidType.v(), Modifier.STATIC);
            extractedClass.addMethod(classInitializer);
            Body classInitializerBody = Jimple.v().newBody(classInitializer);
            classInitializer.setActiveBody(classInitializerBody);

            classInitializerBody.getUnits().addLast(Jimple.v().newAssignStmt(
                    Jimple.v().newStaticFieldRef(myClassField.makeRef()),
                    ClassConstant.fromType(extractedClass.getType())
            ));

            classInitializerBody.getUnits().addLast(Jimple.v().newReturnVoidStmt());
            classInitializerBody.validate();

            // Print the corresponding dex to file
            DexPrinter dexPrinter = new DexPrinter(outputFolder, folder);
            dexPrinter.add(extractedClass);
            dexPrinter.writeToFile();

            // Change the body of the current method
            // Create a new body and set as current method body
            Body newBody = assignEmptyBody(originMethod);

            // Create throw stmt
            RefType type = RefType.v("java.lang.RuntimeException");

            // Init new throw local and insert in the new Body
            Local throwLocal = (new LocalGenerator(newBody)).generateLocal(type);
            Value rValue = UnitHelper.getInitValue(type);
            AssignStmt initLocalVariable = Jimple.v().newAssignStmt(throwLocal, rValue);
            newBody.getUnits().addLast(initLocalVariable);

            // Create units
            Stmt newStmt = Jimple.v().newAssignStmt(throwLocal, Jimple.v().newNewExpr(type));
            Stmt invStmt = Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(throwLocal,
                    Scene.v().getMethod("<java.lang.RuntimeException: void <init>()>").makeRef()));
            Stmt throwStmt = Jimple.v().newThrowStmt(throwLocal);
            newBody.getUnits().addLast(newStmt);
            newBody.getUnits().addLast(invStmt);
            newBody.getUnits().addLast(throwStmt);

            // Validate method
            newBody.validate();

            synchronized (main.Main.stats) {
                Main.stats.numberOfExtractedMethods++;
            }
            // write to file -> get lock
            synchronized (signaturesFile) {
                try {
                    BufferedWriter bw =  new BufferedWriter(new FileWriter(signaturesFile, true));
                    String bytecodeSignature = originMethod.getBytecodeSignature();
                    bw.write((new Gson()).toJson(new HookInfo(newClassName, bytecodeSignature.substring(1, bytecodeSignature.length()-1), folder)));
                    bw.newLine();
                    bw.flush();
                    bw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    /**
     * Check if the input method belongs to the input package name and it is not a constructor or an inherit method -> TODO: check if we can relax the constraints
     *
     * @param method The input method to be control
     *
     * @return true if the method match the criteria, otherwise false
     */
    protected boolean checkMethod(SootMethod method) {
        // filter for package name
        if (!method.getDeclaringClass().getName().contains(packageName))
            return false;

        // check if it a constructor
        if (method.getName().contains("<init>"))
            return false;

        // avoid this : https://stackoverflow.com/questions/28020352/surfaceview-onmeasure-did-not-set-the-measured-dimension-by-calling-setmeasure
        if (method.getName().contains("onMeasure") && this.hasSuperClass(method.getDeclaringClass(), "android.view.View"))
            return false;

        // Ignore synchronized methods
        if (method.isSynchronized() || ((method.getModifiers() & Modifier.DECLARED_SYNCHRONIZED) == Modifier.DECLARED_SYNCHRONIZED))
            return false;

        // check if method is not inheredit from superClass
        SootClass superClass = method.getDeclaringClass().getSuperclass();
        while(true) {

            try {
                superClass.getMethod(method.getSubSignature());
                return false;
            } catch (Exception e) {
                ;
            }

            if (superClass.getName().equals("java.lang.Object"))
                break;

            superClass = superClass.getSuperclass();
        }

        return true;
    }

}
