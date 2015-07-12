package lexek.wschat.services;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import lexek.wschat.chat.Connection;
import lexek.wschat.chat.Room;
import lexek.wschat.db.model.Chatter;
import lexek.wschat.util.LoggingExceptionHandler;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class RoomJoinNotificationService extends AbstractService<Void> {
    private final Disruptor<JoinedRoomEvent> disruptor;
    private final RingBuffer<JoinedRoomEvent> ringBuffer;
    private final List<RoomJoinedEventListener> listeners = new CopyOnWriteArrayList<>();

    public RoomJoinNotificationService() {
        super("notificationService", ImmutableList.<String>of());
        EventFactory<JoinedRoomEvent> eventFactory = JoinedRoomEvent::new;
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("NOTIFICATIONS_%d").build();
        this.disruptor = new Disruptor<>(
            eventFactory,
            32,
            Executors.newSingleThreadExecutor(threadFactory),
            ProducerType.MULTI,
            new BlockingWaitStrategy()
        );
        this.ringBuffer = this.disruptor.getRingBuffer();
        this.disruptor.handleExceptionsWith(new LoggingExceptionHandler());
        this.disruptor.handleEventsWith(new NotificationServiceWorker());
    }

    @Override
    protected void start0() {
        this.disruptor.start();
    }

    @Override
    public void performAction(String action) {
    }

    @Override
    public void stop() {
        this.disruptor.shutdown();
    }

    public void registerListener(RoomJoinedEventListener listener) {
        this.listeners.add(listener);
    }

    public void joinedRoom(Connection connection, Chatter chatter, Room room) {
        long sequence = ringBuffer.next();
        JoinedRoomEvent event = ringBuffer.get(sequence);
        event.setConnection(connection);
        event.setChatter(chatter);
        event.setRoom(room);
        ringBuffer.publish(sequence);
    }

    private static class JoinedRoomEvent {
        private Connection connection;
        private Chatter chatter;
        private Room room;

        public Connection getConnection() {
            return connection;
        }

        public void setConnection(Connection connection) {
            this.connection = connection;
        }

        public Chatter getChatter() {
            return chatter;
        }

        public void setChatter(Chatter chatter) {
            this.chatter = chatter;
        }

        public Room getRoom() {
            return room;
        }

        public void setRoom(Room room) {
            this.room = room;
        }
    }

    private class NotificationServiceWorker implements EventHandler<JoinedRoomEvent> {
        @Override
        public void onEvent(final JoinedRoomEvent event, long l, boolean b) throws Exception {
            listeners.forEach(listener -> listener.joined(event.getConnection(), event.getChatter(), event.getRoom()));
        }
    }
}