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
import com.sonatype.insight.brain.model.configuration.ldap.LdapConnection;
import com.sonatype.insight.brain.security.RotatableSecrets;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * @since 1.7
 */
@Named
@Singleton
public class LdapConnectionDAO
    extends AbstractOperationalSqlDAO<LdapConnection>
    implements RotatableSecrets
{
  @Inject
  public LdapConnectionDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public LdapConnection getByServerId(String serverId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByServerId(tx, serverId);
    }
  }

  public LdapConnection getByServerId(TransactionContext tx, String serverId) {
    String sQuery = "SELECT entity FROM LdapConnection entity" + //
        " WHERE entity.serverId=?1";
    return get(tx, sQuery, serverId);
  }

  public void deleteByServerId(TransactionContext tx, String id) {
    LdapConnection ldapConnection = getByServerId(tx, id);
    if (ldapConnection != null) {
      delete(tx, ldapConnection);
    }
  }
}
