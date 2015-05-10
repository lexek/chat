package lexek.httpserver;

import java.util.regex.Pattern;

public class MatcherEntry {
    private final Pattern pattern;
    private final HttpHandler handler;

    public MatcherEntry(Pattern pattern, HttpHandler handler) {
        this.pattern = pattern;
        this.handler = handler;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public HttpHandler getHandler() {
        return handler;
    }
}
