/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.configuration.ldap.TestLdapServer;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MembershipMappingResourceTest
    extends AbstractResourceTest
{
  private Application app;

  private Organization org;

  private User userA;

  private User userB;

  private User userC;

  private final RoleDAO roleDAO = new RoleDAO();

  private final OrganizationDAO organizationDAO = new OrganizationDAO();

  private final UserDAO userDAO = new UserDAO();

  private User adminUser;

  @Rule
  public TestLdapServer embeddedLdapServer = new TestLdapServer();

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(MembershipMappingResource.RESOURCE_PATH);
  }

  private HttpResponse get(OwnerType ownerType) throws Exception {
    return restRequest().path(MembershipMappingResource.SINGLETON_APPLICABLE_MAPPINGS_PATH).parameter(ownerType).get();
  }

  private HttpResponse get(OwnerType ownerType, String ownerId) throws Exception {
    return restRequest().path(MembershipMappingResource.APPLICABLE_MAPPINGS_PATH).parameter(ownerType, ownerId).get();
  }

  private HttpResponse put(OwnerType ownerType, String roleId, Member... members) throws Exception {
    return restRequest().path(MembershipMappingResource.SINGLETON_ROLE_PATH).parameter(ownerType, roleId).body(members)
        .put();
  }

  private HttpResponse put(OwnerType ownerType, String ownerId, String roleId, Member... members) throws Exception {
    return restRequest().path(MembershipMappingResource.ROLE_PATH).parameter(ownerType, ownerId, roleId).body(members)
        .put();
  }

  private Member newMember(MemberType type, String name) {
    return new Member(type, name, null);
  }

  @Before
  public void init() {
    org = tempEntity.newOrganization("test-org");
    app = tempEntity.newApplication("test-app", "test-app", org.getId());
    userA = tempEntity.newUser("user-a", "John", "Doe", "void@void.com");
    userB = tempEntity.newUser("user-b", "Jane", "Doe", "void@void.com");
    userC = tempEntity.newUser("user-c", "John", "Smith", "void@void.com");
    adminUser = userDAO.getByUsername(User.ADMIN_USERNAME);
  }

  @Test
  public void testCRUD_Application() throws Exception {
    Organization rootOrg = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    User userD = tempEntity.newUser("user-d", "User", "D", "userd@example.com");
    // Initial state
    HttpResponse response = get(OwnerType.APPLICATION, app.getPublicId());
    assertResponseStatus(200, response);
    ApplicableMembershipMappings applicable = response.getBody(ApplicableMembershipMappings.class);
    assertThat(applicable).isNotNull();
    assertThat(applicable.membersByRole).isNotNull();

    List<Role> nonGlobalRoles = roleDAO.getApplicationRoles();
    assertThat(applicable.membersByRole).hasSameSizeAs(nonGlobalRoles);
    for (int i = 0; i < nonGlobalRoles.size(); i++) {
      MembersByRole membersByRole = applicable.membersByRole.get(i);
      Role role = nonGlobalRoles.get(i);
      assertThat(membersByRole.roleId).isEqualTo(role.getId());
      assertThat(membersByRole.roleName).isEqualTo(role.getName());
      assertThat(membersByRole.roleDescription).isEqualTo(role.getDescription());
      assertThat(membersByRole.membersByOwner).hasSize(3);
      assertThat(membersByRole.membersByOwner.get(0).ownerId).isEqualTo(app.getPublicId());
      assertThat(membersByRole.membersByOwner.get(0).members).isEmpty();
      assertThat(membersByRole.membersByOwner.get(1).ownerId).isEqualTo(org.getId());
      assertThat(membersByRole.membersByOwner.get(1).members).isEmpty();
      assertThat(membersByRole.membersByOwner.get(2).ownerId).isEqualTo(org.getParentOrganizationId());
      assertThat(membersByRole.membersByOwner.get(2).members).isEmpty();
    }

    // Create
    response = put(OwnerType.APPLICATION, app.getPublicId(), nonGlobalRoles.get(0).getId(),
        newMember(MemberType.USER, userC.getUsername()));
    assertResponseStatus(204, response);
    response = put(OwnerType.ORGANIZATION, org.getId(), nonGlobalRoles.get(0).getId(),
        newMember(MemberType.USER, userB.getUsername()));
    assertResponseStatus(204, response);
    response = put(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID, nonGlobalRoles.get(0).getId(),
        newMember(MemberType.USER, userA.getUsername()));
    assertResponseStatus(204, response);

    // Read
    response = get(OwnerType.APPLICATION, app.getPublicId());
    assertResponseStatus(200, response);
    applicable = response.getBody(ApplicableMembershipMappings.class);

    assertThat(applicable).isNotNull();
    assertThat(applicable.membersByRole).isNotNull();
    assertThat(applicable.membersByRole).hasSameSizeAs(nonGlobalRoles);

    MembersByRole membersByRole = applicable.membersByRole.get(0);
    assertThat(membersByRole.roleId).isEqualTo(nonGlobalRoles.get(0).getId());
    assertThat(membersByRole.membersByOwner).hasSize(3);
    assertMembersByOwner(membersByRole.membersByOwner.get(0), app, getMembersForUsers(userC));
    assertMembersByOwner(membersByRole.membersByOwner.get(1), org, getMembersForUsers(userB));
    assertMembersByOwner(membersByRole.membersByOwner.get(2), rootOrg, getMembersForUsers(userA));

    // Update
    response = put(OwnerType.APPLICATION, app.getPublicId(), nonGlobalRoles.get(0).getId(),
        newMember(MemberType.USER, userD.getUsername()));
    assertResponseStatus(204, response);

    // Read
    response = get(OwnerType.APPLICATION, app.getPublicId());
    assertResponseStatus(200, response);
    applicable = response.getBody(ApplicableMembershipMappings.class);

    assertThat(applicable).isNotNull();
    assertThat(applicable.membersByRole).isNotNull();
    assertThat(applicable.membersByRole).hasSameSizeAs(nonGlobalRoles);

    membersByRole = applicable.membersByRole.get(0);
    assertThat(membersByRole.roleId).isEqualTo(nonGlobalRoles.get(0).getId());
    assertThat(membersByRole.membersByOwner).hasSize(3);
    assertMembersByOwner(membersByRole.membersByOwner.get(0), app, getMembersForUsers(userD));
    assertMembersByOwner(membersByRole.membersByOwner.get(1), org, getMembersForUsers(userB));
    assertMembersByOwner(membersByRole.membersByOwner.get(2), rootOrg, getMembersForUsers(userA));

    // Delete
    response = put(OwnerType.APPLICATION, app.getPublicId(), nonGlobalRoles.get(0).getId(), new Member[0]);
    assertResponseStatus(204, response);

    // Read
    response = get(OwnerType.APPLICATION, app.getPublicId());
    assertResponseStatus(200, response);
    applicable = response.getBody(ApplicableMembershipMappings.class);

    assertThat(applicable).isNotNull();
    assertThat(applicable.membersByRole).isNotNull();
    assertThat(applicable.membersByRole).hasSameSizeAs(nonGlobalRoles);

    membersByRole = applicable.membersByRole.get(0);
    assertThat(membersByRole.roleId).isEqualTo(nonGlobalRoles.get(0).getId());
    assertThat(membersByRole.membersByOwner).hasSize(3);
    assertMembersByOwner(membersByRole.membersByOwner.get(0), app, Collections.emptyList());
    assertMembersByOwner(membersByRole.membersByOwner.get(1), org, getMembersForUsers(userB));
    assertMembersByOwner(membersByRole.membersByOwner.get(2), rootOrg, getMembersForUsers(userA));
  }

  @Test
  public void testCRUD_Organization() throws Exception {
    Organization rootOrg = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    // Initial state
    HttpResponse response = get(OwnerType.ORGANIZATION, org.getId());
    assertResponseStatus(200, response);
    ApplicableMembershipMappings applicable = response.getBody(ApplicableMembershipMappings.class);
    assertThat(applicable).isNotNull();
    assertThat(applicable.membersByRole).isNotNull();

    List<Role> nonGlobalRoles = roleDAO.getApplicationRoles();
    assertThat(applicable.membersByRole).hasSameSizeAs(nonGlobalRoles);
    for (int i = 0; i < nonGlobalRoles.size(); i++) {
      MembersByRole membersByRole = applicable.membersByRole.get(i);
      Role role = nonGlobalRoles.get(i);
      assertThat(membersByRole.roleId).isEqualTo(role.getId());
      assertThat(membersByRole.roleName).isEqualTo(role.getName());
      assertThat(membersByRole.roleDescription).isEqualTo(role.getDescription());
      assertThat(membersByRole.membersByOwner).hasSize(2);
      assertThat(membersByRole.membersByOwner.get(0).ownerId).isEqualTo(org.getId());
      assertThat(membersByRole.membersByOwner.get(0).members).isEmpty();
      assertThat(membersByRole.membersByOwner.get(1).ownerId).isEqualTo(org.getParentOrganizationId());
      assertThat(membersByRole.membersByOwner.get(1).members).isEmpty();
    }

    // Create
    response = put(OwnerType.ORGANIZATION, org.getId(), nonGlobalRoles.get(0).getId(),
        newMember(MemberType.USER, userB.getUsername()));
    assertResponseStatus(204, response);
    response = put(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID, nonGlobalRoles.get(0).getId(),
        newMember(MemberType.USER, userA.getUsername()));
    assertResponseStatus(204, response);

    // Read
    response = get(OwnerType.ORGANIZATION, org.getId());
    assertResponseStatus(200, response);
    applicable = response.getBody(ApplicableMembershipMappings.class);

    assertThat(applicable).isNotNull();
    assertThat(applicable.membersByRole).isNotNull();
    assertThat(applicable.membersByRole).hasSameSizeAs(nonGlobalRoles);

    MembersByRole membersByRole = applicable.membersByRole.get(0);
    assertThat(membersByRole.roleId).isEqualTo(nonGlobalRoles.get(0).getId());
    assertThat(membersByRole.membersByOwner).hasSize(2);
    assertMembersByOwner(membersByRole.membersByOwner.get(0), org, getMembersForUsers(userB));
    assertMembersByOwner(membersByRole.membersByOwner.get(1), rootOrg, getMembersForUsers(userA));

    // Update
    response = put(OwnerType.ORGANIZATION, org.getId(), nonGlobalRoles.get(0).getId(),
        newMember(MemberType.USER, userC.getUsername()));
    assertResponseStatus(204, response);

    // Read
    response = get(OwnerType.ORGANIZATION, org.getId());
    assertResponseStatus(200, response);
    applicable = response.getBody(ApplicableMembershipMappings.class);

    assertThat(applicable).isNotNull();
    assertThat(applicable.membersByRole).isNotNull();
    assertThat(applicable.membersByRole).hasSameSizeAs(nonGlobalRoles);

    membersByRole = applicable.membersByRole.get(0);
    assertThat(membersByRole.roleId).isEqualTo(nonGlobalRoles.get(0).getId());
    assertThat(membersByRole.membersByOwner).hasSize(2);
    assertMembersByOwner(membersByRole.membersByOwner.get(0), org, getMembersForUsers(userC));
    assertMembersByOwner(membersByRole.membersByOwner.get(1), rootOrg, getMembersForUsers(userA));

    // Delete
    response = put(OwnerType.ORGANIZATION, org.getId(), nonGlobalRoles.get(0).getId(), new Member[0]);
    assertResponseStatus(204, response);

    // Read
    response = get(OwnerType.ORGANIZATION, org.getId());
    assertResponseStatus(200, response);
    applicable = response.getBody(ApplicableMembershipMappings.class);

    assertThat(applicable).isNotNull();
    assertThat(applicable.membersByRole).isNotNull();
    assertThat(applicable.membersByRole).hasSameSizeAs(nonGlobalRoles);

    membersByRole = applicable.membersByRole.get(0);
    assertThat(membersByRole.roleId).isEqualTo(nonGlobalRoles.get(0).getId());
    assertThat(membersByRole.membersByOwner).hasSize(2);
    assertMembersByOwner(membersByRole.membersByOwner.get(0), org, Collections.emptyList());
    assertMembersByOwner(membersByRole.membersByOwner.get(1), rootOrg, getMembersForUsers(userA));
  }

  @Test
  public void testCRUD_Repository() throws Exception {
    Organization rootOrg = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo = tempEntity.newRepository(repoManager);
    User userD = tempEntity.newUser("user-d", "User", "D", "userd@example.com");
    // Initial state
    HttpResponse response = get(OwnerType.REPOSITORY, repo.getId());
    assertResponseStatus(200, response);
    ApplicableMembershipMappings applicable = response.getBody(ApplicableMembershipMappings.class);
    assertThat(applicable).isNotNull();
    assertThat(applicable.membersByRole).isNotNull();

    List<Role> nonGlobalRoles = roleDAO.getApplicationRoles();
    assertThat(applicable.membersByRole).hasSameSizeAs(nonGlobalRoles);
    for (int i = 0; i < nonGlobalRoles.size(); i++) {
      MembersByRole membersByRole = applicable.membersByRole.get(i);
      Role role = nonGlobalRoles.get(i);
      assertThat(membersByRole.roleId).isEqualTo(role.getId());
      assertThat(membersByRole.roleName).isEqualTo(role.getName());
      assertThat(membersByRole.roleDescription).isEqualTo(role.getDescription());
      assertThat(membersByRole.membersByOwner).hasSize(4);
      assertThat(membersByRole.membersByOwner.get(0).ownerId).isEqualTo(repo.getId());
      assertThat(membersByRole.membersByOwner.get(0).members).isEmpty();
      assertThat(membersByRole.membersByOwner.get(1).ownerId).isEqualTo(repoManager.getId());
      assertThat(membersByRole.membersByOwner.get(1).members).isEmpty();
      assertThat(membersByRole.membersByOwner.get(2).ownerId).isEqualTo(RepositoryContainer.REPOSITORY_CONTAINER_ID);
      assertThat(membersByRole.membersByOwner.get(2).members).isEmpty();
      assertThat(membersByRole.membersByOwner.get(3).ownerId).isEqualTo(Organization.ROOT_ORGANIZATION_ID);
      assertThat(membersByRole.membersByOwner.get(3).members).isEmpty();
    }

    // Create
    response = put(OwnerType.REPOSITORY, repo.getId(), nonGlobalRoles.get(0).getId(),
        newMember(MemberType.USER, userD.getUsername()));
    assertResponseStatus(204, response);
    response = put(OwnerType.REPOSITORY_MANAGER, repoManager.getId(), nonGlobalRoles.get(0).getId(),
        newMember(MemberType.USER, userC.getUsername()));
    assertResponseStatus(204, response);
    response = put(OwnerType.REPOSITORY_CONTAINER, nonGlobalRoles.get(0).getId(),
        newMember(MemberType.USER, userB.getUsername()));
    assertResponseStatus(204, response);
    response = put(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID, nonGlobalRoles.get(0).getId(),
        newMember(MemberType.USER, userA.getUsername()));
    assertResponseStatus(204, response);

    // Read
    response = get(OwnerType.REPOSITORY, repo.getId());
    assertResponseStatus(200, response);
    applicable = response.getBody(ApplicableMembershipMappings.class);

    assertThat(applicable).isNotNull();
    assertThat(applicable.membersByRole).isNotNull();
    assertThat(applicable.membersByRole).hasSameSizeAs(nonGlobalRoles);

    MembersByRole membersByRole = applicable.membersByRole.get(0);
    assertThat(membersByRole.roleId).isEqualTo(nonGlobalRoles.get(0).getId());
    assertThat(membersByRole.membersByOwner).hasSize(4);
    assertMembersByOwner(membersByRole.membersByOwner.get(0), repo, getMembersForUsers(userD));
    assertMembersByOwner(membersByRole.membersByOwner.get(1), repoManager, getMembersForUsers(userC));
    assertMembersByOwner(membersByRole.membersByOwner.get(2), RepositoryContainer.SINGLETON, getMembersForUsers(userB));
    assertMembersByOwner(membersByRole.membersByOwner.get(3), rootOrg, getMembersForUsers(userA));

    // Update
    response = put(OwnerType.REPOSITORY, repo.getId(), nonGlobalRoles.get(0).getId(),
        newMember(MemberType.USER, userA.getUsername()));
    assertResponseStatus(204, response);

    // Read
    response = get(OwnerType.REPOSITORY, repo.getId());
    assertResponseStatus(200, response);
    applicable = response.getBody(ApplicableMembershipMappings.class);

    assertThat(applicable).isNotNull();
    assertThat(applicable.membersByRole).isNotNull();
    assertThat(applicable.membersByRole).hasSameSizeAs(nonGlobalRoles);

    membersByRole = applicable.membersByRole.get(0);
    assertThat(membersByRole.roleId).isEqualTo(nonGlobalRoles.get(0).getId());
    assertThat(membersByRole.membersByOwner).hasSize(4);
    assertMembersByOwner(membersByRole.membersByOwner.get(0), repo, getMembersForUsers(userA));
    assertMembersByOwner(membersByRole.membersByOwner.get(1), repoManager, getMembersForUsers(userC));
    assertMembersByOwner(membersByRole.membersByOwner.get(2), RepositoryContainer.SINGLETON, getMembersForUsers(userB));
    assertMembersByOwner(membersByRole.membersByOwner.get(3), rootOrg, getMembersForUsers(userA));

    // Delete
    response = put(OwnerType.REPOSITORY, repo.getId(), nonGlobalRoles.get(0).getId(), new Member[0]);
    assertResponseStatus(204, response);

    // Read
    response = get(OwnerType.REPOSITORY, repo.getId());
    assertResponseStatus(200, response);
    applicable = response.getBody(ApplicableMembershipMappings.class);

    assertThat(applicable).isNotNull();
    assertThat(applicable.membersByRole).isNotNull();
    assertThat(applicable.membersByRole).hasSameSizeAs(nonGlobalRoles);

    membersByRole = applicable.membersByRole.get(0);
    assertThat(membersByRole.roleId).isEqualTo(nonGlobalRoles.get(0).getId());
    assertThat(membersByRole.membersByOwner).hasSize(4);
    assertMembersByOwner(membersByRole.membersByOwner.get(0), repo, Collections.emptyList());
    assertMembersByOwner(membersByRole.membersByOwner.get(1), repoManager, getMembersForUsers(userC));
    assertMembersByOwner(membersByRole.membersByOwner.get(2), RepositoryContainer.SINGLETON, getMembersForUsers(userB));
    assertMembersByOwner(membersByRole.membersByOwner.get(3), rootOrg, getMembersForUsers(userA));
  }

  @Test
  public void testCRUD_RepositoryManager() throws Exception {
    Organization rootOrg = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    // Initial state
    HttpResponse response = get(OwnerType.REPOSITORY_MANAGER, repoManager.getId());
    assertResponseStatus(200, response);
    ApplicableMembershipMappings applicable = response.getBody(ApplicableMembershipMappings.class);
    assertThat(applicable).isNotNull();
    assertThat(applicable.membersByRole).isNotNull();

    List<Role> nonGlobalRoles = roleDAO.getApplicationRoles();
    assertThat(applicable.membersByRole).hasSameSizeAs(nonGlobalRoles);
    for (int i = 0; i < nonGlobalRoles.size(); i++) {
      MembersByRole membersByRole = applicable.membersByRole.get(i);
      Role role = nonGlobalRoles.get(i);
      assertThat(membersByRole.roleId).isEqualTo(role.getId());
      assertThat(membersByRole.roleName).isEqualTo(role.getName());
      assertThat(membersByRole.roleDescription).isEqualTo(role.getDescription());
      assertThat(membersByRole.membersByOwner).hasSize(3);
      assertThat(membersByRole.membersByOwner.get(0).ownerId).isEqualTo(repoManager.getId());
      assertThat(membersByRole.membersByOwner.get(0).members).isEmpty();
      assertThat(membersByRole.membersByOwner.get(1).ownerId).isEqualTo(RepositoryContainer.REPOSITORY_CONTAINER_ID);
      assertThat(membersByRole.membersByOwner.get(1).members).isEmpty();
      assertThat(membersByRole.membersByOwner.get(2).ownerId).isEqualTo(Organization.ROOT_ORGANIZATION_ID);
      assertThat(membersByRole.membersByOwner.get(2).members).isEmpty();
    }

    // Create
    response = put(OwnerType.REPOSITORY_MANAGER, repoManager.getId(), nonGlobalRoles.get(0).getId(),
        newMember(MemberType.USER, userC.getUsername()));
    assertResponseStatus(204, response);
    response = put(OwnerType.REPOSITORY_CONTAINER, nonGlobalRoles.get(0).getId(),
        newMember(MemberType.USER, userB.getUsername()));
    assertResponseStatus(204, response);
    response = put(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID, nonGlobalRoles.get(0).getId(),
        newMember(MemberType.USER, userA.getUsername()));
    assertResponseStatus(204, response);

    // Read
    response = get(OwnerType.REPOSITORY_MANAGER, repoManager.getId());
    assertResponseStatus(200, response);
    applicable = response.getBody(ApplicableMembershipMappings.class);

    assertThat(applicable).isNotNull();
    assertThat(applicable.membersByRole).isNotNull();
    assertThat(applicable.membersByRole).hasSameSizeAs(nonGlobalRoles);

    MembersByRole membersByRole = applicable.membersByRole.get(0);
    assertThat(membersByRole.roleId).isEqualTo(nonGlobalRoles.get(0).getId());
    assertThat(membersByRole.membersByOwner).hasSize(3);
    assertMembersByOwner(membersByRole.membersByOwner.get(0), repoManager, getMembersForUsers(userC));
    assertMembersByOwner(membersByRole.membersByOwner.get(1), RepositoryContainer.SINGLETON, getMembersForUsers(userB));
    assertMembersByOwner(membersByRole.membersByOwner.get(2), rootOrg, getMembersForUsers(userA));

    // Update
    response = put(OwnerType.REPOSITORY_MANAGER, repoManager.getId(), nonGlobalRoles.get(0).getId(),
        newMember(MemberType.USER, userA.getUsername()));
    assertResponseStatus(204, response);

    // Read
    response = get(OwnerType.REPOSITORY_MANAGER, repoManager.getId());
    assertResponseStatus(200, response);
    applicable = response.getBody(ApplicableMembershipMappings.class);

    assertThat(applicable).isNotNull();
    assertThat(applicable.membersByRole).isNotNull();
    assertThat(applicable.membersByRole).hasSameSizeAs(nonGlobalRoles);

    membersByRole = applicable.membersByRole.get(0);
    assertThat(membersByRole.roleId).isEqualTo(nonGlobalRoles.get(0).getId());
    assertThat(membersByRole.membersByOwner).hasSize(3);
    assertMembersByOwner(membersByRole.membersByOwner.get(0), repoManager, getMembersForUsers(userA));
    assertMembersByOwner(membersByRole.membersByOwner.get(1), RepositoryContainer.SINGLETON, getMembersForUsers(userB));
    assertMembersByOwner(membersByRole.membersByOwner.get(2), rootOrg, getMembersForUsers(userA));

    // Delete
    response = put(OwnerType.REPOSITORY_MANAGER, repoManager.getId(), nonGlobalRoles.get(0).getId(), new Member[0]);
    assertResponseStatus(204, response);

    // Read
    response = get(OwnerType.REPOSITORY_MANAGER, repoManager.getId());
    assertResponseStatus(200, response);
    applicable = response.getBody(ApplicableMembershipMappings.class);

    assertThat(applicable).isNotNull();
    assertThat(applicable.membersByRole).isNotNull();
    assertThat(applicable.membersByRole).hasSameSizeAs(nonGlobalRoles);

    membersByRole = applicable.membersByRole.get(0);
    assertThat(membersByRole.roleId).isEqualTo(nonGlobalRoles.get(0).getId());
    assertThat(membersByRole.membersByOwner).hasSize(3);
    assertMembersByOwner(membersByRole.membersByOwner.get(0), repoManager, Collections.emptyList());
    assertMembersByOwner(membersByRole.membersByOwner.get(1), RepositoryContainer.SINGLETON, getMembersForUsers(userB));
    assertMembersByOwner(membersByRole.membersByOwner.get(2), rootOrg, getMembersForUsers(userA));
  }

  @Test
  public void testCRUD_RepositoryContainer() throws Exception {
    Organization rootOrg = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    // Initial state
    HttpResponse response = get(OwnerType.REPOSITORY_CONTAINER);
    assertResponseStatus(200, response);
    ApplicableMembershipMappings applicable = response.getBody(ApplicableMembershipMappings.class);
    assertThat(applicable).isNotNull();
    assertThat(applicable.membersByRole).isNotNull();

    List<Role> nonGlobalRoles = roleDAO.getApplicationRoles();
    assertThat(applicable.membersByRole).hasSameSizeAs(nonGlobalRoles);
    for (int i = 0; i < nonGlobalRoles.size(); i++) {
      MembersByRole membersByRole = applicable.membersByRole.get(i);
      Role role = nonGlobalRoles.get(i);
      assertThat(membersByRole.roleId).isEqualTo(role.getId());
      assertThat(membersByRole.roleName).isEqualTo(role.getName());
      assertThat(membersByRole.roleDescription).isEqualTo(role.getDescription());
      assertThat(membersByRole.membersByOwner).hasSize(2);
      assertThat(membersByRole.membersByOwner.get(0).ownerId).isEqualTo(RepositoryContainer.REPOSITORY_CONTAINER_ID);
      assertThat(membersByRole.membersByOwner.get(0).members).isEmpty();
      assertThat(membersByRole.membersByOwner.get(1).ownerId).isEqualTo(Organization.ROOT_ORGANIZATION_ID);
      assertThat(membersByRole.membersByOwner.get(1).members).isEmpty();
    }

    // Create
    response = put(OwnerType.REPOSITORY_CONTAINER, nonGlobalRoles.get(0).getId(),
        newMember(MemberType.USER, userB.getUsername()));
    assertResponseStatus(204, response);
    response = put(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID, nonGlobalRoles.get(0).getId(),
        newMember(MemberType.USER, userA.getUsername()));
    assertResponseStatus(204, response);

    // Read
    response = get(OwnerType.REPOSITORY_CONTAINER);
    assertResponseStatus(200, response);
    applicable = response.getBody(ApplicableMembershipMappings.class);

    assertThat(applicable).isNotNull();
    assertThat(applicable.membersByRole).isNotNull();
    assertThat(applicable.membersByRole).hasSameSizeAs(nonGlobalRoles);

    MembersByRole membersByRole = applicable.membersByRole.get(0);
    assertThat(membersByRole.roleId).isEqualTo(nonGlobalRoles.get(0).getId());
    assertThat(membersByRole.membersByOwner).hasSize(2);
    assertMembersByOwner(membersByRole.membersByOwner.get(0), RepositoryContainer.SINGLETON, getMembersForUsers(userB));
    assertMembersByOwner(membersByRole.membersByOwner.get(1), rootOrg, getMembersForUsers(userA));

    // Update
    response = put(OwnerType.REPOSITORY_CONTAINER, nonGlobalRoles.get(0).getId(),
        newMember(MemberType.USER, userC.getUsername()));
    assertResponseStatus(204, response);

    // Read
    response = get(OwnerType.REPOSITORY_CONTAINER);
    assertResponseStatus(200, response);
    applicable = response.getBody(ApplicableMembershipMappings.class);

    assertThat(applicable).isNotNull();
    assertThat(applicable.membersByRole).isNotNull();
    assertThat(applicable.membersByRole).hasSameSizeAs(nonGlobalRoles);

    membersByRole = applicable.membersByRole.get(0);
    assertThat(membersByRole.roleId).isEqualTo(nonGlobalRoles.get(0).getId());
    assertThat(membersByRole.membersByOwner).hasSize(2);
    assertMembersByOwner(membersByRole.membersByOwner.get(0), RepositoryContainer.SINGLETON, getMembersForUsers(userC));
    assertMembersByOwner(membersByRole.membersByOwner.get(1), rootOrg, getMembersForUsers(userA));

    // Delete
    response = put(OwnerType.REPOSITORY_CONTAINER, nonGlobalRoles.get(0).getId(), new Member[0]);
    assertResponseStatus(204, response);

    // Read
    response = get(OwnerType.REPOSITORY_CONTAINER);
    assertResponseStatus(200, response);
    applicable = response.getBody(ApplicableMembershipMappings.class);

    assertThat(applicable).isNotNull();
    assertThat(applicable.membersByRole).isNotNull();
    assertThat(applicable.membersByRole).hasSameSizeAs(nonGlobalRoles);

    membersByRole = applicable.membersByRole.get(0);
    assertThat(membersByRole.roleId).isEqualTo(nonGlobalRoles.get(0).getId());
    assertThat(membersByRole.membersByOwner).hasSize(2);
    assertMembersByOwner(membersByRole.membersByOwner.get(0), RepositoryContainer.SINGLETON, Collections.emptyList());
    assertMembersByOwner(membersByRole.membersByOwner.get(1), rootOrg, getMembersForUsers(userA));
  }

  @Test
  public void testLdap_GlobalRoles() throws Exception {
    embeddedLdapServer.start();
    embeddedLdapServer.loadData("/MembershipMappingResourceTest/ldap_users.ldif");

    LdapServer ldapServer = tempEntity.newLdapServer("LDAP");
    tempEntity.newLdapConnection(ldapServer.getId(), embeddedLdapServer.getPort());
    tempEntity.newLdapUserMapping(ldapServer.getId());

    // Initial state
    HttpResponse response = get(OwnerType.GLOBAL, MembershipMapping.GLOBAL_CONTEXT_ID);
    List<Role> roles = assertInitialGlobalState(response);

    // Create
    response = put(OwnerType.GLOBAL, MembershipMapping.GLOBAL_CONTEXT_ID, Role.SYSTEM_ADMIN_ROLE_ID,
        newMember(MemberType.USER, User.ADMIN_USERNAME), newMember(MemberType.USER, "testuser"),
        newMember(MemberType.GROUP, "Alpha"));
    assertResponseStatus(204, response);

    // Read for created data
    response = get(OwnerType.GLOBAL, MembershipMapping.GLOBAL_CONTEXT_ID);
    assertResponseStatus(200, response);
    ApplicableMembershipMappings applicable = response.getBody(ApplicableMembershipMappings.class);

    assertThat(applicable).isNotNull();
    assertThat(applicable.membersByRole).hasSameSizeAs(roles);

    MembersByRole membersByRole = applicable.membersByRole.get(1);
    assertThat(membersByRole.roleId).isEqualTo(Role.SYSTEM_ADMIN_ROLE_ID);
    assertThat(membersByRole.membersByOwner).hasSize(1);
    MembersByOwner membersByOwner = membersByRole.membersByOwner.get(0);
    assertThat(membersByOwner.ownerId).isEqualTo(MembershipMapping.GLOBAL_CONTEXT_ID);
    assertThat(membersByOwner.ownerName).isEqualTo(MembershipMapping.GLOBAL_CONTEXT_NAME);
    assertThat(membersByOwner.ownerType).isEqualTo(OwnerType.GLOBAL);
    assertThat(membersByOwner.members).hasSize(3);

    membersByOwner.members.sort(new MemberComparator());
    assertMember(membersByOwner.members.get(0), MemberType.USER, User.ADMIN_USERNAME, "Admin BuiltIn",
        "admin@localhost", "IQ Server");
    assertMember(membersByOwner.members.get(1), MemberType.GROUP, "Alpha", "Alpha", null, "LDAP");
    assertMember(membersByOwner.members.get(2), MemberType.USER, "testuser", "John Doe", "test.user@company.com",
        "LDAP");

    // Reset Initial State
    response = put(OwnerType.GLOBAL, MembershipMapping.GLOBAL_CONTEXT_ID, Role.SYSTEM_ADMIN_ROLE_ID,
        newMember(MemberType.USER, User.ADMIN_USERNAME));
    assertResponseStatus(204, response);
  }

  @Test
  public void testCRUD_GlobalRoles() throws Exception {
    // Initial state
    HttpResponse response = get(OwnerType.GLOBAL, MembershipMapping.GLOBAL_CONTEXT_ID);
    List<Role> roles = assertInitialGlobalState(response);

    // Create
    response = put(OwnerType.GLOBAL, MembershipMapping.GLOBAL_CONTEXT_ID, roles.get(0).getId(),
        newMember(MemberType.USER, User.ADMIN_USERNAME), newMember(MemberType.USER, userB.getUsername()));
    assertResponseStatus(204, response);

    // Read for created data
    response = get(OwnerType.GLOBAL, MembershipMapping.GLOBAL_CONTEXT_ID);
    assertResponseStatus(200, response);
    ApplicableMembershipMappings applicable = response.getBody(ApplicableMembershipMappings.class);

    assertThat(applicable).isNotNull();
    assertThat(applicable.membersByRole).hasSameSizeAs(roles);

    MembersByRole membersByRole = applicable.membersByRole.get(0);
    assertThat(membersByRole.roleId).isEqualTo(roles.get(0).getId());
    assertThat(membersByRole.membersByOwner).hasSize(1);
    MembersByOwner membersByOwner = membersByRole.membersByOwner.get(0);
    assertThat(membersByOwner.ownerId).isEqualTo(MembershipMapping.GLOBAL_CONTEXT_ID);
    assertThat(membersByOwner.ownerName).isEqualTo(MembershipMapping.GLOBAL_CONTEXT_NAME);
    assertThat(membersByOwner.ownerType).isEqualTo(OwnerType.GLOBAL);
    assertMembers(membersByOwner.members, getMembersForUsers(adminUser, userB));

    // Update
    response = put(OwnerType.GLOBAL, MembershipMapping.GLOBAL_CONTEXT_ID, roles.get(0).getId(),
        newMember(MemberType.USER, User.ADMIN_USERNAME));
    assertResponseStatus(204, response);

    // Read for updated data
    response = get(OwnerType.GLOBAL, MembershipMapping.GLOBAL_CONTEXT_ID);
    assertResponseStatus(200, response);
    applicable = response.getBody(ApplicableMembershipMappings.class);
    assertThat(applicable).isNotNull();
    assertThat(applicable.membersByRole).hasSameSizeAs(roles);

    membersByRole = applicable.membersByRole.get(0);
    assertThat(membersByRole.roleId).isEqualTo(roles.get(0).getId());
    assertThat(membersByRole.membersByOwner).hasSize(1);
    membersByOwner = membersByRole.membersByOwner.get(0);
    assertThat(membersByOwner.ownerId).isEqualTo(MembershipMapping.GLOBAL_CONTEXT_ID);
    assertThat(membersByOwner.ownerName).isEqualTo(MembershipMapping.GLOBAL_CONTEXT_NAME);
    assertThat(membersByOwner.ownerType).isEqualTo(OwnerType.GLOBAL);
    assertMembers(membersByOwner.members, getMembersForUsers(adminUser));

    // Reset Initial State
    response = put(OwnerType.GLOBAL, MembershipMapping.GLOBAL_CONTEXT_ID, roles.get(0).getId(),
        newMember(MemberType.USER, User.ADMIN_USERNAME));
    assertResponseStatus(204, response);
  }

  private List<Role> assertInitialGlobalState(HttpResponse response) {
    assertResponseStatus(200, response);
    ApplicableMembershipMappings applicable = response.getBody(ApplicableMembershipMappings.class);
    assertThat(applicable).isNotNull();
    assertThat(applicable.membersByRole).isNotNull();

    List<Role> roles = roleDAO.getGlobalRoles();
    assertThat(applicable.membersByRole).hasSameSizeAs(roles);
    for (int i = 0; i < roles.size(); i++) {
      MembersByRole membersByRole = applicable.membersByRole.get(i);
      Role role = roles.get(i);
      assertThat(membersByRole.roleId).isEqualTo(role.getId());
      assertThat(membersByRole.roleName).isEqualTo(role.getName());
      assertThat(membersByRole.roleDescription).isEqualTo(role.getDescription());
      assertThat(membersByRole.membersByOwner).hasSize(1);
      MembersByOwner membersByOwner = membersByRole.membersByOwner.get(0);
      assertThat(membersByOwner.ownerId).isEqualTo(MembershipMapping.GLOBAL_CONTEXT_ID);
      assertThat(membersByOwner.ownerName).isEqualTo(MembershipMapping.GLOBAL_CONTEXT_NAME);
      assertThat(membersByOwner.ownerType).isEqualTo(OwnerType.GLOBAL);
      assertMembers(membersByOwner.members, getMembersForUsers(adminUser));
    }

    return roles;
  }

  @Test
  public void testSystemAdministratorRoleCantBeRevokedFromAllUsers() throws Exception {
    Role systemAdminRole = roleDAO.getById(Role.SYSTEM_ADMIN_ROLE_ID);
    assertThat(systemAdminRole).isNotNull();

    HttpResponse response = put(OwnerType.GLOBAL, MembershipMapping.GLOBAL_CONTEXT_ID, systemAdminRole.getId());
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("There must be at least one user in the System Administrator role.");
  }

  private List<Member> getMembersForUsers(final User... users) {
    List<Member> members = new ArrayList<>();
    for (User user : users) {
      members.add(
          new Member(MemberType.USER, user.getUsername(), user.calculateDisplayName(), user.getEmail(), "IQ Server"));
    }
    return members;
  }

  private void assertMembersByOwner(final MembersByOwner membersByOwner,
                                    final Owner expectedOwner,
                                    final List<Member> expectedMembers)
  {
    assertThat(membersByOwner.ownerType).isEqualTo(expectedOwner.getType());
    if (OwnerType.APPLICATION.equals(expectedOwner.getType())) {
      assertThat(membersByOwner.ownerId).isEqualTo(expectedOwner.getPublicId());
    }
    else {
      assertThat(membersByOwner.ownerId).isEqualTo(expectedOwner.getId());
    }
    assertThat(membersByOwner.ownerName).isEqualTo(expectedOwner.getName());

    assertMembers(membersByOwner.members, expectedMembers);
  }

  private void assertMembers(final List<Member> members, final List<Member> expectedMembers) {
    assertThat(members).hasSize(expectedMembers.size());

    members.sort(new MemberComparator());
    expectedMembers.sort(new MemberComparator());

    for (int i = 0; i < expectedMembers.size(); i++) {
      Member expectedMember = expectedMembers.get(i);
      Member member = members.get(i);
      assertMember(member, expectedMember.getType(), expectedMember.getInternalName(), expectedMember.getDisplayName(),
          expectedMember.getEmail(), expectedMember.getRealm());
    }
  }

  private void assertMember(Member member,
                            MemberType type,
                            String internalName,
                            String displayName,
                            String email,
                            String realm)
  {
    assertThat(member.getType()).isEqualTo(type);
    assertThat(member.getInternalName()).isEqualTo(internalName);
    assertThat(member.getDisplayName()).isEqualTo(displayName);
    assertThat(member.getEmail()).isEqualTo(email);
    assertThat(member.getRealm()).isEqualTo(realm);
  }

  private static class MemberComparator
      implements Comparator<Member>
  {
    @Override
    public int compare(final Member member, final Member otherMember) {
      int nameComp = member.getDisplayName().compareTo(otherMember.getDisplayName());
      if (nameComp != 0) {
        return nameComp;
      }
      else {
        return member.getType().compareTo(otherMember.getType());
      }
    }
  }
}
