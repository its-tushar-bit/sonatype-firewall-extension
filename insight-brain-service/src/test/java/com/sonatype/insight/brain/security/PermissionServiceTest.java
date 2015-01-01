/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.test.InjectedTest;

import org.apache.shiro.subject.Subject;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PermissionServiceTest
    extends InjectedTest
{
  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  @Inject
  PermissionService service;

  private Subject subject = mock(Subject.class);

  private UserPrincipal principal = mock(UserPrincipal.class);

  @Test
  public void validateAdminPermission_GlobalContext_AuthenticatedAsAdmin() {
    prepareMocks(User.ADMIN_USERNAME);

    assertPermissions(
        service.hasPermissions(subject, IdUtils.TYPE_GLOBAL, null, Collections.singleton(Permission.ADMIN)), true,
        Permission.ADMIN);
  }

  @Test
  public void validateAdminPermission_GlobalContext_AuthenticatedAsNonAdmin() {
    prepareMocks("nonadmin");

    assertPermissions(
        service.hasPermissions(subject, IdUtils.TYPE_GLOBAL, null, Collections.singleton(Permission.ADMIN)), false,
        Permission.ADMIN);
  }

  @Test
  public void validateAdminPermission_GlobalContext_Unauthenticated() {
    prepareMocks(null);

    assertPermissions(
        service.hasPermissions(subject, IdUtils.TYPE_GLOBAL, null, Collections.singleton(Permission.ADMIN)), false,
        Permission.ADMIN);
  }

  @Test
  public void validateReadWritePermission_OrgAppContext_AuthenticatedAsAdmin() {
    prepareMocks(User.ADMIN_USERNAME);

    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());

    assertPermissions(
        service.hasPermissions(subject, IdUtils.TYPE_APPLICATION, app.getId(), EnumSet.of(Permission.WRITE,
            Permission.READ)),
        true, Permission.READ, Permission.WRITE
    );

    assertPermissions(service.hasPermissions(subject, IdUtils.TYPE_ORGANIZATION, org.getId(),
        EnumSet.of(Permission.WRITE, Permission.READ)), true, Permission.READ, Permission.WRITE);
  }

  @Test
  public void validateReadWritePermission_OrgAppContext_AuthenticatedAsNonAdmin() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    User user = tempEntity.newUser();

    prepareMocks(user.getUsername());

    assertPermissions(
        service.hasPermissions(subject, IdUtils.TYPE_APPLICATION, app.getId(),
            EnumSet.of(Permission.WRITE, Permission.READ)),
        false, Permission.READ, Permission.WRITE
    );

    grantReadPermission(app.getId(), user.getUsername());
    grantReadPermission(org.getId(), user.getUsername());
    grantWritePermission(app.getId(), user.getUsername());
    grantWritePermission(org.getId(), user.getUsername());

    assertPermissions(
        service.hasPermissions(subject, IdUtils.TYPE_APPLICATION, app.getId(),
            EnumSet.of(Permission.WRITE, Permission.READ)),
        true, Permission.READ, Permission.WRITE
    );

    assertPermissions(service.hasPermissions(subject, IdUtils.TYPE_ORGANIZATION, org.getId(),
        EnumSet.of(Permission.WRITE, Permission.READ)), true, Permission.READ, Permission.WRITE);
  }

  @Test
  public void validateReadWritePermission_OrgAppContext_Unauthenticated() {
    prepareMocks(null);

    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());

    assertPermissions(
        service.hasPermissions(subject, IdUtils.TYPE_APPLICATION, app.getId(),
            EnumSet.of(Permission.WRITE, Permission.READ)),
        false, Permission.READ, Permission.WRITE
    );

    assertPermissions(service.hasPermissions(subject, IdUtils.TYPE_ORGANIZATION, org.getId(),
        EnumSet.of(Permission.WRITE, Permission.READ)), false, Permission.READ, Permission.WRITE);
  }

  private void assertPermissions(Set<Permission> permissions, boolean match, Permission... expectedPermissions) {
    //make sure we are only seeing the perms we are looking for
    Assert.assertEquals(match ? expectedPermissions.length : 0, permissions.size());

    for (Permission expectedPermission : expectedPermissions) {
      Assert.assertEquals(match, permissions.contains(expectedPermission));
    }
  }

  private void prepareMocks(String username) {
    if (username != null) {
      when(subject.isAuthenticated()).thenReturn(true);
      when(subject.getPrincipal()).thenReturn(principal);
      when(principal.getUsername()).thenReturn(username);
    }
    else {
      when(subject.isAuthenticated()).thenReturn(false);
    }
  }

  private void grantWritePermission(String contextId, String username) {
    Role role = tempEntity.newRole(false /* global */, Permission.WRITE);
    tempEntity.newMembershipMapping(contextId, role.getId(), username);
  }

  private void grantReadPermission(String contextId, String username) {
    Role role = tempEntity.newRole(false /* global */, Permission.READ);
    tempEntity.newMembershipMapping(contextId, role.getId(), username);
  }
}
