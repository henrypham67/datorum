package io.beandev.datorum.migration;

import io.beandev.datorum.repository.CrudRepository;
import io.beandev.datorum.data.BigId;

interface AggregateMigrationRepository extends CrudRepository<AggregateMigration, BigId> {
}
