package lexek.wschat.proxy;

import lexek.wschat.chat.msg.EmoticonProvider;
import lexek.wschat.db.model.ProxyEmoticon;

import java.util.List;

public abstract class AbstractProxyEmoticonProvider implements EmoticonProvider<ProxyEmoticon> {
    private final ProxyEmoticonService proxyEmoticonService;
    private final ProxyProvider provider;

    public AbstractProxyEmoticonProvider(ProxyEmoticonService proxyEmoticonService, ProxyProvider provider) {
        this.proxyEmoticonService = proxyEmoticonService;
        this.provider = provider;
    }

    @Override
    public List<ProxyEmoticon> getEmoticons() {
        return proxyEmoticonService.getEmoticons(provider.getName());
    }

    public abstract List<ProxyEmoticonDescriptor> loadEmoticons();
}
