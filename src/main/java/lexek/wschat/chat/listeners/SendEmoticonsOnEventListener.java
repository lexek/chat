package lexek.wschat.chat.listeners;

import lexek.wschat.chat.Chatter;
import lexek.wschat.chat.Connection;
import lexek.wschat.chat.Message;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.evt.EventListener;
import lexek.wschat.services.EmoticonService;

public class SendEmoticonsOnEventListener implements EventListener {
    private final EmoticonService emoticonService;

    public SendEmoticonsOnEventListener(EmoticonService emoticonService) {
        this.emoticonService = emoticonService;
    }

    @Override
    public void onEvent(Connection connection, Chatter chatter, Room room) {
        connection.send(Message.emoticonsMessage(emoticonService.getEmoticons()));
    }
}
