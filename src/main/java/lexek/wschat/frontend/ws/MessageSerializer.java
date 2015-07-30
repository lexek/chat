package lexek.wschat.frontend.ws;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import lexek.wschat.chat.Message;

import java.io.IOException;

public class MessageSerializer extends StdSerializer<Message> {
    protected MessageSerializer() {
        super(Message.class);
    }

    @Override
    public void serialize(Message value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeObject(value.getData());
    }
}
