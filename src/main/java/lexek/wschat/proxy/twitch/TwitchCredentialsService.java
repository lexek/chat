package lexek.wschat.proxy.twitch;

import lexek.wschat.db.model.UserAuthDto;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.security.AuthenticationManager;
import lexek.wschat.security.UserAuthEventListener;
import lexek.wschat.security.UserAuthEventType;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TwitchCredentialsService implements UserAuthEventListener {
    private final Logger logger = LoggerFactory.getLogger(TwitchCredentialsService.class);
    private final AuthenticationManager authenticationManager;
    private final Map<Long, Boolean> checkedUsers = new ConcurrentHashMap<>();

    @Inject
    public TwitchCredentialsService(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Override
    public void onEvent(UserAuthEventType type, UserDto user, String service) {
        if (service.equals("twitch")) {
            if (type == UserAuthEventType.DELETED) {
                checkedUsers.remove(user.getId());
            } else if (type == UserAuthEventType.CREATED) {
                checkedUsers.remove(user.getId());
            }
        }
    }

    private boolean needsFetchingConnectionData(long userId) {
        Boolean tmp = checkedUsers.get(userId);
        return tmp == null || tmp;
    }

    private UserCredentials fetchConnectionDataForUser(String userName, long userId) {
        UserCredentials userCredentials = null;
        logger.debug("fetching connection data for user {}", userName);
        boolean r = false;
        UserAuthDto auth = authenticationManager.getAuthDataForUser(userId, "twitch");
        if (auth != null) {
            String token = auth.getAuthenticationKey();
            String extName = auth.getAuthenticationName();
            logger.debug("fetched data for user {} with ext name {}", userName, extName, token);
            if (token != null && extName != null) {
                userCredentials = new UserCredentials(extName, token);
            }
            r = true;
        }
        checkedUsers.put(userId, r);
        return userCredentials;
    }

    public UserCredentials getCredentials(long userId, String name) {
        if (needsFetchingConnectionData(userId)) {
            return fetchConnectionDataForUser(name, userId);
        }
        return null;
    }
}
