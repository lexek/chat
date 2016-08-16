package lexek.wschat.chat.handlers;

import com.google.common.collect.ImmutableSet;
import lexek.wschat.chat.Connection;
import lexek.wschat.chat.model.*;
import lexek.wschat.chat.processing.AbstractGlobalMessageHandler;
import lexek.wschat.services.UserService;
import lexek.wschat.util.Colors;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;

@Service
public class ColorHandler extends AbstractGlobalMessageHandler {
    private final UserService userService;

    @Inject
    public ColorHandler(UserService userService) {
        super(
            ImmutableSet.of(
                MessageProperty.COLOR
            ),
            MessageType.COLOR,
            GlobalRole.USER,
            true
        );

        this.userService = userService;
    }

    @Override
    public void handle(Connection connection, User user, Message message) {
        String color = message.get(MessageProperty.COLOR);
        String colorCode = Colors.getColorCode(color, user.getRole() != GlobalRole.SUPERADMIN);

        if (colorCode != null) {
            if (user.hasRole(GlobalRole.USER)) {
                userService.setColor(user.getWrappedObject(), colorCode);
            }
            user.setColor(colorCode);
            connection.send(Message.colorMessage(colorCode));
        } else {
            connection.send(Message.errorMessage("WRONG_COLOR"));
        }
    }
}
