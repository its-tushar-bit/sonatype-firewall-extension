/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.configuration.ldap.TestLdapServer;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.model.security.UserToken;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.assertj.core.api.Assertions.assertThat;

@Category(SlowTest.class)
public class UserTokenAuthcAuditTest
    extends AbstractAuditTest
{
  @Rule
  public TestLdapServer embeddedTestldapServer = new TestLdapServer();

  @Test
  public void testAuthenticate_LDAPUserDoesNotExistAnymore() throws Exception {
    embeddedTestldapServer.start();
    LdapServer ldapServer = tempEntity.newLdapServer("test");
    tempEntity.newLdapConnection(ldapServer.getId(), embeddedTestldapServer.getPort());
    tempEntity.newLdapUserMapping(ldapServer.getId());

    String userTokenPassword = "TestUserPass";
    String hashedUserTokenPassword =
        getCLMServer().getInstance(PasswordService.class).encryptPassword(userTokenPassword);
    UserToken userToken =
        tempEntity.newUserToken("UserDoesNotExist", "TestUserCode", hashedUserTokenPassword, ldapServer.getId());

    HttpRequest request = restRequest();
    HttpResponse response =
        request.path(UserSessionResource.RESOURCE_PATH).auth(userToken.getUserCode(), userTokenPassword).get();
    assertResponseStatus(401, response);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_USER_TOKEN, null, MDCUsernameScope.SYSTEM);
    assertThat(auditDTO.data).containsEntry("username", userToken.getUsername());
    assertThat(auditDTO.data).containsEntry("userCode", userToken.getUserCode());
  }
}
