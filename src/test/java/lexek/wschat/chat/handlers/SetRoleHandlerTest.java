package lexek.wschat.chat.handlers;

import com.google.common.collect.ImmutableList;
import lexek.wschat.chat.*;
import lexek.wschat.chat.Chatter;
import lexek.wschat.db.model.UserDto;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class SetRoleHandlerTest {
    private UserDto userDto = new UserDto(0L, "user", GlobalRole.USER, "#000000", false, false, null, false);
    private User user = new User(userDto);
    private Chatter chatter = new Chatter(0L, LocalRole.ADMIN, false, null, user);
    private Connection connection = spy(new TestConnection(user));
    private RoomManager roomManager = mock(RoomManager.class);
    private Room room = mock(Room.class);
    private SetRoleHandler handler = new SetRoleHandler(roomManager);

    @Before
    public void resetMocks() {
        reset(roomManager, connection, room);
    }

    @Test
    public void testGetType() throws Exception {
        assertEquals(handler.getType(), MessageType.ROLE);
    }

    @Test
    public void testGetRole() throws Exception {
        assertEquals(handler.getRole(), GlobalRole.USER);
    }

    @Test
    public void testGetArgCount() throws Exception {
        assertEquals(handler.getArgCount(), 3);
    }

    @Test
    public void shouldSuccessWithHigherLocalRole() {
        UserDto userDto = new UserDto(0L, "user", GlobalRole.USER, "#000000", false, false, null, false);
        User user = new User(userDto);
        Chatter chatter = new Chatter(0L, LocalRole.ADMIN, false, null, user);
        Connection connection = spy(new TestConnection(user));
        UserDto otherUserDto = new UserDto(1L, "user", GlobalRole.USER, "#000000", false, false, null, false);
        User otherUser = new User(otherUserDto);
        Chatter otherChatter = new Chatter(1L, LocalRole.USER, false, null, otherUser);
        when(roomManager.getRoomInstance("#main")).thenReturn(room);
        when(room.inRoom(connection)).thenReturn(true);
        when(room.getOnlineChatter(userDto)).thenReturn(chatter);
        when(room.getChatter("username")).thenReturn(otherChatter);
        when(room.getName()).thenReturn("#main");
        when(room.setRole(otherChatter, chatter, LocalRole.MOD)).thenReturn(true);
        handler.handle(ImmutableList.of("#main", "username", "MOD"), connection);
        verify(room).setRole(otherChatter, chatter, LocalRole.MOD);
        verify(connection).send(Message.infoMessage("OK"));
    }

    @Test
    public void shouldSuccessWithHigherGlobalRole() {
        UserDto userDto = new UserDto(0L, "user", GlobalRole.ADMIN, "#000000", false, false, null, false);
        User user = new User(userDto);
        Chatter chatter = new Chatter(0L, LocalRole.USER, false, null, user);
        Connection connection = spy(new TestConnection(user));
        UserDto otherUserDto = new UserDto(1L, "user", GlobalRole.USER, "#000000", false, false, null, false);
        User otherUser = new User(otherUserDto);
        Chatter otherChatter = new Chatter(1L, LocalRole.USER, false, null, otherUser);
        when(roomManager.getRoomInstance("#main")).thenReturn(room);
        when(room.inRoom(connection)).thenReturn(true);
        when(room.getOnlineChatter(userDto)).thenReturn(chatter);
        when(room.getChatter("username")).thenReturn(otherChatter);
        when(room.getName()).thenReturn("#main");
        when(room.setRole(otherChatter, chatter, LocalRole.MOD)).thenReturn(true);
        handler.handle(ImmutableList.of("#main", "username", "MOD"), connection);
        verify(room).setRole(otherChatter, chatter, LocalRole.MOD);
        verify(connection).send(Message.infoMessage("OK"));
    }

    @Test
    public void shouldFailIfOtherUserHasHigherGlobalRole() {
        UserDto userDto = new UserDto(0L, "user", GlobalRole.ADMIN, "#000000", false, false, null, false);
        User user = new User(userDto);
        Chatter chatter = new Chatter(0L, LocalRole.USER, false, null, user);
        Connection connection = spy(new TestConnection(user));
        UserDto otherUserDto = new UserDto(1L, "user", GlobalRole.SUPERADMIN, "#000000", false, false, null, false);
        User otherUser = new User(otherUserDto);
        Chatter otherChatter = new Chatter(1L, LocalRole.USER, false, null, otherUser);
        when(roomManager.getRoomInstance("#main")).thenReturn(room);
        when(room.inRoom(connection)).thenReturn(true);
        when(room.getOnlineChatter(userDto)).thenReturn(chatter);
        when(room.getChatter("username")).thenReturn(otherChatter);
        when(room.getName()).thenReturn("#main");
        when(room.setRole(otherChatter, chatter, LocalRole.MOD)).thenReturn(true);

        handler.handle(ImmutableList.of("#main", "username", "MOD"), connection);

        verify(room, never()).setRole(otherChatter, chatter, LocalRole.MOD);
        verify(connection).send(Message.errorMessage("BAN_DENIED"));
    }

    @Test
    public void shouldFailIfOtherUserHasAdminLocalRole() {
        UserDto userDto = new UserDto(0L, "user", GlobalRole.USER, "#000000", false, false, null, false);
        User user = new User(userDto);
        Chatter chatter = new Chatter(0L, LocalRole.ADMIN, false, null, user);
        Connection connection = spy(new TestConnection(user));
        UserDto otherUserDto = new UserDto(1L, "user", GlobalRole.SUPERADMIN, "#000000", false, false, null, false);
        User otherUser = new User(otherUserDto);
        Chatter otherChatter = new Chatter(1L, LocalRole.ADMIN, false, null, otherUser);
        when(roomManager.getRoomInstance("#main")).thenReturn(room);
        when(room.inRoom(connection)).thenReturn(true);
        when(room.getOnlineChatter(userDto)).thenReturn(chatter);
        when(room.getChatter("username")).thenReturn(otherChatter);
        when(room.getName()).thenReturn("#main");
        when(room.setRole(otherChatter, chatter, LocalRole.MOD)).thenReturn(true);

        handler.handle(ImmutableList.of("#main", "username", "MOD"), connection);

        verify(room, never()).setRole(otherChatter, chatter, LocalRole.MOD);
        verify(connection).send(Message.errorMessage("BAN_DENIED"));
    }

    @Test
    public void testExistingUserWithGoodRoleButInternalError() {
        UserDto otherUserDto = new UserDto(1L, "user", GlobalRole.USER, "#000000", false, false, null, false);
        User otherUser = new User(otherUserDto);
        Chatter otherChatter = new Chatter(1L, LocalRole.USER, false, null, otherUser);
        when(roomManager.getRoomInstance("#main")).thenReturn(room);
        when(room.inRoom(connection)).thenReturn(true);
        when(room.getOnlineChatter(userDto)).thenReturn(chatter);
        when(room.getChatter("username")).thenReturn(otherChatter);
        when(room.getName()).thenReturn("#main");
        when(room.setRole(otherChatter, chatter, LocalRole.MOD)).thenReturn(false);
        handler.handle(ImmutableList.of("#main", "username", "MOD"), connection);
        verify(room).setRole(otherChatter, chatter, LocalRole.MOD);
        verify(connection, times(1)).send(eq(Message.errorMessage("INTERNAL_ERROR")));
    }

    @Test
    public void testUnknownRole() {
        UserDto otherUserDto = new UserDto(1L, "user", GlobalRole.USER, "#000000", false, false, null, false);
        User otherUser = new User(otherUserDto);
        Chatter otherChatter = new Chatter(1L, LocalRole.USER, false, null, otherUser);
        when(roomManager.getRoomInstance("#main")).thenReturn(room);
        when(room.inRoom(connection)).thenReturn(true);
        when(room.getOnlineChatter(userDto)).thenReturn(chatter);
        when(room.getChatter("username")).thenReturn(otherChatter);
        when(room.getName()).thenReturn("#main");
        when(room.setRole(otherChatter, chatter, LocalRole.MOD)).thenReturn(true);
        handler.handle(ImmutableList.of("#main", "username", "KKK"), connection);
        verify(room, never()).setRole(otherChatter, chatter, LocalRole.MOD);
        verify(connection, times(1)).send(eq(Message.errorMessage("UNKNOWN_ROLE")));
    }

    @Test
    public void testExistingUserWithBadLocalRole() {
        UserDto userDto = new UserDto(0L, "user", GlobalRole.USER, "#000000", false, false, null, false);
        User user = new User(userDto);
        Chatter chatter = new Chatter(0L, LocalRole.ADMIN, false, null, user);
        Connection connection = spy(new TestConnection(user));
        UserDto otherUserDto = new UserDto(1L, "user", GlobalRole.USER, "#000000", false, false, null, false);
        User otherUser = new User(otherUserDto);
        Chatter otherChatter = new Chatter(1L, LocalRole.ADMIN, false, null, otherUser);
        when(roomManager.getRoomInstance("#main")).thenReturn(room);
        when(room.inRoom(connection)).thenReturn(true);
        when(room.getOnlineChatter(userDto)).thenReturn(chatter);
        when(room.getChatter("username")).thenReturn(otherChatter);
        when(room.getName()).thenReturn("#main");
        handler.handle(ImmutableList.of("#main", "username", "MOD"), connection);
        verify(room, never()).setRole(otherChatter, chatter, LocalRole.MOD);
        verify(connection, times(1)).send(eq(Message.errorMessage("BAN_DENIED")));
    }

    @Test
    public void testNotExistingUser() {
        when(roomManager.getRoomInstance("#main")).thenReturn(room);
        when(room.inRoom(connection)).thenReturn(true);
        when(room.getOnlineChatter(userDto)).thenReturn(chatter);
        when(room.getChatter("username")).thenReturn(null);
        when(room.getName()).thenReturn("#main");
        handler.handle(ImmutableList.of("#main", "username", "MOD"), connection);
        verify(room, never()).setRole(any(Chatter.class), any(Chatter.class), any(LocalRole.class));
        verify(connection, times(1)).send(eq(Message.errorMessage("UNKNOWN_USER")));
    }

    @Test
    public void shouldFailForUserLocalRole() {
        UserDto userDto = new UserDto(0L, "user", GlobalRole.USER, "#000000", false, false, null, false);
        User user = new User(userDto);
        Chatter chatter = new Chatter(0L, LocalRole.USER, false, null, user);
        Connection connection = spy(new TestConnection(user));
        when(roomManager.getRoomInstance("#main")).thenReturn(room);
        when(room.inRoom(connection)).thenReturn(true);
        when(room.getOnlineChatter(userDto)).thenReturn(chatter);
        when(room.getName()).thenReturn("#main");
        handler.handle(ImmutableList.of("#main", "username", "MOD"), connection);
        verify(room, never()).getChatter("username");
        verify(room, never()).setRole(any(Chatter.class), any(Chatter.class), any(LocalRole.class));
        verify(connection, times(1)).send(eq(Message.errorMessage("NOT_AUTHORIZED")));
    }

    @Test
    public void testNotJoined() {
        when(roomManager.getRoomInstance("#main")).thenReturn(room);
        when(room.inRoom(connection)).thenReturn(false);
        handler.handle(ImmutableList.of("#main", "username", "MOD"), connection);
        verify(room, never()).setRole(any(Chatter.class), any(Chatter.class), any(LocalRole.class));
        verify(connection, times(1)).send(eq(Message.errorMessage("NOT_JOINED")));
    }

    @Test
    public void testBadRoom() {
        when(roomManager.getRoomInstance("#main")).thenReturn(null);
        handler.handle(ImmutableList.of("#main", "username", "MOD"), connection);
        verify(room, never()).setRole(any(Chatter.class), any(Chatter.class), any(LocalRole.class));
        verify(connection, times(1)).send(eq(Message.errorMessage("UNKNOWN_ROOM")));
    }
}