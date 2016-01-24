package lexek.wschat.proxy.twitch;

public interface JTVEventListener {
    void onDisconnected();

    void onMessage(TwitchUser user, String message);

    void onClear(String name);

    void loginFailed();

    void selfJoined(String room);

    void exceptionCaught(Throwable throwable);
}
