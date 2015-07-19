package lexek.wschat.chat.handlers;

import lexek.wschat.chat.*;
import lexek.wschat.chat.Chatter;

import java.util.List;

public abstract class AbstractModerationHandler extends AbstractMsgHandler {
    private final boolean fetchChatterIfOffline;
    private final String deniedErrorName;

    AbstractModerationHandler(MessageType type,
                              RoomManager roomManager,
                              boolean fetchChatterIfOffline,
                              String deniedErrorName) {
        super(type, 2, false, roomManager);
        this.fetchChatterIfOffline = fetchChatterIfOffline;
        this.deniedErrorName = deniedErrorName;
    }

    private static boolean canBan(Chatter modChatter, Chatter userChatter) {
        User user = userChatter.getUser();
        User modUser = modChatter.getUser();
        return !userChatter.hasRole(LocalRole.MOD) &&
            (
                modChatter.hasRole(LocalRole.MOD) &&
                modChatter.hasGreaterRole(userChatter.getRole()) &&
                modUser.hasGreaterRole(user.getRole())
            );
    }

    @Override
    final protected void handle(Connection connection, Room room, Chatter modChatter, List<String> args) {
        if (modChatter.hasRole(LocalRole.MOD)) {
            Chatter userChatter;
            if (fetchChatterIfOffline) {
                userChatter = room.fetchChatter(args.get(1));
            } else {
                userChatter = room.getChatter(args.get(1));
            }
            if (userChatter != null && userChatter.getId() != null) {
                if (canBan(modChatter, userChatter)) {
                    if (performOperation(room, modChatter, userChatter)) {
                        success(connection, room, modChatter, userChatter);
                    } else {
                        connection.send(Message.errorMessage("INTERNAL_ERROR"));
                    }
                } else {
                    connection.send(Message.errorMessage(deniedErrorName));
                }
            } else {
                connection.send(Message.errorMessage("UNKNOWN_USER"));
            }
        } else {
            connection.send(Message.errorMessage("NOT_AUTHORIZED"));
        }
    }

    protected abstract boolean performOperation(Room room, Chatter mod, Chatter userChatter);

    protected abstract void success(Connection connection, Room room, Chatter modChatter, Chatter userChatter);
}
