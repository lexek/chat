package lexek.wschat.chat.handlers;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import lexek.wschat.chat.*;
import lexek.wschat.db.model.UserDto;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class UnbanHandlerTest {
    private UserDto userDto = new UserDto(0L, "user", GlobalRole.MOD, "#000000", false, false, null, false);
    private User user = new User(userDto);
    private Chatter chatter = new Chatter(0L, LocalRole.MOD, false, null, user);
    private Connection connection = spy(new TestConnection(user));
    private Room room = mock(Room.class);
    private UnbanHandler handler = new UnbanHandler();

    @Before
    public void resetMocks() {
        reset(connection, room);
    }

    @Test
    public void testGetType() throws Exception {
        assertEquals(handler.getType(), MessageType.UNBAN);
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
    public void testExistingUserWithGoodRole() {
        UserDto otherUserDto = new UserDto(1L, "username", GlobalRole.USER, "#000000", false, false, null, false);
        User otherUser = new User(otherUserDto);
        Chatter otherChatter = new Chatter(1L, LocalRole.USER, false, null, otherUser);
        when(room.inRoom(connection)).thenReturn(true);
        when(room.getOnlineChatter(userDto)).thenReturn(chatter);
        when(room.getChatter("username")).thenReturn(otherChatter);
        when(room.getName()).thenReturn("#main");
        when(room.unbanChatter(otherChatter, chatter)).thenReturn(true);
        handler.handle(connection, user, room, chatter, new Message(ImmutableMap.of(
            MessageProperty.TYPE, MessageType.UNBAN,
            MessageProperty.ROOM, "#main",
            MessageProperty.NAME, "username"
        )));
        verify(room).unbanChatter(otherChatter, chatter);
        verify(connection).send(Message.infoMessage("OK"));
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
        when(room.unbanChatter(otherChatter, chatter)).thenReturn(false);
        handler.handle(connection, user, room, chatter, new Message(ImmutableMap.of(
            MessageProperty.TYPE, MessageType.UNBAN,
            MessageProperty.ROOM, "#main",
            MessageProperty.NAME, "username"
        )));
        verify(room).unbanChatter(otherChatter, chatter);
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
            MessageProperty.TYPE, MessageType.UNBAN,
            MessageProperty.ROOM, "#main",
            MessageProperty.NAME, "username"
        )));
        verify(room, never()).unbanChatter(otherChatter, chatter);
        verify(connection, times(1)).send(eq(Message.errorMessage("UNBAN_DENIED")));
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
            MessageProperty.TYPE, MessageType.UNBAN,
            MessageProperty.ROOM, "#main",
            MessageProperty.NAME, "username"
        )));
        verify(room, never()).unbanChatter(otherChatter, chatter);
        verify(connection, times(1)).send(eq(Message.errorMessage("UNBAN_DENIED")));
    }

    @Test
    public void testNotExistingUser() {
        when(room.inRoom(connection)).thenReturn(true);
        when(room.getOnlineChatter(userDto)).thenReturn(chatter);
        when(room.getChatter("username")).thenReturn(null);
        when(room.getName()).thenReturn("#main");
        handler.handle(connection, user, room, chatter, new Message(ImmutableMap.of(
            MessageProperty.TYPE, MessageType.UNBAN,
            MessageProperty.ROOM, "#main",
            MessageProperty.NAME, "username"
        )));
        verify(room, never()).unbanChatter(any(Chatter.class), any(Chatter.class));
        verify(connection, times(1)).send(eq(Message.errorMessage("UNKNOWN_USER")));
    }
}