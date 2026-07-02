/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.security;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.dataaccess.filter.DashboardFilterDAO;
import com.sonatype.insight.brain.dataaccess.filter.UserFilterDAO;
import com.sonatype.insight.brain.dataaccess.ide.UserIdePolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.notification.UserViewedProductNotificationDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.security.UserToken;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Table;
import org.jooq.impl.DSL;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.User.USER;

/**
 * @since 1.7
 */
@Named
@Singleton
public class UserDAO
    extends AbstractOperationalSqlDAO<User>
{
  public static final int MAX_FIRST_NAME_SIZE = 100;

  public static final int MAX_LAST_NAME_SIZE = 100;

  public static final int MAX_EMAIL_SIZE = 255;

  private final MembershipMappingDAO membershipMappingDAO;

  private final UserTokenDAO userTokenDAO;

  private final DashboardFilterDAO dashboardFilterDAO;

  private final UserFilterDAO userFilterDAO;

  private final UserViewedProductNotificationDAO userViewedProductNotificationDAO;

  private final UserIdePolicyEvaluationDAO userIdePolicyEvaluationDAO;

  private final SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  @Inject
  public UserDAO(
      final OperationalDataStore operationalDataStore,
      final MembershipMappingDAO membershipMappingDAO,
      final UserTokenDAO userTokenDAO,
      final DashboardFilterDAO dashboardFilterDAO,
      final UserFilterDAO userFilterDAO,
      final UserViewedProductNotificationDAO userViewedProductNotificationDAO,
      final UserIdePolicyEvaluationDAO userIdePolicyEvaluationDAO,
      final SystemConfigurationPropertyDAO systemConfigurationPropertyDAO)
  {
    super(operationalDataStore);
    this.membershipMappingDAO = membershipMappingDAO;
    this.userTokenDAO = userTokenDAO;
    this.dashboardFilterDAO = dashboardFilterDAO;
    this.userFilterDAO = userFilterDAO;
    this.userViewedProductNotificationDAO = userViewedProductNotificationDAO;
    this.userIdePolicyEvaluationDAO = userIdePolicyEvaluationDAO;
    this.systemConfigurationPropertyDAO = systemConfigurationPropertyDAO;
  }

  public User getByUsername(TransactionContext tx, String username) {
    return tx.dsl()
        .selectFrom(USER)
        .where(USER.USERNAME_LOWERCASE.eq(User.normalizeUsername(username)))
        .fetchOneInto(User.class);
  }

  /**
   * Looks up users by their (case-insensitive) usernames.
   *
   * @param usernames The usernames to look up, must not be {@code null}.
   * @return List of matching User objects ordered by their lower case usernames.
   */
  public List<User> getByUsernames(Set<String> usernames) {
    List<String> lowerCaseUsernames = new ArrayList<>();
    for (String username : usernames) {
      lowerCaseUsernames.add(User.normalizeUsername(username));
    }

    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(USER)
          .where(USER.USERNAME_LOWERCASE.in(lowerCaseUsernames))
          .orderBy(USER.USERNAME_LOWERCASE)
          .fetchInto(User.class);
    }
  }

  public List<User> getByEmails(Set<String> emails) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(USER)
          .where(USER.EMAIL.in(emails))
          .orderBy(USER.EMAIL)
          .fetchInto(User.class);
    }
  }

  // real name means full name (First + Last)
  public List<User> getByRealNames(Set<String> fullNames) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(USER)
          .where(USER.FIRST_NAME.concat(" ").concat(USER.LAST_NAME).in(fullNames))
          .orderBy(USER.LAST_NAME, USER.FIRST_NAME)
          .fetchInto(User.class);
    }
  }

  /**
   * Looks up a user by its (case-insensitive) username.
   *
   * @param username The username to look up, must not be {@code null}.
   * @return The user or {@code null} if not found.
   */
  public User getByUsername(String username) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByUsername(tx, username);
    }
  }

  public User getByUsernameNotNull(String username) {
    User user = getByUsername(username);
    if (user == null) {
      throw new NotFoundException("Cannot find a user with username " + username + ".");
    }
    return user;
  }

  /**
   * Find users in the database by matching case-insensitive the supplied name query against their full name.
   *
   * @param nameQuery This string may contain wildcards and it will be checked case-insensitive against
   *          concat(entity.firstName, ' ', entity.lastName)
   */
  public List<User> findUsersByName(String nameQuery) {
    nameQuery = nameQuery.trim().toLowerCase(Locale.ENGLISH);
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(USER)
          .where(DSL.lower(USER.FIRST_NAME).concat(" ").concat(DSL.lower(USER.LAST_NAME)).like(nameQuery))
          .orderBy(USER.USERNAME)
          .fetchInto(User.class);
    }
  }

  private void validateUsername(String username) {
    if (username != null && username.contains(" ")) {
      throw new InvalidNameException("The username cannot contain spaces.");
    }
    NameHelper.validate("The username", username);
  }

  private void validate(User user) {
    validateUsername(user.getUsername());

    NameHelper.validate("The first name", user.getFirstName(), MAX_FIRST_NAME_SIZE);

    NameHelper.validate("The last name", user.getLastName(), MAX_LAST_NAME_SIZE);

    if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
      throw new InvalidUserException("The email is required.");
    }
    if (user.getEmail() != null && user.getEmail().length() > MAX_EMAIL_SIZE) {
      throw new InvalidUserException("The email must be " + MAX_EMAIL_SIZE + " characters or less.");
    }

    if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
      throw new InvalidUserException("The password is required.");
    }
  }

  @Override
  public int insert(TransactionContext tx, User user) {
    validate(user);

    if (getByUsername(tx, user.getUsername()) != null) {
      throw new InvalidNameException(user.getUsername() + " is already used as a username.");
    }

    return super.insert(tx, user);
  }

  @Override
  public void update(TransactionContext tx, User user) {
    validate(user);

    User existingUser = getByUsername(tx, user.getUsername());
    if (existingUser != null && !existingUser.getId().equals(user.getId())) {
      throw new InvalidNameException(user.getUsername() + " is already used as a username.");
    }

    super.update(tx, user);
  }

  @Override
  public void delete(TransactionContext tx, User entity) {
    // Cascade to membership mappings
    for (MembershipMapping membershipMapping : membershipMappingDAO.getByUser(tx, entity.getUsername())) {
      membershipMappingDAO.delete(tx, membershipMapping);
    }

    // Cascade to user token
    UserToken userToken = userTokenDAO.getInternalByUsername(tx, entity.getUsername());
    if (userToken != null) {
      userTokenDAO.delete(tx, userToken);
    }

    // Cascade to dashboard filters
    dashboardFilterDAO.deleteByUsernameAndRealmId(tx, entity.getUsername(), User.INTERNAL_REALM_ID);
    dashboardFilterDAO.deleteLegacyByUsername(tx, entity.getUsername());

    // Cascade to user filters
    userFilterDAO.deleteByUsernameAndRealmId(tx, entity.getUsername(), User.INTERNAL_REALM_ID);

    // Cascade to user viewed product notifications
    userViewedProductNotificationDAO.deleteByUsernameAndRealmId(tx, entity.getUsername(), User.INTERNAL_REALM_ID);
    userViewedProductNotificationDAO.deleteLegacyByUsername(tx, entity.getUsername());

    // Cascade to userIdePolicyEvaluation
    userIdePolicyEvaluationDAO.deleteByUsername(tx, entity.getUsername());

    // Cascade to apiAccessAllowList system configuration
    deleteFromApiAccessAllowList(tx, entity.getUsername());

    // Delete user from database
    super.delete(tx, entity);
  }

  private void deleteFromApiAccessAllowList(final TransactionContext tx, final String username) {
    SystemConfigurationProperty configurationProperty =
        systemConfigurationPropertyDAO.getByName(tx, SystemConfigurationProperty.API_ACCESS_ALLOW_LIST);
    if (configurationProperty != null && StringUtils.isNotEmpty(configurationProperty.getValue())) {
      try {
        List<String> list = JsonUtils.parse(configurationProperty.getValue(), List.class);
        List<String> updatedList = list.stream().filter(id -> !id.equals(username)).collect(Collectors.toList());
        if (updatedList.size() < list.size()) {
          if (updatedList.isEmpty()) {
            systemConfigurationPropertyDAO.delete(tx, configurationProperty);
          }
          else {
            configurationProperty.setValue(JsonUtils.writeUnformatted(updatedList));
            systemConfigurationPropertyDAO.update(tx, configurationProperty);
          }
        }
      }
      catch (IOException e) {
        throw new UncheckedIOException("Invalid json: " + configurationProperty.getValue(), e);
      }
    }
  }

  @Override
  public List<User> getAll() {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(USER)
          .orderBy(USER.USERNAME)
          .fetchInto(User.class);
    }
  }

  @Override
  public Table<?> getJooqTable() {
    return USER;
  }

  @Override
  public Class<User> getEntityClass() {
    return User.class;
  }
}
