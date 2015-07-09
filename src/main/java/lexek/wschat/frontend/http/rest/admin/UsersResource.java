package lexek.wschat.frontend.http.rest.admin;

import lexek.wschat.chat.ConnectionManager;
import lexek.wschat.chat.GlobalRole;
import lexek.wschat.db.model.DataPage;
import lexek.wschat.db.model.OnlineUser;
import lexek.wschat.db.model.UserData;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.db.model.form.UserChangeSet;
import lexek.wschat.security.jersey.Auth;
import lexek.wschat.security.jersey.RequiredRole;
import lexek.wschat.services.UserService;

import javax.validation.constraints.Min;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

@Path("/users")
@RequiredRole(GlobalRole.ADMIN)
public class UsersResource {
    private static final int PAGE_LENGTH = 15;
    private final ConnectionManager connectionManager;
    private final UserService userService;

    public UsersResource(ConnectionManager connectionManager, UserService userService) {
        this.connectionManager = connectionManager;
        this.userService = userService;
    }

    @Path("/online")
    @RequiredRole(GlobalRole.SUPERADMIN)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<OnlineUser> getOnlineUsers() {
        return connectionManager.getConnections()
            .stream()
            .map(connection -> new OnlineUser(connection.getIp(), connection.getUser().getWrappedObject()))
            .collect(Collectors.toList());
    }

    @Path("/all")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public DataPage<UserData> getUsers(
        @QueryParam("page") @Min(0) int page,
        @QueryParam("search") String search
    ) {
        if (search != null) {
            search = search.replace("!", "!!");
            search = search.replace("%", "!%");
            search = search.replace("_", "!_");
            search = '%' + search + '%';
            return userService.searchPaged(page, PAGE_LENGTH, search);
        } else {
            return userService.getAllPaged(page, PAGE_LENGTH);
        }
    }

    @Path("/{userId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public UserData getUserData(
        @PathParam("userId") @Min(0) long userId
    ) {
        return userService.fetchData(userId);
    }

    @Path("/{userId}")
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateUser(
        @PathParam("userId") @Min(0) long userId,
        @Auth UserDto admin,
        UserChangeSet changeSet
    ) {
        UserDto user = userService.fetchById(userId);
        boolean anyChanges = false;
        if (changeSet.getName() != null) {
            if (user.getRole() != GlobalRole.USER || !admin.hasRole(GlobalRole.SUPERADMIN)) {
                return Response.status(403).build();
            }
            anyChanges = true;
        }
        if (changeSet.getRole() != null) {
            if (user.hasRole(admin.getRole()) || !admin.hasRole(changeSet.getRole())) {
                return Response.status(403).build();
            }
            anyChanges = true;
        }
        if (changeSet.getName() != null) {
            if (user.hasRole(admin.getRole())) {
                return Response.status(403).build();
            }
            anyChanges = true;
        }
        if (changeSet.getBanned() != null || changeSet.getRenameAvailable() != null) {
            anyChanges = true;
        }
        if (anyChanges) {
            userService.update(user, admin, changeSet);
            return Response.ok().build();
        } else {
            return Response.status(400).build();
        }
    }

    @Path("/{userId}")
    @RequiredRole(GlobalRole.SUPERADMIN)
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public UserDto deleteUser(
        @PathParam("userId") @Min(0) long userId,
        @Auth UserDto admin
    ) {
        UserDto user = userService.fetchById(userId);
        if (user != null && user.getRole() == GlobalRole.USER) {
            userService.delete(user, admin);
            return user;
        } else {
            throw new WebApplicationException(400);
        }
    }
}
