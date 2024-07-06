package io.beandev.datorum.migration;

import io.beandev.datorum.schema.Aggregate;

public record AggregateMigration(Aggregate aggregate, Migration migration) {}
