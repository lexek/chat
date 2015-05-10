package lexek.wschat.frontend;

import lexek.wschat.chat.InboundMessage;
import lexek.wschat.chat.Message;
import lexek.wschat.chat.User;

public interface Codec {
    String encode(Message message, User user);

    InboundMessage decode(String message);
}
