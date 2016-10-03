package entity.locker;

import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

import static java.lang.Math.abs;

public class EntityLocker<T> {
    private static final int DEFAULT_CONCURRENCY_LEVEL = 16;

    private final Segment<T>[] segments;

    public EntityLocker() {
        this(DEFAULT_CONCURRENCY_LEVEL);
    }

    @SuppressWarnings("unchecked")
    public EntityLocker(int concurrencyLevel) {
        segments = new Segment[concurrencyLevel];
        Arrays.setAll(segments, i -> new Segment<>());
    }

    public void execute(T key, Command command) {
        Segment<T> segment = getSegment(key);
        ReentrantLock lock = segment.getLock(key);
        lock.lock();
        try {
            command.execute();
        } finally {
            lock.unlock();
            segment.freeLock(key);
        }
    }

    private Segment<T> getSegment(T key) {
        return segments[abs(key.hashCode() % segments.length)];
    }

    /**
     * only for testing
     */
    int waitingThreadsCount() {
        return Stream.of(segments)
                .mapToInt(Segment::waitingThreadsCount)
                .sum();
    }

    /**
     * only for testing
     */
    int locksCount() {
        return Stream.of(segments)
                .mapToInt(Segment::size)
                .sum();
    }
}
