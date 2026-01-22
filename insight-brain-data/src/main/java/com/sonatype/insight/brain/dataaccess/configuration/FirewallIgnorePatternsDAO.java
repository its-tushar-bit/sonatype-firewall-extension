/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractDatamartSqlDAO;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.model.configuration.FirewallIgnorePatterns;
import com.sonatype.insight.dataaccess.TransactionContext;

@Named
@Singleton
public class FirewallIgnorePatternsDAO
    extends AbstractDatamartSqlDAO<FirewallIgnorePatterns>
{
  public static final String SINGLETON_ENTITY_ID = "firewall-ignore-patterns";

  @Inject
  public FirewallIgnorePatternsDAO(final DataMartDataStore dataMartDataStore) {
    super(dataMartDataStore);
  }

  public FirewallIgnorePatterns get() {
    return getById(SINGLETON_ENTITY_ID);
  }

  @Override
  public void insert(TransactionContext tx, FirewallIgnorePatterns firewallIgnorePatterns) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void update(TransactionContext tx, FirewallIgnorePatterns firewallIgnorePatterns) {
    firewallIgnorePatterns.setId(SINGLETON_ENTITY_ID);
    super.update(tx, firewallIgnorePatterns);
  }

  @Override
  public void delete(TransactionContext tx, FirewallIgnorePatterns firewallIgnorePatterns) {
    throw new UnsupportedOperationException();
  }
}
