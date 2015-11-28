package lexek.wschat.frontend;

import lexek.wschat.chat.model.Message;

public interface Codec {
    String encode(Message message);

    Message decode(String message);
}
