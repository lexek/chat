package lexek.wschat.frontend.http.rest;

import com.google.common.collect.ImmutableMap;
import lexek.wschat.chat.model.GlobalRole;
import lexek.wschat.db.model.ProxyAuth;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.proxy.ProxyAuthService;
import lexek.wschat.security.jersey.Auth;
import lexek.wschat.security.jersey.RequiredRole;
import lexek.wschat.security.social.provider.SocialAuthProvider;
import lexek.wschat.security.social.SocialProfile;
import lexek.wschat.security.social.SocialRedirect;
import lexek.wschat.security.social.SocialToken;
import org.hibernate.validator.constraints.NotEmpty;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/proxy/auth")
@RequiredRole(GlobalRole.ADMIN)
public class ProxyAuthResource {
    private final ProxyAuthService proxyAuthService;

    public ProxyAuthResource(ProxyAuthService proxyAuthService) {
        this.proxyAuthService = proxyAuthService;
    }

    @Path("/oauth/{serviceName}")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public Response socialAuth(
        @PathParam("serviceName") @NotEmpty String serviceName,
        @QueryParam("code") String code,
        @QueryParam("oauth_token") String oauthToken,
        @QueryParam("oauth_verifier") String oauthVerifier,
        @QueryParam("error") String error,
        @QueryParam("state") String state,
        @CookieParam("social_state") String cookieState,
        @Auth UserDto owner
    ) throws IOException {
        SocialAuthProvider socialAuthProvider = proxyAuthService.getAuthService(serviceName);
        if (socialAuthProvider == null) {
            return Response.status(404).entity(ImmutableMap.of("error", "not found")).build();
        }
        SocialToken token = null;
        if (socialAuthProvider.isV1()) {
            if (oauthToken != null && oauthVerifier != null) {
                if (!oauthToken.equals(cookieState)) {
                    return Response.status(400).entity(ImmutableMap.of("error", "state mismatch")).build();
                }
                token = socialAuthProvider.authenticate(oauthToken, oauthVerifier);
            }
        } else {
            if (code != null) {
                if (state != null && cookieState != null && !state.equals(cookieState)) {
                    return Response.status(400).entity(ImmutableMap.of("error", "state mismatch")).build();
                }
                token = socialAuthProvider.authenticate(code);
            }
        }

        if (token != null) {
            SocialProfile profile = socialAuthProvider.getProfile(token);
            proxyAuthService.registerToken(owner, profile);
            //todo
            return Response.ok(ImmutableMap.of("success", true)).build();
        } else if (error != null) {
            return Response.status(500).entity(ImmutableMap.of("error", error)).build();
        }
        SocialRedirect socialRedirect = socialAuthProvider.getRedirect();
        NewCookie stateCookie = new NewCookie("social_state", socialRedirect.getState());
        return Response.status(302).header("Location", socialRedirect.getUrl()).cookie(stateCookie).build();
    }

    @Path("/all")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public List<ProxyAuth> getAvailableCredentials(@Auth UserDto currentUser) {
        return proxyAuthService.getAllCredentials(currentUser);
    }

    @Path("/{authId}")
    @DELETE
    public Response delete(@PathParam("authId") long authId, @Auth UserDto owner) {
        proxyAuthService.deleteAuth(authId, owner);
        return Response.noContent().build();
    }

    @Path("/services")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public List<Map<String, String>> getServices() {
        return proxyAuthService
            .getServices()
            .stream()
            .map(service -> ImmutableMap.of(
                "name", service.getName(),
                "url", service.getUrl()
            ))
            .collect(Collectors.toList());
    }
}
