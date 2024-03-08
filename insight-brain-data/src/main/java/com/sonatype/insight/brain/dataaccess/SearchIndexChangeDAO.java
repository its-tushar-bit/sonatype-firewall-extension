/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.dataaccess.TransactionContext;

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
  public void insert(TransactionContext tx, SearchIndexChange entity) {
    if (SystemConfigurationPropertyFeature.ADVANCED_SEARCH_CONFIGURATION.isEnabled(tx) &&
        SystemConfigurationPropertyFeature.ADVANCED_SEARCH_ENABLED.isEnabled(tx)) {
      super.insert(tx, entity);
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
