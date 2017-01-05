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
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class MembershipMappingResourceTest
    extends AbstractResourceTest
{
  private Application app;

  private Organization org;

  private User userA;

  private User userB;

  private User userC;

  private RoleDAO roleDAO = new RoleDAO();

  private OrganizationDAO organizationDAO = new OrganizationDAO();

  private UserDAO userDAO = new UserDAO();

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
  public void init() throws Exception {
    org = tempEntity.newOrganization("test-org");
    app = tempEntity.newApplication("test-app", "test-app", org.getId());
    userA = tempEntity.newUser("user-a", "John", "Doe", "void@void.com");
    userB = tempEntity.newUser("user-b", "Jane", "Doe", "void@void.com");
    userC = tempEntity.newUser("user-c", "John", "Smith", "void@void.com");
    adminUser = userDAO.getByUsername(User.ADMIN_USERNAME);
  }

  @Test
  public void testCRUD_AppRoles() throws Exception {
    // Initial state
    HttpResponse response = get(OwnerType.APPLICATION, app.getPublicId());
    assertResponseStatus(200, response);
    ApplicableMembershipMappings applicable = response.getBody(ApplicableMembershipMappings.class);
    assertThat(applicable, is(notNullValue()));
    assertThat(applicable.membersByRole, is(notNullValue()));

    List<Role> appRoles = roleDAO.getApplicationRoles();
    assertThat(applicable.membersByRole, hasSize(appRoles.size()));
    for (int i = 0; i < appRoles.size(); i++) {
      MembersByRole membersByRole = applicable.membersByRole.get(i);
      Role role = appRoles.get(i);
      assertThat(membersByRole.roleId, is(role.getId()));
      assertThat(membersByRole.roleName, is(role.getName()));
      assertThat(membersByRole.roleDescription, is(role.getDescription()));
      assertThat(membersByRole.membersByOwner, is(notNullValue()));
      assertThat(membersByRole.membersByOwner.size(), is(3));
      assertThat(membersByRole.membersByOwner.get(0).ownerId, is(app.getPublicId()));
      assertThat(membersByRole.membersByOwner.get(0).members, is(notNullValue()));
      assertThat(membersByRole.membersByOwner.get(1).ownerId, is(org.getId()));
      assertThat(membersByRole.membersByOwner.get(2).ownerId, is(org.getParentOrganizationId()));
    }

    // Create
    response = put(OwnerType.ORGANIZATION, app.getOrganizationId(), appRoles.get(0).getId(),
        newMember(MemberType.USER, userB.getUsername()));
    assertResponseStatus(204, response);
    response = put(OwnerType.ORGANIZATION, org.getParentOrganizationId(), appRoles.get(0).getId(),
        newMember(MemberType.USER, userC.getUsername()));
    assertResponseStatus(204, response);

    // Read for created data
    response = get(OwnerType.ORGANIZATION, app.getOrganizationId());
    assertResponseStatus(200, response);
    applicable = response.getBody(ApplicableMembershipMappings.class);

    assertThat(applicable, is(notNullValue()));
    assertThat(applicable.membersByRole, is(notNullValue()));
    assertThat(applicable.membersByRole, hasSize(appRoles.size()));

    MembersByRole membersByRole = applicable.membersByRole.get(0);
    assertThat(membersByRole.roleId, is(appRoles.get(0).getId()));
    assertThat(membersByRole.membersByOwner, is(notNullValue()));
    assertThat(membersByRole.membersByOwner, hasSize(2));
    MembersByOwner membersByOwner = membersByRole.membersByOwner.get(0);
    assertMembersByOwner(membersByOwner, org, getMembersForUsers(userB));

    // Check the parent org
    Organization parentOrg = organizationDAO.getById(org.getParentOrganizationId());
    membersByOwner = membersByRole.membersByOwner.get(1);
    assertMembersByOwner(membersByOwner, parentOrg, getMembersForUsers(userC));

    // Update
    response = put(OwnerType.APPLICATION, app.getPublicId(), appRoles.get(0).getId(),
        newMember(MemberType.USER, userA.getUsername()));
    assertResponseStatus(204, response);
    response = put(OwnerType.APPLICATION, app.getPublicId(), appRoles.get(1).getId(),
        newMember(MemberType.USER, userB.getUsername()));
    assertResponseStatus(204, response);

    // Read for updated data
    response = get(OwnerType.APPLICATION, app.getPublicId());
    assertResponseStatus(200, response);
    applicable = response.getBody(ApplicableMembershipMappings.class);
    assertThat(applicable, is(notNullValue()));
    assertThat(applicable.membersByRole, is(notNullValue()));
    assertThat(applicable.membersByRole, hasSize(appRoles.size()));

    membersByRole = applicable.membersByRole.get(0);
    assertThat(membersByRole.roleId, is(appRoles.get(0).getId()));
    assertThat(membersByRole.membersByOwner, is(notNullValue()));
    assertThat(membersByRole.membersByOwner, hasSize(3));

    membersByOwner = membersByRole.membersByOwner.get(0);
    assertMembersByOwner(membersByOwner, app, getMembersForUsers(userA));

    membersByOwner = membersByRole.membersByOwner.get(1);
    assertMembersByOwner(membersByOwner, org, getMembersForUsers(userB));

    membersByOwner = membersByRole.membersByOwner.get(2);
    assertMembersByOwner(membersByOwner, parentOrg, getMembersForUsers(userC));

    membersByRole = applicable.membersByRole.get(1);
    assertThat(membersByRole.roleId, is(appRoles.get(1).getId()));
    assertThat(membersByRole.membersByOwner, is(notNullValue()));
    assertThat(membersByRole.membersByOwner, hasSize(3));
    membersByOwner = membersByRole.membersByOwner.get(0);
    assertMembersByOwner(membersByOwner, app, getMembersForUsers(userB));

    membersByOwner = membersByRole.membersByOwner.get(1);
    assertMembersByOwner(membersByOwner, org, getMembersForUsers());

    membersByOwner = membersByRole.membersByOwner.get(2);
    assertMembersByOwner(membersByOwner, parentOrg, getMembersForUsers());
  }

  @Test
  public void testCRUD_RepositoryContainerRoles() throws Exception {
    // Initial state
    HttpResponse response = get(OwnerType.REPOSITORY_CONTAINER);
    assertResponseStatus(200, response);
    ApplicableMembershipMappings applicable = response.getBody(ApplicableMembershipMappings.class);
    assertThat(applicable, is(notNullValue()));
    assertThat(applicable.membersByRole, is(notNullValue()));

    List<Role> appRoles = roleDAO.getApplicationRoles();
    assertThat(applicable.membersByRole, hasSize(appRoles.size()));
    for (int i = 0; i < appRoles.size(); i++) {
      MembersByRole membersByRole = applicable.membersByRole.get(i);
      Role role = appRoles.get(i);
      assertThat(membersByRole.roleId, is(role.getId()));
      assertThat(membersByRole.roleName, is(role.getName()));
      assertThat(membersByRole.roleDescription, is(role.getDescription()));
      assertThat(membersByRole.membersByOwner, is(notNullValue()));
      assertThat(membersByRole.membersByOwner.size(), is(2));
      assertThat(membersByRole.membersByOwner.get(0).ownerId, is(RepositoryContainer.REPOSITORY_CONTAINER_ID));
      assertThat(membersByRole.membersByOwner.get(0).members, is(notNullValue()));
      assertThat(membersByRole.membersByOwner.get(1).ownerId, is(org.getParentOrganizationId()));
    }

    // Create
    response = put(OwnerType.REPOSITORY_CONTAINER, appRoles.get(0).getId(),
        newMember(MemberType.USER, userB.getUsername()));
    assertResponseStatus(204, response);
    response = put(OwnerType.ORGANIZATION, org.getParentOrganizationId(), appRoles.get(0).getId(),
        newMember(MemberType.USER, userC.getUsername()));
    assertResponseStatus(204, response);

    // Read for created data
    response = get(OwnerType.REPOSITORY_CONTAINER);
    assertResponseStatus(200, response);
    applicable = response.getBody(ApplicableMembershipMappings.class);

    assertThat(applicable, is(notNullValue()));
    assertThat(applicable.membersByRole, is(notNullValue()));
    assertThat(applicable.membersByRole, hasSize(appRoles.size()));

    MembersByRole membersByRole = applicable.membersByRole.get(0);
    assertThat(membersByRole.roleId, is(appRoles.get(0).getId()));
    assertThat(membersByRole.membersByOwner, is(notNullValue()));
    assertThat(membersByRole.membersByOwner, hasSize(2));
    MembersByOwner membersByOwner = membersByRole.membersByOwner.get(0);
    assertMembersByOwner(membersByOwner, RepositoryContainer.SINGLETON, getMembersForUsers(userB));

    // Check the parent org
    Organization parentOrg = organizationDAO.getById(org.getParentOrganizationId());
    membersByOwner = membersByRole.membersByOwner.get(1);
    assertMembersByOwner(membersByOwner, parentOrg, getMembersForUsers(userC));

    // Update
    response = put(OwnerType.REPOSITORY_CONTAINER, appRoles.get(0).getId(),
        newMember(MemberType.USER, userA.getUsername()));
    assertResponseStatus(204, response);
    response = put(OwnerType.REPOSITORY_CONTAINER, appRoles.get(1).getId(),
        newMember(MemberType.USER, userB.getUsername()));
    assertResponseStatus(204, response);

    // Read for updated data
    response = get(OwnerType.REPOSITORY_CONTAINER);
    assertResponseStatus(200, response);
    applicable = response.getBody(ApplicableMembershipMappings.class);
    assertThat(applicable, is(notNullValue()));
    assertThat(applicable.membersByRole, is(notNullValue()));
    assertThat(applicable.membersByRole, hasSize(appRoles.size()));

    membersByRole = applicable.membersByRole.get(0);
    assertThat(membersByRole.roleId, is(appRoles.get(0).getId()));
    assertThat(membersByRole.membersByOwner, is(notNullValue()));
    assertThat(membersByRole.membersByOwner, hasSize(2));

    membersByOwner = membersByRole.membersByOwner.get(0);
    assertMembersByOwner(membersByOwner, RepositoryContainer.SINGLETON, getMembersForUsers(userA));

    membersByOwner = membersByRole.membersByOwner.get(1);
    assertMembersByOwner(membersByOwner, parentOrg, getMembersForUsers(userC));

    membersByRole = applicable.membersByRole.get(1);
    assertThat(membersByRole.roleId, is(appRoles.get(1).getId()));
    assertThat(membersByRole.membersByOwner, is(notNullValue()));
    assertThat(membersByRole.membersByOwner, hasSize(2));
    membersByOwner = membersByRole.membersByOwner.get(0);
    assertMembersByOwner(membersByOwner, RepositoryContainer.SINGLETON, getMembersForUsers(userB));

    membersByOwner = membersByRole.membersByOwner.get(1);
    assertMembersByOwner(membersByOwner, parentOrg, getMembersForUsers());
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
    List<Role> roles = testInitialGlobalState(response);

    // Create
    response = put(OwnerType.GLOBAL, MembershipMapping.GLOBAL_CONTEXT_ID, Role.SYSTEM_ADMIN_ROLE_ID,
        newMember(MemberType.USER, User.ADMIN_USERNAME), newMember(MemberType.USER, "testuser"),
        newMember(MemberType.GROUP, "Alpha"));
    assertResponseStatus(204, response);

    // Read for created data
    response = get(OwnerType.GLOBAL, MembershipMapping.GLOBAL_CONTEXT_ID);
    assertResponseStatus(200, response);
    ApplicableMembershipMappings applicable = response.getBody(ApplicableMembershipMappings.class);

    assertThat(applicable, is(notNullValue()));
    assertThat(applicable.membersByRole, is(notNullValue()));
    assertThat(applicable.membersByRole, hasSize(roles.size()));

    MembersByRole membersByRole = applicable.membersByRole.get(1);
    assertThat(membersByRole.roleId, is(Role.SYSTEM_ADMIN_ROLE_ID));
    assertThat(membersByRole.membersByOwner, is(notNullValue()));
    assertThat(membersByRole.membersByOwner, hasSize(1));
    MembersByOwner membersByOwner = membersByRole.membersByOwner.get(0);
    assertThat(membersByOwner.ownerId, is(MembershipMapping.GLOBAL_CONTEXT_ID));
    assertThat(membersByOwner.ownerName, is(MembershipMapping.GLOBAL_CONTEXT_NAME));
    assertThat(membersByOwner.ownerType, is(OwnerType.GLOBAL));
    assertThat(membersByOwner.members, is(notNullValue()));
    assertThat(membersByOwner.members, hasSize(3));

    Collections.sort(membersByOwner.members, new MemberComparator());
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
    List<Role> roles = testInitialGlobalState(response);

    // Create
    response = put(OwnerType.GLOBAL, MembershipMapping.GLOBAL_CONTEXT_ID, roles.get(0).getId(),
        newMember(MemberType.USER, User.ADMIN_USERNAME), newMember(MemberType.USER, userB.getUsername()));
    assertResponseStatus(204, response);

    // Read for created data
    response = get(OwnerType.GLOBAL, MembershipMapping.GLOBAL_CONTEXT_ID);
    assertResponseStatus(200, response);
    ApplicableMembershipMappings applicable = response.getBody(ApplicableMembershipMappings.class);

    assertThat(applicable, is(notNullValue()));
    assertThat(applicable.membersByRole, is(notNullValue()));
    assertThat(applicable.membersByRole, hasSize(roles.size()));

    MembersByRole membersByRole = applicable.membersByRole.get(0);
    assertThat(membersByRole.roleId, is(roles.get(0).getId()));
    assertThat(membersByRole.membersByOwner, is(notNullValue()));
    assertThat(membersByRole.membersByOwner, hasSize(1));
    MembersByOwner membersByOwner = membersByRole.membersByOwner.get(0);
    assertThat(membersByOwner.ownerId, is(MembershipMapping.GLOBAL_CONTEXT_ID));
    assertThat(membersByOwner.ownerName, is(MembershipMapping.GLOBAL_CONTEXT_NAME));
    assertThat(membersByOwner.ownerType, is(OwnerType.GLOBAL));
    assertMembers(membersByOwner.members, getMembersForUsers(adminUser, userB));

    // Update
    response = put(OwnerType.GLOBAL, MembershipMapping.GLOBAL_CONTEXT_ID, roles.get(0).getId(),
        newMember(MemberType.USER, User.ADMIN_USERNAME));
    assertResponseStatus(204, response);

    // Read for updated data
    response = get(OwnerType.GLOBAL, MembershipMapping.GLOBAL_CONTEXT_ID);
    assertResponseStatus(200, response);
    applicable = response.getBody(ApplicableMembershipMappings.class);
    assertThat(applicable, is(notNullValue()));
    assertThat(applicable.membersByRole, is(notNullValue()));
    assertThat(applicable.membersByRole, hasSize(roles.size()));

    membersByRole = applicable.membersByRole.get(0);
    assertThat(membersByRole.roleId, is(roles.get(0).getId()));
    assertThat(membersByRole.membersByOwner, is(notNullValue()));
    assertThat(membersByRole.membersByOwner, hasSize(1));
    membersByOwner = membersByRole.membersByOwner.get(0);
    assertThat(membersByOwner.ownerId, is(MembershipMapping.GLOBAL_CONTEXT_ID));
    assertThat(membersByOwner.ownerName, is(MembershipMapping.GLOBAL_CONTEXT_NAME));
    assertThat(membersByOwner.ownerType, is(OwnerType.GLOBAL));
    assertMembers(membersByOwner.members, getMembersForUsers(adminUser));

    // Reset Initial State
    response = put(OwnerType.GLOBAL, MembershipMapping.GLOBAL_CONTEXT_ID, roles.get(0).getId(),
        newMember(MemberType.USER, User.ADMIN_USERNAME));
    assertResponseStatus(204, response);
  }

  private List<Role> testInitialGlobalState(HttpResponse response) {
    assertResponseStatus(200, response);
    ApplicableMembershipMappings applicable = response.getBody(ApplicableMembershipMappings.class);
    assertThat(applicable, is(notNullValue()));
    assertThat(applicable.membersByRole, is(notNullValue()));

    List<Role> roles = roleDAO.getGlobalRoles();
    assertThat(applicable.membersByRole, hasSize(roles.size()));
    for (int i = 0; i < roles.size(); i++) {
      MembersByRole membersByRole = applicable.membersByRole.get(i);
      Role role = roles.get(i);
      assertThat(membersByRole.roleId, is(role.getId()));
      assertThat(membersByRole.roleName, is(role.getName()));
      assertThat(membersByRole.roleDescription, is(role.getDescription()));
      assertThat(membersByRole.membersByOwner, is(notNullValue()));
      assertThat(membersByRole.membersByOwner, is(hasSize(1)));
      MembersByOwner membersByOwner = membersByRole.membersByOwner.get(0);
      assertThat(membersByOwner.ownerId, is(MembershipMapping.GLOBAL_CONTEXT_ID));
      assertThat(membersByOwner.ownerName, is(MembershipMapping.GLOBAL_CONTEXT_NAME));
      assertThat(membersByOwner.ownerType, is(OwnerType.GLOBAL));
      assertMembers(membersByOwner.members, getMembersForUsers(adminUser));
    }

    return roles;
  }

  @Test
  public void testSystemAdministratorRoleCantBeRevokedFromAllUsers() throws Exception {
    Role systemAdminRole = roleDAO.getById(Role.SYSTEM_ADMIN_ROLE_ID);
    assertThat(systemAdminRole, is(notNullValue()));

    HttpResponse response = put(OwnerType.GLOBAL, MembershipMapping.GLOBAL_CONTEXT_ID, systemAdminRole.getId());
    assertResponseStatus(400, response);
    assertThat(response.getBodyText(), is("There must be at least one user in the System Administrator role."));
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
    assertThat(membersByOwner.ownerId, is(expectedOwner.getPublicId()));
    assertThat(membersByOwner.ownerName, is(expectedOwner.getName()));
    assertThat(membersByOwner.ownerType, is(expectedOwner.getType()));

    assertMembers(membersByOwner.members, expectedMembers);
  }

  private void assertMembers(final List<Member> members, final List<Member> expectedMembers) {
    assertThat(members, hasSize(expectedMembers.size()));

    Collections.sort(members, new MemberComparator());
    Collections.sort(expectedMembers, new MemberComparator());

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
    assertThat(member.getType(), is(type));
    assertThat(member.getInternalName(), is(internalName));
    assertThat(member.getDisplayName(), is(displayName));
    if (StringUtils.isNotEmpty(email)) {
      assertThat(member.getEmail(), is(email));
    }
    else {
      assertThat(member.getEmail(), is(nullValue()));
    }
    assertThat(member.getRealm(), is(realm));
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
