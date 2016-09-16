package lexek.wschat.chat.msg;

import java.util.List;

public interface MessageProcessor {
    List<MessageNode> process(List<MessageNode> message);
}
