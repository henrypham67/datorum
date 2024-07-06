package io.beandev.datorum.schema;

public record Aggregate(long id, String name, Context context, Version version)  {
    public Aggregate(long id, String name, Context context) {
        this(id, name, context, new Version(1, 0, 0));
    }
}
