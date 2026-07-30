package dev.valarisclient.core.gui.options;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

/**
 * One row on a Valaris options page.
 *
 * <p>Rows are declarative: the version layer supplies getters and setters that
 * close over Minecraft's {@code OptionInstance}s, so {@code core} styles and
 * drives every options screen without referencing a Minecraft class.</p>
 */
public sealed interface OptionRow {

    String label();

    /** Whether the row can be interacted with (greyed out when false). */
    default boolean enabled() {
        return true;
    }

    /** On/off switch. */
    record Toggle(String label, BooleanSupplier get, Consumer<Boolean> set) implements OptionRow {
    }

    /**
     * Advances through a fixed set of values. {@code display} renders the current
     * one, so the version layer keeps ownership of the value type and its name.
     */
    record Cycle(String label, Supplier<String> display, Runnable next) implements OptionRow {
    }

    /**
     * Continuous value expressed as a 0..1 fraction, with its own display text
     * (so "Normal", "70", "Off" etc. stay the version layer's business).
     */
    record Slider(String label, Supplier<String> display,
                  DoubleSupplier fraction, DoubleConsumer setFraction) implements OptionRow {
    }

    /** Navigates somewhere: a sub-page, or a screen we have not restyled yet. */
    record Link(String label, Runnable action, boolean stillVanilla) implements OptionRow {
        public Link(String label, Runnable action) {
            this(label, action, false);
        }
    }

    /** Read-only value. */
    record Info(String label, Supplier<String> value) implements OptionRow {
        @Override
        public boolean enabled() {
            return false;
        }
    }
}
