/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

// Vendored/copied from hosted-data-services/insight-db-common
package com.sonatype.insight.dataaccess;

import java.util.List;

import jakarta.persistence.LockModeType;
import jakarta.persistence.NoResultException;

public abstract class AbstractDAO<T>
{
  public class Query<R>
  {
    private String sQuery;

    private Object[] parameters;

    private LockModeType lockModeType;

    private Integer maxResults;

    public Query(String sQuery, Object... parameters) {
      this.sQuery = sQuery;
      this.parameters = parameters;
    }

    public Query<R> setLockModeType(LockModeType lockModeType) {
      this.lockModeType = lockModeType;
      return this;
    }

    public Query<R> setMaxResults(int maxResults) {
      this.maxResults = maxResults;
      return this;
    }

    public Query<R> forceSingleResult() {
      return setMaxResults(1);
    }

    private jakarta.persistence.Query createQuery(TransactionContext tx) {
      jakarta.persistence.Query jpaQuery = AbstractDAO.this.createQuery(tx, sQuery, parameters);
      if (maxResults != null) {
        jpaQuery.setMaxResults(maxResults);
      }
      if (lockModeType != null) {
        jpaQuery.setLockMode(lockModeType);
      }
      return jpaQuery;
    }

    public R get() {
      try (TransactionContext tx = createReadOnlyTransactionContext()) {
        return get(tx);
      }
    }

    @SuppressWarnings("unchecked")
    public R get(TransactionContext tx) {
      jakarta.persistence.Query jpaQuery = createQuery(tx);
      try {
        return (R) jpaQuery.getSingleResult();
      }
      catch (NoResultException ignored) {
        return null;
      }
    }

    public List<R> getList() {
      try (TransactionContext tx = createReadOnlyTransactionContext()) {
        return getList(tx);
      }
    }

    @SuppressWarnings("unchecked")
    public List<R> getList(TransactionContext tx) {
      jakarta.persistence.Query jpaQuery = createQuery(tx);
      return jpaQuery.getResultList();
    }

    /**
     * Executes the update query in the specified transaction context.
     *
     * @returns the number of affected records
     *
     * @since 2.1.6
     */
    public int executeUpdate(TransactionContext tx) {
      jakarta.persistence.Query jpaQuery = createQuery(tx);
      return jpaQuery.executeUpdate();
    }

    /**
     * Executes the update query in a new transaction context.
     *
     * @returns the number of affected records
     *
     * @since 2.1.6
     */
    public int executeUpdate() {
      try (TransactionContext tx = createTransactionContext()) {
        tx.begin();
        int result = executeUpdate(tx);
        tx.commit();
        return result;
      }
    }
  }

  public Query<T> createQuery(String sQuery, Object... parameters) {
    return new Query<>(sQuery, parameters);
  }

  protected abstract TransactionContext createTransactionContext();

  protected TransactionContext createReadOnlyTransactionContext() {
    return createTransactionContext();
  }

  public T getById(String id) {
    try (TransactionContext tx = createReadOnlyTransactionContext()) {
      return getById(tx, id);
    }
  }

  protected T getById(@SuppressWarnings("unused") TransactionContext tx, @SuppressWarnings("unused") String id) {
    throw new UnsupportedOperationException();
  }

  protected List<T> getList(String sQuery, Object... parameters) {
    try (TransactionContext tx = createReadOnlyTransactionContext()) {
      return getList(tx, sQuery, parameters);
    }
  }

  protected jakarta.persistence.Query createQuery(TransactionContext tx, String sQuery, Object... parameters) {
    jakarta.persistence.Query query = tx.createQuery(sQuery);
    setParameters(query, parameters);
    return query;
  }

  protected jakarta.persistence.Query createNativeQuery(
      TransactionContext tx,
      String sQuery,
      Class<?> resultClass,
      Object... parameters)
  {
    jakarta.persistence.Query query = tx.createNativeQuery(sQuery, resultClass);
    setParameters(query, parameters);
    return query;
  }

  protected jakarta.persistence.Query createNativeQuery(
      TransactionContext tx,
      String sQuery,
      Object... parameters)
  {
    jakarta.persistence.Query query = tx.createNativeQuery(sQuery);
    setParameters(query, parameters);
    return query;
  }

  private void setParameters(jakarta.persistence.Query query, Object[] parameters) {
    if (parameters != null) {
      int parameterPosition = 1;
      for (Object parameter : parameters) {
        query.setParameter(parameterPosition, parameter);
        parameterPosition++;
      }
    }
  }

  @SuppressWarnings("unchecked")
  protected List<T> getList(TransactionContext tx, String sQuery, Object... parameters) {
    jakarta.persistence.Query query = createQuery(tx, sQuery, parameters);
    return query.getResultList();
  }

  protected <C> C getSingle(Class<C> type, String sQuery, Object... parameters) {
    try (TransactionContext tx = createReadOnlyTransactionContext()) {
      return getSingle(tx, type, sQuery, parameters);
    }
  }

  protected <C> C getSingle(TransactionContext tx, Class<C> type, String sQuery, Object... parameters) {
    return type.cast(createQuery(tx, sQuery, parameters).getSingleResult());
  }

  protected <C> C find(Class<C> type, Object primaryKey) {
    try (TransactionContext tx = createReadOnlyTransactionContext()) {
      return find(tx, type, primaryKey);
    }
  }

  protected <C> C find(TransactionContext tx, Class<C> type, Object primaryKey) {
    return tx.find(type, primaryKey);
  }

  protected T get(String sQuery, Object... parameters) {
    try (TransactionContext tx = createReadOnlyTransactionContext()) {
      return get(tx, sQuery, parameters);
    }
  }

  protected T get(TransactionContext tx, String sQuery, Object... parameters) {
    return get(tx, sQuery, null /* lockModeType */, parameters);
  }

  protected T get(TransactionContext tx, String sQuery, LockModeType lockModeType, Object... parameters) {
    return createQuery(sQuery, parameters).setLockModeType(lockModeType).get(tx);
  }

  public void insert(TransactionContext tx, T entity) {
    tx.persist(entity);
  }

  public void insert(T entity) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      insert(tx, entity);
      tx.commit();
    }
  }

  public void update(TransactionContext tx, T entity) {
    entity = tx.merge(entity);
    tx.persist(entity);
  }

  public void update(T entity) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      update(tx, entity);
      tx.commit();
    }
  }

  public void delete(TransactionContext tx, T entity) {
    entity = tx.merge(entity);
    tx.remove(entity);
  }

  public void delete(T entity) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      delete(tx, entity);
      tx.commit();
    }
  }
}
