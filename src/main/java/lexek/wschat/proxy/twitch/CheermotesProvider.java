package lexek.wschat.proxy.twitch;

import com.google.common.base.Suppliers;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Service
public class CheermotesProvider {
    private final TwitchApiClient twitchApiClient;
    private final Supplier<List<Cheermote>> cheermotesSupplier;

    @Inject
    public CheermotesProvider(TwitchApiClient twitchApiClient) {
        this.twitchApiClient = twitchApiClient;
        this.cheermotesSupplier =  Suppliers.memoizeWithExpiration(this::fetchCheermotes, 1, TimeUnit.HOURS);
    }

    public List<Cheermote> getCheermotes() {
        return cheermotesSupplier.get();
    }

    private List<Cheermote> fetchCheermotes() {
        try {
            return twitchApiClient.getCheermoteCodes();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
