package lexek.wschat.chat.processing;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import lexek.wschat.chat.*;
import lexek.wschat.security.CaptchaService;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class HandlerInvoker {
    private static final Set<MessageType> MODERATED_TYPES =
        ImmutableSet.of(MessageType.MSG, MessageType.ME, MessageType.LIKE);

    private final Map<MessageType, GlobalMessageHandler> globalMessageHandlers;
    private final Map<MessageType, RoomMessageHandler> roomMessageHandlers;
    private final RoomManager roomManager;
    private final Set<String> bannedIps;
    private final CaptchaService captchaService;

    public HandlerInvoker(RoomManager roomManager, Set<String> bannedIps, CaptchaService captchaService) {
        this.roomManager = roomManager;
        this.bannedIps = bannedIps;
        this.captchaService = captchaService;
        this.globalMessageHandlers = new HashMap<>();
        this.roomMessageHandlers = new HashMap<>();
    }

    public void register(MessageHandler handler) {
        if (handler instanceof GlobalMessageHandler) {
            if (globalMessageHandlers.containsKey(handler.getType())) {
                throw new RuntimeException("handler for this type is already registered");
            }
            globalMessageHandlers.put(handler.getType(), (GlobalMessageHandler) handler);
        }
        if (handler instanceof RoomMessageHandler) {
            if (roomMessageHandlers.containsKey(handler.getType())) {
                throw new RuntimeException("handler for this type is already registered");
            }
            roomMessageHandlers.put(handler.getType(), (RoomMessageHandler) handler);
        }
    }

    public void handle(Connection connection, Message message) {
        User user = connection.getUser();
        String roomName = message.getRoom();
        if (roomName != null) {
            RoomMessageHandler handler = roomMessageHandlers.get(message.getType());

            //check if handler exists
            if (handler == null) {
                connection.send(Message.errorMessage("UNKNOWN_COMMAND"));
                return;
            }

            //check required properties
            if (!handler.requiredProperties().stream().allMatch(property -> message.get(property) != null)) {
                connection.send(Message.errorMessage("BAD_MESSAGE"));
                return;
            }

            //check message timeout
            int interval = user.getRole().getMessageTimeInterval();
            long timeFromLastMessage = System.currentTimeMillis() - user.getLastMessage();
            if (!handler.isNeedsInterval() || (interval == 0) || (timeFromLastMessage > interval)) {
                if (handler.isNeedsInterval()) {
                    user.setLastMessage(System.currentTimeMillis());
                }
            } else {
                connection.send(Message.errorMessage("TOO_FAST"));
                return;
            }

            Room room = roomManager.getRoomInstance(roomName);

            //check if room exists
            if (room == null) {
                connection.send(Message.errorMessage("UNKNOWN_ROOM"));
                return;
            }

            //if join not required just handle message without further checks
            if (!handler.joinRequired()) {
                handler.handle(connection, user, room, null, message);
                return;
            }

            //check if joined
            if (!room.inRoom(connection)) {
                connection.send(Message.errorMessage("NOT_JOINED"));
                return;
            }

            Chatter chatter = room.getOnlineChatter(connection.getUser().getWrappedObject());

            //check if user can invoke handler
            if (chatter == null || !chatter.hasRole(handler.getRole())) {
                connection.send(Message.errorMessage("NOT_AUTHORIZED"));
                return;
            }

            if (MODERATED_TYPES.contains(message.getType())) {
                if (!(chatter.hasRole(LocalRole.MOD))) {
                    if (chatter.isBanned()) {
                        connection.send(Message.errorMessage("BAN"));
                        return;
                    } else if (chatter.getTimeout() != null) {
                        if (chatter.getTimeout() < System.currentTimeMillis()) {
                            room.removeTimeout(chatter);
                            handle(connection, message);
                            return;
                        } else {
                            long t = (chatter.getTimeout() - System.currentTimeMillis()) / 1000;
                            connection.send(Message.errorMessage("TIMEOUT", ImmutableList.of(t)));
                            return;
                        }
                    }
                }
                if (user.getRole() == GlobalRole.UNAUTHENTICATED) {
                    connection.send(Message.emptyMessage(MessageType.AUTH_REQUIRED));
                    return;
                }
                if (user.getRole().equals(GlobalRole.USER_UNCONFIRMED)) {
                    connection.send(Message.errorMessage("UNVERIFIED_EMAIL"));
                    return;
                }
                if (bannedIps.contains(connection.getIp()) && !user.hasRole(GlobalRole.MOD)) {
                    captchaService.tryAuthorize(
                        connection,
                        () -> handler.handle(connection, connection.getUser(), room, chatter, message)
                    );
                    return;
                }
            }
            handler.handle(connection, connection.getUser(), room, chatter, message);
        } else {
            GlobalMessageHandler handler = globalMessageHandlers.get(message.getType());
            if (handler != null) {
                if (!handler.requiredProperties().stream().allMatch(property -> message.get(property) != null)) {
                    connection.send(Message.errorMessage("BAD_MESSAGE"));
                    return;
                }
                int interval = user.getRole().getMessageTimeInterval();
                long timeFromLastMessage = System.currentTimeMillis() - user.getLastMessage();
                if (!handler.isNeedsInterval() || (interval == 0) || (timeFromLastMessage > interval)) {
                    if (handler.isNeedsInterval()) {
                        user.setLastMessage(System.currentTimeMillis());
                    }
                } else {
                    connection.send(Message.errorMessage("TOO_FAST"));
                    return;
                }

                if (user.hasRole(handler.getRole())) {
                    handler.handle(connection, user, message);
                } else {
                    connection.send(Message.errorMessage("NOT_AUTHORIZED"));
                }
            } else {
                connection.send(Message.errorMessage("UNKNOWN_COMMAND"));
            }
        }
    }
}
