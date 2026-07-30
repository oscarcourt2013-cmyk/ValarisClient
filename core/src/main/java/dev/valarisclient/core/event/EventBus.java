package dev.valarisclient.core.event;

import dev.valarisclient.core.ValarisClient;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Typed event bus — no annotations, no reflection.
 *
 * <p>Dispatch resolves the exact event class to a pre-sorted listener array:
 * {@link #post} is a map lookup plus an indexed loop, allocation-free.
 * Listener arrays are rebuilt on (rare) subscription changes, so a listener
 * may safely unsubscribe itself while an event is being dispatched.</p>
 *
 * <p>Client-thread only, like everything in ValarisClient's core.</p>
 */
public final class EventBus {

    /** Handle returned by {@code subscribe}; call to stop receiving events. */
    @FunctionalInterface
    public interface Subscription {
        void unsubscribe();
    }

    private record Registration(int priority, Consumer<?> handler) {
    }

    private static final Registration[] NO_LISTENERS = new Registration[0];

    private final Map<Class<?>, Registration[]> listeners = new HashMap<>();

    /** Subscribes with default priority 0. */
    public <T> Subscription subscribe(Class<T> eventType, Consumer<T> handler) {
        return subscribe(eventType, 0, handler);
    }

    /**
     * Subscribes to {@code eventType}. Higher priority runs first; ties keep
     * subscription order.
     */
    public <T> Subscription subscribe(Class<T> eventType, int priority, Consumer<T> handler) {
        Registration registration = new Registration(priority, handler);
        Registration[] current = listeners.getOrDefault(eventType, NO_LISTENERS);
        Registration[] updated = Arrays.copyOf(current, current.length + 1);
        updated[current.length] = registration;
        // Stable sort: equal priorities keep insertion order.
        Arrays.sort(updated, (a, b) -> Integer.compare(b.priority, a.priority));
        listeners.put(eventType, updated);
        return () -> remove(eventType, registration);
    }

    /**
     * Dispatches {@code event} to listeners of its exact class.
     * One failing listener is logged and skipped; the others still run.
     */
    @SuppressWarnings("unchecked")
    public <T> void post(T event) {
        Registration[] registrations = listeners.get(event.getClass());
        if (registrations == null) {
            return;
        }
        for (int i = 0; i < registrations.length; i++) {
            try {
                ((Consumer<T>) registrations[i].handler).accept(event);
            } catch (RuntimeException e) {
                ValarisClient.LOGGER.error("Event listener failed for {}", event.getClass().getSimpleName(), e);
            }
        }
    }

    private void remove(Class<?> eventType, Registration registration) {
        Registration[] current = listeners.get(eventType);
        if (current == null) {
            return;
        }
        Registration[] updated = Arrays.stream(current)
                .filter(r -> r != registration)
                .toArray(Registration[]::new);
        if (updated.length == 0) {
            listeners.remove(eventType);
        } else {
            listeners.put(eventType, updated);
        }
    }
}
