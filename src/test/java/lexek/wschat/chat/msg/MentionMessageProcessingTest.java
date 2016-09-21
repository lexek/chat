package lexek.wschat.chat.msg;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class MentionMessageProcessingTest {
    private final MentionMessageProcessor processor = new MentionMessageProcessor();

    @Test
    public void simpleMentionTest() {
        List<MessageNode> message = Lists.newArrayList(MessageNode.textNode("@alexey"));

        processor.process(message);

        List<MessageNode> expectedResult = ImmutableList.of(MessageNode.mentionNode("alexey"));

        assertEquals(message, expectedResult);
    }

    @Test
    public void prefixMentionTest() {
        List<MessageNode> message = Lists.newArrayList(MessageNode.textNode("kek @alexey"));

        processor.process(message);

        List<MessageNode> expectedResult = ImmutableList.of(
            MessageNode.textNode("kek "),
            MessageNode.mentionNode("alexey")
        );

        assertEquals(message, expectedResult);
    }

    @Test
    public void surroundedMentionTest() {
        List<MessageNode> message = Lists.newArrayList(MessageNode.textNode("top @alexey kek"));

        processor.process(message);

        List<MessageNode> expectedResult = ImmutableList.of(
            MessageNode.textNode("top "),
            MessageNode.mentionNode("alexey"),
            MessageNode.textNode(" kek")
        );

        assertEquals(message, expectedResult);
    }

    @Test
    public void multipleMentionTest() {
        List<MessageNode> message = Lists.newArrayList(MessageNode.textNode("top @alexey @alesha kek"));

        processor.process(message);

        List<MessageNode> expectedResult = ImmutableList.of(
            MessageNode.textNode("top "),
            MessageNode.mentionNode("alexey"),
            MessageNode.textNode(" "),
            MessageNode.mentionNode("alesha"),
            MessageNode.textNode(" kek")
        );

        assertEquals(message, expectedResult);
    }

    @Test
    public void multipleNoSpaceMentionTest() {
        List<MessageNode> message = Lists.newArrayList(MessageNode.textNode("top @alexey@alesha kek"));

        processor.process(message);

        List<MessageNode> expectedResult = ImmutableList.of(
            MessageNode.textNode("top "),
            MessageNode.mentionNode("alexey"),
            MessageNode.mentionNode("alesha"),
            MessageNode.textNode(" kek")
        );

        assertEquals(message, expectedResult);
    }

    @Test
    public void multipleNodesTest() {
        List<MessageNode> message = Lists.newArrayList(
            MessageNode.textNode("before"),
            MessageNode.urlNode("https://google.com"),
            MessageNode.textNode("top @alexey@alesha kek"),
            MessageNode.textNode("after @kek")
        );

        processor.process(message);

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

        assertEquals(message, expectedResult);
    }
}