package org.xmlet.xsdasmfaster.classes;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.xmlet.xsdasmfaster.classes.utils.AsmException;
import org.xmlet.xsdparser.xsdelements.XsdAbstractElement;
import org.xmlet.xsdparser.xsdelements.XsdAttribute;
import org.xmlet.xsdparser.xsdelements.XsdElement;
import org.xmlet.xsdparser.xsdelements.XsdRestriction;
import org.xmlet.xsdparser.xsdelements.xsdrestrictions.XsdEnumeration;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static org.objectweb.asm.Opcodes.*;
import static org.xmlet.xsdasmfaster.classes.XsdAsmUtils.*;
import static org.xmlet.xsdasmfaster.classes.XsdSupportingStructure.*;

/**
 * This class is responsible to generate all the code that is {@link Enum} related.
 */
class XsdAsmEnum {

    private XsdAsmEnum(){}

    /**
     * Creates an {@link Enum} class based on the information received.
     * @param attribute The {@link XsdAttribute} that has the restrictions that are used to create the {@link Enum} class.
     * @param enumerations The {@link List} of {@link XsdEnumeration} that contains all the values that will be used in
     *                     the generated {@link Enum} class.
     * @param apiName The name of the generated fluent interface.
     */
    static void createEnum(XsdAttribute attribute, List<XsdEnumeration> enumerations, String apiName){
        String enumName = getEnumName(attribute);
        String enumType = getFullClassTypeName(enumName, apiName);
        String enumTypeDesc = getFullClassTypeNameDesc(enumName, apiName);

        String fullJavaTypeDesc = getFullJavaType(attribute);
        String fullJavaType = fullJavaTypeDesc.substring(1, fullJavaTypeDesc.length() - 1);

        ClassWriter cw = generateClass(enumName, "java/lang/Enum", new String[]{ENUM_INTERFACE}, "Ljava/lang/Enum<" + enumTypeDesc + ">;L" + enumInterfaceType + "<" + fullJavaTypeDesc +">;", ACC_PUBLIC + ACC_FINAL + ACC_SUPER + ACC_ENUM, apiName);
        FieldVisitor fVisitor;

        enumerations.forEach(enumElem -> {
            FieldVisitor fieldVisitor = cw.visitField(ACC_PUBLIC + ACC_FINAL + ACC_STATIC + ACC_ENUM, validateElemName(enumerations, enumElem), enumTypeDesc, null, null);
            fieldVisitor.visitEnd();
        });

        fVisitor = cw.visitField(ACC_PRIVATE + ACC_FINAL, "value", fullJavaTypeDesc, null, null);
        fVisitor.visitEnd();

        fVisitor = cw.visitField(ACC_PRIVATE + ACC_FINAL + ACC_STATIC + ACC_SYNTHETIC, "$VALUES", "[" + enumTypeDesc, null, null);
        fVisitor.visitEnd();

        MethodVisitor mVisitor = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "values", "()[" + enumTypeDesc, null, null);
        mVisitor.visitCode();
        mVisitor.visitFieldInsn(GETSTATIC, enumType, "$VALUES", "[" + enumTypeDesc);
        mVisitor.visitMethodInsn(INVOKEVIRTUAL, "[" + enumTypeDesc, "clone", "()Ljava/lang/Object;", false);
        mVisitor.visitTypeInsn(CHECKCAST, "[" + enumTypeDesc);
        mVisitor.visitInsn(ARETURN);
        mVisitor.visitMaxs(1, 0);
        mVisitor.visitEnd();

        mVisitor = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "valueOf", "(Ljava/lang/String;)" + enumTypeDesc, null, null);
        mVisitor.visitCode();
        mVisitor.visitLdcInsn(Type.getType(enumTypeDesc));
        mVisitor.visitVarInsn(ALOAD, 0);
        mVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Enum", "valueOf", "(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum;", false);
        mVisitor.visitTypeInsn(CHECKCAST, enumType);
        mVisitor.visitInsn(ARETURN);
        mVisitor.visitMaxs(2, 1);
        mVisitor.visitEnd();

        mVisitor = cw.visitMethod(ACC_PRIVATE, CONSTRUCTOR, "(Ljava/lang/String;I" + fullJavaTypeDesc + ")V", "(" + fullJavaTypeDesc + ")V", null);
        mVisitor.visitCode();
        mVisitor.visitVarInsn(ALOAD, 0);
        mVisitor.visitVarInsn(ALOAD, 1);
        mVisitor.visitVarInsn(ILOAD, 2);
        mVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/Enum", CONSTRUCTOR, "(Ljava/lang/String;I)V", false);
        mVisitor.visitVarInsn(ALOAD, 0);
        mVisitor.visitVarInsn(ALOAD, 3);
        mVisitor.visitFieldInsn(PUTFIELD, enumType, "value", fullJavaTypeDesc);
        mVisitor.visitInsn(RETURN);
        mVisitor.visitMaxs(3, 4);
        mVisitor.visitEnd();

        mVisitor = cw.visitMethod(ACC_PUBLIC + ACC_FINAL, "getValue", "()" + fullJavaTypeDesc, null, null);
        mVisitor.visitCode();
        mVisitor.visitVarInsn(ALOAD, 0);
        mVisitor.visitFieldInsn(GETFIELD, enumType, "value", fullJavaTypeDesc);
        mVisitor.visitInsn(ARETURN);
        mVisitor.visitMaxs(1, 1);
        mVisitor.visitEnd();

        mVisitor = cw.visitMethod(ACC_PUBLIC + ACC_BRIDGE + ACC_SYNTHETIC, "getValue", "()Ljava/lang/Object;", null, null);
        mVisitor.visitCode();
        mVisitor.visitVarInsn(ALOAD, 0);
        mVisitor.visitMethodInsn(INVOKEVIRTUAL, enumType, "getValue", "()" + fullJavaTypeDesc, false);
        mVisitor.visitInsn(ARETURN);
        mVisitor.visitMaxs(1, 1);
        mVisitor.visitEnd();

        MethodVisitor staticConstructor = cw.visitMethod(ACC_STATIC, STATIC_CONSTRUCTOR, "()V", null, null);
        staticConstructor.visitCode();

        int iConst = 0;

        for (XsdEnumeration enumElem : enumerations) {
            String elemName = validateElemName(enumerations, enumElem);

            staticConstructor.visitTypeInsn(NEW, enumType);
            staticConstructor.visitInsn(DUP);
            staticConstructor.visitLdcInsn(elemName);
            staticConstructor.visitIntInsn(BIPUSH, iConst);

            Object object = null;

            try {
                object = Class.forName(fullJavaType.replaceAll("/", ".")).getConstructor(String.class).newInstance(enumElem.getValue());
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
                throw new AsmException("Exception when creating Enum classes.", e);
            }

            if (object instanceof Boolean){
                object = String.valueOf(object);
            }

            staticConstructor.visitLdcInsn(object);
            staticConstructor.visitMethodInsn(INVOKESTATIC, fullJavaType, "valueOf", "(" + JAVA_OBJECT_DESC + ")" + fullJavaTypeDesc, false);
            staticConstructor.visitMethodInsn(INVOKESPECIAL, enumType, CONSTRUCTOR, "(Ljava/lang/String;I" + fullJavaTypeDesc + ")V", false);
            staticConstructor.visitFieldInsn(PUTSTATIC, enumType, elemName, enumTypeDesc);
            iConst += 1;
        }

        staticConstructor.visitIntInsn(BIPUSH, enumerations.size());
        staticConstructor.visitTypeInsn(ANEWARRAY, enumType);

        iConst = 0;

        for (XsdEnumeration enumElem : enumerations){
            staticConstructor.visitInsn(DUP);
            staticConstructor.visitIntInsn(BIPUSH, iConst);
            staticConstructor.visitFieldInsn(GETSTATIC, enumType, getEnumElementName(enumElem), enumTypeDesc);
            staticConstructor.visitInsn(AASTORE);
            iConst += 1;
        }

        staticConstructor.visitFieldInsn(PUTSTATIC, enumType, "$VALUES", "[" + enumTypeDesc);
        staticConstructor.visitInsn(RETURN);
        staticConstructor.visitMaxs(6, 0);
        staticConstructor.visitEnd();

        writeClassToFile(enumName, cw, apiName);
    }

    private static String validateElemName(List<XsdEnumeration> enumerations, XsdEnumeration currentEnumeration) {
        String elemName = getEnumElementName(currentEnumeration);

        long count = enumerations.stream().filter(enumeration -> getEnumElementName(enumeration).equals(elemName)).count();

        if (count > 1){
            return currentEnumeration.getValue();
        }

        return elemName;
    }

    /**
     * Asserts if the received {@link XsdAttribute} has an associated {@link Enum} class.
     * @param attribute The {@link XsdAttribute} object.
     * @return Whether the received {@link XsdAttribute} has an associated {@link Enum} or not.
     */
    static boolean attributeHasEnum(XsdAttribute attribute) {
        List<XsdRestriction> restrictions = getAttributeRestrictions(attribute);

        return restrictions != null && restrictions.size() == 1 && restrictions.get(0).getEnumeration() != null && !restrictions.get(0).getEnumeration().isEmpty();
    }

    /**
     * Obtains the name of the {@link Enum} associated with the received {@link XsdAttribute}.
     * Example:
     *  AttrTypeEnumTypeContentType(EnumTypeContentType) NAMED
     *  AttrTypeEnumTypeStyle(EnumTypeStyle)             NO NAME
     * @param attribute The {@link XsdAttribute} that serves as a based for the name;
     * @return The name of the {@link Enum} class associated with the received {@link XsdAttribute}.
     */
    static String getEnumName(XsdAttribute attribute) {
        String enumPrefix = "Enum";

        if (attribute.getType() != null){
            String attributeName = getCleanName(attribute);
            String attributeTypeName = firstToUpper(attribute.getType().replaceAll("[^a-zA-Z0-9]", ""));

            if (attributeTypeName.startsWith(attributeName)){
                return enumPrefix + attributeTypeName;
            }

            return enumPrefix + attributeName + attributeTypeName;
        }

        XsdAbstractElement elem = attribute;

        while (elem != null){
            if (elem instanceof XsdElement){
                return enumPrefix + getCleanName(attribute) + firstToUpper(((XsdElement) elem).getName());
            }

            elem = elem.getParent();
        }

        return enumPrefix + getCleanName(attribute);
    }

}
