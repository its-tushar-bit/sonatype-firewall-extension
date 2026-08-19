/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.configuration.ldap.EmbeddedLdapServerExtension;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.model.security.UserToken;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.security.PasswordService;
import com.sonatype.insight.brain.security.UserSessionResource;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Kept in the {@code com.sonatype.insight.brain.variant} package; implements {@link AuditTestSupport} directly
 * (rather than inheriting {@code AbstractAuditTest}) so it can register its own {@link LogOutput}, mirroring the
 * legacy {@code UserTokenAuthcAuditTest}.
 */
@IqH2Test
class IqH2UserTokenAuthcAuditTest
    implements AuditTestSupport
{
  private IqTestContext ctx;

  @RegisterExtension
  private final LogOutput logOutput = new LogOutput(AuditRecorder.BASE_LOGGER_NAME);

  @RegisterExtension
  private final EmbeddedLdapServerExtension embeddedTestldapServer = new EmbeddedLdapServerExtension();

  @Override
  public LogOutput getLogOutput() {
    return logOutput;
  }

  @Override
  public PolicyDAO getPolicyDAO() {
    return ctx.lookup(PolicyDAO.class);
  }

  @Test
  void testAuthenticate_LDAPUserDoesNotExistAnymore() throws Exception {
    embeddedTestldapServer.start();
    LdapServer ldapServer = ctx.tempEntity().newLdapServer("test");
    ctx.tempEntity().newLdapConnection(ldapServer.getId(), embeddedTestldapServer.getPort());
    ctx.tempEntity().newLdapUserMapping(ldapServer.getId());

    String userTokenPassword = "TestUserPass";
    String hashedUserTokenPassword = ctx.lookup(PasswordService.class).encryptPassword(userTokenPassword);
    UserToken userToken =
        ctx.tempEntity().newUserToken("UserDoesNotExist", "TestUserCode", hashedUserTokenPassword, ldapServer.getId());

    HttpRequest request = ctx.restRequest();
    HttpResponse response =
        request.path(UserSessionResource.RESOURCE_PATH).auth(userToken.getUserCode(), userTokenPassword).get();
    ctx.assertResponseStatus(401, response);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_USER_TOKEN, null, MDCUsernameScope.SYSTEM);
    assertThat(auditDTO.data).containsEntry("username", userToken.getUsername());
    assertThat(auditDTO.data).containsEntry("userCode", userToken.getUserCode());
  }
}
