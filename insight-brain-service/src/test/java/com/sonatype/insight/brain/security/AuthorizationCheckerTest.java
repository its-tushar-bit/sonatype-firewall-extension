/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.AbstractDataTest;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.dataaccess.security.RolePermissionDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.security.UserPrincipal;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static com.sonatype.insight.brain.security.AuthzContext.Key;

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

    AuthorizationChecker authzChecker =
        new AuthorizationChecker(daoFactory.createApplicationDAO(), daoFactory.createOwnerDAO());

    checker = authzChecker;
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

  private Map<Key, Object> appContext(Application app) {
    Map<Key, Object> ctx = new HashMap<>();
    ctx.put(Key.APPLICATION_ID, app.getId());
    return ctx;
  }

  private Map<Key, Object> orgContext(Organization org) {
    Map<Key, Object> ctx = new HashMap<>();
    ctx.put(Key.ORGANIZATION_ID, org.getId());
    return ctx;
  }

  private Map<Key, Object> repoContext(Repository repo) {
    Map<Key, Object> ctx = new HashMap<>();
    ctx.put(Key.REPOSITORY_ID, repo.getId());
    return ctx;
  }

  private Map<Key, Object> emptyContext() {
    return new HashMap<>();
  }

  @Test
  public void testIsPermitted_SystemAdminHasConfigureSystemAccess() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    User user = tempEntity.newUser();
    newMembershipMapping(user, MembershipMapping.GLOBAL_CONTEXT_ID, Role.SYSTEM_ADMIN_ROLE_ID);

    UserPrincipal admin = newPrincipal(user);
    assertThat(checker.isPermitted(admin, Permission.CONFIGURE_SYSTEM, appContext(app))).isTrue();
  }

  @Test
  public void testIsPermitted_PolicyAdminHasIqPermissions() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    User user = tempEntity.newUser();
    newMembershipMapping(user, MembershipMapping.GLOBAL_CONTEXT_ID, Role.POLICY_ADMIN_ROLE_ID);

    UserPrincipal admin = newPrincipal(user);
    assertThat(checker.isPermitted(admin, Permission.READ, appContext(app))).isTrue();
    assertThat(checker.isPermitted(admin, Permission.WRITE, appContext(app))).isTrue();
    assertThat(checker.isPermitted(admin, Permission.EVALUATE_APPLICATION, appContext(app))).isTrue();
    assertThat(checker.isPermitted(admin, Permission.EVALUATE_COMPONENT, appContext(app))).isTrue();
  }

  @Test
  public void testIsPermitted_OwnerHasReadWriteAccess() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    User user = tempEntity.newUser();
    newMembershipMapping(user, app.getId(), roleDAO.getByName("Owner").getId());

    UserPrincipal owner = newPrincipal(user);
    assertThat(checker.isPermitted(owner, Permission.READ, appContext(app))).isTrue();
    assertThat(checker.isPermitted(owner, Permission.WRITE, appContext(app))).isTrue();
    assertThat(checker.isPermitted(owner, Permission.CONFIGURE_SYSTEM, appContext(app))).isFalse();
  }

  @Test
  public void testIsPermitted_DeveloperHasReadAccess() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    User user = tempEntity.newUser();
    newMembershipMapping(user, app.getId(), roleDAO.getByName("Developer").getId());

    UserPrincipal developer = newPrincipal(user);
    assertThat(checker.isPermitted(developer, Permission.READ, appContext(app))).isTrue();
    assertThat(checker.isPermitted(developer, Permission.WRITE, appContext(app))).isFalse();
    assertThat(checker.isPermitted(developer, Permission.CONFIGURE_SYSTEM, appContext(app))).isFalse();
  }

  @Test
  public void testIsPermitted_NonMemberHasNoAccess() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());

    UserPrincipal userPrincipal = newPrincipal(tempEntity.newUser());
    for (Permission perm : Permission.values()) {
      assertThat(checker.isPermitted(userPrincipal, perm, appContext(app))).as(perm.toString()).isFalse();
    }
  }

  @Test
  public void testIsPermitted_AnonymousHasNoAccess() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());

    for (Permission perm : Permission.values()) {
      assertThat(checker.isPermitted(null, perm, appContext(app))).as(perm.toString()).isFalse();
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

    UserPrincipal userPrincipalNoGroups = newPrincipal(user);
    assertThat(checker.isPermitted(userPrincipalNoGroups, globalPermission, appContext(app))).isFalse();
    assertThat(checker.isPermitted(userPrincipalNoGroups, nonGlobalPermission, appContext(app))).isFalse();

    UserPrincipal userPrincipalWithGroup = newPrincipal(user, groupName);
    assertThat(checker.isPermitted(userPrincipalWithGroup, globalPermission, appContext(app))).isTrue();
    assertThat(checker.isPermitted(userPrincipalWithGroup, nonGlobalPermission, appContext(app))).isTrue();
  }

  @Test
  public void testIsPermitted_AccessInherited() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    User user = tempEntity.newUser();
    newMembershipMapping(user, org.getId(), roleDAO.getByName("Owner").getId());

    UserPrincipal owner = newPrincipal(user);
    assertThat(checker.isPermitted(owner, Permission.READ, appContext(app))).isTrue();
    assertThat(checker.isPermitted(owner, Permission.WRITE, appContext(app))).isTrue();
    assertThat(checker.isPermitted(owner, Permission.CONFIGURE_SYSTEM, appContext(app))).isFalse();
  }

  @Test
  public void testFilter_Organizations() {
    List<Organization> entities = Arrays.asList(tempEntity.newOrganization(), tempEntity.newOrganization());
    User user = tempEntity.newUser();
    Role role = tempEntity.newRole(false, Permission.READ);
    newMembershipMapping(user, entities.get(0).getId(), role.getId());

    UserPrincipal userPrincipal = newPrincipal(user);
    assertThat(checker.filterByPermission(userPrincipal, Permission.READ, entities))
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
    assertThat(checker.filterByPermission(userPrincipal, Permission.READ, entities))
        .containsExactly(entities.get(1));
  }

  @Test
  public void testFilter_Repositories() {
    List<Repository> entities = Arrays.asList(tempEntity.newRepository(), tempEntity.newRepository());
    User user = tempEntity.newUser();
    Role role = tempEntity.newRole(false, Permission.READ);
    newMembershipMapping(user, entities.get(0).getId(), role.getId());

    UserPrincipal userPrincipal = newPrincipal(user);
    assertThat(checker.filterByPermission(userPrincipal, Permission.READ, entities))
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
    assertThat(checker.filterByPermission(userPrincipal, Permission.READ, entities))
        .containsExactly(entities.get(0));
  }

  @Test
  public void testFilter_WithGroup() {
    List<Organization> entities = Arrays.asList(tempEntity.newOrganization(), tempEntity.newOrganization());
    User user = tempEntity.newUser();
    Role role = tempEntity.newRole(false, Permission.READ);
    newGroupMapping("group", entities.get(0).getId(), role.getId());

    UserPrincipal userPrincipal = newPrincipal(user, "group");
    assertThat(checker.filterByPermission(userPrincipal, Permission.READ, entities))
        .containsExactly(entities.get(0));
  }

  @Test
  public void testFilter_NonMemberHasNoAccess() {
    Collection<Organization> entities = Collections.singletonList(tempEntity.newOrganization());
    User user = tempEntity.newUser();
    UserPrincipal userPrincipal = newPrincipal(user);

    for (Permission perm : Permission.values()) {
      assertThat(checker.filterByPermission(userPrincipal, Permission.READ, entities))
          .as(perm.toString())
          .isEmpty();
    }
  }

  @Test
  public void testFilter_AnonymousHasNoAccess() {
    Collection<Organization> entities = Collections.singletonList(tempEntity.newOrganization());
    for (Permission perm : Permission.values()) {
      assertThat(checker.filterByPermission(null, Permission.READ, entities)).as(perm.toString())
          .isEmpty();
    }
  }

  /**
   * The authz filtering expects the {@code ContextResolver} to iterate contexts from the bottom of the hierarchy
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
    assertThat(checker.filterByPermission(userPrincipal, Permission.READ, entities))
        .containsExactly(entities.get(0));
  }

  @Test
  public void testFilter_ApplicationsWithContext() {
    Organization org = tempEntity.newOrganization();
    List<Application> entities = Arrays.asList(tempEntity.newApplication(org.getId()),
        tempEntity.newApplication(org.getId()));
    User user = tempEntity.newUser();
    Role role = tempEntity.newRole(false, Permission.READ);
    newMembershipMapping(user, entities.get(1).getId(), role.getId());

    UserPrincipal userPrincipal = newPrincipal(user);
    assertThat(checker.filterByPermission(userPrincipal, Permission.READ, entities, AuthzFilter.Context.APPLICATION))
        .containsExactly(entities.get(1));
  }

  @Test
  public void testFilter_OrganizationsWithContext() {
    List<Organization> entities = Arrays.asList(tempEntity.newOrganization(), tempEntity.newOrganization());
    User user = tempEntity.newUser();
    Role role = tempEntity.newRole(false, Permission.READ);
    newMembershipMapping(user, entities.get(0).getId(), role.getId());

    UserPrincipal userPrincipal = newPrincipal(user);
    assertThat(
        checker.filterByPermission(userPrincipal, Permission.READ, entities, AuthzFilter.Context.ORGANIZATION))
            .containsExactly(entities.get(0));
  }

  @Test
  public void testFilter_RepositoriesWithContext() {
    List<Repository> entities = Arrays.asList(tempEntity.newRepository(), tempEntity.newRepository());
    User user = tempEntity.newUser();
    Role role = tempEntity.newRole(false, Permission.READ);
    newMembershipMapping(user, entities.get(0).getId(), role.getId());

    UserPrincipal userPrincipal = newPrincipal(user);
    assertThat(checker.filterByPermission(userPrincipal, Permission.READ, entities, AuthzFilter.Context.REPOSITORY))
        .containsExactly(entities.get(0));
  }

  @Test
  public void testFilter_RepositoryManagersWithContext() {
    List<RepositoryManager> entities =
        Arrays.asList(tempEntity.newRepositoryManager(), tempEntity.newRepositoryManager());
    User user = tempEntity.newUser();
    Role role = tempEntity.newRole(false, Permission.READ);
    newMembershipMapping(user, entities.get(0).getId(), role.getId());

    UserPrincipal userPrincipal = newPrincipal(user);
    assertThat(checker.filterByPermission(userPrincipal, Permission.READ, entities,
        AuthzFilter.Context.REPOSITORY_MANAGER)).containsExactly(entities.get(0));
  }

  @Test
  public void testFilter_ApplicationOrOrganizationWithContext() {
    // Mixed list of apps and orgs — uses generic owner_ancestor view
    Organization org1 = tempEntity.newOrganization();
    Organization org2 = tempEntity.newOrganization();
    Application app1 = tempEntity.newApplication(org1.getId());
    Application app2 = tempEntity.newApplication(org2.getId());

    User user = tempEntity.newUser();
    Role role = tempEntity.newRole(false, Permission.READ);
    // Grant permission on org1 — should inherit to app1 but not org2/app2
    newMembershipMapping(user, org1.getId(), role.getId());

    List<Owner> entities = Arrays.asList(org1, org2, app1, app2);
    UserPrincipal userPrincipal = newPrincipal(user);
    assertThat(checker.filterByPermission(userPrincipal, Permission.READ, entities,
        AuthzFilter.Context.APPLICATION_OR_ORGANIZATION)).containsExactlyInAnyOrder(org1, app1);
  }

  @Test
  public void testIsPermitted_GlobalPermission_GlobalContext() {
    // The user has a global permission granted via a role in global context.
    User user = tempEntity.newUser("AliBaba");
    Permission permission = Permission.CONFIGURE_SYSTEM;
    assertThat(permission.isGlobal()).isTrue();
    Role role = tempEntity.newRole(true /* global */, permission);
    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, role.getId(), user.getUsername());

    UserPrincipal userPrincipal = newPrincipal(user);
    assertThat(checker.isPermitted(userPrincipal, permission, emptyContext())).isTrue();

    // Verify the user name is checked case insensitive
    user.setUsername("aLIbABA");
    userPrincipal = newPrincipal(user);
    assertThat(checker.isPermitted(userPrincipal, permission, emptyContext())).isTrue();
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

    UserPrincipal userPrincipal = newPrincipal(user);
    assertThat(checker.isPermitted(userPrincipal, permission, emptyContext())).isTrue();

    // Verify the user name is checked case insensitive
    user.setUsername("aLIbABA");
    userPrincipal = newPrincipal(user);
    assertThat(checker.isPermitted(userPrincipal, permission, emptyContext())).isTrue();
  }

  @Test
  public void testIsPermitted_NonGlobalPermission_GlobalContext() {
    // The user has a non-global permission granted via a role in global context.
    User user = tempEntity.newUser();
    Permission permission = Permission.EVALUATE_APPLICATION;
    assertThat(permission.isGlobal()).isFalse();
    Role role = tempEntity.newRole(true /* global */, permission);
    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, role.getId(), user.getUsername());

    UserPrincipal userPrincipal = newPrincipal(user);
    assertThat(checker.isPermitted(userPrincipal, permission, emptyContext())).isTrue();
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

    UserPrincipal userPrincipal = newPrincipal(user);
    assertThat(checker.isPermitted(userPrincipal, permission, emptyContext())).isFalse();
  }

  @Test
  public void testIsPermitted_WithApplicationOwnerContext() {
    // APPLICATION_OWNER is used when creating an app - resolves to the parent organization
    Organization org = tempEntity.newOrganization();
    User user = tempEntity.newUser();
    Role role = tempEntity.newRole(false, Permission.READ);
    newMembershipMapping(user, org.getId(), role.getId());

    // Create a new app with no ID (as it would be before persistence)
    Application app = new Application("test-app", "test-app", org.getId());
    app.setId(null); // Ensure no ID

    Map<Key, Object> ctx = new HashMap<>();
    ctx.put(Key.APPLICATION_OWNER, app);
    UserPrincipal userPrincipal = newPrincipal(user);
    assertThat(checker.isPermitted(userPrincipal, Permission.READ, ctx)).isTrue();
  }

  @Test
  public void testIsPermitted_WithApplicationOwnerContext_NullParent() {
    // When APPLICATION_OWNER has no parent org, should fall back to ROOT_ORGANIZATION_ID
    User user = tempEntity.newUser();
    Role role = tempEntity.newRole(false, Permission.ADD_APPLICATION);
    newMembershipMapping(user, Organization.ROOT_ORGANIZATION_ID, role.getId());

    Application app = new Application("test-app", "test-app", null);
    app.setId(null);

    Map<Key, Object> ctx = new HashMap<>();
    ctx.put(Key.APPLICATION_OWNER, app);
    UserPrincipal userPrincipal = newPrincipal(user);
    assertThat(checker.isPermitted(userPrincipal, Permission.ADD_APPLICATION, ctx)).isTrue();
  }

  @Test
  public void testIsPermitted_WithOrganizationOwnerContext() {
    // ORGANIZATION_OWNER is used when creating an org - resolves to the parent organization
    Organization parentOrg = tempEntity.newOrganization();
    User user = tempEntity.newUser();
    Role role = tempEntity.newRole(false, Permission.READ);
    newMembershipMapping(user, parentOrg.getId(), role.getId());

    // Create a new org with parent but no ID (as it would be before persistence)
    Organization org = new Organization("test-org");
    org.setParentOrganizationId(parentOrg.getId());
    org.setId(null); // Ensure no ID

    Map<Key, Object> ctx = new HashMap<>();
    ctx.put(Key.ORGANIZATION_OWNER, org);
    UserPrincipal userPrincipal = newPrincipal(user);
    assertThat(checker.isPermitted(userPrincipal, Permission.READ, ctx)).isTrue();
  }

  @Test
  public void testIsPermitted_WithRepositoryManagerContext() {
    RepositoryManager rm = tempEntity.newRepositoryManager();
    User user = tempEntity.newUser();
    Role role = tempEntity.newRole(false, Permission.READ);
    newMembershipMapping(user, rm.getId(), role.getId());

    Map<Key, Object> ctx = new HashMap<>();
    ctx.put(Key.REPOSITORY_MANAGER_ID, rm.getId());
    UserPrincipal userPrincipal = newPrincipal(user);
    assertThat(checker.isPermitted(userPrincipal, Permission.READ, ctx)).isTrue();
  }

  @Test
  public void testIsPermitted_WithRepositoryContext() {
    Repository repo = tempEntity.newRepository();
    User user = tempEntity.newUser();
    Role role = tempEntity.newRole(false, Permission.READ);
    newMembershipMapping(user, repo.getId(), role.getId());

    UserPrincipal userPrincipal = newPrincipal(user);
    assertThat(checker.isPermitted(userPrincipal, Permission.READ, repoContext(repo))).isTrue();
  }

  @Test
  public void testIsPermitted_WithApplicationPublicId() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    User user = tempEntity.newUser();
    Role role = tempEntity.newRole(false, Permission.READ);
    newMembershipMapping(user, app.getId(), role.getId());

    Map<Key, Object> ctx = new HashMap<>();
    ctx.put(Key.APPLICATION_PUBLIC_ID, app.getPublicId());
    UserPrincipal userPrincipal = newPrincipal(user);
    assertThat(checker.isPermitted(userPrincipal, Permission.READ, ctx)).isTrue();
  }

  @Test
  public void testIsPermitted_WithTypeAndId_Application() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    User user = tempEntity.newUser();
    Role role = tempEntity.newRole(false, Permission.READ);
    newMembershipMapping(user, app.getId(), role.getId());

    Map<Key, Object> ctx = new HashMap<>();
    ctx.put(Key.ID, app.getPublicId());
    ctx.put(Key.TYPE, OwnerType.APPLICATION);
    UserPrincipal userPrincipal = newPrincipal(user);
    assertThat(checker.isPermitted(userPrincipal, Permission.READ, ctx)).isTrue();
  }

  @Test
  public void testIsPermitted_WithTypeAndId_Organization() {
    Organization org = tempEntity.newOrganization();
    User user = tempEntity.newUser();
    Role role = tempEntity.newRole(false, Permission.READ);
    newMembershipMapping(user, org.getId(), role.getId());

    Map<Key, Object> ctx = new HashMap<>();
    ctx.put(Key.ID, org.getId());
    ctx.put(Key.TYPE, OwnerType.ORGANIZATION);
    UserPrincipal userPrincipal = newPrincipal(user);
    assertThat(checker.isPermitted(userPrincipal, Permission.READ, ctx)).isTrue();
  }

  @Test
  public void testIsPermitted_WithTypeAndId_Repository() {
    Repository repo = tempEntity.newRepository();
    User user = tempEntity.newUser();
    Role role = tempEntity.newRole(false, Permission.READ);
    newMembershipMapping(user, repo.getId(), role.getId());

    Map<Key, Object> ctx = new HashMap<>();
    ctx.put(Key.ID, repo.getId());
    ctx.put(Key.TYPE, OwnerType.REPOSITORY);
    UserPrincipal userPrincipal = newPrincipal(user);
    assertThat(checker.isPermitted(userPrincipal, Permission.READ, ctx)).isTrue();
  }

  @Test
  public void testIsPermitted_WithTypeAndId_InternalId() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    User user = tempEntity.newUser();
    Role role = tempEntity.newRole(false, Permission.READ);
    newMembershipMapping(user, app.getId(), role.getId());

    Map<Key, Object> ctx = new HashMap<>();
    ctx.put(Key.INTERNAL_ID, app.getId());
    ctx.put(Key.TYPE, OwnerType.APPLICATION);
    UserPrincipal userPrincipal = newPrincipal(user);
    assertThat(checker.isPermitted(userPrincipal, Permission.READ, ctx)).isTrue();
  }

  @Test
  public void testIsPermitted_WithOwnerEntity() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    User user = tempEntity.newUser();
    Role role = tempEntity.newRole(false, Permission.READ);
    newMembershipMapping(user, app.getId(), role.getId());

    Map<Key, Object> ctx = new HashMap<>();
    ctx.put(Key.OWNER, app);
    UserPrincipal userPrincipal = newPrincipal(user);
    assertThat(checker.isPermitted(userPrincipal, Permission.READ, ctx)).isTrue();
  }
}
