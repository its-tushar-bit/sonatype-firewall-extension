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
import com.sonatype.insight.brain.api.v2.dto.ApiUserTokenExistsDTO;
import com.sonatype.insight.brain.configuration.ldap.TestLdapServer;
import com.sonatype.insight.brain.dataaccess.security.UserTokenDAO;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.model.security.SamlUser;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.security.UserToken;
import com.sonatype.insight.brain.security.InternalRealm;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.license.model.LicensedFeature;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiUserTokenResourceTest
    extends AbstractResourceTest
{
  @Rule
  public TestLdapServer embeddedTestLdapServer = new TestLdapServer();

  private final UserTokenDAO userTokenDAO = new UserTokenDAO();

  private final Date december01 = new GregorianCalendar(2019, Calendar.DECEMBER, 1).getTime();

  private final Date december15 = new GregorianCalendar(2019, Calendar.DECEMBER, 15).getTime();

  private final Date december31 = new GregorianCalendar(2019, Calendar.DECEMBER, 31).getTime();

  @Test
  public void testCreateUserToken() throws Exception {
    tempEntity.newUser("victor.wooten");

    HttpResponse response = HttpRequest.to(getRestBaseUrl())
        .auth("victor.wooten", "secret")
        .path(PublicApiPaths.USER_TOKEN_RESOURCE_PATH_V2, DefaultApiUserTokenResource.CURRENT_USER)
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

    HttpResponse response = restRequest().path(DefaultApiUserTokenResource.PURGE).delete();

    assertResponseStatus(204, response);
    assertThat(userTokenDAO.getById(userTokenInternalUser.getId())).isNotNull();
    assertThat(userTokenDAO.getById(userTokenLdapUserValid.getId())).isNotNull();
    assertThat(userTokenDAO.getById(userTokenLdapUseInvalid.getId())).isNull();
  }

  @Test
  public void testDeleteCurrentUserToken() throws Exception {
    UserToken userToken = tempEntity.newUserToken(getUsername(), InternalRealm.ID);
    HttpResponse response = restRequest().path(DefaultApiUserTokenResource.CURRENT_USER).delete();

    assertResponseStatus(204, response);
    assertThat(userTokenDAO.getById(userToken.getId())).isNull();
  }

  @Test
  public void testGetUserTokensByCreatedBetweenAndRealmId_SamlUserTokensDisabled() throws Exception {
    setMissingFeature(LicensedFeature.SAML_USER_TOKENS);
    tempEntity.newUserToken("victor.wooten", User.INTERNAL_REALM_ID , december01);
    UserToken userToken = tempEntity.newUserToken("marcus.miller", User.INTERNAL_REALM_ID, december15);
    tempEntity.newUserToken("stanley.clarke", User.INTERNAL_REALM_ID ,december31);
    tempEntity.newUserToken("zak.crawly", SamlUser.SAML_REALM_ID, december15);

    assertUserToken(userToken, null, false);
    assertUserToken(userToken, "InterNaL", false);
    assertUserToken(userToken, SamlUser.SAML_REALM_ID, false);
  }

  @Test
  public void testGetUserTokensByCreatedBetweenAndRealmId_SamlUserTokensEnabled() throws Exception {
    setFeatures(LicensedFeature.SAML_USER_TOKENS);
    tempEntity.newUserToken("victor.wooten", User.INTERNAL_REALM_ID , december01);
    UserToken internalToken = tempEntity.newUserToken("marcus.miller", User.INTERNAL_REALM_ID, december15);
    tempEntity.newUserToken("stanley.clarke", User.INTERNAL_REALM_ID ,december31);
    UserToken samlToken = tempEntity.newUserToken("zak.crawly", SamlUser.SAML_REALM_ID, december15);

    assertUserToken(internalToken, null, true);
    assertUserToken(internalToken, "InterNaL", true);
    assertUserToken(samlToken, "saMl", true);
  }

  private void assertUserToken(UserToken userToken, String realmId, boolean samlUserTokensEnabled) throws Exception {
    HttpRequest httpRequest = restRequest()
        .query("createdAfter", "2019-12-10") //
        .query("createdBefore", "2019-12-20");
    if (realmId != null) {
      httpRequest.query("realm", realmId);
    }
    HttpResponse response = httpRequest.get();

    assertResponseStatus(200, response);

    ApiUserTokenDTO[] responseBody = response.getBody(ApiUserTokenDTO[].class);
    assertThat(responseBody.length).isEqualTo(1);
    assertThat(responseBody[0].userCode).isEqualTo(userToken.getUserCode());
    assertThat(responseBody[0].username).isEqualTo(userToken.getUsername());
    assertThat(responseBody[0].passCode).isNull();
    ArrayNode json = (ArrayNode) new ObjectMapper().readTree(response.getBodyText());
    if (samlUserTokensEnabled) {
      assertThat(json.get(0).has("realm")).isTrue();
      assertThat(responseBody[0].realm).isEqualTo(userToken.getRealmId());
    }
    else {
      assertThat(json.get(0).has("realm")).isFalse();
      assertThat(responseBody[0].realm).isNull();
    }
  }

  @Test
  public void testDeleteUserTokenByUserCode() throws Exception {
    UserToken userToken = tempEntity.newUserToken(getUsername(), InternalRealm.ID);
    HttpResponse response = restRequest()
        .path(DefaultApiUserTokenResource.USER_CODE)
        .parameter(userToken.getUserCode())
        .delete();

    assertResponseStatus(204, response);
    assertThat(userTokenDAO.getById(userToken.getId())).isNull();
  }

  @Test
  public void testGetUserTokenExistsForCurrentUser() throws Exception {
    HttpResponse response = restRequest().path(DefaultApiUserTokenResource.CURRENT_USER_HAS_TOKEN).get();

    assertResponseStatus(200, response);
    ApiUserTokenExistsDTO responseBody = response.getBody(ApiUserTokenExistsDTO.class);
    assertThat(responseBody.userTokenExists).isFalse();

    tempEntity.newUserToken(getUsername(), InternalRealm.ID);

    response = restRequest().path(DefaultApiUserTokenResource.CURRENT_USER_HAS_TOKEN).get();

    assertResponseStatus(200, response);
    responseBody = response.getBody(ApiUserTokenExistsDTO.class);
    assertThat(responseBody.userTokenExists).isTrue();
  }

  @Test
  public void testGetUserTokenByUsernameAndRealmId_SamlUserTokensEnabled_InternalUnknown() throws Exception {
    setFeatures(LicensedFeature.SAML_USER_TOKENS);

    HttpResponse httpResponse = restRequest().path(DefaultApiUserTokenResource.USERNAME).parameter("unknown").get();

    assertResponseStatus(404, httpResponse);
    assertThat(httpResponse.getBodyText()).contains("No user token found for Internal user unknown.");
  }

  @Test
  public void testGetUserTokenByUsernameAndRealmId_SamlUserTokensEnabled_SamlUnknown() throws Exception {
    setFeatures(LicensedFeature.SAML_USER_TOKENS);

    HttpResponse httpResponse =
        restRequest().path(DefaultApiUserTokenResource.USERNAME).parameter("unknown").query("realm", "SAML").get();

    assertResponseStatus(404, httpResponse);
    assertThat(httpResponse.getBodyText()).contains("No user token found for SAML user unknown.");
  }

  @Test
  public void testGetUserTokenByUsernameAndRealmId_SamlUserTokensDisabled_Unknown() throws Exception {
    setMissingFeature(LicensedFeature.SAML_USER_TOKENS);

    HttpResponse httpResponse = restRequest().path(DefaultApiUserTokenResource.USERNAME).parameter("unknown").get();

    assertResponseStatus(404, httpResponse);
    assertThat(httpResponse.getBodyText()).contains("No user token found for user unknown.");
  }

  @Test
  public void testGetUserTokenByUsernameAndRealmId_SamlUserTokensEnabled_NoRealmId() throws Exception {
    testGetUserTokenByUsernameAndRealmId(true, null);
  }

  @Test
  public void testGetUserTokenByUsernameAndRealmId_SamlUserTokensEnabled_UnknownRealmId() throws Exception {
    testGetUserTokenByUsernameAndRealmId(true, "unknown");
  }

  @Test
  public void testGetUserTokenByUsernameAndRealmId_SamlUserTokensEnabled_InternalRealmId() throws Exception {
    testGetUserTokenByUsernameAndRealmId(true, "InTeRnAl");
  }

  @Test
  public void testGetUserTokenByUsernameAndRealmId_SamlUserTokensEnabled_SamlRealmId() throws Exception {
    testGetUserTokenByUsernameAndRealmId(true, "SaMl");
  }

  @Test
  public void testGetUserTokenByUsernameAndRealmId_SamlUserTokensDisabled_NoRealmId() throws Exception {
    testGetUserTokenByUsernameAndRealmId(false, null);
  }

  @Test
  public void testGetUserTokenByUsernameAndRealmId_SamlUserTokensDisabled_UnknownRealmId() throws Exception {
    testGetUserTokenByUsernameAndRealmId(false, "unknown");
  }

  @Test
  public void testGetUserTokenByUsernameAndRealmId_SamlUserTokensDisabled_InternalRealmId() throws Exception {
    testGetUserTokenByUsernameAndRealmId(false, "InTeRnAl");
  }

  @Test
  public void testGetUserTokenByUsernameAndRealmId_SamlUserTokensDisabled_SamlRealmId() throws Exception {
    testGetUserTokenByUsernameAndRealmId(false, "SaMl");
  }

  private void testGetUserTokenByUsernameAndRealmId(boolean isSamlUserTokensEnabled, String realmId) throws Exception {
    UserToken internalUserToken1 =
        tempEntity.newUserToken("username1", "userCode1", "passCode", User.INTERNAL_REALM_ID);
    tempEntity.newUserToken("username2", User.INTERNAL_REALM_ID);
    UserToken samlUserToken1 = tempEntity.newUserToken("username1", "userCode2", "passCode", SamlUser.SAML_REALM_ID);
    tempEntity.newUserToken("username2", "userCode3", "passCode", SamlUser.SAML_REALM_ID);
    tempEntity.newUserToken("username1", "userCode4", "passCode", "other");
    tempEntity.newUserToken("username2", "userCode5", "passCode", "other");
    if (isSamlUserTokensEnabled) {
      setFeatures(LicensedFeature.SAML_USER_TOKENS);
    }
    else {
      setMissingFeature(LicensedFeature.SAML_USER_TOKENS);
    }
    HttpRequest httpRequest = restRequest().path(DefaultApiUserTokenResource.USERNAME).parameter("username1");
    if (realmId != null) {
      httpRequest.query("realm", realmId);
    }

    HttpResponse httpResponse = httpRequest.get();

    assertResponseStatus(200, httpResponse);
    ApiUserTokenDTO result = httpResponse.getBody(ApiUserTokenDTO.class);
    String expectedRealmId;
    if (isSamlUserTokensEnabled && SamlUser.SAML_REALM_ID.equalsIgnoreCase(realmId)) {
      assertThat(result.userCode).isEqualTo(samlUserToken1.getUserCode());
      expectedRealmId = SamlUser.SAML_REALM_ID;
    }
    else {
      assertThat(result.userCode).isEqualTo(internalUserToken1.getUserCode());
      expectedRealmId = User.INTERNAL_REALM_ID;
    }
    assertThat(result.passCode).isNull();
    assertThat(result.username).isEqualTo("username1");
    JsonNode json = new ObjectMapper().readTree(httpResponse.getBodyText());
    if (isSamlUserTokensEnabled) {
      assertThat(json.has("realm")).isTrue();
      assertThat(result.realm).isEqualTo(expectedRealmId);
    }
    else {
      assertThat(json.has("realm")).isFalse();
      assertThat(result.realm).isNull();
    }
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.USER_TOKEN_RESOURCE_PATH_V2);
  }
}
