package dev.stellarclient.core.cloud;

import com.google.gson.JsonElement;
import dev.stellarclient.core.account.PrimeAccountService;

import java.util.List;
import java.util.Optional;

/**
 * Unused remote cloud adapter â€” product currently uses {@link LocalCloudClient} only.
 * Kept for a future optional remote sync backend.
 */
public final class RemoteCloudClient implements CloudClient {

    private final LocalCloudClient local;
    private final PrimeAccountService account;

    public RemoteCloudClient(LocalCloudClient local, PrimeAccountService account) {
        this.local = local;
        this.account = account;
    }

    @Override
    public boolean isAuthenticated() {
        return account.loggedIn();
    }

    @Override
    public Optional<String> accountId() {
        return account.loggedIn() ? Optional.of(account.username()) : Optional.empty();
    }

    @Override
    public void uploadConfig(String profileName, JsonElement config) {
        if (!account.loggedIn()) {
            return;
        }
        local.setAccountId(account.username());
        local.uploadConfig(profileName, config);
    }

    @Override
    public Optional<JsonElement> downloadConfig(String profileName) {
        if (!account.loggedIn()) {
            return Optional.empty();
        }
        local.setAccountId(account.username());
        return local.downloadConfig(profileName);
    }

    @Override
    public List<VersionEntry> listVersions(String profileName) {
        if (!account.loggedIn()) {
            return List.of();
        }
        return local.listVersions(profileName);
    }

    @Override
    public Optional<JsonElement> restoreVersion(String profileName, String versionId) {
        if (!account.loggedIn()) {
            return Optional.empty();
        }
        return local.restoreVersion(profileName, versionId);
    }
}
