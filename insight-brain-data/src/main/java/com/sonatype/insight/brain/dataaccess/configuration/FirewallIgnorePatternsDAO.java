/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import com.sonatype.insight.brain.dataaccess.AbstractDatamartSqlDAO;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.model.configuration.FirewallIgnorePatterns;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.dm.tables.FirewallIgnorePatterns.FIREWALL_IGNORE_PATTERNS;

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
    throw new UnsupportedOperationException("insert() is not supported for singleton entity FirewallIgnorePatterns");
  }

  @Override
  public void update(TransactionContext tx, FirewallIgnorePatterns firewallIgnorePatterns) {
    firewallIgnorePatterns.setId(SINGLETON_ENTITY_ID);
    super.update(tx, firewallIgnorePatterns);
  }

  @Override
  public void delete(TransactionContext tx, FirewallIgnorePatterns firewallIgnorePatterns) {
    throw new UnsupportedOperationException("delete() is not supported for singleton entity FirewallIgnorePatterns");
  }

  @Override
  public Table<?> getJooqTable() {
    return FIREWALL_IGNORE_PATTERNS;
  }

  @Override
  public Class<FirewallIgnorePatterns> getEntityClass() {
    return FirewallIgnorePatterns.class;
  }
}
