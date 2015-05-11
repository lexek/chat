package lexek.wschat.db;

import org.jooq.Record;

import static lexek.wschat.db.jooq.tables.Userauth.USERAUTH;

public class UserAuthDto {
    private long id;
    private UserDto user;
    private String service;
    private String authenticationId;
    private String authenticationKey;
    private String authenticationName;

    public UserAuthDto() {
    }

    public UserAuthDto(long id, UserDto user, String service, String authenticationId, String authenticationKey, String authenticationName) {
        this.id = id;
        this.user = user;
        this.service = service;
        this.authenticationId = authenticationId;
        this.authenticationKey = authenticationKey;
        this.authenticationName = authenticationName;
    }

    public static UserAuthDto fromRecord(Record record) {
        return fromRecord(record, UserDto.fromRecord(record));
    }

    public static UserAuthDto fromRecord(Record record, UserDto userDto) {
        if (record != null && record.getValue(USERAUTH.ID) != null) {
            return new UserAuthDto(
                    record.getValue(USERAUTH.ID),
                    userDto,
                    record.getValue(USERAUTH.SERVICE),
                    record.getValue(USERAUTH.AUTH_ID),
                    record.getValue(USERAUTH.AUTH_KEY),
                    record.getValue(USERAUTH.AUTH_NAME)
            );
        } else {
            return null;
        }
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public UserDto getUser() {
        return user;
    }

    public void setUser(UserDto user) {
        this.user = user;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getAuthenticationId() {
        return authenticationId;
    }

    public void setAuthenticationId(String authenticationId) {
        this.authenticationId = authenticationId;
    }

    public String getAuthenticationKey() {
        return authenticationKey;
    }

    public void setAuthenticationKey(String authenticationKey) {
        this.authenticationKey = authenticationKey;
    }

    public String getAuthenticationName() {
        return authenticationName;
    }

    public void setAuthenticationName(String authenticationName) {
        this.authenticationName = authenticationName;
    }
}
