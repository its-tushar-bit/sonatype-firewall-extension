/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.TemporaryEntity;
import com.sonatype.insight.brain.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.ldap.EmbeddedLdapServer;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.security.MembershipMappingResource.ApplicableMembershipMappings;
import com.sonatype.insight.brain.security.MembershipMappingResource.Member;
import com.sonatype.insight.brain.security.MembershipMappingResource.MembersByOwner;
import com.sonatype.insight.brain.security.MembershipMappingResource.MembersByRole;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.utils.IdUtils;

import com.ning.http.client.Response;
import com.yammer.dropwizard.testing.JsonHelpers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.sonatype.insight.brain.ldap.EmbeddedLdapServer.newEmbeddedLdapServer;
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

  private RoleDAO roleDAO = new RoleDAO();

  private UserDAO userDAO = new UserDAO();

  private EmbeddedLdapServer embeddedLdapServer;

  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  private String getServiceUrl(final String ownerType, final String ownerId) {
    return getRestUrl(MembershipMappingResource.SERVICE_PATH, ownerType, ownerId);
  }

  private String getServiceUrl(String ownerType, String ownerId, String roleId) {
    return getRestUrl(MembershipMappingResource.SERVICE_PATH + '/' + MembershipMappingResource.ROLE_PATH, ownerType,
        ownerId, roleId);
  }

  private Member newMember(MemberType type, String name) {
    return new Member(type, name, null);
  }

  @Before
  public void init() throws Exception {
    org = createOrganization("test-org");
    app = createApplication("test-app", "test-app", org);
    userA = new User("user-a", "secret", "John", "Doe", "void@void.com");
    userDAO.insert(userA);
    userB = new User("user-b", "secret", "Jane", "Doe", "void@void.com");
    userDAO.insert(userB);
  }

  @After
  public void exit() throws Exception {
    if (userA != null) {
      userDAO.delete(userA);
    }
    if (userB != null) {
      userDAO.delete(userB);
    }
    if (embeddedLdapServer != null) {
      embeddedLdapServer.stop();
      embeddedLdapServer = null;
    }
  }

  @Test
  public void testCRUD_AppRoles() throws Exception {
    // Initial state
    Response response = AuthedRestAccess.get(getServiceUrl(IdUtils.TYPE_APPLICATION, app.getPublicId()));
    assertResponseStatus(200, response);
    ApplicableMembershipMappings applicable = JsonHelpers.fromJson(response.getResponseBody(),
        ApplicableMembershipMappings.class);
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
      assertThat(membersByRole.membersByOwner.get(0).ownerId, is(app.getPublicId()));
      assertThat(membersByRole.membersByOwner.get(0).members, is(notNullValue()));
      assertThat(membersByRole.membersByOwner.get(1).ownerId, is(org.getId()));
    }

    // Create
    response = AuthedRestAccess.put(
        getServiceUrl(IdUtils.TYPE_ORGANIZATION, app.getOrganizationId(), appRoles.get(0).getId()),
        JsonHelpers.asJson(Arrays.asList(newMember(MemberType.USER, userB.getUsername()))));
    assertResponseStatus(204, response);

    // Read for created data
    response = AuthedRestAccess.get(getServiceUrl(IdUtils.TYPE_ORGANIZATION, app.getOrganizationId()));
    assertResponseStatus(200, response);
    applicable = JsonHelpers.fromJson(response.getResponseBody(), ApplicableMembershipMappings.class);

    assertThat(applicable, is(notNullValue()));
    assertThat(applicable.membersByRole, is(notNullValue()));
    assertThat(applicable.membersByRole, hasSize(appRoles.size()));

    MembersByRole membersByRole = applicable.membersByRole.get(0);
    assertThat(membersByRole.roleId, is(appRoles.get(0).getId()));
    assertThat(membersByRole.membersByOwner, is(notNullValue()));
    assertThat(membersByRole.membersByOwner, hasSize(1));
    MembersByOwner membersByOwner = membersByRole.membersByOwner.get(0);
    assertThat(membersByOwner.ownerId, is(org.getId()));
    assertThat(membersByOwner.ownerName, is(org.getName()));
    assertThat(membersByOwner.ownerType, is(IdUtils.TYPE_ORGANIZATION));
    assertThat(membersByOwner.members, is(notNullValue()));
    assertThat(membersByOwner.members, hasSize(1));
    assertThat(membersByOwner.members.get(0).internalName, is(userB.getUsername()));
    assertThat(membersByOwner.members.get(0).displayName, is(userB.getFirstName() + " " + userB.getLastName()));
    assertThat(membersByOwner.members.get(0).email, is(userB.getEmail()));
    assertThat(membersByOwner.members.get(0).realm, is("CLM"));
    assertThat(membersByOwner.members.get(0).type, is(MemberType.USER));

    // Update
    response = AuthedRestAccess.put(
        getServiceUrl(IdUtils.TYPE_APPLICATION, app.getPublicId(), appRoles.get(0).getId()),
        JsonHelpers.asJson(Arrays.asList(newMember(MemberType.USER, userA.getUsername()))));
    assertResponseStatus(204, response);
    response = AuthedRestAccess.put(
        getServiceUrl(IdUtils.TYPE_APPLICATION, app.getPublicId(), appRoles.get(1).getId()),
        JsonHelpers.asJson(Arrays.asList(newMember(MemberType.USER, userB.getUsername()))));
    assertResponseStatus(204, response);

    // Read for updated data
    response = AuthedRestAccess.get(getServiceUrl(IdUtils.TYPE_APPLICATION, app.getPublicId()));
    assertResponseStatus(200, response);
    applicable = JsonHelpers.fromJson(response.getResponseBody(), ApplicableMembershipMappings.class);
    assertThat(applicable, is(notNullValue()));
    assertThat(applicable.membersByRole, is(notNullValue()));
    assertThat(applicable.membersByRole, hasSize(appRoles.size()));

    membersByRole = applicable.membersByRole.get(0);
    assertThat(membersByRole.roleId, is(appRoles.get(0).getId()));
    assertThat(membersByRole.membersByOwner, is(notNullValue()));
    assertThat(membersByRole.membersByOwner, hasSize(2));
    membersByOwner = membersByRole.membersByOwner.get(0);
    assertThat(membersByOwner.ownerId, is(app.getPublicId()));
    assertThat(membersByOwner.ownerName, is(app.getName()));
    assertThat(membersByOwner.ownerType, is(IdUtils.TYPE_APPLICATION));
    assertThat(membersByOwner.members, is(notNullValue()));
    assertThat(membersByOwner.members, hasSize(1));
    assertThat(membersByOwner.members.get(0).internalName, is(userA.getUsername()));
    assertThat(membersByOwner.members.get(0).displayName, is(userA.getFirstName() + " " + userA.getLastName()));
    assertThat(membersByOwner.members.get(0).email, is(userA.getEmail()));
    assertThat(membersByOwner.members.get(0).realm, is("CLM"));
    assertThat(membersByOwner.members.get(0).type, is(MemberType.USER));
    membersByOwner = membersByRole.membersByOwner.get(1);
    assertThat(membersByOwner.ownerId, is(org.getId()));
    assertThat(membersByOwner.ownerName, is(org.getName()));
    assertThat(membersByOwner.ownerType, is(IdUtils.TYPE_ORGANIZATION));
    assertThat(membersByOwner.members, is(notNullValue()));
    assertThat(membersByOwner.members, hasSize(1));
    assertThat(membersByOwner.members.get(0).internalName, is(userB.getUsername()));
    assertThat(membersByOwner.members.get(0).displayName, is(userB.getFirstName() + " " + userB.getLastName()));
    assertThat(membersByOwner.members.get(0).email, is(userB.getEmail()));
    assertThat(membersByOwner.members.get(0).realm, is("CLM"));
    assertThat(membersByOwner.members.get(0).type, is(MemberType.USER));
    membersByRole = applicable.membersByRole.get(1);
    assertThat(membersByRole.roleId, is(appRoles.get(1).getId()));
    assertThat(membersByRole.membersByOwner, is(notNullValue()));
    assertThat(membersByRole.membersByOwner, hasSize(2));
    membersByOwner = membersByRole.membersByOwner.get(0);
    assertThat(membersByOwner.ownerId, is(app.getPublicId()));
    assertThat(membersByOwner.ownerName, is(app.getName()));
    assertThat(membersByOwner.ownerType, is(IdUtils.TYPE_APPLICATION));
    assertThat(membersByOwner.members, is(notNullValue()));
    assertThat(membersByOwner.members, hasSize(1));
    assertThat(membersByOwner.members.get(0).internalName, is(userB.getUsername()));
    assertThat(membersByOwner.members.get(0).displayName, is(userB.getFirstName() + " " + userB.getLastName()));
    assertThat(membersByOwner.members.get(0).email, is(userB.getEmail()));
    assertThat(membersByOwner.members.get(0).realm, is("CLM"));
    assertThat(membersByOwner.members.get(0).type, is(MemberType.USER));
    membersByOwner = membersByRole.membersByOwner.get(1);
    assertThat(membersByOwner.ownerId, is(org.getId()));
    assertThat(membersByOwner.ownerName, is(org.getName()));
    assertThat(membersByOwner.ownerType, is(IdUtils.TYPE_ORGANIZATION));
    assertThat(membersByOwner.members, is(notNullValue()));
    assertThat(membersByOwner.members, hasSize(0));
  }

  @Test
  public void testLdap_GlobalRoles() throws Exception {
    embeddedLdapServer = newEmbeddedLdapServer();
    embeddedLdapServer.start();
    embeddedLdapServer.loadData("/UserResourceTest/ldap_users.ldif");

    LdapServer ldapServer = tempEntity.newLdapServer("LDAP");
    tempEntity.newLdapConnection(ldapServer.getId(), embeddedLdapServer.getPort());
    tempEntity.newLdapUserMapping(ldapServer.getId());

    // Initial state
    Response response = AuthedRestAccess.get(getServiceUrl(IdUtils.TYPE_GLOBAL, MembershipMapping.GLOBAL_CONTEXT_ID));
    List<Role> roles = testInitialGlobalState(response);

    // Create
    response = AuthedRestAccess.put(
        getServiceUrl(IdUtils.TYPE_GLOBAL, MembershipMapping.GLOBAL_CONTEXT_ID, roles.get(0).getId()),
        JsonHelpers.asJson(
            Arrays.asList(newMember(MemberType.USER, User.ADMIN_USERNAME), newMember(MemberType.USER, "testuser"),
                newMember(MemberType.GROUP, "Alpha"))));
    assertResponseStatus(204, response);

    // Read for created data
    response = AuthedRestAccess.get(getServiceUrl(IdUtils.TYPE_GLOBAL, MembershipMapping.GLOBAL_CONTEXT_ID));
    assertResponseStatus(200, response);
    ApplicableMembershipMappings applicable = JsonHelpers.fromJson(response.getResponseBody(), ApplicableMembershipMappings.class);

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
    assertThat(membersByOwner.ownerType, is(IdUtils.TYPE_GLOBAL));
    assertThat(membersByOwner.members, is(notNullValue()));
    assertThat(membersByOwner.members, hasSize(3));

    Collections.sort(membersByOwner.members, new MemberComparator());
    assertThat(membersByOwner.members.get(0).internalName, is(User.ADMIN_USERNAME));
    assertThat(membersByOwner.members.get(0).type, is(MemberType.USER));
    assertThat(membersByOwner.members.get(1).internalName, is("Alpha"));
    assertThat(membersByOwner.members.get(1).displayName, is("Alpha"));
    assertThat(membersByOwner.members.get(1).realm, is("LDAP"));
    assertThat(membersByOwner.members.get(1).type, is(MemberType.GROUP));
    assertThat(membersByOwner.members.get(2).internalName, is("testuser"));
    assertThat(membersByOwner.members.get(2).displayName, is("John Doe"));
    assertThat(membersByOwner.members.get(2).email, is("test.user@company.com"));
    assertThat(membersByOwner.members.get(2).realm, is("LDAP"));
    assertThat(membersByOwner.members.get(2).type, is(MemberType.USER));

    // Reset Initial State
    response = AuthedRestAccess.put(
        getServiceUrl(IdUtils.TYPE_GLOBAL, MembershipMapping.GLOBAL_CONTEXT_ID, roles.get(0).getId()),
        JsonHelpers.asJson(Arrays.asList(newMember(MemberType.USER, User.ADMIN_USERNAME))));
    assertResponseStatus(204, response);
  }

  @Test
  public void testCRUD_GlobalRoles() throws Exception {
    // Initial state
    Response response = AuthedRestAccess.get(getServiceUrl(IdUtils.TYPE_GLOBAL, MembershipMapping.GLOBAL_CONTEXT_ID));
    List<Role> roles = testInitialGlobalState(response);

    // Create
    response = AuthedRestAccess.put(
        getServiceUrl(IdUtils.TYPE_GLOBAL, MembershipMapping.GLOBAL_CONTEXT_ID, roles.get(0).getId()),
        JsonHelpers.asJson(Arrays.asList(newMember(MemberType.USER, User.ADMIN_USERNAME), newMember(MemberType.USER, userB.getUsername()))));
    assertResponseStatus(204, response);

    // Read for created data
    response = AuthedRestAccess.get(getServiceUrl(IdUtils.TYPE_GLOBAL, MembershipMapping.GLOBAL_CONTEXT_ID));
    assertResponseStatus(200, response);
    ApplicableMembershipMappings applicable = JsonHelpers.fromJson(response.getResponseBody(), ApplicableMembershipMappings.class);

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
    assertThat(membersByOwner.ownerType, is(IdUtils.TYPE_GLOBAL));
    assertThat(membersByOwner.members, is(notNullValue()));
    assertThat(membersByOwner.members, hasSize(2));

    Collections.sort(membersByOwner.members, new MemberComparator());
    assertThat(membersByOwner.members.get(0).internalName, is(User.ADMIN_USERNAME));
    assertThat(membersByOwner.members.get(0).type, is(MemberType.USER));
    assertThat(membersByOwner.members.get(1).internalName, is(userB.getUsername()));
    assertThat(membersByOwner.members.get(1).displayName, is(userB.getFirstName() + " " + userB.getLastName()));
    assertThat(membersByOwner.members.get(1).email, is(userB.getEmail()));
    assertThat(membersByOwner.members.get(1).realm, is("CLM"));
    assertThat(membersByOwner.members.get(1).type, is(MemberType.USER));

    // Update
    response = AuthedRestAccess.put(
        getServiceUrl(IdUtils.TYPE_GLOBAL, MembershipMapping.GLOBAL_CONTEXT_ID, roles.get(0).getId()),
        JsonHelpers.asJson(Arrays.asList(newMember(MemberType.USER, User.ADMIN_USERNAME))));
    assertResponseStatus(204, response);

    // Read for updated data
    response = AuthedRestAccess.get(getServiceUrl(IdUtils.TYPE_GLOBAL, MembershipMapping.GLOBAL_CONTEXT_ID));
    assertResponseStatus(200, response);
    applicable = JsonHelpers.fromJson(response.getResponseBody(), ApplicableMembershipMappings.class);
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
    assertThat(membersByOwner.ownerType, is(IdUtils.TYPE_GLOBAL));
    assertThat(membersByOwner.members, is(notNullValue()));
    assertThat(membersByOwner.members, hasSize(1));
    assertThat(membersByOwner.members.get(0).internalName, is(User.ADMIN_USERNAME));
    assertThat(membersByOwner.members.get(0).type, is(MemberType.USER));

    // Reset Initial State
    response = AuthedRestAccess.put(
        getServiceUrl(IdUtils.TYPE_GLOBAL, MembershipMapping.GLOBAL_CONTEXT_ID, roles.get(0).getId()),
        JsonHelpers.asJson(Arrays.asList(newMember(MemberType.USER, User.ADMIN_USERNAME))));
    assertResponseStatus(204, response);
  }


  private List<Role> testInitialGlobalState(Response response) throws IOException {
    assertResponseStatus(200, response);
    ApplicableMembershipMappings applicable = JsonHelpers.fromJson(response.getResponseBody(),
        ApplicableMembershipMappings.class);
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
      assertThat(membersByOwner.ownerType, is(IdUtils.TYPE_GLOBAL));
      assertThat(membersByOwner.members, is(notNullValue()));
      assertThat(membersByOwner.members, hasSize(1));
      assertThat(membersByOwner.members.get(0).internalName, is(User.ADMIN_USERNAME));
      assertThat(membersByOwner.members.get(0).type, is(MemberType.USER));
    }

    return roles;
  }

  @Test
  public void testAdministratorRoleCantBeRevokedFromAllUsers() throws Exception {
    Role admin = roleDAO.getByName("Administrator");
    assertThat(admin, is(notNullValue()));

    Response response = AuthedRestAccess.put(
        getServiceUrl(IdUtils.TYPE_GLOBAL, MembershipMapping.GLOBAL_CONTEXT_ID, admin.getId()),
        toJson(Collections.emptyList()));
    assertResponseStatus(400, response);
    assertThat(response.getResponseBody(), is("There must be at least one user in the administrator role."));
  }

  private static class MemberComparator implements Comparator<Member> {
    @Override
    public int compare(final Member member, final Member otherMember) {
      int nameComp = member.displayName.compareTo(otherMember.displayName);
      if (nameComp != 0) {
        return nameComp;
      } else {
        return member.type.compareTo(otherMember.type);
      }
    }
  }
}
