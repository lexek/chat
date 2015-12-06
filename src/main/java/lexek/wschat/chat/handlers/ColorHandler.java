package lexek.wschat.chat.handlers;

import com.google.common.collect.ImmutableSet;
import lexek.wschat.chat.Connection;
import lexek.wschat.chat.model.*;
import lexek.wschat.chat.processing.AbstractGlobalMessageHandler;
import lexek.wschat.db.dao.UserDao;
import lexek.wschat.util.Colors;

public class ColorHandler extends AbstractGlobalMessageHandler {
    private final UserDao userDao;

    public ColorHandler(UserDao userDao) {
        super(
            ImmutableSet.of(
                MessageProperty.COLOR
            ),
            MessageType.COLOR,
            GlobalRole.USER,
            true
        );

        this.userDao = userDao;
    }

    @Override
    public void handle(Connection connection, User user, Message message) {
        String color = message.get(MessageProperty.COLOR);
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
