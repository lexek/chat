package lexek.wschat.chat.handlers;

import com.google.common.collect.ImmutableList;
import lexek.wschat.chat.*;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.db.dao.UserDao;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class ColorHandlerTest {
    private UserDto userDto = new UserDto(0L, "user", GlobalRole.USER, "#1337ff", false, false, null, true);
    private User user = new User(userDto);
    private Connection connection = spy(new TestConnection(user));
    private UserDao userDao = mock(UserDao.class);
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
    public void testGetArgCount() throws Exception {
        assertEquals(handler.getArgCount(), 1);
    }

    @Test
    public void testSetColorWithGoodColor() {
        handler.handle(ImmutableList.of("springgreen"), connection);
        verify(userDao, times(1)).setColor(eq(0L), eq("#00BA5D"));
        verify(connection, times(1)).send(eq(Message.colorMessage("#00BA5D")));
        assertEquals(user.getColor(), "#00BA5D");
    }

    @Test
    public void testSetColorWithBadColor() {
        handler.handle(ImmutableList.of("#000000"), connection);
        verifyZeroInteractions(userDao);
        verify(connection, times(1)).send(eq(Message.errorMessage("WRONG_COLOR")));
    }

    @Test
    public void testSuperadminSetColorWithBadColor() {
        user.setRole(GlobalRole.SUPERADMIN);
        handler.handle(ImmutableList.of("#000000"), connection);
        verify(userDao, times(1)).setColor(eq(0L), eq("#000000"));
        verify(connection, times(1)).send(eq(Message.colorMessage("#000000")));
        assertEquals(user.getColor(), "#000000");
    }
}