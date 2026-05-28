package dev.jafu.client.config;

import java.util.ArrayList;
import java.util.List;

public final class JafuConfigManager {
    private static final List<JafuConfigurable> CONFIGS = new ArrayList<>();

    private JafuConfigManager() {
    }

    public static synchronized void register(JafuConfigurable config) {
        for (JafuConfigurable existing : CONFIGS) {
            if (existing.configId().equals(config.configId())) {
                return;
            }
        }
        CONFIGS.add(config);
    }

    public static synchronized ConfigOperationResult reloadAll() {
        int failed = 0;
        for (JafuConfigurable config : CONFIGS) {
            try {
                if (!config.load()) {
                    failed++;
                }
            } catch (RuntimeException ignored) {
                failed++;
            }
        }
        return new ConfigOperationResult(CONFIGS.size(), failed);
    }

    public static synchronized ConfigOperationResult saveAll() {
        int failed = 0;
        for (JafuConfigurable config : CONFIGS) {
            try {
                if (!config.save()) {
                    failed++;
                }
            } catch (RuntimeException ignored) {
                failed++;
            }
        }
        return new ConfigOperationResult(CONFIGS.size(), failed);
    }

    public static synchronized int registeredCount() {
        return CONFIGS.size();
    }

    public record ConfigOperationResult(int total, int failed) {
        public boolean successful() {
            return failed == 0;
        }

        public int savedOrLoaded() {
            return total - failed;
        }
    }
}
