package lexek.wschat.chat.msg;

import com.google.common.collect.ImmutableList;

import java.util.List;

public class TextReturningMessageProcessingService implements MessageProcessingService {
    @Override
    public List<MessageNode> processMessage(String message, boolean root) {
        return ImmutableList.of(MessageNode.textNode(message));
    }
}
