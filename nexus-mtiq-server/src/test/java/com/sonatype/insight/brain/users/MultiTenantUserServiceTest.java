/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.users;

import java.util.List;

import com.sonatype.insight.brain.auth.MultiTenantAuth0ManagementService;
import com.sonatype.insight.brain.db.dao.TenantMetadataDAO;
import com.sonatype.insight.brain.model.security.TenantMetadata;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.security.MultiTenantSsoUserService;
import com.sonatype.insight.brain.security.SsoUser;
import com.sonatype.insight.brain.service.AbstractMultiTenantBaseIntegrationResourceTest;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantTestHelper;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.shiro.session.mgt.eis.SessionDAO;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MultiTenantUserServiceTest
    extends AbstractMultiTenantBaseIntegrationResourceTest
{
  private MultiTenantSsoUserService ssoUserService;

  private TenantMetadataDAO tenantMetadataDAO;

  private final MultiTenantAuth0ManagementService auth0ManagementService =
      Mockito.mock(MultiTenantAuth0ManagementService.class);

  private final CurrentUser currentUser = Mockito.mock(CurrentUser.class);

  private final DefaultWebSessionManager webSessionManager = Mockito.mock(DefaultWebSessionManager.class);

  private final SessionDAO sessionDAO = Mockito.mock(SessionDAO.class);

  private MtiqUserService underTest;

  @Before
  public void setUp() {
    ssoUserService = lookup(MultiTenantSsoUserService.class);
    tenantMetadataDAO = lookup(TenantMetadataDAO.class);

    underTest = new MultiTenantUserService(webSessionManager, sessionDAO, ssoUserService, tenantMetadataDAO,
        auth0ManagementService, currentUser);
  }

  @Test
  public void test_canListUsers() {
    MtiqUserDTO user1 = createMtiqUser("foo1");
    MtiqUserDTO user2 = createMtiqUser("foo2");

    ssoUserService.upsertByUsername(user1);
    ssoUserService.upsertByUsername(user2);

    assertThat(underTest.getAllUsers()).hasSize(2);
  }

  @Test
  public void test_listUsersIsOrderedByEmail() {
    MtiqUserDTO user1 = createMtiqUser("zebra");
    MtiqUserDTO user2 = createMtiqUser("ant");
    MtiqUserDTO user3 = createMtiqUser("monkey");
    MtiqUserDTO user4 = createMtiqUser("horse");

    ssoUserService.upsertByUsername(user1);
    ssoUserService.upsertByUsername(user2);
    ssoUserService.upsertByUsername(user3);
    ssoUserService.upsertByUsername(user4);

    List<MtiqUserDTO> allUsers = underTest.getAllUsers();
    assertThat(allUsers).containsSequence(user2, user4, user3, user1);
  }

  @Test
  public void test_canInviteAUser() {
    MtiqUserDTO user = createMtiqUser("foo");

    TenantTestHelper.testAsNewTenant(testName, tenant -> {
      provisionTenant(tenant.tenantSlug);
      createTenantMetadata(tenant);

      assertThat(ssoUserService.getAll()).hasSize(0);

      underTest.inviteUser(user);

      List<SsoUser> allUsers = ssoUserService.getAll();
      assertThat(allUsers).hasSize(1);
      assertThat(MtiqUserDTO.ssoUserToMtiqUser(allUsers.get(0))).usingRecursiveComparison().isEqualTo(user);
    });

    List<SsoUser> allUsers = ssoUserService.getAll();
    assertThat(allUsers).hasSize(0);
  }

  @Test
  public void test_canInviteMultipleUsers() {
    MtiqUserDTO user1 = createMtiqUser("foo");
    MtiqUserDTO user2 = createMtiqUser("bar");
    MtiqUserDTO user3 = createMtiqUser("baz");

    TenantTestHelper.testAsNewTenant(testName, tenant -> {
      provisionTenant(tenant.tenantSlug);
      createTenantMetadata(tenant);

      underTest.inviteUser(user1);
      underTest.inviteUser(user2);

      List<SsoUser> allUsers = ssoUserService.getAll();
      assertThat(allUsers).hasSize(2);
    });

    TenantTestHelper.testAsNewTenant(testName, tenant -> {
      provisionTenant(tenant.tenantSlug);
      createTenantMetadata(tenant);

      underTest.inviteUser(user3);

      List<SsoUser> allUsers = ssoUserService.getAll();
      assertThat(allUsers).hasSize(1);
      assertThat(MtiqUserDTO.ssoUserToMtiqUser(allUsers.get(0))).usingRecursiveComparison().isNotEqualTo(user1);
      assertThat(MtiqUserDTO.ssoUserToMtiqUser(allUsers.get(0))).usingRecursiveComparison().isNotEqualTo(user2);
      assertThat(MtiqUserDTO.ssoUserToMtiqUser(allUsers.get(0))).usingRecursiveComparison().isEqualTo(user3);
    });
  }

  @Test
  public void test_inviteFailsIfTenantMetadataMissing() {
    MtiqUserDTO user = createMtiqUser("foo");

    TenantTestHelper.testAsNewTenant(testName, tenant -> {
      provisionTenant(tenant.tenantSlug);

      assertThatThrownBy(() -> underTest.inviteUser(user)).isInstanceOf(RuntimeException.class)
          .hasMessageContaining("Tenant metadata not found");

      List<SsoUser> allUsers = ssoUserService.getAll();
      assertThat(allUsers).hasSize(0);
    });
  }

  @Test
  public void test_canDeleteUser() {
    MtiqUserDTO user1 = createMtiqUser("foo");
    MtiqUserDTO user2 = createMtiqUser("bar");

    TenantTestHelper.testAsNewTenant(testName, tenant -> {
      provisionTenant(tenant.tenantSlug);
      TenantMetadata tenantMetadata = createTenantMetadata(tenant);

      when(currentUser.getUsername()).thenReturn("random@email.com");
      ssoUserService.upsertByUsername(user1);
      ssoUserService.upsertByUsername(user2);

      underTest.deleteByUsername(user2.getEmail());

      verify(auth0ManagementService).deleteUser(user2.getEmail(), tenantMetadata.getConnectionId());
      assertThat(ssoUserService.getByUsername(user2.getEmail())).isNull();
    });
  }

  @Test
  public void test_deletionFailsIfUserDoesNotExist() {
    MtiqUserDTO user1 = createMtiqUser("foo");
    MtiqUserDTO user2 = createMtiqUser("bar");

    TenantTestHelper.testAsNewTenant(testName, tenant -> {
      provisionTenant(tenant.tenantSlug);
      TenantMetadata tenantMetadata = createTenantMetadata(tenant);

      when(currentUser.getUsername()).thenReturn("admin@email.com");
      ssoUserService.upsertByUsername(user1);
      ssoUserService.upsertByUsername(user2);

      underTest.deleteByUsername("random@email.com");

      verify(auth0ManagementService).deleteUser("random@email.com", tenantMetadata.getConnectionId());
      assertThat(ssoUserService.getByUsername("random@email.com")).isNull();
    });
  }

  @Test
  public void test_deletionFailsIfTenantMetadataDoesNotExist() {
    MtiqUserDTO user1 = createMtiqUser("foo");
    MtiqUserDTO user2 = createMtiqUser("bar");

    TenantTestHelper.testAsNewTenant(testName, tenant -> {
      provisionTenant(tenant.tenantSlug);
      ssoUserService.upsertByUsername(user1);
      ssoUserService.upsertByUsername(user2);

      assertThatThrownBy(() -> underTest.deleteByUsername("random@email.com"))
          .isInstanceOf(RuntimeException.class);
    });
  }

  @Test
  public void test_deletionFailsIfUserIsLoggedInUser() {
    when(currentUser.getUsername()).thenReturn("random@email.com");
    TenantTestHelper.testAsNewTenant(testName, tenant -> {
      provisionTenant(tenant.tenantSlug);

      assertThatThrownBy(() -> underTest.deleteByUsername("random@email.com"))
          .isInstanceOf(BadRequestException.class);
    });
  }

  private TenantMetadata createTenantMetadata(final Tenant tenant) {
    TenantMetadata tenantMetadata = new TenantMetadata();
    String tempName = tenant.tenantSlug.substring(tenant.tenantSlug.length() - 20);
    tenantMetadata.setApplicationId("appId-" + tempName);
    tenantMetadata.setApplicationName("appName-" + tempName);
    tenantMetadata.setConnectionId("conId-" + tempName);
    tenantMetadata.setConnectionName("conName-" + tempName);
    tenantMetadataDAO.insert(tenantMetadata);
    return tenantMetadata;
  }

  private MtiqUserDTO createMtiqUser(String first) {
    MtiqUserDTO user = new MtiqUserDTO();
    String email = first + "@example.com";

    user.setFirstName(first);
    user.setLastName(testName.getMethodName());
    user.setEmail(email);
    user.setUsername(email);
    return user;
  }
}
