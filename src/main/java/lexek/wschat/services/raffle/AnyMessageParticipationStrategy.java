package lexek.wschat.services.raffle;

import lexek.wschat.chat.model.Message;

public class AnyMessageParticipationStrategy implements ParticipationStrategy {
    @Override
    public boolean canParticipate(Message message) {
        return true;
    }
}
