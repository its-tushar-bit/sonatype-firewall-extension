/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.dataaccess.TransactionContext;

public class SearchIndexChangeDAO
    extends AbstractOperationalSqlDAO<SearchIndexChange>
{
  private boolean isAdvancedSearchEnabled(TransactionContext tx) {
    return Boolean.parseBoolean(new SystemConfigurationPropertyDAO()
        .getByName(tx, SystemConfigurationProperty.ADVANCED_SEARCH_ENABLED).getValue());
  }

  @Override
  public void insert(TransactionContext tx, SearchIndexChange entity) {
    if (isAdvancedSearchEnabled(tx)) {
      super.insert(tx, entity);
    }
  }

  @Override
  public void update(TransactionContext tx, SearchIndexChange entity) {
    throw new UnsupportedOperationException();
  }
}
