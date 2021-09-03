package utils;

import soot.*;
import soot.javaToJimple.LocalGenerator;
import soot.jimple.*;

public class UnitHelper {

    /**
     * Make the class public
     *
     * @param aClass a soot class
     */
    public static void makePublic(SootClass aClass, String packageName) {
        if(aClass.hasOuterClass())
            makePublic(aClass.getOuterClass(), packageName);

        int modifier = aClass.getModifiers();
        if (aClass.isPrivate()) {
            if (aClass.isLibraryClass() || aClass.isJavaLibraryClass() ||
                    !aClass.getName().contains(packageName))
                throw new IllegalArgumentException("Cannot make public library classes");
            modifier ^= Modifier.PRIVATE;
        }
        if (aClass.isProtected()) {
            if (aClass.isLibraryClass() || aClass.isJavaLibraryClass() ||
                    !aClass.getName().contains(packageName))
                throw new IllegalArgumentException("Cannot make public library classes");
            modifier ^= Modifier.PROTECTED;
        }
        modifier |= Modifier.PUBLIC;
        aClass.setModifiers(modifier);
    }

    /**
     * Make a member (e.g., field) public
     *
     * @param member a member of a class
     */
    public static void makePublic(ClassMember member, String packageName) {
        makePublic(member.getDeclaringClass(), packageName);

        int modifier = member.getModifiers();
        if (member.isPrivate()) {
            if ((member instanceof SootMethod && ((SootMethod) member).isJavaLibraryMethod()) ||
                    member.getDeclaringClass().isLibraryClass() || member.getDeclaringClass().isJavaLibraryClass() ||
                    !member.getDeclaringClass().getName().contains(packageName))
                throw new IllegalArgumentException("Cannot make public library method");
            modifier ^= Modifier.PRIVATE;
        }
        if (member.isProtected()) {
            if ((member instanceof SootMethod && ((SootMethod) member).isJavaLibraryMethod()) ||
                    member.getDeclaringClass().isLibraryClass() || member.getDeclaringClass().isJavaLibraryClass() ||
                    !member.getDeclaringClass().getName().contains(packageName))
                throw new IllegalArgumentException("Cannot make public library method");
            modifier ^= Modifier.PROTECTED;
        }
        modifier |= Modifier.PUBLIC;
        if (modifier != member.getModifiers())
            member.setModifiers(modifier);
    }

    /**
     * Return the default init value for a specific object type
     *
     * @param type the object type
     *
     * @return the default value
     */
    public static Value getInitValue(Type type) {
        Value rValue;
        if(type instanceof PrimType) {
            if(type instanceof IntType || type instanceof ByteType || type instanceof ShortType
                    || type instanceof BooleanType || type instanceof CharType) {
                rValue = IntConstant.v(0);
            } else if (type instanceof LongType) {
                rValue = LongConstant.v(0);
            } else if (type instanceof FloatType) {
                rValue = FloatConstant.v(0);
            } else if (type instanceof DoubleType) {
                rValue = DoubleConstant.v(0);
            } else {
                throw new IllegalArgumentException("Unknown primitive type: " + type.getClass().getCanonicalName());
            }
        } else {
            rValue = NullConstant.v();
        }
        return rValue;
    }

    /**
     * The primitive type into the corresponding soot class
     *
     * @param primitiveType Type
     *
     * @return the corresponding SootClass
     */
    public static SootClass primitiveToBoxedClass(Type primitiveType) {
        if(primitiveType == IntType.v()) {
            return Scene.v().getSootClass("java.lang.Integer");
        } else if (primitiveType == ByteType.v()) {
            return Scene.v().getSootClass("java.lang.Byte");
        } else if (primitiveType == ShortType.v()) {
            return Scene.v().getSootClass("java.lang.Short");
        } else if (primitiveType == LongType.v()) {
            return Scene.v().getSootClass("java.lang.Long");
        } else if (primitiveType == FloatType.v()) {
            return Scene.v().getSootClass("java.lang.Float");
        } else if (primitiveType == DoubleType.v()) {
            return Scene.v().getSootClass("java.lang.Double");
        } else if (primitiveType == CharType.v()) {
            return Scene.v().getSootClass("java.lang.Character");
        } else if (primitiveType == BooleanType.v()) {
            return Scene.v().getSootClass("java.lang.Boolean");
        } else {
            throw new IllegalArgumentException("Provided type is not a primitive type");
        }
    }

    /**
     * Return the local value of the corresponding PrimType from its boxed class
     *
     * @param value Value
     *
     * @return the new PrimType Local
     */
    public static Value boxedTypeValueToPrimitiveValue(Value value){
        if (!(value instanceof Local))
            throw new IllegalArgumentException("value must be of type Local");

        Local local = (Local) value;
        if (local.getType() instanceof PrimType) {
            return local;
        } else {
            PrimType primitiveType = boxedClassToPrimitive(value.getType().toString());
            if(primitiveType == IntType.v()) {
                return Jimple.v().newVirtualInvokeExpr(local, (Scene.v().getSootClass("java.lang.Integer")).getMethod("int intValue()").makeRef());
            } else if (primitiveType == ByteType.v()) {
                return Jimple.v().newVirtualInvokeExpr(local, (Scene.v().getSootClass("java.lang.Byte")).getMethod("byte byteValue()").makeRef());
            } else if (primitiveType == ShortType.v()) {
                return Jimple.v().newVirtualInvokeExpr(local, (Scene.v().getSootClass("java.lang.Short")).getMethod("short shortValue()").makeRef());
            } else if (primitiveType == LongType.v()) {
                return Jimple.v().newVirtualInvokeExpr(local, (Scene.v().getSootClass("java.lang.Long")).getMethod("long longValue()").makeRef());
            } else if (primitiveType == FloatType.v()) {
                return Jimple.v().newVirtualInvokeExpr(local, (Scene.v().getSootClass("java.lang.Float")).getMethod("float floatValue()").makeRef());
            } else if (primitiveType == DoubleType.v()) {
                return Jimple.v().newVirtualInvokeExpr(local, (Scene.v().getSootClass("java.lang.Double")).getMethod("double doubleValue()").makeRef());
            } else if (primitiveType == CharType.v()) {
                return Jimple.v().newVirtualInvokeExpr(local, (Scene.v().getSootClass("java.lang.Character")).getMethod("char charValue()").makeRef());
            } else if (primitiveType == BooleanType.v()) {
                return Jimple.v().newVirtualInvokeExpr(local, (Scene.v().getSootClass("java.lang.Boolean")).getMethod("boolean booleanValue()").makeRef());
            }
        }
        return null;
    }

    /**
     * Return the corresponding PrimType to the type in input
     *
     * @param type String
     *
     * @return PrimType
     */
    private static PrimType boxedClassToPrimitive(String type){
        if(type.equals("java.lang.Integer")) {
            return IntType.v();
        } else if (type.equals("java.lang.Byte")) {
            return ByteType.v();
        } else if (type.equals("java.lang.Boolean")) {
            return BooleanType.v();
        } else if (type.equals("java.lang.Short")) {
            return ShortType.v();
        } else if (type.equals("java.lang.Long")) {
            return LongType.v();
        } else if (type.equals("java.lang.Float")) {
            return FloatType.v();
        } else if (type.equals("java.lang.Double")) {
            return DoubleType.v();
        } else if (type.equals("java.lang.Character")) {
            return CharType.v();
        } else {
            throw new IllegalArgumentException("Provided type is not a primitive type");
        }
    }

    /**
     * Create the local variable for a Wrapper Object of primitive type primitiveType.
     *
     * @param body Body
     * @param type Type
     *
     * @return the new Local variable
     */
    public static Local boxPrimitive(Body body, Type type) {
        SootClass wrapperClass = UnitHelper.primitiveToBoxedClass(type);
        return (new LocalGenerator(body)).generateLocal(wrapperClass.getType());
    }

}
