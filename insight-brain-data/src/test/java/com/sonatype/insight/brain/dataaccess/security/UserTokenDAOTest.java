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
import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.security.OAuth2User;
import com.sonatype.insight.brain.model.security.SamlUser;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.security.UserToken;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

public class UserTokenDAOTest
    extends AbstractDbDAOTest
{
  private final Date december30 = new GregorianCalendar(2019, Calendar.DECEMBER, 30).getTime();

  private final Date december29 = new GregorianCalendar(2019, Calendar.DECEMBER, 29).getTime();

  private final Date december28 = new GregorianCalendar(2019, Calendar.DECEMBER, 28).getTime();

  private final Date december27 = new GregorianCalendar(2019, Calendar.DECEMBER, 27).getTime();

  private UserTokenDAO userTokenDAO;

  @Before
  @Override
  public void setup() {
    super.setup();
    userTokenDAO = daoFactory.createUserTokenDAO();
  }

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
    assertThat(byUsername.getLastAccessTime()).isNull();

    // Only last access time can be updated
    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> {
      UserToken token = userTokenDAO.getById(userToken.getId());
      token.setUsername(token.getUsername() + "2");
      userTokenDAO.update(token);
    }).withMessage("Cannot update anything except last access time.");
    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> {
      UserToken token = userTokenDAO.getById(userToken.getId());
      token.setUserCode(token.getUserCode() + "2");
      userTokenDAO.update(token);
    }).withMessage("Cannot update anything except last access time.");
    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> {
      UserToken token = userTokenDAO.getById(userToken.getId());
      token.setPassCode(token.getPassCode() + "2");
      userTokenDAO.update(token);
    }).withMessage("Cannot update anything except last access time.");
    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> {
      UserToken token = userTokenDAO.getById(userToken.getId());
      token.setRealmId(token.getRealmId() + "2");
      userTokenDAO.update(token);
    }).withMessage("Cannot update anything except last access time.");
    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> {
      UserToken token = userTokenDAO.getById(userToken.getId());
      token.setCreateTime(new Date(0));
      userTokenDAO.update(token);
    }).withMessage("Cannot update anything except last access time.");

    byUsername.setLastAccessTime(new Date());
    userTokenDAO.update(byUsername);
    UserToken updated = userTokenDAO.getById(userToken.getId());
    assertThat(updated.getUsername()).isEqualTo(byUsername.getUsername());
    assertThat(updated.isInternalUser()).isEqualTo(byUsername.isInternalUser());
    assertThat(updated.getUserCode()).isEqualTo(byUsername.getUserCode());
    assertThat(updated.getPassCode()).isEqualTo(byUsername.getPassCode());
    assertThat(updated.getRealmId()).isEqualTo(byUsername.getRealmId());
    assertThat(updated.getCreateTime()).isEqualTo(byUsername.getCreateTime());
    assertThat(updated.getLastAccessTime()).isEqualTo(byUsername.getLastAccessTime());

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

    assertThat(userTokenDAO.getByCreateDateBetweenAndRealmId(null, null, null))
        .extracting(UserToken::getCreateTime)
        .containsExactlyInAnyOrder(december27, december28, december29, december30);
  }

  @Test
  public void testGetByCreateDateBetween_CreatedAfter() {
    tempEntity.newUserToken("foo", december27);
    tempEntity.newUserToken("bar", december28);
    tempEntity.newUserToken("baz", december29);
    tempEntity.newUserToken("qux", december30);

    assertThat(userTokenDAO.getByCreateDateBetweenAndRealmId(december28, null, null))
        .extracting(UserToken::getCreateTime)
        .containsExactlyInAnyOrder(december28, december29, december30);
  }

  @Test
  public void testGetByCreateDateBetween_CreatedBefore() {
    tempEntity.newUserToken("foo", december27);
    tempEntity.newUserToken("bar", december28);
    tempEntity.newUserToken("baz", december29);
    tempEntity.newUserToken("qux", december30);

    assertThat(userTokenDAO.getByCreateDateBetweenAndRealmId(null, december28, null))
        .extracting(UserToken::getCreateTime)
        .containsExactlyInAnyOrder(december27, december28);
  }

  @Test
  public void testGetByCreateDateBetweenAndRealmId_All() {
    tempEntity.newUserToken("foo", User.INTERNAL_REALM_ID);
    tempEntity.newUserToken("bar", User.INTERNAL_REALM_ID);
    tempEntity.newUserToken("baz", SamlUser.SAML_REALM_ID);
    tempEntity.newUserToken("qux", "ldapServerId");

    assertThat(userTokenDAO.getByCreateDateBetweenAndRealmId(null, null, User.INTERNAL_REALM_ID))
        .hasSize(2)
        .extracting(UserToken::getRealmId)
        .containsOnly(User.INTERNAL_REALM_ID);
    assertThat(userTokenDAO.getByCreateDateBetweenAndRealmId(null, null, SamlUser.SAML_REALM_ID))
        .hasSize(1)
        .extracting(UserToken::getRealmId)
        .containsOnly(SamlUser.SAML_REALM_ID);
  }

  @Test
  public void testGetByCreateDateBetweenAndRealmId_BetweenDates() {
    UserToken internal1 = tempEntity.newUserToken("foo", User.INTERNAL_REALM_ID, december27);
    UserToken internal2 = tempEntity.newUserToken("bar", User.INTERNAL_REALM_ID, december29);
    UserToken internal3 = tempEntity.newUserToken("baz", User.INTERNAL_REALM_ID, december30);
    UserToken saml1 = tempEntity.newUserToken("pid", SamlUser.SAML_REALM_ID, december27);
    tempEntity.newUserToken("qux", SamlUser.SAML_REALM_ID, december29);

    assertThat(userTokenDAO.getByCreateDateBetweenAndRealmId(december27, december28, User.INTERNAL_REALM_ID))
        .hasSize(1)
        .extracting("id", "realmId")
        .containsOnly(tuple(internal1.getId(), User.INTERNAL_REALM_ID));
    assertThat(userTokenDAO.getByCreateDateBetweenAndRealmId(december27, december28, SamlUser.SAML_REALM_ID))
        .hasSize(1)
        .extracting("id", "realmId")
        .containsOnly(tuple(saml1.getId(), SamlUser.SAML_REALM_ID));
    assertThat(userTokenDAO.getByCreateDateBetweenAndRealmId(december28, null, User.INTERNAL_REALM_ID))
        .hasSize(2)
        .extracting("id", "realmId")
        .containsOnly(tuple(internal2.getId(), User.INTERNAL_REALM_ID),
            tuple(internal3.getId(), User.INTERNAL_REALM_ID));
    assertThat(userTokenDAO.getByCreateDateBetweenAndRealmId(null, december30, User.INTERNAL_REALM_ID))
        .hasSize(3)
        .extracting("id", "realmId")
        .containsOnly(tuple(internal1.getId(), User.INTERNAL_REALM_ID),
            tuple(internal2.getId(), User.INTERNAL_REALM_ID), tuple(internal3.getId(), User.INTERNAL_REALM_ID));
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
    UserToken userToken1 = tempEntity.newUserToken("username1", TemporaryEntity.uuid());
    UserToken userToken2 = tempEntity.newUserToken("username2", TemporaryEntity.uuid());
    tempEntity.newUserToken("username3", User.INTERNAL_REALM_ID);
    tempEntity.newUserToken("username4", SamlUser.SAML_REALM_ID);
    tempEntity.newUserToken("username5", OAuth2User.OAUTH2_REALM_ID);

    assertThat(userTokenDAO.getAllLdap()).extracting(UserToken::getUsername)
        .containsExactlyInAnyOrder(userToken1.getUsername(), userToken2.getUsername());
  }

  @Test
  public void testGetByUsernameAndRealmId_Internal() {
    UserToken internalUserToken1 =
        tempEntity.newUserToken("username1", "userCode1", "passCode", User.INTERNAL_REALM_ID);
    tempEntity.newUserToken("username2", "userCode2", "passCode", User.INTERNAL_REALM_ID);
    tempEntity.newUserToken("username1", "userCode3", "passCode", "other");

    assertThat(userTokenDAO.getByUsernameAndRealmId("USERNAME1", User.INTERNAL_REALM_ID))
        .usingRecursiveComparison(JPA.RECURSIVE_COMPARISON_CONFIG)
        .isEqualTo(internalUserToken1);
    assertThat(userTokenDAO.getByUsernameAndRealmId("username1", User.INTERNAL_REALM_ID))
        .usingRecursiveComparison(JPA.RECURSIVE_COMPARISON_CONFIG)
        .isEqualTo(internalUserToken1);
    assertThat(userTokenDAO.getByUsernameAndRealmId("UsErNaMe1", User.INTERNAL_REALM_ID))
        .usingRecursiveComparison(JPA.RECURSIVE_COMPARISON_CONFIG)
        .isEqualTo(internalUserToken1);
  }

  @Test
  public void testGetByUsernameAndRealmId_Saml() {
    UserToken samlUserToken1 =
        tempEntity.newUserToken("username1", "userCode1", "passCode", SamlUser.SAML_REALM_ID);
    tempEntity.newUserToken("username2", "userCode2", "passCode", SamlUser.SAML_REALM_ID);
    tempEntity.newUserToken("username1", "userCode3", "passCode", "other");

    assertThat(userTokenDAO.getByUsernameAndRealmId("USERNAME1", SamlUser.SAML_REALM_ID)).isNull();
    assertThat(userTokenDAO.getByUsernameAndRealmId("username1", SamlUser.SAML_REALM_ID))
        .usingRecursiveComparison(JPA.RECURSIVE_COMPARISON_CONFIG)
        .isEqualTo(samlUserToken1);
    assertThat(userTokenDAO.getByUsernameAndRealmId("UsErNaMe1", SamlUser.SAML_REALM_ID)).isNull();
  }

  @Test
  public void testGetByUsernameAndRealmId_OAuth2() {
    UserToken samlUserToken1 =
        tempEntity.newUserToken("username1", "userCode1", "passCode", OAuth2User.OAUTH2_REALM_ID);
    tempEntity.newUserToken("username2", "userCode2", "passCode", OAuth2User.OAUTH2_REALM_ID);
    tempEntity.newUserToken("username1", "userCode3", "passCode", "other");

    assertThat(userTokenDAO.getByUsernameAndRealmId("USERNAME1", OAuth2User.OAUTH2_REALM_ID)).isNull();
    assertThat(userTokenDAO.getByUsernameAndRealmId("username1", OAuth2User.OAUTH2_REALM_ID))
        .usingRecursiveComparison(JPA.RECURSIVE_COMPARISON_CONFIG)
        .isEqualTo(samlUserToken1);
    assertThat(userTokenDAO.getByUsernameAndRealmId("UsErNaMe1", OAuth2User.OAUTH2_REALM_ID)).isNull();
  }
}
