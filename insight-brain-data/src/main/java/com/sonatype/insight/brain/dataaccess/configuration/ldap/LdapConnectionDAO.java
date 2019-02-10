/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration.ldap;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.configuration.ldap.LdapConnection;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;

/**
 * @since 1.7
 */
public class LdapConnectionDAO
    extends AbstractOperationalSqlDAO<LdapConnection>
{
  @Override
  public LdapConnection getById(TransactionContext tx, String id) {
    String sQuery = "SELECT entity FROM LdapConnection entity" + //
        " WHERE entity.id=?1";
    return get(tx, sQuery, id);
  }

  public LdapConnection getByIdNotNull(String id) {
    LdapConnection conn = getById(id);
    if (conn == null) {
      throw new NotFoundException("Cannot find LdapConnection with ID " + id + ".");
    }
    return conn;
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
    LdapConnection conn = getByServerId(tx, id);
    if (conn != null) {
      delete(tx, conn);
    }
  }
}
