package lexek.wschat.frontend.irc;

import com.google.common.collect.ImmutableMap;
import lexek.wschat.chat.model.*;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.frontend.Codec;

import java.util.Arrays;
import java.util.List;

public class IrcCodec implements Codec {
    private final String serverName;

    public IrcCodec(String serverName) {
        this.serverName = serverName;
    }

    private static ParsedMessage parseMessage(String message) {
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

        return new ParsedMessage(prefix, arg);
    }

    @Override
    public String encode(Message message) {
        switch (message.getType()) {
            case MSG: {
                String room = message.get(MessageProperty.ROOM);
                String name = message.get(MessageProperty.NAME);
                String text = message.get(MessageProperty.TEXT);
                return ":" + name + " PRIVMSG " + room + " :" + text;
            }
            case RECAPTCHA: {
                String captchaId = message.get(MessageProperty.TEXT);
                String name = message.get(MessageProperty.NAME);
                return ":server PRIVMSG " + name + " :https://" + serverName + ":1337/recaptcha/" + captchaId;
            }
            case NAMES: {
                List<Chatter> users = message.get(MessageProperty.CHATTERS);
                String room = message.get(MessageProperty.ROOM);
                String name = message.get(MessageProperty.NAME);
                StringBuilder sb = new StringBuilder();
                for (Chatter u : users) {
                    if (u.hasRole(LocalRole.MOD)) {
                        sb.append("@").append(u.getUser().getName()).append(' ');
                    } else {
                        sb.append(u.getUser().getName()).append(' ');
                    }
                }
                return ":" + name + " 353 " + name + " = " + room + " :@server " + sb.toString() + "\r\n" +
                    ":" + name + " 366 " + name + " " + room + " :End of /NAMES list";
            }
            case JOIN: {
                UserDto joinedUser = message.get(MessageProperty.USER);
                LocalRole localRole = message.get(MessageProperty.LOCAL_ROLE);
                String room = message.get(MessageProperty.ROOM);
                String msg = ":" + joinedUser.getName() + "!" + joinedUser.getName() + "@" + serverName + " JOIN " + room;
                if (localRole.compareTo(LocalRole.MOD) >= 0) {
                    msg += "\r\n:server MODE " + room + " +o " + joinedUser.getName();
                }
                return msg;
            }
            case SELF_JOIN: {
                Chatter chatter = message.get(MessageProperty.CHATTER);
                String room = message.get(MessageProperty.ROOM);
                String name = chatter.getName();
                String msg = ":" + name + "!" + name + "@" + serverName + " JOIN " + room;
                if (chatter.hasRole(LocalRole.MOD)) {
                    msg = msg + "\r\n:server MODE " + room + " +o " + name;
                }
                return msg + "\r\n:server TOPIC " + room + " :yoba.vg";
            }
            case PART: {
                String room = message.get(MessageProperty.ROOM);
                String name = message.get(MessageProperty.NAME);
                return ":" + name + "!" + name + "@" + serverName + " PART " + room;
            }
            case PONG:
                String text = message.get(MessageProperty.TEXT);
                if (text != null) {
                    return ':' + serverName + " PONG " + serverName + " :" + text;
                } else {
                    return ':' + serverName + " PONG " + serverName;
                }
            default:
                return null;
            //return ":frontend PRIVMSG " + user.getName() + " :" + message.getType() + ' ' + message.getArgs();
        }
    }

    @Override
    public Message decode(String message) {
        ParsedMessage parsedMessage = parseMessage(message.trim());
        String command = parsedMessage.getArg()[0];
        switch (command) {
            case "PRIVMSG":
                String room = parsedMessage.getArg()[1];
                if (room.startsWith("#")) {
                    String msg = parsedMessage.getArg()[2].trim();
                    if (msg.startsWith("ACTION")) {
                        return new Message(ImmutableMap.of(
                            MessageProperty.TYPE, MessageType.ME,
                            MessageProperty.ROOM, room,
                            MessageProperty.TEXT, msg.substring("ACTION ".length())
                        ));
                    } else {
                        return new Message(ImmutableMap.of(
                            MessageProperty.TYPE, MessageType.MSG,
                            MessageProperty.ROOM, room,
                            MessageProperty.TEXT, msg
                        ));
                    }
                } else {
                    return new Message(ImmutableMap.of(
                        MessageProperty.TYPE, MessageType.UNKNOWN
                    ));
                }
            case "PING":
                return new Message(ImmutableMap.of(
                    MessageProperty.TYPE, MessageType.PING,
                    MessageProperty.TEXT, parsedMessage.getArg()[1]
                ));
            case "PART":
                if (parsedMessage.getArg().length == 2) {
                    return new Message(ImmutableMap.of(
                        MessageProperty.TYPE, MessageType.PART,
                        MessageProperty.TEXT, parsedMessage.getArg()[1]
                    ));
                } else {
                    return new Message(ImmutableMap.of(
                        MessageProperty.TYPE, MessageType.UNKNOWN
                    ));
                }
            case "JOIN":
                return new Message(ImmutableMap.of(
                    MessageProperty.TYPE, MessageType.JOIN,
                    MessageProperty.ROOM, parsedMessage.getArg()[1]
                ));
            default:
                try {
                    if (parsedMessage.getArg().length > 1) {
                        return new Message(ImmutableMap.of(
                            MessageProperty.TYPE, MessageType.valueOf(command),
                            MessageProperty.TEXT, parsedMessage.getArg()[1]
                        ));
                    } else {
                        return new Message(ImmutableMap.of(
                            MessageProperty.TYPE, MessageType.valueOf(command)
                        ));
                    }
                } catch (IllegalArgumentException e) {
                    return new Message(ImmutableMap.of(
                        MessageProperty.TYPE, MessageType.UNKNOWN
                    ));
                }
        }
    }
}
