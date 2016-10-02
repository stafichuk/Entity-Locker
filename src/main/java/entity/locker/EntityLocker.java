package entity.locker;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

public class EntityLocker<T> {
    private final ConcurrentMap<T, ReentrantLock> lockMap = new ConcurrentHashMap<>();

    public void execute(T key, Command command) {
        ReentrantLock lock = getLock(key);
        lock.lock();
        try {
            command.execute();
        } finally {
            lock.unlock();
        }
    }

    private ReentrantLock getLock(T key) {
        return lockMap.computeIfAbsent(key, k -> new ReentrantLock());
    }

    int waitingThreadsCount() {
        return lockMap.values()
                .stream()
                .mapToInt(ReentrantLock::getQueueLength)
                .sum();
    }
}
