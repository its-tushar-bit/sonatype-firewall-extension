/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

import java.net.HttpCookie;

import com.sonatype.insight.brain.api.v2.service.ApiReverseProxyAuthenticationConfigurationService;
import com.sonatype.insight.brain.model.configuration.ReverseProxyAuthenticationConfiguration;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.organization.ApplicationResource;
import com.sonatype.insight.brain.security.SecurityModule;
import com.sonatype.insight.brain.security.UserSessionResource;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AuthenticationAuditTest
    extends AbstractAuditTest
{
  private static final String RESTRICTED_PATH = "/" + ApplicationResource.RESOURCE_PATH;

  private static final String RESTRICTED_UNSAFE_PATH = RESTRICTED_PATH + "/applicationPublicId";

  private static final String AUTH_RESOURCE_PATH = "/" + UserSessionResource.RESOURCE_PATH;

  @Test
  public void testLoginLogout() throws Exception {
    HttpCookie sessionCookie = restRequest().path(UserSessionResource.RESOURCE_PATH).post().getSessionCookie();

    AuditDTO log = awaitLogEntries(AuditEvent.LOGIN, 1).get(0);
    assertThat(log.domain).isEqualTo("authentication");
    assertThat(log.type).isEqualTo("login");
    assertThat(log.timestamp).isNotEmpty();
    assertThat(log.error).isNull();
    assertThat(log.username).isEqualTo(User.ADMIN_USERNAME);
    assertThat(log.requestMethod).isEqualTo("POST");
    assertThat(log.requestUri).isEqualTo(AUTH_RESOURCE_PATH);

    restRequest().path(UserSessionResource.RESOURCE_PATH, UserSessionResource.LOGOUT_PATH).anon().cookie(sessionCookie)
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
  public void testImplicitLoginByReverseProxy() throws Exception {
    ReverseProxyAuthenticationConfiguration rutConfig = tempEntity.newReverseProxyAuthenticationConfiguration(true,
        ReverseProxyAuthenticationConfiguration.DEFAULT_USERNAME_HEADER, false, null);
    getCLMServer().getInstance(ApiReverseProxyAuthenticationConfigurationService.class)
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
  public void testNoAuthenticationHeadersOrCookies() throws Exception {
    restRequest().anon().path(RESTRICTED_PATH).get();

    AuditDTO log = awaitLogEntries(AuditEvent.AUTHENTICATION_FAILURE, 1).get(0);
    assertAuditLog(log, "GET", RESTRICTED_PATH, "unauthenticated");
  }

  @Test
  public void testInvalidUserNamePassword() throws Exception {
    restRequest().auth("invalidUser", "invalidPassword").path(AUTH_RESOURCE_PATH).post();

    AuditDTO log = awaitLogEntries(AuditEvent.AUTHENTICATION_FAILURE, 1).get(0);
    assertAuditLog(log, "POST", AUTH_RESOURCE_PATH, "bad-authentication");
  }

  @Test
  public void testInvalidCsrfToken() throws Exception {
    restRequest().path(RESTRICTED_UNSAFE_PATH).noCsrfToken().delete();

    AuditDTO log = awaitLogEntries(AuditEvent.AUTHENTICATION_FAILURE, 1).get(0);
    assertAuditLog(log, "DELETE", RESTRICTED_UNSAFE_PATH, "bad-csrf-token");
  }

  @Test
  public void testBadSessionCookie() throws Exception {
    restRequest().path(RESTRICTED_PATH).anon().cookie(SecurityModule.SESSION_COOKIE_NAME, "bad").get();

    AuditDTO log = awaitLogEntries(AuditEvent.AUTHENTICATION_FAILURE, 1).get(0);
    assertAuditLog(log, "GET", RESTRICTED_PATH, "bad-session");
  }

  @Test
  public void testAuthenticationInternalError() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("ldap");
    tempEntity.newLdapConnection(ldapServer.getId());
    tempEntity.newLdapUserMapping(ldapServer.getId());

    restRequest().auth("user", "pass").path(RESTRICTED_PATH).get();

    AuditDTO log = awaitLogEntries(AuditEvent.AUTHENTICATION_FAILURE, 1).get(0);
    assertAuditLog(log, "GET", RESTRICTED_PATH, AuditRecorder.SERVER_ERROR);
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
