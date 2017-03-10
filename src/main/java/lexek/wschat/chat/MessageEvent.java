package lexek.wschat.chat;

import lexek.wschat.chat.filters.BroadcastFilter;
import lexek.wschat.chat.model.Message;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class MessageEvent {
    private final Message message;
    private final BroadcastFilter broadcastFilter;
}
