package lexek.wschat.services.raffle;

import lexek.wschat.chat.model.Message;
import lexek.wschat.proxy.Proxy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Raffle {
    private volatile RaffleStatus status = RaffleStatus.ACTIVE;
    private final long id;
    private final String name;
    private final boolean blacklistEnabled;
    private final Set<RaffleParticipant> participants = new HashSet<>();
    private final Set<RaffleParticipant> blacklist = new HashSet<>();
    private final Set<Proxy> allowedProxies;
    private final ParticipationStrategy participationStrategy;
    private RaffleParticipant winner;
    private List<Message> winnerMessages = new ArrayList<>();

    public Raffle(long id, String name, boolean blacklistEnabled, Set<Proxy> allowedProxies, ParticipationStrategy participationStrategy) {
        this.id = id;
        this.name = name;
        this.blacklistEnabled = blacklistEnabled;
        this.allowedProxies = allowedProxies;
        this.participationStrategy = participationStrategy;
    }

    public long getId() {
        return this.id;
    }

    public String getName() {
        return name;
    }

    public synchronized RaffleStatus getStatus() {
        return this.status;
    }

    public Set<Proxy> getAllowedProxies() {
        return allowedProxies;
    }

    public boolean canParticipate(Message message) {
        return participationStrategy.canParticipate(message);
    }

    public void addParticipant(RaffleParticipant participant) {
        if (blacklistEnabled && blacklist.contains(participant)) {
            participants.remove(participant);
            blacklist.add(participant);
            return;
        }
        participants.add(participant);
    }

    public Set<RaffleParticipant> getParticipants() {
        return participants;
    }

    public void setWinner(RaffleParticipant winner) {
        this.winner = winner;
        this.winnerMessages.clear();
    }

    public RaffleParticipant getWinner() {
        return winner;
    }

    public void setStatus(RaffleStatus status) {
        this.status = status;
    }

    public List<Message> getWinnerMessages() {
        return this.winnerMessages;
    }
}
