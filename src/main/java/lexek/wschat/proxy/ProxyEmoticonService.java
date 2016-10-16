package lexek.wschat.proxy;

import lexek.wschat.chat.e.InvalidInputException;
import lexek.wschat.db.dao.ProxyEmoticonDao;
import lexek.wschat.db.model.ProxyEmoticon;
import org.apache.batik.transcoder.SVGAbstractTranscoder;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;

@Service
public class ProxyEmoticonService {
    private final Logger logger = LoggerFactory.getLogger(ProxyEmoticonService.class);
    private final ProxyManager proxyManager;
    private final ProxyEmoticonDao proxyEmoticonDao;
    private final ProxyEmoticonProviderFactory proxyEmoticonProviderFactory;
    private final File dataDir;
    private final HttpClient httpClient;

    @Inject
    public ProxyEmoticonService(
        ProxyManager proxyManager,
        ProxyEmoticonDao proxyEmoticonDao,
        ProxyEmoticonProviderFactory proxyEmoticonProviderFactory, @Named("core.dataDirectory") File dataDir,
        HttpClient httpClient
    ) {
        this.proxyManager = proxyManager;
        this.proxyEmoticonDao = proxyEmoticonDao;
        this.proxyEmoticonProviderFactory = proxyEmoticonProviderFactory;
        this.dataDir = dataDir;
        this.httpClient = httpClient;
    }

    public void loadEmoticons(String providerName) throws Exception {
        proxyEmoticonDao.deleteAll(providerName);

        ProxyProvider provider = proxyManager.getProvider(providerName);
        if (!provider.isSupportsEmoticons()) {
            throw new InvalidInputException("provider", "provider doesn't support emoticon loading");
        }
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
                            if (file.getParentFile().mkdirs()) {
                                logger.info("created missing directories for {}", file);
                            }
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

                if (fileName.endsWith(".svg")) {
                    int width = (Integer) emoticon.getExtra().get("width");
                    int height = (Integer) emoticon.getExtra().get("height");
                    PNGTranscoder transcoder = new PNGTranscoder();
                    transcoder.addTranscodingHint(SVGAbstractTranscoder.KEY_WIDTH, (float) width);
                    transcoder.addTranscodingHint(SVGAbstractTranscoder.KEY_HEIGHT, (float) height);
                    transcoder.addTranscodingHint(SVGAbstractTranscoder.KEY_ALLOWED_SCRIPT_TYPES, "");
                    transcoder.addTranscodingHint(SVGAbstractTranscoder.KEY_CONSTRAIN_SCRIPT_ORIGIN, false);

                    String newFileName = fileName.substring(0, fileName.length() - 4) + ".png";
                    File newFile = new File(basePath, newFileName);
                    transcoder.transcode(
                        new TranscoderInput(Files.newInputStream(file.toPath())),
                        new TranscoderOutput(Files.newOutputStream(newFile.toPath(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE))
                    );

                    fileName = newFileName;
                    file = newFile;
                }

                if (file.exists()) {
                    proxyEmoticonDao.saveEmoticon(
                        providerName,
                        new ProxyEmoticon(null, emoticon.getCode(), fileName, emoticon.getExtra())
                    );
                }
            } catch (Exception e) {
                logger.error("unable to load emoticon", e);
            }
        }
        proxyEmoticonProviderFactory.flush(providerName);
    }
}
