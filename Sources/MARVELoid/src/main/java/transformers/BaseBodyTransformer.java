package transformers;

import soot.*;
import soot.javaToJimple.LocalGenerator;
import soot.jimple.*;
import utils.UnitHelper;

import java.util.*;

public abstract class BaseBodyTransformer extends BodyTransformer {
    protected final int chance;
    protected static final Set<String> modifiedMethods = new HashSet<>();

    public BaseBodyTransformer(int chance) {
        if (chance < 0 || chance > 100) {
            throw new IllegalArgumentException("The \"chance\" argument must be between 0 and 100");
        }

        this.chance = chance;
    }

    protected static boolean addMethod(SootMethod method) {
        synchronized (modifiedMethods) {
            if (modifiedMethods.contains(method.getName()))
                return false;
            modifiedMethods.add(method.getName());
            return true;
        }
    }

    protected abstract boolean checkMethod(SootMethod method);

    /**
     * Return the class name for the extracted class
     *
     * @param method The origin method
     *
     * @return the name of a class
     */
    protected String getNewClassName(SootMethod method){
        return method.getDeclaringClass() +
                method.getName().replace("<", "").replace(">","") +
                UUID.randomUUID().toString().replaceAll("-", "");
    }

    /**
     * Return a new random class name
     *
     * @return the name of a class
     */
    protected String getNewClassNameRandom(){
        return "A" + UUID.randomUUID().toString().replaceAll("-", "") +
                UUID.randomUUID().toString().replaceAll("-", "");
    }

    /**
     * Transform all protected and private fields or methods in public ones.
     *
     * @param units The iterator over a list of units
     */
    protected void makeAllPublic(Iterator<Unit> units, String packageName) {
        while (units.hasNext()) {
            Stmt stmt = (Stmt) units.next();
            if (stmt.containsInvokeExpr() &&
                    (stmt.getInvokeExpr() instanceof SpecialInvokeExpr || stmt.getInvokeExpr() instanceof StaticInvokeExpr ||
                            stmt.getInvokeExpr() instanceof VirtualInvokeExpr || stmt.getInvokeExpr() instanceof InterfaceInvokeExpr)) {
                UnitHelper.makePublic(stmt.getInvokeExpr().getMethod(), packageName);
            }
            if (stmt.containsFieldRef()) {
                UnitHelper.makePublic(stmt.getFieldRef().getField(), packageName);
            }
        }
    }

    /**
     * This methdos create a new class (named 'newClassName') with a method (named 'newMethodName') which have the same input body.
     *
     * @param body The wanted body
     * @param newClassName The new class name
     * @param newMethodName The new method name
     *
     * @return The body of the new method
     */
    protected Body extractMethodBody(Body body, String newClassName, String newMethodName, boolean wrapPrimitiveTypes) {
        SootMethod originMethod = body.getMethod();

        // Create a new method
        List<Type> params = new ArrayList<>();
        if (!originMethod.isStatic()) {
            params.add(body.getThisLocal().getType());
        }
        if (!wrapPrimitiveTypes) {
            params.addAll(originMethod.getParameterTypes());
        } else {
            for (Iterator<Type> iter = originMethod.getParameterTypes().iterator(); iter.hasNext(); ) {
                Type paramType = iter.next();
                if (paramType instanceof PrimType) {
                    params.add(UnitHelper.primitiveToBoxedClass(paramType).getType());
                } else {
                    params.add(paramType);
                }
            }
        }
        SootMethod extractedMethod = new SootMethod(newMethodName, params, originMethod.getReturnType(), Modifier.PUBLIC | Modifier.STATIC);

        // Create a new class
        SootClass extractedClass = new SootClass(newClassName, Modifier.PUBLIC);
        extractedClass.setSuperclass(Scene.v().getObjectType().getSootClass());
        extractedMethod.setDeclaringClass(extractedClass);
        extractedClass.addMethod(extractedMethod);

        // Create a new body
        Body extractedBody = Jimple.v().newBody();
        extractedBody.setMethod(extractedMethod);
        extractedMethod.setActiveBody(extractedBody);


        // For the protector, a primitive type is passed as a wrapperd object and it could be extracted for the boxed one
        if (!wrapPrimitiveTypes) {
            if (originMethod.isStatic()) {
                extractedBody.getLocals().addAll(body.getLocals());
                extractedBody.getUnits().addAll(body.getUnits());
            } else {
                // Insert all original local, renaming the 'this' local
                Local newThisLocal = null;
                for (Iterator<Local> locals = body.getLocals().snapshotIterator(); locals.hasNext(); ) {
                    Local local = locals.next();

                    if (local.equals(body.getThisLocal())) {
                        local.setName("$nl0");
                        newThisLocal = local;
                    }

                    extractedBody.getLocals().addLast(local);
                }

                // Insert all original unit, removing the init of 'this' object
                for (Iterator<Unit> units = body.getUnits().snapshotIterator(); units.hasNext(); ) {
                    Stmt stmt = (Stmt) units.next();

                    if (stmt instanceof IdentityStmt) {
                        if (((IdentityStmt) stmt).getLeftOp().equals(body.getThisLocal())) {
                            extractedBody.getUnits().addLast(Jimple.v().newIdentityStmt(newThisLocal,
                                    Jimple.v().newParameterRef(newThisLocal.getType(), 0)));
                            continue;
                        } else if (((IdentityStmt) stmt).getRightOp() instanceof ParameterRef) {
                            // increase the number of the params
                            ParameterRef parameterRef = (ParameterRef) ((IdentityStmt) stmt).getRightOp();
                            extractedBody.getUnits().addLast(Jimple.v().newIdentityStmt(((IdentityStmt) stmt).getLeftOp(),
                                    Jimple.v().newParameterRef(((IdentityStmt) stmt).getLeftOp().getType(), parameterRef.getIndex() + 1)));
                            continue;
                        }
                    }

                    extractedBody.getUnits().addLast(stmt);
                }
            }
        } else {
            Local newThisLocal = null;
            Map<Local, Local> mapWrappedLocals = new HashMap<>();
            for (Iterator<Local> locals = body.getLocals().snapshotIterator(); locals.hasNext(); ) {
                Local local = locals.next();

                if (!originMethod.isStatic() && local.equals(body.getThisLocal())) {
                    local.setName("$nl0");
                    newThisLocal = local;
                } else if (local.getType() instanceof PrimType) {
                    // TODO: create a new wrpped object as input args and unwrap to original one
                    Local newLocal = UnitHelper.boxPrimitive(extractedBody, local.getType());
                    mapWrappedLocals.put(local, newLocal);
                }

                extractedBody.getLocals().addLast(local);
            }

            List<Unit> castUnits = new ArrayList<>();
            boolean firstUnitAfetrIdentity = true;
            for (Iterator<Unit> units = body.getUnits().snapshotIterator(); units.hasNext(); ) {
                Stmt stmt = (Stmt) units.next();

                if (stmt instanceof IdentityStmt) {
                    if (!originMethod.isStatic() && ((IdentityStmt) stmt).getLeftOp().equals(body.getThisLocal())) {
                        extractedBody.getUnits().addLast(Jimple.v().newIdentityStmt(newThisLocal,
                                Jimple.v().newParameterRef(newThisLocal.getType(), 0)));
                        continue;
                    } else if (((IdentityStmt) stmt).getRightOp() instanceof ParameterRef) {
                        // increase the number of the params
                        ParameterRef parameterRef = (ParameterRef) ((IdentityStmt) stmt).getRightOp();
                        Value currentLocal = ((IdentityStmt) stmt).getLeftOp();
                        int index = parameterRef.getIndex();
                        if (!originMethod.isStatic())
                            index += 1;
                        if (currentLocal instanceof Local && mapWrappedLocals.containsKey(currentLocal)) {
                            extractedBody.getUnits().addLast(Jimple.v().newIdentityStmt(mapWrappedLocals.get(currentLocal),
                                    Jimple.v().newParameterRef(mapWrappedLocals.get(currentLocal).getType(), index)));
                            // unwrap current local
                            castUnits.add(
                                    Jimple.v().newAssignStmt(currentLocal, UnitHelper.boxedTypeValueToPrimitiveValue(mapWrappedLocals.get(currentLocal))));
                        } else {
                            extractedBody.getUnits().addLast(Jimple.v().newIdentityStmt(((IdentityStmt) stmt).getLeftOp(),
                                    Jimple.v().newParameterRef(((IdentityStmt) stmt).getLeftOp().getType(), index)));
                        }
                        continue;
                    }
                }

                if (firstUnitAfetrIdentity) {
                    for (Unit castUnit : castUnits) {
                        extractedBody.getUnits().addLast(castUnit);
                    }
                    firstUnitAfetrIdentity = false;
                }

                extractedBody.getUnits().addLast(stmt);
            }
        }

        // Insert all traps
        extractedBody.getTraps().addAll(body.getTraps());
        extractedMethod.setDeclared(true);

        // Validate body
        extractedBody.validate();

        return extractedBody;
    }

    protected Body assignEmptyBody(SootMethod originMethod) {
        Body newBody = Jimple.v().newBody();
        newBody.setMethod(originMethod);
        originMethod.setActiveBody(newBody);

        if (!originMethod.isStatic()) {
            // Create a dummy this local variable
            Local thisLocal = Jimple.v().newLocal("r0", originMethod.getDeclaringClass().getType());
            newBody.getLocals().addFirst(thisLocal);
            newBody.getUnits().addFirst(
                    Jimple.v().newIdentityStmt(thisLocal, Jimple.v().newThisRef(RefType.v(originMethod.getDeclaringClass()))));
        }

        // Create dummies local for each input parameters
        int index = 0;
        for(Type parameterType : originMethod.getParameterTypes()) {
            Local local = (new LocalGenerator(newBody)).generateLocal(parameterType);// Jimple.v().newLocal("r0", originMethod.getDeclaringClass().getType());
            newBody.getUnits().addLast(
                    Jimple.v().newIdentityStmt(local, Jimple.v().newParameterRef(parameterType, index++)));
        }

        return newBody;
    }

    protected boolean hasSuperClass(SootClass clazz, String superClass) {
        while(clazz.hasSuperclass()) {
            if (clazz.getName().equals(superClass))
                return true;
            clazz = clazz.getSuperclass();
        }
        return false;
    }

}
