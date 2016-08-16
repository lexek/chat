package lexek.wschat.chat.listeners;

import lexek.wschat.chat.Connection;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.evt.ChatEventType;
import lexek.wschat.chat.evt.EventListener;
import lexek.wschat.chat.model.Chatter;
import lexek.wschat.chat.model.Message;
import lexek.wschat.services.EmoticonService;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;

@Service
public class SendEmoticonsOnEventListener implements EventListener {
    private final EmoticonService emoticonService;

    @Inject
    public SendEmoticonsOnEventListener(EmoticonService emoticonService) {
        this.emoticonService = emoticonService;
    }

    @Override
    public void onEvent(Connection connection, Chatter chatter, Room room) {
        connection.send(Message.emoticonsMessage(emoticonService.getEmoticons()));
    }

    @Override
    public int getOrder() {
        return 1;
    }

    @Override
    public ChatEventType getEventType() {
        return ChatEventType.CONNECT;
    }
}
