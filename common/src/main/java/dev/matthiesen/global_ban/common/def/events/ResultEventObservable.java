package dev.matthiesen.global_ban.common.def.events;

import dev.matthiesen.matthiesen_core.common.api.events.EventPriority;
import dev.matthiesen.matthiesen_core.common.api.events.EventSubscription;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class ResultEventObservable<T, R> {
    private static final Logger LOGGER = LogManager.getLogger(ResultEventObservable.class);
    private final CopyOnWriteArrayList<Entry<T, R>> entries = new CopyOnWriteArrayList<>();
    private final AtomicLong sequenceCounter = new AtomicLong();

    public ResultEventObservable() {}

    public EventSubscription subscribe(ResultListener<T, R> listener) {
        return subscribe(EventPriority.NORMAL, listener);
    }

    public EventSubscription subscribe(EventPriority priority, ResultListener<T, R> listener) {
        AtomicBoolean active = new AtomicBoolean(true);
        entries.add(new Entry<>(priority.getValue(), sequenceCounter.getAndIncrement(), listener, active));
        return EventSubscription.of(active);
    }

    public R emit(T event) {
        List<Entry<T, R>> snapshot = new ArrayList<>(entries.size());
        List<Entry<T, R>> dead = new ArrayList<>();

        for (Entry<T, R> e : entries) {
            if (e.active().get()) {
                snapshot.add(e);
            } else {
                dead.add(e);
            }
        }

        if (!dead.isEmpty()) {
            entries.removeAll(dead);
        }

        snapshot.sort(Comparator.comparingInt((Entry<T, R> a) -> a.priority()).thenComparingLong(Entry::sequence));

        for (Entry<T, R> entry : snapshot) {
            try {
                R result = entry.listener().handle(event);
                if (result != null) {
                    return result;
                }
            } catch (Throwable t) {
                LOGGER.error("[PlatformEvents] Unhandled exception in result listener for event '{}': {}",
                        event.getClass().getSimpleName(), t.getMessage(), t);
            }
        }

        return null;
    }

    @FunctionalInterface
    public interface ResultListener<T, R> {
        R handle(T event);
    }

    private record Entry<E, R>(int priority, long sequence, ResultListener<E, R> listener, AtomicBoolean active) {}
}
