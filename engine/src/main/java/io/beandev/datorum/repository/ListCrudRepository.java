package io.beandev.datorum.repository;

import java.util.List;

public interface ListCrudRepository<T, ID> extends CrudRepository<T, ID> {

    /**
     * Saves all given entities.
     *
     * @param entities must not be {@literal null} nor must it contain {@literal null}.
     * @return the saved entities; will never be {@literal null}. The returned {@literal Iterable} will have the same size
     *         as the {@literal Iterable} passed as an argument.
     * @throws IllegalArgumentException in case the given {@link Iterable entities} or one of its entities is
     *           {@literal null}.
     * @throws OptimisticLockingFailureException when at least one entity uses optimistic locking and has a version
     *           attribute with a different value from that found in the persistence store. Also thrown if at least one
     *           entity is assumed to be present but does not exist in the database.
     */
    <S extends T> List<S> saveAll(Iterable<S> entities);

    /**
     * Returns all instances of the type.
     *
     * @return all entities
     */
    List<T> findAll();

    /**
     * Returns all instances of the type {@code T} with the given IDs.
     * <p>
     * If some or all ids are not found, no entities are returned for these IDs.
     * <p>
     * Note that the order of elements in the result is not guaranteed.
     *
     * @param ids must not be {@literal null} nor contain any {@literal null} values.
     * @return guaranteed to be not {@literal null}. The size can be equal or less than the number of given
     *         {@literal ids}.
     * @throws IllegalArgumentException in case the given {@link Iterable ids} or one of its items is {@literal null}.
     */
    List<T> findAllById(Iterable<ID> ids);

}