package lexek.wschat.chat.handlers;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import lexek.wschat.chat.*;
import lexek.wschat.chat.model.*;
import lexek.wschat.db.model.UserDto;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ClearRoomHandlerTest {
    private UserDto userDto = new UserDto(0L, "user", GlobalRole.MOD, "#000000", false, false, null, false);
    private User user = new User(userDto);
    private Chatter chatter = new Chatter(0L, LocalRole.MOD, false, null, user);
    private Connection connection = spy(new TestConnection(user));
    private MessageBroadcaster messageBroadcaster = mock(MessageBroadcaster.class);
    private Room room = mock(Room.class);
    private ClearRoomHandler handler = new ClearRoomHandler(messageBroadcaster);

    public ClearRoomHandlerTest() {
        room.join(connection);
    }

    @Before
    public void setUp() throws Exception {
        reset(messageBroadcaster, room, connection);
    }

    @Test
    public void testGetType() throws Exception {
        assertEquals(handler.getType(), MessageType.CLEAR_ROOM);
    }

    @Test
    public void testGetRole() throws Exception {
        assertEquals(handler.getRole(), LocalRole.MOD);
    }

    @Test
    public void shouldHaveRequiredProperties() throws Exception {
        assertEquals(
            handler.requiredProperties(),
            ImmutableSet.of(MessageProperty.ROOM)
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
        when(room.inRoom(connection)).thenReturn(true);
        when(room.getOnlineChatter(userDto)).thenReturn(chatter);
        when(room.getName()).thenReturn("#main");
        handler.handle(connection, user, room, chatter, new Message(ImmutableMap.of(
            MessageProperty.TYPE, MessageType.CLEAR_ROOM,
            MessageProperty.ROOM, "#main"
        )));
        verify(messageBroadcaster, times(1)).submitMessage(
            eq(Message.clearMessage("#main")),
            eq(connection),
            eq(room.FILTER));
    }
}