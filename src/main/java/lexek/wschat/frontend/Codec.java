package lexek.wschat.frontend;

import lexek.wschat.chat.model.Message;
import lexek.wschat.chat.model.User;

public interface Codec {
    String encode(Message message, User user);

    Message decode(String message);
}
