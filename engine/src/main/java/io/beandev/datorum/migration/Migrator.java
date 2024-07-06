package io.beandev.datorum.migration;

import java.util.Arrays;

public class Migrator {
    private final MigrationRepository repository;

    public Migrator(MigrationRepository repository) {
        this.repository = repository;
    }

    public void apply(Migration migration) {
        Arrays.stream(migration.differences()).forEach(difference -> {
            System.out.println(difference);
            Scope scope = difference.scope();
            switch (scope) {
                case Scope.APP:
                    System.out.println("APP");
                    break;
                case Scope.CONTEXT:
                    System.out.println("CONTEXT");
                    break;
                case Scope.AGGREGATE:
                    System.out.println("AGGREGATE");
                    break;
                case Scope.ENTITY:
                    System.out.println("ENTITY");
                    break;
                case Scope.ATTRIBUTE:
                    System.out.println("ATTRIBUTE");
                default:
                    break;
            }
            difference.id();
            Difference.Action type = difference.action();
            switch (type) {
                case Difference.Action.CREATE -> System.out.println("CREATE");
                case Difference.Action.RENAME -> System.out.println("RENAME");
                case Difference.Action.CHANGE_ATTRIBUTE_OWNER -> System.out.println("ATTRIBUTE_OWNER");
                case Difference.Action.CHANGE_DATA_TYPE -> System.out.println("DATA_TYPE");
            }
        });
        repository.save(migration);
    }

    void applyAll(Migration[] migrations) {
    }

    public Progress progress() {
//        var migration = new Migration(
//                new Command[]{
//                        new Command(
//                                new AggregateCommand(
//                                        new Aggregate(
//                                                1,
//                                                "Aggregate",
//                                                new Context(
//                                                        1,
//                                                        "Context",
//                                                        new App(
//                                                                1,
//                                                                "App"
//                                                        )
//                                                )
//                                        )
//                                )
//                        )
//                }
//        );
//        Arrays.stream(migration.commands()).forEach(command -> {
//            System.out.println(command.aggregate());
//        });
        return new Progress();
    }
}
