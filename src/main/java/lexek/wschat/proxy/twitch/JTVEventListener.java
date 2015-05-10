package lexek.wschat.proxy.twitch;

public interface JTVEventListener {
    void onConnected();

    void onDisconnected();

    void onMessage(TwitchUser user, String message);

    void onServerMessage(String message);

    void onClear(String name);
}
