package lexek.wschat.proxy;

import lexek.wschat.db.dao.ProxyEmoticonDao;
import lexek.wschat.db.model.ProxyEmoticon;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Named;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

@Service
public class ProxyEmoticonService {
    private final Logger logger = LoggerFactory.getLogger(ProxyEmoticonService.class);
    private final ProxyManager proxyManager;
    private final ProxyEmoticonDao proxyEmoticonDao;
    private final File dataDir;
    private final HttpClient httpClient;

    @Inject
    public ProxyEmoticonService(
        ProxyManager proxyManager,
        ProxyEmoticonDao proxyEmoticonDao,
        @Named("core.dataDirectory") File dataDir,
        HttpClient httpClient
    ) {
        this.proxyManager = proxyManager;
        this.proxyEmoticonDao = proxyEmoticonDao;
        this.dataDir = dataDir;
        this.httpClient = httpClient;
    }

    public List<ProxyEmoticon> getEmoticons(String service) {
        return proxyEmoticonDao.getEmoticons(service);
    }

    public void loadEmoticons(String providerName) throws IOException {
        ProxyProvider provider = proxyManager.getProvider(providerName);
        List<ProxyEmoticonDescriptor> proxyEmoticons = provider.fetchEmoticonDescriptors();

        File basePath = new File(
            dataDir,
            "/emoticons/" + providerName
        );

        if (!basePath.exists()) {
            Files.createDirectory(basePath.toPath());
        }

        //todo: progress indication
        for (ProxyEmoticonDescriptor emoticon : proxyEmoticons) {
            String fileName = emoticon.getFileName();
            logger.trace("saving emoticon {} to {}", emoticon.getCode(), fileName);
            File file = new File(basePath, fileName);

            try {
                if (!file.exists()) {
                    HttpGet httpGet = new HttpGet(emoticon.getUrl());
                    HttpResponse response = httpClient.execute(httpGet);
                    try {
                        int statusCode = response.getStatusLine().getStatusCode();
                        if (statusCode == 200) {
                            Files.copy(response.getEntity().getContent(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        } else {
                            logger.warn(
                                "Unable to load emoticon {}: {} ({})\r\n{}",
                                emoticon.getCode(), emoticon.getUrl(), statusCode, EntityUtils.toString(response.getEntity())
                            );
                        }
                    } finally {
                        EntityUtils.consume(response.getEntity());
                    }
                }

                BufferedImage image = ImageIO.read(file);
                int width = image.getWidth();
                int height = image.getHeight();
                proxyEmoticonDao.saveEmoticon(
                    providerName,
                    new ProxyEmoticon(null, emoticon.getCode(), fileName, height, width)
                );
            } catch (Exception e) {
                logger.error("unable to load emoticon", e);
            }
        }
    }
}
