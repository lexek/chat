package lexek.wschat.db.model;

import org.jooq.Record;

import static lexek.wschat.db.jooq.tables.Session.SESSION;

public class SessionDto {
    private long id;
    private UserAuthDto userAuth;
    private String sessionId;
    private String ip;
    private long expires;

    public SessionDto() {
    }

    public SessionDto(long id, UserAuthDto userAuth, String sessionId, String ip, long expires) {
        this.id = id;
        this.userAuth = userAuth;
        this.sessionId = sessionId;
        this.ip = ip;
        this.expires = expires;
    }

    public static SessionDto fromRecord(Record record) {
        return fromRecord(record, UserAuthDto.fromRecord(record));
    }

    public static SessionDto fromRecord(Record record, UserAuthDto userAuth) {
        if (record != null && record.getValue(SESSION.ID) != null) {
            return new SessionDto(
                    record.getValue(SESSION.ID),
                    userAuth,
                    record.getValue(SESSION.SID),
                    record.getValue(SESSION.IP),
                    record.getValue(SESSION.EXPIRES)
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

    public UserAuthDto getUserAuth() {
        return userAuth;
    }

    public void setUserAuth(UserAuthDto userAuth) {
        this.userAuth = userAuth;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public long getExpires() {
        return expires;
    }

    public void setExpires(long expires) {
        this.expires = expires;
    }
}
