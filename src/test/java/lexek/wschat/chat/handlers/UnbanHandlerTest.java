package lexek.wschat.chat.handlers;

import com.google.common.collect.ImmutableList;
import lexek.wschat.chat.*;
import lexek.wschat.db.model.Chatter;
import lexek.wschat.db.model.UserDto;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class UnbanHandlerTest {
    private UserDto userDto = new UserDto(0L, "user", GlobalRole.MOD, "#000000", false, false, null);
    private User user = new User(userDto);
    private Chatter chatter = new Chatter(0L, LocalRole.MOD, false, null, user);
    private Connection connection = spy(new TestConnection(user));
    private RoomManager roomManager = mock(RoomManager.class);
    private Room room = mock(Room.class);
    private UnbanHandler handler = new UnbanHandler(roomManager);

    @Before
    public void resetMocks() {
        reset(roomManager, connection, room);
    }

    @Test
    public void testGetType() throws Exception {
        assertEquals(handler.getType(), MessageType.UNBAN);
    }

    @Test
    public void testGetRole() throws Exception {
        assertEquals(handler.getRole(), GlobalRole.USER);
    }

    @Test
    public void testGetArgCount() throws Exception {
        assertEquals(handler.getArgCount(), 2);
    }

    @Test
    public void testExistingUserWithGoodRole() {
        UserDto otherUserDto = new UserDto(1L, "username", GlobalRole.USER, "#000000", false, false, null);
        User otherUser = new User(otherUserDto);
        Chatter otherChatter = new Chatter(1L, LocalRole.USER, false, null, otherUser);
        when(roomManager.getRoomInstance("#main")).thenReturn(room);
        when(room.inRoom(connection)).thenReturn(true);
        when(room.getChatter(0L)).thenReturn(chatter);
        when(room.fetchChatter("username")).thenReturn(otherChatter);
        when(room.getName()).thenReturn("#main");
        when(room.unbanChatter(otherChatter, chatter)).thenReturn(true);
        handler.handle(ImmutableList.of("#main", "username"), connection);
        verify(room).unbanChatter(otherChatter, chatter);
        verify(connection).send(Message.infoMessage("OK"));
    }

    @Test
    public void testExistingUserWithGoodRoleButInternalError() {
        UserDto otherUserDto = new UserDto(1L, "username", GlobalRole.USER, "#000000", false, false, null);
        User otherUser = new User(otherUserDto);
        Chatter otherChatter = new Chatter(1L, LocalRole.USER, false, null, otherUser);
        when(roomManager.getRoomInstance("#main")).thenReturn(room);
        when(room.inRoom(connection)).thenReturn(true);
        when(room.getChatter(0L)).thenReturn(chatter);
        when(room.fetchChatter("username")).thenReturn(otherChatter);
        when(room.getName()).thenReturn("#main");
        when(room.unbanChatter(otherChatter, chatter)).thenReturn(false);
        handler.handle(ImmutableList.of("#main", "username"), connection);
        verify(room).unbanChatter(otherChatter, chatter);
        verify(connection, times(1)).send(eq(Message.errorMessage("INTERNAL_ERROR")));
    }


    @Test
    public void testExistingUserWithBadLocalRole() {
        UserDto otherUserDto = new UserDto(1L, "username", GlobalRole.USER, "#000000", false, false, null);
        User otherUser = new User(otherUserDto);
        Chatter otherChatter = new Chatter(1L, LocalRole.ADMIN, false, null, otherUser);
        when(roomManager.getRoomInstance("#main")).thenReturn(room);
        when(room.inRoom(connection)).thenReturn(true);
        when(room.getChatter(0L)).thenReturn(chatter);
        when(room.fetchChatter("username")).thenReturn(otherChatter);
        when(room.getName()).thenReturn("#main");
        handler.handle(ImmutableList.of("#main", "username"), connection);
        verify(room, never()).unbanChatter(otherChatter, chatter);
        verify(connection, times(1)).send(eq(Message.errorMessage("UNBAN_DENIED")));
    }

    @Test
    public void testExistingUserWithBadGlobalRole() {
        UserDto otherUserDto = new UserDto(1L, "username", GlobalRole.MOD, "#000000", false, false, null);
        User otherUser = new User(otherUserDto);
        Chatter otherChatter = new Chatter(1L, LocalRole.USER, false, null, otherUser);
        when(roomManager.getRoomInstance("#main")).thenReturn(room);
        when(room.inRoom(connection)).thenReturn(true);
        when(room.getChatter(0L)).thenReturn(chatter);
        when(room.fetchChatter("username")).thenReturn(otherChatter);
        when(room.getName()).thenReturn("#main");
        handler.handle(ImmutableList.of("#main", "username"), connection);
        verify(room, never()).unbanChatter(otherChatter, chatter);
        verify(connection, times(1)).send(eq(Message.errorMessage("UNBAN_DENIED")));
    }

    @Test
    public void testNotExistingUser() {
        when(roomManager.getRoomInstance("#main")).thenReturn(room);
        when(room.inRoom(connection)).thenReturn(true);
        when(room.getChatter(0L)).thenReturn(chatter);
        when(room.fetchChatter("username")).thenReturn(null);
        when(room.getName()).thenReturn("#main");
        handler.handle(ImmutableList.of("#main", "username"), connection);
        verify(room, never()).unbanChatter(any(Chatter.class), any(Chatter.class));
        verify(connection, times(1)).send(eq(Message.errorMessage("UNKNOWN_USER")));
    }

    @Test
    public void testWithBadRole() {
        UserDto userDto = new UserDto(0L, "user", GlobalRole.USER, "#000000", false, false, null);
        User user = new User(userDto);
        Chatter chatter = new Chatter(0L, LocalRole.USER, false, null, user);
        Connection connection = spy(new TestConnection(user));
        when(roomManager.getRoomInstance("#main")).thenReturn(room);
        when(room.inRoom(connection)).thenReturn(true);
        when(room.getChatter(0L)).thenReturn(chatter);
        when(room.getName()).thenReturn("#main");
        handler.handle(ImmutableList.of("#main", "username"), connection);
        verify(room, never()).fetchChatter("username");
        verify(room, never()).unbanChatter(any(Chatter.class), any(Chatter.class));
        verify(connection, times(1)).send(eq(Message.errorMessage("NOT_AUTHORIZED")));
    }

    @Test
    public void testNotJoined() {
        when(roomManager.getRoomInstance("#main")).thenReturn(room);
        when(room.inRoom(connection)).thenReturn(false);
        handler.handle(ImmutableList.of("#main", "username"), connection);
        verify(room, never()).unbanChatter(any(Chatter.class), any(Chatter.class));
        verify(connection, times(1)).send(eq(Message.errorMessage("NOT_JOINED")));
    }

    @Test
    public void testBadRoom() {
        when(roomManager.getRoomInstance("#main")).thenReturn(null);
        handler.handle(ImmutableList.of("#main", "username"), connection);
        verify(room, never()).unbanChatter(any(Chatter.class), any(Chatter.class));
        verify(connection, times(1)).send(eq(Message.errorMessage("UNKNOWN_ROOM")));
    }
}