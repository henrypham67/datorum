package io.beandev.datorum.schema;

public record App(long id, String name) {
    public record Partition(long id, String name, App app, Context context, Aggregate aggregate) {
        public Partition(long id, String name, Aggregate aggregate) {
            this(id, name, aggregate.context().app(), aggregate.context(), aggregate);
        }
    }
}
