package io.beandev.datorum.repository.jooq;

import io.beandev.datorum.repository.ListCrudRepository;
import org.jooq.DSLContext;
import org.jooq.Query;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;

import static java.lang.System.out;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

public class SimpleJooqRepository<T, ID> implements ListCrudRepository<T, ID> {
    private final DataSource dataSource;

    private final Class<T> clazz;

    public SimpleJooqRepository(Class<T> clazz, DataSource ds) {
        this.clazz = clazz;
        this.dataSource = ds;

        out.println("Class name is " + this.clazz.getSimpleName());
    }

    @Override
    public T save(T entity) {
        return null;
    }

    @Override
    public <S extends T> List<S> saveAll(Iterable<S> entities) {
        return null;
    }

    @Override
    public List<T> findAll() {
        var ps = this.clazz.getPackageName().split("\\.");
        out.println("Package is " + ps);
        out.println("Package is " + ps[ps.length - 1]);
        // Connection is the only JDBC resource that we need
        // PreparedStatement and ResultSet are handled by jOOQ, internally
        try (Connection conn = this.dataSource.getConnection()) {
            DSLContext create = DSL.using(conn, SQLDialect.POSTGRES);
//            DSLContext create = DSL.using(ds, SQLDialect.POSTGRES);

// Fetch a SQL string from a jOOQ Query in order to manually execute it with another tool.
// For simplicity reasons, we're using the API to construct case-insensitive object references, here.
            Query query = create.select(field("book.title")
//                            , field("AUTHOR.FIRST_NAME"), field("AUTHOR.LAST_NAME")
                    )
                    .from(table("book"))
//                    .join(table("AUTHOR"))
//                    .on(field("BOOK.AUTHOR_ID").eq(field("AUTHOR.ID")))
//                    .where(field("BOOK.PUBLISHED_IN").eq(1948))
                    ;

            String sql = query.getSQL();
            List<Object> bindValues = query.getBindValues();

            var queryStmt = create.select(field("book.title"))
                    .from(table("book"));
            Result<?> result = queryStmt.fetch();
            result.stream().forEach(out::println);

            queryStmt.fetch().stream().forEach(title -> out.println("Title: " + title.value1()));

//            var col = queryStmt.fetch().stream().map(Record1::value1).toList();

        }
        // For the sake of this tutorial, let's keep exception handling simple
        catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public List<T> findAllById(Iterable<ID> ids) {
        return null;
    }
}
