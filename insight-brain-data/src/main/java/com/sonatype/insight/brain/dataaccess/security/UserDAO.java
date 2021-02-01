/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.filter.DashboardFilterDAO;
import com.sonatype.insight.brain.dataaccess.filter.UserFilterDAO;
import com.sonatype.insight.brain.dataaccess.notification.UserViewedProductNotificationDAO;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.security.UserToken;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;

/**
 * @since 1.7
 */
public class UserDAO
    extends AbstractOperationalSqlDAO<User>
{
  public static final int MAX_FIRST_NAME_SIZE = 100;

  public static final int MAX_LAST_NAME_SIZE = 100;

  public static final int MAX_EMAIL_SIZE = 255;

  public User getByUsername(TransactionContext tx, String username) {
    String sQuery = "SELECT entity FROM User entity" + //
        " WHERE entity.usernameLowercase=?1";
    return get(tx, sQuery, User.normalizeUsername(username));
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

    String sQuery = "SELECT entity from User entity" + //
        " WHERE entity.usernameLowercase IN ?1" + //
        " ORDER BY entity.usernameLowercase";
    return getList(sQuery, lowerCaseUsernames);
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
    String sQuery = "SELECT entity FROM User entity" + //
        " WHERE lower(concat(entity.firstName, ' ', entity.lastName)) LIKE ?1" + //
        " ORDER BY entity.username";
    return getList(sQuery, nameQuery);
  }

  @Override
  protected User getById(TransactionContext tx, String id) {
    String sQuery = "SELECT entity FROM User entity" + //
        " WHERE entity.id=?1";
    return get(tx, sQuery, id);
  }

  public User getByIdNotNull(TransactionContext tx, String id) {
    User user = getById(tx, id);
    if (user == null) {
      throw new NotFoundException("Cannot find a user with ID " + id + ".");
    }
    return user;
  }

  public User getByIdNotNull(String id) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByIdNotNull(tx, id);
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
  public void insert(TransactionContext tx, User user) {
    validate(user);

    if (getByUsername(tx, user.getUsername()) != null) {
      throw new InvalidNameException(user.getUsername() + " is already used as a username.");
    }

    super.insert(tx, user);
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
    MembershipMappingDAO membershipMappingDAO = new MembershipMappingDAO();
    for (MembershipMapping membershipMapping : membershipMappingDAO.getByUser(tx, entity.getUsername())) {
      membershipMappingDAO.delete(tx, membershipMapping);
    }

    // Cascade to user token
    UserTokenDAO userTokenDAO = new UserTokenDAO();
    UserToken userToken = userTokenDAO.getInternalByUsername(tx, entity.getUsername());
    if (userToken != null) {
      userTokenDAO.delete(tx, userToken);
    }

    // Cascade to dashboard filters
    DashboardFilterDAO dashboardFilterDAO = new DashboardFilterDAO();
    dashboardFilterDAO.deleteByUsernameAndRealmId(tx, entity.getUsername(), User.INTERNAL_REALM_ID);
    dashboardFilterDAO.deleteLegacyByUsername(tx, entity.getUsername());

    // Cascade to user filters
    UserFilterDAO userFilterDAO = new UserFilterDAO();
    userFilterDAO.deleteByUsernameAndRealmId(tx, entity.getUsername(), User.INTERNAL_REALM_ID);

    // Cascade to user viewed product notifications
    UserViewedProductNotificationDAO userViewedProductNotificationDAO = new UserViewedProductNotificationDAO();
    userViewedProductNotificationDAO.deleteByUsernameAndRealmId(tx, entity.getUsername(), User.INTERNAL_REALM_ID);
    userViewedProductNotificationDAO.deleteLegacyByUsername(tx, entity.getUsername());

    super.delete(tx, entity);
  }

  public List<User> getAll() {
    String sQuery = "SELECT entity FROM User entity" + //
        " ORDER BY entity.username";
    return getList(sQuery);
  }
}
