package lexek.wschat.chat.msg;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class DefaultMessageProcessingService implements MessageProcessingService {
    private final List<MessageProcessor> processors;

    public DefaultMessageProcessingService() {
        this.processors = new ArrayList<>();
    }

    public void addProcessor(MessageProcessor processor) {
        this.processors.add(processor);
    }

    @Override
    public List<MessageNode> processMessage(String message, boolean isRoot) {
        LinkedList<MessageNode> nodes = new LinkedList<>();
        nodes.add(MessageNode.textNode(message));
        processMessage(nodes, isRoot);
        return nodes;
    }

    public void processMessage(List<MessageNode> message, boolean isRoot) {
        for (MessageProcessor processor : processors) {
            if (!isRoot && processor.handlesChildren()) {
                continue;
            }
            processor.process(message);
        }
    }
}
