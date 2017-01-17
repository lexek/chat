package lexek.wschat.services.raffle;

import lexek.wschat.chat.model.Message;

public interface ParticipationStrategy {
    boolean canParticipate(Message message);
}
