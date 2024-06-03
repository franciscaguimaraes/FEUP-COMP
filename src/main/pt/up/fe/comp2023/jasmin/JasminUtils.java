package pt.up.fe.comp2023.jasmin;

import org.specs.comp.ollir.*;

import static pt.up.fe.comp2023.jasmin.JasminBuilder.classUnit;

public class JasminUtils {
    public static String getJasminType(Type type) {
        StringBuilder stringBuilder = new StringBuilder();
        ElementType elementType = type.getTypeOfElement();

        switch (elementType) {
            case VOID:
                stringBuilder.append("V");
                break;
            case INT32:
                stringBuilder.append("I");
                break;
            case STRING:
                stringBuilder.append("Ljava/lang/String;");
                break;
            case BOOLEAN:
                stringBuilder.append("Z");
                break;
            case OBJECTREF:
                if (type instanceof ClassType classType) stringBuilder.append("L" + classType.getName() + ";");
                else if (type instanceof ArrayType arrayType)  stringBuilder.append("L" + (arrayType).getElementType() + ";");
                else stringBuilder.append("Ljava/lang/Object;");
                break;
            case ARRAYREF:
                stringBuilder.append("[I");
                break;
            default:
                stringBuilder.append("Error: type not supported;\n");
                break;
        }
        return stringBuilder.toString();
    }

    public static String getImpClass(String className, ClassUnit classUnit) {
        if (className.equals("this")) return classUnit.getClassName();
        for (String imp : classUnit.getImports()) {
            if (imp.endsWith(className)) return imp.replaceAll("\\.", "/");
        }
        return className;
    }
}
