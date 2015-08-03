package lexek.wschat.chat.processing;

import com.google.common.collect.ImmutableSet;
import lexek.wschat.chat.*;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.security.CaptchaService;
import lexek.wschat.services.RoomJoinNotificationService;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;

public class HandlerInvokerTest {
    @Test
    public void testHandle() throws Exception {
        RoomJoinNotificationService roomJoinNotificationService = mock(RoomJoinNotificationService.class);
        Room room = mock(Room.class);
        RoomManager roomManager = mock(RoomManager.class);
        CaptchaService captchaService = mock(CaptchaService.class);

        UserDto userDto = new UserDto(0L, "user", GlobalRole.USER, "#ffffff", false, false, null, false);
        User user = new User(userDto);
        Connection connection = spy(new TestConnection(user));
        Chatter chatter = new Chatter(0L, LocalRole.USER, false, null, user);

        when(room.inRoom(connection)).thenReturn(false);
        when(room.join(connection)).thenReturn(chatter);
        when(room.getName()).thenReturn("#main");
        when(room.inRoom(user)).thenReturn(false);

        HandlerInvoker handlerInvoker = new HandlerInvoker(roomManager, ImmutableSet.of(), captchaService);

        verify(room).join(connection);
        verify(connection).send(eq(Message.selfJoinMessage("#main", chatter)));
        verify(roomJoinNotificationService).joinedRoom(eq(connection), eq(chatter), eq(room));
    }
}