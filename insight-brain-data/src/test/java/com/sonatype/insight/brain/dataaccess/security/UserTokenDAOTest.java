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
    String username = "John.Doe";
    String realmId = "testRealmId";

    // Create
    Date start = new Date();
    UserToken userToken = tempEntity.newUserToken(username, realmId);
    Date end = new Date();
    assertThat(userToken.getId()).isNotNull();

    // Read
    UserToken byUsername = userTokenDAO.getById(userToken.getId());
    assertThat(byUsername.getUsername()).isEqualTo(username);
    assertThat(byUsername.isInternalUser()).isFalse();
    assertThat(byUsername.getUserCode()).isEqualTo(userToken.getUserCode());
    assertThat(byUsername.getPassCode()).isEqualTo(userToken.getPassCode());
    assertThat(byUsername.getRealmId()).isEqualTo(realmId);
    assertThat(byUsername.getCreateTime()).isBetween(start, end, true, true);

    // Update is not allowed.
    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> userTokenDAO.update(byUsername))
        .withMessage("The UserToken table does not support update operations.");

    // Delete
    userTokenDAO.delete(userToken);
    assertThat(userTokenDAO.getById(userToken.getId())).isNull();
  }

  @Test
  public void testInsertUserToken_DuplicateUserCode() {
    String userCode = "testUserCode";
    tempEntity.newUserToken("testUsername", userCode, "testPassCode", "testRealmId");
    assertThatThrownBy(() -> tempEntity.newUserToken("testUsername1", userCode, "testPassCode1", "testRealmId1"));
  }

  @Test
  public void testInsertUserToken_DuplicateUsernameAndRealmId() {
    String username = "testUsername";
    String realmId = "testRealmId";
    tempEntity.newUserToken(username, "testUserCode", "testPassCode", realmId);
    assertThatThrownBy(() -> tempEntity.newUserToken(username, "testUserCode1", "testPassCode1", realmId));
  }
}
