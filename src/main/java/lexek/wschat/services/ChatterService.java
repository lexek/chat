package lexek.wschat.services;

import lexek.wschat.chat.Chatter;
import lexek.wschat.chat.LocalRole;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.User;
import lexek.wschat.db.dao.ChatterDao;

public class ChatterService {
    private final ChatterDao chatterDao;
    private final JournalService journalService;

    public ChatterService(ChatterDao chatterDao, JournalService journalService) {
        this.chatterDao = chatterDao;
        this.journalService = journalService;
    }

    public Chatter getChatter(Room room, String name) {
        return chatterDao.getChatter(name, room.getId());
    }

    public Chatter getChatter(Room room, User user) {
        return chatterDao.getChatter(user, room.getId());
    }

    public boolean banChatter(Room room, Chatter chatter, Chatter mod) {
        boolean result = false;
        if (chatter != null && chatter.getId() != null) {
            result = chatterDao.banChatter(chatter.getId());
            if (result) {
                chatter.setBanned(true);
                journalService.roomBan(chatter.getUser().getWrappedObject(), mod.getUser().getWrappedObject(), room);
            }
        }
        return result;
    }

    public boolean unbanChatter(Room room, Chatter chatter, Chatter mod) {
        boolean result = false;
        if (chatter != null && chatter.getId() != null) {
            result = chatterDao.unbanChatter(chatter.getId());
            if (result) {
                chatter.setBanned(false);
                chatter.setTimeout(null);
                journalService.roomUnban(chatter.getUser().getWrappedObject(), mod.getUser().getWrappedObject(), room);
            }
        }
        return result;
    }

    public boolean timeoutChatter(Room room, Chatter chatter, long until) {
        boolean result = false;
        if (chatter != null && chatter.getId() != null) {
            result = chatterDao.setTimeout(chatter.getId(), until);
            if (result) {
                chatter.setTimeout(until);
            }
        }
        return result;
    }

    public boolean setRole(Room room, Chatter chatter, Chatter admin, LocalRole newRole) {
        boolean result = false;
        if (chatter != null && chatter.getId() != null && newRole != LocalRole.GUEST) {
            result = chatterDao.setRole(chatter.getId(), newRole);
            if (result) {
                chatter.setRole(newRole);
                journalService.roomRole(
                    chatter.getUser().getWrappedObject(),
                    admin.getUser().getWrappedObject(),
                    room,
                    newRole);
            }
        }
        return result;
    }

    public void removeTimeout(Chatter chatter) {
        if (chatterDao.setTimeout(chatter.getId(), null)) {
            chatter.setTimeout(null);
        }
    }
}
