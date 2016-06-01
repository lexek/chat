package lexek.wschat.chat.handlers;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import lexek.wschat.chat.Connection;
import lexek.wschat.chat.MessageBroadcaster;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.TestConnection;
import lexek.wschat.chat.model.*;
import lexek.wschat.db.model.UserDto;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class LikeHandlerTest {
    @Test
    public void shouldHaveLikeType() {
        LikeHandler handler = new LikeHandler(null);
        assertEquals(handler.getType(), MessageType.LIKE);
    }

    @Test
    public void shouldBeAvailableOnlyForUsers() {
        LikeHandler handler = new LikeHandler(null);
        assertEquals(handler.getRole(), LocalRole.USER);
    }

    @Test
    public void shouldHaveRequiredProperties() throws Exception {
        LikeHandler handler = new LikeHandler(null);
        assertEquals(
            handler.requiredProperties(),
            ImmutableSet.of(MessageProperty.ROOM, MessageProperty.MESSAGE_ID)
        );
    }

    @Test
    public void shouldRequireJoin() {
        LikeHandler handler = new LikeHandler(null);
        assertTrue(handler.joinRequired());
    }

    @Test
    public void shouldRequireTimeout() {
        LikeHandler handler = new LikeHandler(null);
        assertTrue(handler.isNeedsInterval());
    }

    @Test
    public void shouldWork() {
        Room room = mock(Room.class);
        MessageBroadcaster messageBroadcaster = mock(MessageBroadcaster.class);
        LikeHandler handler = new LikeHandler(messageBroadcaster);

        UserDto userDto = new UserDto(0L, "user", GlobalRole.USER, "#ffffff", false, false, null, false, false);
        User user = new User(userDto);
        Connection connection = spy(new TestConnection(user));
        Chatter chatter = new Chatter(0L, LocalRole.USER, false, null, user);

        when(room.inRoom(connection)).thenReturn(true);
        when(room.join(connection)).thenReturn(chatter);
        when(room.getName()).thenReturn("#main");
        when(room.getOnlineChatter(userDto)).thenReturn(chatter);
        when(room.getOnlineChatterByName(user.getName())).thenReturn(chatter);

        handler.handle(connection, user, room, chatter, new Message(ImmutableMap.of(
            MessageProperty.TYPE, MessageType.LIKE,
            MessageProperty.MESSAGE_ID, 0L
        )));

        verify(messageBroadcaster).submitMessage(eq(Message.likeMessage("#main", user.getName(), 0L)), eq(room.FILTER));
    }

}