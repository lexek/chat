package lexek.wschat.frontend.irc;

import com.google.common.collect.ImmutableMap;
import lexek.wschat.chat.*;
import lexek.wschat.frontend.Codec;

import java.util.Arrays;
import java.util.List;

public class IrcCodec implements Codec {
    private final String serverName;

    public IrcCodec(String serverName) {
        this.serverName = serverName;
    }

    private static ParsedMessage parseMessage(String msg) {
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

        return new ParsedMessage(prefix, arg);
    }

    @Override
    public String encode(Message message, User user) {
        switch (message.getType()) {
            case MSG: {
                String room = message.get(MessageProperty.ROOM);
                String name = message.get(MessageProperty.NAME);
                String text = message.get(MessageProperty.TEXT);
                return ":" + name + " PRIVMSG " + room + " :" + text;
            }
            case RECAPTCHA: {
                String captchaId = message.get(MessageProperty.TEXT);
                return ":server PRIVMSG " + user.getName() + " :https://" + serverName + ":1337/recaptcha/" + captchaId;
            }
            case NAMES: {
                List<Chatter> users = message.get(MessageProperty.NAMES);
                String room = message.get(MessageProperty.ROOM);
                StringBuilder sb = new StringBuilder();
                for (Chatter u : users) {
                    if (u.hasRole(LocalRole.MOD)) {
                        sb.append("@").append(u.getUser().getName()).append(' ');
                    } else {
                        sb.append(u.getUser().getName()).append(' ');
                    }
                }
                return ":" + user.getName() + " 353 " + user.getName() + " = " + room + " :@server " + sb.toString() + "\r\n" +
                    ":" + user.getName() + " 366 " + user.getName() + " " + room + " :End of /NAMES list";
            }
            case JOIN: {
                String room = message.get(MessageProperty.ROOM);
                String msg = ":" + user.getName() + "!" + user.getName() + "@" + serverName + " JOIN " + room;
                if (user.getRole().compareTo(GlobalRole.MOD) >= 0) {
                    msg += "\r\n:server MODE " + room + " +o " + user.getName();
                }
                return msg;
            }
            case SELF_JOIN: {
                String room = message.get(MessageProperty.ROOM);
                String msg;
                if (user.getRole().compareTo(GlobalRole.MOD) >= 0) {
                    msg = ":" + user.getName() + "!" + user.getName() + "@" + serverName + " JOIN " + room + "\r\n" +
                        ":server MODE " + room + " +o " + user.getName();
                } else {
                    msg = ":" + user.getName() + "!" + user.getName() + "@" + serverName + " JOIN " + room;
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
                    MessageProperty.TEXT, parsedMessage.getArg()[1]
                ));
            default:
                try {
                    return new Message(ImmutableMap.of(
                        MessageProperty.TYPE, MessageType.valueOf(command),
                        MessageProperty.TEXT, parsedMessage.getArg()[1]
                    ));
                } catch (IllegalArgumentException e) {
                    return new Message(ImmutableMap.of(
                        MessageProperty.TYPE, MessageType.UNKNOWN
                    ));
                }
        }
    }
}
