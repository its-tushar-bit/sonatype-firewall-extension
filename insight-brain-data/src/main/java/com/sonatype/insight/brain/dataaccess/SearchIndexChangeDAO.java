/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.db.IdUtil;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.dataaccess.TransactionContext;

@Named
@Singleton
public class SearchIndexChangeDAO
    extends AbstractOperationalSqlDAO<SearchIndexChange>
{
  private static final String SEARCH_INDEX_CHANGE_INSERT_QUERY =
      "INSERT INTO %s.search_index_change (" +
          "  search_index_change_id, change_type, change_data" +
          ") VALUES (?1, ?2, ?3)";

  @Inject
  public SearchIndexChangeDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  @Override
  public void insert(TransactionContext tx, SearchIndexChange entity) {
    if (SystemConfigurationPropertyFeature.ADVANCED_SEARCH_CONFIGURATION.isEnabled() &&
        SystemConfigurationPropertyFeature.ADVANCED_SEARCH_ENABLED.isEnabled())
    {
      jakarta.persistence.Query nativeQuery =
          tx.createNativeQuery(String.format(SEARCH_INDEX_CHANGE_INSERT_QUERY, getDatabaseSchema()));
      nativeQuery.setParameter(1, IdUtil.newUUID());
      nativeQuery.setParameter(2, entity.getChangeType().name());
      nativeQuery.setParameter(3, entity.getChangeData());
      nativeQuery.executeUpdate();
    }
  }

  @Override
  public void update(TransactionContext tx, SearchIndexChange entity) {
    throw new UnsupportedOperationException();
  }

  public List<SearchIndexChange> getBatch(final int limit) {
    Query<SearchIndexChange> query = new Query<>("SELECT entity FROM SearchIndexChange entity");
    query.setMaxResults(limit);
    return query.getList();
  }
}
