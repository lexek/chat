package lexek.wschat.chat;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

public class TextMessageMatcher extends BaseMatcher<Message> {
    private final Message message;

    private TextMessageMatcher(Message message) {
        this.message = message;
    }

    @Override
    public boolean matches(Object item) {
        if (item instanceof Message) {
            Message msg = (Message) item;
            return msg.get(Message.Keys.TYPE).equals(message.getType()) &&
                msg.get(Message.Keys.ROOM).equals(message.get(Message.Keys.ROOM)) &&
                msg.get(Message.Keys.NAME).equals(message.get(Message.Keys.NAME)) &&
                msg.get(Message.Keys.GLOBAL_ROLE).equals(message.get(Message.Keys.GLOBAL_ROLE)) &&
                msg.get(Message.Keys.LOCAL_ROLE).equals(message.get(Message.Keys.LOCAL_ROLE)) &&
                msg.get(Message.Keys.COLOR).equals(message.get(Message.Keys.COLOR)) &&
                msg.get(Message.Keys.MESSAGE_ID).equals(message.get(Message.Keys.MESSAGE_ID)) &&
                msg.get(Message.Keys.TIME) != null &&
                msg.get(Message.Keys.TEXT).equals(message.get(Message.Keys.TEXT));
        }
        return false;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(message.toString());
    }

    public static TextMessageMatcher textMessage(Message message) {
        return new TextMessageMatcher(message);
    }
}
