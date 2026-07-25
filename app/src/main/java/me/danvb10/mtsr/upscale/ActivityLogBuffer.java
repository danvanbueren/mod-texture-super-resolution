package me.danvb10.mtsr.upscale;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Thread-safe bounded activity log for background upscale work.
 * Entries are returned oldest-first so callers can display newest entries last.
 */
public final class ActivityLogBuffer {

    private final int capacity;
    private final Deque<String> entries = new ArrayDeque<>();

    /** Creates a log buffer with the supplied maximum entry count. */
    public ActivityLogBuffer(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
    }

    /** Appends an entry, evicting the oldest entry when full. */
    public synchronized void append(String entry) {
        entries.addLast(entry);
        while (entries.size() > capacity) {
            entries.removeFirst();
        }
    }

    /** Returns a bounded snapshot in oldest-first order. */
    public synchronized List<String> snapshot() {
        return List.copyOf(new ArrayList<>(entries));
    }
}
