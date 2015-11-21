package lexek.wschat.chat;

import lexek.wschat.chat.filters.BroadcastFilter;
import lexek.wschat.chat.filters.GlobalRoleFilter;

public enum GlobalRole {
    UNAUTHENTICATED(300),
    USER_UNCONFIRMED(30000),
    USER(200),
    MOD(0),
    ADMIN(0),
    SUPERADMIN(0);

    public final BroadcastFilter<GlobalRole> FILTER = new GlobalRoleFilter(this);
    private final int messageTimeInterval;

    GlobalRole(int messageTimeInterval) {
        this.messageTimeInterval = messageTimeInterval;
    }

    public int getMessageTimeInterval() {
        return messageTimeInterval;
    }
}
