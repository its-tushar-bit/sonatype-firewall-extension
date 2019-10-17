/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.security;

import java.util.Date;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.security.UserToken;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class UserTokenDAOTest
    extends AbstractDbDAOTest
{
  private UserTokenDAO userTokenDAO = new UserTokenDAO();

  @Test
  public void testCrud() {
    // Create
    Date start = new Date();
    UserToken userToken = tempEntity.newUserToken("John.Doe", false /* isInternalUser */);
    Date end = new Date();
    assertThat(userToken.getId()).isNotNull();

    // Read
    UserToken byUsername = userTokenDAO.getByUsername("jOhn.dOe");
    assertThat(byUsername.getUsername()).isEqualTo("John.Doe");
    assertThat(byUsername.getUsernameLowercase()).isEqualTo("john.doe");
    assertThat(byUsername.isInternalUser()).isFalse();
    assertThat(byUsername.getUserCode()).isEqualTo(userToken.getUserCode());
    assertThat(byUsername.getPassCode()).isEqualTo(userToken.getPassCode());
    assertThat(byUsername.getCreateTime()).isBetween(start, end, true, true);

    // Update is not allowed.
    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> userTokenDAO.update(byUsername))
        .withMessage("The UserToken table does not support update operations.");

    // Delete
    userTokenDAO.delete(userToken);
    assertThat(userTokenDAO.getById(userToken.getId())).isNull();
  }

  @Test
  public void testInsertUserToken_Duplicate() {
    tempEntity.newUserToken("John.Doe", false /* isInternalUser */);
    assertThatThrownBy(() -> tempEntity.newUserToken("jOhn.doe", false /* isInternalUser */));
    assertThatThrownBy(() -> tempEntity.newUserToken("jOhn.doe", true /* isInternalUser */));
  }

  @Test
  public void testGetByUsername_DoesNotExist() {
    assertThat(userTokenDAO.getByUsername("no_such_user")).isNull();
  }

  @Test
  public void testGetByUsername_CaseInsensitive() {
    UserToken userToken = tempEntity.newUserToken("John.Doe", false /* isInternalUser */);
    assertThat(userTokenDAO.getByUsername("jOhn.dOe").getId()).isEqualTo(userToken.getId());
  }
}
