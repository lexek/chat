package lexek.wschat.proxy.twitch;

import com.google.common.base.Suppliers;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Service
public class CheermotesProvider {
    private final TwitchApiClient twitchApiClient;
    private final Supplier<Set<String>> cheermotesSupplier;

    @Inject
    public CheermotesProvider(TwitchApiClient twitchApiClient) {
        this.twitchApiClient = twitchApiClient;
        this.cheermotesSupplier =  Suppliers.memoizeWithExpiration(this::fetchCheermotes, 1, TimeUnit.HOURS);
    }

    public Set<String> getCheermotes() {
        return cheermotesSupplier.get();
    }

    private Set<String> fetchCheermotes() {
        try {
            return twitchApiClient.getCheermoteCodes();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
