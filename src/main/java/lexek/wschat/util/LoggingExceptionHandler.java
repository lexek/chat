package lexek.wschat.util;

import com.lmax.disruptor.ExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingExceptionHandler implements ExceptionHandler {
    private final Logger logger = LoggerFactory.getLogger(LoggingExceptionHandler.class);

    @Override
    public void handleEventException(final Throwable ex, final long sequence, final Object event) {
        logger.warn("Exception processing: " + sequence + " " + event, ex);
    }

    @Override
    public void handleOnStartException(final Throwable ex) {
        logger.error("Exception during onStart()", ex);
    }

    @Override
    public void handleOnShutdownException(final Throwable ex) {
        logger.error("Exception during onShutdown()", ex);
    }
}
