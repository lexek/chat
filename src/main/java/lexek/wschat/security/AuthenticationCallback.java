package lexek.wschat.security;

import lexek.wschat.chat.Connection;

public interface AuthenticationCallback<T extends Connection> {
    void authenticationComplete(T connection);

    void captchaRequired(T connection, String name, long captchaId);
}
