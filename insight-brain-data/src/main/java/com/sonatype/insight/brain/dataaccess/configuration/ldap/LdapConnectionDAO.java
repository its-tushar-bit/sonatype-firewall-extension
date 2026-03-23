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

import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.LdapConnection.LDAP_CONNECTION;

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
  public LdapConnectionDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public LdapConnection getByServerId(final String serverId) {
    return getByField(LDAP_CONNECTION.LDAP_SERVER_ID, serverId);
  }

  public LdapConnection getByServerId(final TransactionContext tx, final String serverId) {
    return getByField(tx, LDAP_CONNECTION.LDAP_SERVER_ID, serverId);
  }

  public void deleteByServerId(final TransactionContext tx, final String id) {
    LdapConnection ldapConnection = getByServerId(tx, id);
    if (ldapConnection != null) {
      delete(tx, ldapConnection);
    }
  }

  @Override
  public Table<?> getJooqTable() {
    return LDAP_CONNECTION;
  }

  @Override
  public Class<LdapConnection> getEntityClass() {
    return LdapConnection.class;
  }
}
