package lexek.wschat.frontend.http.rest.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.e.InternalErrorException;
import lexek.wschat.chat.model.Chatter;
import lexek.wschat.chat.model.GlobalRole;
import lexek.wschat.chat.model.LocalRole;
import lexek.wschat.chat.model.User;
import lexek.wschat.db.dao.ChatterDao;
import lexek.wschat.db.model.ChatterData;
import lexek.wschat.db.model.DataPage;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.security.jersey.Auth;
import lexek.wschat.security.jersey.RequiredRole;
import lexek.wschat.services.ChatterService;
import lexek.wschat.services.RoomService;
import lexek.wschat.services.UserService;
import lexek.wschat.util.Pages;

import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/rooms/{roomId}/chatters")
@RequiredRole(GlobalRole.ADMIN)
public class ChattersResource {
    private static final int PAGE_LENGTH = 10;
    private final ChatterDao chatterDao;
    private final RoomService roomService;
    private final ChatterService chatterService;
    private final UserService userService;

    public ChattersResource(ChatterDao chatterDao, RoomService roomService, ChatterService chatterService, UserService userService) {
        this.chatterDao = chatterDao;
        this.roomService = roomService;
        this.chatterService = chatterService;
        this.userService = userService;
    }

    @GET
    @RequiredRole(GlobalRole.UNAUTHENTICATED)
    @Path("/publicList")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Map> getChatters(
        @PathParam("roomId") @Size(min = 2, max = 10) String roomName
    ) {
        Room room = roomService.getRoomInstance(roomName);
        return room.getOnlineChatters()
            .stream()
            .filter(chatter -> chatter.hasRole(LocalRole.USER))
            .map(chatter -> ImmutableMap.of(
                "name", chatter.getUser().getName(),
                "timedOut", chatter.getTimeout() != null,
                "banned", chatter.isBanned(),
                "role", chatter.getRole(),
                "globalRole", chatter.getUser().getRole()
            ))
            .collect(Collectors.toList());
    }

    @GET
    @Path("/all")
    @Produces(MediaType.APPLICATION_JSON)
    public DataPage<ChatterData> getChatters(
        @PathParam("roomId") @Min(0) long roomId,
        @QueryParam("page") @Min(0) int page,
        @QueryParam("search") String search,
        @QueryParam("onlyBanned") @DefaultValue("false") boolean onlyBanned
    ) {
        if (onlyBanned) {
            return chatterDao.getBanned(roomId, page, PAGE_LENGTH);
        }
        if (search != null) {
            return chatterDao.searchPaged(roomId, page, PAGE_LENGTH, Pages.escapeSearch(search));
        } else {
            return chatterDao.getAllPaged(roomId, page, PAGE_LENGTH);
        }
    }

    @GET
    @Path("/online")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ChatterData> getOnlineChatters(@Min(0) @PathParam("roomId") long roomId) {
        Room room = roomService.getRoomInstance(roomId);
        return room.getOnlineChatters()
            .stream()
            .filter(chatter -> chatter.hasRole(LocalRole.USER))
            .map(chatter -> new ChatterData(
                chatter.getId(),
                chatter.getUser().getId(),
                chatter.getUser().getName(),
                chatter.getRole(),
                chatter.getUser().getRole(),
                chatter.getTimeout() != null,
                chatter.isBanned()))
            .collect(Collectors.toList());
    }

    @Path("/{name}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ChatterData setBanned(
        JsonNode node,
        @Min(0) @PathParam("roomId") long roomId,
        @Auth UserDto admin,
        @PathParam("name") String name
    ) {
        boolean value = node.get("banned").booleanValue();
        Room room = roomService.getRoomInstance(roomId);

        Chatter adminChatter = room.getOnlineChatter(admin);
        if (adminChatter == null) {
            User chatAdmin = userService.cache(admin);
            adminChatter = chatterService.getChatter(room, chatAdmin);
        }

        Chatter userChatter = room.getChatter(name);
        if (canBan(adminChatter, userChatter)) {
            if (value) {
                if (!chatterService.banChatter(room, userChatter, adminChatter)) {
                    throw new InternalErrorException("Couldn't ban this user.");
                }
            } else {
                if (!chatterService.unbanChatter(room, userChatter, adminChatter)) {
                    throw new InternalErrorException("Couldn't unban this user.");
                }
            }
        } else {
            throw new WebApplicationException("You can't ban this user", 401);
        }
        return new ChatterData(
            userChatter.getId(),
            userChatter.getUser().getId(),
            userChatter.getUser().getName(),
            userChatter.getRole(),
            userChatter.getUser().getRole(),
            userChatter.getTimeout() != null,
            userChatter.isBanned()
        );
    }

    private static boolean canBan(Chatter modChatter, Chatter userChatter) {
        User user = userChatter.getUser();
        User modUser = modChatter.getUser();
        return !userChatter.hasRole(LocalRole.MOD) &&
            (
                modChatter.hasRole(LocalRole.MOD) &&
                    modChatter.hasGreaterRole(userChatter.getRole()) &&
                    modUser.hasGreaterRole(user.getRole())
            );
    }
}
