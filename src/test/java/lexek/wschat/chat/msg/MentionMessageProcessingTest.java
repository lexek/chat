package lexek.wschat.chat.msg;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class MentionMessageProcessingTest {
    private final MentionMessageProcessor processor = new MentionMessageProcessor();

    @Test
    public void simpleMentionTest() {
        List<MessageNode> message = ImmutableList.of(MessageNode.textNode("@alexey"));

        List<MessageNode> actualResult = processor.process(message);

        List<MessageNode> expectedResult = ImmutableList.of(MessageNode.mentionNode("alexey"));

        assertEquals(actualResult, expectedResult);
    }

    @Test
    public void prefixMentionTest() {
        List<MessageNode> message = ImmutableList.of(MessageNode.textNode("kek @alexey"));

        List<MessageNode> actualResult = processor.process(message);

        List<MessageNode> expectedResult = ImmutableList.of(
            MessageNode.textNode("kek "),
            MessageNode.mentionNode("alexey")
        );

        assertEquals(actualResult, expectedResult);
    }

    @Test
    public void surroundedMentionTest() {
        List<MessageNode> message = ImmutableList.of(MessageNode.textNode("top @alexey kek"));

        List<MessageNode> actualResult = processor.process(message);

        List<MessageNode> expectedResult = ImmutableList.of(
            MessageNode.textNode("top "),
            MessageNode.mentionNode("alexey"),
            MessageNode.textNode(" kek")
        );

        assertEquals(actualResult, expectedResult);
    }

    @Test
    public void multipleMentionTest() {
        List<MessageNode> message = ImmutableList.of(MessageNode.textNode("top @alexey @alesha kek"));

        List<MessageNode> actualResult = processor.process(message);

        List<MessageNode> expectedResult = ImmutableList.of(
            MessageNode.textNode("top "),
            MessageNode.mentionNode("alexey"),
            MessageNode.textNode(" "),
            MessageNode.mentionNode("alesha"),
            MessageNode.textNode(" kek")
        );

        assertEquals(actualResult, expectedResult);
    }

    @Test
    public void multipleNoSpaceMentionTest() {
        List<MessageNode> message = ImmutableList.of(MessageNode.textNode("top @alexey@alesha kek"));

        List<MessageNode> actualResult = processor.process(message);

        List<MessageNode> expectedResult = ImmutableList.of(
            MessageNode.textNode("top "),
            MessageNode.mentionNode("alexey"),
            MessageNode.mentionNode("alesha"),
            MessageNode.textNode(" kek")
        );

        assertEquals(actualResult, expectedResult);
    }

    @Test
    public void multipleNodesTest() {
        List<MessageNode> message = ImmutableList.of(
            MessageNode.textNode("before"),
            MessageNode.urlNode("https://google.com"),
            MessageNode.textNode("top @alexey@alesha kek"),
            MessageNode.textNode("after @kek")
        );

        List<MessageNode> actualResult = processor.process(message);

        List<MessageNode> expectedResult = ImmutableList.of(
            MessageNode.textNode("before"),
            MessageNode.urlNode("https://google.com"),
            MessageNode.textNode("top "),
            MessageNode.mentionNode("alexey"),
            MessageNode.mentionNode("alesha"),
            MessageNode.textNode(" kek"),
            MessageNode.textNode("after "),
            MessageNode.mentionNode("kek")
        );

        assertEquals(actualResult, expectedResult);
    }
}