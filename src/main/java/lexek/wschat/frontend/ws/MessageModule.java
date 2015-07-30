package lexek.wschat.frontend.ws;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleSerializers;

import java.util.Collections;

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
        context.addSerializers(new SimpleSerializers(Collections.singletonList(new MessageSerializer())));
    }
}
