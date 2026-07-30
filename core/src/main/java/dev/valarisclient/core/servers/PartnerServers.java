package dev.valarisclient.core.servers;

import java.util.List;
import java.util.Locale;

/**
 * Partner multiplayer servers.
 *
 * <p>Nothing is injected into the player's server list any more: the list is
 * theirs. {@link #LEGACY_ADDRESSES} names entries a previous build pinned there
 * so they can be cleaned up on load.</p>
 */
public final class PartnerServers {

    public record Entry(String name, String address) {
    }

    private static final List<Entry> PARTNERS = List.of();

    /** Addresses older builds injected and blocked from deletion. */
    private static final List<String> LEGACY_ADDRESSES = List.of("elysiasmp.fr");

    private PartnerServers() {
    }

    /** True for a server a previous build pinned into the vanilla list. */
    public static boolean isLegacyInjectedAddress(String address) {
        if (address == null || address.isBlank()) {
            return false;
        }
        String normalized = normalize(address);
        for (String legacy : LEGACY_ADDRESSES) {
            if (normalized.equals(legacy) || normalized.startsWith(legacy + ":")) {
                return true;
            }
        }
        return false;
    }

    public static List<Entry> partners() {
        return PARTNERS;
    }

    public static boolean isPartnerAddress(String address) {
        if (address == null || address.isBlank()) {
            return false;
        }
        String normalized = normalize(address);
        for (Entry entry : PARTNERS) {
            if (normalized.equals(normalize(entry.address()))
                    || normalized.startsWith(normalize(entry.address()) + ":")) {
                return true;
            }
        }
        return false;
    }

    /** Display name used when injecting into the vanilla list. */
    public static String displayName(Entry entry) {
        return entry.name() + " ★";
    }

    /**
     * Nice label for Discord / UI when the address matches a partner
     * (without the list star), or {@code null} if unknown.
     */
    public static String partnerLabel(String address) {
        if (address == null || address.isBlank()) {
            return null;
        }
        String normalized = normalize(address);
        for (Entry entry : PARTNERS) {
            String partner = normalize(entry.address());
            if (normalized.equals(partner) || normalized.startsWith(partner + ":")) {
                return entry.name();
            }
        }
        return null;
    }

    private static String normalize(String address) {
        String a = address.trim().toLowerCase(Locale.ROOT);
        if (a.startsWith("minecraft://")) {
            a = a.substring("minecraft://".length());
        }
        int slash = a.indexOf('/');
        if (slash >= 0) {
            a = a.substring(0, slash);
        }
        return a;
    }
}
