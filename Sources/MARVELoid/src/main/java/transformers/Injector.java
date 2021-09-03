package transformers;

import com.google.gson.Gson;
import main.Main;
import models.InjectedInfo;
import models.JavaIntegrityCheckWrapper;
import soot.*;
import soot.javaToJimple.LocalGenerator;
import soot.jimple.IdentityStmt;
import soot.jimple.IntConstant;
import soot.jimple.Jimple;
import soot.jimple.StringConstant;
import utils.Random;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Injector extends BaseBodyTransformer {
    private final String packageName;
    private final File injectorDetailsFiles;

    public Injector(String packageName, String outputFolder, int chanceInjector) {
        super(chanceInjector);
        this.packageName = packageName;
        this.injectorDetailsFiles = new File(outputFolder + "/injectorDetails");

        // create directory if not exists
        File directory = new File(outputFolder);
        if (!directory.exists()){
            directory.mkdirs();
        } else {
            // overwrite content of signature file
            try {
                BufferedWriter bw = new BufferedWriter(new FileWriter(this.injectorDetailsFiles, false));
                bw.write("");
                bw.flush();
                bw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected boolean checkMethod(SootMethod method) {
        // filter for package name
        if (!method.getDeclaringClass().getName().contains(packageName))
            return false;

        // check if it a constructor
        if (method.getName().contains("<init>"))
            return false;

        // Ignore synchronized methods
        if (method.isSynchronized() || ((method.getModifiers() & Modifier.DECLARED_SYNCHRONIZED) == Modifier.DECLARED_SYNCHRONIZED))
            return false;

        // avoid this : https://stackoverflow.com/questions/28020352/surfaceview-onmeasure-did-not-set-the-measured-dimension-by-calling-setmeasure
        if (method.getName().contains("onMeasure") && this.hasSuperClass(method.getDeclaringClass(), "android.view.View"))
            return false;

        // Ignore static constructor methods
        if (method.getName().contains("<clinit>"))
            return false;

        return true;
    }

    @Override
    protected void internalTransform(Body b, String phaseName, Map<String, String> options) {
        // filter dummy functions (e.g., syncronized, constructors, ecc..)
        if (!checkMethod(b.getMethod())) {
            return;
        }

        if (Random.checkChance(chance)) {
            // Add a random AT control -> At the moment only the javaIntegrityChecks.checkVirtualEnvironment
            for (Iterator<Unit> iter = b.getUnits().snapshotIterator(); iter.hasNext(); ) {
                // Insert the check after all identify stmt (this object and params)
                Unit unit = iter.next();
                if (unit instanceof IdentityStmt) {
                    continue;
                }

                JavaIntegrityCheckWrapper javaIntegrityCheckWrapper = JavaIntegrityCheckWrapper.getRandomIntegrityCheck();
                assert javaIntegrityCheckWrapper != null;

                List<Local> args = new ArrayList<>();

                if (javaIntegrityCheckWrapper.isUseContext()) {
                    Local contextLocal = (new LocalGenerator(b)).generateLocal(Scene.v().getSootClass("android.content.Context").getType());
                    SootMethod getContextLocal = Scene.v().getSootClass("embedded.UtilHelper").getMethod("android.content.Context getAppContext()");
                    b.getUnits().insertBeforeNoRedirect(Jimple.v().newAssignStmt(contextLocal, Jimple.v().newStaticInvokeExpr(getContextLocal.makeRef())), unit);
                    args.add(contextLocal);
                }

                byte[] value = null;
                JavaIntegrityCheckWrapper.ExpectedValue expectedValue = null;
                if (javaIntegrityCheckWrapper.isHasExpectedValue()) {
                    expectedValue = javaIntegrityCheckWrapper.getExpectedValue();
                    Local expectedLocal = (new LocalGenerator(b)).generateLocal(ArrayType.v(ByteType.v(), 1));
                    b.getUnits().insertBeforeNoRedirect(
                            Jimple.v().newAssignStmt(expectedLocal, Jimple.v().newNewArrayExpr(ByteType.v(), IntConstant.v(expectedValue.hash.length))), unit);
                    for (int i = 0; i < expectedValue.hash.length; i++) {
                        b.getUnits().insertBeforeNoRedirect(
                                Jimple.v().newAssignStmt(Jimple.v().newArrayRef(expectedLocal, IntConstant.v(i)), IntConstant.v(expectedValue.hash[i])), unit);
                    }
                    args.add(expectedLocal);
                    value = expectedValue.value;
                }

                JavaIntegrityCheckWrapper.Argument packageName = null;
                String packageNameValue = null;
                if ((packageName = javaIntegrityCheckWrapper.getPackageName()) != null) {
                    Local local = (new LocalGenerator(b)).generateLocal(Scene.v().getSootClass("java.lang.String").getType());

                    packageNameValue = packageName.getValue();
                    b.getUnits().insertBeforeNoRedirect(
                            Jimple.v().newAssignStmt(local, StringConstant.v(packageNameValue)),
                            unit
                    );

                    args.add(local);
                }

                SootMethod integrityMethod = Scene.v().getSootClass("embedded.JavaIntegrityChecks").getMethod(javaIntegrityCheckWrapper.getSignature());
                b.getUnits().insertBeforeNoRedirect(Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(integrityMethod.makeRef(), args)), unit);

                b.validate();

                System.out.println("Injector add " + javaIntegrityCheckWrapper.getSignature() + " AT method to "
                        + b.getMethod().getName() + " of class " + b.getMethod().getDeclaringClass().getName());
                synchronized (main.Main.stats) {
                    Main.stats.numberOfInjectedATs++;
                }
                if (javaIntegrityCheckWrapper.isWriteToFile()) {
                    // write info to file
                    synchronized (injectorDetailsFiles) {
                        try {
                            BufferedWriter bw = new BufferedWriter(new FileWriter(injectorDetailsFiles, true));
                            bw.write(
                                    (new Gson()).toJson(
                                            new InjectedInfo(
                                                    packageNameValue,
                                                    value)));
                            bw.newLine();
                            bw.flush();
                            bw.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                break;
            }

        }
    }
}
