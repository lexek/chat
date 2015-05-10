package lexek.wschat.frontend.irc;

class ParsedMessage {
    private final String prefix;
    private final String[] arg;

    ParsedMessage(String prefix, String[] arg) {
        this.prefix = prefix;
        this.arg = arg;
    }

    public String[] getArg() {
        return arg;
    }

    public String getPrefix() {
        return prefix;
    }
}
