package lexek.wschat.chat.msg;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import lexek.wschat.db.model.Emoticon;
import lexek.wschat.services.EmoticonService;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class EmoticonMessageProcessorTest {
    @Test
    public void shoudHandleSingleEmoticon() {
        EmoticonService emoticonService = Mockito.mock(EmoticonService.class);
        Mockito.when(emoticonService.getEmoticons()).thenReturn(ImmutableList.of(
            new Emoticon(0L, "Kappa", "Kappa.png", 30, 30).initPattern()
        ));
        EmoticonMessageProcessor processor = new EmoticonMessageProcessor(emoticonService);

        List<MessageNode> message = Lists.newArrayList(MessageNode.textNode("Kappa"));

        processor.process(message);

        List<MessageNode> expectedResult = ImmutableList.of(
            MessageNode.emoticonNode("Kappa", "Kappa.png", 30, 30)
        );

        assertEquals(message, expectedResult);
    }

    @Test
    public void shoudHandleEmoticonInText() {
        EmoticonService emoticonService = Mockito.mock(EmoticonService.class);
        Mockito.when(emoticonService.getEmoticons()).thenReturn(ImmutableList.of(
            new Emoticon(0L, "Kappa", "Kappa.png", 30, 30).initPattern()
        ));
        EmoticonMessageProcessor processor = new EmoticonMessageProcessor(emoticonService);

        List<MessageNode> message = Lists.newArrayList(MessageNode.textNode("top Kappa kek"));

        processor.process(message);

        List<MessageNode> expectedResult = ImmutableList.of(
            MessageNode.textNode("top "),
            MessageNode.emoticonNode("Kappa", "Kappa.png", 30, 30),
            MessageNode.textNode(" kek")
        );

        assertEquals(message, expectedResult);
    }

    @Test
    public void shouldHandleChildren() {
        EmoticonService emoticonService = Mockito.mock(EmoticonService.class);
        EmoticonMessageProcessor processor = new EmoticonMessageProcessor(emoticonService);
        assertEquals(processor.handlesChildren(), true);
    }
}