package lexek.wschat.chat.handlers;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import lexek.wschat.chat.Connection;
import lexek.wschat.chat.TestConnection;
import lexek.wschat.chat.e.EntityNotFoundException;
import lexek.wschat.chat.e.InvalidInputException;
import lexek.wschat.chat.e.LimitExceededException;
import lexek.wschat.chat.model.*;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.services.IgnoreService;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class IgnoreHandlerTest {
    @Test
    public void testGetType() throws Exception {
        IgnoreHandler handler = new IgnoreHandler(null);
        assertEquals(handler.getType(), MessageType.IGNORE);
    }

    @Test
    public void shouldHaveUserRole() throws Exception {
        IgnoreHandler handler = new IgnoreHandler(null);
        assertEquals(handler.getRole(), GlobalRole.USER);
    }

    @Test
    public void shouldHaveRequiredProperties() throws Exception {
        IgnoreHandler handler = new IgnoreHandler(null);
        assertEquals(
            handler.requiredProperties(),
            ImmutableSet.of(MessageProperty.NAME)
        );
    }

    @Test
    public void shouldRequireTimeout() {
        IgnoreHandler handler = new IgnoreHandler(null);
        assertTrue(handler.isNeedsInterval());
    }

    @Test
    public void shouldWork() {
        UserDto userDto = new UserDto(0L, "user", GlobalRole.USER, "#1337ff", false, false, null, true, false);
        User user = new CachedUser(userDto, cache);
        Connection connection = spy(new TestConnection(user));
        IgnoreService ignoreService = mock(IgnoreService.class);
        when(ignoreService.getIgnoredNames(user)).thenReturn(ImmutableList.of("kkk", "someuser"));
        IgnoreHandler handler = new IgnoreHandler(ignoreService);

        handler.handle(connection, user, new Message(ImmutableMap.of(
            MessageProperty.TYPE, MessageType.IGNORE,
            MessageProperty.NAME, "someuser"
        )));

        verify(ignoreService).ignore(user, "someuser");
        verify(connection).send(eq(Message.ignoredMessage(ImmutableList.of("kkk", "someuser"))));
        verify(connection).send(eq(Message.ignoreMessage(MessageType.IGNORE, "someuser")));
    }

    @Test
    public void shouldSendErrorOnValidationFailure() {
        UserDto userDto = new UserDto(0L, "user", GlobalRole.USER, "#1337ff", false, false, null, true, false);
        User user = new CachedUser(userDto, cache);
        Connection connection = spy(new TestConnection(user));
        IgnoreService ignoreService = mock(IgnoreService.class);
        doThrow(new InvalidInputException("name", "ERROR_NAME")).when(ignoreService).ignore(user, "someuser");
        IgnoreHandler handler = new IgnoreHandler(ignoreService);

        handler.handle(connection, user, new Message(ImmutableMap.of(
            MessageProperty.TYPE, MessageType.IGNORE,
            MessageProperty.NAME, "someuser"
        )));

        verify(ignoreService).ignore(user, "someuser");
        verify(connection).send(eq(Message.errorMessage("ERROR_NAME")));
        verify(connection, never()).send(eq(Message.ignoredMessage(ignoreService.getIgnoredNames(user))));
        verify(connection, never()).send(eq(Message.ignoreMessage(MessageType.IGNORE, "someuser")));
    }

    @Test
    public void shouldSendErrorOnUnknownUser() {
        UserDto userDto = new UserDto(0L, "user", GlobalRole.USER, "#1337ff", false, false, null, true, false);
        User user = new CachedUser(userDto, cache);
        Connection connection = spy(new TestConnection(user));
        IgnoreService ignoreService = mock(IgnoreService.class);
        doThrow(new EntityNotFoundException("user")).when(ignoreService).ignore(user, "someuser");
        IgnoreHandler handler = new IgnoreHandler(ignoreService);

        handler.handle(connection, user, new Message(ImmutableMap.of(
            MessageProperty.TYPE, MessageType.IGNORE,
            MessageProperty.NAME, "someuser"
        )));

        verify(ignoreService).ignore(user, "someuser");
        verify(connection).send(eq(Message.errorMessage("UNKNOWN_USER")));
        verify(connection, never()).send(eq(Message.ignoredMessage(ignoreService.getIgnoredNames(user))));
        verify(connection, never()).send(eq(Message.ignoreMessage(MessageType.IGNORE, "someuser")));
    }

    @Test
    public void shouldSendErrorOnLimit() {
        UserDto userDto = new UserDto(0L, "user", GlobalRole.USER, "#1337ff", false, false, null, true, false);
        User user = new CachedUser(userDto, cache);
        Connection connection = spy(new TestConnection(user));
        IgnoreService ignoreService = mock(IgnoreService.class);
        doThrow(new LimitExceededException("ignored")).when(ignoreService).ignore(user, "someuser");
        IgnoreHandler handler = new IgnoreHandler(ignoreService);

        handler.handle(connection, user, new Message(ImmutableMap.of(
            MessageProperty.TYPE, MessageType.IGNORE,
            MessageProperty.NAME, "someuser"
        )));

        verify(ignoreService).ignore(user, "someuser");
        verify(connection).send(eq(Message.errorMessage("IGNORE_LIMIT_REACHED")));
        verify(connection, never()).send(eq(Message.ignoredMessage(ignoreService.getIgnoredNames(user))));
        verify(connection, never()).send(eq(Message.ignoreMessage(MessageType.IGNORE, "someuser")));
    }
}