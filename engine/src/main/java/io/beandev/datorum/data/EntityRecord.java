package io.beandev.datorum.data;

import io.beandev.datorum.schema.Entity;

public record EntityRecord(BigId id,
                           Entity entity, EntityRecord rootRecord,
                           String stringValue,
                           Integer integerValue,
                           Float floatValue,
                           Boolean booleanValue,
                           Long dateValue,
                           Long datetimeValue) {
    public EntityRecord(BigId id, Entity entity, EntityRecord rootValue, String stringValue) {
        this(id, entity, rootValue, stringValue, null, null, null, null, null);
    }

    public EntityRecord(BigId id, Entity entity, EntityRecord rootValue, Integer integerValue) {
        this(id, entity, rootValue, null, integerValue, null, null, null, null);
    }
}
