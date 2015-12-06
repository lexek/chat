package lexek.wschat.chat;

import lexek.wschat.chat.model.Message;
import lexek.wschat.chat.model.MessageProperty;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

public class TextMessageMatcher extends BaseMatcher<Message> {
    private final Message message;

    private TextMessageMatcher(Message message) {
        this.message = message;
    }

    public static TextMessageMatcher textMessage(Message message) {
        return new TextMessageMatcher(message);
    }

    @Override
    public boolean matches(Object item) {
        if (item instanceof Message) {
            Message msg = (Message) item;
            return msg.get(MessageProperty.TYPE).equals(message.getType()) &&
                msg.get(MessageProperty.ROOM).equals(message.get(MessageProperty.ROOM)) &&
                msg.get(MessageProperty.NAME).equals(message.get(MessageProperty.NAME)) &&
                msg.get(MessageProperty.GLOBAL_ROLE).equals(message.get(MessageProperty.GLOBAL_ROLE)) &&
                msg.get(MessageProperty.LOCAL_ROLE).equals(message.get(MessageProperty.LOCAL_ROLE)) &&
                msg.get(MessageProperty.COLOR).equals(message.get(MessageProperty.COLOR)) &&
                msg.get(MessageProperty.MESSAGE_ID).equals(message.get(MessageProperty.MESSAGE_ID)) &&
                msg.get(MessageProperty.TIME) != null &&
                msg.get(MessageProperty.TEXT).equals(message.get(MessageProperty.TEXT));
        }
        return false;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(message.toString());
    }
}
