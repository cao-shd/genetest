package github.plugin.genetest.util;

public class NameUtils {
    public static String toUnderscoreCase(String camelCase) {
        StringBuilder underscoreCase = new StringBuilder();
        for (int i = 0; i < camelCase.length(); i++) {
            char c = camelCase.charAt(i);
            if (Character.isUpperCase(c)) {
                underscoreCase.append("_").append(Character.toLowerCase(c));
            } else {
                underscoreCase.append(c);
            }
        }
        return underscoreCase.toString();
    }

    public static String toCamelCase(String pascalCase) {
        return Character.toLowerCase(pascalCase.charAt(0)) +
            pascalCase.substring(1).trim();
    }

}
