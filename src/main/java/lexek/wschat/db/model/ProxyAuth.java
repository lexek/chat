package lexek.wschat.db.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ProxyAuth {
    private final Long id;
    private final String service;
    private final String externalId;
    private final String externalName;
    private final UserDto owner;
    @JsonIgnore
    private final String key;

    public ProxyAuth(Long id, String service, String externalId, String externalName, UserDto owner, String key) {
        this.id = id;
        this.service = service;
        this.externalId = externalId;
        this.externalName = externalName;
        this.owner = owner;
        this.key = key;
    }

    public ProxyAuth(Long id, ProxyAuth proxyAuth) {
        this.id = id;
        this.service = proxyAuth.service;
        this.externalId = proxyAuth.externalId;
        this.externalName = proxyAuth.externalName;
        this.owner = proxyAuth.owner;
        this.key = proxyAuth.key;
    }

    public Long getId() {
        return id;
    }

    public String getService() {
        return service;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getExternalName() {
        return externalName;
    }

    public UserDto getOwner() {
        return owner;
    }

    public String getKey() {
        return key;
    }
}
