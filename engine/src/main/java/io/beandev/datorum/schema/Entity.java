package io.beandev.datorum.schema;

public record Entity(long id, String name, Aggregate aggregate, boolean isRoot) {
    public Entity(long id, String name, Aggregate aggregate) {
        this(id, name, aggregate, false);
        if (aggregate == null) {
            throw new IllegalArgumentException("Aggregate cannot be null");
        }
    }
}
