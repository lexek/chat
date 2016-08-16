package lexek.wschat.frontend.http.rest.admin;

import io.netty.util.NetUtil;
import lexek.wschat.chat.model.GlobalRole;
import lexek.wschat.security.AuthenticationManager;
import lexek.wschat.security.jersey.RequiredRole;

import javax.inject.Inject;
import javax.validation.ValidationException;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Collection;

@Path("/security/ip-block")
@RequiredRole(GlobalRole.SUPERADMIN)
public class IpBlockResource {
    private final AuthenticationManager authenticationManager;

    @Inject
    public IpBlockResource(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<String> getBlockedList() {
        return authenticationManager.getBannedIps();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<String> blockIp(@QueryParam("ip") @NotNull String ip) {
        if (!NetUtil.isValidIpV4Address(ip)) {
            throw new ValidationException("Invalid IPv4 address");
        }
        authenticationManager.getBannedIps().add(ip);
        return authenticationManager.getBannedIps();
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<String> unblockIp(@QueryParam("ip") @NotNull String ip) {
        if (!NetUtil.isValidIpV4Address(ip)) {
            throw new ValidationException("Invalid IPv4 address");
        }
        authenticationManager.getBannedIps().remove(ip);
        return authenticationManager.getBannedIps();
    }
}
