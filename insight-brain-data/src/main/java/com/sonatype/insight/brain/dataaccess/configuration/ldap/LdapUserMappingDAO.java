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

import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.LdapUsermapping.LDAP_USERMAPPING;

/**
 * @since 1.7
 */
@Named
@Singleton
public class LdapUserMappingDAO
    extends AbstractOperationalSqlDAO<LdapUserMapping>
{
  @Inject
  public LdapUserMappingDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public LdapUserMapping getByServerId(final String serverId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByServerId(tx, serverId);
    }
  }

  private LdapUserMapping getByServerId(final TransactionContext tx, final String serverId) {
    return tx.dsl()
        .selectFrom(LDAP_USERMAPPING)
        .where(LDAP_USERMAPPING.LDAP_SERVER_ID.eq(serverId))
        .fetchOneInto(LdapUserMapping.class);
  }

  public void deleteByServerId(final TransactionContext tx, final String id) {
    LdapUserMapping ldapUserMapping = getByServerId(tx, id);
    if (ldapUserMapping != null) {
      delete(tx, ldapUserMapping);
    }
  }

  @Override
  public Table<?> getJooqTable() {
    return LDAP_USERMAPPING;
  }

  @Override
  public Class<LdapUserMapping> getEntityClass() {
    return LdapUserMapping.class;
  }
}
