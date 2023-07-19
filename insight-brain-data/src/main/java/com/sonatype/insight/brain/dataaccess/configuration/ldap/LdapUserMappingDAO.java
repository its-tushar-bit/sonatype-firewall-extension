/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration.ldap;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.configuration.ldap.LdapUserMapping;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * @since 1.7
 */
public class LdapUserMappingDAO
    extends AbstractOperationalSqlDAO<LdapUserMapping>
{
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
