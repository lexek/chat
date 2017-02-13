package lexek.wschat.chat.msg;

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
                String prefix = style.getPrefix();
                if (text.startsWith(prefix)) {
                    //hack for quotes
                    if (prefix.equals(">") && text.length() > 1 && text.charAt(1) == '(') {
                        return;
                    }
                    String prefixlessText = text.substring(prefix.length());
                    MessageNode newNode = MessageNode.styledNode(
                        text,
                        processingService.processMessage(prefixlessText, false),
                        style.getStyle()
                    );
                    message.clear();
                    message.add(newNode);
                    break;
                }
            }
        }
    }

    @Override
    public boolean handlesChildren() {
        return false;
    }
}
