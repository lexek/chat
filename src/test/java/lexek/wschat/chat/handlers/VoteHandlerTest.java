package lexek.wschat.chat.handlers;

import com.google.common.collect.ImmutableList;
import lexek.wschat.chat.*;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.services.poll.PollService;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class VoteHandlerTest {
    private UserDto userDto = new UserDto(0L, "user", GlobalRole.MOD, "#000000", false, false, null);
    private User user = new User(userDto);
    private Connection connection = spy(new TestConnection(user));
    private RoomManager roomManager = mock(RoomManager.class);
    private Room room = mock(Room.class);
    private PollService pollService = mock(PollService.class);
    private VoteHandler handler = new VoteHandler(roomManager, pollService);

    @Before
    public void resetMocks() {
        reset(connection, roomManager, room, pollService);
    }

    @Test
    public void testGetType() throws Exception {
        assertEquals(handler.getType(), MessageType.POLL_VOTE);
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
    public void testIsNeedsInterval() throws Exception {
        assertEquals(handler.isNeedsInterval(), true);
    }

    @Test
    public void testIsNeedsLogging() throws Exception {
        assertEquals(handler.isNeedsLogging(), false);
    }

    @Test
    public void testGoodScenario() {
        when(roomManager.getRoomInstance("#main")).thenReturn(room);
        when(room.contains(connection)).thenReturn(true);
        when(room.getName()).thenReturn("#main");
        when(pollService.vote(room, user, 0)).thenReturn(true);
        handler.handle(ImmutableList.of("#main", "0"), connection);
        verify(pollService).vote(room, user, 0);
        verifyZeroInteractions(pollService);
        verify(connection, times(1)).send(eq(Message.pollVotedMessage("#main")));
    }

    @Test
    public void testBadOption() {
        when(roomManager.getRoomInstance("#main")).thenReturn(room);
        when(room.contains(connection)).thenReturn(true);
        when(room.getName()).thenReturn("#main");
        when(pollService.vote(room, user, 0)).thenReturn(true);
        handler.handle(ImmutableList.of("#main", "-1"), connection);
        verify(pollService, never()).vote(room, user, 0);
        verifyZeroInteractions(pollService);
        verify(connection, times(1)).send(eq(Message.errorMessage("BAD_ARG")));
    }

    @Test
    public void testNotJoined() {
        when(roomManager.getRoomInstance("#main")).thenReturn(room);
        when(room.contains(connection)).thenReturn(false);
        handler.handle(ImmutableList.of("#main", "username"), connection);
        verifyZeroInteractions(pollService);
        verify(connection, times(1)).send(eq(Message.errorMessage("NOT_JOINED")));
    }

    @Test
    public void testBadRoom() {
        when(roomManager.getRoomInstance("#main")).thenReturn(null);
        handler.handle(ImmutableList.of("#main", "username"), connection);
        verifyZeroInteractions(pollService);
        verify(connection, times(1)).send(eq(Message.errorMessage("UNKNOWN_ROOM")));
    }

}