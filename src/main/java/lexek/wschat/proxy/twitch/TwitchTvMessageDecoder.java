package lexek.wschat.proxy.twitch;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

public class TwitchTvMessageDecoder extends MessageToMessageDecoder<String> {
    private final Logger logger = LoggerFactory.getLogger(TwitchTvMessageDecoder.class);
    private TwitchUser user;

    @Override
    protected void decode(ChannelHandlerContext ctx, String message, List<Object> out) throws Exception {
        logger.trace(message);
        String prefix = null;
        String trailing;
        String arg[];
        String msg = message;
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
                TwitchUser user = getUser();
                user.setNick(nick);
                out.add(new TwitchUserMessage(arg[2], user));
                this.user = null;
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

    private static String parseNick(String nick) {
        if (nick.contains("!")) {
            nick = nick.substring(0, nick.indexOf('!'));
        }
        return nick;
    }

    private TwitchUser getUser() {
        if (user == null) {
            user = new TwitchUser();
        }
        return user;
    }
}
