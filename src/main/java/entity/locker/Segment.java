package entity.locker;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

class Segment<K> {
    private final Map<K, CountingLock> lockMap = new HashMap<>();

    synchronized ReentrantLock getLock(K key) {
        CountingLock lock = lockMap.computeIfAbsent(key, k -> new CountingLock());
        lock.uses++;
        return lock;
    }

    synchronized void freeLock(K key) {
        CountingLock lock = lockMap.get(key);
        lock.uses--;
        if (lock.uses == 0) {
            lockMap.remove(key);
        }
    }

    synchronized int size() {
        return lockMap.size();
    }

    /**
     * only for testing
     */
    int waitingThreadsCount() {
        return lockMap.values()
                .stream()
                .mapToInt(ReentrantLock::getQueueLength)
                .sum();
    }

    private static class CountingLock extends ReentrantLock {
        private int uses = 0;
    }
}
