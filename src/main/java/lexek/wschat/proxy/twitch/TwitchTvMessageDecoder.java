package lexek.wschat.proxy.twitch;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.Arrays;
import java.util.List;

public class TwitchTvMessageDecoder extends MessageToMessageDecoder<String> {
    private TwitchUser user;

    @Override
    protected void decode(ChannelHandlerContext ctx, String msg, List<Object> out) throws Exception {
        String prefix = null;
        String trailing;
        String arg[];
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
            case "PRIVMSG": {
                String nick = prefix;
                if (nick.contains("!")) {
                    nick = nick.substring(0, nick.indexOf('!'));
                }
                if (nick.equals("jtv")) {
                    String jtvCommandArgs[] = arg[2].split(" ");
                    switch (jtvCommandArgs[0]) {
                        case "USERCOLOR":
                            getUser().setColor(jtvCommandArgs[1]);
                            break;
                        case "SPECIALUSER":
                            TwitchUser user = getUser();
                            switch (jtvCommandArgs[2]) {
                                case "staff":
                                    user.setStaff(true);
                                    break;
                                case "admin":
                                    user.setAdmin(true);
                                    break;
                                case "subscriber":
                                    user.setSubscriber(true);
                                    break;
                            }
                            break;
                        case "EMOTESET":
                            int[] sets;
                            String t = jtvCommandArgs[2].trim().substring(1, jtvCommandArgs[2].length() - 1);
                            if (t.isEmpty()) {
                                sets = new int[0];
                            } else {
                                String[] tmp = t.split("\\s*,\\s*");
                                sets = new int[tmp.length];
                                for (int i = 0; i < tmp.length; ++i) {
                                    sets[i] = Integer.parseInt(tmp[i]);
                                }
                            }
                            getUser().setEmoticonSets(sets);
                            break;
                        case "CLEARCHAT":
                            out.add(new TwitchEventMessage(TwitchEventMessage.Type.CLEAR, jtvCommandArgs[1]));
                            this.user = null;
                            break;
                    }
                } else {
                    TwitchUser user = getUser();
                    user.setNick(nick);
                    out.add(new TwitchUserMessage(arg[2], user));
                    this.user = null;
                }
                break;
            }
        }
    }

    private TwitchUser getUser() {
        if (user == null) {
            user = new TwitchUser();
        }
        return user;
    }
}
