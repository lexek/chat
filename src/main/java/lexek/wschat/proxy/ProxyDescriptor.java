package lexek.wschat.proxy;

import lexek.wschat.chat.Room;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.EnumSet;
import java.util.Optional;

@Getter
@AllArgsConstructor
public class ProxyDescriptor {
    private final long id;
    private final ProxyProvider provider;
    private final Room room;
    private final String remoteRoom;
    private final Optional<Long> authId;
    private final EnumSet<ProxyFeature> features;

    public boolean hasFeature(ProxyFeature feature) {
        return features.contains(feature);
    }

    public Long authId() {
        return authId.orElseThrow(() -> new IllegalStateException("auth id is not present"));
    }
}
