package lexek.wschat.chat.msg;

import java.util.List;

public interface MessageProcessingService {
    List<MessageNode> processMessage(String message, boolean root);
}
