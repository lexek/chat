package lexek.wschat.chat.handlers;

import com.google.common.collect.ImmutableList;
import lexek.wschat.chat.*;
import lexek.wschat.db.model.Chatter;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.services.RoomJoinNotificationService;
import org.junit.Test;
import static org.mockito.Mockito.*;

import static org.junit.Assert.*;

public class JoinHandlerTest {
    @Test
    public void shouldHaveJoinType() {
        JoinHandler handler = new JoinHandler(null, null, null);
        assertEquals(handler.getType(), MessageType.JOIN);
    }

    @Test
    public void shouldHaveSingleArgument() {
        JoinHandler handler = new JoinHandler(null, null, null);
        assertEquals(handler.getArgCount(), 1);
    }

    @Test
    public void shouldBeAvailableForAllRoles() {
        JoinHandler handler = new JoinHandler(null, null, null);
        assertEquals(handler.getRole(), GlobalRole.UNAUTHENTICATED);
    }

    @Test
    public void should() {
        RoomJoinNotificationService roomJoinNotificationService = mock(RoomJoinNotificationService.class);
        RoomManager roomManager = mock(RoomManager.class);
        Room room = mock(Room.class);
        when(roomManager.getRoomInstance("#main")).thenReturn(room);
        when(roomManager.getRoomInstance(0L)).thenReturn(room);
        MessageBroadcaster messageBroadcaster = mock(MessageBroadcaster.class);
        JoinHandler handler = new JoinHandler(roomJoinNotificationService, roomManager, messageBroadcaster);

        UserDto userDto = new UserDto(0L, "user", GlobalRole.USER, "#ffffff", false, false, null);
        User user = new User(userDto);
        Connection connection = spy(new TestConnection(user));
        Chatter chatter = new Chatter(0L, LocalRole.USER, false, null, user);

        when(room.inRoom(connection)).thenReturn(false);
        when(room.join(connection)).thenReturn(chatter);
        when(room.getName()).thenReturn("#main");
        when(room.hasUser(user)).thenReturn(false);

        handler.handle(ImmutableList.of("#main"), connection);

        verify(room).join(connection);
        verify(messageBroadcaster).submitMessage(eq(Message.joinMessage("#main", userDto)), eq(connection), eq(room.FILTER));
        verify(connection).send(eq(Message.selfJoinMessage("#main", chatter)));
        verify(roomJoinNotificationService).joinedRoom(eq(connection), eq(chatter), eq(room));
    }

    @Test
    public void shouldNotBroadcastMessageWhenOtherSessionActive() {
        RoomJoinNotificationService roomJoinNotificationService = mock(RoomJoinNotificationService.class);
        RoomManager roomManager = mock(RoomManager.class);
        Room room = mock(Room.class);
        when(roomManager.getRoomInstance("#main")).thenReturn(room);
        when(roomManager.getRoomInstance(0L)).thenReturn(room);
        MessageBroadcaster messageBroadcaster = mock(MessageBroadcaster.class);
        JoinHandler handler = new JoinHandler(roomJoinNotificationService, roomManager, messageBroadcaster);

        UserDto userDto = new UserDto(0L, "user", GlobalRole.USER, "#ffffff", false, false, null);
        User user = new User(userDto);
        Connection connection = spy(new TestConnection(user));
        Chatter chatter = new Chatter(0L, LocalRole.USER, false, null, user);

        when(room.inRoom(connection)).thenReturn(false);
        when(room.join(connection)).thenReturn(chatter);
        when(room.getName()).thenReturn("#main");
        when(room.hasUser(user)).thenReturn(true);

        handler.handle(ImmutableList.of("#main"), connection);

        verify(room).join(connection);
        verify(messageBroadcaster, never()).submitMessage(any(Message.class), any(Connection.class), any(BroadcastFilter.class));
        verify(connection).send(eq(Message.selfJoinMessage("#main", chatter)));
        verify(roomJoinNotificationService).joinedRoom(eq(connection), eq(chatter), eq(room));
    }

    @Test
    public void shouldNotBroadcastMessageWhenRoleLowerThanUser() {
        RoomJoinNotificationService roomJoinNotificationService = mock(RoomJoinNotificationService.class);
        RoomManager roomManager = mock(RoomManager.class);
        Room room = mock(Room.class);
        when(roomManager.getRoomInstance("#main")).thenReturn(room);
        when(roomManager.getRoomInstance(0L)).thenReturn(room);
        MessageBroadcaster messageBroadcaster = mock(MessageBroadcaster.class);
        JoinHandler handler = new JoinHandler(roomJoinNotificationService, roomManager, messageBroadcaster);

        UserDto userDto = new UserDto(0L, "user", GlobalRole.UNAUTHENTICATED, "#ffffff", false, false, null);
        User user = new User(userDto);
        Connection connection = spy(new TestConnection(user));
        Chatter chatter = new Chatter(0L, LocalRole.GUEST, false, null, user);

        when(room.inRoom(connection)).thenReturn(false);
        when(room.join(connection)).thenReturn(chatter);
        when(room.getName()).thenReturn("#main");
        when(room.hasUser(user)).thenReturn(false);

        handler.handle(ImmutableList.of("#main"), connection);

        verify(room).join(connection);
        verify(messageBroadcaster, never()).submitMessage(any(Message.class), any(Connection.class), any(BroadcastFilter.class));
        verify(connection).send(eq(Message.selfJoinMessage("#main", chatter)));
        verify(roomJoinNotificationService).joinedRoom(eq(connection), eq(chatter), eq(room));
    }

    @Test
    public void shouldReturnErrorIfAlreadyInRoom() {
        RoomJoinNotificationService roomJoinNotificationService = mock(RoomJoinNotificationService.class);
        RoomManager roomManager = mock(RoomManager.class);
        Room room = mock(Room.class);
        when(roomManager.getRoomInstance("#main")).thenReturn(room);
        when(roomManager.getRoomInstance(0L)).thenReturn(room);
        MessageBroadcaster messageBroadcaster = mock(MessageBroadcaster.class);
        JoinHandler handler = new JoinHandler(roomJoinNotificationService, roomManager, messageBroadcaster);

        UserDto userDto = new UserDto(0L, "user", GlobalRole.USER, "#ffffff", false, false, null);
        User user = new User(userDto);
        Connection connection = spy(new TestConnection(user));
        Chatter chatter = new Chatter(0L, LocalRole.USER, false, null, user);

        when(room.inRoom(connection)).thenReturn(true);
        when(room.join(connection)).thenReturn(chatter);
        when(room.getName()).thenReturn("#main");
        when(room.hasUser(user)).thenReturn(true);

        handler.handle(ImmutableList.of("#main"), connection);

        verify(room, never()).join(connection);
        verify(messageBroadcaster, never()).submitMessage(any(Message.class), any(Connection.class), any(BroadcastFilter.class));
        verify(connection, never()).send(eq(Message.selfJoinMessage("#main", chatter)));
        verify(roomJoinNotificationService, never()).joinedRoom(any(Connection.class), any(Chatter.class), any(Room.class));
        verify(connection).send(eq(Message.errorMessage("ROOM_ALREADY_JOINED")));
    }

    @Test
    public void shouldReturnErrorIfNoRoom() {
        RoomJoinNotificationService roomJoinNotificationService = mock(RoomJoinNotificationService.class);
        RoomManager roomManager = mock(RoomManager.class);
        when(roomManager.getRoomInstance("#main")).thenReturn(null);
        when(roomManager.getRoomInstance(0L)).thenReturn(null);
        MessageBroadcaster messageBroadcaster = mock(MessageBroadcaster.class);
        JoinHandler handler = new JoinHandler(roomJoinNotificationService, roomManager, messageBroadcaster);

        UserDto userDto = new UserDto(0L, "user", GlobalRole.USER, "#ffffff", false, false, null);
        User user = new User(userDto);
        Connection connection = spy(new TestConnection(user));
        Chatter chatter = new Chatter(0L, LocalRole.USER, false, null, user);

        handler.handle(ImmutableList.of("#main"), connection);

        verify(messageBroadcaster, never()).submitMessage(any(Message.class), any(Connection.class), any(BroadcastFilter.class));
        verify(connection, never()).send(eq(Message.selfJoinMessage("#main", chatter)));
        verify(roomJoinNotificationService, never()).joinedRoom(any(Connection.class), any(Chatter.class), any(Room.class));
        verify(connection).send(eq(Message.errorMessage("ROOM_NOT_FOUND", ImmutableList.of("#main"))));
    }

}