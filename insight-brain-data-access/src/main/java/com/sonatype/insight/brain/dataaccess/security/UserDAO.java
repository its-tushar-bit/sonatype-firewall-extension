/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.security;

import java.util.Locale;

import javax.persistence.EntityManager;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.error.exception.NotFoundException;

/**
 * @since 1.7
 */
public class UserDAO
    extends AbstractOperationalSqlDAO<User>
{
  private User getByUsernameLowercase(EntityManager em, String usernameLowercase) {
    String sQuery = "SELECT entity FROM User entity" + //
        " WHERE entity.usernameLowercase=?1";
    return get(em, sQuery, usernameLowercase);
  }

  public User getByUsernameLowercase(String usernameLowercase) {
    EntityManager em = createEntityManager();
    try {
      return getByUsernameLowercase(em, usernameLowercase);
    }
    finally {
      close(em);
    }
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
    if (username == null || username.isEmpty()) {
      throw new InvalidNameException("The username cannot be null or empty");
    }
    if (username.contains(" ")) {
      throw new InvalidNameException("The username cannot contain spaces");
    }
    NameHelper.validate("The username", username);
  }

  @Override
  public void insert(EntityManager em, User user) {
    validateUsername(user.getUsername());

    if (getByUsernameLowercase(em, user.getUsername().toLowerCase(Locale.ENGLISH)) != null) {
      throw new InvalidNameException(user.getUsername() + " is already used as a username.");
    }

    super.insert(em, user);
  }

  @Override
  public void update(EntityManager em, User user) {
    validateUsername(user.getUsername());

    User existingUser = getByUsernameLowercase(em, user.getUsername().toLowerCase(Locale.ENGLISH));
    if (existingUser != null && !existingUser.getId().equals(user.getId())) {
      throw new InvalidNameException(user.getUsername() + " is already used as a username.");
    }

    super.update(em, user);
  }
}
