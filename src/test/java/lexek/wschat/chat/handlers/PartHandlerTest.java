package lexek.wschat.chat.handlers;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import lexek.wschat.chat.Connection;
import lexek.wschat.chat.MessageBroadcaster;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.TestConnection;
import lexek.wschat.chat.filters.BroadcastFilter;
import lexek.wschat.chat.model.*;
import lexek.wschat.db.model.UserDto;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class PartHandlerTest {
    @Test
    public void shouldHaveJoinType() {
        PartHandler handler = new PartHandler(null);
        assertEquals(handler.getType(), MessageType.PART);
    }

    @Test
    public void shouldBeAvailableForAllRoles() {
        PartHandler handler = new PartHandler(null);
        assertEquals(handler.getRole(), LocalRole.GUEST);
    }

    @Test
    public void shouldHaveRequiredProperties() throws Exception {
        PartHandler handler = new PartHandler(null);
        assertEquals(
            handler.requiredProperties(),
            ImmutableSet.of(MessageProperty.ROOM)
        );
    }

    @Test
    public void shouldRequireJoin() {
        PartHandler handler = new PartHandler(null);
        assertTrue(handler.joinRequired());
    }

    @Test
    public void shouldRequireTimeout() {
        PartHandler handler = new PartHandler(null);
        assertTrue(handler.isNeedsInterval());
    }

    @Test
    public void should() {
        Room room = mock(Room.class);
        MessageBroadcaster messageBroadcaster = mock(MessageBroadcaster.class);
        PartHandler handler = new PartHandler(messageBroadcaster);

        UserDto userDto = new UserDto(0L, "user", GlobalRole.USER, "#ffffff", false, false, null, false, false);
        User user = new CachedUser(userDto, cache);
        Connection connection = spy(new TestConnection(user));
        Chatter chatter = new Chatter(0L, LocalRole.USER, false, null, user);

        when(room.inRoom(connection)).thenReturn(true);
        when(room.getOnlineChatterByName(user.getName())).thenReturn(chatter);
        when(room.getOnlineChatter(userDto)).thenReturn(chatter);
        when(room.part(connection)).thenReturn(true);
        when(room.getName()).thenReturn("#main");

        handler.handle(connection, user, room, chatter, new Message(ImmutableMap.of(
            MessageProperty.TYPE, MessageType.PART,
            MessageProperty.ROOM, "#main"
        )));

        verify(room).part(connection);
        verify(messageBroadcaster).submitMessage(eq(Message.partMessage("#main", "user")), eq(room.FILTER));
    }

    @Test
    public void shouldNotBroadcastMessageWhenOtherSessionActive() {
        Room room = mock(Room.class);
        MessageBroadcaster messageBroadcaster = mock(MessageBroadcaster.class);
        PartHandler handler = new PartHandler(messageBroadcaster);

        UserDto userDto = new UserDto(0L, "user", GlobalRole.USER, "#ffffff", false, false, null, false, false);
        User user = new CachedUser(userDto, cache);
        Connection connection = spy(new TestConnection(user));
        Chatter chatter = new Chatter(0L, LocalRole.USER, false, null, user);

        when(room.inRoom(connection)).thenReturn(true);
        when(room.getOnlineChatterByName(user.getName())).thenReturn(chatter);
        when(room.getOnlineChatter(userDto)).thenReturn(chatter);
        when(room.part(connection)).thenReturn(false);
        when(room.getName()).thenReturn("#main");

        handler.handle(connection, user, room, chatter, new Message(ImmutableMap.of(
            MessageProperty.TYPE, MessageType.PART,
            MessageProperty.ROOM, "#main"
        )));

        verify(room).part(connection);
        verify(messageBroadcaster, never()).submitMessage(any(Message.class), any(BroadcastFilter.class));
    }

    @Test
    public void shouldNotBroadcastMessageWhenRoleLowerThanUser() {
        Room room = mock(Room.class);
        MessageBroadcaster messageBroadcaster = mock(MessageBroadcaster.class);
        PartHandler handler = new PartHandler(messageBroadcaster);

        UserDto userDto = new UserDto(0L, "user", GlobalRole.UNAUTHENTICATED, "#ffffff", false, false, null, false, false);
        User user = new CachedUser(userDto, cache);
        Connection connection = spy(new TestConnection(user));
        Chatter chatter = new Chatter(0L, LocalRole.GUEST, false, null, user);

        when(room.inRoom(connection)).thenReturn(true);
        when(room.getOnlineChatterByName(user.getName())).thenReturn(chatter);
        when(room.getOnlineChatter(userDto)).thenReturn(chatter);
        when(room.part(connection)).thenReturn(true);
        when(room.getName()).thenReturn("#main");

        handler.handle(connection, user, room, chatter, new Message(ImmutableMap.of(
            MessageProperty.TYPE, MessageType.PART,
            MessageProperty.ROOM, "#main"
        )));

        verify(room).part(connection);
        verify(messageBroadcaster, never()).submitMessage(any(Message.class), any(BroadcastFilter.class));
    }
}