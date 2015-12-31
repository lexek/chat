package lexek.wschat.frontend.http.rest;

import lexek.httpserver.Request;
import lexek.httpserver.Response;
import lexek.httpserver.SimpleHttpHandler;

public class RedirectToAppHandler extends SimpleHttpHandler {
    @Override
    protected void handle(Request request, Response response) throws Exception {
        response.redirect("/chat.html");
    }
}
