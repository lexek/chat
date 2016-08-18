package lexek.wschat.security;

import lexek.wschat.db.model.UserDto;

public interface UserAuthEventListener {
    void onEvent(UserAuthEventType type, UserDto user, String service);
}
