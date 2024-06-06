/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import com.sonatype.insight.brain.dataaccess.search.EmptySearchIndexManager;
import com.sonatype.insight.brain.dataaccess.search.SearchIndexManager;
import com.sonatype.insight.brain.db.IdUtil;
import com.sonatype.insight.brain.db.datastore.DataStore;
import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.dataaccess.AbstractDAO;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.model.HasStringId;

import com.google.common.collect.Lists;

import static java.util.stream.Collectors.toList;

public abstract class AbstractSqlDAO<T extends HasStringId>
    extends AbstractDAO<T>
{
  private final SearchIndexManager searchIndexManager;

  public static final int H2_IN_OPERATOR_THRESHOLD = 2000;

  public static final int POSTGRES_IN_OPERATOR_THRESHOLD = Short.MAX_VALUE;

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
    return IdUtil.newUUID();
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

  public boolean isDatabaseEmbedded(DataStore datastore) {
    return datastore.isDatabaseEmbedded();
  }

  public int getInOperatorThreshold(DataStore dataStore) {
    return isDatabaseEmbedded(dataStore) ? H2_IN_OPERATOR_THRESHOLD : POSTGRES_IN_OPERATOR_THRESHOLD;
  }

  /**
   * This method should be used for queries that use an "IN" clause.
   * H2 and Postgres limit the number of elements in "IN" clauses. This method breaks the list of values into
   * partitions, runs the given query on each partition and merges the results from all partitions.
   *
   * @param <E> The type of the values in the list to be used in the "IN" clause.
   * @param inClauseValues List of values to be used in the "IN" clause.
   * @param getter Function to be used to query the values.
   * @param dataStore A related set of data/tables
   */
  protected <E> List<T> getListWithSqlInClause(List<E> inClauseValues, Function<Collection<E>, List<T>> getter,
                                               DataStore dataStore)
  {
    int inOperatorThreshold = getInOperatorThreshold(dataStore);
    if (inClauseValues.size() >= inOperatorThreshold) {
      List<List<E>> inClauseValuesPartitions = Lists.partition(inClauseValues, inOperatorThreshold);

      return inClauseValuesPartitions.stream().map(getter).flatMap(Collection::stream).collect(toList());
    }
    else {
      return getter.apply(inClauseValues);
    }
  }
}
