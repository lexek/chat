package lexek.httpserver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileSystemStaticHandler extends AbstractStaticHandler {
    private final File dataPath;

    public FileSystemStaticHandler(File dataPath) {
        this.dataPath = dataPath;
    }

    @Override
    protected StaticHandlerContext getContext(String uri) {
        return new FileSystemContext(uri);
    }

    private class FileSystemContext implements StaticHandlerContext {
        private File file;
        private Path path;

        private FileSystemContext(String path) {
            try {
                this.file = new File(dataPath, path).getCanonicalFile();
                this.path = Paths.get(this.file.toURI());
            } catch (IOException e) {
                this.file = null;
                this.path = null;
            }
        }

        @Override
        public boolean exists() {
            if (file == null) {
                return false;
            }
            try {
                String fullPath = file.getPath();
                String dataCanonicalPath = dataPath.getCanonicalPath();
                return file.exists()
                    && file.isFile()
                    && fullPath.startsWith(dataCanonicalPath + File.separator);
            } catch (IOException e) {
                return false;
            }
        }

        @Override
        public String getContentType() {
            try {
                return Files.probeContentType(path);
            } catch (IOException e) {
                return null;
            }
        }

        @Override
        public long lastModified() {
            try {
                return Files.getLastModifiedTime(path).toMillis();
            } catch (IOException e) {
                return 0;
            }
        }

        @Override
        public byte[] read() throws IOException {
            return Files.readAllBytes(path);
        }
    }
}
