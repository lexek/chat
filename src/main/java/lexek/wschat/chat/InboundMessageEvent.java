package lexek.wschat.chat;

import lexek.wschat.chat.model.Message;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class InboundMessageEvent {
    private final Connection connection;
    private final Message message;
}
