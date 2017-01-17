package lexek.wschat.services.raffle;

import com.google.common.collect.ImmutableList;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.filters.BroadcastFilter;
import lexek.wschat.chat.model.Message;
import lexek.wschat.chat.model.MessageProperty;
import lexek.wschat.chat.model.MessageType;
import lexek.wschat.proxy.Proxy;
import lexek.wschat.services.MessageConsumerService;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.List;

@Service
public class RaffleMessageConsumer implements MessageConsumerService {
    private final RaffleService raffleService;

    @Inject
    public RaffleMessageConsumer(RaffleService raffleService) {
        this.raffleService = raffleService;
    }

    @Override
    public void consume(Message message, BroadcastFilter filter) {
        if (message.getType() == MessageType.MSG || message.getType() == MessageType.MSG_EXT) {
            for (Raffle raffle : getRafflesForFilter(filter)) {
                if (message.getType() == MessageType.MSG_EXT) {
                    String externalName = message.get(MessageProperty.NAME);
                    String externalService = message.get(MessageProperty.SERVICE);
                    if (raffle.getStatus() == RaffleStatus.PICKING) {
                        RaffleParticipant winner = raffle.getWinner();
                        if (externalName.equals(winner.getExternalName()) && externalService.equals(winner.getExternalService())) {
                            raffle.getWinnerMessages().add(message);
                        }
                    } else if (raffle.getStatus() == RaffleStatus.ACTIVE) {
                        String externalRoom = message.get(MessageProperty.SERVICE_RESOURCE);
                        for (Proxy proxy : raffle.getAllowedProxies()) {
                            String providerName = proxy.provider().getName();
                            if (providerName.equals(externalService) && proxy.remoteRoom().equals(externalRoom)) {
                                if (raffle.canParticipate(message)) {
                                    raffle.addParticipant(new RaffleParticipant(null, null, externalService, externalName));
                                }
                            }
                        }
                    }
                }
                if (message.getType() == MessageType.MSG) {
                    Long userId = message.get(MessageProperty.USER_ID);
                    if (raffle.getStatus() == RaffleStatus.ACTIVE) {
                        if (raffle.canParticipate(message)) {
                            raffle.addParticipant(new RaffleParticipant(
                                userId,
                                message.get(MessageProperty.NAME),
                                null, null
                            ));
                        }
                    } else if (raffle.getStatus() == RaffleStatus.PICKING) {
                        RaffleParticipant winner = raffle.getWinner();
                        if (userId.equals(winner.getInternalId())) {
                            raffle.getWinnerMessages().add(message);
                        }
                    }
                }
            }
        }
    }

    private List<Raffle> getRafflesForFilter(BroadcastFilter filter) {
        if (filter.getType() == BroadcastFilter.Type.ROOM) {
            Room room = (Room) filter.getData();
            return raffleService.getActiveRaffles(room);
        }
        return ImmutableList.of();
    }
}
