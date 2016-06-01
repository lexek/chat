package lexek.wschat.chat.handlers;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import lexek.wschat.chat.Connection;
import lexek.wschat.chat.MessageBroadcaster;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.TestConnection;
import lexek.wschat.chat.model.*;
import lexek.wschat.db.model.UserDto;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class ClearUserHandlerTest {
    private UserDto userDto = new UserDto(0L, "user", GlobalRole.MOD, "#000000", false, false, null, false, false);
    private User user = new User(userDto);
    private Chatter chatter = new Chatter(0L, LocalRole.MOD, false, null, user);
    private Connection connection = spy(new TestConnection(user));
    private MessageBroadcaster messageBroadcaster = mock(MessageBroadcaster.class);
    private Room room = mock(Room.class);
    private ClearUserHandler handler = new ClearUserHandler(messageBroadcaster);

    @Before
    public void before() {
        reset(messageBroadcaster, connection, room);
    }

    @Test
    public void testGetType() throws Exception {
        assertEquals(handler.getType(), MessageType.CLEAR);
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
        UserDto otherUserDto = new UserDto(1L, "username", GlobalRole.USER, "#000000", false, false, null, false, false);
        User otherUser = new User(otherUserDto);
        Chatter otherChatter = new Chatter(1L, LocalRole.USER, false, null, otherUser);
        when(room.inRoom(connection)).thenReturn(true);
        when(room.getOnlineChatter(userDto)).thenReturn(chatter);
        when(room.getOnlineChatterByName("username")).thenReturn(otherChatter);
        when(room.getName()).thenReturn("#main");
        handler.handle(connection, user, room, chatter, new Message(ImmutableMap.of(
            MessageProperty.TYPE, MessageType.CLEAR,
            MessageProperty.ROOM, "#main",
            MessageProperty.NAME, "username"
        )));
        verify(messageBroadcaster, times(1)).submitMessage(
            eq(Message.moderationMessage(MessageType.CLEAR, "#main", "user", "username")),
            eq(room.FILTER)
        );
    }

    @Test
    public void localOnlyAdminShouldBeAbleToClear() {
        UserDto userDto = new UserDto(0L, "user", GlobalRole.USER, "#000000", false, false, null, false, false);
        User user = new User(userDto);
        Chatter chatter = new Chatter(0L, LocalRole.MOD, false, null, user);
        Connection connection = spy(new TestConnection(user));
        UserDto otherUserDto = new UserDto(1L, "username", GlobalRole.USER, "#000000", false, false, null, false, false);
        User otherUser = new User(otherUserDto);
        Chatter otherChatter = new Chatter(1L, LocalRole.USER, false, null, otherUser);
        when(room.inRoom(connection)).thenReturn(true);
        when(room.getOnlineChatter(userDto)).thenReturn(chatter);
        when(room.getOnlineChatterByName("username")).thenReturn(otherChatter);
        when(room.getName()).thenReturn("#main");
        handler.handle(connection, user, room, chatter, new Message(ImmutableMap.of(
            MessageProperty.TYPE, MessageType.CLEAR,
            MessageProperty.ROOM, "#main",
            MessageProperty.NAME, "username"
        )));
        verify(messageBroadcaster, times(1)).submitMessage(
            eq(Message.moderationMessage(MessageType.CLEAR, "#main", "user", "username")),
            eq(room.FILTER)
        );
    }


    @Test
    public void testExistingUserWithBadLocalRole() {
        UserDto otherUserDto = new UserDto(1L, "username", GlobalRole.USER, "#000000", false, false, null, false, false);
        User otherUser = new User(otherUserDto);
        Chatter otherChatter = new Chatter(1L, LocalRole.ADMIN, false, null, otherUser);
        when(room.inRoom(connection)).thenReturn(true);
        when(room.getOnlineChatter(userDto)).thenReturn(chatter);
        when(room.getOnlineChatterByName("username")).thenReturn(otherChatter);
        when(room.getName()).thenReturn("#main");
        handler.handle(connection, user, room, chatter, new Message(ImmutableMap.of(
            MessageProperty.TYPE, MessageType.CLEAR,
            MessageProperty.ROOM, "#main",
            MessageProperty.NAME, "username"
        )));
        verifyZeroInteractions(messageBroadcaster);
        verify(connection, times(1)).send(eq(Message.errorMessage("CLEAR_DENIED")));
    }

    @Test
    public void testExistingUserWithBadGlobalRole() {
        UserDto otherUserDto = new UserDto(1L, "username", GlobalRole.MOD, "#000000", false, false, null, false, false);
        User otherUser = new User(otherUserDto);
        Chatter otherChatter = new Chatter(1L, LocalRole.USER, false, null, otherUser);
        when(room.inRoom(connection)).thenReturn(true);
        when(room.getOnlineChatter(userDto)).thenReturn(chatter);
        when(room.getOnlineChatterByName("username")).thenReturn(otherChatter);
        when(room.getName()).thenReturn("#main");
        handler.handle(connection, user, room, chatter, new Message(ImmutableMap.of(
            MessageProperty.TYPE, MessageType.CLEAR,
            MessageProperty.ROOM, "#main",
            MessageProperty.NAME, "username"
        )));
        verifyZeroInteractions(messageBroadcaster);
        verify(connection, times(1)).send(eq(Message.errorMessage("CLEAR_DENIED")));
    }

    @Test
    public void testNotExistingUser() {
        when(room.inRoom(connection)).thenReturn(true);
        when(room.getOnlineChatter(userDto)).thenReturn(chatter);
        when(room.getOnlineChatterByName("username")).thenReturn(null);
        when(room.getName()).thenReturn("#main");
        handler.handle(connection, user, room, chatter, new Message(ImmutableMap.of(
            MessageProperty.TYPE, MessageType.CLEAR,
            MessageProperty.ROOM, "#main",
            MessageProperty.NAME, "username"
        )));
        verifyZeroInteractions(messageBroadcaster);
        verify(connection, times(1)).send(eq(Message.errorMessage("UNKNOWN_USER")));
    }
}
