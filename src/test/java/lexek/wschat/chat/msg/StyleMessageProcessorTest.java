package lexek.wschat.chat.msg;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class StyleMessageProcessorTest {
    @Test
    public void shouldHandleSimpleStyle() {
        List<StyleDescription> styles = ImmutableList.of(
            new StyleDescription(Pattern.compile("\\*\\*([^*]+)\\*\\*"), MessageNode.Style.BOLD)
        );
        MessageProcessingService messageProcessingService = Mockito.spy(new MessageProcessingService() {
            @Override
            public List<MessageNode> processMessage(String message, boolean root) {
                return ImmutableList.of(MessageNode.textNode(message));
            }
        });
        StyleMessageProcessor processor = new StyleMessageProcessor(styles, messageProcessingService);

        List<MessageNode> message = Lists.newArrayList(MessageNode.textNode("**kek**"));

        processor.process(message);

        MessageNode styledNode = MessageNode.styledNode(
            "kek",
            ImmutableList.of(MessageNode.textNode("kek")),
            MessageNode.Style.BOLD
        );
        List<MessageNode> expectedResult = ImmutableList.of(styledNode);
        assertEquals(message, expectedResult);
        Mockito.verify(messageProcessingService).processMessage("kek", false);
    }

    @Test
    public void shouldHandleSimpleStyleWithComplexChildrenProcessing() {
        List<StyleDescription> styles = ImmutableList.of(
            new StyleDescription(Pattern.compile("\\*\\*([^*]+)\\*\\*"), MessageNode.Style.BOLD)
        );
        MessageNode childNode = MessageNode.emoticonNode("kek", "kek.png", 11, 12);
        //todo: create custom message processing services for tests
        MessageProcessingService messageProcessingService = Mockito.spy(new MessageProcessingService() {
            @Override
            public List<MessageNode> processMessage(String message, boolean root) {
                return ImmutableList.of(childNode);
            }
        });
        StyleMessageProcessor processor = new StyleMessageProcessor(styles, messageProcessingService);

        List<MessageNode> message = Lists.newArrayList(MessageNode.textNode("**kek**"));

        processor.process(message);

        MessageNode styledNode = MessageNode.styledNode("kek", ImmutableList.of(childNode), MessageNode.Style.BOLD);
        List<MessageNode> expectedResult = ImmutableList.of(styledNode);
        assertEquals(message, expectedResult);
        Mockito.verify(messageProcessingService).processMessage("kek", false);
    }
}