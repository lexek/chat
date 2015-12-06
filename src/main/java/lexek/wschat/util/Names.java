package lexek.wschat.util;

import java.util.regex.Pattern;

public class Names {
    public static final Pattern USERNAME_PATTERN = Pattern.compile("[a-z][a-z0-9_]{2,16}");
    public static final Pattern PASSWORD_PATTERN = Pattern.compile(".{6,30}");

    private static final String ATOM = "[a-z0-9!#$%&'*+/=?^_`{|}~-]";
    private static final String DOMAIN = "(" + ATOM + "+(\\." + ATOM + "+)*";
    private static final String IP_DOMAIN = "\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\]";

    public static final Pattern EMAIL = java.util.regex.Pattern.compile(
        "^" + ATOM + "+(\\." + ATOM + "+)*@"
            + DOMAIN
            + "|"
            + IP_DOMAIN
            + ")$",
        java.util.regex.Pattern.CASE_INSENSITIVE
    );
}
