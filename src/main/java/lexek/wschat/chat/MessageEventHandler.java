package lexek.wschat.chat;

import com.lmax.disruptor.EventHandler;
import org.jvnet.hk2.annotations.Contract;

@Contract
public interface MessageEventHandler extends EventHandler<MessageEvent> {
}
