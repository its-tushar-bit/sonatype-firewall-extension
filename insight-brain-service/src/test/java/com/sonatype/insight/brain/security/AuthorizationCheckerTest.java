/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.sonatype.insight.brain.AbstractDataTest;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.dataaccess.security.RolePermissionDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.security.AuthzFilter.Context;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AuthorizationCheckerTest
    extends AbstractDataTest
{
  private AuthorizationChecker checker;

  private MembershipMappingDAO membershipMappingDAO;

  private RoleDAO roleDAO;

  private RolePermissionDAO rolePermissionDAO;

  @Before
  public void setUp() {
    membershipMappingDAO = daoFactory.createMembershipMappingDAO();
    roleDAO = daoFactory.createRoleDAO();
    rolePermissionDAO = daoFactory.createRolePermissionDAO();

    ContextResolver contextResolver =
        new ContextResolver(daoFactory.createApplicationDAO(), daoFactory.createOrganizationDAO(),
            daoFactory.createRepositoryManagerDAO(), daoFactory.createRepositoryDAO(), daoFactory.createOwnerDAO());

    checker = new AuthorizationChecker(contextResolver, rolePermissionDAO, membershipMappingDAO);
  }

  private MembershipMapping newMembershipMapping(User user, String contextId, String roleId) {
    MembershipMapping membership = new MembershipMapping(contextId, roleId, user.getUsername(), MemberType.USER);
    membershipMappingDAO.insert(membership);
    return membership;
  }

  private MembershipMapping newGroupMapping(String groupname, String contextId, String roleId) {
    MembershipMapping membership = new MembershipMapping(contextId, roleId, groupname, MemberType.GROUP);
    membershipMappingDAO.insert(membership);
    return membership;
  }

  private UserPrincipal newPrincipal(User user, String... groups) {
    return new UserPrincipal(user.getUsername(), user.calculateDisplayName(), InternalRealm.ID,
        Sets.newHashSet(groups));
  }

  @Test
  public void testIsPermitted_SystemAdminHasConfigureSystemAccess() {
    User user = tempEntity.newUser();
    newMembershipMapping(user, MembershipMapping.GLOBAL_CONTEXT_ID, Role.SYSTEM_ADMIN_ROLE_ID);
    Collection<String> contextIds = Arrays.asList("app", "org", MembershipMapping.GLOBAL_CONTEXT_ID);

    UserPrincipal admin = newPrincipal(user);
    assertThat(checker.isPermitted(admin, Permission.CONFIGURE_SYSTEM, contextIds)).isTrue();
  }

  @Test
  public void testIsPermitted_PolicyAdminHasIqPermissions() {
    User user = tempEntity.newUser();
    newMembershipMapping(user, MembershipMapping.GLOBAL_CONTEXT_ID, Role.POLICY_ADMIN_ROLE_ID);
    Collection<String> contextIds = Arrays.asList("app", "org", MembershipMapping.GLOBAL_CONTEXT_ID);

    UserPrincipal admin = newPrincipal(user);
    assertThat(checker.isPermitted(admin, Permission.READ, contextIds)).isTrue();
    assertThat(checker.isPermitted(admin, Permission.WRITE, contextIds)).isTrue();
    assertThat(checker.isPermitted(admin, Permission.EVALUATE_APPLICATION, contextIds)).isTrue();
    assertThat(checker.isPermitted(admin, Permission.EVALUATE_COMPONENT, contextIds)).isTrue();
  }

  @Test
  public void testIsPermitted_OwnerHasReadWriteAccess() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    User user = tempEntity.newUser();
    newMembershipMapping(user, app.getId(), roleDAO.getByName("Owner").getId());
    Collection<String> contextIds = Collections.singletonList(app.getId());

    UserPrincipal owner = newPrincipal(user);
    assertThat(checker.isPermitted(owner, Permission.READ, contextIds)).isTrue();
    assertThat(checker.isPermitted(owner, Permission.WRITE, contextIds)).isTrue();
    assertThat(checker.isPermitted(owner, Permission.CONFIGURE_SYSTEM, contextIds)).isFalse();
  }

  @Test
  public void testIsPermitted_DeveloperHasReadAccess() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    User user = tempEntity.newUser();
    newMembershipMapping(user, app.getId(), roleDAO.getByName("Developer").getId());
    Collection<String> contextIds = Collections.singletonList(app.getId());

    UserPrincipal developer = newPrincipal(user);
    assertThat(checker.isPermitted(developer, Permission.READ, contextIds)).isTrue();
    assertThat(checker.isPermitted(developer, Permission.WRITE, contextIds)).isFalse();
    assertThat(checker.isPermitted(developer, Permission.CONFIGURE_SYSTEM, contextIds)).isFalse();
  }

  @Test
  public void testIsPermitted_NonMemberHasNoAccess() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    User user = tempEntity.newUser();
    Collection<String> contextIds = Arrays.asList(app.getId(), org.getId(), MembershipMapping.GLOBAL_CONTEXT_ID);

    UserPrincipal userPrincipal = newPrincipal(user);
    for (Permission perm : Permission.values()) {
      assertThat(checker.isPermitted(userPrincipal, perm, contextIds)).as(perm.toString()).isFalse();
    }
  }

  @Test
  public void testIsPermitted_AnonymousHasNoAccess() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    Collection<String> contextIds = Arrays.asList(app.getId(), org.getId(), MembershipMapping.GLOBAL_CONTEXT_ID);

    for (Permission perm : Permission.values()) {
      assertThat(checker.isPermitted(null, perm, contextIds)).as(perm.toString()).isFalse();
    }
  }

  @Test
  public void testIsPermitted_MemberHasAccessThroughGroup() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    User user = tempEntity.newUser();
    Permission globalPermission = Permission.CONFIGURE_SYSTEM;
    assertThat(globalPermission.isGlobal()).isTrue();
    Permission nonGlobalPermission = Permission.READ;
    assertThat(nonGlobalPermission.isGlobal()).isFalse();
    Role role = tempEntity.newRole(false /* global */, globalPermission, nonGlobalPermission);
    String groupName = "group";
    newGroupMapping(groupName, app.getId(), role.getId());
    Collection<String> contextIds = Collections.singletonList(app.getId());

    UserPrincipal userPrincipalNoGroups = newPrincipal(user);
    assertThat(checker.isPermitted(userPrincipalNoGroups, globalPermission, contextIds)).isFalse();
    assertThat(checker.isPermitted(userPrincipalNoGroups, nonGlobalPermission, contextIds)).isFalse();

    UserPrincipal userPrincipalWithGroup = newPrincipal(user, groupName);
    assertThat(checker.isPermitted(userPrincipalWithGroup, globalPermission, contextIds)).isTrue();
    assertThat(checker.isPermitted(userPrincipalWithGroup, nonGlobalPermission, contextIds)).isTrue();
  }

  @Test
  public void testIsPermitted_AccessInherited() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    User user = tempEntity.newUser();
    newMembershipMapping(user, org.getId(), roleDAO.getByName("Owner").getId());
    Collection<String> contextIds = Arrays.asList(app.getId(), org.getId());

    UserPrincipal owner = newPrincipal(user);
    assertThat(checker.isPermitted(owner, Permission.READ, contextIds)).isTrue();
    assertThat(checker.isPermitted(owner, Permission.WRITE, contextIds)).isTrue();
    assertThat(checker.isPermitted(owner, Permission.CONFIGURE_SYSTEM, contextIds)).isFalse();
  }

  @Test
  public void testFilter_Organizations() {
    List<Organization> entities = Arrays.asList(tempEntity.newOrganization(), tempEntity.newOrganization());
    User user = tempEntity.newUser();
    Role role = tempEntity.newRole(false, Permission.READ);
    newMembershipMapping(user, entities.get(0).getId(), role.getId());

    UserPrincipal userPrincipal = newPrincipal(user);
    assertThat(checker.filterByPermission(userPrincipal, Permission.READ, entities, Context.ORGANIZATION))
        .containsExactly(entities.get(0));
  }

  @Test
  public void testFilter_Applications() {
    Organization org = tempEntity.newOrganization();
    List<Application> entities = Arrays.asList(tempEntity.newApplication(org.getId()),
        tempEntity.newApplication(org.getId()));
    User user = tempEntity.newUser();
    Role role = tempEntity.newRole(false, Permission.READ);
    newMembershipMapping(user, entities.get(1).getId(), role.getId());

    UserPrincipal userPrincipal = newPrincipal(user);
    assertThat(checker.filterByPermission(userPrincipal, Permission.READ, entities, Context.APPLICATION))
        .containsExactly(entities.get(1));
  }

  @Test
  public void testFilter_Repositories() {
    List<Repository> entities = Arrays.asList(tempEntity.newRepository(), tempEntity.newRepository());
    User user = tempEntity.newUser();
    Role role = tempEntity.newRole(false, Permission.READ);
    newMembershipMapping(user, entities.get(0).getId(), role.getId());

    UserPrincipal userPrincipal = newPrincipal(user);
    assertThat(checker.filterByPermission(userPrincipal, Permission.READ, entities, Context.REPOSITORY))
        .containsExactly(entities.get(0));
  }

  @Test
  public void testFilter_RepositorManagers() {
    List<RepositoryManager> entities =
        Arrays.asList(tempEntity.newRepositoryManager(), tempEntity.newRepositoryManager());
    User user = tempEntity.newUser();
    Role role = tempEntity.newRole(false, Permission.READ);
    newMembershipMapping(user, entities.get(0).getId(), role.getId());

    UserPrincipal userPrincipal = newPrincipal(user);
    assertThat(checker.filterByPermission(userPrincipal, Permission.READ, entities, Context.REPOSITORY_MANAGER))
        .containsExactly(entities.get(0));
  }

  @Test
  public void testFilter_WithGroup() {
    List<Organization> entities = Arrays.asList(tempEntity.newOrganization(), tempEntity.newOrganization());
    User user = tempEntity.newUser();
    Role role = tempEntity.newRole(false, Permission.READ);
    newGroupMapping("group", entities.get(0).getId(), role.getId());

    UserPrincipal userPrincipal = newPrincipal(user, "group");
    assertThat(checker.filterByPermission(userPrincipal, Permission.READ, entities, Context.ORGANIZATION))
        .containsExactly(entities.get(0));
  }

  @Test
  public void testFilter_NonMemberHasNoAccess() {
    Collection<Organization> entities = Collections.singletonList(tempEntity.newOrganization());
    User user = tempEntity.newUser();
    UserPrincipal userPrincipal = newPrincipal(user);

    for (Permission perm : Permission.values()) {
      assertThat(checker.filterByPermission(userPrincipal, Permission.READ, entities, Context.ORGANIZATION))
          .as(perm.toString()).isEmpty();
    }
  }

  @Test
  public void testFilter_AnonymousHasNoAccess() {
    Collection<Organization> entities = Collections.singletonList(tempEntity.newOrganization());
    for (Permission perm : Permission.values()) {
      assertThat(checker.filterByPermission(null, Permission.READ, entities, Context.ORGANIZATION)).as(perm.toString())
          .isEmpty();
    }
  }

  /**
   * The authz filtering expects the {@link ContextResolver} to iterate contexts from the bottom of the hierarchy
   * upwards. This better hold true or permissions would be "inherited" into the wrong direction.
   */
  @Test
  public void testFilter_ContextsAreEvaluatedInProperOrder() {
    Organization org = tempEntity.newOrganization();
    List<Application> entities = Arrays.asList(tempEntity.newApplication(org.getId()),
        tempEntity.newApplication(org.getId()));
    User user = tempEntity.newUser();
    Role role = tempEntity.newRole(false, Permission.READ);
    newMembershipMapping(user, entities.get(0).getId(), role.getId());

    UserPrincipal userPrincipal = newPrincipal(user);
    assertThat(checker.filterByPermission(userPrincipal, Permission.READ, entities, Context.APPLICATION))
        .containsExactly(entities.get(0));
  }

  @Test
  public void testIsPermitted_GlobalPermission_GlobalContext() {
    // The user has a global permission granted via a role in global context.
    User user = tempEntity.newUser("AliBaba");
    Permission permission = Permission.CONFIGURE_SYSTEM;
    assertThat(permission.isGlobal()).isTrue();
    Role role = tempEntity.newRole(true /* global */, permission);
    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, role.getId(), user.getUsername());
    Collection<String> contextIds = Collections.singletonList(MembershipMapping.GLOBAL_CONTEXT_ID);

    UserPrincipal userPrincipal = newPrincipal(user);
    assertThat(checker.isPermitted(userPrincipal, permission, contextIds)).isTrue();

    // Verify the user name is checked case insensitive
    user.setUsername("aLIbABA");
    userPrincipal = newPrincipal(user);
    assertThat(checker.isPermitted(userPrincipal, permission, contextIds)).isTrue();
  }

  @Test
  public void testIsPermitted_GlobalPermission_NonGlobalContext() {
    // The user has a global permission granted via a role in a non-global context (context==org), which effectively
    // grants the user that global permission in global context.
    Organization org = tempEntity.newOrganization();
    User user = tempEntity.newUser("AliBaba");
    Permission permission = Permission.CONFIGURE_SYSTEM;
    assertThat(permission.isGlobal()).isTrue();
    Role role = tempEntity.newRole(false /* global */, permission);
    tempEntity.newMembershipMapping(org.getId(), role.getId(), user.getUsername());
    Collection<String> contextIds = Collections.singletonList(MembershipMapping.GLOBAL_CONTEXT_ID);

    UserPrincipal userPrincipal = newPrincipal(user);
    assertThat(checker.isPermitted(userPrincipal, permission, contextIds)).isTrue();

    // Verify the user name is checked case insensitive
    user.setUsername("aLIbABA");
    userPrincipal = newPrincipal(user);
    assertThat(checker.isPermitted(userPrincipal, permission, contextIds)).isTrue();
  }

  @Test
  public void testIsPermitted_NonGlobalPermission_GlobalContext() {
    // The user has a non-global permission granted via a role in global context.
    User user = tempEntity.newUser();
    Permission permission = Permission.EVALUATE_APPLICATION;
    assertThat(permission.isGlobal()).isFalse();
    Role role = tempEntity.newRole(true /* global */, permission);
    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, role.getId(), user.getUsername());
    Collection<String> contextIds = Collections.singletonList(MembershipMapping.GLOBAL_CONTEXT_ID);

    UserPrincipal userPrincipal = newPrincipal(user);
    assertThat(checker.isPermitted(userPrincipal, permission, contextIds)).isTrue();
  }

  @Test
  public void testIsPermitted_NonGlobalPermission_NonGlobalContext() {
    // The user has a non-global permission granted via a role in a non-global context (context==org), which should not
    // grant the user that permission in global context.
    Organization org = tempEntity.newOrganization();
    User user = tempEntity.newUser();
    Permission permission = Permission.EVALUATE_APPLICATION;
    assertThat(permission.isGlobal()).isFalse();
    Role role = tempEntity.newRole(false /* global */, permission);
    tempEntity.newMembershipMapping(org.getId(), role.getId(), user.getUsername());
    Collection<String> contextIds = Collections.singletonList(MembershipMapping.GLOBAL_CONTEXT_ID);

    UserPrincipal userPrincipal = newPrincipal(user);
    assertThat(checker.isPermitted(userPrincipal, permission, contextIds)).isFalse();
  }
}
