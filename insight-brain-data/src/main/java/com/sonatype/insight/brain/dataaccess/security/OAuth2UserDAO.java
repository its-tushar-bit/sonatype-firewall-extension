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
import com.sonatype.insight.brain.model.security.OAuth2User;
import com.sonatype.insight.brain.model.security.UserToken;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

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
    String sQuery = SELECT_FROM_ENTITY + //
        " WHERE entity.id IN ?1" + //
        ORDER_BY_USERNAME;
    return getList(tx, sQuery, ids);
  }

  public List<OAuth2User> getByUsernames(Set<String> usernames) {
    if (CollectionUtils.isEmpty(usernames)) {
      return Collections.emptyList();
    }
    String sQuery = SELECT_FROM_ENTITY + //
        " WHERE entity.username IN ?1" + //
        ORDER_BY_USERNAME;
    return getList(sQuery, usernames);
  }

  public List<OAuth2User> getByEmails(Set<String> emails) {
    if (CollectionUtils.isEmpty(emails)) {
      return Collections.emptyList();
    }
    String sQuery = """
        SELECT entity from OAuth2User entity
          WHERE entity.email IN ?1
          ORDER BY entity.email""";
    return getList(sQuery, emails);
  }

  // real name means full name (first name + " " + last name)
  public List<OAuth2User> getByRealNames(Set<String> names) {
    if (CollectionUtils.isEmpty(names)) {
      return Collections.emptyList();
    }
    String sQuery = """
        SELECT entity from OAuth2User entity
          WHERE CONCAT(entity.firstName, ' ', entity.lastName) IN ?1
          ORDER BY entity.lastName, entity.firstName""";
    return getList(sQuery, names);
  }

  public List<OAuth2User> findUsersByNameOrUsernameQuery(String nameQuery) {
    nameQuery = nameQuery.trim().toLowerCase(Locale.ENGLISH);
    String sQuery = SELECT_FROM_ENTITY +
        " WHERE lower(concat(coalesce(entity.firstName,''), ' ', coalesce(entity.lastName,''))) LIKE ?1" +
        " OR lower(entity.username) LIKE ?1" +
        ORDER_BY_USERNAME;
    return getList(sQuery, nameQuery);
  }

  public OAuth2User getByUsername(TransactionContext tx, String username) {
    String sQuery = SELECT_FROM_ENTITY + //
        " WHERE entity.username=?1";
    return get(tx, sQuery, username);
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
    String sQuery = SELECT_FROM_ENTITY + //
        ORDER_BY_USERNAME;
    return getList(sQuery);
  }

  public void withAllUsersWithGroups(Consumer<OAuth2User> consumer) {
    String sQuery =
        "SELECT oauth2_user.oauth2_user_id, oauth2_user.username, oauth2_user.first_name," + //
            " oauth2_user.last_name, oauth2_user.email," + //
            " STRING_AGG(oauth2_group.name, CHR(44)) groups" +
            " FROM " + getDatabaseSchema() + ".oauth2_user oauth2_user" + //
            "   LEFT JOIN " + getDatabaseSchema() + ".oauth2_user_group oauth2_user_group" + //
            "     ON oauth2_user_group.oauth2_user_id = oauth2_user.oauth2_user_id" + //
            "   LEFT JOIN " + getDatabaseSchema() + ".oauth2_group oauth2_group" + //
            "     ON oauth2_group.oauth2_group_id = oauth2_user_group.oauth2_group_id" + //
            " GROUP BY oauth2_user.oauth2_user_id, oauth2_user.username, oauth2_user.first_name," + //
            " oauth2_user.last_name, oauth2_user.email";

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

            OAuth2User oAuth2User =
                new OAuth2User(username, firstName, lastName, email, groupsSet);
            oAuth2User.setId(id);

            return oAuth2User;
          })
          .forEach(consumer);
    }
  }
}
