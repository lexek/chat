package lexek.wschat.services.raffle;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import jersey.repackaged.com.google.common.collect.Lists;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.e.BadRequestException;
import lexek.wschat.chat.e.InvalidInputException;
import lexek.wschat.proxy.Proxy;
import org.apache.commons.lang3.StringUtils;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class RaffleService {
    private final AtomicLong internalId = new AtomicLong();
    private final ListMultimap<Long, Raffle> activeRaffles = Multimaps.synchronizedListMultimap(ArrayListMultimap.create());
    private final SecureRandom secureRandom = new SecureRandom();

    @Inject
    public RaffleService() {
    }

    public void createRaffle(
        Room room,
        String name,
        boolean blacklistEnabled,
        Set<Proxy> allowedProxies,
        RaffleType type,
        Map<String, String> params
    ) {
        ParticipationStrategy participationStrategy = null;
        switch (type) {
            case ANY_MESSAGE:
                participationStrategy = new AnyMessageParticipationStrategy();
                break;
            case KEYWORD:
                String keyword = StringUtils.trimToNull(params.get("keyword"));
                if (keyword == null) {
                    throw new InvalidInputException("keyword", "KEYWORD_EMPTY");
                }
                participationStrategy = new KeywordParticipationStrategy(params.get("keyword"));
                break;
            default:
                throw new IllegalArgumentException();
        }
        Raffle raffle = new Raffle(internalId.getAndIncrement(), name, blacklistEnabled, allowedProxies, participationStrategy);
        activeRaffles.put(room.getId(), raffle);
        //todo: broadcast
    }

    public List<Raffle> getActiveRaffles(Room room) {
        return activeRaffles.get(room.getId());
    }

    public RaffleParticipant pickWinner(long raffleId) {
        Raffle raffle = findActiveRaffle(raffleId);

        if (raffle.getStatus() != RaffleStatus.PICKING || raffle.getStatus() != RaffleStatus.ACTIVE) {
            throw new BadRequestException("RAFFLE_ILLEGAL_STATUS");
        }

        raffle.setStatus(RaffleStatus.PICKING);

        List<RaffleParticipant> participants = Lists.newArrayList(raffle.getParticipants());
        int participantAmount = participants.size();

        if (participantAmount == 0) {
            throw new BadRequestException("RAFFLE_NEED_MORE_PARTICIPANTS");
        }

        RaffleParticipant winner = participants.get(secureRandom.nextInt(participantAmount));

        raffle.setWinner(winner);

        return winner;
    }

    public void finishRaffle(long raffleId) {
        Raffle raffle = findActiveRaffle(raffleId);

        if (raffle.getStatus() != RaffleStatus.PICKING) {
            throw new BadRequestException("RAFFLE_ILLEGAL_STATUS");
        }

        raffle.setStatus(RaffleStatus.DONE);
        //todo: remove from active
        //todo: broadcast
        //todo: store to database
    }

    private Raffle findActiveRaffle(long id) {
        return activeRaffles
            .values()
            .stream()
            .filter(e -> e.getId() == id)
            .findFirst()
            .orElseThrow(() -> new BadRequestException("RAFFLE_UNKNOWN"));
    }
}
