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
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.security.UserToken;
import com.sonatype.insight.brain.security.CrowdRealm;
import com.sonatype.insight.brain.security.InternalRealm;
import com.sonatype.insight.brain.security.SamlRealm;
import com.sonatype.insight.brain.security.SsoUserService;
import com.sonatype.insight.brain.security.oauth2.OAuth2Realm;
import com.sonatype.insight.brain.variant.IqH2Test;
import com.sonatype.insight.brain.variant.IqTestContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kept in the original package/simple name because {@link #testPurgeUserTokens} resolves the LDAP fixture
 * via {@code getClass().getSimpleName()}.
 */
@IqH2Test
public class ApiUserTokenResourceTest
{
  private final Date december01 = new GregorianCalendar(2019, Calendar.DECEMBER, 1).getTime();

  private final Date december15 = new GregorianCalendar(2019, Calendar.DECEMBER, 15).getTime();

  private final Date december31 = new GregorianCalendar(2019, Calendar.DECEMBER, 31).getTime();

  private IqTestContext ctx;

  private final TestLdapServer embeddedTestLdapServer = new TestLdapServer();

  private UserTokenDAO userTokenDAO;

  @BeforeEach
  public void setup() {
    userTokenDAO = ctx.lookup(UserTokenDAO.class);
  }

  @AfterEach
  public void tearDown() throws Exception {
    embeddedTestLdapServer.stop();
  }

  @Test
  public void testCreateUserToken() throws Exception {
    ctx.tempEntity().newUser("victor.wooten");

    HttpResponse response = HttpRequest.to(ctx.restBaseUrl())
        .auth("victor.wooten", "secret")
        .path(PublicApiPaths.USER_TOKEN_RESOURCE_PATH_V2, ApiUserTokenResource.CURRENT_USER)
        .post();
    ctx.assertResponseStatus(200, response);

    ApiUserTokenDTO userTokenDTO = response.getBody(ApiUserTokenDTO.class);
    assertThat(userTokenDTO.userCode).isNotNull();
    assertThat(userTokenDTO.passCode).isNotNull();
  }

  @Test
  public void testPurgeUserTokens() throws Exception {
    embeddedTestLdapServer.start();
    embeddedTestLdapServer.loadData("/" + getClass().getSimpleName() + "/ldap_users.ldif");

    LdapServer ldapServer = ctx.tempEntity().newLdapServer("test");
    ctx.tempEntity().newLdapConnection(ldapServer.getId(), embeddedTestLdapServer.getPort());
    ctx.tempEntity().newLdapUserMapping(ldapServer.getId());

    // Token for internal user, should not be purged.
    UserToken userTokenInternalUser = ctx.tempEntity().newUserToken("JohnDoe", InternalRealm.ID);
    // Token for existing LDAP user, should not be purged.
    UserToken userTokenLdapUserValid = ctx.tempEntity().newUserToken("testuser", ldapServer.getId());
    // Token for non-existing LDAP user, should be purged.
    UserToken userTokenLdapUseInvalid = ctx.tempEntity().newUserToken("no-such-user", ldapServer.getId());

    HttpResponse response = restRequest().path(ApiUserTokenResource.PURGE).delete();

    ctx.assertResponseStatus(204, response);
    assertThat(userTokenDAO.getById(userTokenInternalUser.getId())).isNotNull();
    assertThat(userTokenDAO.getById(userTokenLdapUserValid.getId())).isNotNull();
    assertThat(userTokenDAO.getById(userTokenLdapUseInvalid.getId())).isNull();
  }

  @Test
  public void testDeleteCurrentUserToken() throws Exception {
    UserToken userToken = ctx.tempEntity().newUserToken(ctx.getUsername(), InternalRealm.ID);
    HttpResponse response = restRequest().path(ApiUserTokenResource.CURRENT_USER).delete();

    ctx.assertResponseStatus(204, response);
    assertThat(userTokenDAO.getById(userToken.getId())).isNull();
  }

  @Test
  public void testGetUserTokensByCreatedBetweenAndRealmId_CrowdIntegrationDisabled() throws Exception {
    SystemConfigurationPropertyFeature.CROWD_INTEGRATION.setEnabled(false);
    ctx.tempEntity().newUserToken("victor.wooten", User.INTERNAL_REALM_ID, december01);
    UserToken userToken = ctx.tempEntity().newUserToken("marcus.miller", User.INTERNAL_REALM_ID, december15);
    ctx.tempEntity().newUserToken("stanley.clarke", User.INTERNAL_REALM_ID, december31);
    UserToken samlToken = ctx.tempEntity().newUserToken("zak.crawly", SamlRealm.ID, december15);
    UserToken oauth2Token = ctx.tempEntity().newUserToken("john.doe", OAuth2Realm.ID, december15);

    assertUserToken(userToken, null);
    assertUserToken(userToken, "InterNaL");
    assertUserToken(samlToken, SamlRealm.ID);
    assertUserToken(oauth2Token, OAuth2Realm.ID);
  }

  @Test
  public void testGetUserTokensByCreatedBetweenAndRealmId() throws Exception {
    ctx.tempEntity().newUserToken("victor.wooten", User.INTERNAL_REALM_ID, december01);
    UserToken internalToken = ctx.tempEntity().newUserToken("marcus.miller", User.INTERNAL_REALM_ID, december15);
    ctx.tempEntity().newUserToken("stanley.clarke", User.INTERNAL_REALM_ID, december31);
    UserToken samlToken = ctx.tempEntity().newUserToken("zak.crawly", SamlRealm.ID, december15);
    UserToken oauth2Token = ctx.tempEntity().newUserToken("john.doe", OAuth2Realm.ID, december15);

    assertUserToken(internalToken, null);
    assertUserToken(internalToken, "InterNaL");
    assertUserToken(samlToken, "saMl");
    assertUserToken(oauth2Token, "OaUth2");
  }

  @Test
  public void testGetUserTokensByCreatedBetweenAndRealmId_CrowdIntegrationFeatureDisabled() throws Exception {
    SystemConfigurationPropertyFeature.CROWD_INTEGRATION.setEnabled(false);
    ctx.tempEntity().newUserToken("victor.wooten", User.INTERNAL_REALM_ID, december01);
    UserToken userToken = ctx.tempEntity().newUserToken("marcus.miller", User.INTERNAL_REALM_ID, december15);
    ctx.tempEntity().newUserToken("stanley.clarke", User.INTERNAL_REALM_ID, december31);
    ctx.tempEntity().newUserToken("zak.crawly", CrowdRealm.ID, december15);

    assertUserToken(userToken, null);
    assertUserToken(userToken, "InterNaL");
    assertUserToken(userToken, CrowdRealm.ID);
  }

  @Test
  public void testGetUserTokensByCreatedBetweenAndRealmId_CrowdIntegrationFeatureEnabled() throws Exception {
    ctx.tempEntity().newUserToken("victor.wooten", User.INTERNAL_REALM_ID, december01);
    UserToken internalToken = ctx.tempEntity().newUserToken("marcus.miller", User.INTERNAL_REALM_ID, december15);
    ctx.tempEntity().newUserToken("stanley.clarke", User.INTERNAL_REALM_ID, december31);
    UserToken crowdToken = ctx.tempEntity().newUserToken("zak.crawly", CrowdRealm.ID, december15);

    assertUserToken(internalToken, null);
    assertUserToken(internalToken, "InterNaL");
    assertUserToken(crowdToken, "cRoWd");
  }

  private void assertUserToken(UserToken userToken, String realmId) throws Exception {
    HttpRequest httpRequest = restRequest()
        .query("createdAfter", "2019-12-10") //
        .query("createdBefore", "2019-12-20");
    if (realmId != null) {
      httpRequest.query("realm", realmId);
    }
    HttpResponse response = httpRequest.get();

    ctx.assertResponseStatus(200, response);

    ApiUserTokenDTO[] responseBody = response.getBody(ApiUserTokenDTO[].class);
    assertThat(responseBody.length).isEqualTo(1);
    assertThat(responseBody[0].userCode).isEqualTo(userToken.getUserCode());
    assertThat(responseBody[0].username).isEqualTo(userToken.getUsername());
    assertThat(responseBody[0].passCode).isNull();
    ArrayNode json = (ArrayNode) new ObjectMapper().readTree(response.getBodyText());
    assertThat(json.get(0).has("realm")).isTrue();
    assertThat(responseBody[0].realm).isEqualTo(userToken.getRealmId());
  }

  @Test
  public void testDeleteUserTokenByUserCode() throws Exception {
    UserToken userToken = ctx.tempEntity().newUserToken(ctx.getUsername(), InternalRealm.ID);
    HttpResponse response = restRequest()
        .path(ApiUserTokenResource.USER_CODE)
        .parameter(userToken.getUserCode())
        .delete();

    ctx.assertResponseStatus(204, response);
    assertThat(userTokenDAO.getById(userToken.getId())).isNull();
  }

  @Test
  public void testGetUserTokenExistsForCurrentUser() throws Exception {
    HttpResponse response = restRequest().path(ApiUserTokenResource.CURRENT_USER_HAS_TOKEN).get();

    ctx.assertResponseStatus(200, response);
    ApiUserTokenExistsDTO responseBody = response.getBody(ApiUserTokenExistsDTO.class);
    assertThat(responseBody.userTokenExists).isFalse();

    ctx.tempEntity().newUserToken(ctx.getUsername(), InternalRealm.ID);

    response = restRequest().path(ApiUserTokenResource.CURRENT_USER_HAS_TOKEN).get();

    ctx.assertResponseStatus(200, response);
    responseBody = response.getBody(ApiUserTokenExistsDTO.class);
    assertThat(responseBody.userTokenExists).isTrue();
  }

  @Test
  public void testGetUserTokenByUsernameAndRealmId_InternalUnknown() throws Exception {
    HttpResponse httpResponse = restRequest().path(ApiUserTokenResource.USERNAME).parameter("unknown").get();

    ctx.assertResponseStatus(404, httpResponse);
    assertThat(httpResponse.getBodyText()).contains("No user token found for Internal user unknown.");
  }

  @Test
  public void testGetUserTokenByUsernameAndRealmId_SamlUnknown() throws Exception {
    HttpResponse httpResponse =
        restRequest().path(ApiUserTokenResource.USERNAME).parameter("unknown").query("realm", "SAML").get();

    ctx.assertResponseStatus(404, httpResponse);
    assertThat(httpResponse.getBodyText()).contains("No user token found for SAML user unknown.");
  }

  @Test
  public void testGetUserTokenByUsernameAndRealmId_OAuth2Unknown() throws Exception {
    enableSsoWithOAuth2();

    HttpResponse httpResponse =
        restRequest().path(ApiUserTokenResource.USERNAME).parameter("unknown").query("realm", "OAUTH2").get();

    ctx.assertResponseStatus(404, httpResponse);
    assertThat(httpResponse.getBodyText()).contains("No user token found for OAUTH2 user unknown.");
  }

  @Test
  public void testGetUserTokenByUsernameAndRealmId_CrowdIntegrationDisabled_Unknown() throws Exception {
    SystemConfigurationPropertyFeature.CROWD_INTEGRATION.setEnabled(false);

    HttpResponse httpResponse = restRequest().path(ApiUserTokenResource.USERNAME).parameter("unknown").get();

    ctx.assertResponseStatus(404, httpResponse);
    assertThat(httpResponse.getBodyText()).contains("No user token found for Internal user unknown.");
  }

  @Test
  public void testGetUserTokenByUsernameAndRealmId_CrowdIntegrationFeatureEnabled_InternalUnknown() throws Exception {
    HttpResponse httpResponse = restRequest().path(ApiUserTokenResource.USERNAME).parameter("unknown").get();

    ctx.assertResponseStatus(404, httpResponse);
    assertThat(httpResponse.getBodyText()).contains("No user token found for Internal user unknown.");
  }

  @Test
  public void testGetUserTokenByUsernameAndRealmId_CrowdIntegrationFeatureEnabled_CrowdUnknown() throws Exception {
    HttpResponse httpResponse =
        restRequest().path(ApiUserTokenResource.USERNAME).parameter("unknown").query("realm", "CROWD").get();

    ctx.assertResponseStatus(404, httpResponse);
    assertThat(httpResponse.getBodyText()).contains("No user token found for Crowd user unknown.");
  }

  @Test
  public void testGetUserTokenByUsernameAndRealmId_CrowdIntegrationFeatureEnabled_NoRealmId() throws Exception {
    testGetUserTokenByUsernameAndRealmId_Crowd(true, null);
  }

  @Test
  public void testGetUserTokenByUsernameAndRealmId_CrowdIntegrationFeatureEnabled_UnknownRealmId() throws Exception {
    testGetUserTokenByUsernameAndRealmId_Crowd(true, "unknown");
  }

  @Test
  public void testGetUserTokenByUsernameAndRealmId_CrowdIntegrationFeatureEnabled_InternalRealmId() throws Exception {
    testGetUserTokenByUsernameAndRealmId_Crowd(true, "InTeRnAl");
  }

  @Test
  public void testGetUserTokenByUsernameAndRealmId_CrowdIntegrationFeatureEnabled_CrowdRealmId() throws Exception {
    testGetUserTokenByUsernameAndRealmId_Crowd(true, "cRoWd");
  }

  @Test
  public void testGetUserTokenByUsernameAndRealmId_CrowdIntegrationFeatureDisabled_NoRealmId() throws Exception {
    testGetUserTokenByUsernameAndRealmId_Crowd(false, null);
  }

  @Test
  public void testGetUserTokenByUsernameAndRealmId_CrowdIntegrationFeatureDisabled_UnknownRealmId() throws Exception {
    testGetUserTokenByUsernameAndRealmId_Crowd(false, "unknown");
  }

  @Test
  public void testGetUserTokenByUsernameAndRealmId_CrowdIntegrationFeatureDisabled_InternalRealmId() throws Exception {
    testGetUserTokenByUsernameAndRealmId_Crowd(false, "InTeRnAl");
  }

  @Test
  public void testGetUserTokenByUsernameAndRealmId_CrowdIntegrationFeatureDisabled_CrowdRealmId() throws Exception {
    testGetUserTokenByUsernameAndRealmId_Crowd(false, "cRoWd");
  }

  @Test
  public void testGetUserTokenByUsernameAndRealmId_CrowdIntegrationFeatureDisabled_SamlRealmId() throws Exception {
    testGetUserTokenByUsernameAndRealmId_Crowd(false, "SaMl");
  }

  @Test
  public void testGetUserTokenByUsernameAndRealmId_CrowdIntegrationFeatureDisabled_OAuth2RealmId() throws Exception {
    enableSsoWithOAuth2();
    testGetUserTokenByUsernameAndRealmId_Crowd(false, "OaUth2");
  }

  private void testGetUserTokenByUsernameAndRealmId_Crowd(
      boolean isCrowdIntegrationFeatureEnabled,
      String realmId) throws Exception
  {
    testGetUserTokenByUsernameAndRealmId(isCrowdIntegrationFeatureEnabled, realmId);
  }

  private void testGetUserTokenByUsernameAndRealmId(
      boolean isCrowdIntegrationFeatureEnabled,
      String realmId) throws Exception
  {
    UserToken internalUserToken1 =
        ctx.tempEntity().newUserToken("username1", "userCode1", "passCode", User.INTERNAL_REALM_ID);
    ctx.tempEntity().newUserToken("username2", User.INTERNAL_REALM_ID);
    UserToken samlUserToken1 = ctx.tempEntity().newUserToken("username1", "userCode2", "passCode", SamlRealm.ID);
    ctx.tempEntity().newUserToken("username2", "userCode3", "passCode", SamlRealm.ID);
    UserToken oauth2UserToken1 =
        ctx.tempEntity().newUserToken("username1", "userCode4", "passCode", OAuth2Realm.ID);
    ctx.tempEntity().newUserToken("username2", "userCode5", "passCode", OAuth2Realm.ID);
    UserToken crowdUserToken1 = ctx.tempEntity().newUserToken("username1", "userCode6", "passCode", CrowdRealm.ID);
    ctx.tempEntity().newUserToken("username2", "userCode7", "passCode", CrowdRealm.ID);
    ctx.tempEntity().newUserToken("username1", "userCode8", "passCode", "other");
    ctx.tempEntity().newUserToken("username2", "userCode9", "passCode", "other");
    SystemConfigurationPropertyFeature.CROWD_INTEGRATION.setEnabled(isCrowdIntegrationFeatureEnabled);
    HttpRequest httpRequest = restRequest().path(ApiUserTokenResource.USERNAME).parameter("username1");
    if (realmId != null) {
      httpRequest.query("realm", realmId);
    }

    HttpResponse httpResponse = httpRequest.get();

    ctx.assertResponseStatus(200, httpResponse);
    ApiUserTokenDTO result = httpResponse.getBody(ApiUserTokenDTO.class);
    String expectedRealmId;
    if (SamlRealm.ID.equalsIgnoreCase(realmId)) {
      assertThat(result.userCode).isEqualTo(samlUserToken1.getUserCode());
      expectedRealmId = SamlRealm.ID;
    }
    else if (OAuth2Realm.ID.equalsIgnoreCase(realmId)) {
      assertThat(result.userCode).isEqualTo(oauth2UserToken1.getUserCode());
      expectedRealmId = OAuth2Realm.ID;
    }
    else if (isCrowdIntegrationFeatureEnabled && CrowdRealm.ID.equalsIgnoreCase(realmId)) {
      assertThat(result.userCode).isEqualTo(crowdUserToken1.getUserCode());
      expectedRealmId = CrowdRealm.ID;
    }
    else {
      assertThat(result.userCode).isEqualTo(internalUserToken1.getUserCode());
      expectedRealmId = User.INTERNAL_REALM_ID;
    }
    assertThat(result.passCode).isNull();
    assertThat(result.username).isEqualTo("username1");
    JsonNode json = new ObjectMapper().readTree(httpResponse.getBodyText());
    assertThat(json.has("realm")).isTrue();
    assertThat(result.realm).isEqualTo(expectedRealmId);
  }

  private void enableSsoWithOAuth2() {
    SystemConfigurationPropertyFeature.OAUTH2_ENABLED.setEnabled(true);
    ctx.tempEntity().newOAuth2Configuration();
    ctx.lookup(SsoUserService.class).loadSsoConfiguration();
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(PublicApiPaths.USER_TOKEN_RESOURCE_PATH_V2);
  }
}
