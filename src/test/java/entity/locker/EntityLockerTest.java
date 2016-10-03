package entity.locker;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EntityLockerTest {
    @Rule
    public Timeout globalTimeout = Timeout.seconds(1);

    @Test
    public void commandIsExecuted() {
        EntityLocker<String> locker = new EntityLocker<>();
        AtomicBoolean bool = new AtomicBoolean(false);
        locker.execute("test", () -> bool.set(true));
        assertTrue(bool.get());
    }

    @Test
    public void atMostOneThreadCanExecuteCodeOnOneEntity() throws Exception {
        EntityLocker<String> locker = new EntityLocker<>();
        CountDownLatch thread1StartedLatch = new CountDownLatch(1);
        CountDownLatch thread1FinishLatch = new CountDownLatch(1);
        Thread thread1 = new Thread(() ->
                locker.execute("test", () -> {
                    try {
                        thread1StartedLatch.countDown();
                        thread1FinishLatch.await();
                    } catch (InterruptedException ignored) {
                    }
                })
        );
        thread1.start();
        thread1StartedLatch.await();
        Thread thread2 = new Thread(() ->
                locker.execute("test", () -> {
                })
        );
        thread2.start();
        while (locker.waitingThreadsCount() < 1) {
            Thread.sleep(10);
        }
        thread1FinishLatch.countDown();
        thread2.join();
        thread1.join();
    }

    @Test
    public void lockIsFreedWhenCommandThrowsException() throws Exception {
        EntityLocker<String> locker = new EntityLocker<>();
        Thread thread1 = new Thread(() -> {
            try {
                locker.execute("test", () -> {
                    throw new RuntimeException();
                });
            } catch (Exception ignored) {
            }
        });
        thread1.start();
        Thread thread2 = new Thread(() ->
                locker.execute("test", () -> {
                })
        );
        thread2.start();
        thread2.join();
        thread1.join();
    }

    @Test
    public void concurrentExecutionOfProtectedCodeOnDifferentEntities() throws Exception {
        EntityLocker<String> locker = new EntityLocker<>();
        CountDownLatch thread1StartedLatch = new CountDownLatch(1);
        Thread thread1 = new Thread(() ->
                locker.execute("test1", () -> {
                    try {
                        thread1StartedLatch.countDown();
                        Thread.sleep(Long.MAX_VALUE);
                    } catch (InterruptedException ignored) {
                    }
                })
        );
        thread1.start();
        thread1StartedLatch.await();
        Thread thread2 = new Thread(() ->
                locker.execute("test2", () -> {
                })
        );
        thread2.start();
        thread2.join();
        thread1.interrupt();
        thread1.join();
    }

    @Test
    public void lockIsDeletedWhenNotUsedAnymore() throws Exception {
        EntityLocker<String> locker = new EntityLocker<>();
        Thread thread = new Thread(() ->
                locker.execute("test", () -> {
                })
        );
        thread.start();
        thread.join();
        assertEquals(0, locker.locksCount());
    }
}
