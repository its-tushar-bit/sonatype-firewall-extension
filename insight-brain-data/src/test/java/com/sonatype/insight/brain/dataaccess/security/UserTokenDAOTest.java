/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.security;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.security.SamlUser;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.security.UserToken;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class UserTokenDAOTest
    extends AbstractDbDAOTest
{
  private final UserTokenDAO userTokenDAO = new UserTokenDAO();

  private final Date december30 = new GregorianCalendar(2019, Calendar.DECEMBER, 30).getTime();

  private final Date december29 = new GregorianCalendar(2019, Calendar.DECEMBER, 29).getTime();

  private final Date december28 = new GregorianCalendar(2019, Calendar.DECEMBER, 28).getTime();

  private final Date december27 = new GregorianCalendar(2019, Calendar.DECEMBER, 27).getTime();

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

  @Test
  public void testGetByCreateDateBetween() {
    tempEntity.newUserToken("foo", december27);
    tempEntity.newUserToken("bar", december28);
    tempEntity.newUserToken("baz", december29);
    tempEntity.newUserToken("qux", december30);

    assertThat(userTokenDAO.getByCreateDateBetween(null, null))
        .extracting(UserToken::getCreateTime)
        .containsExactlyInAnyOrder(december27, december28, december29, december30);
  }

  @Test
  public void testGetByCreateDateBetween_CreatedAfter() {
    tempEntity.newUserToken("foo", december27);
    tempEntity.newUserToken("bar", december28);
    tempEntity.newUserToken("baz", december29);
    tempEntity.newUserToken("qux", december30);

    assertThat(userTokenDAO.getByCreateDateBetween(december28, null))
        .extracting(UserToken::getCreateTime)
        .containsExactlyInAnyOrder(december28, december29, december30);
  }

  @Test
  public void testGetByCreateDateBetween_CreatedBefore() {
    tempEntity.newUserToken("foo", december27);
    tempEntity.newUserToken("bar", december28);
    tempEntity.newUserToken("baz", december29);
    tempEntity.newUserToken("qux", december30);

    assertThat(userTokenDAO.getByCreateDateBetween(null, december28))
        .extracting(UserToken::getCreateTime)
        .containsExactlyInAnyOrder(december27, december28);
  }

  @Test
  public void testGetByCreateDateBetween_CreatedAfterAndBefore() {
    tempEntity.newUserToken("foo", december27);
    tempEntity.newUserToken("bar", december28);
    tempEntity.newUserToken("baz", december29);
    tempEntity.newUserToken("qux", december30);

    assertThat(userTokenDAO.getByCreateDateBetween(december27, december28))
        .extracting(UserToken::getCreateTime)
        .containsExactlyInAnyOrder(december27, december28);
  }

  @Test
  public void testUserTokenExists() {
    String username = "John.Doe";
    String realmId = "testRealmId";

    assertThat(userTokenDAO.userTokenExists(username, realmId)).isFalse();

    UserToken userToken = tempEntity.newUserToken(username, realmId);
    assertThat(userTokenDAO.userTokenExists(username, realmId)).isTrue();

    userTokenDAO.delete(userToken);
    assertThat(userTokenDAO.userTokenExists(username, realmId)).isFalse();
  }

  @Test
  public void testGetAllLdap() {
    UserToken userToken1 = tempEntity.newUserToken("username1", tempEntity.uuid());
    UserToken userToken2 = tempEntity.newUserToken("username2", tempEntity.uuid());
    tempEntity.newUserToken("username3", User.INTERNAL_REALM_ID);
    tempEntity.newUserToken("username4", SamlUser.SAML_REALM_ID);

    assertThat(userTokenDAO.getAllLdap()).extracting(UserToken::getUsername)
        .containsExactlyInAnyOrder(userToken1.getUsername(), userToken2.getUsername());
  }
}
