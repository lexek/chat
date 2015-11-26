package lexek.wschat.chat.handlers;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import lexek.wschat.chat.Connection;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.TestConnection;
import lexek.wschat.chat.model.*;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.services.ChatterService;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class SetRoleHandlerTest {
    private UserDto userDto = new UserDto(0L, "user", GlobalRole.USER, "#000000", false, false, null, false);
    private User user = new User(userDto);
    private Chatter chatter = new Chatter(0L, LocalRole.ADMIN, false, null, user);
    private Connection connection = spy(new TestConnection(user));
    private ChatterService chatterService = mock(ChatterService.class);
    private Room room = mock(Room.class);
    private SetRoleHandler handler = new SetRoleHandler(chatterService);

    @Before
    public void resetMocks() {
        reset(chatterService, connection, room);
    }

    @Test
    public void testGetType() throws Exception {
        assertEquals(handler.getType(), MessageType.ROLE);
    }

    @Test
    public void testGetRole() throws Exception {
        assertEquals(handler.getRole(), LocalRole.ADMIN);
    }

    @Test
    public void shouldHaveRequiredProperties() throws Exception {
        assertEquals(
            handler.requiredProperties(),
            ImmutableSet.of(MessageProperty.ROOM, MessageProperty.NAME, MessageProperty.LOCAL_ROLE)
        );
    }

    @Test
    public void shouldRequireJoin() {
        assertTrue(handler.joinRequired());
    }

    @Test
    public void shouldNotRequireTimeout() {
        assertFalse(handler.isNeedsInterval());
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
        when(room.inRoom(connection)).thenReturn(true);
        when(room.getOnlineChatter(userDto)).thenReturn(chatter);
        when(room.getChatter("username")).thenReturn(otherChatter);
        when(room.getName()).thenReturn("#main");
        when(chatterService.setRole(room, otherChatter, chatter, LocalRole.MOD)).thenReturn(true);
        handler.handle(connection, user, room, chatter, new Message(ImmutableMap.of(
            MessageProperty.TYPE, MessageType.ROLE,
            MessageProperty.ROOM, "#main",
            MessageProperty.NAME, "username",
            MessageProperty.LOCAL_ROLE, LocalRole.MOD
        )));
        verify(chatterService).setRole(room, otherChatter, chatter, LocalRole.MOD);
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
        when(room.inRoom(connection)).thenReturn(true);
        when(room.getOnlineChatter(userDto)).thenReturn(chatter);
        when(room.getChatter("username")).thenReturn(otherChatter);
        when(room.getName()).thenReturn("#main");
        when(chatterService.setRole(room, otherChatter, chatter, LocalRole.MOD)).thenReturn(true);
        handler.handle(connection, user, room, chatter, new Message(ImmutableMap.of(
            MessageProperty.TYPE, MessageType.ROLE,
            MessageProperty.ROOM, "#main",
            MessageProperty.NAME, "username",
            MessageProperty.LOCAL_ROLE, LocalRole.MOD
        )));
        verify(chatterService).setRole(room, otherChatter, chatter, LocalRole.MOD);
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
        when(room.inRoom(connection)).thenReturn(true);
        when(room.getOnlineChatter(userDto)).thenReturn(chatter);
        when(room.getChatter("username")).thenReturn(otherChatter);
        when(room.getName()).thenReturn("#main");
        when(chatterService.setRole(room, otherChatter, chatter, LocalRole.MOD)).thenReturn(true);

        handler.handle(connection, user, room, chatter, new Message(ImmutableMap.of(
            MessageProperty.TYPE, MessageType.ROLE,
            MessageProperty.ROOM, "#main",
            MessageProperty.NAME, "username",
            MessageProperty.LOCAL_ROLE, LocalRole.MOD
        )));

        verify(chatterService, never()).setRole(room, otherChatter, chatter, LocalRole.MOD);
        verify(connection).send(Message.errorMessage("ROLE_DENIED"));
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
        when(room.inRoom(connection)).thenReturn(true);
        when(room.getOnlineChatter(userDto)).thenReturn(chatter);
        when(room.getChatter("username")).thenReturn(otherChatter);
        when(room.getName()).thenReturn("#main");
        when(chatterService.setRole(room, otherChatter, chatter, LocalRole.MOD)).thenReturn(true);

        handler.handle(connection, user, room, chatter, new Message(ImmutableMap.of(
            MessageProperty.TYPE, MessageType.ROLE,
            MessageProperty.ROOM, "#main",
            MessageProperty.NAME, "username",
            MessageProperty.LOCAL_ROLE, LocalRole.MOD
        )));

        verify(chatterService, never()).setRole(room, otherChatter, chatter, LocalRole.MOD);
        verify(connection).send(Message.errorMessage("ROLE_DENIED"));
    }

    @Test
    public void testExistingUserWithGoodRoleButInternalError() {
        UserDto otherUserDto = new UserDto(1L, "user", GlobalRole.USER, "#000000", false, false, null, false);
        User otherUser = new User(otherUserDto);
        Chatter otherChatter = new Chatter(1L, LocalRole.USER, false, null, otherUser);
        when(room.inRoom(connection)).thenReturn(true);
        when(room.getOnlineChatter(userDto)).thenReturn(chatter);
        when(room.getChatter("username")).thenReturn(otherChatter);
        when(room.getName()).thenReturn("#main");
        when(chatterService.setRole(room, otherChatter, chatter, LocalRole.MOD)).thenReturn(false);

        handler.handle(connection, user, room, chatter, new Message(ImmutableMap.of(
            MessageProperty.TYPE, MessageType.ROLE,
            MessageProperty.ROOM, "#main",
            MessageProperty.NAME, "username",
            MessageProperty.LOCAL_ROLE, LocalRole.MOD
        )));

        verify(chatterService).setRole(room, otherChatter, chatter, LocalRole.MOD);
        verify(connection, times(1)).send(eq(Message.errorMessage("INTERNAL_ERROR")));
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
        when(room.inRoom(connection)).thenReturn(true);
        when(room.getOnlineChatter(userDto)).thenReturn(chatter);
        when(room.getChatter("username")).thenReturn(otherChatter);
        when(room.getName()).thenReturn("#main");
        handler.handle(connection, user, room, chatter, new Message(ImmutableMap.of(
            MessageProperty.TYPE, MessageType.ROLE,
            MessageProperty.ROOM, "#main",
            MessageProperty.NAME, "username",
            MessageProperty.LOCAL_ROLE, LocalRole.MOD
        )));
        verify(chatterService, never()).setRole(room, otherChatter, chatter, LocalRole.MOD);
        verify(connection, times(1)).send(eq(Message.errorMessage("ROLE_DENIED")));
    }

    @Test
    public void testNotExistingUser() {
        when(room.inRoom(connection)).thenReturn(true);
        when(room.getOnlineChatter(userDto)).thenReturn(chatter);
        when(room.getChatter("username")).thenReturn(null);
        when(room.getName()).thenReturn("#main");
        handler.handle(connection, user, room, chatter, new Message(ImmutableMap.of(
            MessageProperty.TYPE, MessageType.ROLE,
            MessageProperty.ROOM, "#main",
            MessageProperty.NAME, "username",
            MessageProperty.LOCAL_ROLE, LocalRole.MOD
        )));
        verify(chatterService, never()).setRole(eq(room), any(Chatter.class), any(Chatter.class), any(LocalRole.class));
        verify(connection, times(1)).send(eq(Message.errorMessage("UNKNOWN_USER")));
    }
}