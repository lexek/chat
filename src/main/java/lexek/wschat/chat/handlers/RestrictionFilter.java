package lexek.wschat.chat.handlers;

import lexek.wschat.chat.*;
import lexek.wschat.security.CaptchaService;

import java.util.List;
import java.util.Set;

public class RestrictionFilter extends AbstractMessageHandler {
    private final CaptchaService captchaService;
    private final MessageHandler handler;
    private final Set<String> bannedIps;

    public RestrictionFilter(CaptchaService captchaService, MessageHandler handler, Set<String> bannedIps) {
        super(handler.getType(), GlobalRole.UNAUTHENTICATED, handler.getArgCount(), handler.isNeedsInterval(), handler.isNeedsLogging());
        this.captchaService = captchaService;
        this.handler = handler;
        this.bannedIps = bannedIps;
    }

    @Override
    public void handle(final List<String> args, final Connection connection) {
        final User user = connection.getUser();
        if (user.getRole() == GlobalRole.UNAUTHENTICATED) {
            connection.send(Message.emptyMessage(MessageType.AUTH_REQUIRED));
        } else if (user.getRole().equals(GlobalRole.USER_UNCONFIRMED)) {
            connection.send(Message.errorMessage("UNVERIFIED_EMAIL"));
        } else {
            if (bannedIps.contains(connection.getIp()) && (user.getRole().compareTo(GlobalRole.MOD) < 0)) {
                Runnable r = () -> handler.handle(args, connection);
                captchaService.tryAuthorize(connection, r);
            } else {
                handler.handle(args, connection);
            }
        }
    }
}
