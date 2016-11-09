package com.kavmors.apksheller;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaceholderHelper {
    public static String replace(String content, Map<String, String> config) {
        Matcher matcher = null;
        Pattern pattern = Pattern.compile("\\$\\{.+\\}");
        do {
            if (matcher != null) {
                content = matcher.replaceFirst(getReplaceValue(config, matcher.group()));
            }

            matcher = pattern.matcher(content);
        } while(matcher.find());
        return content;
    }

    private static String getReplaceValue(Map<String, String> config, String key) {
        key = key.replace("${", "").replace("}", "");
        String value = config.get(key);
        return value == null ? "" : value;
    }
}
