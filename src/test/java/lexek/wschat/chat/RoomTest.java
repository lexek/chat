package lexek.wschat.chat;

import lexek.wschat.db.model.UserDto;
import lexek.wschat.services.ChatterService;
import lexek.wschat.services.UserService;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class RoomTest {
    @Test
    public void shouldReturnGuestChatterForUserWithNullId() throws Exception {
        UserService userService = mock(UserService.class);
        ChatterService chatterService = mock(ChatterService.class);
        Room room = new Room(userService, chatterService, 0L, "#main", "top kek");
        UserDto userDto = new UserDto(null, "hellomate", GlobalRole.UNAUTHENTICATED, "#133711", false, false, null, false);
        Chatter chatter = room.getOnlineChatter(userDto);
        assertEquals(chatter, Chatter.GUEST_CHATTER);
    }

    @Test
    public void testGetChatter1() throws Exception {
        UserService userService = mock(UserService.class);
        ChatterService chatterService = mock(ChatterService.class);
        Room room = new Room(userService, chatterService, 0L, "#main", "top kek");
        UserDto userDto = new UserDto(null, "hellomate", GlobalRole.UNAUTHENTICATED, "#133711", false, false, null, false);
        Chatter chatter = room.getOnlineChatter(userDto);
        assertEquals(chatter, Chatter.GUEST_CHATTER);
    }

    @Test
    public void testJoin() throws Exception {

    }

    @Test
    public void testPart() throws Exception {

    }

    @Test
    public void testFetchChatter() throws Exception {

    }

    @Test
    public void testInRoom() throws Exception {

    }

    @Test
    public void testGetOnlineChatters() throws Exception {

    }

    @Test
    public void testGetName() throws Exception {

    }

    @Test
    public void testGetTopic() throws Exception {

    }

    @Test
    public void testSetTopic() throws Exception {

    }

    @Test
    public void testGetId() throws Exception {

    }

    @Test
    public void testGetHistory() throws Exception {

    }

    @Test
    public void testHasUser() throws Exception {

    }

    @Test
    public void testGetOnline() throws Exception {

    }

    @Test
    public void testRemoveTimeout() throws Exception {

    }

    @Test
    public void testTimeoutChatter() throws Exception {

    }

    @Test
    public void testBanChatter() throws Exception {

    }

    @Test
    public void testSetRole() throws Exception {

    }

    @Test
    public void testUnbanChatter() throws Exception {

    }
}