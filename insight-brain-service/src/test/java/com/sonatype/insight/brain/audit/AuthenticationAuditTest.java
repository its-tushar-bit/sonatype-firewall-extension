/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

import java.io.IOException;
import java.net.HttpCookie;
import java.util.List;

import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.organization.ApplicationResource;
import com.sonatype.insight.brain.security.SecurityModule;
import com.sonatype.insight.brain.security.UserSessionResource;
import com.sonatype.insight.brain.service.AbstractBrainServiceTest;
import com.sonatype.insight.brain.service.ReverseProxyAuthenticationConfig;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.json.store.UncheckedIOException;
import com.sonatype.insight.test.LogOutput;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

public class AuthenticationAuditTest
    extends AbstractBrainServiceTest
{
  private static final String RESTRICTED_PATH = "/" + ApplicationResource.RESOURCE_PATH;

  private static final String RESTRICTED_UNSAFE_PATH = RESTRICTED_PATH + "/applicationPublicId";

  private static final String AUTH_RESOURCE_PATH = "/" + UserSessionResource.RESOURCE_PATH;

  private static final String AUDIT_LOGGER = "com.sonatype.insight.audit.authentication";

  @Rule
  public LogOutput logOutput = new LogOutput("com.sonatype.insight.audit");

  @Before
  public void before() {
    logOutput.before();
  }

  @Test
  public void testLoginLogout() throws Exception {
    HttpCookie sessionCookie = restRequest().path(UserSessionResource.RESOURCE_PATH).post().getSessionCookie();

    List<String> auditAuthenticationMessages = awaitLogMessages(AUDIT_LOGGER, 1);
    AuditDTO log = parseAuditLog(auditAuthenticationMessages.get(0));
    assertThat(log.domain, is("authentication"));
    assertThat(log.type, is("login"));
    assertThat(log.timestamp, not(isEmptyOrNullString()));
    assertThat(log.error, is(nullValue()));
    assertThat(log.username, is(User.ADMIN_USERNAME));
    assertThat(log.method, is("POST"));
    assertThat(log.path, is(AUTH_RESOURCE_PATH));

    restRequest().path(UserSessionResource.RESOURCE_PATH, UserSessionResource.LOGOUT_PATH).anon().cookie(sessionCookie)
        .delete();

    auditAuthenticationMessages = awaitLogMessages(AUDIT_LOGGER, 2);
    log = parseAuditLog(auditAuthenticationMessages.get(1));
    assertThat(log.domain, is("authentication"));
    assertThat(log.type, is("logout"));
    assertThat(log.timestamp, not(isEmptyOrNullString()));
    assertThat(log.error, is(nullValue()));
    assertThat(log.username, is(User.ADMIN_USERNAME));
    assertThat(log.method, is("DELETE"));
    assertThat(log.path, is(AUTH_RESOURCE_PATH + '/' + UserSessionResource.LOGOUT_PATH));
  }

  @Test
  public void testImplicitLoginByReverseProxy() throws Exception {
    ReverseProxyAuthenticationConfig rutConfig = new ReverseProxyAuthenticationConfig();
    rutConfig.setEnabled(true);
    String username = "rut-user";
    initServer(config -> config.setReverseProxyAuthentication(rutConfig));
    logOutput.before(); // need to restore appender after DW is done setting up logging

    restRequest().path(RESTRICTED_PATH).anon().header(rutConfig.getUsernameHeader(), username).get();

    List<String> auditAuthenticationMessages = awaitLogMessages(AUDIT_LOGGER, 1);
    AuditDTO log = parseAuditLog(auditAuthenticationMessages.get(0));
    assertThat(log.domain, is("authentication"));
    assertThat(log.type, is("login"));
    assertThat(log.timestamp, not(isEmptyOrNullString()));
    assertThat(log.error, is(nullValue()));
    assertThat(log.username, is(username));
    assertThat(log.method, is("GET"));
    assertThat(log.path, is(RESTRICTED_PATH));
  }

  @Test
  public void testNoAuthenticationHeadersOrCookies() throws Exception {
    restRequest().anon().path(RESTRICTED_PATH).get();

    List<String> auditAuthenticationMessages = awaitLogMessages(AUDIT_LOGGER, 1);
    assertAuditLog(auditAuthenticationMessages.get(0), "GET", RESTRICTED_PATH, "unauthenticated");
  }

  @Test
  public void testInvalidUserNamePassword() throws Exception {
    restRequest().auth("invalidUser", "invalidPassword").path(AUTH_RESOURCE_PATH).post();

    List<String> auditAuthenticationMessages = awaitLogMessages(AUDIT_LOGGER, 1);
    assertAuditLog(auditAuthenticationMessages.get(0), "POST", AUTH_RESOURCE_PATH, "bad-authentication");
  }

  @Test
  public void testInvalidCsrfToken() throws Exception {
    restRequest().path(RESTRICTED_UNSAFE_PATH).noCsrfToken().delete();

    List<String> auditAuthenticationMessages = awaitLogMessages(AUDIT_LOGGER, 1);
    assertAuditLog(auditAuthenticationMessages.get(0), "DELETE", RESTRICTED_UNSAFE_PATH, "bad-csrf-token");
  }

  @Test
  public void testBadSessionCookie() throws Exception {
    restRequest().path(RESTRICTED_PATH).anon().cookie(SecurityModule.SESSION_COOKIE_NAME, "bad").get();

    List<String> auditAuthenticationMessages = awaitLogMessages(AUDIT_LOGGER, 1);
    assertAuditLog(auditAuthenticationMessages.get(0), "GET", RESTRICTED_PATH, "bad-session");
  }

  @Test
  public void testAuthenticationInternalError() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("ldap");
    tempEntity.newLdapConnection(ldapServer.getId());
    tempEntity.newLdapUserMapping(ldapServer.getId());

    restRequest().auth("user", "pass").path(RESTRICTED_PATH).get();

    List<String> auditAuthenticationMessages = awaitLogMessages(AUDIT_LOGGER, 1);
    assertAuditLog(auditAuthenticationMessages.get(0), "GET", RESTRICTED_PATH, AuditRecorder.SERVER_ERROR);
  }

  private List<String> awaitLogMessages(String logger, int count) {
    return await().atMost(5, SECONDS)
        .until(() -> logOutput.getInfoMessages(logger), hasSize(greaterThanOrEqualTo(count)));
  }

  private AuditDTO parseAuditLog(String auditLogEntry) {
    try {
      return JsonUtils.parse(auditLogEntry, AuditDTO.class);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private void assertAuditLog(final String auditLogEntry,
                              final String method,
                              final String resourcePath,
                              final String error)
  {
    AuditDTO auditDTO = parseAuditLog(auditLogEntry);
    assertThat(auditDTO.method, is(method));
    assertThat(auditDTO.path, is(resourcePath));
    assertThat(auditDTO.domain, is("authentication"));
    assertThat(auditDTO.type, is("failure"));
    assertThat(auditDTO.error, is(error));
  }
}
