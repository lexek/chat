package lexek.wschat.chat.handlers;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import lexek.wschat.chat.*;
import lexek.wschat.chat.model.*;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.services.ChatterService;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class BanHandlerTest {
    private UserDto userDto = new UserDto(0L, "user", GlobalRole.MOD, "#000000", false, false, null, false);
    private User user = new User(userDto);
    private Chatter chatter = new Chatter(0L, LocalRole.MOD, false, null, user);
    private Connection connection = spy(new TestConnection(user));
    private ChatterService chatterService = mock(ChatterService.class);
    private Room room = mock(Room.class);
    private BanHandler handler = new BanHandler(chatterService);

    @Before
    public void resetMocks() {
        reset(chatterService, connection, room);
    }

    @Test
    public void testGetType() throws Exception {
        assertEquals(handler.getType(), MessageType.BAN);
    }

    @Test
    public void testGetRole() throws Exception {
        assertEquals(handler.getRole(), LocalRole.MOD);
    }

    @Test
    public void shouldHaveRequiredProperties() throws Exception {
        assertEquals(
            handler.requiredProperties(),
            ImmutableSet.of(MessageProperty.ROOM, MessageProperty.NAME)
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
    public void testExistingUserWithGoodRole() {
        UserDto otherUserDto = new UserDto(1L, "username", GlobalRole.USER, "#000000", false, false, null, false);
        User otherUser = new User(otherUserDto);
        Chatter otherChatter = new Chatter(1L, LocalRole.USER, false, null, otherUser);
        when(room.inRoom(connection)).thenReturn(true);
        when(room.getOnlineChatter(userDto)).thenReturn(chatter);
        when(room.getChatter("username")).thenReturn(otherChatter);
        when(room.getName()).thenReturn("#main");
        when(chatterService.banChatter(room, otherChatter, chatter)).thenReturn(true);
        handler.handle(connection, user, room, chatter, new Message(ImmutableMap.of(
            MessageProperty.TYPE, MessageType.BAN,
            MessageProperty.ROOM, "#main",
            MessageProperty.NAME, "username"
        )));
        verify(chatterService).banChatter(room, otherChatter, chatter);
    }

    @Test
    public void testExistingUserWithGoodRoleButInternalError() {
        UserDto otherUserDto = new UserDto(1L, "username", GlobalRole.USER, "#000000", false, false, null, false);
        User otherUser = new User(otherUserDto);
        Chatter otherChatter = new Chatter(1L, LocalRole.USER, false, null, otherUser);
        when(room.inRoom(connection)).thenReturn(true);
        when(room.getOnlineChatter(userDto)).thenReturn(chatter);
        when(room.getChatter("username")).thenReturn(otherChatter);
        when(room.getName()).thenReturn("#main");
        when(chatterService.banChatter(room, otherChatter, chatter)).thenReturn(false);
        handler.handle(connection, user, room, chatter, new Message(ImmutableMap.of(
            MessageProperty.TYPE, MessageType.BAN,
            MessageProperty.ROOM, "#main",
            MessageProperty.NAME, "username"
        )));
        verify(chatterService).banChatter(room, otherChatter, chatter);
        verify(connection, times(1)).send(eq(Message.errorMessage("INTERNAL_ERROR")));
    }


    @Test
    public void testExistingUserWithBadLocalRole() {
        UserDto otherUserDto = new UserDto(1L, "username", GlobalRole.USER, "#000000", false, false, null, false);
        User otherUser = new User(otherUserDto);
        Chatter otherChatter = new Chatter(1L, LocalRole.ADMIN, false, null, otherUser);
        when(room.inRoom(connection)).thenReturn(true);
        when(room.getOnlineChatter(userDto)).thenReturn(chatter);
        when(room.getChatter("username")).thenReturn(otherChatter);
        when(room.getName()).thenReturn("#main");
        handler.handle(connection, user, room, chatter, new Message(ImmutableMap.of(
            MessageProperty.TYPE, MessageType.BAN,
            MessageProperty.ROOM, "#main",
            MessageProperty.NAME, "username"
        )));
        verify(chatterService, never()).banChatter(room, otherChatter, chatter);
        verify(connection, times(1)).send(eq(Message.errorMessage("BAN_DENIED")));
    }

    @Test
    public void testExistingUserWithBadGlobalRole() {
        UserDto otherUserDto = new UserDto(1L, "username", GlobalRole.MOD, "#000000", false, false, null, false);
        User otherUser = new User(otherUserDto);
        Chatter otherChatter = new Chatter(1L, LocalRole.USER, false, null, otherUser);
        when(room.inRoom(connection)).thenReturn(true);
        when(room.getOnlineChatter(userDto)).thenReturn(chatter);
        when(room.getChatter("username")).thenReturn(otherChatter);
        when(room.getName()).thenReturn("#main");
        handler.handle(connection, user, room, chatter, new Message(ImmutableMap.of(
            MessageProperty.TYPE, MessageType.BAN,
            MessageProperty.ROOM, "#main",
            MessageProperty.NAME, "username"
        )));
        verify(chatterService, never()).banChatter(room, otherChatter, chatter);
        verify(connection, times(1)).send(eq(Message.errorMessage("BAN_DENIED")));
    }

    @Test
    public void testNotExistingUser() {
        when(room.inRoom(connection)).thenReturn(true);
        when(room.getOnlineChatter(userDto)).thenReturn(chatter);
        when(room.getChatter("username")).thenReturn(null);
        when(room.getName()).thenReturn("#main");
        handler.handle(connection, user, room, chatter, new Message(ImmutableMap.of(
            MessageProperty.TYPE, MessageType.BAN,
            MessageProperty.ROOM, "#main",
            MessageProperty.NAME, "username"
        )));
        verify(chatterService, never()).banChatter(eq(room), any(Chatter.class), any(Chatter.class));
        verify(connection, times(1)).send(eq(Message.errorMessage("UNKNOWN_USER")));
    }
}