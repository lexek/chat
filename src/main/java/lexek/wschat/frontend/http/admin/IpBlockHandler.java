package lexek.wschat.frontend.http.admin;

import io.netty.handler.codec.http.HttpMethod;
import io.netty.util.NetUtil;
import lexek.httpserver.Request;
import lexek.httpserver.Response;
import lexek.httpserver.SimpleHttpHandler;
import lexek.wschat.chat.GlobalRole;
import lexek.wschat.security.AuthenticationManager;

import java.util.Set;

public class IpBlockHandler extends SimpleHttpHandler {
    private final Set<String> blockedIps;
    private final AuthenticationManager authenticationManager;

    public IpBlockHandler(Set<String> blockedIps, AuthenticationManager authenticationManager) {
        this.blockedIps = blockedIps;
        this.authenticationManager = authenticationManager;
    }

    @Override
    protected void handle(Request request, Response response) throws Exception {
        if (authenticationManager.hasRole(request, GlobalRole.SUPERADMIN)) {
            if (request.method() == HttpMethod.GET) {
                response.jsonContent(blockedIps);
            } else if (request.method() == HttpMethod.POST) {
                String add = request.queryParam("add");
                String remove = request.queryParam("remove");
                if (add != null && NetUtil.isValidIpV4Address(add)) {
                    blockedIps.add(add);
                }
                if (remove != null) {
                    blockedIps.remove(remove);
                }
                response.jsonContent(blockedIps);
            }
            return;
        }
        response.badRequest();
    }
}
