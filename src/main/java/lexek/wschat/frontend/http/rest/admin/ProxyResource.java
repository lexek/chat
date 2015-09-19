package lexek.wschat.frontend.http.rest.admin;

import lexek.wschat.chat.GlobalRole;
import lexek.wschat.db.jooq.tables.pojos.ChatProxy;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.db.model.rest.ProxyProviderRestModel;
import lexek.wschat.db.model.rest.ProxyRestModel;
import lexek.wschat.proxy.Proxy;
import lexek.wschat.proxy.ProxyManager;
import lexek.wschat.security.jersey.Auth;
import lexek.wschat.security.jersey.RequiredRole;
import lexek.wschat.services.RoomService;
import org.hibernate.validator.constraints.NotEmpty;

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

    public ProxyResource(RoomService roomService, ProxyManager proxyManager) {
        this.roomService = roomService;
        this.proxyManager = proxyManager;
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
                proxy.outboundEnabled()
            ))
            .collect(Collectors.toList());
    }

    @Path("/providers")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<ProxyProviderRestModel> providers() {
        return proxyManager
            .getProviders()
            .stream()
            .map(provider -> new ProxyProviderRestModel(
                provider.getName(),
                provider.isSupportsAuthentication(),
                provider.isSupportsOutbound()
            ))
            .collect(Collectors.toList());
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public ProxyRestModel create(
        @PathParam("roomId") long roomId,
        @Auth UserDto admin,
        ChatProxy chatProxy
    ) {
        Proxy result = proxyManager.newProxy(
            admin,
            roomService.getRoomInstance(roomId),
            chatProxy.getProviderName(),
            chatProxy.getRemoteRoom().toLowerCase(),
            chatProxy.getAuthName(),
            chatProxy.getAuthKey(),
            chatProxy.getEnableOutbound()
        );
        return new ProxyRestModel(
            result.id(),
            result.provider().getName(),
            result.remoteRoom(),
            result.lastError(),
            result.state(),
            result.outboundEnabled()
        );
    }

    @Path("/{providerName}/{remoteRoom}")
    @DELETE
    public void delete(
        @PathParam("roomId") @Min(0) long roomId,
        @PathParam("providerName") @NotEmpty String providerName,
        @PathParam("remoteRoom") @NotEmpty String remoteRoom,
        @Auth UserDto admin
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
