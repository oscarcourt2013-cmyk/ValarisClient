package dev.valerisclient.core.cloud;

import com.google.gson.JsonElement;

import java.util.List;
import java.util.Optional;

/**
 * Config backup contract. v1 ships a local on-disk store under {@code cloud/};
 * swap the implementation for a remote API later without changing callers.
 */
public interface CloudClient {

    record VersionEntry(String id, String label, long timestampMillis) {
    }

    boolean isAuthenticated();

    Optional<String> accountId();

    void uploadConfig(String profileName, JsonElement config);

    Optional<JsonElement> downloadConfig(String profileName);

    List<VersionEntry> listVersions(String profileName);

    Optional<JsonElement> restoreVersion(String profileName, String versionId);
}
