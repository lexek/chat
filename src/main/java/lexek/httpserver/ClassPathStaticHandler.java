package lexek.httpserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;

public class ClassPathStaticHandler extends AbstractStaticHandler {
    private final Logger logger = LoggerFactory.getLogger(ClassPathStaticHandler.class);
    private final Class clazz;
    private final File root;

    public ClassPathStaticHandler(Class clazz, String root) {
        this.clazz = clazz;
        this.root = new File(root);
    }

    @Override
    protected StaticHandlerContext getContext(String uri) {
        return new ClassPathContext(uri);
    }

    private class ClassPathContext implements StaticHandlerContext {
        private String jarFileName = null;
        private InputStream resource;
        private File file;

        private ClassPathContext(String uri) {
            this.file = new File(root, uri);
        }

        @Override
        public boolean exists() {
            this.resource = getResource();
            return resource != null;
        }

        @Override
        public String getContentType() {
            return guessMimeType(path());
        }

        @Override
        public long lastModified() {
            URL url = clazz.getClassLoader().getResource(path());
            String fileName;
            if (url.getProtocol().equals("file")) {
                fileName = url.getFile();
            } else if (url.getProtocol().equals("jar")) {
                if (jarFileName == null) {
                    try {
                        JarURLConnection jarUrl = (JarURLConnection) url.openConnection();
                        jarFileName = jarUrl.getJarFile().getName();
                    } catch (IOException e) {
                        logger.warn("exception while getting last modified value", e);
                    }
                }
                fileName = jarFileName;
            } else {
                throw new IllegalArgumentException("Not a file");
            }
            File f = new File(fileName);
            return f.lastModified();
        }

        @Override
        public byte[] read() throws IOException {
            byte[] data;
            int length = resource.available();
            data = new byte[length];
            try {
                int read = 0;
                while (read < length) {
                    int more = resource.read(data, read, data.length - read);
                    if (more == -1) {
                        break;
                    } else {
                        read += more;
                    }
                }
            } finally {
                resource.close();
            }
            return data;
        }

        private String path() {
            String resourcePath = file.getPath();
            if ('/' != File.separatorChar) {
                resourcePath = resourcePath.replace(File.separatorChar, '/');
            }
            if (resourcePath.startsWith("/")) {
                resourcePath = resourcePath.substring(1);
            }
            return resourcePath;
        }

        private InputStream getResource() {
            return clazz.getClassLoader().getResourceAsStream(path());
        }
    }
}
