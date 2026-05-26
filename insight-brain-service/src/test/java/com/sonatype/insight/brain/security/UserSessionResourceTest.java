/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.HttpCookie;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import jakarta.ws.rs.core.Response.Status;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.configuration.saml.SamlConfigurationService;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.configuration.saml.SamlConfiguration;
import com.sonatype.insight.brain.model.security.Group;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.security.UserSessionResource.AuthenticationStatus;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.service.ErrorResponseGenerator;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.LOGOUT_AUTH0_ON_LOGOUT;
import static org.assertj.core.api.Assertions.assertThat;

public class UserSessionResourceTest
    extends AbstractResourceTest
{
  public SamlDeploymentManager samlDeploymentManager;

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(UserSessionResource.RESOURCE_PATH);
  }

  private HttpResponse logout(HttpCookie cookie) throws Exception {
    return restRequest().path(UserSessionResource.LOGOUT_PATH).cookie(cookie).anon().delete();
  }

  private HttpResponse login() throws Exception {
    return login(null, null);
  }

  private HttpResponse login(String username, String password) throws Exception {
    return restRequest().auth(username, password).post();
  }

  private HttpResponse secureLogin() throws Exception {
    return restRequest().auth().header("X-Forwarded-Proto", "https").post();
  }

  private HttpResponse status(HttpCookie cookie) throws Exception {
    return restRequest().cookie(cookie).anon().get();
  }

  @Before
  public void before() {
    samlDeploymentManager = getCLMServer().getInstance(SamlDeploymentManager.class);
  }

  @Test
  public void testSessionManagement() throws Exception {

    // uninstall license and should find all these tests run uninhibited as they are unlicensed paths
    getTestProductLicenseManager().uninstallLicense();

    // now run the test with bad username
    HttpResponse response = login("admin2", "admin");
    assertResponseStatus(401, response);
    // see: com.sonatype.insight.brain.security.UserFriendlyBasicHttpAuthenticationFilter.sendChallenge()
    assertThat(response.getHeader("WWW-Authenticate")).isNull();
    assertThat(response.getBodyText()).isEqualTo(ErrorResponseGenerator.MSG_LOGIN_FAILURE_DEFAULT);

    // now run the test with bad password
    response = login(User.ADMIN_USERNAME, "wrong password");
    assertResponseStatus(401, response);
    assertThat(response.getHeader("WWW-Authenticate")).isNull();
    assertThat(response.getBodyText()).isEqualTo(ErrorResponseGenerator.MSG_LOGIN_FAILURE_DEFAULT);

    // now run the test with no header, validate failure
    response = login();
    assertResponseStatus(401, response);
    assertThat(response.getHeader("WWW-Authenticate")).isNull();
    assertThat(response.getBodyText()).isEqualTo(ErrorResponseGenerator.MSG_MISSING_CREDENTIALS);

    // now run with valid data
    response = login(User.ADMIN_USERNAME, "admin123");
    assertResponseStatus(204, response);

    // validate cookie is present
    HttpCookie loggedInSessionCookie = response.getSessionCookie();
    assertThat(loggedInSessionCookie).isNotNull();
    assertThat(loggedInSessionCookie.getValue()).isNotEqualTo("deleteMe");

    // logout is successful
    response = logout(loggedInSessionCookie);
    assertResponseStatus(204, response);

    // logout removes session id
    HttpCookie logoutSessionCookie = response.getSessionCookie();
    assertThat(logoutSessionCookie).isNotNull();
    assertThat(logoutSessionCookie.getValue()).isEqualTo("deleteMe");
  }

  @Test
  public void testStatus() throws Exception {
    Integer originalGlobalSessionTimeout = (Integer) getProperty(SystemConfigurationProperty.SESSION_TIMEOUT_MINUTES);
    try {
      // logged out by default, so 401 expected
      HttpResponse response = status(null);
      assertResponseStatus(401, response);

      response = login(User.ADMIN_USERNAME, "admin123");
      assertResponseStatus(204, response);

      HttpCookie sessionCookie = response.getSessionCookie();
      assertThat(sessionCookie).isNotNull();

      response = status(sessionCookie);
      assertResponseStatus(200, response);
      AuthenticationStatus status = response.getBody(AuthenticationStatus.class);
      assertThat(status.isAuthenticated()).isTrue();
      assertThat(status.getUsername()).isEqualTo(User.ADMIN_USERNAME);
      assertThat(status.getGroups()).containsExactly(Group.AUTHENTICATED_USERS_GROUP_ID);
      assertThat(status.getSessionTimeoutMilliseconds()).isEqualTo(originalGlobalSessionTimeout * 60 * 1000);

      setProperties(Map.of(SystemConfigurationProperty.SESSION_TIMEOUT_MINUTES, originalGlobalSessionTimeout + 1));

      response = status(sessionCookie);
      assertResponseStatus(200, response);
      status = response.getBody(AuthenticationStatus.class);
      assertThat(status.isAuthenticated()).isTrue();
      assertThat(status.getUsername()).isEqualTo(User.ADMIN_USERNAME);
      assertThat(status.getGroups()).containsExactly(Group.AUTHENTICATED_USERS_GROUP_ID);
      assertThat(status.getSessionTimeoutMilliseconds()).isEqualTo(originalGlobalSessionTimeout * 60 * 1000);

      logout(sessionCookie);
      response = login(User.ADMIN_USERNAME, "admin123");
      assertResponseStatus(204, response);

      sessionCookie = response.getSessionCookie();
      assertThat(sessionCookie).isNotNull();

      response = status(sessionCookie);
      assertResponseStatus(200, response);
      status = response.getBody(AuthenticationStatus.class);
      assertThat(status.isAuthenticated()).isTrue();
      assertThat(status.getUsername()).isEqualTo(User.ADMIN_USERNAME);
      assertThat(status.getGroups()).containsExactly(Group.AUTHENTICATED_USERS_GROUP_ID);
      assertThat(status.getSessionTimeoutMilliseconds()).isEqualTo((originalGlobalSessionTimeout + 1) * 60 * 1000L);

      response = logout(sessionCookie);
      assertResponseStatus(204, response);

      // this cookie should no longer be valid
      response = status(sessionCookie);
      assertResponseStatus(401, response);
    }
    finally {
      resetProperties(SystemConfigurationProperty.SESSION_TIMEOUT_MINUTES);
    }
  }

  @Test
  public void testLogoutNoAuth() throws Exception {
    // no cookie, no auth
    HttpResponse response = logout(null);
    assertResponseStatus(204, response);
  }

  @Test
  public void testLogin_CookiesNotSecure() throws Exception {
    HttpResponse response = login(User.ADMIN_USERNAME, "admin123");
    assertResponseStatus(204, response);

    HttpCookie sessionCookie = response.getSessionCookie();
    assertThat(sessionCookie).isNotNull();
    assertThat(sessionCookie.getSecure()).isFalse();
  }

  @Test
  public void testSecureLogin_CookiesSecure() throws Exception {
    HttpResponse response = secureLogin();
    assertResponseStatus(204, response);

    HttpCookie sessionCookie = response.getSessionCookie();
    assertThat(sessionCookie).isNotNull();
    assertThat(sessionCookie.getSecure()).isTrue();
  }

  @Test
  public void testBuildAuth0LogoutUrl() throws Exception {
    SamlConfigurationService samlConfigurationService = lookup(SamlConfigurationService.class);
    SamlConfiguration samlConfiguration = tempEntity.newSamlConfiguration(auth0IdpXml(), null);
    samlConfigurationService.insert(samlConfiguration);
    samlDeploymentManager.updateFromConfiguration();

    tempEntity.newSystemConfigurationProperty(LOGOUT_AUTH0_ON_LOGOUT, "true");

    HttpResponse response = logout(null);

    assertThat(response.getHeader("Location")).startsWith(
        "https://idp-entity-id/v2/logout?client_id=rfCvE9qbgAu0ASBCCwe8QZugsAJzf1TK&returnTo=http://localhost");
  }

  @Test
  public void testUrlDefaultsWhenAuth0LogoutSetButNoSamlConfig() throws Exception {
    // Ensure the config is re-read
    samlDeploymentManager.register();

    tempEntity.newSystemConfigurationProperty(LOGOUT_AUTH0_ON_LOGOUT, "true");

    HttpResponse response = logout(null);

    assertThat(response.getHeader("Location")).isNull();
    assertThat(response.getStatusCode()).isEqualTo(Status.NO_CONTENT.getStatusCode());
  }

  private String auth0IdpXml() {
    try {
      return IOUtils.toString(
          getClass().getResourceAsStream("/UserSessionResourceTest/identity-provider-metadata.xml"),
          StandardCharsets.UTF_8);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
