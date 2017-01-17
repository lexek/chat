package lexek.wschat.services.raffle;

import java.util.Objects;

public class RaffleParticipant {
    private final Long internalId;
    private final String internalName;
    private final String externalService;
    private final String externalName;

    public RaffleParticipant(Long internalId, String internalName, String externalService, String externalName) {
        if (internalId == null && externalName == null) {
            throw new IllegalArgumentException();
        }
        this.internalId = internalId;
        this.internalName = internalName;
        this.externalService = externalService;
        this.externalName = externalName;
    }

    public Long getInternalId() {
        return internalId;
    }

    public String getInternalName() {
        return internalName;
    }

    public String getExternalService() {
        return externalService;
    }

    public String getExternalName() {
        return externalName;
    }

    public boolean equals(RaffleParticipant o) {
        if (internalId != null && o.internalId != null) {
            return internalId.equals(o.internalId);
        }
        return externalName.equals(o.externalName) && externalService.equals(o.externalService);
    }

    @Override
    public int hashCode() {
        return Objects.hash(internalId, internalName, externalService, externalName);
    }
}
