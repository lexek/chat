package lexek.wschat.proxy;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lexek.wschat.chat.e.InternalErrorException;
import lexek.wschat.chat.msg.EmoticonProvider;
import lexek.wschat.db.dao.ProxyEmoticonDao;
import lexek.wschat.db.model.ProxyEmoticon;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
public class ProxyEmoticonProviderFactory {
    private final LoadingCache<String, List<ProxyEmoticon>> cache;

    @Inject
    public ProxyEmoticonProviderFactory(ProxyEmoticonDao proxyEmoticonDao) {
        cache = CacheBuilder.newBuilder().build(new CacheLoader<String, List<ProxyEmoticon>>() {
            @Override
            public List<ProxyEmoticon> load(String key) throws Exception {
                List<ProxyEmoticon> emoticons = proxyEmoticonDao.getEmoticons(key);
                if (emoticons != null) {
                    emoticons.forEach(ProxyEmoticon::initPattern);
                    return emoticons;
                }
                throw new IllegalArgumentException();
            }
        });
    }

    public EmoticonProvider<ProxyEmoticon> getProvider(String providerName) {
        return () -> {
            try {
                return cache.get(providerName);
            } catch (ExecutionException e) {
                throw new InternalErrorException(e);
            }
        };
    }

    public void flush(String providerName) {
        cache.invalidate(providerName);
    }
}
