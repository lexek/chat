package lexek.wschat.frontend.irc;

import lexek.wschat.chat.*;
import lexek.wschat.db.model.Chatter;
import lexek.wschat.frontend.Codec;

import java.util.Arrays;
import java.util.List;

import static lexek.wschat.chat.Message.Keys.*;

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
                String room = message.get(ROOM);
                String name = message.get(NAME);
                String text = message.get(TEXT);
                return ":" + name + " PRIVMSG " + room + " :" + text;
            }
            case RECAPTCHA: {
                String captchaId = message.get(TEXT);
                return ":server PRIVMSG " + user.getName() + " :https://" + serverName + ":1337/recaptcha/" + captchaId;
            }
            case NAMES: {
                List<Chatter> users = message.get(NAMES);
                String room = message.get(ROOM);
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
                String room = message.get(ROOM);
                String msg = ":" + user.getName() + "!" + user.getName() + "@" + serverName + " JOIN " + room;
                if (user.getRole().compareTo(GlobalRole.MOD) >= 0) {
                    msg += "\r\n:server MODE " + room + " +o " + user.getName();
                }
                return msg;
            }
            case SELF_JOIN: {
                String room = message.get(ROOM);
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
                String room = message.get(ROOM);
                String name = message.get(NAME);
                return ":" + name + "!" + name + "@" + serverName + " PART " + room;
            }
            case PONG:
                String text = message.get(TEXT);
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
    public InboundMessage decode(String message) {
        ParsedMessage parsedMessage = parseMessage(message.trim());
        String command = parsedMessage.getArg()[0];
        switch (command) {
            case "ACTION":
                return new InboundMessage(MessageType.ME, parsedMessage.getArg()[2]);
            case "PRIVMSG":
                String room = parsedMessage.getArg()[1];
                if (room.startsWith("#")) {
                    String msg = parsedMessage.getArg()[2].trim();
                    if (msg.startsWith("ACTION")) {
                        return new InboundMessage(MessageType.ME, room, msg.substring("ACTION ".length()));
                    } else if (msg.startsWith("/")) {
                        String[] tmp = msg.split(" ", 2);
                        try {
                            return new InboundMessage(MessageType.valueOf(tmp[0].toUpperCase()), tmp[1]);
                        } catch (IllegalArgumentException e) {
                            return new InboundMessage(MessageType.UNKNOWN, tmp);
                        }
                    } else {
                        return new InboundMessage(MessageType.MSG, room, msg);
                    }
                } else {
                    return new InboundMessage(MessageType.UNKNOWN, parsedMessage.getArg());
                }
            case "PING":
                return new InboundMessage(MessageType.PING, parsedMessage.getArg()[1]);
            case "PART":
                if (parsedMessage.getArg().length == 2) {
                    return new InboundMessage(MessageType.PART, parsedMessage.getArg()[1]);
                } else {
                    return new InboundMessage(MessageType.UNKNOWN, parsedMessage.getArg());
                }
            case "JOIN":
                return new InboundMessage(MessageType.JOIN, parsedMessage.getArg()[1]);
            default:
                try {
                    return new InboundMessage(MessageType.valueOf(command), Arrays.copyOfRange(parsedMessage.getArg(), 1, parsedMessage.getArg().length));
                } catch (IllegalArgumentException e) {
                    return new InboundMessage(MessageType.UNKNOWN, parsedMessage.getArg());
                }
        }
    }
}
