package lexek.wschat.chat.handlers;

import com.google.common.collect.ImmutableList;
import lexek.wschat.chat.*;
import lexek.wschat.chat.Chatter;
import lexek.wschat.db.model.UserDto;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static lexek.wschat.chat.TextMessageMatcher.textMessage;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;

public class MsgHandlerTest {
    @Test
    public void shouldHaveLikeType() {
        MsgHandler handler = new MsgHandler(null, null, null);
        assertEquals(handler.getType(), MessageType.MSG);
    }

    @Test
    public void shouldHaveSingleArgument() {
        MsgHandler handler = new MsgHandler(null, null, null);
        assertEquals(handler.getArgCount(), 2);
    }

    @Test
    public void shouldBeAvailableOnlyForUsers() {
        MsgHandler handler = new MsgHandler(null, null, null);
        assertEquals(handler.getRole(), GlobalRole.USER);
    }

    @Test
    public void shouldWork() {
        RoomManager roomManager = mock(RoomManager.class);
        Room room = mock(Room.class);
        when(roomManager.getRoomInstance("#main")).thenReturn(room);
        when(roomManager.getRoomInstance(0L)).thenReturn(room);
        MessageBroadcaster messageBroadcaster = mock(MessageBroadcaster.class);
        MsgHandler handler = new MsgHandler(new AtomicLong(), messageBroadcaster, roomManager);

        UserDto userDto = new UserDto(0L, "user", GlobalRole.USER, "#ffffff", false, false, null, false);
        User user = new User(userDto);
        Connection connection = spy(new TestConnection(user));
        Chatter chatter = new Chatter(0L, LocalRole.USER, false, null, user);

        when(room.inRoom(connection)).thenReturn(true);
        when(room.join(connection)).thenReturn(chatter);
        when(room.getName()).thenReturn("#main");
        when(room.getChatter(user.getId())).thenReturn(chatter);
        when(room.getChatter(user.getName())).thenReturn(chatter);

        handler.handle(ImmutableList.of("#main", "top kek"), connection);

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
        RoomManager roomManager = mock(RoomManager.class);
        Room room = mock(Room.class);
        when(roomManager.getRoomInstance("#main")).thenReturn(room);
        when(roomManager.getRoomInstance(0L)).thenReturn(room);
        MessageBroadcaster messageBroadcaster = mock(MessageBroadcaster.class);
        MsgHandler handler = new MsgHandler(new AtomicLong(), messageBroadcaster, roomManager);

        UserDto userDto = new UserDto(0L, "user", GlobalRole.USER, "#ffffff", false, false, null, false);
        User user = new User(userDto);
        Connection connection = spy(new TestConnection(user));
        Chatter chatter = new Chatter(0L, LocalRole.USER, false, null, user);

        when(room.inRoom(connection)).thenReturn(true);
        when(room.join(connection)).thenReturn(chatter);
        when(room.getName()).thenReturn("#main");
        when(room.getChatter(user.getId())).thenReturn(chatter);
        when(room.getChatter(user.getName())).thenReturn(chatter);

        char[] array = new char[421];
        Arrays.fill(array, 'a');
        String s = new String(array);
        handler.handle(ImmutableList.of("#main", s), connection);

        verify(messageBroadcaster, never()).submitMessage(
            any(Message.class),
            eq(connection),
            eq(room.FILTER));
        verify(connection).send(eq(Message.errorMessage("MESSAGE_TOO_BIG")));
    }

    @Test
    public void shouldNotHaveErrorForLongModMessage() {
        RoomManager roomManager = mock(RoomManager.class);
        Room room = mock(Room.class);
        when(roomManager.getRoomInstance("#main")).thenReturn(room);
        when(roomManager.getRoomInstance(0L)).thenReturn(room);
        MessageBroadcaster messageBroadcaster = mock(MessageBroadcaster.class);
        MsgHandler handler = new MsgHandler(new AtomicLong(), messageBroadcaster, roomManager);

        UserDto userDto = new UserDto(0L, "user", GlobalRole.MOD, "#ffffff", false, false, null, false);
        User user = new User(userDto);
        Connection connection = spy(new TestConnection(user));
        Chatter chatter = new Chatter(0L, LocalRole.USER, false, null, user);

        when(room.inRoom(connection)).thenReturn(true);
        when(room.join(connection)).thenReturn(chatter);
        when(room.getName()).thenReturn("#main");
        when(room.getChatter(user.getId())).thenReturn(chatter);
        when(room.getChatter(user.getName())).thenReturn(chatter);

        char[] array = new char[421];
        Arrays.fill(array, 'a');
        String s = new String(array);
        handler.handle(ImmutableList.of("#main", s), connection);

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
        RoomManager roomManager = mock(RoomManager.class);
        Room room = mock(Room.class);
        when(roomManager.getRoomInstance("#main")).thenReturn(room);
        when(roomManager.getRoomInstance(0L)).thenReturn(room);
        MessageBroadcaster messageBroadcaster = mock(MessageBroadcaster.class);
        MsgHandler handler = new MsgHandler(new AtomicLong(), messageBroadcaster, roomManager);

        UserDto userDto = new UserDto(0L, "user", GlobalRole.USER, "#ffffff", false, false, null, false);
        User user = new User(userDto);
        Connection connection = spy(new TestConnection(user));
        Chatter chatter = new Chatter(0L, LocalRole.USER, false, null, user);

        when(room.inRoom(connection)).thenReturn(true);
        when(room.join(connection)).thenReturn(chatter);
        when(room.getName()).thenReturn("#main");
        when(room.getChatter(user.getId())).thenReturn(chatter);
        when(room.getChatter(user.getName())).thenReturn(chatter);

        handler.handle(ImmutableList.of("#main", ""), connection);

        verify(messageBroadcaster, never()).submitMessage(
            any(Message.class),
            eq(connection),
            eq(room.FILTER));
        verify(connection).send(eq(Message.errorMessage("EMPTY_MESSAGE")));
    }

    @Test
    public void shouldHaveErrorForSpaceOnlyMessage() {
        RoomManager roomManager = mock(RoomManager.class);
        Room room = mock(Room.class);
        when(roomManager.getRoomInstance("#main")).thenReturn(room);
        when(roomManager.getRoomInstance(0L)).thenReturn(room);
        MessageBroadcaster messageBroadcaster = mock(MessageBroadcaster.class);
        MsgHandler handler = new MsgHandler(new AtomicLong(), messageBroadcaster, roomManager);

        UserDto userDto = new UserDto(0L, "user", GlobalRole.USER, "#ffffff", false, false, null, false);
        User user = new User(userDto);
        Connection connection = spy(new TestConnection(user));
        Chatter chatter = new Chatter(0L, LocalRole.USER, false, null, user);

        when(room.inRoom(connection)).thenReturn(true);
        when(room.join(connection)).thenReturn(chatter);
        when(room.getName()).thenReturn("#main");
        when(room.getChatter(user.getId())).thenReturn(chatter);
        when(room.getChatter(user.getName())).thenReturn(chatter);

        handler.handle(ImmutableList.of("#main", "      \r\n"), connection);

        verify(messageBroadcaster, never()).submitMessage(
            any(Message.class),
            eq(connection),
            eq(room.FILTER));
        verify(connection).send(eq(Message.errorMessage("EMPTY_MESSAGE")));
    }

    @Test
    public void shouldHaveErrorForGlobalBannedUser() {
        RoomManager roomManager = mock(RoomManager.class);
        Room room = mock(Room.class);
        when(roomManager.getRoomInstance("#main")).thenReturn(room);
        when(roomManager.getRoomInstance(0L)).thenReturn(room);
        MessageBroadcaster messageBroadcaster = mock(MessageBroadcaster.class);
        MsgHandler handler = new MsgHandler(new AtomicLong(), messageBroadcaster, roomManager);

        UserDto userDto = new UserDto(0L, "user", GlobalRole.USER, "#ffffff", true, false, null, false);
        User user = new User(userDto);
        Connection connection = spy(new TestConnection(user));
        Chatter chatter = new Chatter(0L, LocalRole.USER, false, null, user);

        when(room.inRoom(connection)).thenReturn(true);
        when(room.join(connection)).thenReturn(chatter);
        when(room.getName()).thenReturn("#main");
        when(room.getChatter(user.getId())).thenReturn(chatter);
        when(room.getChatter(user.getName())).thenReturn(chatter);

        handler.handle(ImmutableList.of("#main", "asd"), connection);

        verify(messageBroadcaster, never()).submitMessage(
            any(Message.class),
            eq(connection),
            eq(room.FILTER));
        verify(connection).send(eq(Message.errorMessage("BAN")));
    }

    @Test
    public void shouldHaveErrorForLocalBannedUser() {
        RoomManager roomManager = mock(RoomManager.class);
        Room room = mock(Room.class);
        when(roomManager.getRoomInstance("#main")).thenReturn(room);
        when(roomManager.getRoomInstance(0L)).thenReturn(room);
        MessageBroadcaster messageBroadcaster = mock(MessageBroadcaster.class);
        MsgHandler handler = new MsgHandler(new AtomicLong(), messageBroadcaster, roomManager);

        UserDto userDto = new UserDto(0L, "user", GlobalRole.USER, "#ffffff", false, false, null, false);
        User user = new User(userDto);
        Connection connection = spy(new TestConnection(user));
        Chatter chatter = new Chatter(0L, LocalRole.USER, true, null, user);

        when(room.inRoom(connection)).thenReturn(true);
        when(room.join(connection)).thenReturn(chatter);
        when(room.getName()).thenReturn("#main");
        when(room.getChatter(user.getId())).thenReturn(chatter);
        when(room.getChatter(user.getName())).thenReturn(chatter);

        handler.handle(ImmutableList.of("#main", "asd"), connection);

        verify(messageBroadcaster, never()).submitMessage(
            any(Message.class),
            eq(connection),
            eq(room.FILTER));
        verify(connection).send(eq(Message.errorMessage("BAN")));
    }

    @Test
    public void shouldHaveErrorForTimedOutUser() {
        RoomManager roomManager = mock(RoomManager.class);
        Room room = mock(Room.class);
        when(roomManager.getRoomInstance("#main")).thenReturn(room);
        when(roomManager.getRoomInstance(0L)).thenReturn(room);
        MessageBroadcaster messageBroadcaster = mock(MessageBroadcaster.class);
        MsgHandler handler = new MsgHandler(new AtomicLong(), messageBroadcaster, roomManager);

        UserDto userDto = new UserDto(0L, "user", GlobalRole.USER, "#ffffff", false, false, null, false);
        User user = new User(userDto);
        Connection connection = spy(new TestConnection(user));
        Chatter chatter = new Chatter(0L,
            LocalRole.USER,
            false,
            System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1337),
            user);

        when(room.inRoom(connection)).thenReturn(true);
        when(room.join(connection)).thenReturn(chatter);
        when(room.getName()).thenReturn("#main");
        when(room.getChatter(user.getId())).thenReturn(chatter);
        when(room.getChatter(user.getName())).thenReturn(chatter);

        handler.handle(ImmutableList.of("#main", "asd"), connection);

        verify(messageBroadcaster, never()).submitMessage(
            any(Message.class),
            eq(connection),
            eq(room.FILTER));
        verify(connection).send(argThat(
            new ArgumentMatcher<Message>() {
                @Override
                public boolean matches(Object argument) {
                    return argument instanceof Message &&
                        ((Message) argument).get(Message.Keys.TYPE).equals(MessageType.ERROR) &&
                        ((Message) argument).get(Message.Keys.TEXT).equals("TIMEOUT");
                }
            }
        ));
    }

    @Test
    public void shouldRemoveExpiredTimeout() {
        RoomManager roomManager = mock(RoomManager.class);
        Room room = mock(Room.class);
        when(roomManager.getRoomInstance("#main")).thenReturn(room);
        when(roomManager.getRoomInstance(0L)).thenReturn(room);
        MessageBroadcaster messageBroadcaster = mock(MessageBroadcaster.class);
        MsgHandler handler = new MsgHandler(new AtomicLong(), messageBroadcaster, roomManager);

        UserDto userDto = new UserDto(0L, "user", GlobalRole.USER, "#ffffff", false, false, null, false);
        User user = new User(userDto);
        Connection connection = spy(new TestConnection(user));
        Chatter chatter = new Chatter(0L,
            LocalRole.USER,
            false,
            System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1337),
            user);

        when(room.inRoom(connection)).thenReturn(true);
        when(room.join(connection)).thenReturn(chatter);
        when(room.getName()).thenReturn("#main");
        when(room.getChatter(user.getId())).thenReturn(chatter);
        when(room.getChatter(user.getName())).thenReturn(chatter);
        doAnswer(invocation -> {
            ((Chatter) invocation.getArguments()[0]).setTimeout(null);
            return null;
        }).when(room).removeTimeout(chatter);

        handler.handle(ImmutableList.of("#main", "top kek"), connection);

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
        verify(room).removeTimeout(chatter);
    }
}