package lexek.wschat.services;

import lexek.wschat.chat.MessageBroadcaster;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.filters.UserInRoomFilter;
import lexek.wschat.chat.model.*;
import lexek.wschat.db.dao.ChatterDao;

public class ChatterService {
    private final ChatterDao chatterDao;
    private final JournalService journalService;
    private final UserService userService;
    private final MessageBroadcaster messageBroadcaster;

    public ChatterService(ChatterDao chatterDao, JournalService journalService, UserService userService, MessageBroadcaster messageBroadcaster) {
        this.chatterDao = chatterDao;
        this.journalService = journalService;
        this.userService = userService;
        this.messageBroadcaster = messageBroadcaster;
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
            Message message = Message.moderationMessage(
                MessageType.BAN,
                room.getName(),
                mod.getUser().getName(),
                chatter.getUser().getName()
            );
            messageBroadcaster.submitMessage(message, room.FILTER);
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
            messageBroadcaster.submitMessage(
                Message.infoMessage("You have been unbanned", room.getName()),
                new UserInRoomFilter(chatter.getUser(), room)
            );
            result = true;
        }
        return result;
    }

    public boolean timeoutChatter(Room room, Chatter chatter, Chatter mod, long until) {
        boolean result = false;
        if (chatter != null && chatter.getId() != null) {
            chatterDao.setTimeout(chatter.getId(), until);
            chatter.setTimeout(until);
            Message message = Message.moderationMessage(
                MessageType.TIMEOUT,
                room.getName(),
                mod.getUser().getName(),
                chatter.getUser().getName()
            );
            messageBroadcaster.submitMessage(message, room.FILTER);
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
