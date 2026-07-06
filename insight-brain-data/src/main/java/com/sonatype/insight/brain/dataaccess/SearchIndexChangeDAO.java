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

import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.SearchIndexChange.SEARCH_INDEX_CHANGE;

@Named
@Singleton
public class SearchIndexChangeDAO
    extends AbstractOperationalSqlDAO<SearchIndexChange>
{
  @Inject
  public SearchIndexChangeDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  @Override
  public int insert(TransactionContext tx, SearchIndexChange entity) {
    if (SystemConfigurationPropertyFeature.ADVANCED_SEARCH_CONFIGURATION.isEnabled() &&
        SystemConfigurationPropertyFeature.ADVANCED_SEARCH_ENABLED.isEnabled())
    {
      return super.insert(tx, entity);
    }
    return 0;
  }

  @Override
  public int update(TransactionContext tx, SearchIndexChange entity) {
    throw new UnsupportedOperationException();
  }

  public List<SearchIndexChange> getBatch(final int limit) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(SEARCH_INDEX_CHANGE)
          .limit(limit)
          .fetchInto(SearchIndexChange.class);
    }
  }

  @Override
  public Table<?> getJooqTable() {
    return SEARCH_INDEX_CHANGE;
  }

  @Override
  public Class<SearchIndexChange> getEntityClass() {
    return SearchIndexChange.class;
  }
}
