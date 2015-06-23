package lexek.wschat.frontend.http.admin;

import com.google.common.hash.Hashing;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.multipart.FileUpload;
import lexek.httpserver.Request;
import lexek.httpserver.Response;
import lexek.httpserver.SimpleHttpHandler;
import lexek.wschat.chat.GlobalRole;
import lexek.wschat.db.jooq.tables.pojos.Emoticon;
import lexek.wschat.db.model.UserAuthDto;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.security.AuthenticationManager;
import lexek.wschat.services.EmoticonService;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class EmoticonHandler extends SimpleHttpHandler {
    private final Path emoticonsDir;
    private final EmoticonService emoticonService;
    private final AuthenticationManager authenticationManager;
    private String cachedValue = null;

    public EmoticonHandler(File dataDir, EmoticonService emoticonService, AuthenticationManager authenticationManager) {
        this.emoticonService = emoticonService;
        this.authenticationManager = authenticationManager;
        this.emoticonsDir = Paths.get(dataDir.toURI()).resolve("emoticons");
    }

    private void handleDelete(Response response, UserDto admin, long id) {
        emoticonService.delete(id, admin);
        cachedValue = null;
        response.stringContent("ok");
    }

    private void handleUpload(Request request, Response response, UserDto admin) throws IOException {
        FileUpload tmpFile = request.postParamFile("file");
        String code = request.postParam("code");
        if (tmpFile != null && !tmpFile.getFilename().isEmpty() && code != null && !code.isEmpty()) {
            Path emoticonFile = createEmoticonFile(tmpFile.getFilename());
            FileChannel fileChannel = FileChannel.open(emoticonFile, StandardOpenOption.WRITE);
            fileChannel.write(tmpFile.getByteBuf().nioBuffer());
            fileChannel.close();
            BufferedImage image = ImageIO.read(emoticonFile.toFile());
            int width = image.getWidth();
            int height = image.getHeight();
            if (width > 200 || height > 200) {
                response.badRequest();
            } else {
                emoticonService.add(new Emoticon(null, code, emoticonFile.getFileName().toString(), height, width), admin);
                cachedValue = null;
                response.redirect("/admin/emoticons");
            }
        } else {
            response.badRequest();
        }
    }

    private Path createEmoticonFile(String originalName) throws IOException {
        String extension = originalName.substring(originalName.lastIndexOf("."));
        String newName = Hashing.md5()
            .newHasher()
            .putUnencodedChars(originalName)
            .putLong(System.currentTimeMillis())
            .hash() + extension;
        Path file = emoticonsDir.resolve(newName);
        return Files.createFile(file);
    }

    @Override
    protected void handle(Request request, Response response) throws Exception {
        if (request.method() == HttpMethod.POST) {
            UserAuthDto userAuthDto = authenticationManager.checkFullAuthentication(request);
            if (userAuthDto != null && userAuthDto.getUser() != null && userAuthDto.getUser().hasRole(GlobalRole.ADMIN)) {
                String deleteParam = request.queryParam("delete");
                UserDto admin = userAuthDto.getUser();
                if (deleteParam != null) {
                    handleDelete(response, admin, Long.valueOf(deleteParam));
                } else {
                    handleUpload(request, response, admin);
                }
            }
        } else if (request.method() == HttpMethod.GET) {
            if (cachedValue == null) {
                cachedValue = emoticonService.getAllAsJson();
            }
            response.stringContent(cachedValue, "application/json; charset=utf-8");
        } else {
            response.badRequest();
        }
    }
}
