package dev.valerisclient.core.gui.options;

import java.util.List;

/**
 * One screen's worth of options. Every Valeris options screen -- the hub and
 * each sub-screen -- is just a title plus a list of {@link OptionRow}s.
 */
public record OptionsPage(String title, List<OptionRow> rows) {

    public OptionsPage(String title, List<OptionRow> rows) {
        this.title = title;
        this.rows = List.copyOf(rows);
    }
}
