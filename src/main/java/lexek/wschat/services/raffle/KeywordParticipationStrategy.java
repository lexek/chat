package lexek.wschat.services.raffle;

import lexek.wschat.chat.model.Message;
import lexek.wschat.chat.model.MessageProperty;
import lexek.wschat.chat.msg.MessageNode;

import java.util.stream.Collectors;

public class KeywordParticipationStrategy implements ParticipationStrategy {
    private final String keyword;

    public KeywordParticipationStrategy(String keyword) {
        this.keyword = keyword;
    }

    @Override
    public boolean canParticipate(Message message) {
        return message
            .get(MessageProperty.MESSAGE_NODES)
            .stream()
            .map(MessageNode::getText)
            .collect(Collectors.joining(""))
            .contains(keyword);
    }
}
