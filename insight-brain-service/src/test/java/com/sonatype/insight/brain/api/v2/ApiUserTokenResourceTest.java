/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiUserTokenDTO;
import com.sonatype.insight.brain.configuration.ldap.TestLdapServer;
import com.sonatype.insight.brain.dataaccess.security.UserTokenDAO;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.model.security.UserToken;
import com.sonatype.insight.brain.security.InternalRealm;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiUserTokenResourceTest
    extends AbstractResourceTest
{
  @Rule
  public TestLdapServer embeddedTestLdapServer = new TestLdapServer();

  private final UserTokenDAO userTokenDAO = new UserTokenDAO();

  @Test
  public void testCreateUserToken() throws Exception {
    tempEntity.newUser("victor.wooten");

    HttpResponse response = HttpRequest.to(getRestBaseUrl())
        .auth("victor.wooten", "secret").path(PublicApiPaths.USER_TOKEN_RESOURCE_PATH_V2)
        .path(ApiUserTokenResource.CURRENT_USER)
        .post();
    assertResponseStatus(200, response);

    ApiUserTokenDTO userTokenDTO = response.getBody(ApiUserTokenDTO.class);
    assertThat(userTokenDTO.userCode).isNotNull();
    assertThat(userTokenDTO.passCode).isNotNull();
  }

  @Test
  public void testPurgeUserTokens() throws Exception {
    embeddedTestLdapServer.start();
    embeddedTestLdapServer.loadData("/" + getClass().getSimpleName() + "/ldap_users.ldif");

    LdapServer ldapServer = tempEntity.newLdapServer("test");
    tempEntity.newLdapConnection(ldapServer.getId(), embeddedTestLdapServer.getPort());
    tempEntity.newLdapUserMapping(ldapServer.getId());

    // Token for internal user, should not be purged.
    UserToken userTokenInternalUser = tempEntity.newUserToken("JohnDoe", InternalRealm.ID);
    // Token for existing LDAP user, should not be purged.
    UserToken userTokenLdapUserValid = tempEntity.newUserToken("testuser", ldapServer.getId());
    // Token for non-existing LDAP user, should be purged.
    UserToken userTokenLdapUseInvalid = tempEntity.newUserToken("no-such-user", ldapServer.getId());

    HttpResponse response = restRequest().path(PublicApiPaths.USER_TOKEN_RESOURCE_PATH_V2)
        .path(ApiUserTokenResource.PURGE).delete();

    assertResponseStatus(204, response);
    assertThat(userTokenDAO.getById(userTokenInternalUser.getId())).isNotNull();
    assertThat(userTokenDAO.getById(userTokenLdapUserValid.getId())).isNotNull();
    assertThat(userTokenDAO.getById(userTokenLdapUseInvalid.getId())).isNull();
  }

  @Test
  public void testDeleteCurrentUserToken() throws Exception {
    UserToken userToken = tempEntity.newUserToken(getUsername(), InternalRealm.ID);
    HttpResponse response =
        restRequest().path(PublicApiPaths.USER_TOKEN_RESOURCE_PATH_V2).path(ApiUserTokenResource.CURRENT_USER).delete();

    assertResponseStatus(204, response);
    assertThat(userTokenDAO.getById(userToken.getId())).isNull();
  }

  @Test
  public void testGetUserTokensCreatedBetween() throws Exception {
    Date december01 = new GregorianCalendar(2019, Calendar.DECEMBER, 1).getTime();
    Date december15 = new GregorianCalendar(2019, Calendar.DECEMBER, 15).getTime();
    Date december31 = new GregorianCalendar(2019, Calendar.DECEMBER, 31).getTime();

    tempEntity.newUserToken("victor.wooten", december01);
    UserToken userToken = tempEntity.newUserToken("marcus.miller", december15);
    tempEntity.newUserToken("stanley.clarke", december31);

    HttpResponse response = restRequest()
        .path(PublicApiPaths.USER_TOKEN_RESOURCE_PATH_V2) //
        .query("createdAfter", "2019-12-10") //
        .query("createdBefore", "2019-12-20") //
        .get();

    assertResponseStatus(200, response);

    ApiUserTokenDTO[] responseBody = response.getBody(ApiUserTokenDTO[].class);
    assertThat(responseBody.length).isEqualTo(1);
    assertThat(responseBody[0].userCode).isEqualTo(userToken.getUserCode());
    assertThat(responseBody[0].passCode).isNull();
  }

  @Test
  public void testDeleteUserTokenByUserCode() throws Exception {
    UserToken userToken = tempEntity.newUserToken(getUsername(), InternalRealm.ID);
    HttpResponse response = restRequest()
        .path(PublicApiPaths.USER_TOKEN_RESOURCE_PATH_V2)
        .path(ApiUserTokenResource.USER_CODE)
        .parameter(userToken.getUserCode())
        .delete();

    assertResponseStatus(204, response);
    assertThat(userTokenDAO.getById(userToken.getId())).isNull();
  }
}
