package lexek.wschat.frontend.http.rest.admin;

import io.netty.util.NetUtil;
import lexek.wschat.chat.GlobalRole;
import lexek.wschat.security.jersey.RequiredRole;

import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.Set;

@Path("/security/ip-block")
@RequiredRole(GlobalRole.SUPERADMIN)
public class IpBlockResource {
    private final Set<String> blockedIps;

    public IpBlockResource(Set<String> blockedIps) {
        this.blockedIps = blockedIps;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<String> getBlockedList() {
        return blockedIps;
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<String> blockIp(@QueryParam("ip") @NotNull String ip) {
        if (!NetUtil.isValidIpV4Address(ip)) {
            throw new WebApplicationException(400);
        }
        blockedIps.add(ip);
        return blockedIps;
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<String> unblockIp(@QueryParam("ip") @NotNull String ip) {
        if (!NetUtil.isValidIpV4Address(ip)) {
            throw new WebApplicationException(400);
        }
        blockedIps.remove(ip);
        return blockedIps;
    }
}
