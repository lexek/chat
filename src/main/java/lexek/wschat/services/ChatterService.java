package lexek.wschat.services;

import lexek.wschat.chat.Chatter;
import lexek.wschat.chat.LocalRole;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.User;
import lexek.wschat.db.dao.ChatterDao;

public class ChatterService {
    private final ChatterDao chatterDao;
    private final JournalService journalService;
    private final UserService userService;

    public ChatterService(ChatterDao chatterDao, JournalService journalService, UserService userService) {
        this.chatterDao = chatterDao;
        this.journalService = journalService;
        this.userService = userService;
    }

    public Chatter getChatter(Room room, String name) {
        return chatterDao.getChatter(userService.cache(userService.fetchByName(name)), room.getId());
    }

    public Chatter getChatter(Room room, User user) {
        return chatterDao.getChatter(user, room.getId());
    }

    public boolean banChatter(Room room, Chatter chatter, Chatter mod) {
        boolean result = false;
        if (chatter != null && chatter.getId() != null) {
            chatterDao.banChatter(chatter.getId());
            chatter.setBanned(true);
            journalService.roomBan(chatter.getUser().getWrappedObject(), mod.getUser().getWrappedObject(), room);
            result = true;
        }
        return result;
    }

    public boolean unbanChatter(Room room, Chatter chatter, Chatter mod) {
        boolean result = false;
        if (chatter != null && chatter.getId() != null) {
            chatterDao.unbanChatter(chatter.getId());
            chatter.setBanned(false);
            chatter.setTimeout(null);
            journalService.roomUnban(chatter.getUser().getWrappedObject(), mod.getUser().getWrappedObject(), room);
            result = true;
        }
        return result;
    }

    public boolean timeoutChatter(Room room, Chatter chatter, long until) {
        boolean result = false;
        if (chatter != null && chatter.getId() != null) {
            chatterDao.setTimeout(chatter.getId(), until);
            chatter.setTimeout(until);
            result = true;
        }
        return result;
    }

    public boolean setRole(Room room, Chatter chatter, Chatter admin, LocalRole newRole) {
        boolean result = false;
        if (chatter != null && chatter.getId() != null && newRole != LocalRole.GUEST) {
            chatterDao.setRole(chatter.getId(), newRole);
            chatter.setRole(newRole);
            journalService.roomRole(
                chatter.getUser().getWrappedObject(),
                admin.getUser().getWrappedObject(),
                room,
                newRole);
            result = true;
        }
        return result;
    }

    public void removeTimeout(Chatter chatter) {
        chatterDao.setTimeout(chatter.getId(), null);
        chatter.setTimeout(null);
    }
}
