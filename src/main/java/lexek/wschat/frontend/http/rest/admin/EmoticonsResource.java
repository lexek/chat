package lexek.wschat.frontend.http.rest.admin;

import com.google.common.hash.Hashing;
import lexek.wschat.chat.GlobalRole;
import lexek.wschat.db.jooq.tables.pojos.Emoticon;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.security.jersey.Auth;
import lexek.wschat.security.jersey.RequiredRole;
import lexek.wschat.services.EmoticonService;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.imageio.ImageIO;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Path("/emoticons")
@RequiredRole(GlobalRole.ADMIN)
public class EmoticonsResource {
    private final java.nio.file.Path emoticonsDir;
    private final EmoticonService emoticonService;
    private String cachedValue = null;

    public EmoticonsResource(File dataDir, EmoticonService emoticonService) {
        this.emoticonService = emoticonService;
        this.emoticonsDir = Paths.get(dataDir.toURI()).resolve("emoticons");
    }

    @Path("/add")
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response add(
        @NotNull @Size(min = 1, max = 50) @FormDataParam("code") String code,
        @NotNull @FormDataParam("file") FormDataContentDisposition fileData,
        @NotNull @FormDataParam("file") File file,
        @Auth UserDto admin
    ) throws URISyntaxException, IOException {
        if (fileData.getFileName().isEmpty()) {
            return Response.status(400).entity("you should provide file").build();
        }
        java.nio.file.Path emoticonFile = createEmoticonFile(fileData.getFileName());
        Files.move(file.toPath(), emoticonFile);
        BufferedImage image = ImageIO.read(emoticonFile.toFile());
        int width = image.getWidth();
        int height = image.getHeight();
        if (width > 200 || height > 200) {
            return Response.status(400).entity("bad request").build();
        } else {
            boolean success = true;
            try {
                emoticonService.add(new Emoticon(null, code, emoticonFile.getFileName().toString(), height, width), admin);
                cachedValue = null;
            } catch (Exception e) {
                success = false;
            }
            if (success) {
                return Response.seeOther(new URI("/admin/emoticons")).build();
            } else {
                return Response.status(400).entity("code already taken").build();
            }
        }
    }

    private java.nio.file.Path createEmoticonFile(String originalName) throws IOException {
        String extension = originalName.substring(originalName.lastIndexOf("."));
        String newName = Hashing.md5()
            .newHasher()
            .putUnencodedChars(originalName)
            .putLong(System.currentTimeMillis())
            .hash() + extension;
        return emoticonsDir.resolve(newName);
    }

    @Path("/{emoticonId}")
    @DELETE
    @Produces(MediaType.TEXT_PLAIN)
    public String handleDelete(@PathParam("emoticonId") long emoticonId,
                               @Auth UserDto admin) {
        emoticonService.delete(emoticonId, admin);
        cachedValue = null;
        return "ok";
    }

    //TODO: better representation
    @Path("/all")
    @RequiredRole(GlobalRole.UNAUTHENTICATED)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getAllEmoticons() {
        if (cachedValue == null) {
            cachedValue = emoticonService.getAllAsJson();
        }
        return cachedValue;
    }
}
