/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.users;

import java.util.List;

import com.sonatype.insight.brain.auth.MultiTenantAuth0ApiSupplier;
import com.sonatype.insight.brain.auth.MultiTenantAuth0ManagementService;
import com.sonatype.insight.brain.dataaccess.security.SamlUserDAO;
import com.sonatype.insight.brain.db.dao.TenantMetadataDAO;
import com.sonatype.insight.brain.model.security.SamlUser;
import com.sonatype.insight.brain.model.security.TenantMetadata;
import com.sonatype.insight.brain.service.AbstractMultiTenantBrainServiceTest;
import com.sonatype.insight.brain.service.Auth0Config;
import com.sonatype.insight.brain.service.MultiTenantInsightConfig;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantTestHelper;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class MultiTenantUserServiceTest
    extends AbstractMultiTenantBrainServiceTest
{
  private final SamlUserDAO samlUserDAO = new SamlUserDAO();

  private final TenantMetadataDAO tenantMetadataDAO = new TenantMetadataDAO();

  private final TestMultiTenantAuth0ManagementService auth0ManagementService
      = new TestMultiTenantAuth0ManagementService();

  private final MtiqUserService underTest = new MultiTenantUserService(samlUserDAO, tenantMetadataDAO,
      auth0ManagementService);

  @Test
  public void test_canListUsers() {
    MtiqUserDTO user1 = createMtiqUser("foo1");
    MtiqUserDTO user2 = createMtiqUser("foo2");

    samlUserDAO.insert(MtiqUserDTO.samlUserFromMtiqUser(user1));
    samlUserDAO.insert(MtiqUserDTO.samlUserFromMtiqUser(user2));

    assertThat(underTest.getAllUsers()).hasSize(2);
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
      samlUserDAO.insert(MtiqUserDTO.samlUserFromMtiqUser(user1));
      samlUserDAO.insert(MtiqUserDTO.samlUserFromMtiqUser(user2));

      underTest.deleteByUser(user2);

      assertThat(samlUserDAO.getByUsername(user2.getFirstName() + user2.getLastName())).isNull();
    });
  }

  private void createTenantMetadata(final Tenant tenant) {
    TenantMetadata tenantMetadata = new TenantMetadata();
    tenantMetadata.setApplicationId("appId-" + tenant.tenantSlug);
    tenantMetadata.setApplicationName("appName-" + tenant.tenantSlug);
    tenantMetadata.setConnectionId("conId-" + tenant.tenantSlug);
    tenantMetadata.setConnectionName("conName-" + tenant.tenantSlug);
    tenantMetadataDAO.insert(tenantMetadata);
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

  private class TestMultiTenantAuth0ManagementService
      extends MultiTenantAuth0ManagementService
  {
    public TestMultiTenantAuth0ManagementService() {
      super(new TestMultiTenantInsightConfig(), new MultiTenantAuth0ApiSupplier());
    }

    @Override
    public void createOrUpdateUser(
        final String email,
        final String firstName,
        final String lastName,
        final String connectionName, final String connectionId)
    {
      //no-op
    }
  }

  private class TestMultiTenantInsightConfig
      extends MultiTenantInsightConfig
  {
    @Override
    public String getAuth0Domain() {
      return "foodomain";
    }

    @Override
    public Auth0Config getAuth0Config() {
      Auth0Config auth0Config = new Auth0Config();
      auth0Config.setClientId("clientId");
      auth0Config.setDomain("domain");
      auth0Config.setClientSecret("clientSecret");
      return auth0Config;
    }
  }
}
