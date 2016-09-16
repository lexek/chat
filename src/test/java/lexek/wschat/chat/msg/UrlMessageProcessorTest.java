package lexek.wschat.chat.msg;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class UrlMessageProcessorTest {
    private final UrlMessageProcessor processor = new UrlMessageProcessor();

    @Test
    public void simpleUrlTest() {
        List<MessageNode> message = ImmutableList.of(MessageNode.textNode("https://google.com"));

        List<MessageNode> actualResult = processor.process(message);

        List<MessageNode> expectedResult = ImmutableList.of(MessageNode.urlNode("https://google.com"));

        assertEquals(actualResult, expectedResult);
    }

    @Test
    public void prefixUrlTest() {
        List<MessageNode> message = ImmutableList.of(MessageNode.textNode("kek https://google.com"));

        List<MessageNode> actualResult = processor.process(message);

        List<MessageNode> expectedResult = ImmutableList.of(
            MessageNode.textNode("kek "),
            MessageNode.urlNode("https://google.com")
        );

        assertEquals(actualResult, expectedResult);
    }

    @Test
    public void surroundedUrlTest() {
        List<MessageNode> message = ImmutableList.of(MessageNode.textNode("top https://google.com kek"));

        List<MessageNode> actualResult = processor.process(message);

        List<MessageNode> expectedResult = ImmutableList.of(
            MessageNode.textNode("top "),
            MessageNode.urlNode("https://google.com"),
            MessageNode.textNode(" kek")
        );

        assertEquals(actualResult, expectedResult);
    }

    @Test
    public void multipleUrlTest() {
        List<MessageNode> message = ImmutableList.of(MessageNode.textNode("top https://google.com https://vk.com kek"));

        List<MessageNode> actualResult = processor.process(message);

        List<MessageNode> expectedResult = ImmutableList.of(
            MessageNode.textNode("top "),
            MessageNode.urlNode("https://google.com"),
            MessageNode.textNode(" "),
            MessageNode.urlNode("https://vk.com"),
            MessageNode.textNode(" kek")
        );

        assertEquals(actualResult, expectedResult);
    }

    @Test
    public void multipleNodesTest() {
        List<MessageNode> message = ImmutableList.of(
            MessageNode.textNode("before"),
            MessageNode.mentionNode("lol"),
            MessageNode.textNode("top https://google.com https://vk.com kek"),
            MessageNode.textNode("after https://google.com")
        );

        List<MessageNode> actualResult = processor.process(message);

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

        assertEquals(actualResult, expectedResult);
    }
}
