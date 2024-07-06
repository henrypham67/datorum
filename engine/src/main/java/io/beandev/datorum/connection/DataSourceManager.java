package io.beandev.datorum.connection;

import javax.sql.DataSource;
import java.util.Map;

public class DataSourceManager {
    private final Map<String, DataSource> dataSourceMap;

    public DataSourceManager(Map<String, DataSource> dsm) {
        dataSourceMap = dsm;
    }

    public DataSourceManager putDataSource(String name, DataSource dataSource) {
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null");
        }

        if (dataSource == null) {
            throw new IllegalArgumentException("dataSource cannot be null");
        }

        dataSourceMap.put(name, dataSource);
        return this;
    }

    public DataSource getDataSource(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null");
        }

        return dataSourceMap.get(name);
    }
}
