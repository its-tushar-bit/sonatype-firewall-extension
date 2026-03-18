/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

// Vendored/copied from hosted-data-services/insight-db-common
package com.sonatype.insight.dataaccess;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Query;

/**
 * Holds the transaction context for data access. It can be used to begin and commit transactions. Instances of this
 * class need to be closed.
 *
 * @since 2.1.2
 */
public class TransactionContext
    implements AutoCloseable
{
  private final EntityManager entityManager;

  public TransactionContext(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  public void begin() {
    getTransaction().begin();
  }

  public void commit() {
    getTransaction().commit();
  }

  public void persist(Object entity) {
    entityManager.persist(entity);
  }

  public <T> T merge(T entity) {
    return entityManager.merge(entity);
  }

  public void remove(Object entity) {
    entityManager.remove(entity);
  }

  public <T> T find(Class<T> entityType, Object primaryKey) {
    return entityManager.find(entityType, primaryKey);
  }

  public Query createQuery(String qlString) {
    return entityManager.createQuery(qlString);
  }

  public Query createNativeQuery(String qlString) {
    return entityManager.createNativeQuery(qlString);
  }

  public Query createNativeQuery(String qlString, Class<?> resultClass) {
    return entityManager.createNativeQuery(qlString, resultClass);
  }

  public boolean isActive() {
    return getTransaction().isActive();
  }

  @Override
  public void close() {
    try {
      if (getTransaction().isActive()) {
        getTransaction().rollback();
      }
    }
    finally {
      entityManager.close();
    }
  }

  private EntityTransaction getTransaction() {
    return entityManager.getTransaction();
  }
}
