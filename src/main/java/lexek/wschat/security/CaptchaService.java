package lexek.wschat.security;

import com.google.common.cache.CacheBuilder;
import lexek.wschat.chat.Connection;
import lexek.wschat.chat.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class CaptchaService {
    private final Logger logger = LoggerFactory.getLogger(CaptchaService.class);
    private final ConcurrentMap<Long, Runnable> onSuccessActions = CacheBuilder.newBuilder()
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .maximumSize(200)
        .<Long, Runnable>build()
        .asMap();
    private final AtomicLong counter = new AtomicLong();

    public long tryAuthorize(final Connection connection, Runnable onSuccess) {
        long id = counter.incrementAndGet();
        connection.send(Message.captchaMessage(String.valueOf(id), connection.getUser().getName()));
        onSuccessActions.put(id, onSuccess);
        return id;
    }

    public long tryAuthorize(Runnable onSuccess) {
        long id = counter.incrementAndGet();
        onSuccessActions.put(id, onSuccess);
        return id;
    }

    public void success(long id) {
        Runnable r = onSuccessActions.remove(id);
        if (r != null) {
            r.run();
        } else {
            logger.warn("Wrong id: {}", id);
        }
    }

    public boolean isValidId(long id) {
        return onSuccessActions.containsKey(id);
    }
}
