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

import org.jooq.Table;
import org.jooq.impl.DSL;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.SamlGroup.SAML_GROUP;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.SamlUser.SAML_USER;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.SamlUserGroup.SAML_USER_GROUP;

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
    return tx.dsl()
        .selectFrom(SAML_USER)
        .where(SAML_USER.SAML_USER_ID.in(ids))
        .orderBy(SAML_USER.USERNAME)
        .fetch()
        .into(SamlUser.class);
  }

  public List<SamlUser> getByUsernames(Set<String> usernames) {
    if (CollectionUtils.isEmpty(usernames)) {
      return Collections.emptyList();
    }
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(SAML_USER)
          .where(SAML_USER.USERNAME.in(usernames))
          .orderBy(SAML_USER.USERNAME)
          .fetch()
          .into(SamlUser.class);
    }
  }

  public List<SamlUser> getByEmails(Set<String> emails) {
    if (CollectionUtils.isEmpty(emails)) {
      return Collections.emptyList();
    }
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(SAML_USER)
          .where(SAML_USER.EMAIL.in(emails))
          .orderBy(SAML_USER.EMAIL)
          .fetch()
          .into(SamlUser.class);
    }
  }

  // real name means full name (first name + " " + last name)
  public List<SamlUser> getByRealNames(Set<String> names) {
    if (CollectionUtils.isEmpty(names)) {
      return Collections.emptyList();
    }
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(SAML_USER)
          .where(DSL.concat(SAML_USER.FIRST_NAME, DSL.inline(" "), SAML_USER.LAST_NAME).in(names))
          .orderBy(SAML_USER.LAST_NAME, SAML_USER.FIRST_NAME)
          .fetch()
          .into(SamlUser.class);
    }
  }

  public List<SamlUser> findUsersByNameOrUsernameQuery(String nameQuery) {
    nameQuery = nameQuery.trim().toLowerCase(Locale.ENGLISH);
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(SAML_USER)
          .where(DSL.lower(DSL.concat(
              DSL.coalesce(SAML_USER.FIRST_NAME, DSL.inline("")),
              DSL.inline(" "),
              DSL.coalesce(SAML_USER.LAST_NAME, DSL.inline("")))).like(nameQuery))
          .or(DSL.lower(SAML_USER.USERNAME).like(nameQuery))
          .orderBy(SAML_USER.USERNAME)
          .fetch()
          .into(SamlUser.class);
    }
  }

  public SamlUser getByUsername(TransactionContext tx, String username) {
    return tx.dsl()
        .selectFrom(SAML_USER)
        .where(SAML_USER.USERNAME.eq(username))
        .fetchOneInto(SamlUser.class);
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
  public List<SamlUser> getAll() {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(SAML_USER)
          .orderBy(SAML_USER.USERNAME)
          .fetch()
          .into(SamlUser.class);
    }
  }

  public void withAllUsersWithGroups(Consumer<SamlUser> consumer) {
    try (TransactionContext tx = createTransactionContext()) {
      com.sonatype.insight.brain.jooq.generated.ods.tables.SamlGroup sg = SAML_GROUP.as("saml_group");
      com.sonatype.insight.brain.jooq.generated.ods.tables.SamlUserGroup sug = SAML_USER_GROUP.as("saml_user_group");

      tx.dsl()
          .select(
              SAML_USER.SAML_USER_ID,
              SAML_USER.USERNAME,
              SAML_USER.FIRST_NAME,
              SAML_USER.LAST_NAME,
              SAML_USER.EMAIL,
              DSL.groupConcat(sg.NAME).separator(",").as("groups"))
          .from(SAML_USER)
          .leftJoin(sug)
          .on(sug.SAML_USER_ID.eq(SAML_USER.SAML_USER_ID))
          .leftJoin(sg)
          .on(sg.SAML_GROUP_ID.eq(sug.SAML_GROUP_ID))
          .groupBy(SAML_USER.SAML_USER_ID, SAML_USER.USERNAME, SAML_USER.FIRST_NAME,
              SAML_USER.LAST_NAME, SAML_USER.EMAIL)
          .fetchStream()
          .map(record -> {
            String id = record.get(SAML_USER.SAML_USER_ID);
            String username = record.get(SAML_USER.USERNAME);
            String firstName = record.get(SAML_USER.FIRST_NAME);
            String lastName = record.get(SAML_USER.LAST_NAME);
            String email = record.get(SAML_USER.EMAIL);
            String groups = record.get("groups", String.class);

            Set<String> groupsSet = new LinkedHashSet<>();
            if (StringUtils.isNotBlank(groups)) {
              groupsSet.addAll(Arrays.asList(groups.split(",")));
            }

            SamlUser samlUser = new SamlUser(username, firstName, lastName, email, groupsSet);
            samlUser.setId(id);

            return samlUser;
          })
          .forEach(consumer);
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
  public Table<?> getJooqTable() {
    return SAML_USER;
  }

  @Override
  public Class<SamlUser> getEntityClass() {
    return SamlUser.class;
  }
}
