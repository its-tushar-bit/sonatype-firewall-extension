/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration.ldap;

import java.util.Collections;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.model.configuration.ldap.LdapConnection;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.model.configuration.ldap.LdapUserMapping;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;

import org.junit.Test;

public class LdapResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(LdapResource.RESOURCE_PATH);
  }

  @Test
  public void testTestConnection() throws Exception {
    grantConfigureSystemPermission();

    LdapServer ldapServer = tempEntity.newLdapServer("testTestConnection");
    // The LdapConnection should not be persisted to the db at this point.
    LdapConnection ldapConnection = new LdapConnection();
    ldapConnection.setServerId(ldapServer.getId());
    testAuthzPut(
        restRequest().path("{ldapServerId}/testConnection").parameter(ldapServer.getId()).body(ldapConnection));
  }

  @Test
  public void testTestLogin() throws Exception {
    grantConfigureSystemPermission();

    LdapServer ldapServer = tempEntity.newLdapServer("testTestLogin");
    // The LdapUserMapping should not be persisted to the db at this point.
    LdapUserMapping ldapUserMapping = new LdapUserMapping();
    ldapUserMapping.setServerId(ldapServer.getId());
    LdapTestLoginRequest ldapTestLoginRequest = new LdapTestLoginRequest();
    ldapTestLoginRequest.setUserMapping(ldapUserMapping);
    ldapTestLoginRequest.setUsername("testTestLogin");
    ldapTestLoginRequest.setPassword("testTestLogin");
    testAuthzPut(
        restRequest().path("{ldapServerId}/testLogin").parameter(ldapServer.getId()).body(ldapTestLoginRequest));
  }

  @Test
  public void testTestUserMapping() throws Exception {
    grantConfigureSystemPermission();

    LdapServer ldapServer = tempEntity.newLdapServer("testUserMapping");
    tempEntity.newLdapConnection(ldapServer.getId());
    // The LdapUserMapping should not be persisted to the db at this point.
    LdapUserMapping ldapUserMapping = new LdapUserMapping();
    ldapUserMapping.setServerId(ldapServer.getId());
    // 400 because we don't need a successful call. We only need to get past authorization.
    testAuthzPut(
        restRequest().path("{ldapServerId}/testUserMapping").parameter(ldapServer.getId()).body(ldapUserMapping), 400);
  }

  @Test
  public void testPriority() throws Exception {
    grantConfigureSystemPermission();

    LdapServer ldapServer = tempEntity.newLdapServer("testPriority");
    testAuthzPut(restRequest().path(LdapResource.PRIORITY_PATH).body(Collections.singletonList(ldapServer.getId())));
  }
}
