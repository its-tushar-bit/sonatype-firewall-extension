/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.users;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.security.SamlUserDAO;
import com.sonatype.insight.brain.model.security.SamlUser;
import com.sonatype.insight.brain.service.AbstractMultiTenantBrainServiceTest;
import com.sonatype.insight.brain.tenancy.TenantTestHelper;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
public class MultiTenantUserServiceTest
    extends AbstractMultiTenantBrainServiceTest
{
  private final SamlUserDAO samlUserDAO = new SamlUserDAO();

  private final MtiqUserService underTest = new MultiTenantUserService(samlUserDAO);

  @Test
  public void canListUsers() {
    MtiqUserDTO user1 = createMtiqUser("foo1");
    MtiqUserDTO user2 = createMtiqUser("foo2");

    samlUserDAO.insert(MtiqUserDTO.samlUserFromMtiqUser(user1));
    samlUserDAO.insert(MtiqUserDTO.samlUserFromMtiqUser(user2));

    assertThat(underTest.getAllUsers()).hasSize(2);
  }

  @Test
  public void canAddAUser() {
    MtiqUserDTO user = createMtiqUser("foo");

    TenantTestHelper.testAsNewTenant(testName, tenant -> {
      provisionTenant(tenant.tenantSlug);

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
  public void canAddMultipleUsers() {
    MtiqUserDTO user1 = createMtiqUser("foo");
    MtiqUserDTO user2 = createMtiqUser("bar");
    MtiqUserDTO user3 = createMtiqUser("baz");

    TenantTestHelper.testAsNewTenant(testName, tenant -> {
      provisionTenant(tenant.tenantSlug);

      underTest.inviteUser(user1);
      underTest.inviteUser(user2);

      List<SamlUser> allUsers = samlUserDAO.getAll();
      assertThat(allUsers).hasSize(2);
    });

    TenantTestHelper.testAsNewTenant(testName, tenant -> {
      provisionTenant(tenant.tenantSlug);

      underTest.inviteUser(user3);

      List<SamlUser> allUsers = samlUserDAO.getAll();
      assertThat(allUsers).hasSize(1);
      assertThat(MtiqUserDTO.samlUserToMtiqUser(allUsers.get(0))).usingRecursiveComparison().isNotEqualTo(user1);
      assertThat(MtiqUserDTO.samlUserToMtiqUser(allUsers.get(0))).usingRecursiveComparison().isNotEqualTo(user2);
      assertThat(MtiqUserDTO.samlUserToMtiqUser(allUsers.get(0))).usingRecursiveComparison().isEqualTo(user3);
    });
  }

  @Test
  public void canDeleteUser() {
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
