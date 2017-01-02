package lexek.wschat.security;

import lexek.wschat.db.dao.SessionDao;
import lexek.wschat.db.model.SessionDto;
import lexek.wschat.db.model.UserDto;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

@Service
public class SessionService {
    private final Logger logger = LoggerFactory.getLogger(SessionService.class);
    private final SessionDao sessionDao;
    private final SecureTokenGenerator secureTokenGenerator;

    @Inject
    public SessionService(SessionDao sessionDao, SecureTokenGenerator secureTokenGenerator) {
        this.sessionDao = sessionDao;
        this.secureTokenGenerator = secureTokenGenerator;
    }

    public SessionDto getSession(String sid, String ip) {
        try {
            return sessionDao.getSession(sid, ip);
        } catch (Exception e) {
            logger.error("exception", e);
            return null;
        }
    }

    public SessionDto createSession(String ip, UserDto user) {
        String sid = user.getId() + "_" + secureTokenGenerator.generateSessionId();
        return sessionDao.createSession(sid, ip, user, System.currentTimeMillis());
    }

    public void invalidateSession(String sid) {
        try {
            sessionDao.invalidateSession(sid);
        } catch (Exception e) {
            logger.error("exception", e);
        }
    }
}
