package io.beandev.datorum.migration;

import io.beandev.datorum.schema.Attribute;

public record Difference(long id,
                         String name,
                         Scope scope,
                         Action action,
                         Attribute.DataType dataType,
                         Long relationEntityId,
                         Long ownerEntityId) {

    public Difference {
        if (scope == null) {
            throw new IllegalArgumentException("Scope cannot be null");
        }
        if (action == null) {
            throw new IllegalArgumentException("Action cannot be null");
        }

        if (action == Action.CHANGE_DATA_TYPE && dataType == null) {
            throw new IllegalArgumentException("DataAction cannot be null");
        }

        if (dataType != null &&
                dataType.type() == Attribute.DataType.Type.RELATION &&
                relationEntityId == null) {
            throw new IllegalArgumentException("RelationEntityId cannot be null");
        }

        if (action == Action.CHANGE_ATTRIBUTE_OWNER && ownerEntityId == null) {
            throw new IllegalArgumentException("AttributeOwner cannot be null");
        }

        if (action == Action.RENAME && name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
    }

    public Difference(long id, String name, Scope scope, Action action) {
        this(id, name, scope, action, null, null, null);
    }

    public Difference(long id, String name, Scope scope) {
        this(id, name, scope, Action.CREATE, null, null, null);
    }

    public String dataTypeName() {
        return dataType == null ? null : dataType.type().name();
    }

    public enum Action {
        CREATE,
        RENAME,
        CHANGE_ATTRIBUTE_OWNER,
        CHANGE_DATA_TYPE,
        INCREASE,
        DECREASE,
        DELETE
    }
}
