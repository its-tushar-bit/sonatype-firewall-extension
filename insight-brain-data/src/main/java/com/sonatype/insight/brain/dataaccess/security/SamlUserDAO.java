/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.security;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.filter.DashboardFilterDAO;
import com.sonatype.insight.brain.dataaccess.filter.UserFilterDAO;
import com.sonatype.insight.brain.dataaccess.notification.UserViewedProductNotificationDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.security.SamlUser;
import com.sonatype.insight.brain.model.security.UserToken;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * @since 1.133
 */
@Named
@Singleton
public class SamlUserDAO
    extends AbstractOperationalSqlDAO<SamlUser>
{
  private final UserTokenDAO userTokenDAO;

  private final DashboardFilterDAO dashboardFilterDAO;

  private final UserFilterDAO userFilterDAO;

  private final UserViewedProductNotificationDAO userViewedProductNotificationDAO;

  private final SamlUserGroupDAO samlUserGroupDAO;

  @Inject
  public SamlUserDAO(
      final OperationalDataStore operationalDataStore,
      final UserTokenDAO userTokenDAO,
      final DashboardFilterDAO dashboardFilterDAO,
      final UserFilterDAO userFilterDAO,
      final UserViewedProductNotificationDAO userViewedProductNotificationDAO,
      final SamlUserGroupDAO samlUserGroupDAO)
  {
    super(operationalDataStore);
    this.userTokenDAO = userTokenDAO;
    this.dashboardFilterDAO = dashboardFilterDAO;
    this.userFilterDAO = userFilterDAO;
    this.userViewedProductNotificationDAO = userViewedProductNotificationDAO;
    this.samlUserGroupDAO = samlUserGroupDAO;
  }

  public List<SamlUser> getByIds(Set<String> ids) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByIds(tx, ids);
    }
  }

  public List<SamlUser> getByIds(TransactionContext tx, Set<String> ids) {
    if (CollectionUtils.isEmpty(ids)) {
      return Collections.emptyList();
    }
    String sQuery = "SELECT entity from SamlUser entity" + //
        " WHERE entity.id IN ?1" + //
        " ORDER BY entity.username";
    return getList(tx, sQuery, ids);
  }

  public List<SamlUser> getByUsernames(Set<String> usernames) {
    if (CollectionUtils.isEmpty(usernames)) {
      return Collections.emptyList();
    }
    String sQuery = "SELECT entity from SamlUser entity" + //
        " WHERE entity.username IN ?1" + //
        " ORDER BY entity.username";
    return getList(sQuery, usernames);
  }

  public List<SamlUser> getByEmails(Set<String> emails) {
    if (CollectionUtils.isEmpty(emails)) {
      return Collections.emptyList();
    }
    String sQuery = """
        SELECT entity from SamlUser entity
          WHERE entity.email IN ?1
          ORDER BY entity.email""";
    return getList(sQuery, emails);
  }

  // real name means full name (first name + " " + last name)
  public List<SamlUser> getByRealNames(Set<String> names) {
    if (CollectionUtils.isEmpty(names)) {
      return Collections.emptyList();
    }
    String sQuery = """
        SELECT entity from SamlUser entity
          WHERE CONCAT(entity.firstName, ' ', entity.lastName) IN ?1
          ORDER BY entity.lastName, entity.firstName""";
    return getList(sQuery, names);
  }

  public List<SamlUser> findUsersByNameOrUsernameQuery(String nameQuery) {
    nameQuery = nameQuery.trim().toLowerCase(Locale.ENGLISH);
    String sQuery = "SELECT entity FROM SamlUser entity" +
        " WHERE lower(concat(coalesce(entity.firstName,''), ' ', coalesce(entity.lastName,''))) LIKE ?1" +
        " OR lower(entity.username) LIKE ?1" +
        " ORDER BY entity.username";
    return getList(sQuery, nameQuery);
  }

  public SamlUser getByUsername(TransactionContext tx, String username) {
    String sQuery = "SELECT entity FROM SamlUser entity" + //
        " WHERE entity.username=?1";
    return get(tx, sQuery, username);
  }

  public SamlUser getByUsername(String username) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByUsername(tx, username);
    }
  }

  public SamlUser getByUsernameNotNull(String username) {
    SamlUser samlUser = getByUsername(username);
    if (samlUser == null) {
      throw new NotFoundException("Cannot find a SAML user with username " + username + ".");
    }
    return samlUser;
  }

  public void upsertByUsername(TransactionContext tx, SamlUser samlUser) {
    SamlUser stored = getByUsername(tx, samlUser.getUsername());
    if (stored == null) {
      insert(tx, samlUser);
    }
    else {
      samlUser.setId(stored.getId());
      update(tx, samlUser);
    }
  }

  public void upsertByUsername(SamlUser samlUser) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      upsertByUsername(tx, samlUser);
      tx.commit();
    }
  }

  @Override
  public void delete(TransactionContext tx, SamlUser entity) {
    // Cascade to user token
    UserToken userToken = userTokenDAO.getByUsernameAndRealmId(tx, entity.getUsername(), SamlUser.SAML_REALM_ID);
    if (userToken != null) {
      userTokenDAO.delete(tx, userToken);
    }

    // Cascade to dashboard filters
    dashboardFilterDAO.deleteByUsernameAndRealmId(tx, entity.getUsername(), SamlUser.SAML_REALM_ID);

    // Cascade to user filters
    userFilterDAO.deleteByUsernameAndRealmId(tx, entity.getUsername(), SamlUser.SAML_REALM_ID);

    // Cascade to user viewed product notifications
    userViewedProductNotificationDAO.deleteByUsernameAndRealmId(tx, entity.getUsername(), SamlUser.SAML_REALM_ID);

    // Cascade to saml user group mappings
    samlUserGroupDAO.deleteBySamlUserId(tx, entity.getId());

    super.delete(tx, entity);
  }

  @Override
  public List<SamlUser> getAll() {
    String sQuery = "SELECT entity FROM SamlUser entity" + //
        " ORDER BY entity.username";
    return getList(sQuery);
  }

  public void withAllUsersWithGroups(Consumer<SamlUser> consumer) {
    String sQuery =
        "SELECT saml_user.saml_user_id, saml_user.username, saml_user.first_name," +
            " saml_user.last_name, saml_user.email," + //
            " STRING_AGG(saml_group.name, CHR(44)) groups" +
            " FROM " + getDatabaseSchema() + ".saml_user saml_user" + //
            "   LEFT JOIN " + getDatabaseSchema() + ".saml_user_group saml_user_group" + //
            "     ON saml_user_group.saml_user_id = saml_user.saml_user_id" + //
            "   LEFT JOIN " + getDatabaseSchema() + ".saml_group saml_group" + //
            "     ON saml_group.saml_group_id = saml_user_group.saml_group_id" + //
            " GROUP BY saml_user.saml_user_id, saml_user.username, saml_user.first_name," + //
            " saml_user.last_name, saml_user.email";

    try (TransactionContext tx = createTransactionContext()) {
      jakarta.persistence.Query query = tx.createNativeQuery(sQuery);

      ((Stream<Object[]>) query.getResultStream())
          .map(array -> {
            String id = (String) array[0];
            String username = (String) array[1];
            String firstName = (String) array[2];
            String lastName = (String) array[3];
            String email = (String) array[4];
            String groups = (String) array[5];

            Set<String> groupsSet = new LinkedHashSet<>();
            if (StringUtils.isNotBlank(groups)) {
              groupsSet.addAll(Arrays.asList(groups.split(",")));
            }

            SamlUser samlUser =
                new SamlUser(username, firstName, lastName, email, groupsSet);
            samlUser.setId(id);

            return samlUser;
          })
          .forEach(consumer);
    }
  }
}
