package lexek.wschat.chat.msg;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

public class MessageProcessingService {
    private final List<MessageProcessor> processors;

    public MessageProcessingService() {
        this.processors = new ArrayList<>();
        this.processors.add(new UrlMessageProcessor());
    }

    public List<MessageNode> parseMessage(String text) {
        List<MessageNode> nodes = ImmutableList.of(MessageNode.textNode(text));

        return null;
    }
}
