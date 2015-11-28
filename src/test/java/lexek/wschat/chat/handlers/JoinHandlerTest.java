package lexek.wschat.chat.handlers;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import lexek.wschat.chat.Connection;
import lexek.wschat.chat.MessageBroadcaster;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.TestConnection;
import lexek.wschat.chat.evt.EventDispatcher;
import lexek.wschat.chat.filters.BroadcastFilter;
import lexek.wschat.chat.model.*;
import lexek.wschat.db.model.UserDto;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.*;

public class JoinHandlerTest {
    @Test
    public void shouldHaveJoinType() {
        JoinHandler handler = new JoinHandler(null, null);
        assertEquals(handler.getType(), MessageType.JOIN);
    }

    @Test
    public void shouldBeAvailableForAllRoles() {
        JoinHandler handler = new JoinHandler(null, null);
        assertEquals(handler.getRole(), LocalRole.GUEST);
    }

    @Test
    public void shouldHaveRequiredProperties() {
        JoinHandler handler = new JoinHandler(null, null);
        assertEquals(
            handler.requiredProperties(),
            ImmutableSet.of(MessageProperty.ROOM)
        );
    }

    @Test
    public void shouldNotRequireJoin() {
        JoinHandler handler = new JoinHandler(null, null);
        assertFalse(handler.joinRequired());
    }

    @Test
    public void shouldNotRequireTimeout() {
        JoinHandler handler = new JoinHandler(null, null);
        assertFalse(handler.isNeedsInterval());
    }

    @Test
    public void should() {
        EventDispatcher eventDispatcher = mock(EventDispatcher.class);
        Room room = mock(Room.class);
        MessageBroadcaster messageBroadcaster = mock(MessageBroadcaster.class);
        JoinHandler handler = new JoinHandler(eventDispatcher, messageBroadcaster);

        UserDto userDto = new UserDto(0L, "user", GlobalRole.USER, "#ffffff", false, false, null, false);
        User user = new User(userDto);
        Connection connection = spy(new TestConnection(user));
        Chatter chatter = new Chatter(0L, LocalRole.USER, false, null, user);

        when(room.inRoom(connection)).thenReturn(false);
        when(room.join(connection)).thenReturn(chatter);
        when(room.getName()).thenReturn("#main");
        when(room.inRoom(user)).thenReturn(false);

        handler.handle(connection, user, room, chatter, new Message(ImmutableMap.of(
            MessageProperty.TYPE, MessageType.JOIN,
            MessageProperty.ROOM, "#main"
        )));

        verify(room).join(connection);
        verify(messageBroadcaster).submitMessage(eq(
            Message.joinMessage("#main", userDto, LocalRole.USER)),
            eq(room.FILTER)
        );
        verify(connection).send(eq(Message.selfJoinMessage("#main", chatter)));
        verify(eventDispatcher).joinedRoom(eq(connection), eq(chatter), eq(room));
    }

    @Test
    public void shouldNotBroadcastMessageWhenOtherSessionActive() {
        EventDispatcher eventDispatcher = mock(EventDispatcher.class);
        Room room = mock(Room.class);
        MessageBroadcaster messageBroadcaster = mock(MessageBroadcaster.class);
        JoinHandler handler = new JoinHandler(eventDispatcher, messageBroadcaster);

        UserDto userDto = new UserDto(0L, "user", GlobalRole.USER, "#ffffff", false, false, null, false);
        User user = new User(userDto);
        Connection connection = spy(new TestConnection(user));
        Chatter chatter = new Chatter(0L, LocalRole.USER, false, null, user);

        when(room.inRoom(connection)).thenReturn(false);
        when(room.join(connection)).thenReturn(chatter);
        when(room.getName()).thenReturn("#main");
        when(room.inRoom(user)).thenReturn(true);

        handler.handle(connection, user, room, chatter, new Message(ImmutableMap.of(
            MessageProperty.TYPE, MessageType.JOIN,
            MessageProperty.ROOM, "#main"
        )));

        verify(room).join(connection);
        verify(messageBroadcaster, never()).submitMessage(any(Message.class), any(BroadcastFilter.class));
        verify(connection).send(eq(Message.selfJoinMessage("#main", chatter)));
        verify(eventDispatcher).joinedRoom(eq(connection), eq(chatter), eq(room));
    }

    @Test
    public void shouldNotBroadcastMessageWhenRoleLowerThanUser() {
        EventDispatcher eventDispatcher = mock(EventDispatcher.class);
        Room room = mock(Room.class);
        MessageBroadcaster messageBroadcaster = mock(MessageBroadcaster.class);
        JoinHandler handler = new JoinHandler(eventDispatcher, messageBroadcaster);

        UserDto userDto = new UserDto(0L, "user", GlobalRole.UNAUTHENTICATED, "#ffffff", false, false, null, false);
        User user = new User(userDto);
        Connection connection = spy(new TestConnection(user));
        Chatter chatter = new Chatter(0L, LocalRole.GUEST, false, null, user);

        when(room.inRoom(connection)).thenReturn(false);
        when(room.join(connection)).thenReturn(chatter);
        when(room.getName()).thenReturn("#main");
        when(room.inRoom(user)).thenReturn(false);

        handler.handle(connection, user, room, chatter, new Message(ImmutableMap.of(
            MessageProperty.TYPE, MessageType.JOIN,
            MessageProperty.ROOM, "#main"
        )));

        verify(room).join(connection);
        verify(messageBroadcaster, never()).submitMessage(any(Message.class), any(BroadcastFilter.class));
        verify(connection).send(eq(Message.selfJoinMessage("#main", chatter)));
        verify(eventDispatcher).joinedRoom(eq(connection), eq(chatter), eq(room));
    }

    @Test
    public void shouldReturnErrorIfAlreadyInRoom() {
        EventDispatcher eventDispatcher = mock(EventDispatcher.class);
        Room room = mock(Room.class);
        MessageBroadcaster messageBroadcaster = mock(MessageBroadcaster.class);
        JoinHandler handler = new JoinHandler(eventDispatcher, messageBroadcaster);

        UserDto userDto = new UserDto(0L, "user", GlobalRole.USER, "#ffffff", false, false, null, false);
        User user = new User(userDto);
        Connection connection = spy(new TestConnection(user));
        Chatter chatter = new Chatter(0L, LocalRole.USER, false, null, user);

        when(room.inRoom(connection)).thenReturn(true);
        when(room.join(connection)).thenReturn(chatter);
        when(room.getName()).thenReturn("#main");
        when(room.inRoom(user)).thenReturn(true);

        handler.handle(connection, user, room, chatter, new Message(ImmutableMap.of(
            MessageProperty.TYPE, MessageType.JOIN,
            MessageProperty.ROOM, "#main"
        )));

        verify(room, never()).join(connection);
        verify(messageBroadcaster, never()).submitMessage(any(Message.class), any(BroadcastFilter.class));
        verify(connection, never()).send(eq(Message.selfJoinMessage("#main", chatter)));
        verify(eventDispatcher, never()).joinedRoom(any(Connection.class), any(Chatter.class), any(Room.class));
        verify(connection).send(eq(Message.errorMessage("ROOM_ALREADY_JOINED")));
    }
}