package lexek.wschat.chat.msg;

import java.util.List;

public interface MessageProcessor {
    void process(List<MessageNode> message);

    boolean handlesChildren();
}
