package io.beandev.datorum.migration;

import io.beandev.datorum.repository.CrudRepository;
import io.beandev.datorum.schema.Aggregate;
import io.beandev.datorum.data.BigId;

import java.util.stream.Stream;

public interface MigrationRepository extends CrudRepository<Migration, BigId> {
    void createBaseTables();

    Stream<Migration> forEachPastMigrations(Migration latestMigration);

    AggregateSnapshot findAggregateSnapshot(Aggregate aggregate);

    Stream<Migration> findByAggregate(Aggregate aggregate);
}
