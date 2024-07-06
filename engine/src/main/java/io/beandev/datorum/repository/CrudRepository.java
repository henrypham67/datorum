package io.beandev.datorum.repository;

public interface CrudRepository<T, ID> extends Repository<T, ID> {
    T save(T entity);
}
