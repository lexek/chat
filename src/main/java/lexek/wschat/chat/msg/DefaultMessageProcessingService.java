package lexek.wschat.chat.msg;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

public class DefaultMessageProcessingService implements MessageProcessingService {
    private final List<MessageProcessor> processors;

    public DefaultMessageProcessingService() {
        this.processors = new ArrayList<>();
        this.processors.add(new UrlMessageProcessor());
    }

    @Override
    public List<MessageNode> processMessage(String message, boolean isRoot) {
        List<MessageNode> nodes = ImmutableList.of(MessageNode.textNode(message));
        processMessage(nodes, isRoot);
        return nodes;
    }

    public void processMessage(List<MessageNode> message, boolean isRoot) {
        //todo
    }
}
