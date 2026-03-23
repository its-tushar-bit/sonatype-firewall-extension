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
import com.sonatype.insight.brain.model.security.OAuth2User;
import com.sonatype.insight.brain.model.security.UserToken;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Table;
import org.jooq.impl.DSL;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.Oauth2Group.OAUTH2_GROUP;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.Oauth2User.OAUTH2_USER;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.Oauth2UserGroup.OAUTH2_USER_GROUP;

@Named
@Singleton
public class OAuth2UserDAO
    extends AbstractOperationalSqlDAO<OAuth2User>
{
  public static final String SELECT_FROM_ENTITY = "SELECT entity from OAuth2User entity";

  public static final String ORDER_BY_USERNAME = " ORDER BY entity.username";

  private final UserTokenDAO userTokenDAO;

  private final DashboardFilterDAO dashboardFilterDAO;

  private final UserFilterDAO userFilterDAO;

  private final UserViewedProductNotificationDAO userViewedProductNotificationDAO;

  private final OAuth2UserGroupDAO oAuth2UserGroupDAO;

  @Inject
  public OAuth2UserDAO(
      final OperationalDataStore operationalDataStore,
      final UserTokenDAO userTokenDAO,
      final DashboardFilterDAO dashboardFilterDAO,
      final UserFilterDAO userFilterDAO,
      final UserViewedProductNotificationDAO userViewedProductNotificationDAO,
      final OAuth2UserGroupDAO oAuth2UserGroupDAO)
  {
    super(operationalDataStore);
    this.userTokenDAO = userTokenDAO;
    this.dashboardFilterDAO = dashboardFilterDAO;
    this.userFilterDAO = userFilterDAO;
    this.userViewedProductNotificationDAO = userViewedProductNotificationDAO;
    this.oAuth2UserGroupDAO = oAuth2UserGroupDAO;
  }

  public List<OAuth2User> getByIds(Set<String> ids) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByIds(tx, ids);
    }
  }

  public List<OAuth2User> getByIds(TransactionContext tx, Set<String> ids) {
    if (CollectionUtils.isEmpty(ids)) {
      return Collections.emptyList();
    }
    return tx.dsl()
        .selectFrom(OAUTH2_USER)
        .where(OAUTH2_USER.OAUTH2_USER_ID.in(ids))
        .orderBy(OAUTH2_USER.USERNAME)
        .fetch()
        .into(OAuth2User.class);
  }

  public List<OAuth2User> getByUsernames(Set<String> usernames) {
    if (CollectionUtils.isEmpty(usernames)) {
      return Collections.emptyList();
    }
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(OAUTH2_USER)
          .where(OAUTH2_USER.USERNAME.in(usernames))
          .orderBy(OAUTH2_USER.USERNAME)
          .fetch()
          .into(OAuth2User.class);
    }
  }

  public List<OAuth2User> getByEmails(Set<String> emails) {
    if (CollectionUtils.isEmpty(emails)) {
      return Collections.emptyList();
    }
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(OAUTH2_USER)
          .where(OAUTH2_USER.EMAIL.in(emails))
          .orderBy(OAUTH2_USER.EMAIL)
          .fetch()
          .into(OAuth2User.class);
    }
  }

  // real name means full name (first name + " " + last name)
  public List<OAuth2User> getByRealNames(Set<String> names) {
    if (CollectionUtils.isEmpty(names)) {
      return Collections.emptyList();
    }
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(OAUTH2_USER)
          .where(OAUTH2_USER.FIRST_NAME.concat(" ").concat(OAUTH2_USER.LAST_NAME).in(names))
          .orderBy(OAUTH2_USER.LAST_NAME, OAUTH2_USER.FIRST_NAME)
          .fetch()
          .into(OAuth2User.class);
    }
  }

  public List<OAuth2User> findUsersByNameOrUsernameQuery(String nameQuery) {
    nameQuery = nameQuery.trim().toLowerCase(Locale.ENGLISH);
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(OAUTH2_USER)
          .where(DSL.lower(
              DSL.coalesce(OAUTH2_USER.FIRST_NAME, DSL.inline(""))
                  .concat(" ")
                  .concat(DSL.coalesce(OAUTH2_USER.LAST_NAME, DSL.inline(""))))
              .like(nameQuery)
              .or(DSL.lower(OAUTH2_USER.USERNAME).like(nameQuery)))
          .orderBy(OAUTH2_USER.USERNAME)
          .fetch()
          .into(OAuth2User.class);
    }
  }

  public OAuth2User getByUsername(TransactionContext tx, String username) {
    return tx.dsl()
        .selectFrom(OAUTH2_USER)
        .where(OAUTH2_USER.USERNAME.eq(username))
        .fetchOneInto(OAuth2User.class);
  }

  public OAuth2User getByUsername(String username) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByUsername(tx, username);
    }
  }

  public OAuth2User getByUsernameNotNull(String username) {
    OAuth2User oAuth2User = getByUsername(username);
    if (oAuth2User == null) {
      throw new NotFoundException("Cannot find a OAuth2 user with username " + username + ".");
    }
    return oAuth2User;
  }

  public void upsertByUsername(TransactionContext tx, OAuth2User oAuth2User) {
    OAuth2User stored = getByUsername(tx, oAuth2User.getUsername());
    if (stored == null) {
      insert(tx, oAuth2User);
    }
    else {
      oAuth2User.setId(stored.getId());
      update(tx, oAuth2User);
    }
  }

  public void upsertByUsername(OAuth2User oAuth2User) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      upsertByUsername(tx, oAuth2User);
      tx.commit();
    }
  }

  @Override
  public void delete(TransactionContext tx, OAuth2User entity) {
    // Cascade to user token
    UserToken userToken = userTokenDAO.getByUsernameAndRealmId(tx, entity.getUsername(), OAuth2User.OAUTH2_REALM_ID);
    if (userToken != null) {
      userTokenDAO.delete(tx, userToken);
    }

    // Cascade to dashboard filters
    dashboardFilterDAO.deleteByUsernameAndRealmId(tx, entity.getUsername(), OAuth2User.OAUTH2_REALM_ID);

    // Cascade to user filters
    userFilterDAO.deleteByUsernameAndRealmId(tx, entity.getUsername(), OAuth2User.OAUTH2_REALM_ID);

    // Cascade to user viewed product notifications
    userViewedProductNotificationDAO.deleteByUsernameAndRealmId(tx, entity.getUsername(), OAuth2User.OAUTH2_REALM_ID);

    // Cascade to oauth2 user group mappings
    oAuth2UserGroupDAO.deleteByOAuth2UserId(tx, entity.getId());

    super.delete(tx, entity);
  }

  @Override
  public List<OAuth2User> getAll() {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(OAUTH2_USER)
          .orderBy(OAUTH2_USER.USERNAME)
          .fetch()
          .into(OAuth2User.class);
    }
  }

  public void withAllUsersWithGroups(Consumer<OAuth2User> consumer) {
    try (TransactionContext tx = createTransactionContext()) {
      com.sonatype.insight.brain.jooq.generated.ods.tables.Oauth2Group og = OAUTH2_GROUP.as("oauth2_group");
      com.sonatype.insight.brain.jooq.generated.ods.tables.Oauth2UserGroup oug =
          OAUTH2_USER_GROUP.as("oauth2_user_group");

      tx.dsl()
          .select(
              OAUTH2_USER.OAUTH2_USER_ID,
              OAUTH2_USER.USERNAME,
              OAUTH2_USER.FIRST_NAME,
              OAUTH2_USER.LAST_NAME,
              OAUTH2_USER.EMAIL,
              DSL.groupConcat(og.NAME).separator(",").as("groups"))
          .from(OAUTH2_USER)
          .leftJoin(oug)
          .on(oug.OAUTH2_USER_ID.eq(OAUTH2_USER.OAUTH2_USER_ID))
          .leftJoin(og)
          .on(og.OAUTH2_GROUP_ID.eq(oug.OAUTH2_GROUP_ID))
          .groupBy(OAUTH2_USER.OAUTH2_USER_ID, OAUTH2_USER.USERNAME, OAUTH2_USER.FIRST_NAME,
              OAUTH2_USER.LAST_NAME, OAUTH2_USER.EMAIL)
          .fetchStream()
          .map(record -> {
            String id = record.get(OAUTH2_USER.OAUTH2_USER_ID);
            String username = record.get(OAUTH2_USER.USERNAME);
            String firstName = record.get(OAUTH2_USER.FIRST_NAME);
            String lastName = record.get(OAUTH2_USER.LAST_NAME);
            String email = record.get(OAUTH2_USER.EMAIL);
            String groups = record.get("groups", String.class);

            Set<String> groupsSet = new LinkedHashSet<>();
            if (StringUtils.isNotBlank(groups)) {
              groupsSet.addAll(Arrays.asList(groups.split(",")));
            }

            OAuth2User oAuth2User = new OAuth2User(username, firstName, lastName, email, groupsSet);
            oAuth2User.setId(id);

            return oAuth2User;
          })
          .forEach(consumer);
    }
  }

  @Override
  public Table<?> getJooqTable() {
    return OAUTH2_USER;
  }

  @Override
  public List<OAuth2User> getAll(TransactionContext tx) {
    return tx.dsl().selectFrom(OAUTH2_USER).fetchInto(OAuth2User.class);
  }

  @Override
  public Class<OAuth2User> getEntityClass() {
    return OAuth2User.class;
  }
}
