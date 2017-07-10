package lexek.wschat.proxy.twitch;

import lombok.Getter;

@Getter
public class CheermoteTier {
    private final String color;
    private final String url;
    private final long minBits;

    public CheermoteTier(String color, String url, long minBits) {
        this.color = color;
        this.url = url;
        this.minBits = minBits;
    }
}
