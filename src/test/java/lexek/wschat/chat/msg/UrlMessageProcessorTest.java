package lexek.wschat.chat.msg;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class UrlMessageProcessorTest {
    private final UrlMessageProcessor processor = new UrlMessageProcessor();

    @Test
    public void simpleUrlTest() {
        List<MessageNode> message = Lists.newArrayList(MessageNode.textNode("https://google.com"));

        processor.process(message);

        List<MessageNode> expectedResult = ImmutableList.of(MessageNode.urlNode("https://google.com"));

        assertEquals(message, expectedResult);
    }

    @Test
    public void prefixUrlTest() {
        List<MessageNode> message = Lists.newArrayList(MessageNode.textNode("kek https://google.com"));

        processor.process(message);

        List<MessageNode> expectedResult = ImmutableList.of(
            MessageNode.textNode("kek "),
            MessageNode.urlNode("https://google.com")
        );

        assertEquals(message, expectedResult);
    }

    @Test
    public void surroundedUrlTest() {
        List<MessageNode> message = Lists.newArrayList(MessageNode.textNode("top https://google.com kek"));

        processor.process(message);

        List<MessageNode> expectedResult = ImmutableList.of(
            MessageNode.textNode("top "),
            MessageNode.urlNode("https://google.com"),
            MessageNode.textNode(" kek")
        );

        assertEquals(message, expectedResult);
    }

    @Test
    public void multipleUrlTest() {
        List<MessageNode> message = Lists.newArrayList(MessageNode.textNode("top https://google.com https://vk.com kek"));

        processor.process(message);

        List<MessageNode> expectedResult = ImmutableList.of(
            MessageNode.textNode("top "),
            MessageNode.urlNode("https://google.com"),
            MessageNode.textNode(" "),
            MessageNode.urlNode("https://vk.com"),
            MessageNode.textNode(" kek")
        );

        assertEquals(message, expectedResult);
    }

    @Test
    public void multipleNodesTest() {
        List<MessageNode> message = Lists.newArrayList(
            MessageNode.textNode("before"),
            MessageNode.mentionNode("lol"),
            MessageNode.textNode("top https://google.com https://vk.com kek"),
            MessageNode.textNode("after https://google.com")
        );

        processor.process(message);

        List<MessageNode> expectedResult = ImmutableList.of(
            MessageNode.textNode("before"),
            MessageNode.mentionNode("lol"),
            MessageNode.textNode("top "),
            MessageNode.urlNode("https://google.com"),
            MessageNode.textNode(" "),
            MessageNode.urlNode("https://vk.com"),
            MessageNode.textNode(" kek"),
            MessageNode.textNode("after "),
            MessageNode.urlNode("https://google.com")
        );

        assertEquals(message, expectedResult);
    }
}
