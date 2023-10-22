package github.plugin.genetest.util;

public class FieldUtils {

    public static String defaultValue(String typeName, boolean genericExists) {
        switch (typeName) {
            case "Integer":
                return "0";
            case "Long":
                return "0L";
            case "Double":
                return "0.0";
            case "Character":
                return "' '";
            case "Boolean":
                return "false";
            case "String":
                return "\"\"";
            case "List":
                return "new ArrayList<>()";
            case "Map":
                return "new HashMap<>()";
            default:
                if (genericExists) {
                    return "new " + typeName + "<>()";
                } else {
                    return "new " + typeName + "()";
                }
        }
    }

}
