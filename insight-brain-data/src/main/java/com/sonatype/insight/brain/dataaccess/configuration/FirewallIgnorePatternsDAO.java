/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.configuration.FirewallIgnorePatterns;
import com.sonatype.insight.dataaccess.TransactionContext;

public class FirewallIgnorePatternsDAO
    extends AbstractOperationalSqlDAO<FirewallIgnorePatterns>
{
  public static final String SINGLETON_ENTITY_ID = "firewall-ignore-patterns";

  public FirewallIgnorePatterns get() {
    return getById(SINGLETON_ENTITY_ID);
  }

  @Override
  protected FirewallIgnorePatterns getById(TransactionContext tx, String id) {
    String sQuery = "SELECT entity FROM FirewallIgnorePatterns entity" + //
        " WHERE entity.id=?1";
    return get(tx, sQuery, id);
  }

  @Override
  public void insert(TransactionContext tx, FirewallIgnorePatterns firewallIgnorePatterns) {
    firewallIgnorePatterns.setId(SINGLETON_ENTITY_ID);
    super.insert(tx, firewallIgnorePatterns);
  }

  @Override
  public void update(TransactionContext tx, FirewallIgnorePatterns firewallIgnorePatterns) {
    firewallIgnorePatterns.setId(SINGLETON_ENTITY_ID);
    super.update(tx, firewallIgnorePatterns);
  }

  public void delete() {
    FirewallIgnorePatterns firewallIgnorePatterns = get();
    if (firewallIgnorePatterns != null) {
      delete(firewallIgnorePatterns);
    }
  }
}
