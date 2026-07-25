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
    private long sequence;

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
        sequence++;
    }

    /** Returns a bounded snapshot in oldest-first order. */
    public synchronized List<String> snapshot() {
        return List.copyOf(new ArrayList<>(entries));
    }

    /** Returns a bounded snapshot and its change sequence. */
    public synchronized Snapshot snapshot(int maximumEntries) {
        int skip = Math.max(0, entries.size() - maximumEntries);
        List<String> result = new ArrayList<>(entries);
        return new Snapshot(sequence, List.copyOf(result.subList(skip, result.size())));
    }

    /** Returns the current change sequence without copying entries. */
    public synchronized long sequence() {
        return sequence;
    }

    /** Immutable log snapshot with its corresponding sequence number. */
    public record Snapshot(long sequence, List<String> entries) {
    }
}
