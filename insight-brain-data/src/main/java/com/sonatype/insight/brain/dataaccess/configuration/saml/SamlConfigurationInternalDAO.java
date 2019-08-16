/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration.saml;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * @since 1.72
 */
class SamlConfigurationInternalDAO
    extends AbstractOperationalSqlDAO<SamlConfigurationInternal>
{
  @Override
  protected SamlConfigurationInternal getById(TransactionContext tx, String id) {
    String sQuery = "SELECT entity FROM SamlConfigurationInternal entity" + //
        " WHERE entity.id=?1";
    return get(tx, sQuery, id);
  }

  /**
   * Returns the one and only SAML configuration or null if SAML is not configured.
   */
  SamlConfigurationInternal get() {
    String sQuery = "SELECT entity FROM SamlConfigurationInternal entity";
    return createQuery(sQuery).forceSingleResult().get();
  }
}
