/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.security;

import java.util.Locale;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.security.User;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * @since 1.7
 */
public class UserDAOTest
    extends AbstractDbDAOTest
{
  @Test
  public void testGetByUsernameLowercase() throws Exception {
    UserDAO dao = new UserDAO();
    User user = dao.getByUsernameLowercase(User.ADMIN_USERNAME);
    assertThat(user, notNullValue());
    assertThat(user.getUsername(), is(User.ADMIN_USERNAME));
    assertThat(user.getUsernameLowercase(), is(User.ADMIN_USERNAME));

    user = dao.getByUsernameLowercase(User.ADMIN_USERNAME.toUpperCase(Locale.ENGLISH));
    assertThat(user, nullValue());
  }
}
