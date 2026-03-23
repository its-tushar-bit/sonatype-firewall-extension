/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration.ldap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.DataAccessException;
import com.sonatype.insight.brain.dataaccess.filter.DashboardFilterDAO;
import com.sonatype.insight.brain.dataaccess.filter.UserFilterDAO;
import com.sonatype.insight.brain.dataaccess.notification.UserViewedProductNotificationDAO;
import com.sonatype.insight.brain.dataaccess.security.UserTokenDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.Table;
import org.jooq.impl.DSL;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.LdapServer.LDAP_SERVER;

/**
 * @since 1.7
 */
@Named
@Singleton
public class LdapServerDAO
    extends AbstractOperationalSqlDAO<LdapServer>
{
  private final LdapConnectionDAO ldapConnectionDAO;

  private final LdapUserMappingDAO ldapUserMappingDAO;

  private final UserTokenDAO userTokenDAO;

  private final DashboardFilterDAO dashboardFilterDAO;

  private final UserFilterDAO userFilterDAO;

  private final UserViewedProductNotificationDAO userViewedProductNotificationDAO;

  @Inject
  public LdapServerDAO(
      final OperationalDataStore operationalDataStore,
      final LdapConnectionDAO ldapConnectionDAO,
      final LdapUserMappingDAO ldapUserMappingDAO,
      final UserTokenDAO userTokenDAO,
      final DashboardFilterDAO dashboardFilterDAO,
      final UserFilterDAO userFilterDAO,
      final UserViewedProductNotificationDAO userViewedProductNotificationDAO)
  {
    super(operationalDataStore);
    this.ldapConnectionDAO = ldapConnectionDAO;
    this.ldapUserMappingDAO = ldapUserMappingDAO;
    this.userTokenDAO = userTokenDAO;
    this.dashboardFilterDAO = dashboardFilterDAO;
    this.userFilterDAO = userFilterDAO;
    this.userViewedProductNotificationDAO = userViewedProductNotificationDAO;
  }

  @Override
  public void insert(final TransactionContext tx, final LdapServer config) {
    NameHelper.validate(config.getName());
    if (getByName(tx, config.getName()) != null) {
      throw new InvalidNameException(config.getName() + " is already used as a name.");
    }
    int priority = getNextPriority(tx);
    config.setPriority(priority);
    super.insert(tx, config);
  }

  @Override
  public void update(final TransactionContext tx, final LdapServer config) {
    NameHelper.validate(config.getName());
    LdapServer existingConfig = getByName(tx, config.getName());
    if (existingConfig != null && !existingConfig.getId().equals(config.getId())) {
      throw new InvalidNameException(config.getName() + " is already used as a name.");
    }
    super.update(tx, config);
  }

  @Override
  public void delete(final TransactionContext tx, final LdapServer entity) {
    ldapConnectionDAO.deleteByServerId(tx, entity.getId());
    ldapUserMappingDAO.deleteByServerId(tx, entity.getId());
    userTokenDAO.deleteByRealmId(tx, entity.getId());
    dashboardFilterDAO.deleteByRealmId(tx, entity.getId());
    userFilterDAO.deleteByRealmId(tx, entity.getId());
    userViewedProductNotificationDAO.deleteByRealmId(tx, entity.getId());
    super.delete(tx, entity);
  }

  private LdapServer getByName(final TransactionContext tx, String name) {
    if (name == null || name.trim().isEmpty()) {
      throw new DataAccessException("The LdapServer name cannot be null or empty.");
    }
    // LdapConfiguration Name is whitespace and case insensitive
    name = NameHelper.normalize(name);
    return toEntity(tx.dsl()
        .selectFrom(LDAP_SERVER)
        .where(LDAP_SERVER.NAME_LOWERCASE_NO_WHITESPACE.eq(name))
        .fetchOne());
  }

  public LdapServer getByName(final String name) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByName(tx, name);
    }
  }

  @Override
  public List<LdapServer> getAll() {
    try (TransactionContext tx = createTransactionContext()) {
      return getAll(tx);
    }
  }

  @Override
  public List<LdapServer> getAll(final TransactionContext tx) {
    return tx.dsl()
        .selectFrom(LDAP_SERVER)
        .orderBy(LDAP_SERVER.PRIORITY)
        .fetch()
        .stream()
        .map(this::toEntity)
        .collect(Collectors.toList());
  }

  private int getNextPriority(final TransactionContext tx) {
    Integer currentPriority = tx.dsl()
        .select(DSL.max(LDAP_SERVER.PRIORITY))
        .from(LDAP_SERVER)
        .fetchOne(0, Integer.class);
    return currentPriority == null ? 1 : currentPriority + 1;
  }

  public void updatePriority(final List<String> serverIds) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      updatePriority(tx, serverIds);
      tx.commit();
    }
  }

  private void updatePriority(final TransactionContext tx, final List<String> ldapServerIds) {
    if (new HashSet<>(ldapServerIds).size() != ldapServerIds.size()) {
      throw new DataAccessException("Unable to update priority of Ldap servers due to duplicate server IDs.");
    }

    if (ldapServerIds.size() != getAll().size()) {
      throw new DataAccessException("Unable to update priority of Ldap servers due to server list mismatch.");
    }

    // shifting priorities temporarily due to unique key constraint
    List<LdapServer> ldapServers = new ArrayList<>();
    int maxPriority = getNextPriority(tx) - 1;
    for (String ldapServerId : ldapServerIds) {
      LdapServer ldapServer = getByIdNotNull(tx, ldapServerId);
      ldapServer.setPriority(ldapServer.getPriority() + maxPriority);
      update(tx, ldapServer);
      ldapServers.add(ldapServer);
    }

    // re-ordering of priorities
    int i = 1;
    for (LdapServer ldapServer : ldapServers) {
      ldapServer.setPriority(i);
      update(tx, ldapServer);
      i++;
    }
  }

  @Override
  public Table<?> getJooqTable() {
    return LDAP_SERVER;
  }

  @Override
  public Class<LdapServer> getEntityClass() {
    return LdapServer.class;
  }
}
