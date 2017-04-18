package lexek.wschat.chat.handlers;

import com.google.common.collect.ImmutableSet;
import lexek.wschat.chat.Connection;
import lexek.wschat.chat.TestConnection;
import lexek.wschat.chat.model.*;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.services.UserService;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class ColorHandlerTest {
    private UserDto userDto = new UserDto(0L, "user", GlobalRole.USER, "#1337ff", false, false, null, true, false);
    private User user = new CachedUser(userDto, cache);
    private Connection connection = spy(new TestConnection(user));
    private UserService userDao = mock(UserService.class);
    private final ColorHandler handler = new ColorHandler(userDao);

    @Before
    public void resetMocks() {
        userDto.setRole(GlobalRole.USER);
        userDto.setColor("#1337ff");
        reset(userDao, connection);
    }

    @Test
    public void testGetType() throws Exception {
        assertEquals(handler.getType(), MessageType.COLOR);
    }

    @Test
    public void testGetRole() throws Exception {
        assertEquals(handler.getRole(), GlobalRole.USER);
    }

    @Test
    public void shouldHaveRequiredProperties() throws Exception {
        assertEquals(
            handler.requiredProperties(),
            ImmutableSet.of(MessageProperty.COLOR)
        );
    }

    @Test
    public void shouldRequireTimeout() {
        assertTrue(handler.isNeedsInterval());
    }

    @Test
    public void testSetColorWithGoodColor() {
        handler.handle(connection, user, Message.colorMessage("springgreen"));
        verify(userDao, times(1)).setColor(eq(userDto), eq("#00BA5D"));
        verify(connection, times(1)).send(eq(Message.colorMessage("#00BA5D")));
        assertEquals(user.getColor(), "#00BA5D");
    }

    @Test
    public void testSetColorWithBadColor() {
        handler.handle(connection, user, Message.colorMessage("#000000"));
        verifyZeroInteractions(userDao);
        verify(connection, times(1)).send(eq(Message.errorMessage("WRONG_COLOR")));
    }

    @Test
    public void testSuperadminSetColorWithBadColor() {
        user.setRole(GlobalRole.SUPERADMIN);
        handler.handle(connection, user, Message.colorMessage("#000000"));
        verify(userDao, times(1)).setColor(eq(userDto), eq("#000000"));
        verify(connection, times(1)).send(eq(Message.colorMessage("#000000")));
        assertEquals(user.getColor(), "#000000");
    }
}