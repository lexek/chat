package lexek.wschat.proxy.twitch;

import com.google.common.primitives.Longs;
import lexek.wschat.chat.msg.MessageNode;
import lexek.wschat.chat.msg.MessageProcessor;

import java.util.List;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CheermotesMessageProcessor implements MessageProcessor {
    private static final String BASE_URL = "https://static-cdn.jtvnw.net/bits";
    private final List<Cheermote> cheermotes;

    public CheermotesMessageProcessor(CheermotesProvider cheermotesProvider) {
        this.cheermotes = cheermotesProvider
            .getCheermotes()
            .stream()
            .map(name -> new Cheermote(Pattern.compile("(?:^|\\s)(" + name + ")(\\d+)(?:\\s|$)"), name))
            .collect(Collectors.toList());
    }

    @Override
    public void process(List<MessageNode> message) {
        for (Cheermote emoticon : cheermotes) {
            Pattern pattern = emoticon.getPattern();
            for (ListIterator<MessageNode> iterator = message.listIterator(); iterator.hasNext(); ) {
                MessageNode node = iterator.next();
                if (node.getType() == MessageNode.Type.TEXT) {
                    iterator.remove();
                    String text = node.getText();
                    Matcher matcher = pattern.matcher(node.getText());
                    int start = 0;
                    while (matcher.find()) {
                        int matchStart = matcher.start();
                        int matchEnd = matcher.end();
                        String before = text.substring(start, matchStart);
                        if (before.length() > 0) {
                            iterator.add(MessageNode.textNode(before));
                        }
                        start = matchEnd;
                        iterator.add(MessageNode.emoticonNode(
                            text.substring(matchStart, matchEnd),
                            //static-cdn.jtvnw.net/bits/<theme>/<type>/<color>/<size>
                            BASE_URL + "/light/animated/" + getColor(Longs.tryParse(matcher.group(2))) + "/1",
                            -1L
                        ));
                    }
                    //add leftovers to result
                    if (start != text.length()) {
                        iterator.add(MessageNode.textNode(text.substring(start, text.length())));
                    }
                }
            }
        }
    }

    private String getColor(long amount) {
        if (amount < 100) {
            return "gray";
        }
        if (amount < 1000) {
            return "purple";
        }
        if (amount < 5000) {
            return "green";
        }
        if (amount < 10000) {
            return "blue";
        }
        return "red";
    }

    @Override
    public boolean handlesChildren() {
        return true;
    }

    private class Cheermote {
        private final Pattern pattern;
        private final String name;

        private Cheermote(Pattern pattern, String name) {
            this.pattern = pattern;
            this.name = name;
        }

        public Pattern getPattern() {
            return pattern;
        }

        public String getName() {
            return name;
        }
    }
}
