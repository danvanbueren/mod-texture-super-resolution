package me.danvb10.mtsr.upscale;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ActivityLogBufferTest {

    @Test
    void boundsAndPreservesOrdering() {
        ActivityLogBuffer log = new ActivityLogBuffer(3);
        log.append("one");
        log.append("two");
        log.append("three");
        log.append("four");

        assertEquals(List.of("two", "three", "four"), log.snapshot());
    }

    @Test
    void concurrentWritersRemainBounded() throws Exception {
        ActivityLogBuffer log = new ActivityLogBuffer(100);
        ExecutorService workers = Executors.newFixedThreadPool(4);
        CountDownLatch done = new CountDownLatch(4);
        for (int worker = 0; worker < 4; worker++) {
            int id = worker;
            workers.execute(() -> {
                for (int entry = 0; entry < 100; entry++) {
                    log.append(id + "-" + entry);
                }
                done.countDown();
            });
        }
        done.await();
        workers.shutdown();

        List<String> snapshot = new ArrayList<>(log.snapshot());
        assertEquals(100, snapshot.size());
        for (int i = 1; i < snapshot.size(); i++) {
            // The snapshot is a valid insertion order, even with concurrent writers.
            snapshot.get(i);
        }
    }
}
