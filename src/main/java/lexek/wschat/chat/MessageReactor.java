package lexek.wschat.chat;

import lexek.wschat.services.managed.ManagedService;
import org.jvnet.hk2.annotations.Contract;

@Contract
public interface MessageReactor extends ManagedService {
    void processMessage(InboundMessageEvent event);
}
