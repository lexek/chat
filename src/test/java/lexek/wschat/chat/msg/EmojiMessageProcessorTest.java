package lexek.wschat.chat.msg;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * based on
 * <a href="https://gist.github.com/heyarny/71c246f2f7fa4d9d10904fb9d5b1fa1d">
 * gist.github.com/heyarny/71c246f2f7fa4d9d10904fb9d5b1fa1d
 * </a>
 */
public class EmojiMessageProcessorTest {
    @Test
    public void shouldHandleSimpleEmoji() {
        EmojiMessageProcessor processor = new EmojiMessageProcessor();

        List<MessageNode> message = Lists.newArrayList(MessageNode.textNode("\uD83D\uDC4C"));

        processor.process(message);

        List<MessageNode> expectedResult = ImmutableList.of(
            MessageNode.emojiNode("\uD83D\uDC4C", "1f44c")
        );

        assertEquals(expectedResult, message);
    }

    @Test
    public void shouldHandleEmojiInsideText() {
        EmojiMessageProcessor processor = new EmojiMessageProcessor();

        List<MessageNode> message = Lists.newArrayList(MessageNode.textNode("lol \uD83D\uDC4C ok"));

        processor.process(message);

        List<MessageNode> expectedResult = ImmutableList.of(
            MessageNode.textNode("lol "),
            MessageNode.emojiNode("\uD83D\uDC4C", "1f44c"),
            MessageNode.textNode(" ok")
        );

        assertEquals(expectedResult, message);
    }
}