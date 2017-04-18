package lexek.wschat.chat.handlers;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import lexek.wschat.chat.*;
import lexek.wschat.chat.filters.UserInRoomFilter;
import lexek.wschat.chat.model.*;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.services.poll.PollService;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class VoteHandlerTest {
    private UserDto userDto = new UserDto(0L, "user", GlobalRole.MOD, "#000000", false, false, null, false, false);
    private User user = new CachedUser(userDto, cache);
    private Chatter chatter = new Chatter(0L, LocalRole.USER, false, null, user);
    private Connection connection = spy(new TestConnection(user));
    private RoomManager roomManager = mock(RoomManager.class);
    private Room room = mock(Room.class);
    private PollService pollService = mock(PollService.class);
    private MessageBroadcaster messageBroadcaster = mock(MessageBroadcaster.class);
    private VoteHandler handler = new VoteHandler(pollService, messageBroadcaster);

    @Before
    public void resetMocks() {
        reset(connection, roomManager, room, pollService, messageBroadcaster);
    }

    @Test
    public void testGetType() throws Exception {
        assertEquals(handler.getType(), MessageType.POLL_VOTE);
    }

    @Test
    public void testGetRole() throws Exception {
        assertEquals(handler.getRole(), LocalRole.USER);
    }

    @Test
    public void testIsNeedsInterval() throws Exception {
        assertEquals(handler.isNeedsInterval(), true);
    }

    @Test
    public void shouldHaveRequiredProperties() throws Exception {
        assertEquals(
            handler.requiredProperties(),
            ImmutableSet.of(MessageProperty.ROOM, MessageProperty.POLL_OPTION)
        );
    }

    @Test
    public void shouldRequireJoin() {
        assertTrue(handler.joinRequired());
    }

    @Test
    public void shouldRequireTimeout() {
        assertTrue(handler.isNeedsInterval());
    }

    @Test
    public void testGoodScenario() {
        when(roomManager.getRoomInstance("#main")).thenReturn(room);
        when(room.inRoom(connection)).thenReturn(true);
        when(room.getName()).thenReturn("#main");
        handler.handle(connection, user, room, chatter, new Message(ImmutableMap.of(
            MessageProperty.TYPE, MessageType.POLL_VOTE,
            MessageProperty.ROOM, "#main",
            MessageProperty.POLL_OPTION, 0
        )));
        verify(pollService).vote(room, user, 0);
        verifyZeroInteractions(pollService);
        verify(messageBroadcaster, times(1))
            .submitMessage(
                eq(Message.pollVotedMessage("#main")),
                eq(new UserInRoomFilter(user, room))
            );
    }

    @Test
    public void testBadOption() {
        when(roomManager.getRoomInstance("#main")).thenReturn(room);
        when(room.inRoom(connection)).thenReturn(true);
        when(room.getName()).thenReturn("#main");
        handler.handle(connection, user, room, chatter, new Message(ImmutableMap.of(
            MessageProperty.TYPE, MessageType.POLL_VOTE,
            MessageProperty.ROOM, "#main",
            MessageProperty.POLL_OPTION, -1
        )));
        verify(pollService, never()).vote(room, user, 0);
        verifyZeroInteractions(pollService);
        verify(connection, times(1)).send(eq(Message.errorMessage("BAD_ARG")));
        verify(messageBroadcaster, never())
            .submitMessage(
                eq(Message.pollVotedMessage("#main")),
                eq(new UserInRoomFilter(user, room))
            );
    }
}