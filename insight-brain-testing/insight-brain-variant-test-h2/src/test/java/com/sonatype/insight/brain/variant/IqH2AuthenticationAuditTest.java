/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.v2.service.ApiReverseProxyAuthenticationConfigurationService;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditErrorType;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.dataaccess.configuration.oauth2.OAuth2ConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.configuration.ReverseProxyAuthenticationConfiguration;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.model.configuration.oauth2.OAuth2Configuration;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.organization.ApplicationResource;
import com.sonatype.insight.brain.security.UserSessionResource;
import com.sonatype.insight.brain.security.oauth2.JWTGenerator;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.brain.spring.config.SecurityConfiguration;
import com.sonatype.insight.test.LogOutput;

import java.net.HttpCookie;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Kept in the {@code com.sonatype.insight.brain.variant} package; implements {@link AuditTestSupport} directly
 * (rather than inheriting {@code AbstractAuditTest}) so it can register its own {@link LogOutput} scoped to the
 * {@code authentication} audit domain, mirroring the legacy {@code AuthenticationAuditTest}'s
 * {@code StartedLogOutput}.
 */
@IqH2Test
class IqH2AuthenticationAuditTest
    implements AuditTestSupport
{
  private IqTestContext ctx;

  @RegisterExtension
  private final LogOutput authenticationLogOutput =
      new LogOutput(AuditRecorder.toLoggerName(AuditEvent.AUTHENTICATION_FAILURE.getDomain()));

  private static final String RESTRICTED_PATH = "/" + ApplicationResource.RESOURCE_PATH;

  private static final String RESTRICTED_UNSAFE_PATH = RESTRICTED_PATH + "/applicationPublicId";

  private static final String AUTH_RESOURCE_PATH = "/" + UserSessionResource.RESOURCE_PATH;

  private final JWTGenerator jwtGenerator = new JWTGenerator();

  @Override
  public LogOutput getLogOutput() {
    return authenticationLogOutput;
  }

  @Override
  public PolicyDAO getPolicyDAO() {
    return ctx.lookup(PolicyDAO.class);
  }

  private HttpRequest restRequest() {
    return ctx.restRequest();
  }

  @Test
  void testLoginLogout() throws Exception {
    HttpCookie sessionCookie = restRequest().path(UserSessionResource.RESOURCE_PATH).post().getSessionCookie();

    AuditDTO log = awaitLogEntries(AuditEvent.LOGIN, 1).get(0);
    assertThat(log.domain).isEqualTo("authentication");
    assertThat(log.type).isEqualTo("login");
    assertThat(log.timestamp).isNotEmpty();
    assertThat(log.error).isNull();
    assertThat(log.username).isEqualTo(User.ADMIN_USERNAME);
    assertThat(log.requestMethod).isEqualTo("POST");
    assertThat(log.requestUri).isEqualTo(AUTH_RESOURCE_PATH);

    restRequest().path(UserSessionResource.RESOURCE_PATH, UserSessionResource.LOGOUT_PATH)
        .anon()
        .cookie(sessionCookie)
        .delete();

    log = awaitLogEntries(AuditEvent.LOGOUT, 1).get(0);
    assertThat(log.domain).isEqualTo("authentication");
    assertThat(log.type).isEqualTo("logout");
    assertThat(log.timestamp).isNotEmpty();
    assertThat(log.error).isNull();
    assertThat(log.username).isEqualTo(User.ADMIN_USERNAME);
    assertThat(log.requestMethod).isEqualTo("DELETE");
    assertThat(log.requestUri).isEqualTo(AUTH_RESOURCE_PATH + '/' + UserSessionResource.LOGOUT_PATH);
  }

  @Test
  void testImplicitLoginByReverseProxy() throws Exception {
    ReverseProxyAuthenticationConfiguration rutConfig = ctx.tempEntity()
        .newReverseProxyAuthenticationConfiguration(
            true, ReverseProxyAuthenticationConfiguration.DEFAULT_USERNAME_HEADER, false, null);
    ctx.lookup(ApiReverseProxyAuthenticationConfigurationService.class)
        .applyReverseProxyAuthenticationConfigurationToClients();
    String username = "rut-user";

    restRequest().path(RESTRICTED_PATH).anon().header(rutConfig.getUsernameHeader(), username).get();

    AuditDTO log = awaitLogEntries(AuditEvent.LOGIN, 1).get(0);
    assertThat(log.domain).isEqualTo("authentication");
    assertThat(log.type).isEqualTo("login");
    assertThat(log.timestamp).isNotEmpty();
    assertThat(log.error).isNull();
    assertThat(log.username).isEqualTo(username);
    assertThat(log.requestMethod).isEqualTo("GET");
    assertThat(log.requestUri).isEqualTo(RESTRICTED_PATH);
  }

  @Test
  void testNoAuthenticationHeadersOrCookies() throws Exception {
    restRequest().anon().path(RESTRICTED_PATH).get();

    AuditDTO log = awaitLogEntries(AuditEvent.AUTHENTICATION_FAILURE, 1).get(0);
    assertAuditLog(log, "GET", RESTRICTED_PATH, "unauthenticated");
  }

  @Test
  void testInvalidUserNamePassword() throws Exception {
    restRequest().auth("invalidUser", "invalidPassword").path(AUTH_RESOURCE_PATH).post();

    AuditDTO log = awaitLogEntries(AuditEvent.AUTHENTICATION_FAILURE, 1).get(0);
    assertAuditLog(log, "POST", AUTH_RESOURCE_PATH, "bad-authentication");
  }

  @Test
  void testInvalidCsrfToken() throws Exception {
    restRequest().path(RESTRICTED_UNSAFE_PATH).noCsrfToken().delete();

    AuditDTO log = awaitLogEntries(AuditEvent.AUTHENTICATION_FAILURE, 1).get(0);
    assertAuditLog(log, "DELETE", RESTRICTED_UNSAFE_PATH, "bad-csrf-token");
  }

  @Test
  void testBadSessionCookie() throws Exception {
    restRequest().path(RESTRICTED_PATH).anon().cookie(SecurityConfiguration.SESSION_COOKIE_NAME, "bad").get();

    AuditDTO log = awaitLogEntries(AuditEvent.AUTHENTICATION_FAILURE, 1).get(0);
    assertAuditLog(log, "GET", RESTRICTED_PATH, "bad-session");
  }

  @Test
  void testAuthenticationInternalError() throws Exception {
    LdapServer ldapServer = ctx.tempEntity().newLdapServer("ldap");
    ctx.tempEntity().newLdapConnection(ldapServer.getId());
    ctx.tempEntity().newLdapUserMapping(ldapServer.getId());

    restRequest().auth("user", "pass").path(RESTRICTED_PATH).get();

    AuditDTO log = awaitLogEntries(AuditEvent.AUTHENTICATION_FAILURE, 1).get(0);
    assertAuditLog(log, "GET", RESTRICTED_PATH, AuditErrorType.SERVER_ERROR.getValue());
  }

  @Test
  void testOAuth2LoginWithJWT() throws Exception {
    final String sub = "oauth-user-123";
    final String issuer = "https://test-idp.example.com";
    final String username = "oauth-test-user";
    final String firstName = "OAuth";
    final String lastName = "TestUser";
    final String email = "oauth.user@example.com";
    final List<String> groups = Arrays.asList("developers", "admins");

    SystemConfigurationPropertyFeature.OAUTH2_ENABLED.setEnabled(true);

    Map<String, Object> claims = jwtGenerator.getCustomClaims(username, firstName, lastName, email, groups);
    String token = jwtGenerator.generateJWT(sub, issuer, claims);

    configureOAuth2(issuer);

    restRequest().anon().header("Authorization", "Bearer " + token).path(AUTH_RESOURCE_PATH).get();

    AuditDTO log = awaitLogEntries(AuditEvent.LOGIN, 1).get(0);
    assertThat(log.domain).isEqualTo("authentication");
    assertThat(log.type).isEqualTo("login");
    assertThat(log.timestamp).isNotEmpty();
    assertThat(log.error).isNull();
    assertThat(log.username).isEqualTo(email);
    assertThat(log.requestMethod).isEqualTo("GET");
    assertThat(log.requestUri).isEqualTo(AUTH_RESOURCE_PATH);
  }

  @Test
  void testOAuth2LoginWithJWT_UsernameFromSubject() throws Exception {
    final String sub = "subject-username";
    final String issuer = "https://test-idp.example.com";

    SystemConfigurationPropertyFeature.OAUTH2_ENABLED.setEnabled(true);

    String token = jwtGenerator.generateJWT(sub, issuer);

    configureOAuth2(issuer);

    restRequest().anon().header("Authorization", "Bearer " + token).path(AUTH_RESOURCE_PATH).get();

    AuditDTO log = awaitLogEntries(AuditEvent.LOGIN, 1).get(0);
    assertThat(log.domain).isEqualTo("authentication");
    assertThat(log.type).isEqualTo("login");
    assertThat(log.timestamp).isNotEmpty();
    assertThat(log.error).isNull();
    assertThat(log.username).isEqualTo(sub);
    assertThat(log.requestMethod).isEqualTo("GET");
    assertThat(log.requestUri).isEqualTo(AUTH_RESOURCE_PATH);
  }

  private void configureOAuth2(String issuer) {
    OAuth2Configuration oAuth2Configuration = new OAuth2Configuration(issuer, jwtGenerator.getJwsAlgorithm(), null,
        jwtGenerator.getJWKSetString());
    oAuth2Configuration.setUsernameClaim(JWTGenerator.USERNAME_CLAIM);
    oAuth2Configuration.setFirstNameClaim(JWTGenerator.FIRST_NAME_CLAIM);
    oAuth2Configuration.setLastNameClaim(JWTGenerator.LAST_NAME_CLAIM);
    oAuth2Configuration.setEmailClaim(JWTGenerator.EMAIL_CLAIM);
    oAuth2Configuration.setGroupsClaim(JWTGenerator.GROUPS_CLAIM);

    ctx.lookup(OAuth2ConfigurationDAO.class).insert(oAuth2Configuration);
  }

  private void assertAuditLog(
      final AuditDTO auditDTO,
      final String method,
      final String resourcePath,
      final String error)
  {
    assertThat(auditDTO.requestMethod).isEqualTo(method);
    assertThat(auditDTO.requestUri).isEqualTo(resourcePath);
    assertThat(auditDTO.domain).isEqualTo("authentication");
    assertThat(auditDTO.type).isEqualTo("failure");
    assertThat(auditDTO.error).isEqualTo(error);
  }
}
