package lexek.wschat.frontend.ws;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleDeserializers;
import com.fasterxml.jackson.databind.module.SimpleSerializers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import lexek.wschat.chat.model.Message;

public class MessageModule extends Module {
    @Override
    public String getModuleName() {
        return "chat.message";
    }

    @Override
    public Version version() {
        return new Version(1, 0, 0, "SNAPSHOT", "lexek.wschat", "wschat");
    }

    @Override
    public void setupModule(SetupContext context) {
        context.addSerializers(new SimpleSerializers(ImmutableList.of(new MessageSerializer())));
        context.addDeserializers(new SimpleDeserializers(ImmutableMap.of(Message.class, new MessageDeserializer())));
    }
}
