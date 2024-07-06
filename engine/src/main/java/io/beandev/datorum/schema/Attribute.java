package io.beandev.datorum.schema;

public record Attribute(long id, String name,
                        DataType type,
                        Entity relation, Entity entity,
                        Migration migration,

                        boolean isNullable,

                        boolean isUnique,
                        boolean isActive) {

    public Attribute(long id, String name, DataType type, Entity entity) {
        this(id, name, type, null, entity, null, true, false, true);
    }

    public record DataType(Type type, Integer precisionOrLength, Integer scale) {
        public DataType(Type type) {
            this(type, null, null);
        }

        public DataType(Type type, Integer length) {
            this(type, length, null);
        }

        public enum Type {
            BOOLEAN,
            INTEGER,
            LONG,
            FLOAT,
            DOUBLE,
            BYTES,
            DECIMAL,
            STRING,
            RELATION,
            MAP
        }
    }

    public record Migration(long id, Attribute from, Attribute to, boolean isActive) {
    }
}
