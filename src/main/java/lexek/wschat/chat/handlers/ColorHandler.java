package lexek.wschat.chat.handlers;

import lexek.wschat.chat.*;
import lexek.wschat.db.UserDao;
import lexek.wschat.util.Colors;

import java.util.List;

public class ColorHandler extends AbstractMessageHandler {
    private final UserDao userDao;

    public ColorHandler(UserDao userDao) {
        super(MessageType.COLOR, GlobalRole.USER, 1, false, false);

        this.userDao = userDao;
    }

    @Override
    public void handle(List<String> args, Connection connection) {
        User user = connection.getUser();
        String color = args.get(0).toLowerCase();
        String colorCode = Colors.getColorCode(color, user.getRole() != GlobalRole.SUPERADMIN);

        if (colorCode != null) {
            if (user.hasRole(GlobalRole.USER)) {
                userDao.setColor(user.getId(), colorCode);
            }
            user.setColor(colorCode);
            connection.send(Message.colorMessage(colorCode));
        } else {
            connection.send(Message.errorMessage("WRONG_COLOR"));
        }
    }
}
