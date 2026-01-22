/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration.ldap;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.configuration.ldap.LdapUserMapping;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * @since 1.7
 */
@Named
@Singleton
public class LdapUserMappingDAO
    extends AbstractOperationalSqlDAO<LdapUserMapping>
{
  @Inject
  public LdapUserMappingDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public LdapUserMapping getByServerId(String serverId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByServerId(tx, serverId);
    }
  }

  private LdapUserMapping getByServerId(TransactionContext tx, String serverId) {
    String sQuery = "SELECT entity FROM LdapUserMapping entity" + //
        " WHERE entity.serverId=?1";
    return get(tx, sQuery, serverId);
  }

  public void deleteByServerId(TransactionContext tx, String id) {
    LdapUserMapping ldapUserMapping = getByServerId(tx, id);
    if (ldapUserMapping != null) {
      delete(tx, ldapUserMapping);
    }
  }
}
