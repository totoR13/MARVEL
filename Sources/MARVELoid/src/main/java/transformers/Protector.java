package transformers;

import com.google.gson.Gson;
import embedded.EncryptHelper;
import main.Main;
import models.ProtectorInfo;
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

// NB: LB is out of scope now --> We are interested to container-plugin comunication mechanism
// Due to this, we encrypt all method's body (this is only a PoC)
public class Protector extends BaseBodyTransformer {
    private static final List<String> LIFECYCLES_METHODS = Arrays.asList("onCreate", "onDestroy", "onStart", "onStop", "onResume", "onPause", "onRestart");
    private final String packageName;
    private final File protectorDetailsFiles;

    public Protector(String packageName, String outputFolder, int chanceEncrypt) {
        super(chanceEncrypt);

        this.packageName = packageName;
        this.protectorDetailsFiles = new File(outputFolder + "/protectorDetails");

        // create directory if not exists
        File directory = new File(outputFolder);
        if (!directory.exists()){
            directory.mkdirs();
        } else {
            // overwrite content of signature file
            try {
                BufferedWriter bw = new BufferedWriter(new FileWriter(this.protectorDetailsFiles, false));
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
        // filter dummy functions (e.g., syncronized, constructors, ecc..)
        if (!checkMethod(b.getMethod())) {
            return;
        }

        // Encrypt code if some (pseudo-random) condition match -> before retrieve the decryption key
        if (Random.checkChance(chance)) {
            SootMethod originMethod = b.getMethod();

            // Update method -> make public all private/protected functions/fields
            try {
                makeAllPublic(b.getUnits().snapshotIterator(), packageName);
            } catch (IllegalArgumentException e) {
                return;
            }

            // Extract body and save into a new class
            String newClassName = getNewClassName(originMethod);
            SootClass extractedClass = (extractMethodBody(b, newClassName, "hook", true)).getMethod().getDeclaringClass();

            // Write to tmp file and read the corresponding byte array
            DexPrinter dexBytePrinter = new DexPrinter();
            dexBytePrinter.add(extractedClass);
            byte[] classPayload = dexBytePrinter.retrieveByteArray();

            // Encrypt it and save into an array
            String encryptionKey = Random.getRandomString();
            String encrypted = EncryptHelper.encrypt(classPayload, encryptionKey);

            // Create new body
            // Create a new body and set as current method body
            Body newBody = assignEmptyBody(originMethod);

            // Assign the encrypted string to a new variable
            StringConstant encryptedCode = StringConstant.v(encrypted);
            Local cipher = (new LocalGenerator(newBody)).generateLocal(encryptedCode.getType());
            newBody.getUnits().addLast(Jimple.v().newAssignStmt(cipher, encryptedCode));

            // Invoke util method
            // create input parameters
            String randomString = Random.getRandomString();
            Local string = (new LocalGenerator(newBody)).generateLocal(Scene.v().getSootClass("java.lang.String").getType());
            newBody.getUnits().addLast(Jimple.v().newAssignStmt(string, StringConstant.v(randomString)));
            Local className = (new LocalGenerator(newBody)).generateLocal(Scene.v().getSootClass("java.lang.String").getType());
            newBody.getUnits().addLast(Jimple.v().newAssignStmt(className, StringConstant.v(extractedClass.getName())));
            Local methodName = (new LocalGenerator(newBody)).generateLocal(Scene.v().getSootClass("java.lang.String").getType());
            newBody.getUnits().addLast(Jimple.v().newAssignStmt(methodName, StringConstant.v("hook")));

            // retrive and invoke function
            Local returnValue = (new LocalGenerator(newBody)).generateLocal(Scene.v().getSootClass("java.lang.Object").getType());
            String signature = "java.lang.Object decryptAndInvoke(java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.Object[])";
            SootMethod function = Scene.v().getSootClass("embedded.UtilHelper").getMethod(signature);

            // init args
            Local args = (new LocalGenerator(newBody)).generateLocal(ArrayType.v(Scene.v().getObjectType(), 1));
            int nParams = originMethod.getParameterCount();
            if (!originMethod.isStatic()) {
                nParams += 1;
            }
            if (nParams > 0) {
                newBody.getUnits().addLast(Jimple.v().newAssignStmt(args,
                        Jimple.v().newNewArrayExpr(Scene.v().getObjectType(), IntConstant.v(nParams))));

                // add local object as first param
                int startIndex = 0;
                if (!originMethod.isStatic()) {
                    startIndex = 1;
                    newBody.getUnits().addLast(
                            Jimple.v().newAssignStmt(Jimple.v().newArrayRef(args, IntConstant.v(0)), newBody.getThisLocal()));
                }

                for (int i = 0; i < originMethod.getParameterCount(); i++) {
                    Local param = newBody.getParameterLocal(i);
                    if (param.getType() instanceof PrimType) {
                        // wrap the primitive type
                        Local oldParam = param;
                        param = UnitHelper.boxPrimitive(newBody, param.getType());
                        newBody.getUnits().addLast(Jimple.v().newAssignStmt(param,
                                Jimple.v().newStaticInvokeExpr(UnitHelper.primitiveToBoxedClass(oldParam.getType()).getMethod("valueOf",
                                        Collections.singletonList(oldParam.getType())).makeRef(), oldParam)));
                    }
                    newBody.getUnits().addLast(
                            Jimple.v().newAssignStmt(Jimple.v().newArrayRef(args, IntConstant.v(i+startIndex)), param));
                }
            } else {
                newBody.getUnits().addLast(Jimple.v().newAssignStmt(args, NullConstant.v()));
            }

            // return the object
            if (!originMethod.getReturnType().equals(VoidType.v())) {
                AssignStmt assignStmt = Jimple.v().newAssignStmt(returnValue, Jimple.v().newStaticInvokeExpr(function.makeRef(),
                        string, className, methodName, cipher, args));
                newBody.getUnits().addLast(assignStmt);

                // if the original method return a primitive type -> unpack it
                if (originMethod.getReturnType() instanceof PrimType) {
                    // cast to the correct type
                    Local castedReturnValue = UnitHelper.boxPrimitive(newBody, originMethod.getReturnType());
                    newBody.getUnits().addLast(
                            Jimple.v().newAssignStmt(castedReturnValue, Jimple.v().newCastExpr(returnValue, castedReturnValue.getType())));

                    // get the primitive type from the casted object
                    returnValue = (new LocalGenerator(newBody)).generateLocal(originMethod.getReturnType());
                    newBody.getUnits().addLast(
                            Jimple.v().newAssignStmt(returnValue, UnitHelper.boxedTypeValueToPrimitiveValue(castedReturnValue)));
                } else {
                    // add cast to correct object
                    Local castedReturnValue = (new LocalGenerator(newBody)).generateLocal(originMethod.getReturnType());
                    newBody.getUnits().addLast(
                            Jimple.v().newAssignStmt(castedReturnValue, Jimple.v().newCastExpr(returnValue, castedReturnValue.getType())));
                    returnValue = castedReturnValue;
                }


                newBody.getUnits().addLast(Jimple.v().newReturnStmt(returnValue));
            } else {
                newBody.getUnits().addLast(Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(function.makeRef(),
                        string, className, methodName, cipher, args)));

                newBody.getUnits().addLast(Jimple.v().newReturnVoidStmt());
            }

            // validate newBody
            newBody.validate();

            System.out.println("Protector encrypted method " + originMethod.getName() + " of class " + originMethod.getDeclaringClass().getName());
            synchronized (Main.stats) {
                Main.stats.numberOfEncryptedMethods++;
            }
            synchronized (protectorDetailsFiles) {
                try {
                    BufferedWriter bw = new BufferedWriter(new FileWriter(protectorDetailsFiles, true));
                    bw.write(
                            (new Gson()).toJson(
                                    new ProtectorInfo(
                                            randomString,
                                            encryptionKey)));
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
     * Check if the input method belongs to the input package name and it is not a synchronized method -> TODO: check if we can relax the criterias
     *
     * @param method The input method to be control
     *
     * @return true if the method match the criteria, otherwise false
     */
    @Override
    protected boolean checkMethod(SootMethod method) {
        // filter for package name
        if (!method.getDeclaringClass().getName().contains(packageName))
            return false;

        // check if it a constructor
        if (method.getName().contains("<init>"))
            return false;

        // remove synchronized methods
        if (method.isSynchronized() || ((method.getModifiers() & Modifier.DECLARED_SYNCHRONIZED) == Modifier.DECLARED_SYNCHRONIZED))
            return false;

        // avoid this : https://stackoverflow.com/questions/28020352/surfaceview-onmeasure-did-not-set-the-measured-dimension-by-calling-setmeasure
        if (method.getName().contains("onMeasure") && this.hasSuperClass(method.getDeclaringClass(), "android.view.View"))
            return false;

        // Ignore methods of application class
        /*String originalAppClass = ManifestHelper.getApplicationClassName();
        String newAppClass = AppTransformer.applicationClass.getName();
        if (newAppClass.equals(method.getDeclaringClass().getName()) ||
                (originalAppClass != null && originalAppClass.equals(method.getDeclaringClass().getName())))
            return false;*/

        // Ignore static constructor methods
        if (method.getName().contains("<clinit>"))
            return false;

        // Ignore methods of android lifecicle (e.g., onCreate, onDestroy, ecc..)
        if (LIFECYCLES_METHODS.contains(method.getName()))
            return false;

        return true;
    }
}
