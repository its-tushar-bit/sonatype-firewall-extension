/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.users;

import java.util.List;

import com.sonatype.insight.brain.auth.MultiTenantAuth0ManagementService;
import com.sonatype.insight.brain.dataaccess.security.SamlUserDAO;
import com.sonatype.insight.brain.db.dao.TenantMetadataDAO;
import com.sonatype.insight.brain.model.security.SamlUser;
import com.sonatype.insight.brain.model.security.TenantMetadata;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.service.AbstractMultiTenantBrainServiceTest;
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
    extends AbstractMultiTenantBrainServiceTest
{
  private final SamlUserDAO samlUserDAO = new SamlUserDAO();

  private final TenantMetadataDAO tenantMetadataDAO = new TenantMetadataDAO();

  private final MultiTenantAuth0ManagementService auth0ManagementService =
      Mockito.mock(MultiTenantAuth0ManagementService.class);

  private final CurrentUser currentUser = Mockito.mock(CurrentUser.class);

  private final DefaultWebSessionManager webSessionManager = Mockito.mock(DefaultWebSessionManager.class);

  private final SessionDAO sessionDAO = Mockito.mock(SessionDAO.class);

  private MtiqUserService underTest;

  @Before
  public void setUp() throws Exception {
    underTest = new MultiTenantUserService(webSessionManager, sessionDAO, samlUserDAO, tenantMetadataDAO,
        auth0ManagementService, currentUser);
  }

  @Test
  public void test_canListUsers() {
    MtiqUserDTO user1 = createMtiqUser("foo1");
    MtiqUserDTO user2 = createMtiqUser("foo2");

    samlUserDAO.insert(MtiqUserDTO.samlUserFromMtiqUser(user1));
    samlUserDAO.insert(MtiqUserDTO.samlUserFromMtiqUser(user2));

    assertThat(underTest.getAllUsers()).hasSize(2);
  }

  @Test
  public void test_listUsersIsOrderedByEmail() {
    MtiqUserDTO user1 = createMtiqUser("zebra");
    MtiqUserDTO user2 = createMtiqUser("ant");
    MtiqUserDTO user3 = createMtiqUser("monkey");
    MtiqUserDTO user4 = createMtiqUser("horse");

    samlUserDAO.insert(MtiqUserDTO.samlUserFromMtiqUser(user1));
    samlUserDAO.insert(MtiqUserDTO.samlUserFromMtiqUser(user2));
    samlUserDAO.insert(MtiqUserDTO.samlUserFromMtiqUser(user3));
    samlUserDAO.insert(MtiqUserDTO.samlUserFromMtiqUser(user4));

    List<MtiqUserDTO> allUsers = underTest.getAllUsers();
    assertThat(allUsers).containsSequence(user2, user4, user3, user1);
  }

  @Test
  public void test_canInviteAUser() {
    MtiqUserDTO user = createMtiqUser("foo");

    TenantTestHelper.testAsNewTenant(testName, tenant -> {
      provisionTenant(tenant.tenantSlug);
      createTenantMetadata(tenant);

      assertThat(samlUserDAO.getAll()).hasSize(0);

      underTest.inviteUser(user);

      List<SamlUser> allUsers = samlUserDAO.getAll();
      assertThat(allUsers).hasSize(1);
      assertThat(MtiqUserDTO.samlUserToMtiqUser(allUsers.get(0))).usingRecursiveComparison().isEqualTo(user);
    });

    List<SamlUser> allUsers = samlUserDAO.getAll();
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

      List<SamlUser> allUsers = samlUserDAO.getAll();
      assertThat(allUsers).hasSize(2);
    });

    TenantTestHelper.testAsNewTenant(testName, tenant -> {
      provisionTenant(tenant.tenantSlug);
      createTenantMetadata(tenant);

      underTest.inviteUser(user3);

      List<SamlUser> allUsers = samlUserDAO.getAll();
      assertThat(allUsers).hasSize(1);
      assertThat(MtiqUserDTO.samlUserToMtiqUser(allUsers.get(0))).usingRecursiveComparison().isNotEqualTo(user1);
      assertThat(MtiqUserDTO.samlUserToMtiqUser(allUsers.get(0))).usingRecursiveComparison().isNotEqualTo(user2);
      assertThat(MtiqUserDTO.samlUserToMtiqUser(allUsers.get(0))).usingRecursiveComparison().isEqualTo(user3);
    });
  }

  @Test
  public void test_inviteFailsIfTenantMetadataMissing() {
    MtiqUserDTO user = createMtiqUser("foo");

    TenantTestHelper.testAsNewTenant(testName, tenant -> {
      provisionTenant(tenant.tenantSlug);

      assertThatThrownBy(() -> underTest.inviteUser(user)).isInstanceOf(RuntimeException.class)
          .hasMessageContaining("Tenant metadata not found");

      List<SamlUser> allUsers = samlUserDAO.getAll();
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
      samlUserDAO.insert(MtiqUserDTO.samlUserFromMtiqUser(user1));
      samlUserDAO.insert(MtiqUserDTO.samlUserFromMtiqUser(user2));

      underTest.deleteByUsername(user2.getEmail());

      verify(auth0ManagementService).deleteUser(user2.getEmail(), tenantMetadata.getConnectionId());
      assertThat(samlUserDAO.getByUsername(user2.getEmail())).isNull();
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
      samlUserDAO.insert(MtiqUserDTO.samlUserFromMtiqUser(user1));
      samlUserDAO.insert(MtiqUserDTO.samlUserFromMtiqUser(user2));

      underTest.deleteByUsername("random@email.com");

      verify(auth0ManagementService).deleteUser("random@email.com", tenantMetadata.getConnectionId());
      assertThat(samlUserDAO.getByUsername("random@email.com")).isNull();
    });
  }

  @Test
  public void test_deletionFailsIfTenantMetadataDoesNotExist() {
    MtiqUserDTO user1 = createMtiqUser("foo");
    MtiqUserDTO user2 = createMtiqUser("bar");

    TenantTestHelper.testAsNewTenant(testName, tenant -> {
      provisionTenant(tenant.tenantSlug);
      samlUserDAO.insert(MtiqUserDTO.samlUserFromMtiqUser(user1));
      samlUserDAO.insert(MtiqUserDTO.samlUserFromMtiqUser(user2));

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
