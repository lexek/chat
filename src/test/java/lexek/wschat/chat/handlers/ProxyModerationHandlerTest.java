package lexek.wschat.chat.handlers;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import lexek.wschat.chat.Connection;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.TestConnection;
import lexek.wschat.chat.model.*;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.proxy.ModerationOperation;
import lexek.wschat.proxy.ProxyManager;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class ProxyModerationHandlerTest {
    @Test
    public void shouldHaveLikeType() {
        ProxyModerationHandler handler = new ProxyModerationHandler(null);
        assertEquals(handler.getType(), MessageType.PROXY_MOD);
    }

    @Test
    public void shouldBeAvailableOnlyForUsers() {
        ProxyModerationHandler handler = new ProxyModerationHandler(null);
        assertEquals(handler.getRole(), LocalRole.MOD);
    }

    @Test
    public void shouldHaveRequiredProperties() throws Exception {
        ProxyModerationHandler handler = new ProxyModerationHandler(null);
        assertEquals(
            handler.requiredProperties(),
            ImmutableSet.of(
                MessageProperty.ROOM,
                MessageProperty.NAME,
                MessageProperty.SERVICE,
                MessageProperty.SERVICE_RESOURCE,
                MessageProperty.TEXT
            )
        );
    }

    @Test
    public void shouldRequireJoin() {
        ProxyModerationHandler handler = new ProxyModerationHandler(null);
        assertTrue(handler.joinRequired());
    }

    @Test
    public void shouldRequireTimeout() {
        ProxyModerationHandler handler = new ProxyModerationHandler(null);
        assertFalse(handler.isNeedsInterval());
    }

    @Test
    public void shouldWork() {
        Room room = mock(Room.class);
        ProxyManager proxyManager = mock(ProxyManager.class);
        ProxyModerationHandler handler = new ProxyModerationHandler(proxyManager);

        UserDto userDto = new UserDto(0L, "user", GlobalRole.USER, "#ffffff", false, false, null, false, false);
        User user = new CachedUser(userDto, cache);
        Connection connection = spy(new TestConnection(user));
        Chatter chatter = new Chatter(0L, LocalRole.USER, false, null, user);

        when(room.inRoom(connection)).thenReturn(true);
        when(room.join(connection)).thenReturn(chatter);
        when(room.getName()).thenReturn("#main");
        when(room.getOnlineChatter(userDto)).thenReturn(chatter);
        when(room.getOnlineChatterByName(user.getName())).thenReturn(chatter);

        handler.handle(connection, user, room, chatter, new Message(ImmutableMap.<MessageProperty, Object>builder()
            .put(MessageProperty.TYPE, MessageType.PROXY_MOD)
            .put(MessageProperty.ROOM, "#main")
            .put(MessageProperty.NAME, "kek")
            .put(MessageProperty.SERVICE, "service")
            .put(MessageProperty.SERVICE_RESOURCE, "resource")
            .put(MessageProperty.TEXT, "BAN")
            .build()
        ));

        verify(proxyManager).moderate(eq(room), eq("service"), eq("resource"), eq(ModerationOperation.BAN), eq("kek"));
    }

    @Test
    public void shouldSendErrorOnUnknownType() {
        Room room = mock(Room.class);
        ProxyManager proxyManager = mock(ProxyManager.class);
        ProxyModerationHandler handler = new ProxyModerationHandler(proxyManager);

        UserDto userDto = new UserDto(0L, "user", GlobalRole.USER, "#ffffff", false, false, null, false, false);
        User user = new CachedUser(userDto, cache);
        Connection connection = spy(new TestConnection(user));
        Chatter chatter = new Chatter(0L, LocalRole.USER, false, null, user);

        when(room.inRoom(connection)).thenReturn(true);
        when(room.join(connection)).thenReturn(chatter);
        when(room.getName()).thenReturn("#main");
        when(room.getOnlineChatter(userDto)).thenReturn(chatter);
        when(room.getOnlineChatterByName(user.getName())).thenReturn(chatter);

        handler.handle(connection, user, room, chatter, new Message(ImmutableMap.<MessageProperty, Object>builder()
            .put(MessageProperty.TYPE, MessageType.PROXY_MOD)
            .put(MessageProperty.ROOM, "#main")
            .put(MessageProperty.NAME, "kek")
            .put(MessageProperty.SERVICE, "service")
            .put(MessageProperty.SERVICE_RESOURCE, "resource")
            .put(MessageProperty.TEXT, "WHATT")
            .build()
        ));

        verify(proxyManager, never()).moderate(any(), any(), any(), any(), any());
    }
}