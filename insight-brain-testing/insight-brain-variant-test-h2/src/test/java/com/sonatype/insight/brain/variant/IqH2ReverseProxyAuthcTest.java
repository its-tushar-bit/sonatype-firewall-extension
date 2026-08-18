/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.net.HttpCookie;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.service.ApiReverseProxyAuthenticationConfigurationService;
import com.sonatype.insight.brain.configuration.ldap.EmbeddedLdapServerExtension;
import com.sonatype.insight.brain.model.configuration.ReverseProxyAuthenticationConfiguration;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.security.ReverseProxyAuthenticationFilter;
import com.sonatype.insight.brain.security.UserSessionResource;
import com.sonatype.insight.brain.security.UserSessionResource.AuthenticationStatus;
import com.sonatype.insight.brain.service.ErrorResponseGenerator;
import com.sonatype.insight.test.LogOutput;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kept in the {@code com.sonatype.insight.brain.variant} package; the {@code /ReverseProxyAuthcTest/ldap_users.ldif}
 * fixture path is a literal string in the ported test bodies (not derived from {@code getClass().getSimpleName()}),
 * so the default package/name applies. Each legacy {@code @RunWith(Parameterized.class)} constructor scenario
 * (ldapConfigured/ldapUser/localUser) is ported to a {@code @ParameterizedTest} per test method, with the legacy
 * {@code @Before} body inlined as {@link #initUsers(boolean, boolean, boolean)} since JUnit 5 has no per-scenario
 * {@code @BeforeEach} parameter injection.
 */
@IqH2Test
class IqH2ReverseProxyAuthcTest
{
  private IqTestContext ctx;

  @RegisterExtension
  private final EmbeddedLdapServerExtension testLdapServer = new EmbeddedLdapServerExtension();

  private final TestLogOutput logOutput = new TestLogOutput(ReverseProxyAuthenticationFilter.class);

  @BeforeEach
  void setUp() {
    logOutput.before();
    logOutput.clear();
  }

  @AfterEach
  void tearDown() throws Exception {
    logOutput.tearDown();
  }

  static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
      {false, false, false}, // totally unknown username, no LDAP configured
      {true, false, false}, // totally unknown username, LDAP configured
      {false, false, true}, // username only present in local db, no LDAP configured
      {true, false, true}, // username only present in local db, LDAP configured
      {true, true, false}, // username only present in LDAP
      {true, true, true} // username present in local db and LDAP
    });
  }

  static Stream<Arguments> ldapScenarios() {
    return data().stream().map(row -> Arguments.of(row[0], row[1], row[2]));
  }

  private void initUsers(boolean setupLdap, boolean ldapUser, boolean localUser) throws Exception {
    if (setupLdap) {
      testLdapServer.start();
      if (ldapUser) {
        testLdapServer.loadData("/ReverseProxyAuthcTest/ldap_users.ldif");
      }
      LdapServer ldapServer = ctx.tempEntity().newLdapServer("LDAP");
      ctx.tempEntity().newLdapConnection(ldapServer.getId(), testLdapServer.getPort());
      ctx.tempEntity().newLdapUserMapping(ldapServer.getId());
    }
    if (localUser) {
      // Be sure to keep the detail in-sync with the ldap defined user details for the testuser
      ctx.tempEntity().newUser("testuser", "John", "Doe", "test.user@company.com");
    }
  }

  private String displayName(boolean ldapUser, boolean localUser) {
    return localUser || ldapUser ? "John Doe" : "testuser";
  }

  @ParameterizedTest(name = "ldapConfigured={0}, ldapUser={1}, localUser={2}")
  @MethodSource("ldapScenarios")
  void testDisabledByDefault(boolean setupLdap, boolean ldapUser, boolean localUser) throws Exception {
    initUsers(setupLdap, ldapUser, localUser);

    ReverseProxyAuthenticationConfiguration reverseProxyAuthcConfig = new ReverseProxyAuthenticationConfiguration();
    assertThat(reverseProxyAuthcConfig.isEnabled()).isFalse();
    assertThat(reverseProxyAuthcConfig.getUsernameHeader()).isEqualTo("REMOTE_USER");
    HttpResponse response = ctx.restRequest().path("rest/anything").header("REMOTE_USER", "testuser").anon().get();
    ctx.assertResponseStatus(401, response);
    assertThat(response.getBodyText()).isEqualTo(ErrorResponseGenerator.MSG_MISSING_CREDENTIALS);
  }

  @ParameterizedTest(name = "ldapConfigured={0}, ldapUser={1}, localUser={2}")
  @MethodSource("ldapScenarios")
  void testEnabled_DefaultHeader(boolean setupLdap, boolean ldapUser, boolean localUser) throws Exception {
    initUsers(setupLdap, ldapUser, localUser);

    ctx.tempEntity()
        .newReverseProxyAuthenticationConfiguration(true,
            ReverseProxyAuthenticationConfiguration.DEFAULT_USERNAME_HEADER, true, null);
    ctx.lookup(ApiReverseProxyAuthenticationConfigurationService.class)
        .applyReverseProxyAuthenticationConfigurationToClients();

    HttpRequest request = ctx.restRequest().header("REMOTE_USER", "testuser").anon();
    HttpResponse response = request.subpath(UserSessionResource.RESOURCE_PATH).get();
    ctx.assertResponseStatus(200, response);
    assertThat(response.getSessionCookie()).isNotNull();
    AuthenticationStatus authStatus = response.getBody(AuthenticationStatus.class);
    assertThat(authStatus.isAuthenticated()).isTrue();
    assertThat(authStatus.isInternalUser()).isFalse();
    assertThat(authStatus.getDisplayName()).isEqualTo(displayName(ldapUser, localUser));

    response = request.subpath(PublicApiPaths.ORG_RESOURCE_PATH).get();
    ctx.assertResponseStatus(200, response);
    assertThat(response.getSessionCookie()).isNull();
  }

  @ParameterizedTest(name = "ldapConfigured={0}, ldapUser={1}, localUser={2}")
  @MethodSource("ldapScenarios")
  void testLogout_reverseProxyIsEnabledWithLogoutUrl(
      boolean setupLdap,
      boolean ldapUser,
      boolean localUser) throws Exception
  {
    initUsers(setupLdap, ldapUser, localUser);

    String logoutUrl = "http://localhost/logout";
    ctx.tempEntity()
        .newReverseProxyAuthenticationConfiguration(true,
            ReverseProxyAuthenticationConfiguration.DEFAULT_USERNAME_HEADER, false, logoutUrl);
    ctx.lookup(ApiReverseProxyAuthenticationConfigurationService.class)
        .applyReverseProxyAuthenticationConfigurationToClients();

    HttpRequest request =
        ctx.restRequest().header(ReverseProxyAuthenticationConfiguration.DEFAULT_USERNAME_HEADER, "testuser").anon();
    HttpResponse response = request.subpath(UserSessionResource.RESOURCE_PATH).get();
    ctx.assertResponseStatus(HttpStatus.SC_OK, response);
    HttpCookie sessionCookie = response.getSessionCookie();
    assertThat(sessionCookie).isNotNull();
    AuthenticationStatus authStatus = response.getBody(AuthenticationStatus.class);
    assertThat(authStatus.isAuthenticated()).isTrue();
    assertThat(authStatus.isInternalUser()).isFalse();
    assertThat(authStatus.getDisplayName()).isEqualTo(displayName(ldapUser, localUser));

    response = request.subpath(UserSessionResource.RESOURCE_PATH, UserSessionResource.LOGOUT_PATH)
        .cookie(sessionCookie)
        .delete();
    ctx.assertResponseStatus(HttpStatus.SC_NO_CONTENT, response);
    assertThat(response.getHeader("Location")).isEqualTo(logoutUrl);
  }

  @ParameterizedTest(name = "ldapConfigured={0}, ldapUser={1}, localUser={2}")
  @MethodSource("ldapScenarios")
  void testLogout_reverseProxyIsEnabledWithoutLogoutUrl(
      boolean setupLdap,
      boolean ldapUser,
      boolean localUser) throws Exception
  {
    initUsers(setupLdap, ldapUser, localUser);
    enableReverseProxyAuthentication();

    HttpRequest request = ctx.restRequest().header("REMOTE_USER", "testuser").anon();
    HttpResponse response = request.subpath(UserSessionResource.RESOURCE_PATH).get();
    ctx.assertResponseStatus(HttpStatus.SC_OK, response);
    HttpCookie sessionCookie = response.getSessionCookie();
    assertThat(sessionCookie).isNotNull();
    AuthenticationStatus authStatus = response.getBody(AuthenticationStatus.class);
    assertThat(authStatus.isAuthenticated()).isTrue();
    assertThat(authStatus.isInternalUser()).isFalse();
    assertThat(authStatus.getDisplayName()).isEqualTo(displayName(ldapUser, localUser));

    response = request.subpath(UserSessionResource.RESOURCE_PATH, UserSessionResource.LOGOUT_PATH)
        .cookie(sessionCookie)
        .delete();
    ctx.assertResponseStatus(HttpStatus.SC_NO_CONTENT, response);
    assertThat(response.getHeader("Location")).isNull();
  }

  @ParameterizedTest(name = "ldapConfigured={0}, ldapUser={1}, localUser={2}")
  @MethodSource("ldapScenarios")
  void testEnabled_CustomHeader(boolean setupLdap, boolean ldapUser, boolean localUser) throws Exception {
    initUsers(setupLdap, ldapUser, localUser);

    ctx.tempEntity().newReverseProxyAuthenticationConfiguration(true, "X-Remote-User", false, null);
    ctx.lookup(ApiReverseProxyAuthenticationConfigurationService.class)
        .applyReverseProxyAuthenticationConfigurationToClients();

    HttpRequest request = ctx.restRequest().header("X-Remote-User", "testuser").anon();
    HttpResponse response = request.subpath(UserSessionResource.RESOURCE_PATH).get();
    ctx.assertResponseStatus(200, response);
    assertThat(response.getSessionCookie()).isNotNull();
    AuthenticationStatus authStatus = response.getBody(AuthenticationStatus.class);
    assertThat(authStatus.isAuthenticated()).isTrue();
    assertThat(authStatus.isInternalUser()).isFalse();
    assertThat(authStatus.getDisplayName()).isEqualTo(displayName(ldapUser, localUser));
  }

  @ParameterizedTest(name = "ldapConfigured={0}, ldapUser={1}, localUser={2}")
  @MethodSource("ldapScenarios")
  void testEnabled_SessionCreatedForAnyRequest(
      boolean setupLdap,
      boolean ldapUser,
      boolean localUser) throws Exception
  {
    initUsers(setupLdap, ldapUser, localUser);
    enableReverseProxyAuthentication();

    HttpRequest request = ctx.restRequest().header("REMOTE_USER", "testuser").anon();
    HttpResponse response = request.subpath("rest/anything").get();
    ctx.assertResponseStatus(404, response);
    assertThat(response.getSessionCookie()).isNotNull();
  }

  @ParameterizedTest(name = "ldapConfigured={0}, ldapUser={1}, localUser={2}")
  @MethodSource("ldapScenarios")
  void testEnabled_HeaderWithValidUserDoesNotMatchSession(
      boolean setupLdap,
      boolean ldapUser,
      boolean localUser) throws Exception
  {
    initUsers(setupLdap, ldapUser, localUser);
    enableReverseProxyAuthentication();

    ctx.tempEntity().newUser("Beta", "Beta", "User", "beta.user@company.com");

    HttpRequest request = ctx.restRequest().header("REMOTE_USER", "testuser").anon();
    HttpResponse response = request.subpath(UserSessionResource.RESOURCE_PATH).get();

    HttpRequest mismatchRequest =
        ctx.restRequest().cookie(response.getSessionCookie()).header("REMOTE_USER", "Beta").anon();
    HttpResponse mismatchResponse = mismatchRequest.subpath(UserSessionResource.RESOURCE_PATH).get();
    ctx.assertResponseStatus(200, mismatchResponse);
    AuthenticationStatus authenticationStatus = mismatchResponse.getBody(AuthenticationStatus.class);
    assertThat(authenticationStatus.getUsername()).isEqualTo("Beta");
    assertThat(authenticationStatus.isInternalUser()).isFalse();
    assertThat(logOutput).atInfoLevel()
        .contains("Detected mismatch between user specified by reverse proxy authentication (Beta)"
            + " and user specified by session cookie (testuser)");
  }

  private void enableReverseProxyAuthentication() {
    ctx.tempEntity()
        .newReverseProxyAuthenticationConfiguration(true,
            ReverseProxyAuthenticationConfiguration.DEFAULT_USERNAME_HEADER, false, null);
    ctx.lookup(ApiReverseProxyAuthenticationConfigurationService.class)
        .applyReverseProxyAuthenticationConfigurationToClients();
  }

  private static final class TestLogOutput
      extends LogOutput
  {
    TestLogOutput(Class<?>... types) {
      super(types);
    }

    void tearDown() {
      after();
    }
  }
}
