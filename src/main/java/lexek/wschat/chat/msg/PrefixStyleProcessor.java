package lexek.wschat.chat.msg;

import com.google.common.collect.Lists;

import java.util.List;

public class PrefixStyleProcessor implements MessageProcessor {
    private final List<PrefixStyleDescription> styles;
    private final MessageProcessingService processingService;

    public PrefixStyleProcessor(List<PrefixStyleDescription> styles, MessageProcessingService processingService) {
        this.styles = styles;
        this.processingService = processingService;
    }

    @Override
    public void process(List<MessageNode> message) {
        if (message.size() != 1) {
            throw new IllegalArgumentException("message length should be equal to 1");
        }
        MessageNode firstNode = message.get(0);
        String text = firstNode.getText();
        if (firstNode.getType() == MessageNode.Type.TEXT) {
            for (PrefixStyleDescription style : styles) {
                if (text.startsWith(style.getPrefix())) {
                    MessageNode newNode = MessageNode.styledNode(text, Lists.newLinkedList(message), style.getStyle());
                    message.clear();
                    message.add(newNode);
                    processingService.processMessage(text, false);
                    break;
                }
            }
        }
    }

    @Override
    public boolean handlesRoot() {
        return true;
    }

    @Override
    public boolean handlesChildren() {
        return false;
    }
}
