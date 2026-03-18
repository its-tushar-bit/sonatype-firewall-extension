/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration.ldap;

import java.util.Collections;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.model.configuration.ldap.LdapConnection;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.model.configuration.ldap.LdapUserMapping;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class LdapServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private LdapService ldapService;

  @Test
  public void testAddLdapServer_Authorized() {
    grantConfigureSystemPermission();
    ldapService.addLdapServer(new LdapServer("test"));
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddLdapServer_Unauthorized() {
    login();
    ldapService.addLdapServer(new LdapServer("test"));
  }

  @Test(expected = UnauthenticatedException.class)
  public void testAddLdapServer_Unauthenticated() {
    ldapService.addLdapServer(new LdapServer("test"));
  }

  @Test
  public void testUpdateLdapServer_Authorized() {
    grantConfigureSystemPermission();
    ldapService.updateLdapServer(tempEntity.newLdapServer("test"));
  }

  @Test(expected = UnauthorizedException.class)
  public void testUpdateLdapServer_Unauthorized() {
    login();
    ldapService.updateLdapServer(tempEntity.newLdapServer("test"));
  }

  @Test(expected = UnauthenticatedException.class)
  public void testUpdateLdapServer_Unauthenticated() {
    ldapService.updateLdapServer(tempEntity.newLdapServer("test"));
  }

  @Test
  public void testDeleteLdapServer_Authorized() {
    grantConfigureSystemPermission();
    ldapService.deleteLdapServer(tempEntity.newLdapServer("test").getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeleteLdapServer_Unauthorized() {
    login();
    ldapService.deleteLdapServer(tempEntity.newLdapServer("test").getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeleteLdapServer_Unauthenticated() {
    ldapService.deleteLdapServer(tempEntity.newLdapServer("test").getId());
  }

  @Test
  public void testGetAllLdapServers_Authorized() {
    grantConfigureSystemPermission();
    ldapService.getAllLdapServers();
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetAllLdapServers_Unauthorized() {
    login();
    ldapService.getAllLdapServers();
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetAllLdapServers_Unauthenticated() {
    ldapService.getAllLdapServers();
  }

  @Test
  public void testGetLdapConnection_Authorized() {
    grantConfigureSystemPermission();
    ldapService.getLdapConnection(tempEntity.newLdapServer("test").getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetLdapConnection_Unauthorized() {
    login();
    ldapService.getLdapConnection("fake LDAP server id");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetLdapConnection_Unauthenticated() {
    ldapService.getLdapConnection("fake LDAP server id");
  }

  @Test
  public void testUpsertLdapConnection_Authorized() {
    grantConfigureSystemPermission();
    LdapServer ldapServer = tempEntity.newLdapServer("test");
    ldapService.upsertLdapConnection(ldapServer.getId(), tempEntity.newLdapConnection(ldapServer.getId()));
  }

  @Test(expected = UnauthorizedException.class)
  public void testUpsertLdapConnection_Unauthorized() {
    login();
    ldapService.upsertLdapConnection("fake LDAP server id", new LdapConnection());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testUpsertLdapConnection_Unauthenticated() {
    ldapService.upsertLdapConnection("fake LDAP server id", new LdapConnection());
  }

  @Test
  public void testGetLdapUserMapping_Authorized() {
    grantConfigureSystemPermission();
    ldapService.getLdapUserMapping(tempEntity.newLdapServer("test").getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetLdapUserMapping_Unauthorized() {
    login();
    ldapService.getLdapUserMapping("fake LDAP server id");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetLdapUserMapping_Unauthenticated() {
    ldapService.getLdapUserMapping("fake LDAP server id");
  }

  @Test
  public void testUpsertLdapUserMapping_Authorized() {
    grantConfigureSystemPermission();
    LdapServer ldapServer = tempEntity.newLdapServer("test");
    ldapService.upsertLdapUserMapping(ldapServer.getId(), tempEntity.newLdapUserMapping(ldapServer.getId()));
  }

  @Test(expected = UnauthorizedException.class)
  public void testUpsertLdapUserMapping_Unauthorized() {
    login();
    ldapService.upsertLdapUserMapping("fake LDAP server id", new LdapUserMapping());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testUpsertLdapUserMapping_Unauthenticated() {
    ldapService.upsertLdapUserMapping("fake LDAP server id", new LdapUserMapping());
  }

  @Test
  public void testTestLdapConnection_Authorized() {
    grantConfigureSystemPermission();
    LdapServer ldapServer = tempEntity.newLdapServer("test");
    ldapService.testLdapConnection(ldapServer.getId(), tempEntity.newLdapConnection(ldapServer.getId()));
  }

  @Test(expected = UnauthorizedException.class)
  public void testTestLdapConnection_Unauthorized() {
    login();
    ldapService.testLdapConnection("fake LDAP server id", new LdapConnection());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testTestLdapConnection_Unauthenticated() {
    ldapService.testLdapConnection("fake LDAP server id", new LdapConnection());
  }

  @Test
  public void testTestUserLogin_Authorized() {
    grantConfigureSystemPermission();
    LdapServer ldapServer = tempEntity.newLdapServer("test");
    ldapService.testUserLogin(ldapServer.getId(), tempEntity.newLdapUserMapping(ldapServer.getId()), "user",
        "pass".toCharArray());
  }

  @Test(expected = UnauthorizedException.class)
  public void testTestUserLogin_Unauthorized() {
    login();
    ldapService.testUserLogin("fake LDAP server id", new LdapUserMapping(), "user", "pass".toCharArray());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testTestUserLogin_Unauthenticated() {
    ldapService.testUserLogin("fake LDAP server id", new LdapUserMapping(), "user", "pass".toCharArray());
  }

  @Test
  public void testTestLdapUserMapping_Authorized() {
    grantConfigureSystemPermission();
    LdapServer ldapServer = tempEntity.newLdapServer("test");
    assertThatThrownBy(() -> ldapService
        .testLdapUserMapping(ldapServer.getId(), tempEntity.newLdapUserMapping(ldapServer.getId()), 20))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("LDAP connection is not configured");
  }

  @Test(expected = UnauthorizedException.class)
  public void testTestLdapUserMapping_Unauthorized() {
    login();
    ldapService.testLdapUserMapping("fake LDAP server id", new LdapUserMapping(), 0);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testTestLdapUserMapping_Unauthenticated() {
    ldapService.testLdapUserMapping("fake LDAP server id", new LdapUserMapping(), 0);
  }

  @Test
  public void testUpdatePriority_Authorized() {
    grantConfigureSystemPermission();
    ldapService.updatePriority(Collections.emptyList());
  }

  @Test(expected = UnauthorizedException.class)
  public void testUpdatePriority_Unauthorized() {
    login();
    ldapService.updatePriority(Collections.emptyList());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testUpdatePriority_Unauthenticated() {
    ldapService.updatePriority(Collections.emptyList());
  }
}
