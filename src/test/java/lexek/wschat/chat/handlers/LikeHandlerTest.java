package lexek.wschat.chat.handlers;

import com.google.common.collect.ImmutableList;
import lexek.wschat.chat.*;
import lexek.wschat.chat.Chatter;
import lexek.wschat.db.model.UserDto;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;

public class LikeHandlerTest {
    @Test
    public void shouldHaveLikeType() {
        LikeHandler handler = new LikeHandler(null, null);
        assertEquals(handler.getType(), MessageType.LIKE);
    }

    @Test
    public void shouldHaveSingleArgument() {
        LikeHandler handler = new LikeHandler(null, null);
        assertEquals(handler.getArgCount(), 2);
    }

    @Test
    public void shouldBeAvailableOnlyForUsers() {
        LikeHandler handler = new LikeHandler(null, null);
        assertEquals(handler.getRole(), GlobalRole.USER);
    }

    @Test
    public void shouldWork() {
        RoomManager roomManager = mock(RoomManager.class);
        Room room = mock(Room.class);
        when(roomManager.getRoomInstance("#main")).thenReturn(room);
        when(roomManager.getRoomInstance(0L)).thenReturn(room);
        MessageBroadcaster messageBroadcaster = mock(MessageBroadcaster.class);
        LikeHandler handler = new LikeHandler(messageBroadcaster, roomManager);

        UserDto userDto = new UserDto(0L, "user", GlobalRole.USER, "#ffffff", false, false, null, false);
        User user = new User(userDto);
        Connection connection = spy(new TestConnection(user));
        Chatter chatter = new Chatter(0L, LocalRole.USER, false, null, user);

        when(room.inRoom(connection)).thenReturn(true);
        when(room.join(connection)).thenReturn(chatter);
        when(room.getName()).thenReturn("#main");
        when(room.getChatter(user.getId())).thenReturn(chatter);
        when(room.getChatter(user.getName())).thenReturn(chatter);

        handler.handle(ImmutableList.of("#main", "0"), connection);

        verify(messageBroadcaster).submitMessage(eq(Message.likeMessage("#main", user.getName(), 0L)), eq(connection), eq(room.FILTER));
    }

}