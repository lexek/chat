package lexek.wschat.frontend.http.rest.admin;

import lexek.wschat.chat.e.EntityNotFoundException;
import lexek.wschat.chat.model.GlobalRole;
import lexek.wschat.chat.model.User;
import lexek.wschat.db.jooq.tables.pojos.ChatProxy;
import lexek.wschat.db.model.ProxyAuth;
import lexek.wschat.db.model.rest.ProxyProviderRestModel;
import lexek.wschat.db.model.rest.ProxyRestModel;
import lexek.wschat.proxy.Proxy;
import lexek.wschat.proxy.ProxyAuthService;
import lexek.wschat.proxy.ProxyManager;
import lexek.wschat.security.jersey.Auth;
import lexek.wschat.security.jersey.RequiredRole;
import lexek.wschat.services.RoomService;
import org.hibernate.validator.constraints.NotEmpty;

import javax.inject.Inject;
import javax.validation.constraints.Min;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.stream.Collectors;

@Path("/rooms/{roomId}/proxies")
@RequiredRole(GlobalRole.ADMIN)
public class ProxyResource {
    private final RoomService roomService;
    private final ProxyManager proxyManager;
    private final ProxyAuthService proxyAuthService;

    @Inject
    public ProxyResource(RoomService roomService, ProxyManager proxyManager, ProxyAuthService proxyAuthService) {
        this.roomService = roomService;
        this.proxyManager = proxyManager;
        this.proxyAuthService = proxyAuthService;
    }

    @Path("/list")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<ProxyRestModel> providers(@PathParam("roomId") long roomId) {
        return proxyManager
            .getProxiesByRoom(roomService.getRoomInstance(roomId))
            .stream()
            .map(proxy -> new ProxyRestModel(
                proxy.id(),
                proxy.provider().getName(),
                proxy.remoteRoom(),
                proxy.lastError(),
                proxy.state(),
                proxy.outboundEnabled(),
                proxy.moderationEnabled()))
            .collect(Collectors.toList());
    }

    @Path("/providers")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<ProxyProviderRestModel> providers(@Auth User currentUser) {
        return proxyManager
            .getProviders()
            .stream()
            .map(provider -> new ProxyProviderRestModel(
                provider.getName(),
                provider.isSupportsAuth(),
                provider.requiresAuth(),
                provider.isSupportsOutbound(),
                proxyAuthService.getAvailableCredentials(currentUser, provider.getSupportedAuthServices())
            ))
            .collect(Collectors.toList());
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public ProxyRestModel create(
        @PathParam("roomId") long roomId,
        @Auth User admin,
        ChatProxy chatProxy
    ) {
        ProxyAuth proxyAuth = null;
        if (chatProxy.getAuthId() != null) {
            proxyAuth = proxyAuthService.getAuth(chatProxy.getAuthId());
            if (proxyAuth == null) {
                throw new EntityNotFoundException("proxyAuth");
            }
        }
        Proxy result = proxyManager.newProxy(
            admin,
            roomService.getRoomInstance(roomId),
            chatProxy.getProviderName().trim(),
            chatProxy.getRemoteRoom().toLowerCase().trim(),
            proxyAuth,
            chatProxy.getEnableOutbound()
        );
        return new ProxyRestModel(
            result.id(),
            result.provider().getName(),
            result.remoteRoom(),
            result.lastError(),
            result.state(),
            result.outboundEnabled(),
            result.moderationEnabled());
    }

    @Path("/{providerName}/{remoteRoom}")
    @DELETE
    public void delete(
        @PathParam("roomId") @Min(0) long roomId,
        @PathParam("providerName") @NotEmpty String providerName,
        @PathParam("remoteRoom") @NotEmpty String remoteRoom,
        @Auth User admin
    ) {
        proxyManager.remove(admin, roomService.getRoomInstance(roomId), providerName, remoteRoom);
    }

    @Path("/{providerName}/{remoteRoom}/start")
    @POST
    public void start(
        @PathParam("roomId") @Min(0) long roomId,
        @PathParam("providerName") @NotEmpty String providerName,
        @PathParam("remoteRoom") @NotEmpty String remoteRoom
    ) {
        proxyManager.startProxy(roomService.getRoomInstance(roomId), providerName, remoteRoom);
    }

    @Path("/{providerName}/{remoteRoom}/stop")
    @POST
    public void stop(
        @PathParam("roomId") @Min(0) long roomId,
        @PathParam("providerName") @NotEmpty String providerName,
        @PathParam("remoteRoom") @NotEmpty String remoteRoom
    ) {
        proxyManager.stopProxy(roomService.getRoomInstance(roomId), providerName, remoteRoom);
    }
}
