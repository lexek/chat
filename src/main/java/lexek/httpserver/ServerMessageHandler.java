package lexek.httpserver;

public class ServerMessageHandler {
    protected void handle(ServerMessage serverMessage, Response response) throws Exception {
        response.renderTemplate("server_message", serverMessage);
    }
}
