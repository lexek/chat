package lexek.wschat.proxy.twitch;

import com.google.common.primitives.Longs;
import lexek.wschat.chat.msg.MessageNode;
import lexek.wschat.chat.msg.MessageProcessor;

import java.util.List;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CheermotesMessageProcessor implements MessageProcessor {
    private final List<Cheermote> cheermotes;

    public CheermotesMessageProcessor(CheermotesProvider cheermotesProvider) {
        this.cheermotes = cheermotesProvider.getCheermotes();
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

                        Long bits = Longs.tryParse(matcher.group(2));

                        CheermoteTier tier = null;

                        if (bits != null) {
                            for (CheermoteTier t : emoticon.getTiers()) {
                                if (bits >= t.getMinBits()) {
                                    tier = t;
                                } else {
                                    break;
                                }
                            }
                        }

                        if (tier != null) {
                            iterator.add(MessageNode.cheermoteNode(
                                text.substring(matchStart, matchEnd),
                                tier.getUrl(),
                                tier.getColor(),
                                bits
                            ));
                        }
                    }
                    //add leftovers to result
                    if (start != text.length()) {
                        iterator.add(MessageNode.textNode(text.substring(start, text.length())));
                    }
                }
            }
        }
    }

    @Override
    public boolean handlesChildren() {
        return true;
    }
}
