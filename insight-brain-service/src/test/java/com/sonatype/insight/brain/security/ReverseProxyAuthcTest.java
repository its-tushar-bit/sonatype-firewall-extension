/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.net.HttpCookie;
import java.util.Arrays;
import java.util.Collection;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.service.ApiReverseProxyAuthenticationConfigurationService;
import com.sonatype.insight.brain.configuration.ldap.TestLdapServer;
import com.sonatype.insight.brain.model.configuration.ReverseProxyAuthenticationConfiguration;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.security.UserSessionResource.AuthenticationStatus;
import com.sonatype.insight.brain.service.ErrorResponseGenerator;
import com.sonatype.insight.brain.testing.AbstractBrainServiceIntegrationTest;
import com.sonatype.insight.test.LogOutput;

import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.experimental.categories.Category;

@RunWith(Parameterized.class)
public class ReverseProxyAuthcTest
    extends AbstractBrainServiceIntegrationTest
{
  @Rule
  public TestLdapServer testLdapServer = new TestLdapServer();

  @Rule
  public LogOutput logOutput = new LogOutput(ReverseProxyAuthenticationFilter.class);

  private final boolean setupLdap;

  private final boolean ldapUser;

  private final boolean localUser;

  public ReverseProxyAuthcTest(boolean setupLdap, boolean ldapUser, boolean localUser) {
    this.setupLdap = setupLdap;
    this.ldapUser = ldapUser;
    this.localUser = localUser;
  }

  @Parameterized.Parameters(name = "ldapConfigured={0}, ldapUser={1}, localUser={2}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
        {false, false, false}, // totally unknown username, no LDAP configured
        {true, false, false},  // totally unknown username, LDAP configured
        {false, false, true},  // username only present in local db, no LDAP configured
        {true, false, true},   // username only present in local db, LDAP configured
        {true, true, false},   // username only present in LDAP
        {true, true, true}     // username present in local db and LDAP
    });
  }

  @Before
  public void init() throws Exception {
    if (setupLdap) {
      testLdapServer.start();
      if (ldapUser) {
        testLdapServer.loadData("/ReverseProxyAuthcTest/ldap_users.ldif");
      }
      LdapServer ldapServer = tempEntity.newLdapServer("LDAP");
      tempEntity.newLdapConnection(ldapServer.getId(), this.testLdapServer.getPort());
      tempEntity.newLdapUserMapping(ldapServer.getId());
    }
    if (localUser) {
      // Be sure to keep the detail in-sync with the ldap defined user details for the testuser
      tempEntity.newUser("testuser", "John", "Doe", "test.user@company.com");
    }
  }

  private String displayName() {
    return localUser || ldapUser ? "John Doe" : "testuser";
  }

  @Test
  public void testDisabledByDefault() throws Exception {
    ReverseProxyAuthenticationConfiguration reverseProxyAuthcConfig = new ReverseProxyAuthenticationConfiguration();
    assertThat(reverseProxyAuthcConfig.isEnabled()).isFalse();
    assertThat(reverseProxyAuthcConfig.getUsernameHeader()).isEqualTo("REMOTE_USER");
    HttpResponse response = restRequest().path("rest/anything").header("REMOTE_USER", "testuser").anon().get();
    assertResponseStatus(401, response);
    assertThat(response.getBodyText()).isEqualTo(ErrorResponseGenerator.MSG_MISSING_CREDENTIALS);
  }

  @Test
  public void testEnabled_DefaultHeader() throws Exception {
    tempEntity.newReverseProxyAuthenticationConfiguration(true,
        ReverseProxyAuthenticationConfiguration.DEFAULT_USERNAME_HEADER, true, null);
    getCLMServer().getInstance(ApiReverseProxyAuthenticationConfigurationService.class)
        .applyReverseProxyAuthenticationConfigurationToClients();

    HttpRequest request = restRequest().header("REMOTE_USER", "testuser").anon();
    HttpResponse response = request.subpath(UserSessionResource.RESOURCE_PATH).get();
    assertResponseStatus(200, response);
    assertThat(response.getSessionCookie()).isNotNull();
    AuthenticationStatus authStatus = response.getBody(AuthenticationStatus.class);
    assertThat(authStatus.isAuthenticated()).isTrue();
    assertThat(authStatus.isInternalUser()).isFalse();
    assertThat(authStatus.getDisplayName()).isEqualTo(displayName());

    response = request.subpath(PublicApiPaths.ORG_RESOURCE_PATH).get();
    assertResponseStatus(200, response);
    assertThat(response.getSessionCookie()).isNull();
  }

  @Test
  public void testLogout_reverseProxyIsEnabledWithLogoutUrl() throws Exception {
    String logoutUrl = "http://localhost/logout";
    tempEntity.newReverseProxyAuthenticationConfiguration(true,
          ReverseProxyAuthenticationConfiguration.DEFAULT_USERNAME_HEADER, false, logoutUrl);
    getCLMServer().getInstance(ApiReverseProxyAuthenticationConfigurationService.class)
        .applyReverseProxyAuthenticationConfigurationToClients();

    HttpRequest request =
        restRequest().header(ReverseProxyAuthenticationConfiguration.DEFAULT_USERNAME_HEADER, "testuser").anon();
    HttpResponse response = request.subpath(UserSessionResource.RESOURCE_PATH).get();
    assertResponseStatus(HttpStatus.SC_OK, response);
    HttpCookie sessionCookie = response.getSessionCookie();
    assertThat(sessionCookie).isNotNull();
    AuthenticationStatus authStatus = response.getBody(AuthenticationStatus.class);
    assertThat(authStatus.isAuthenticated()).isTrue();
    assertThat(authStatus.isInternalUser()).isFalse();
    assertThat(authStatus.getDisplayName()).isEqualTo(displayName());

    response = request.subpath(UserSessionResource.RESOURCE_PATH, UserSessionResource.LOGOUT_PATH).cookie(sessionCookie)
        .delete();
    assertResponseStatus(HttpStatus.SC_NO_CONTENT, response);
    assertThat(response.getHeader("Location")).isEqualTo(logoutUrl);
  }

  @Test
  public void testLogout_reverseProxyIsEnabledWithoutLogoutUrl() throws Exception {
    enableReverseProxyAuthentication();

    HttpRequest request = restRequest().header("REMOTE_USER", "testuser").anon();
    HttpResponse response = request.subpath(UserSessionResource.RESOURCE_PATH).get();
    assertResponseStatus(HttpStatus.SC_OK, response);
    HttpCookie sessionCookie = response.getSessionCookie();
    assertThat(sessionCookie).isNotNull();
    AuthenticationStatus authStatus = response.getBody(AuthenticationStatus.class);
    assertThat(authStatus.isAuthenticated()).isTrue();
    assertThat(authStatus.isInternalUser()).isFalse();
    assertThat(authStatus.getDisplayName()).isEqualTo(displayName());

    response = request.subpath(UserSessionResource.RESOURCE_PATH, UserSessionResource.LOGOUT_PATH).cookie(sessionCookie)
        .delete();
    assertResponseStatus(HttpStatus.SC_NO_CONTENT, response);
    assertThat(response.getHeader("Location")).isNull();
  }

  @Test
  public void testEnabled_CustomHeader() throws Exception {
    tempEntity.newReverseProxyAuthenticationConfiguration(true, "X-Remote-User", false, null);
    getCLMServer().getInstance(ApiReverseProxyAuthenticationConfigurationService.class)
        .applyReverseProxyAuthenticationConfigurationToClients();

    HttpRequest request = restRequest().header("X-Remote-User", "testuser").anon();
    HttpResponse response = request.subpath(UserSessionResource.RESOURCE_PATH).get();
    assertResponseStatus(200, response);
    assertThat(response.getSessionCookie()).isNotNull();
    AuthenticationStatus authStatus = response.getBody(AuthenticationStatus.class);
    assertThat(authStatus.isAuthenticated()).isTrue();
    assertThat(authStatus.isInternalUser()).isFalse();
    assertThat(authStatus.getDisplayName()).isEqualTo(displayName());
  }

  @Test
  public void testEnabled_SessionCreatedForAnyRequest() throws Exception {
    enableReverseProxyAuthentication();

    HttpRequest request = restRequest().header("REMOTE_USER", "testuser").anon();
    HttpResponse response = request.subpath("rest/anything").get();
    assertResponseStatus(404, response);
    assertThat(response.getSessionCookie()).isNotNull();
  }

  @Test
  public void testEnabled_HeaderWithValidUserDoesNotMatchSession() throws Exception {
    enableReverseProxyAuthentication();

    tempEntity.newUser("Beta", "Beta", "User", "beta.user@company.com");

    HttpRequest request = restRequest().header("REMOTE_USER", "testuser").anon();
    HttpResponse response = request.subpath(UserSessionResource.RESOURCE_PATH).get();

    HttpRequest mismatchRequest =
        restRequest().cookie(response.getSessionCookie()).header("REMOTE_USER", "Beta").anon();
    HttpResponse mismatchResponse = mismatchRequest.subpath(UserSessionResource.RESOURCE_PATH).get();
    assertResponseStatus(200, mismatchResponse);
    AuthenticationStatus authenticationStatus = mismatchResponse.getBody(AuthenticationStatus.class);
    assertThat(authenticationStatus.getUsername()).isEqualTo("Beta");
    assertThat(authenticationStatus.isInternalUser()).isFalse();
    assertThat(logOutput).atInfoLevel()
        .contains("Detected mismatch between user specified by reverse proxy authentication (Beta)"
            + " and user specified by session cookie (testuser)");
  }

  private void enableReverseProxyAuthentication() {
    tempEntity.newReverseProxyAuthenticationConfiguration(true,
        ReverseProxyAuthenticationConfiguration.DEFAULT_USERNAME_HEADER, false, null);
    getCLMServer().getInstance(ApiReverseProxyAuthenticationConfigurationService.class)
        .applyReverseProxyAuthenticationConfigurationToClients();
  }
}
