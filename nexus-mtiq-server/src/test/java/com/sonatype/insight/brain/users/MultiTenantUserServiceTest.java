/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.users;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.sonatype.insight.brain.auth.MultiTenantAuth0ApiSupplier;
import com.sonatype.insight.brain.auth.MultiTenantAuth0ManagementService;
import com.sonatype.insight.brain.dataaccess.security.SamlUserDAO;
import com.sonatype.insight.brain.db.dao.TenantMetadataDAO;
import com.sonatype.insight.brain.model.security.SamlUser;
import com.sonatype.insight.brain.model.security.TenantMetadata;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.service.AbstractMultiTenantBrainServiceTest;
import com.sonatype.insight.brain.service.Auth0Config;
import com.sonatype.insight.brain.service.MultiTenantInsightConfig;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantTestHelper;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

public class MultiTenantUserServiceTest
    extends AbstractMultiTenantBrainServiceTest
{
  private final SamlUserDAO samlUserDAO = new SamlUserDAO();

  private final TenantMetadataDAO tenantMetadataDAO = new TenantMetadataDAO();

  private TestMultiTenantAuth0ManagementService auth0ManagementService;

  private final CurrentUser currentUser = Mockito.mock(CurrentUser.class);

  private MtiqUserService underTest;

  @Before
  public void setUp() throws Exception {
    auth0ManagementService = new TestMultiTenantAuth0ManagementService();
    underTest = new MultiTenantUserService(samlUserDAO, tenantMetadataDAO,
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

      samlUserDAO.insert(MtiqUserDTO.samlUserFromMtiqUser(user1));
      samlUserDAO.insert(MtiqUserDTO.samlUserFromMtiqUser(user2));

      underTest.deleteByUsername(user2.getEmail());

      assertThat(samlUserDAO.getByUsername(user2.getEmail())).isNull();
      auth0ManagementService.contains(user2.getEmail(), tenantMetadata.getConnectionId());
    });
  }

  @Test
  public void test_deletionFailsIfUserDoesNotExist() {
    MtiqUserDTO user1 = createMtiqUser("foo");
    MtiqUserDTO user2 = createMtiqUser("bar");

    TenantTestHelper.testAsNewTenant(testName, tenant -> {
      provisionTenant(tenant.tenantSlug);
      TenantMetadata tenantMetadata = createTenantMetadata(tenant);
      samlUserDAO.insert(MtiqUserDTO.samlUserFromMtiqUser(user1));
      samlUserDAO.insert(MtiqUserDTO.samlUserFromMtiqUser(user2));

      underTest.deleteByUsername("random@email.com");

      assertThat(samlUserDAO.getByUsername("random@email.com")).isNull();
      auth0ManagementService.contains("random@email.com", tenantMetadata.getConnectionId());
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

  private class VerificationEntity
  {
    String email;

    String identifier;

    public VerificationEntity(
        final String email,
        final String identifier)
    {
      this.email = email;
      this.identifier = identifier;
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      VerificationEntity that = (VerificationEntity) o;
      return Objects.equals(email, that.email) && Objects.equals(identifier, that.identifier);
    }

    @Override
    public int hashCode() {
      return Objects.hash(email, identifier);
    }
  }

  private class TestMultiTenantAuth0ManagementService
      extends MultiTenantAuth0ManagementService
  {
    private final List<VerificationEntity> entities = new ArrayList<>();

    public TestMultiTenantAuth0ManagementService() {
      super(new TestMultiTenantInsightConfig(), new MultiTenantAuth0ApiSupplier());
    }

    @Override
    public void createOrUpdateUser(
        final String email,
        final String firstName,
        final String lastName,
        final String connectionName,
        final String applicationId,
        final String connectionId)
    {
      entities.add(new VerificationEntity(email, applicationId));
    }

    @Override
    public void deleteUser(final String email, final String id) {
      entities.add(new VerificationEntity(email, id));
    }

    public void contains(final String email, final String id) {
      assertThat(entities).contains(new VerificationEntity(email, id));
    }

    public void doesNotContain(final String email, final String id) {
      assertThat(entities).doesNotContain(new VerificationEntity(email, id));
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
