/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.UUID;

import com.sonatype.insight.brain.dataaccess.search.EmptySearchIndexManager;
import com.sonatype.insight.brain.dataaccess.search.SearchIndexManager;
import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.dataaccess.AbstractDAO;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.model.HasStringId;

public abstract class AbstractSqlDAO<T extends HasStringId>
    extends AbstractDAO<T>
{
  private final SearchIndexManager searchIndexManager;

  /**
   * Constructor for DAOs that require the search index. These DAOs must override one of the methods:
   * <ul>
   *   <li>{@link #newSearchIndexChange(HasStringId)}</li>
   *   <li>{@link #newSearchIndexChangeForInsert(HasStringId)}</li>
   *   <li>{@link #newSearchIndexChangeForUpdate(HasStringId)}</li>
   *   <li>{@link #newSearchIndexChangeForDelete(HasStringId)}</li>
   * </ul>
   */
  protected AbstractSqlDAO(final SearchIndexManager searchIndexManager) {
    this.searchIndexManager = searchIndexManager;
  }

  /**
   * Constructor for DAOs that will <B>NOT</B> require the search index. The {@link #searchIndexManager} will be set to
   * a {@link EmptySearchIndexManager} instance. If the <T> entity for this DAO needs to be searchable then use the
   * {@link #AbstractOperationalSqlDAO(OperationalDataStore, SearchIndexManager)} constructor.
   */
  protected AbstractSqlDAO() {
    // Note: singleton pattern used to reduce churn in tests
    this(EmptySearchIndexManager.getInstance());
  }

  private String newUUID() {
    return UUID.randomUUID().toString().replace("-", "");
  }

  @Override
  public void insert(TransactionContext tx, T entity) {
    String id = entity.getId();
    if (id == null || id.trim().isEmpty()) {
      entity.setId(newUUID());
    }
    super.insert(tx, entity);

    insertSearchIndexChange(tx, newSearchIndexChangeForInsert(entity));
  }

  @Override
  public void update(TransactionContext tx, T entity) {
    super.update(tx, entity);
    insertSearchIndexChange(tx, newSearchIndexChangeForUpdate(entity));
  }

  @Override
  public void delete(TransactionContext tx, T entity) {
    super.delete(tx, entity);
    insertSearchIndexChange(tx, newSearchIndexChangeForDelete(entity));
  }

  private void insertSearchIndexChange(final TransactionContext tx, final SearchIndexChange searchIndexChange) {
    searchIndexManager.insert(tx, searchIndexChange);
  }

  protected SearchIndexChange newSearchIndexChangeForInsert(T entity) {
    return newSearchIndexChange(entity);
  }

  protected SearchIndexChange newSearchIndexChangeForUpdate(T entity) {
    return newSearchIndexChange(entity);
  }

  protected SearchIndexChange newSearchIndexChangeForDelete(T entity) {
    return newSearchIndexChange(entity);
  }

  protected SearchIndexChange newSearchIndexChange(T entity) {
    // by default, no contribution to the search index
    return null;
  }
}
