package lexek.wschat.chat.handlers;

import com.google.common.collect.ImmutableList;
import lexek.wschat.chat.*;
import lexek.wschat.db.model.Chatter;
import lexek.wschat.services.AnnouncementService;
import lexek.wschat.services.PollService;
import lexek.wschat.services.PollState;

import java.util.List;

public class JoinHandler extends AbstractMessageHandler {
    private final RoomManager roomManager;
    private final AnnouncementService announcementService;
    private final MessageBroadcaster messageBroadcaster;
    private final PollService pollService;

    public JoinHandler(RoomManager roomManager,
                       AnnouncementService announcementService,
                       MessageBroadcaster messageBroadcaster,
                       PollService pollService) {
        super(MessageType.JOIN, GlobalRole.UNAUTHENTICATED, 1, false, true);
        this.roomManager = roomManager;
        this.announcementService = announcementService;
        this.messageBroadcaster = messageBroadcaster;
        this.pollService = pollService;
    }

    @Override
    public void handle(List<String> args, Connection connection) {
        final Room room = roomManager.getRoomInstance(args.get(0));
        if (room != null && !room.contains(connection)) {
            User user = connection.getUser();
            boolean sendJoin = !room.hasUser(user);
            Chatter chatter = room.join(connection);

            Message joinMessage = Message.joinMessage(room.getName(), user.getWrappedObject());
            connection.send(Message.selfJoinMessage(room.getName(), chatter));
            connection.send(Message.historyMessage(room.getHistory()));
            if (sendJoin) {
                if (chatter.hasRole(LocalRole.USER)) {
                    messageBroadcaster.submitMessage(joinMessage, connection, room.FILTER);
                }
            }
            if (connection.isNeedNames()) {
                ImmutableList.Builder<Chatter> users = ImmutableList.builder();
                room.getChatters().stream().filter(c -> c.hasRole(LocalRole.USER)).forEach(users::add);
                connection.send(Message.namesMessage(room.getName(), users.build()));
            }

            announcementService.sendAnnouncements(connection, room);

            PollState activePoll = pollService.getActivePoll(room);
            if (activePoll != null) {
                connection.send(Message.pollMessage(MessageType.POLL, room.getName(), activePoll));
                if (activePoll.getVoted().contains(user.getId())) {
                    connection.send(Message.pollVotedMessage(room.getName()));
                }
            }
        } else {
            if (room == null) {
                connection.send(Message.errorMessage("ROOM_NOT_FOUND", ImmutableList.of(args.get(0))));
            } else {
                connection.send(Message.errorMessage("ROOM_ALREADY_JOINED"));
            }
        }
    }
}
