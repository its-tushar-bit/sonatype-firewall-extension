/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.configuration.CpeMatchingConfiguration;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * @since 1.190
 */
@Named
@Singleton
public class CpeMatchingConfigurationDAO
    extends AbstractOperationalSqlDAO<CpeMatchingConfiguration>
{
  @Inject
  public CpeMatchingConfigurationDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public CpeMatchingConfiguration getByOwnerId(String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerId(tx, ownerId);
    }
  }

  public CpeMatchingConfiguration getByOwnerId(TransactionContext tx, String ownerId) {
    String sQuery = "SELECT entity FROM CpeMatchingConfiguration entity WHERE entity.ownerId = ?1";
    return get(tx, sQuery, ownerId);
  }
}
