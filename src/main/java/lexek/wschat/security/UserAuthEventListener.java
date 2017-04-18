package lexek.wschat.security;

import lexek.wschat.chat.model.User;
import org.jvnet.hk2.annotations.Contract;

@Contract
public interface UserAuthEventListener {
    void onEvent(UserAuthEventType type, User user, String service);
}
