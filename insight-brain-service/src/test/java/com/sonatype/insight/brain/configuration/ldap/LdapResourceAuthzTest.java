/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration.ldap;

import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapServerDAO;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;
import com.sonatype.insight.test.RestAccess;

import com.ning.http.client.Response;
import org.junit.Test;

public class LdapResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @Test
  public void testGetAll() throws Exception {
    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, Role.ADMIN_ROLE_ID, authorized.getUsername());

    String url = getRestUrl(LdapResource.SERVICE_PATH);
    Response response = RestAccess.get(url, unauthorized.getUsername(), unauthorized.getPassword());
    assertResponseStatus(403, response);

    response = RestAccess.get(url, authorized.getUsername(), authorized.getPassword());
    assertResponseStatus(200, response);
  }

  @Test
  public void testGetConnection() throws Exception {
    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, Role.ADMIN_ROLE_ID, authorized.getUsername());

    LdapServer ldapServer = tempEntity.newLdapServer("testGetConnection");
    String url = getRestUrl(LdapResource.SERVICE_PATH + "/{ldapServerId}/connection", ldapServer.getId());
    Response response = RestAccess.get(url, unauthorized.getUsername(), unauthorized.getPassword());
    assertResponseStatus(403, response);

    response = RestAccess.get(url, authorized.getUsername(), authorized.getPassword());
    assertResponseStatus(200, response);
  }

  @Test
  public void testGetUserMapping() throws Exception {
    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, Role.ADMIN_ROLE_ID, authorized.getUsername());

    LdapServer ldapServer = tempEntity.newLdapServer("testGetUserMapping");
    String url = getRestUrl(LdapResource.SERVICE_PATH + "/{ldapServerId}/userMapping", ldapServer.getId());
    Response response = RestAccess.get(url, unauthorized.getUsername(), unauthorized.getPassword());
    assertResponseStatus(403, response);

    response = RestAccess.get(url, authorized.getUsername(), authorized.getPassword());
    assertResponseStatus(200, response);
  }

  @Test
  public void testTestConnection() throws Exception {
    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, Role.ADMIN_ROLE_ID, authorized.getUsername());

    LdapServer ldapServer = tempEntity.newLdapServer("testTestConnection");
    // The LdapConnection should not be persisted to the db at this point.
    LdapConnection ldapConnection = new LdapConnection();
    ldapConnection.setServerId(ldapServer.getId());
    String url = getRestUrl(LdapResource.SERVICE_PATH + "/{ldapServerId}/testConnection", ldapServer.getId());
    Response response = RestAccess.put(url, unauthorized.getUsername(), unauthorized.getPassword(),
        toJson(ldapConnection));
    assertResponseStatus(403, response);

    response = RestAccess.put(url, authorized.getUsername(), authorized.getPassword(), toJson(ldapConnection));
    assertResponseStatus(200, response);
  }

  @Test
  public void testTestLogin() throws Exception {
    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, Role.ADMIN_ROLE_ID, authorized.getUsername());

    LdapServer ldapServer = tempEntity.newLdapServer("testTestLogin");
    // The LdapUserMapping should not be persisted to the db at this point.
    LdapUserMapping ldapUserMapping = new LdapUserMapping();
    ldapUserMapping.setServerId(ldapServer.getId());
    LdapTestLoginRequest ldapTestLoginRequest = new LdapTestLoginRequest();
    ldapTestLoginRequest.setUserMapping(ldapUserMapping);
    ldapTestLoginRequest.setUsername("testTestLogin");
    ldapTestLoginRequest.setPassword("testTestLogin");
    String url = getRestUrl(LdapResource.SERVICE_PATH + "/{ldapServerId}/testLogin", ldapServer.getId());
    Response response = RestAccess.put(url, unauthorized.getUsername(), unauthorized.getPassword(),
        toJson(ldapTestLoginRequest));
    assertResponseStatus(403, response);

    response = RestAccess.put(url, authorized.getUsername(), authorized.getPassword(), toJson(ldapTestLoginRequest));
    assertResponseStatus(200, response);
  }

  @Test
  public void testTestUserMapping() throws Exception {
    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, Role.ADMIN_ROLE_ID, authorized.getUsername());

    LdapServer ldapServer = tempEntity.newLdapServer("testUserMapping");
    tempEntity.newLdapConnection(ldapServer.getId());
    // The LdapUserMapping should not be persisted to the db at this point.
    LdapUserMapping ldapUserMapping = new LdapUserMapping();
    ldapUserMapping.setServerId(ldapServer.getId());
    String url = getRestUrl(LdapResource.SERVICE_PATH + "/{ldapServerId}/testUserMapping", ldapServer.getId());
    Response response = RestAccess.put(url, unauthorized.getUsername(), unauthorized.getPassword(),
        toJson(ldapUserMapping));
    assertResponseStatus(403, response);

    response = RestAccess.put(url, authorized.getUsername(), authorized.getPassword(), toJson(ldapUserMapping));
    // 400 because we don't need a successful call. We only need to get past authorization.
    assertResponseStatus(400, response);
  }

  @Test
  public void testAddLdapServer() throws Exception {
    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, Role.ADMIN_ROLE_ID, authorized.getUsername());

    LdapServer ldapServer = new LdapServer("testAddLdapServer");
    String url = getRestUrl(LdapResource.SERVICE_PATH);
    Response response = RestAccess
        .post(url, unauthorized.getUsername(), unauthorized.getPassword(), toJson(ldapServer));
    assertResponseStatus(403, response);

    response = RestAccess.post(url, authorized.getUsername(), authorized.getPassword(), toJson(ldapServer));
    assertResponseStatus(200, response);
    ldapServer = fromJson(response, LdapServer.class);
    new LdapServerDAO().delete(ldapServer);
  }

  @Test
  public void testUpdateLdapServer() throws Exception {
    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, Role.ADMIN_ROLE_ID, authorized.getUsername());

    LdapServer ldapServer = tempEntity.newLdapServer("testUpdateLdapServer");
    String url = getRestUrl(LdapResource.SERVICE_PATH);
    Response response = RestAccess.put(url, unauthorized.getUsername(), unauthorized.getPassword(), toJson(ldapServer));
    assertResponseStatus(403, response);

    response = RestAccess.put(url, authorized.getUsername(), authorized.getPassword(), toJson(ldapServer));
    assertResponseStatus(200, response);
  }

  @Test
  public void testDeleteLdapServer() throws Exception {
    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, Role.ADMIN_ROLE_ID, authorized.getUsername());

    LdapServer ldapServer = tempEntity.newLdapServer("testDeleteLdapServer");
    String url = getRestUrl(LdapResource.SERVICE_PATH + "/{ldapServerId}", ldapServer.getId());
    Response response = RestAccess.delete(url, unauthorized.getUsername(), unauthorized.getPassword());
    assertResponseStatus(403, response);

    response = RestAccess.delete(url, authorized.getUsername(), authorized.getPassword());
    assertResponseStatus(204, response);
  }

  @Test
  public void testUpdateLdapConnection() throws Exception {
    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, Role.ADMIN_ROLE_ID, authorized.getUsername());

    LdapServer ldapServer = tempEntity.newLdapServer("testUpdateLdapConnection");
    LdapConnection ldapConnection = tempEntity.newLdapConnection(ldapServer.getId());
    String url = getRestUrl(LdapResource.SERVICE_PATH + "/{ldapServerId}/connection", ldapServer.getId());
    Response response = RestAccess.put(url, unauthorized.getUsername(), unauthorized.getPassword(),
        toJson(ldapConnection));
    assertResponseStatus(403, response);

    response = RestAccess.put(url, authorized.getUsername(), authorized.getPassword(), toJson(ldapConnection));
    assertResponseStatus(200, response);
  }

  @Test
  public void testUpdateUserMapping() throws Exception {
    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, Role.ADMIN_ROLE_ID, authorized.getUsername());

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
    String url = getRestUrl(LdapResource.SERVICE_PATH + "/{ldapServerId}/userMapping", ldapServer.getId());
    Response response = RestAccess.put(url, unauthorized.getUsername(), unauthorized.getPassword(),
        toJson(ldapUserMapping));
    assertResponseStatus(403, response);

    response = RestAccess.put(url, authorized.getUsername(), authorized.getPassword(), toJson(ldapUserMapping));
    assertResponseStatus(200, response);
  }
}
