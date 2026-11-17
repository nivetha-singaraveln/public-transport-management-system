package com.vcet.transport.dao;

import com.vcet.transport.exception.TransportException;
import java.util.List;
import java.util.Optional;

/**
 * Generic DAO contract (Generics unit).
 * @param <T>  entity type
 * @param <ID> primary key type
 */
public interface CrudDao<T, ID> {
    T add(T entity) throws TransportException;
    Optional<T> findById(ID id) throws TransportException;
    List<T> findAll() throws TransportException;
    boolean update(T entity) throws TransportException;
    boolean delete(ID id) throws TransportException;
}
