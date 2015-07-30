package lexek.wschat.frontend.http.rest;

import com.google.common.collect.ImmutableMap;
import lexek.wschat.db.model.form.UsernameForm;
import lexek.wschat.services.UserService;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Map;

@Path("/checkUsername")
public class CheckUsernameResource {
    private final UserService userService;

    public CheckUsernameResource(UserService userService) {
        this.userService = userService;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map check(
        @Valid UsernameForm usernameForm
    ) {
        return ImmutableMap.of("available", userService.checkIfAvailable(usernameForm.getName()));
    }
}
