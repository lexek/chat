package lexek.wschat.chat.handlers;

import lexek.wschat.chat.*;
import lexek.wschat.services.UserService;
import lexek.wschat.util.Names;

import java.util.List;

public class NameHandler extends AbstractMessageHandler {
    private final UserService userService;

    public NameHandler(UserService userService) {
        super(MessageType.NAME, GlobalRole.USER, 1, false, false);

        this.userService = userService;
    }

    @Override
    public void handle(List<String> args, Connection connection) {
        User user = connection.getUser();
        if (connection.getUser().isRenameAvailable()) {
            String newName = args.get(0).toLowerCase();
            if (Names.USERNAME_PATTERN.matcher(newName).matches()) {
                if (!userService.changeName(user.getWrappedObject(), newName)) {
                    connection.send(Message.errorMessage("NAME_TAKEN"));
                }
            } else {
                connection.send(Message.errorMessage("NAME_BAD_FORMAT"));
            }
        } else if (user.getRole() == GlobalRole.SUPERADMIN) {
            String newName = args.get(0);
            if (newName != null && !newName.isEmpty()) {
                if (!userService.changeName(user.getWrappedObject(), newName)) {
                    connection.send(Message.errorMessage("NAME_TAKEN"));
                }
            } else {
                connection.send(Message.errorMessage("NAME_BAD_FORMAT"));
            }
        } else {
            connection.send(Message.errorMessage("NAME_DENIED"));
        }
    }
}
