package github.plugin.genetest.util;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {

    public static Optional<String> extract(String str, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(str);
        if (matcher.find()) {
            String extract = matcher.group(1);
            return Optional.of(extract);
        } else {
            return Optional.empty();
        }
    }

    public static boolean isBlank(String str) {
        return (str == null || str.trim().isEmpty());
    }

    public static List<String> split(String str, String regex) {
        String[] split = str.split(regex);
        return Arrays.asList(split);
    }

    public static String splitLast(String str, String regex) {
        List<String> split = split(str, regex);
        if (split.isEmpty()) {
            return "";
        }
        return split.get(split.size() - 1);
    }

    public static String splitFirst(String str, String regex) {
        List<String> split = split(str, regex);
        if (split.isEmpty()) {
            return "";
        }
        return split.get(0);
    }

    public static boolean includeStartsWith(String includeStr, String str) {
        List<String> includes = StringUtils.split(includeStr, ",");
        for (String classFullName : includes) {
            if (str.startsWith(classFullName)) {
                return true;
            }
        }
        return false;
    }

}
