package lexek.wschat.chat.msg;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class PrefixStyleProcessorTest {
    @Test
    public void shouldHandleSimpleStyle() {
        List<PrefixStyleDescription> styles = ImmutableList.of(
            new PrefixStyleDescription(">", MessageNode.Style.QUOTE),
            new PrefixStyleDescription("!!!", MessageNode.Style.NSFW)
        );
        MessageProcessingService messageProcessingService = Mockito.spy(new TextReturningMessageProcessingService());
        PrefixStyleProcessor processor = new PrefixStyleProcessor(styles, messageProcessingService);
        List<MessageNode> message = Lists.newArrayList(MessageNode.textNode(">!!!kek"));

        processor.process(message);

        MessageNode styledNode = MessageNode.styledNode(
            ">!!!kek",
            ImmutableList.of(MessageNode.textNode("!!!kek")),
            MessageNode.Style.QUOTE
        );
        List<MessageNode> expectedResult = ImmutableList.of(styledNode);
        assertEquals(message, expectedResult);
        Mockito.verify(messageProcessingService).processMessage("!!!kek", false);
    }

    @Test
    public void shouldNotHandleChildren() {
        PrefixStyleProcessor processor = new PrefixStyleProcessor(null, null);
        assertEquals(processor.handlesChildren(), false);
    }
}