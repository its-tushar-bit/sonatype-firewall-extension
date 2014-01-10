/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.security;

import java.util.List;
import java.util.Locale;

import javax.persistence.EntityManager;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.User;
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

  private User getByUsername(EntityManager em, String username) {
    String sQuery = "SELECT entity FROM User entity" + //
        " WHERE entity.usernameLowercase=?1";
    return get(em, sQuery, username.toLowerCase(Locale.ENGLISH));
  }

  /**
   * Looks up a user by its (case-insensitive) username.
   * 
   * @param username The username to look up, must not be {@code null}.
   * @return The user or {@code null} if not found.
   */
  public User getByUsername(String username) {
    EntityManager em = createEntityManager();
    try {
      return getByUsername(em, username);
    }
    finally {
      close(em);
    }
  }

  /**
   * Find users in the database by matching against the first or last name
   * 
   * @param nameFragment This string will be prefixed and suffixed with wildcard characters and passed into the sql query
   * @return List of matching User objects
   */
  public List<User> findUsersByName(String nameFragment) {
    nameFragment = '%' + nameFragment.trim().toLowerCase(Locale.ENGLISH) + '%';
    String sQuery = "SELECT entity from User entity WHERE lower(entity.firstName) LIKE ?1" //
        + " OR lower(entity.lastName) LIKE ?2";
    return getList(sQuery, nameFragment, nameFragment);
  }

  @Override
  protected User getById(EntityManager em, String id) {
    String sQuery = "SELECT entity FROM User entity" + //
        " WHERE entity.id=?1";
    return get(em, sQuery, id);
  }

  public User getByIdNotNull(EntityManager em, String id) {
    User user = getById(em, id);
    if (user == null) {
      throw new NotFoundException("Cannot find a user with id " + id);
    }
    return user;
  }

  public User getByIdNotNull(String id) {
    EntityManager em = createEntityManager();
    try {
      return getByIdNotNull(em, id);
    }
    finally {
      close(em);
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

    if (user.getFirstName() == null || user.getFirstName().trim().isEmpty()) {
      throw new InvalidUserException("The first name is required.");
    }
    if (user.getFirstName().length() > MAX_FIRST_NAME_SIZE) {
      throw new InvalidUserException("The first name must be " + MAX_FIRST_NAME_SIZE + " characters or less.");
    }

    if (user.getLastName() == null || user.getLastName().trim().isEmpty()) {
      throw new InvalidUserException("The last name is required.");
    }
    if (user.getLastName().length() > MAX_LAST_NAME_SIZE) {
      throw new InvalidUserException("The last name must be " + MAX_LAST_NAME_SIZE + " characters or less.");
    }

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
  public void insert(EntityManager em, User user) {
    validate(user);

    if (getByUsername(em, user.getUsername()) != null) {
      throw new InvalidNameException(user.getUsername() + " is already used as a username.");
    }

    super.insert(em, user);
  }

  @Override
  public void update(EntityManager em, User user) {
    validate(user);

    User existingUser = getByUsername(em, user.getUsername());
    if (existingUser != null && !existingUser.getId().equals(user.getId())) {
      throw new InvalidNameException(user.getUsername() + " is already used as a username.");
    }

    super.update(em, user);
  }

  @Override
  public void delete(EntityManager em, User entity) {
    // Cascade to membership mappings
    MembershipMappingDAO membershipMappingDAO = new MembershipMappingDAO();
    for (MembershipMapping membershipMapping : membershipMappingDAO.getByUser(em, entity.getUsername())) {
      membershipMappingDAO.delete(em, membershipMapping);
    }

    super.delete(em, entity);
  }

  public List<User> getAll() {
    String sQuery = "SELECT entity FROM User entity" + //
        " ORDER BY entity.username";
    return getList(sQuery);
  }
}
