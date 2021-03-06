package com.ten.ware.dbsplit.core;

import java.util.List;

/**
 * 更上层的API，只需要操作bean
 */
public interface SimpleSplitJdbcOperations extends SplitJdbcOperations {

    <K, T> void insert(K splitKey, T bean);

    <K, T> void update(K splitKey, T bean);

    <K, T> void delete(K splitKey, long id, Class<T> clazz);

    <K, T> T get(K splitKey, long id, final Class<T> clazz);

    <K, T> T get(K splitKey, String key, String value,
                 final Class<T> clazz);

    <K, T> List<T> search(K splitKey, T bean);

    <K, T> List<T> search(K splitKey, T bean, String name,
                          Object valueFrom, Object valueTo);

    <K, T> List<T> search(K splitKey, T bean, String name, Object value);
}
