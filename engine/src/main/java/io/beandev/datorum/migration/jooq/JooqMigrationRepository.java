package io.beandev.datorum.migration.jooq;

import io.beandev.datorum.migration.AggregateSnapshot;
import io.beandev.datorum.migration.Migration;
import io.beandev.datorum.migration.MigrationRepository;
import io.beandev.datorum.schema.Aggregate;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

public class JooqMigrationRepository implements MigrationRepository {

    private final DataSource dataSource;

    public JooqMigrationRepository(DataSource ds) {
        dataSource = ds;
    }

    @Override
    public Migration save(Migration migration) {
        try (var conn = dataSource.getConnection()) {
            // Initialize the DSLContext
            DSLContext create = DSL.using(conn, SQLDialect.POSTGRES);

            // Start the transaction
            create.transaction(configuration -> {
                DSLContext ctx = DSL.using(configuration);
                ctx.insertInto(
                        table("datorum_schema.migration"),
                        field("parent_id"),
                        field("id"),
                        field("previous_parent_id"),
                        field("previous_id"),
                        field("hash"),
                        field("status")
                ).values(
                        migration.parentId(),
                        migration.id(),
                        migration.previousMigrationParentId(),
                        migration.previousMigrationId(),
                        migration.hash(),
                        migration.status()
                ).execute();

                Arrays.stream(migration.differences()).forEach(difference -> {
                    ctx.insertInto(
                            table("datorum_schema.difference"),
                            field("id"),
                            field("migration_parent_id"),
                            field("migration_id"),
                            field("name"),
                            field("scope"),
                            field("action"),
                            field("data_type"),
                            field("relation_entity_id"),
                            field("owner_entity_id")
                    ).values(
                            difference.id(),
                            migration.parentId(),
                            migration.id(),
                            difference.name(),
                            difference.scope().name(),
                            difference.action().name(),
                            difference.dataTypeName(),
                            difference.relationEntityId(),
                            difference.ownerEntityId()
                    ).execute();
                });
            });

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return migration;
    }

    @Override
    public void createBaseTables() {
        try (var conn = dataSource.getConnection()) {
            // Initialize the DSLContext
            DSLContext create = DSL.using(conn, SQLDialect.POSTGRES);

            // Start the transaction
            create.transaction(configuration -> {
                DSLContext ctx = DSL.using(configuration);

                Result<Record> result = ctx.select()
                        .from(table("pg_catalog.pg_type"))
                        .join(table("pg_catalog.pg_namespace"))
                        .on(field("pg_type.typnamespace").eq(field("pg_namespace.oid")))
                        .where(field("typname").eq("_scopeenum"))
                        .and(field("pg_namespace.nspname").eq("datorum_schema"))
                        .and(field("pg_type.typtype").eq("b"))
                        .fetch();

                if (result.stream().count() > 0) {
                    return;
                }

                ctx.execute("""
                            CREATE TYPE datorum_schema.ScopeEnum AS ENUM(
                                'APP',
                                'CONTEXT',
                                'AGGREGATE',
                                'ENTITY',
                                'ATTRIBUTE'
                            );
                            
                            CREATE TYPE datorum_schema.DifferenceActionEnum AS ENUM (
                                'CREATE',
                                'RENAME',
                                'CHANGE_ATTRIBUTE_OWNER',
                                'CHANGE_DATA_TYPE',
                                'INCREASE',
                                'DECREASE',
                                'DELETE'
                            );
                            
                            CREATE TYPE datorum_schema.DifferenceType AS (
                                id        BIGINT,
                                name      VARCHAR(255),
                                scope     datorum_schema.ScopeEnum,
                                action    datorum_schema.DifferenceActionEnum,
                                data_type datorum_schema.DataType,
                                relation  datorum_schema.EntityType,
                                owner     datorum_schema.EntityType
                            );
                            
                            CREATE TYPE datorum_schema.StatusEnum AS ENUM (
                                'IN_PROGRESS',
                                'COMPLETED',
                                'FAILED',
                                'PENDING'
                            );
                        """);

                ctx.execute("""
                            CREATE TABLE IF NOT EXISTS datorum_schema.migration (
                                parent_id              BIGINT,
                                id                     BIGINT,
                                previous_parent_id     BIGINT, 
                                previous_id            BIGINT,
                                hash                   VARCHAR(40),
                                status                 datorum_schema.StatusEnum,
                                PRIMARY KEY (parent_id, id),
                                FOREIGN KEY (previous_parent_id, previous_id) REFERENCES datorum_schema.migration(parent_id, id)                                
                            );
                            
                            CREATE TABLE IF NOT EXISTS datorum_schema.difference (
                                id                     BIGINT PRIMARY KEY,
                                migration_parent_id    BIGINT,
                                migration_id           BIGINT,
                                name                VARCHAR(255),
                                scope               datorum_schema.ScopeEnum,
                                action              datorum_schema.DifferenceActionEnum,
                                data_type           datorum_schema.DataType,
                                relation_entity_id  BIGINT,
                                owner_entity_id     BIGINT,                            
                                FOREIGN KEY (migration_parent_id, migration_id) REFERENCES datorum_schema.migration(parent_id, id)                                
                            );
                            
                            CREATE INDEX difference__migration_id ON datorum_schema.difference (migration_parent_id, migration_id);
                            
                            INSERT INTO datorum_schema.system_info (property_name, property_value) 
                            VALUES ('migration.version', 'v1.0.0');
                        """);
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param latestMigration
     * @return
     */
    @Override
    public Stream<Migration> forEachPastMigrations(Migration latestMigration) {
        return null;
    }

    /**
     * @param aggregate
     * @return
     */
    @Override
    public AggregateSnapshot findAggregateSnapshot(Aggregate aggregate) {
        return null;
    }

    @Override
    public Stream<Migration> findByAggregate(Aggregate aggregate) {
        return null;
    }
}
