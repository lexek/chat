package lexek.wschat.frontend.http.rest;

import lexek.wschat.chat.model.GlobalRole;
import lexek.wschat.db.model.UserData;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.security.jersey.Auth;
import lexek.wschat.security.jersey.RequiredRole;
import lexek.wschat.services.UserService;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/profile")
@RequiredRole(GlobalRole.USER_UNCONFIRMED)
public class ProfileResource {
    private final UserService userService;

    public ProfileResource(UserService userService) {
        this.userService = userService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public UserData getProfile(@Auth UserDto user) {
        return userService.fetchData(user.getId());
    }
}
