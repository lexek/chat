package lexek.wschat.chat.handlers;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import lexek.wschat.chat.*;
import lexek.wschat.chat.model.*;
import lexek.wschat.db.model.UserDto;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

import static lexek.wschat.chat.TextMessageMatcher.textMessage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class MsgHandlerTest {
    @Test
    public void shouldHaveLikeType() {
        MsgHandler handler = new MsgHandler(null, null);
        assertEquals(handler.getType(), MessageType.MSG);
    }

    @Test
    public void shouldBeAvailableOnlyForUsers() {
        MsgHandler handler = new MsgHandler(null, null);
        assertEquals(handler.getRole(), LocalRole.USER);
    }

    @Test
    public void shouldHaveRequiredProperties() throws Exception {
        MsgHandler handler = new MsgHandler(null, null);
        assertEquals(
            handler.requiredProperties(),
            ImmutableSet.of(MessageProperty.ROOM, MessageProperty.TEXT)
        );
    }

    @Test
    public void shouldRequireJoin() {
        MsgHandler handler = new MsgHandler(null, null);
        assertTrue(handler.joinRequired());
    }

    @Test
    public void shouldRequireTimeout() {
        MsgHandler handler = new MsgHandler(null, null);
        assertTrue(handler.isNeedsInterval());
    }

    @Test
    public void shouldWork() {
        Room room = mock(Room.class);
        MessageBroadcaster messageBroadcaster = mock(MessageBroadcaster.class);
        MsgHandler handler = new MsgHandler(new AtomicLong(), messageBroadcaster);

        UserDto userDto = new UserDto(0L, "user", GlobalRole.USER, "#ffffff", false, false, null, false);
        User user = new User(userDto);
        Connection connection = spy(new TestConnection(user));
        Chatter chatter = new Chatter(0L, LocalRole.USER, false, null, user);

        when(room.inRoom(connection)).thenReturn(true);
        when(room.join(connection)).thenReturn(chatter);
        when(room.getName()).thenReturn("#main");
        when(room.getOnlineChatter(userDto)).thenReturn(chatter);
        when(room.getOnlineChatterByName(user.getName())).thenReturn(chatter);

        handler.handle(connection, user, room, chatter, new Message(ImmutableMap.of(
            MessageProperty.TYPE, MessageType.MSG,
            MessageProperty.ROOM, "#main",
            MessageProperty.TEXT, "top kek"
        )));

        verify(messageBroadcaster).submitMessage(
            argThat(textMessage(Message.msgMessage(
                "#main",
                "user",
                LocalRole.USER,
                GlobalRole.USER,
                "#ffffff",
                0L,
                0L,
                "top kek"))),
            eq(connection),
            eq(room.FILTER));
    }

    @Test
    public void shouldHaveErrorForLongUserMessage() {
        Room room = mock(Room.class);
        MessageBroadcaster messageBroadcaster = mock(MessageBroadcaster.class);
        MsgHandler handler = new MsgHandler(new AtomicLong(), messageBroadcaster);

        UserDto userDto = new UserDto(0L, "user", GlobalRole.USER, "#ffffff", false, false, null, false);
        User user = new User(userDto);
        Connection connection = spy(new TestConnection(user));
        Chatter chatter = new Chatter(0L, LocalRole.USER, false, null, user);

        when(room.inRoom(connection)).thenReturn(true);
        when(room.join(connection)).thenReturn(chatter);
        when(room.getName()).thenReturn("#main");
        when(room.getOnlineChatter(userDto)).thenReturn(chatter);
        when(room.getOnlineChatterByName(user.getName())).thenReturn(chatter);

        char[] array = new char[421];
        Arrays.fill(array, 'a');
        String s = new String(array);
        handler.handle(connection, user, room, chatter, new Message(ImmutableMap.of(
            MessageProperty.TYPE, MessageType.MSG,
            MessageProperty.ROOM, "#main",
            MessageProperty.TEXT, s
        )));

        verify(messageBroadcaster, never()).submitMessage(
            any(Message.class),
            eq(connection),
            eq(room.FILTER));
        verify(connection).send(eq(Message.errorMessage("MESSAGE_TOO_BIG")));
    }

    @Test
    public void shouldNotHaveErrorForLongModMessage() {
        Room room = mock(Room.class);
        MessageBroadcaster messageBroadcaster = mock(MessageBroadcaster.class);
        MsgHandler handler = new MsgHandler(new AtomicLong(), messageBroadcaster);

        UserDto userDto = new UserDto(0L, "user", GlobalRole.MOD, "#ffffff", false, false, null, false);
        User user = new User(userDto);
        Connection connection = spy(new TestConnection(user));
        Chatter chatter = new Chatter(0L, LocalRole.USER, false, null, user);

        when(room.inRoom(connection)).thenReturn(true);
        when(room.join(connection)).thenReturn(chatter);
        when(room.getName()).thenReturn("#main");
        when(room.getOnlineChatter(userDto)).thenReturn(chatter);
        when(room.getOnlineChatterByName(user.getName())).thenReturn(chatter);

        char[] array = new char[421];
        Arrays.fill(array, 'a');
        String s = new String(array);
        handler.handle(connection, user, room, chatter, new Message(ImmutableMap.of(
            MessageProperty.TYPE, MessageType.MSG,
            MessageProperty.ROOM, "#main",
            MessageProperty.TEXT, s
        )));

        verify(messageBroadcaster).submitMessage(
            argThat(textMessage(Message.msgMessage(
                "#main",
                "user",
                LocalRole.MOD,
                GlobalRole.MOD,
                "#ffffff",
                0L,
                0L,
                s))),
            eq(connection),
            eq(room.FILTER));
    }

    @Test
    public void shouldHaveErrorForEmptyMessage() {
        Room room = mock(Room.class);
        MessageBroadcaster messageBroadcaster = mock(MessageBroadcaster.class);
        MsgHandler handler = new MsgHandler(new AtomicLong(), messageBroadcaster);

        UserDto userDto = new UserDto(0L, "user", GlobalRole.USER, "#ffffff", false, false, null, false);
        User user = new User(userDto);
        Connection connection = spy(new TestConnection(user));
        Chatter chatter = new Chatter(0L, LocalRole.USER, false, null, user);

        when(room.inRoom(connection)).thenReturn(true);
        when(room.join(connection)).thenReturn(chatter);
        when(room.getName()).thenReturn("#main");
        when(room.getOnlineChatter(userDto)).thenReturn(chatter);
        when(room.getOnlineChatterByName(user.getName())).thenReturn(chatter);

        handler.handle(connection, user, room, chatter, new Message(ImmutableMap.of(
            MessageProperty.TYPE, MessageType.MSG,
            MessageProperty.ROOM, "#main",
            MessageProperty.TEXT, ""
        )));

        verify(messageBroadcaster, never()).submitMessage(
            any(Message.class),
            eq(connection),
            eq(room.FILTER));
        verify(connection).send(eq(Message.errorMessage("EMPTY_MESSAGE")));
    }

    @Test
    public void shouldHaveErrorForSpaceOnlyMessage() {
        Room room = mock(Room.class);
        MessageBroadcaster messageBroadcaster = mock(MessageBroadcaster.class);
        MsgHandler handler = new MsgHandler(new AtomicLong(), messageBroadcaster);

        UserDto userDto = new UserDto(0L, "user", GlobalRole.USER, "#ffffff", false, false, null, false);
        User user = new User(userDto);
        Connection connection = spy(new TestConnection(user));
        Chatter chatter = new Chatter(0L, LocalRole.USER, false, null, user);

        when(room.inRoom(connection)).thenReturn(true);
        when(room.join(connection)).thenReturn(chatter);
        when(room.getName()).thenReturn("#main");
        when(room.getOnlineChatter(userDto)).thenReturn(chatter);
        when(room.getOnlineChatterByName(user.getName())).thenReturn(chatter);

        handler.handle(connection, user, room, chatter, new Message(ImmutableMap.of(
            MessageProperty.TYPE, MessageType.MSG,
            MessageProperty.ROOM, "#main",
            MessageProperty.TEXT, "      \r\n"
        )));


        verify(messageBroadcaster, never()).submitMessage(
            any(Message.class),
            eq(connection),
            eq(room.FILTER));
        verify(connection).send(eq(Message.errorMessage("EMPTY_MESSAGE")));
    }
}