/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration.ldap;

import java.util.Collections;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapServerDAO;
import com.sonatype.insight.brain.model.configuration.ldap.LdapConnection;
import com.sonatype.insight.brain.model.configuration.ldap.LdapGroupMappingType;
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
  public void testGetAll() throws Exception {
    grantConfigureSystemPermission();

    testAuthzGet(restRequest());
  }

  @Test
  public void testGetConnection() throws Exception {
    grantConfigureSystemPermission();

    LdapServer ldapServer = tempEntity.newLdapServer("testGetConnection");
    testAuthzGet(restRequest().path("{ldapServerId}/connection").parameter(ldapServer.getId()));
  }

  @Test
  public void testGetUserMapping() throws Exception {
    grantConfigureSystemPermission();

    LdapServer ldapServer = tempEntity.newLdapServer("testGetUserMapping");
    testAuthzGet(restRequest().path("{ldapServerId}/userMapping").parameter(ldapServer.getId()));
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
  public void testAddLdapServer() throws Exception {
    grantConfigureSystemPermission();

    HttpResponse response = testAuthzPost(restRequest().body(new LdapServer("testAddLdapServer")));
    new LdapServerDAO().delete(response.getBody(LdapServer.class));
  }

  @Test
  public void testUpdateLdapServer() throws Exception {
    grantConfigureSystemPermission();

    testAuthzPut(restRequest().body(tempEntity.newLdapServer("testUpdateLdapServer")));
  }

  @Test
  public void testDeleteLdapServer() throws Exception {
    grantConfigureSystemPermission();

    LdapServer ldapServer = tempEntity.newLdapServer("testDeleteLdapServer");
    testAuthzDelete(restRequest().path("{ldapServerId}").parameter(ldapServer.getId()));
  }

  @Test
  public void testUpdateLdapConnection() throws Exception {
    grantConfigureSystemPermission();

    LdapServer ldapServer = tempEntity.newLdapServer("testUpdateLdapConnection");
    LdapConnection ldapConnection = tempEntity.newLdapConnection(ldapServer.getId());
    testAuthzPut(restRequest().path("{ldapServerId}/connection").parameter(ldapServer.getId()).body(ldapConnection));
  }

  @Test
  public void testUpdateUserMapping() throws Exception {
    grantConfigureSystemPermission();

    LdapServer ldapServer = tempEntity.newLdapServer("testUpdateUserMapping");
    tempEntity.newLdapConnection(ldapServer.getId());
    LdapUserMapping ldapUserMapping = new LdapUserMapping();
    ldapUserMapping.setUserBaseDN("userBaseDN");
    ldapUserMapping.setUserSubtree(true);
    ldapUserMapping.setUserObjectClass("userObjectClass");
    ldapUserMapping.setUserFilter("userFilter");
    ldapUserMapping.setUserIDAttribute("userIDAttribute");
    ldapUserMapping.setUserRealNameAttribute("realNameAttribute");
    ldapUserMapping.setUserEmailAttribute("emailAttribute");
    ldapUserMapping.setUserPasswordAttribute("passwordAttribute");
    ldapUserMapping.setGroupMappingType(LdapGroupMappingType.STATIC);
    ldapUserMapping.setGroupBaseDN("groupBaseDN");
    ldapUserMapping.setGroupSubtree(true);
    ldapUserMapping.setGroupObjectClass("groupObjectClass");
    ldapUserMapping.setGroupIDAttribute("groupIDAttribute");
    ldapUserMapping.setGroupMemberAttribute("groupMemberAttribute");
    ldapUserMapping.setGroupMemberFormat("groupMemberFormat");
    ldapUserMapping.setUserMemberOfGroupAttribute("userMemberOfGroupAttribute");
    ldapUserMapping.setServerId(ldapServer.getId());
    testAuthzPut(restRequest().path("{ldapServerId}/userMapping").parameter(ldapServer.getId()).body(ldapUserMapping));
  }

  @Test
  public void testPriority() throws Exception {
    grantConfigureSystemPermission();

    LdapServer ldapServer = tempEntity.newLdapServer("testPriority");
    testAuthzPut(restRequest().path(LdapResource.PRIORITY_PATH).body(Collections.singletonList(ldapServer.getId())));
  }
}
