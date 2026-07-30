package dev.valarisclient.core.event;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventBusTest {

    private record TestEvent(String payload) {
    }

    @Test
    void postReachesSubscribersOfExactType() {
        EventBus bus = new EventBus();
        AtomicInteger received = new AtomicInteger();
        bus.subscribe(TestEvent.class, e -> received.incrementAndGet());

        bus.post(new TestEvent("a"));
        bus.post("not a TestEvent");
        assertEquals(1, received.get());
    }

    @Test
    void higherPriorityRunsFirstTiesKeepOrder() {
        EventBus bus = new EventBus();
        List<String> order = new ArrayList<>();
        bus.subscribe(TestEvent.class, 0, e -> order.add("low-1"));
        bus.subscribe(TestEvent.class, 10, e -> order.add("high"));
        bus.subscribe(TestEvent.class, 0, e -> order.add("low-2"));

        bus.post(new TestEvent("x"));
        assertEquals(List.of("high", "low-1", "low-2"), order);
    }

    @Test
    void unsubscribeStopsDelivery() {
        EventBus bus = new EventBus();
        AtomicInteger received = new AtomicInteger();
        EventBus.Subscription subscription = bus.subscribe(TestEvent.class, e -> received.incrementAndGet());

        bus.post(new TestEvent("a"));
        subscription.unsubscribe();
        bus.post(new TestEvent("b"));
        assertEquals(1, received.get());
    }

    @Test
    void listenerMayUnsubscribeItselfDuringDispatch() {
        EventBus bus = new EventBus();
        AtomicInteger first = new AtomicInteger();
        AtomicInteger second = new AtomicInteger();

        EventBus.Subscription[] handle = new EventBus.Subscription[1];
        handle[0] = bus.subscribe(TestEvent.class, e -> {
            first.incrementAndGet();
            handle[0].unsubscribe();
        });
        bus.subscribe(TestEvent.class, e -> second.incrementAndGet());

        bus.post(new TestEvent("a"));
        bus.post(new TestEvent("b"));

        assertEquals(1, first.get());
        assertEquals(2, second.get());
    }

    @Test
    void failingListenerDoesNotBlockOthers() {
        EventBus bus = new EventBus();
        AtomicInteger received = new AtomicInteger();
        bus.subscribe(TestEvent.class, 10, e -> {
            throw new IllegalStateException("boom");
        });
        bus.subscribe(TestEvent.class, 0, e -> received.incrementAndGet());

        bus.post(new TestEvent("a"));
        assertEquals(1, received.get());
    }
}
