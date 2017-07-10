package lexek.wschat.proxy.twitch;

import lombok.Getter;

import java.util.List;
import java.util.regex.Pattern;

@Getter
public class Cheermote {
    private final String prefix;
    private final Pattern pattern;
    private final List<CheermoteTier> tiers;

    public Cheermote(String prefix, List<CheermoteTier> tiers) {
        this.prefix = prefix;
        this.pattern = Pattern.compile("(?:^|\\s)(" + prefix + ")(\\d+)(?:\\s|$)", Pattern.CASE_INSENSITIVE);
        this.tiers = tiers;
    }
}
