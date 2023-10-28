package github.plugin.genetest.util;

public class NameUtils {
    public static String toUnderscoreCase(String camelCase) {
        StringBuilder underscoreCase = new StringBuilder();
        for (int i = 0; i < camelCase.length(); i++) {
            char cur = camelCase.charAt(i);
            char pre;
            if (i != 0) {
                pre = camelCase.charAt(i - 1);
            } else {
                pre = 'A';
            }

            if (Character.isUpperCase(cur)) {
                if (Character.isUpperCase(pre) || pre == '_') {
                    underscoreCase.append(Character.toLowerCase(cur));
                } else {
                    underscoreCase.append("_").append(Character.toLowerCase(cur));
                }

            } else {
                underscoreCase.append(cur);
            }
        }
        return underscoreCase.toString();
    }

    public static void main(String[] args) {
        String expression = toUnderscoreCase("MODE_OVERWRITE.equals(mode)");
        System.out.println(expression);
    }

    public static String toCamelCase(String pascalCase) {
        return Character.toLowerCase(pascalCase.charAt(0)) +
            pascalCase.substring(1).trim();
    }

}
