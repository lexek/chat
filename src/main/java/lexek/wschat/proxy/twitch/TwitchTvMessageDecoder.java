package lexek.wschat.proxy.twitch;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import lexek.wschat.chat.msg.DefaultMessageProcessingService;
import lexek.wschat.chat.msg.MessageNode;
import lexek.wschat.chat.msg.UrlMessageProcessor;
import lexek.wschat.util.Colors;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class TwitchTvMessageDecoder extends MessageToMessageDecoder<String> {
    private final Logger logger = LoggerFactory.getLogger(TwitchTvMessageDecoder.class);
    private final DefaultMessageProcessingService messageProcessingService;

    public TwitchTvMessageDecoder() {
        messageProcessingService = new DefaultMessageProcessingService();
        messageProcessingService.addProcessor(new UrlMessageProcessor());
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, String message, List<Object> out) throws Exception {
        logger.trace(message);
        String prefix = null;
        String trailing;
        String arg[];
        String msg = message;
        Map<String, String> tags = new HashMap<>();
        if (msg.startsWith("@")) {
            int spaceIdx = msg.indexOf(' ');
            for (String tagString : msg.substring(1, spaceIdx).split(";")) {
                String[] tag = tagString.split("=", -1);
                if (tag[1].length() > 0) {
                    tags.put(tag[0], tag[1]);
                }
            }
            msg = msg.substring(spaceIdx + 1);
        }
        if (msg.startsWith(":")) {
            String tmp[] = msg.substring(1).split(" ", 2);
            prefix = tmp[0];
            msg = tmp[1];
        }
        if (msg.contains(" :")) {
            String tmp[] = msg.split(" :", 2);
            msg = tmp[0];
            trailing = tmp[1];
            arg = msg.split(" ");
            arg = Arrays.copyOf(arg, arg.length + 1);
            arg[arg.length - 1] = trailing;
        } else {
            arg = msg.split(" ");
        }
        String command = arg[0];

        switch (command) {
            case "CLEARCHAT": {
                out.add(new TwitchEventMessage(TwitchEventMessage.Type.CLEAR, arg[2]));
                break;
            }
            case "NOTICE": {
                if (arg[2].equals("Error logging in")) {
                    out.add(new TwitchEventMessage(TwitchEventMessage.Type.LOGIN_FAILED, null));
                }
                break;
            }
            case "PRIVMSG": {
                String nick = prefix;
                if (nick == null) {
                    logger.warn("nick is null. WTF?");
                    return;
                }
                if (nick.contains("!")) {
                    nick = nick.substring(0, nick.indexOf('!'));
                }
                out.add(new TwitchUserMessage(
                    nick,
                    tags.getOrDefault("color", Colors.generateColor(nick)),
                    parseUserMessage(arg[2], tags))
                );
                break;
            }
            case "JOIN": {
                out.add(new TwitchJoinEvent(parseNick(prefix), arg[1]));
                break;
            }
            case "PING": {
                out.add(new TwitchEventMessage(TwitchEventMessage.Type.PING, arg.length == 2 ? arg[1] : null));
                break;
            }
        }
    }

    private List<MessageNode> parseUserMessage(String text, Map<String, String> tags) {
        List<MessageNode> message = new LinkedList<>();

        String emotesString = tags.get("emotes");
        if (emotesString != null) {
            TreeSet<TwitchEmote> emotes = new TreeSet<>();
            for (String emote : emotesString.split("/")) {
                String[] e = emote.split(":", -1);
                String id = e[0];
                for (String indexPair : e[1].split(",")) {
                    String[] indexes = indexPair.split("-");
                    int startIndex = Integer.parseInt(indexes[0]);
                    int endIndex = Integer.parseInt(indexes[1]) + 1;
                    emotes.add(new TwitchEmote(id, startIndex, endIndex));
                }
            }
            int idx = 0;
            for (TwitchEmote emote : emotes) {
                int startIndex = emote.startIndex;
                int endIndex = emote.endIndex;
                if (idx != startIndex) {
                    message.add(MessageNode.textNode(text.substring(idx, startIndex)));
                }
                //todo: emoticon sizing
                message.add(MessageNode.emoticonNode(
                    text.substring(startIndex, endIndex),
                    "//static-cdn.jtvnw.net/emoticons/v1/" + emote.id + "/1.0"
                ));
                idx = endIndex;
            }
            if (text.length() - 1 != idx) {
                message.add(MessageNode.textNode(text.substring(idx, text.length())));
            }
        } else {
            message.add(MessageNode.textNode(text));
        }
        messageProcessingService.processMessage(message, true);
        return message;
    }

    private static String parseNick(String nick) {
        if (nick.contains("!")) {
            nick = nick.substring(0, nick.indexOf('!'));
        }
        return nick;
    }

    private static class TwitchEmote implements Comparable<TwitchEmote> {
        private final String id;
        private final int startIndex;
        private final int endIndex;

        private TwitchEmote(String id, int startIndex, int endIndex) {
            this.id = id;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }

        @Override
        public int compareTo(@NotNull TwitchEmote o) {
            return this.startIndex - o.startIndex;
        }

    }
}
