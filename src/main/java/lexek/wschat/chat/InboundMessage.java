package lexek.wschat.chat;

import com.google.common.collect.ImmutableList;

import java.util.List;

public class InboundMessage {
    private final MessageType type;
    private final List<String> args;

    public InboundMessage(MessageType type, List<String> args) {
        this.type = type;
        this.args = args;
    }

    public InboundMessage(MessageType type, String... args) {
        this.type = type;
        this.args = ImmutableList.copyOf(args);
    }

    public MessageType getType() {
        return type;
    }

    public List<String> getArgs() {
        return args != null ? args : ImmutableList.<String>of();
    }

    public String getArg(int i) {
        return args.get(i);
    }

    public int getArgCount() {
        return args != null ? this.args.size() : 0;
    }

    @Override
    public String toString() {
        return "InboundMessage{" +
                "type=" + type +
                ", args=" + args +
                '}';
    }
}
