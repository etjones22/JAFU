package dev.jafu.client.config;

public interface JafuConfigurable {
    String configId();

    boolean load();

    boolean save();
}
