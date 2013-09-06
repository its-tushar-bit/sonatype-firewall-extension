/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.security;

import javax.persistence.EntityManager;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.error.exception.NotFoundException;

/**
 * @since 1.7
 */
public class UserDAO
    extends AbstractOperationalSqlDAO<User>
{
  public User getByUsernameLowercase(String usernameLowercase) {
    String sQuery = "SELECT entity FROM User entity" + //
        " WHERE entity.usernameLowercase=?1";
    return get(sQuery, usernameLowercase);
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
}
