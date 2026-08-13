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
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ComponentH2Test
public class LdapServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private LdapService ldapService;

  @Test
  public void testAddLdapServer_Authorized() {
    grantConfigureSystemPermission();
    ldapService.addLdapServer(new LdapServer("test"));
  }

  @Test
  public void testAddLdapServer_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> ldapService.addLdapServer(new LdapServer("test")));
  }

  @Test
  public void testAddLdapServer_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> ldapService.addLdapServer(new LdapServer("test")));
  }

  @Test
  public void testUpdateLdapServer_Authorized() {
    grantConfigureSystemPermission();
    ldapService.updateLdapServer(tempEntity.newLdapServer("test"));
  }

  @Test
  public void testUpdateLdapServer_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> ldapService.updateLdapServer(tempEntity.newLdapServer("test")));
  }

  @Test
  public void testUpdateLdapServer_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> ldapService.updateLdapServer(tempEntity.newLdapServer("test")));
  }

  @Test
  public void testDeleteLdapServer_Authorized() {
    grantConfigureSystemPermission();
    ldapService.deleteLdapServer(tempEntity.newLdapServer("test").getId());
  }

  @Test
  public void testDeleteLdapServer_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> ldapService.deleteLdapServer(tempEntity.newLdapServer("test").getId()));
  }

  @Test
  public void testDeleteLdapServer_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> ldapService.deleteLdapServer(tempEntity.newLdapServer("test").getId()));
  }

  @Test
  public void testGetAllLdapServers_Authorized() {
    grantConfigureSystemPermission();
    ldapService.getAllLdapServers();
  }

  @Test
  public void testGetAllLdapServers_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> ldapService.getAllLdapServers());
  }

  @Test
  public void testGetAllLdapServers_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> ldapService.getAllLdapServers());
  }

  @Test
  public void testGetLdapConnection_Authorized() {
    grantConfigureSystemPermission();
    ldapService.getLdapConnection(tempEntity.newLdapServer("test").getId());
  }

  @Test
  public void testGetLdapConnection_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> ldapService.getLdapConnection("fake LDAP server id"));
  }

  @Test
  public void testGetLdapConnection_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> ldapService.getLdapConnection("fake LDAP server id"));
  }

  @Test
  public void testUpsertLdapConnection_Authorized() {
    grantConfigureSystemPermission();
    LdapServer ldapServer = tempEntity.newLdapServer("test");
    ldapService.upsertLdapConnection(ldapServer.getId(), tempEntity.newLdapConnection(ldapServer.getId()));
  }

  @Test
  public void testUpsertLdapConnection_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> ldapService.upsertLdapConnection("fake LDAP server id", new LdapConnection()));
  }

  @Test
  public void testUpsertLdapConnection_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> ldapService.upsertLdapConnection("fake LDAP server id", new LdapConnection()));
  }

  @Test
  public void testGetLdapUserMapping_Authorized() {
    grantConfigureSystemPermission();
    ldapService.getLdapUserMapping(tempEntity.newLdapServer("test").getId());
  }

  @Test
  public void testGetLdapUserMapping_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> ldapService.getLdapUserMapping("fake LDAP server id"));
  }

  @Test
  public void testGetLdapUserMapping_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> ldapService.getLdapUserMapping("fake LDAP server id"));
  }

  @Test
  public void testUpsertLdapUserMapping_Authorized() {
    grantConfigureSystemPermission();
    LdapServer ldapServer = tempEntity.newLdapServer("test");
    ldapService.upsertLdapUserMapping(ldapServer.getId(), tempEntity.newLdapUserMapping(ldapServer.getId()));
  }

  @Test
  public void testUpsertLdapUserMapping_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> ldapService.upsertLdapUserMapping("fake LDAP server id", new LdapUserMapping()));
  }

  @Test
  public void testUpsertLdapUserMapping_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> ldapService.upsertLdapUserMapping("fake LDAP server id", new LdapUserMapping()));
  }

  @Test
  public void testTestLdapConnection_Authorized() {
    grantConfigureSystemPermission();
    LdapServer ldapServer = tempEntity.newLdapServer("test");
    ldapService.testLdapConnection(ldapServer.getId(), tempEntity.newLdapConnection(ldapServer.getId()));
  }

  @Test
  public void testTestLdapConnection_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> ldapService.testLdapConnection("fake LDAP server id", new LdapConnection()));
  }

  @Test
  public void testTestLdapConnection_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> ldapService.testLdapConnection("fake LDAP server id", new LdapConnection()));
  }

  @Test
  public void testTestUserLogin_Authorized() {
    grantConfigureSystemPermission();
    LdapServer ldapServer = tempEntity.newLdapServer("test");
    ldapService.testUserLogin(ldapServer.getId(), tempEntity.newLdapUserMapping(ldapServer.getId()), "user",
        "pass".toCharArray());
  }

  @Test
  public void testTestUserLogin_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> ldapService.testUserLogin("fake LDAP server id", new LdapUserMapping(), "user",
            "pass".toCharArray()));
  }

  @Test
  public void testTestUserLogin_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> ldapService.testUserLogin("fake LDAP server id", new LdapUserMapping(), "user",
            "pass".toCharArray()));
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

  @Test
  public void testTestLdapUserMapping_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> ldapService.testLdapUserMapping("fake LDAP server id", new LdapUserMapping(), 0));
  }

  @Test
  public void testTestLdapUserMapping_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> ldapService.testLdapUserMapping("fake LDAP server id", new LdapUserMapping(), 0));
  }

  @Test
  public void testUpdatePriority_Authorized() {
    grantConfigureSystemPermission();
    ldapService.updatePriority(Collections.emptyList());
  }

  @Test
  public void testUpdatePriority_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> ldapService.updatePriority(Collections.emptyList()));
  }

  @Test
  public void testUpdatePriority_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> ldapService.updatePriority(Collections.emptyList()));
  }
}
