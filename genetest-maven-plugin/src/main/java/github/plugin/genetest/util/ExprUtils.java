package github.plugin.genetest.util;

public class ExprUtils {

    public static String replaceEquals(String expression) {
        return expression.replaceAll("==", "_equals_");
    }

    public static String replaceBracket(String expression) {
        return expression.replaceAll("\\(", "_").replaceAll("\\)", "_");
    }

    public static String replaceNot(String expression) {
        return expression.replaceAll("!", "_not_");
    }

    public static String replaceQuote(String expression) {
        return expression.replaceAll("\"", "");
    }

    public static String replaceDot(String expression) {
        return expression.replaceAll("\\.", "_");
    }

    public static String replaceSpace(String expression) {
        return expression.replaceAll(" ", "");
    }

    public static String replaceDoubleUnderLine(String expression) {
        return expression.replaceAll("__", "_");
    }


    public static String replaceLastUnderline(String expression) {
        int index = expression.length() - 1;
        if (expression.charAt(index) == '_') {
            return expression.substring(0, expression.length() - 1);
        }
        return expression;
    }

    public static String expression(String expression) {
        expression = replaceSpace(expression);
        expression = replaceQuote(expression);
        expression = replaceEquals(expression);
        expression = replaceNot(expression);
        expression = replaceBracket(expression);
        expression = replaceDot(expression);
        expression = NameUtils.toUnderscoreCase(expression);
        expression = replaceDoubleUnderLine(expression);
        expression = replaceLastUnderline(expression);
        return expression;
    }

}
