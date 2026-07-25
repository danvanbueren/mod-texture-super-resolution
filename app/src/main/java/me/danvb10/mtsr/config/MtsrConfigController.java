package me.danvb10.mtsr.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Applies validated configuration mutations and persists them immediately.
 * This class is independent of Minecraft widgets for unit testing.
 */
public final class MtsrConfigController {

    private static final Logger LOGGER = LoggerFactory.getLogger("mtsr");

    private final MtsrConfig config;
    private final MtsrConfigStore store;

    public MtsrConfigController(MtsrConfig config, MtsrConfigStore store) {
        this.config = config;
        this.store = store;
    }

    /** Applies a mutation, validates it, and persists the result. */
    public boolean update(Consumer<MtsrConfig> mutation) {
        mutate(mutation);
        return persist();
    }

    /** Applies a mutation and validates it without writing to disk. */
    public void mutate(Consumer<MtsrConfig> mutation) {
        mutation.accept(config);
        config.validate();
    }

    /** Persists the current validated configuration. */
    public boolean persist() {
        try {
            store.save(config);
            return true;
        } catch (IOException e) {
            LOGGER.warn("Failed to persist configuration", e);
            return false;
        }
    }

    /** Adds a trimmed namespace exclusion and persists it. */
    public boolean addExcludedNamespace(String namespace) {
        String trimmed = namespace == null ? "" : namespace.trim();
        if (trimmed.isBlank()) {
            return false;
        }
        Set<String> namespaces = new java.util.LinkedHashSet<>(config.extraExcludedNamespaces());
        namespaces.add(trimmed);
        return update(value -> value.extraExcludedNamespaces(namespaces));
    }

    /** Removes a namespace exclusion and persists it. */
    public boolean removeExcludedNamespace(String namespace) {
        Set<String> namespaces = new java.util.LinkedHashSet<>(config.extraExcludedNamespaces());
        if (!namespaces.remove(namespace)) {
            return false;
        }
        return update(value -> value.extraExcludedNamespaces(namespaces));
    }
}
