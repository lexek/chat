package lexek.wschat.security;

import lexek.wschat.db.model.UserDto;
import org.jvnet.hk2.annotations.Contract;

@Contract
public interface UserAuthEventListener {
    void onEvent(UserAuthEventType type, UserDto user, String service);
}
