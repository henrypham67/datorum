package io.beandev.datorum.migration;

public record Migration(
        long parentId,
        long id,
        Migration previousMigration,
        Difference[] differences,
        String hash,
        Status status) {
    public Migration(
            long parentId,
            long id,
            Difference[] differences) {
        this(parentId, id,
                null, differences,
                null, null);
    }

    public boolean isCompatibleWith(Migration[] siblings) {

        return true;
    }

    public Long previousMigrationParentId() {
        return previousMigration == null ? null : previousMigration.parentId();
    }

    public Long previousMigrationId() {
        return previousMigration == null ? null : previousMigration.id();
    }

//    public Migration {
//        if (commands.length == 0) {
//            throw new IllegalArgumentException("commands must not be empty");
//        }
//
//        if (aggregate != null && !commands[0].aggregate().equals(aggregate)) {
//            throw new IllegalArgumentException("Aggregate from commands does not match the input aggregate");
//        }
//
//        var firstAggregate = commands[0].aggregate();
//
//        Arrays.stream(commands).forEach(
//                command -> {
//                    if (!command.aggregate().equals(firstAggregate)) {
//                        throw new IllegalArgumentException("Commands do not have the same aggregate, expected " + firstAggregate + " but got " + command.aggregate());
//                    }
//                }
//        );
//
//        aggregate = firstAggregate;
//
//        // Convert the array of objects to JSON string
//        ObjectMapper objectMapper = new ObjectMapper();
//        String json;
//        try {
//            String commandsJson = objectMapper.writeValueAsString(commands);
//            json = objectMapper.writeValueAsString(new String[]{
//                    previousMigration == null ? "" : previousMigration.hash(),
//                    commandsJson
//            });
//        } catch (JsonProcessingException e) {
//            throw new RuntimeException(e);
//        }
//
//        // Create MessageDigest instance for SHA-1
//        MessageDigest shaDigest = null;
//        try {
//            shaDigest = MessageDigest.getInstance("SHA-1");
//        } catch (NoSuchAlgorithmException e) {
//            throw new RuntimeException(e);
//        }
//
//        // Apply SHA-1 Message Digest
//        byte[] result = shaDigest.digest(json.getBytes());
//
//        StringBuilder sb = new StringBuilder();
//        for (byte b : result) {
//            sb.append(String.format("%02x", b));
//        }
//        hash = sb.toString();
//    }
//
//    public Migration(Command[] commands) {
//        this(0, null, commands, null, null);
//    }
//
//    public Migration(Migration previousMigration, Command[] commands) {
//        this(0, previousMigration, commands, null, null);
//    }
//
//    public Migration(long id, Migration previousMigration, Command[] commands) {
//        this(id, previousMigration, commands, null, null);
//    }
}
