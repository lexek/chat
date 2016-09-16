package lexek.wschat.chat.msg;

import com.google.common.collect.ImmutableList;
import lexek.wschat.db.model.Emoticon;
import lexek.wschat.services.EmoticonService;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.Assert.*;

public class EmoticonMessageProcessorTest {
    @Test
    public void shoudHandleSingleEmoticon() {
        EmoticonService emoticonService = Mockito.mock(EmoticonService.class);
        Mockito.when(emoticonService.getEmoticons()).thenReturn(ImmutableList.of(
            new Emoticon(0L, "Kappa", "Kappa.png", 30, 30).initPattern()
        ));
        EmoticonMessageProcessor processor = new EmoticonMessageProcessor(emoticonService);

        List<MessageNode> message = ImmutableList.of(MessageNode.textNode("Kappa"));

        List<MessageNode> actualResult = processor.process(message);

        List<MessageNode> expectedResult = ImmutableList.of(
            MessageNode.emoticonNode("Kappa", "Kappa.png", 30, 30)
        );

        assertEquals(actualResult, expectedResult);

    }

    @Test
    public void shoudHandleEmoticonInText() {
        EmoticonService emoticonService = Mockito.mock(EmoticonService.class);
        Mockito.when(emoticonService.getEmoticons()).thenReturn(ImmutableList.of(
            new Emoticon(0L, "Kappa", "Kappa.png", 30, 30).initPattern()
        ));
        EmoticonMessageProcessor processor = new EmoticonMessageProcessor(emoticonService);

        List<MessageNode> message = ImmutableList.of(MessageNode.textNode("top Kappa kek"));

        List<MessageNode> actualResult = processor.process(message);

        List<MessageNode> expectedResult = ImmutableList.of(
            MessageNode.textNode("top "),
            MessageNode.emoticonNode("Kappa", "Kappa.png", 30, 30),
            MessageNode.textNode(" kek")
        );

        assertEquals(actualResult, expectedResult);
    }
}