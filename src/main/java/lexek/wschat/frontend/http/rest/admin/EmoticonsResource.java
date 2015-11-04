package lexek.wschat.frontend.http.rest.admin;

import lexek.wschat.chat.GlobalRole;
import lexek.wschat.chat.e.InvalidInputException;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.security.jersey.Auth;
import lexek.wschat.security.jersey.RequiredRole;
import lexek.wschat.services.EmoticonService;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

@Path("/emoticons")
@RequiredRole(GlobalRole.ADMIN)
public class EmoticonsResource {
    private final EmoticonService emoticonService;
    private String cachedValue = null;

    public EmoticonsResource(EmoticonService emoticonService) {
        this.emoticonService = emoticonService;
    }

    @Path("/add")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response add(
        @NotNull @Size(min = 1, max = 50) @FormDataParam("code") String code,
        @NotNull @FormDataParam("file") FormDataContentDisposition fileData,
        @NotNull @FormDataParam("file") File file,
        @Auth UserDto admin
    ) throws URISyntaxException, IOException {
        if (fileData.getFileName().isEmpty()) {
            throw new InvalidInputException("file", "You should provide file");
        }
        if (emoticonService.add(code, file, fileData.getFileName(), admin)) {
            cachedValue = null;
            return Response.seeOther(new URI("/admin/emoticons")).build();
        } else {
            return Response.status(400).entity("code already taken").build();
        }
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
