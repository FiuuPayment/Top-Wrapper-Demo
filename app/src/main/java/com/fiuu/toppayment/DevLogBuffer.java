package com.fiuu.toppayment;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Session-only, in-memory backing store for the developer log panel. Shared by every mounted panel instance. */
public class DevLogBuffer {

    public interface Listener {
        void onAppended(Entry entry);
        void onCleared();
    }

    public static class Entry {
        public final long timestamp;
        public final String tag;
        public final String message;

        Entry(long timestamp, String tag, String message) {
            this.timestamp = timestamp;
            this.tag = tag;
            this.message = message;
        }
    }

    // Bounds memory during a long test session; oldest lines are dropped first.
    private static final int MAX_ENTRIES = 300;

    private static final DevLogBuffer instance = new DevLogBuffer();

    public static DevLogBuffer getInstance() {
        return instance;
    }

    private final Deque<Entry> entries = new ArrayDeque<>();
    private final List<Listener> listeners = new ArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private DevLogBuffer() {}

    public synchronized void append(String tag, String message) {
        Entry entry = new Entry(System.currentTimeMillis(), tag, message == null ? "null" : message);
        entries.addLast(entry);
        while (entries.size() > MAX_ENTRIES) {
            entries.removeFirst();
        }
        notifyAppended(entry);
    }

    public synchronized void clear() {
        entries.clear();
        notifyCleared();
    }

    public synchronized List<Entry> getAll() {
        return new ArrayList<>(entries);
    }

    public void addListener(Listener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public void removeListener(Listener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    private void notifyAppended(Entry entry) {
        mainHandler.post(() -> {
            for (Listener listener : snapshotListeners()) {
                listener.onAppended(entry);
            }
        });
    }

    private void notifyCleared() {
        mainHandler.post(() -> {
            for (Listener listener : snapshotListeners()) {
                listener.onCleared();
            }
        });
    }

    private List<Listener> snapshotListeners() {
        synchronized (listeners) {
            return new ArrayList<>(listeners);
        }
    }
}
