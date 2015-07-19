package lexek.wschat.chat.handlers;

import lexek.wschat.chat.*;
import lexek.wschat.db.model.Chatter;

import java.util.List;

public class SetRoleHandler extends AbstractMsgHandler {

    public SetRoleHandler(RoomManager roomManager) {
        super(MessageType.ROLE, 3, false, roomManager);
    }

    private static boolean canChangeRole(Chatter modChatter, Chatter userChatter, LocalRole newRole) {
        User user = userChatter.getUser();
        User modUser = modChatter.getUser();
        return newRole != LocalRole.GUEST &&
            (
                (
                    modChatter.hasRole(LocalRole.ADMIN) &&
                        modChatter.hasGreaterRole(userChatter.getRole())
                ) || (
                    user != null &&
                        modUser.hasRole(GlobalRole.ADMIN) &&
                        modUser.hasGreaterRole(user.getRole())
                )
            );
    }

    @Override
    protected void handle(Connection connection, Room room, Chatter modChatter, List<String> args) {
        LocalRole newRole;
        try {
            newRole = LocalRole.valueOf(args.get(2));
        } catch (IllegalArgumentException e) {
            connection.send(Message.errorMessage("UNKNOWN_ROLE"));
            return;
        }
        User mod = connection.getUser();
        if (modChatter.hasRole(LocalRole.ADMIN) || mod.hasRole(GlobalRole.ADMIN)) {
            Chatter userChatter = room.fetchChatter(args.get(1));
            if (userChatter != null && userChatter.getId() != null) {
                if (canChangeRole(modChatter, userChatter, newRole)) {
                    if (room.setRole(userChatter, modChatter, newRole)) {
                        connection.send(Message.infoMessage("OK"));
                    } else {
                        connection.send(Message.errorMessage("INTERNAL_ERROR"));
                    }
                } else {
                    connection.send(Message.errorMessage("BAN_DENIED"));
                }
            } else {
                connection.send(Message.errorMessage("UNKNOWN_USER"));
            }
        } else {
            connection.send(Message.errorMessage("NOT_AUTHORIZED"));
        }
    }
}
