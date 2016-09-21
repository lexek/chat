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

    public List<MessageNode> processMessage(String message) {
        List<MessageNode> nodes = ImmutableList.of(MessageNode.textNode(message));
        processMessage(nodes);
        return nodes;
    }

    public void processMessage(List<MessageNode> message) {
        //todo
    }
}
