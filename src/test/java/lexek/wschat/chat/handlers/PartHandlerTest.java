package lexek.wschat.chat.handlers;

import com.google.common.collect.ImmutableList;
import lexek.wschat.chat.*;
import lexek.wschat.db.model.Chatter;
import lexek.wschat.db.model.UserDto;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class PartHandlerTest {
    @Test
    public void shouldHaveJoinType() {
        PartHandler handler = new PartHandler(null, null);
        assertEquals(handler.getType(), MessageType.PART);
    }

    @Test
    public void shouldHaveSingleArgument() {
        PartHandler handler = new PartHandler(null, null);
        assertEquals(handler.getArgCount(), 1);
    }

    @Test
    public void shouldBeAvailableForAllRoles() {
        PartHandler handler = new PartHandler(null, null);
        assertEquals(handler.getRole(), GlobalRole.UNAUTHENTICATED);
    }

    @Test
    public void should() {
        RoomManager roomManager = mock(RoomManager.class);
        Room room = mock(Room.class);
        when(roomManager.getRoomInstance("#main")).thenReturn(room);
        when(roomManager.getRoomInstance(0L)).thenReturn(room);
        MessageBroadcaster messageBroadcaster = mock(MessageBroadcaster.class);
        PartHandler handler = new PartHandler(messageBroadcaster, roomManager);

        UserDto userDto = new UserDto(0L, "user", GlobalRole.USER, "#ffffff", false, false, null);
        User user = new User(userDto);
        Connection connection = spy(new TestConnection(user));
        Chatter chatter = new Chatter(0L, LocalRole.USER, false, null, user);

        when(room.contains(connection)).thenReturn(true);
        when(room.getChatter(user.getName())).thenReturn(chatter);
        when(room.getChatter(user.getId())).thenReturn(chatter);
        when(room.part(connection)).thenReturn(true);
        when(room.getName()).thenReturn("#main");

        handler.handle(ImmutableList.of("#main"), connection);

        verify(room).part(connection);
        verify(messageBroadcaster).submitMessage(eq(Message.partMessage("#main", "user")), eq(connection), eq(room.FILTER));
    }

    @Test
    public void shouldNotBroadcastMessageWhenOtherSessionActive() {
        RoomManager roomManager = mock(RoomManager.class);
        Room room = mock(Room.class);
        when(roomManager.getRoomInstance("#main")).thenReturn(room);
        when(roomManager.getRoomInstance(0L)).thenReturn(room);
        MessageBroadcaster messageBroadcaster = mock(MessageBroadcaster.class);
        PartHandler handler = new PartHandler(messageBroadcaster, roomManager);

        UserDto userDto = new UserDto(0L, "user", GlobalRole.USER, "#ffffff", false, false, null);
        User user = new User(userDto);
        Connection connection = spy(new TestConnection(user));
        Chatter chatter = new Chatter(0L, LocalRole.USER, false, null, user);

        when(room.contains(connection)).thenReturn(true);
        when(room.getChatter(user.getName())).thenReturn(chatter);
        when(room.getChatter(user.getId())).thenReturn(chatter);
        when(room.part(connection)).thenReturn(false);
        when(room.getName()).thenReturn("#main");

        handler.handle(ImmutableList.of("#main"), connection);

        verify(room).part(connection);
        verify(messageBroadcaster, never()).submitMessage(any(Message.class), any(Connection.class), any(BroadcastFilter.class));
    }

    @Test
    public void shouldReturnErrorIfNotInRoom() {
        RoomManager roomManager = mock(RoomManager.class);
        Room room = mock(Room.class);
        when(roomManager.getRoomInstance("#main")).thenReturn(room);
        when(roomManager.getRoomInstance(0L)).thenReturn(room);
        MessageBroadcaster messageBroadcaster = mock(MessageBroadcaster.class);
        PartHandler handler = new PartHandler(messageBroadcaster, roomManager);

        UserDto userDto = new UserDto(0L, "user", GlobalRole.USER, "#ffffff", false, false, null);
        User user = new User(userDto);
        Connection connection = spy(new TestConnection(user));

        when(room.contains(connection)).thenReturn(false);

        handler.handle(ImmutableList.of("#main"), connection);

        verify(room, never()).part(connection);
        verify(messageBroadcaster, never()).submitMessage(any(Message.class), any(Connection.class), any(BroadcastFilter.class));
        verify(connection).send(eq(Message.errorMessage("ROOM_NOT_JOINED")));
    }

    @Test
    public void shouldReturnErrorIfNoRoom() {
        RoomManager roomManager = mock(RoomManager.class);
        when(roomManager.getRoomInstance("#main")).thenReturn(null);
        when(roomManager.getRoomInstance(0L)).thenReturn(null);
        MessageBroadcaster messageBroadcaster = mock(MessageBroadcaster.class);
        PartHandler handler = new PartHandler(messageBroadcaster, roomManager);

        UserDto userDto = new UserDto(0L, "user", GlobalRole.USER, "#ffffff", false, false, null);
        User user = new User(userDto);
        Connection connection = spy(new TestConnection(user));

        handler.handle(ImmutableList.of("#main"), connection);

        verify(messageBroadcaster, never()).submitMessage(any(Message.class), any(Connection.class), any(BroadcastFilter.class));
        verify(connection).send(eq(Message.errorMessage("ROOM_NOT_FOUND", ImmutableList.of("#main"))));
    }

}