package dev.valerisclient.core.modules.smp;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** Safe regex helpers for economy text parsing. */
final class SmpRegexUtil {

    private SmpRegexUtil() {
    }

    static String firstMatch(String text, String regex) {
        if (text == null || text.isEmpty() || regex == null || regex.isBlank()) {
            return "";
        }
        try {
            Matcher matcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(text);
            if (matcher.find()) {
                if (matcher.groupCount() >= 1) {
                    return matcher.group(1);
                }
                return matcher.group();
            }
        } catch (PatternSyntaxException ignored) {
        }
        return "";
    }
}
