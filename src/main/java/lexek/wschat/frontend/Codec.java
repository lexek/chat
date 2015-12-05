package lexek.wschat.frontend;

import lexek.wschat.chat.model.Message;

public interface Codec {
    /**
     * encodes message
     * @param message message to encode
     * @return message encoded as string, null when it shouldn't be sent
     */
    String encode(Message message);

    /**
     * decodes message
     * @param message encoded string
     * @return decoded message, when message can't be decoded should return message with UNKNOWN type
     */
    Message decode(String message);
}
