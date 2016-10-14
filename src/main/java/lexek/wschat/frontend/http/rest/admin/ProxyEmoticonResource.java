package lexek.wschat.frontend.http.rest.admin;

import lexek.wschat.chat.model.GlobalRole;
import lexek.wschat.proxy.ProxyEmoticonService;
import lexek.wschat.security.jersey.RequiredRole;
import org.hibernate.validator.constraints.NotEmpty;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import java.io.IOException;

@Path("/proxy/emoticons")
@RequiredRole(GlobalRole.SUPERADMIN)
public class ProxyEmoticonResource {
    private final ProxyEmoticonService proxyEmoticonService;

    @Inject
    public ProxyEmoticonResource(ProxyEmoticonService proxyEmoticonService) {
        this.proxyEmoticonService = proxyEmoticonService;
    }

    @Path("/{providerName}")
    @POST
    public void loadEmoticons(
        @PathParam("providerName") @NotEmpty String providerName
    ) throws Exception {
        proxyEmoticonService.loadEmoticons(providerName);
    }
}
