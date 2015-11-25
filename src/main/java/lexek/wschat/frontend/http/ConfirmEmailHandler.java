package lexek.wschat.frontend.http;

import lexek.httpserver.Request;
import lexek.httpserver.Response;
import lexek.httpserver.SimpleHttpHandler;

public class ConfirmEmailHandler extends SimpleHttpHandler {

    @Override
    protected void handle(Request request, Response response) throws Exception {
        response.stringContent("this address is no longer valid, you need to request new verification email");
    }
}
