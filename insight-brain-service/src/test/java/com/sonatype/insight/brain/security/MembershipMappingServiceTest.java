/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiMemberDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRoleMemberMappingDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRoleMemberMappingListDTO;
import com.sonatype.insight.brain.configuration.ldap.LdapService;
import com.sonatype.insight.brain.configuration.ldap.TestLdapServer;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapUserMappingDAO;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.ldap.LdapConnection;
import com.sonatype.insight.brain.model.configuration.ldap.LdapGroupMappingType;
import com.sonatype.insight.brain.model.configuration.ldap.LdapProtocol;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.model.configuration.ldap.LdapUserMapping;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.webhook.ManagementEvent.RoleEvent;
import com.sonatype.insight.brain.webhook.TestEventHandler;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static com.sonatype.insight.brain.model.security.Role.APPLICATION_EVALUATOR_ROLE_ID;
import static com.sonatype.insight.brain.model.security.Role.DEVELOPER_ROLE_ID;
import static com.sonatype.insight.brain.model.security.Role.POLICY_ADMIN_ROLE_ID;
import static com.sonatype.insight.brain.model.security.Role.SYSTEM_ADMIN_ROLE_ID;
import static com.sonatype.insight.brain.webhook.EventAction.CREATED;
import static com.sonatype.insight.brain.webhook.EventAction.DELETED;
import static com.sonatype.insight.brain.webhook.EventAction.UPDATED;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class MembershipMappingServiceTest
    extends AbstractComponentTest
{
  @Inject
  private MembershipMappingService membershipMappingService;

  @Inject
  private AsyncEventBus eventBus;

  @Inject
  private MembershipMappingDAO membershipMappingDAO;

  @Inject
  private LdapUserMappingDAO ldapUserMappingDAO;

  @Inject
  private RoleDAO roleDAO;

  @Inject
  private OwnerDAO ownerDAO;

  @Inject
  private UserDAO userDAO;

  @Inject
  private LdapService ldapService;

  @Rule
  public TestLdapServer testLdapServer = new TestLdapServer();

  private TestEventHandler<RoleEvent> handler;

  @After
  public void after() {
    if (handler != null) {
      eventBus.unregister(handler);
    }
  }

  @Test
  public void testLoadMembersByRoleForNonGlobalContext_GlobalContext() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> membershipMappingService.loadMembersByRoleForNonGlobalContext(OwnerType.GLOBAL, "ownerId",
            null /* roles */, null/* membersByRoleByRoleId */)).withMessage("The 'global' context is not allowed.");
  }

  @Test
  public void testSetMembershipMappingsForNonGlobalContext_GlobalContext() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> membershipMappingService.setMembershipMappingsForNonGlobalContext(OwnerType.GLOBAL, "ownerId",
            null /* roleToMembers */)).withMessage("The 'global' context is not allowed.");
  }

  @Test
  public void testSetMembershipMappings_PostsEvent() throws Exception {
    handler = new TestEventHandler<>(new CountDownLatch(1), RoleEvent.class);
    eventBus.register(handler);

    Role role = tempEntity.newRole(false, Permission.READ);
    tempEntity.newMembershipMapping(ROOT_ORGANIZATION_ID, role.getId(), "username");
    Member member = new Member(MemberType.USER, "username", "username");

    Map<String, List<Member>> roleToMembers = Collections.singletonMap(role.getId(), Collections.singletonList(member));
    membershipMappingService.setMembershipMappings(OwnerType.ORGANIZATION, ROOT_ORGANIZATION_ID, roleToMembers);

    assertThat(handler.getLatch().await(5, SECONDS)).isTrue();
    assertThat(handler.getEvent().action).isEqualTo(UPDATED);
  }

  @Test
  public void testGetApplicableMembershipMappings_ApplicationContext() throws Exception {
    testGetApplicableMembershipMappings(tempEntity.newApplicationWithParent());
  }

  @Test
  public void testGetApplicableMembershipMappings_OrganizationContext() throws Exception {
    testGetApplicableMembershipMappings(tempEntity.newOrganization());
  }

  @Test
  public void testGetApplicableMembershipMappings_RepositoryContainerContext() throws Exception {
    testGetApplicableMembershipMappings(RepositoryContainer.SINGLETON);
  }

  @Test
  public void testGetApplicableMembershipMappings_RepositoryManagerContext() throws Exception {
    testGetApplicableMembershipMappings(tempEntity.newRepositoryManager());
  }

  @Test
  public void testGetApplicableMembershipMappings_RepositoryContext() throws Exception {
    testGetApplicableMembershipMappings(tempEntity.newRepository());
  }

  private void testGetApplicableMembershipMappings(Owner owner) throws Exception {
    startLdapServer();

    tempEntity.newMembershipMapping(owner.getId(), Role.DEVELOPER_ROLE_ID, "test_user1", MemberType.USER);
    tempEntity.newMembershipMapping(owner.getId(), Role.COMPONENT_EVALUATOR_ROLE_ID, "test_group1", MemberType.GROUP);
    User user = tempEntity.newUser("internal_user");
    tempEntity.newMembershipMapping(owner.getId(), Role.APPLICATION_EVALUATOR_ROLE_ID, "internal_user",
        MemberType.USER);

    ApplicableMembershipMappings applicableMembershipMappings =
        membershipMappingService.getApplicableMembershipMappings(owner.getType(), owner.getId());

    assertThat(applicableMembershipMappings.membersByRole).hasSize(5);

    MembersByRole membersByRoles = applicableMembershipMappings.membersByRole.get(0);
    Member expectedMember = new Member(MemberType.USER, user.getUsername(), user.calculateDisplayName(),
        user.getEmail(), InternalRealm.DISPLAY_NAME);
    assertMembersByRoleOwner(membersByRoles, roleDAO.getById(Role.APPLICATION_EVALUATOR_ROLE_ID), owner,
        expectedMember);

    membersByRoles = applicableMembershipMappings.membersByRole.get(1);
    expectedMember = new Member(MemberType.GROUP, "test_group1", "test_group1", null, null);
    assertMembersByRoleOwner(membersByRoles, roleDAO.getById(Role.COMPONENT_EVALUATOR_ROLE_ID), owner,
        expectedMember);

    membersByRoles = applicableMembershipMappings.membersByRole.get(2);
    expectedMember =
        new Member(MemberType.USER, "test_user1", "Test User1", "test.user1@example.com", "Test LDAP Server");
    assertMembersByRoleOwner(membersByRoles, roleDAO.getById(Role.DEVELOPER_ROLE_ID), owner, expectedMember);

    membersByRoles = applicableMembershipMappings.membersByRole.get(3);
    assertMembersByRoleOwner(membersByRoles, roleDAO.getById(Role.LEGAL_REVIEWER_ROLE_ID), owner, null);

    membersByRoles = applicableMembershipMappings.membersByRole.get(4);
    assertMembersByRoleOwner(membersByRoles, roleDAO.getById(Role.OWNER_ROLE_ID), owner, null);
  }

  @Test
  public void testGetApplicableMembershipMappings_GlobalContext() throws Exception {
    startLdapServer();

    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, Role.SYSTEM_ADMIN_ROLE_ID, "test_user1",
        MemberType.USER);
    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, Role.POLICY_ADMIN_ROLE_ID, "test_group1",
        MemberType.GROUP);

    ApplicableMembershipMappings applicableMembershipMappings =
        membershipMappingService.getApplicableMembershipMappings(OwnerType.GLOBAL, MembershipMapping.GLOBAL_CONTEXT_ID);

    assertThat(applicableMembershipMappings.membersByRole).hasSize(2);

    MembersByRole membersByRoles = applicableMembershipMappings.membersByRole.get(0);
    Member expectedMember = new Member(MemberType.GROUP, "test_group1", "test_group1", null, null);
    assertMembersByRoleGlobal(membersByRoles, roleDAO.getById(Role.POLICY_ADMIN_ROLE_ID), expectedMember);

    membersByRoles = applicableMembershipMappings.membersByRole.get(1);
    expectedMember =
        new Member(MemberType.USER, "test_user1", "Test User1", "test.user1@example.com", "Test LDAP Server");
    assertMembersByRoleGlobal(membersByRoles, roleDAO.getById(Role.SYSTEM_ADMIN_ROLE_ID),
        expectedMember);
  }

  private void assertMembersByRoleOwner(
      MembersByRole membersByRoles,
      Role expectedRole,
      Owner expectedOwner,
      Member expectedMember)
  {
    assertThat(membersByRoles.roleId).isEqualTo(expectedRole.getId());
    assertThat(membersByRoles.roleName).isEqualTo(expectedRole.getName());
    assertThat(membersByRoles.roleDescription).isEqualTo(expectedRole.getDescription());

    int ownerHierarchyDepth = 0;
    for (Owner owner : ownerDAO.walkHierarchy(expectedOwner)) {
      MembersByOwner membersByOwner = membersByRoles.membersByOwner.get(ownerHierarchyDepth);
      assertThat(membersByOwner.ownerType).isEqualTo(owner.getType());
      if (OwnerType.APPLICATION.equals(owner.getType())) {
        assertThat(membersByOwner.ownerId).isEqualTo(owner.getPublicId());
      }
      else {
        assertThat(membersByOwner.ownerId).isEqualTo(owner.getId());
      }
      assertThat(membersByOwner.ownerName).isEqualTo(owner.getName());

      if (expectedOwner.getId().equals(owner.getId()) && expectedMember != null) {
        assertThat(membersByOwner.members).hasSize(1);
        Member member = membersByOwner.members.get(0);
        assertThat(member.getType()).isEqualTo(expectedMember.getType());
        assertThat(member.getInternalName()).isEqualTo(expectedMember.getInternalName());
        assertThat(member.getDisplayName()).isEqualTo(expectedMember.getDisplayName());
        assertThat(member.getEmail()).isEqualTo(expectedMember.getEmail());
        assertThat(member.getRealm()).isEqualTo(expectedMember.getRealm());
      }
      else {
        assertThat(membersByOwner.members).hasSize(0);
      }

      ownerHierarchyDepth++;
    }
    assertThat(membersByRoles.membersByOwner).hasSize(ownerHierarchyDepth);
  }

  private void assertMembersByRoleGlobal(
      MembersByRole membersByRoles,
      Role expectedRole,
      Member expectedMember)
  {
    assertThat(membersByRoles.roleId).isEqualTo(expectedRole.getId());
    assertThat(membersByRoles.roleName).isEqualTo(expectedRole.getName());
    assertThat(membersByRoles.roleDescription).isEqualTo(expectedRole.getDescription());

    assertThat(membersByRoles.membersByOwner).hasSize(1);
    MembersByOwner membersByOwner = membersByRoles.membersByOwner.get(0);
    assertThat(membersByOwner.ownerId).isEqualTo(MembershipMapping.GLOBAL_CONTEXT_ID);
    assertThat(membersByOwner.ownerName).isEqualTo(MembershipMapping.GLOBAL_CONTEXT_NAME);
    assertThat(membersByOwner.ownerType).isEqualTo(OwnerType.GLOBAL);

    assertThat(membersByOwner.members).hasSize(2);
    Member member = membersByOwner.members.get(0);
    User admin = userDAO.getByUsername(User.ADMIN_USERNAME);
    assertThat(member.getType()).isEqualTo(MemberType.USER);
    assertThat(member.getInternalName()).isEqualTo(admin.getUsername());
    assertThat(member.getDisplayName()).isEqualTo(admin.calculateDisplayName());
    assertThat(member.getEmail()).isEqualTo(admin.getEmail());
    assertThat(member.getRealm()).isEqualTo(InternalRealm.DISPLAY_NAME);
    member = membersByOwner.members.get(1);
    assertThat(member.getType()).isEqualTo(expectedMember.getType());
    assertThat(member.getInternalName()).isEqualTo(expectedMember.getInternalName());
    assertThat(member.getDisplayName()).isEqualTo(expectedMember.getDisplayName());
    assertThat(member.getEmail()).isEqualTo(expectedMember.getEmail());
    assertThat(member.getRealm()).isEqualTo(expectedMember.getRealm());
  }

  private void startLdapServer() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test LDAP Server");

    testLdapServer.start();
    testLdapServer.loadData("/" + getClass().getSimpleName() + "/ldap.ldif");

    ldapService.upsertLdapConnection(createLdapConnection(ldapServer, testLdapServer));

    ldapUserMappingDAO.insert(createUserMapping(ldapServer));
  }

  private LdapConnection createLdapConnection(LdapServer ldapServer, TestLdapServer testLdapServer) {
    LdapConnection ldapConnection = ldapService.getLdapConnection(ldapServer.getId());
    ldapConnection.setServerId(ldapServer.getId());
    ldapConnection.setProtocol(LdapProtocol.LDAP);
    ldapConnection.setSearchBase("dc=company,dc=com");
    if (testLdapServer != null) {
      ldapConnection.setHostname(testLdapServer.getHostname());
      ldapConnection.setPort(testLdapServer.getPort());
    }
    return ldapConnection;
  }

  private LdapUserMapping createUserMapping(LdapServer ldapServer) {
    LdapUserMapping ldapUserMapping = new LdapUserMapping();
    ldapUserMapping.setServerId(ldapServer.getId());
    ldapUserMapping.setUserBaseDN("ou=users");
    ldapUserMapping.setUserObjectClass("person");
    ldapUserMapping.setUserIDAttribute("uid");
    ldapUserMapping.setUserRealNameAttribute("cn");
    ldapUserMapping.setUserEmailAttribute("mail");
    ldapUserMapping.setUserSubtree(true);
    ldapUserMapping.setGroupBaseDN("ou=groups");
    ldapUserMapping.setGroupIDAttribute("cn");
    ldapUserMapping.setGroupSubtree(true);
    ldapUserMapping.setGroupMappingType(LdapGroupMappingType.DYNAMIC);
    ldapUserMapping.setUserMemberOfGroupAttribute("departmentNumber");
    return ldapUserMapping;
  }

  @Test
  public void testGetApplicableMembershipMappings_DynamicGroupSearchAllEnabled() {
    setupLdapWithDynamicGroupType("test server 1", true);
    setupLdapWithDynamicGroupType("test server 2", true);

    ApplicableMembershipMappings actual = membershipMappingService
        .getApplicableMembershipMappings(OwnerType.ORGANIZATION, ROOT_ORGANIZATION_ID);

    assertThat(actual.groupSearchEnabled).isTrue();
  }

  @Test
  public void testGetApplicableMembershipMappings_MixedDynamicGroupSearch() {
    setupLdapWithDynamicGroupType("test server 1", false);
    setupLdapWithDynamicGroupType("test server 2", true);

    ApplicableMembershipMappings actual = membershipMappingService
        .getApplicableMembershipMappings(OwnerType.ORGANIZATION, ROOT_ORGANIZATION_ID);

    assertThat(actual.groupSearchEnabled).isFalse();
  }

  @Test
  public void testGetApplicableMembershipMappings_MixedGroupSearch() {
    setupLdapWithNonDynamicGroupType("test server 1", LdapGroupMappingType.STATIC);
    setupLdapWithNonDynamicGroupType("test server 2", LdapGroupMappingType.NONE);
    setupLdapWithDynamicGroupType("test server 3", false);

    ApplicableMembershipMappings actual = membershipMappingService
        .getApplicableMembershipMappings(OwnerType.ORGANIZATION, ROOT_ORGANIZATION_ID);

    assertThat(actual.groupSearchEnabled).isFalse();
  }

  @Test
  public void testGrantRoleMembership_NonGlobal() throws InterruptedException {
    handler = new TestEventHandler<>(new CountDownLatch(1), RoleEvent.class);
    eventBus.register(handler);

    String username = tempEntity.newUser("a-user").getUsername();
    String applicationId = tempEntity.newApplicationWithParent().getId();

    membershipMappingService
        .grantRoleMembership(OwnerType.APPLICATION, applicationId, Role.DEVELOPER_ROLE_ID, MemberType.USER,
            username);

    MembershipMapping membershipMapping = membershipMappingDAO
        .getByContextIdAndRoleIdAndMemberNameAndMemberType(applicationId, Role.DEVELOPER_ROLE_ID, username,
            MemberType.USER);
    assertThat(membershipMapping.getMemberName()).isEqualTo(username);
    assertThat(membershipMapping.getMemberType()).isEqualTo(MemberType.USER);
    assertThat(membershipMapping.getRoleId()).isEqualTo(Role.DEVELOPER_ROLE_ID);
    assertThat(membershipMapping.getContextId()).isEqualTo(applicationId);

    assertThat(handler.getLatch().await(5, SECONDS)).isTrue();
    assertThat(handler.getEvent().action).isEqualTo(CREATED);

    Member member = handler.getEvent().roleIdToMemberMap.entrySet().iterator().next().getValue().get(0);
    assertThat(member.getInternalName()).isEqualTo(membershipMapping.getMemberName());
    assertThat(member.getType()).isEqualTo(membershipMapping.getMemberType());
  }

  @Test
  public void testGrantRoleMembership_Global() {
    String username = tempEntity.newUser("a-user").getUsername();

    membershipMappingService
        .grantRoleMembership(OwnerType.GLOBAL, MembershipMapping.GLOBAL_CONTEXT_ID, Role.SYSTEM_ADMIN_ROLE_ID,
            MemberType.USER, username);

    MembershipMapping membershipMapping = membershipMappingDAO
        .getByContextIdAndRoleIdAndMemberNameAndMemberType(MembershipMapping.GLOBAL_CONTEXT_ID,
            Role.SYSTEM_ADMIN_ROLE_ID, username, MemberType.USER);

    assertThat(membershipMapping.getMemberName()).isEqualTo(username);
    assertThat(membershipMapping.getMemberType()).isEqualTo(MemberType.USER);
    assertThat(membershipMapping.getRoleId()).isEqualTo(Role.SYSTEM_ADMIN_ROLE_ID);
    assertThat(membershipMapping.getContextId()).isEqualTo(MembershipMapping.GLOBAL_CONTEXT_ID);
  }

  @Test
  public void testGrantRoleMembership_AlreadyExisting() {
    Application application = tempEntity.newApplicationWithParent();
    String username = tempEntity.newUser("a-user").getUsername();

    String contextId = application.getId();
    String memberName = username;
    MemberType memberType = MemberType.USER;

    tempEntity.newMembershipMapping(contextId, Role.DEVELOPER_ROLE_ID, memberName, memberType);

    membershipMappingService
        .grantRoleMembership(OwnerType.APPLICATION, contextId, Role.DEVELOPER_ROLE_ID, memberType, memberName);
  }

  @Test
  public void testGrantRoleMembership_NoMemberName() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(
            () -> membershipMappingService
                .grantRoleMembership(OwnerType.APPLICATION, "owner-id", Role.DEVELOPER_ROLE_ID, MemberType.USER, ""))
        .withMessageContaining("Internal name of role member has not been specified");
  }

  @Test
  public void testGrantRoleMembership_NoMemberType() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> membershipMappingService
            .grantRoleMembership(OwnerType.APPLICATION, "owner-id", Role.DEVELOPER_ROLE_ID, null, "username"))
        .withMessageContaining("Type of role member has not been specified");
  }

  @Test
  public void testGrantRoleMembership_UnknownContextId() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> membershipMappingService
            .grantRoleMembership(OwnerType.APPLICATION, "owner-id", Role.DEVELOPER_ROLE_ID, MemberType.USER,
                "username"))
        .withMessageContaining("Application with ID owner-id does not exist.");
  }

  @Test
  public void testGrantRoleMembership_ContextTypeAndIdMismatch() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> membershipMappingService
            .grantRoleMembership(OwnerType.ORGANIZATION, "no-such-application", Role.DEVELOPER_ROLE_ID,
                MemberType.USER, "username"))
        .withMessageContaining("Organization with ID no-such-application does not exist.");
  }

  @Test
  public void testGrantRoleMembership_RoleValidationOwnerNotGlobalRoleGlobal() {
    String username = tempEntity.newUser("a-user").getUsername();
    String applicationId = tempEntity.newApplicationWithParent().getId();

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> membershipMappingService
            .grantRoleMembership(OwnerType.APPLICATION, applicationId, Role.SYSTEM_ADMIN_ROLE_ID, MemberType.USER,
                username))
        .withMessageContaining("Cannot map members to global role in context of application.");
  }

  @Test
  public void testGrantRoleMembership_RoleValidationOwnerGlobalRoleNotGlobal() {
    String username = tempEntity.newUser("a-user").getUsername();

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> membershipMappingService
            .grantRoleMembership(OwnerType.GLOBAL, MembershipMapping.GLOBAL_CONTEXT_ID, Role.DEVELOPER_ROLE_ID,
                MemberType.USER, username))
        .withMessageContaining("Cannot map members to application role in global context.");
  }

  @Test
  public void testRevokeRoleMembership() throws InterruptedException {
    handler = new TestEventHandler<>(new CountDownLatch(1), RoleEvent.class);
    eventBus.register(handler);

    String contextId = tempEntity.newApplicationWithParent().getId();
    String memberName = tempEntity.newUser("a-user").getUsername();
    MemberType memberType = MemberType.USER;

    MembershipMapping membershipMapping =
        tempEntity.newMembershipMapping(contextId, Role.DEVELOPER_ROLE_ID, memberName, memberType);

    membershipMappingService
        .revokeRoleMembership(OwnerType.APPLICATION, contextId, Role.DEVELOPER_ROLE_ID, memberType, memberName);
    assertThat(membershipMappingDAO.getById(membershipMapping.getId())).isNull();

    assertThat(handler.getLatch().await(5, SECONDS)).isTrue();
    assertThat(handler.getEvent().action).isEqualTo(DELETED);

    Member member = handler.getEvent().roleIdToMemberMap.entrySet().iterator().next().getValue().get(0);
    assertThat(member.getInternalName()).isEqualTo(membershipMapping.getMemberName());
    assertThat(member.getType()).isEqualTo(membershipMapping.getMemberType());
  }

  @Test
  public void testRevokeRoleMembership_Global() {
    String contextId = MembershipMapping.GLOBAL_CONTEXT_ID;
    String memberName = tempEntity.newUser("a-user").getUsername();
    MemberType memberType = MemberType.USER;

    MembershipMapping membershipMapping =
        tempEntity.newMembershipMapping(contextId, Role.POLICY_ADMIN_ROLE_ID, memberName, memberType);

    membershipMappingService
        .revokeRoleMembership(OwnerType.GLOBAL, contextId, Role.POLICY_ADMIN_ROLE_ID, memberType, memberName);
    assertThat(membershipMappingDAO.getById(membershipMapping.getId())).isNull();
  }

  @Test
  public void testRevokeRoleMembership_NotExisting() {
    String contextId = tempEntity.newApplicationWithParent().getId();
    String memberName = tempEntity.newUser("a-user").getUsername();
    MemberType memberType = MemberType.USER;

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> membershipMappingService
            .revokeRoleMembership(OwnerType.APPLICATION, contextId, Role.DEVELOPER_ROLE_ID, memberType, memberName))
        .withMessageContaining("Role membership not found.");
  }

  @Test
  public void testGetRoleMembershipsOmitEmpty_InheritedMembers() {
    tempEntity.newMembershipMapping(Organization.ROOT_ORGANIZATION_ID, Role.DEVELOPER_ROLE_ID,
        tempEntity.newUser("root-org-user").getUsername());
    Organization organization = tempEntity.newOrganization();
    tempEntity.newMembershipMapping(organization.getId(), Role.DEVELOPER_ROLE_ID, "org-user");
    Application application = tempEntity.newApplication(organization.getId());
    tempEntity.newMembershipMapping(application.getId(), Role.DEVELOPER_ROLE_ID, "app-user");

    ApiRoleMemberMappingListDTO listDTO =
        membershipMappingService.getRoleMembershipsOmitEmpty(OwnerType.APPLICATION, application.getId());

    List<ApiRoleMemberMappingDTO> memberMappings = listDTO.memberMappings;
    assertThat(memberMappings).hasSize(1);

    ApiRoleMemberMappingDTO apiRoleMemberMappingDTO = memberMappings.get(0);
    assertThat(apiRoleMemberMappingDTO.roleId).isEqualTo(DEVELOPER_ROLE_ID);
    assertThat(apiRoleMemberMappingDTO.members).hasSize(3);
    assertApiMemberDTO(apiRoleMemberMappingDTO.members.get(0), application.getId(), OwnerType.APPLICATION,
        MemberType.USER, "app-user");
    assertApiMemberDTO(apiRoleMemberMappingDTO.members.get(1), organization.getId(), OwnerType.ORGANIZATION,
        MemberType.USER, "org-user");
    assertApiMemberDTO(apiRoleMemberMappingDTO.members.get(2), Organization.ROOT_ORGANIZATION_ID,
        OwnerType.ORGANIZATION, MemberType.USER, "root-org-user");
  }

  @Test
  public void testGetRoleMembershipsOmitEmpty() {
    Organization organization = tempEntity.newOrganization();
    tempEntity.newMembershipMapping(organization.getId(), Role.DEVELOPER_ROLE_ID, "a-user");

    ApiRoleMemberMappingListDTO listDTO = membershipMappingService
        .getRoleMembershipsOmitEmpty(OwnerType.ORGANIZATION, organization.getId());

    List<ApiRoleMemberMappingDTO> memberMappings = listDTO.memberMappings;
    assertThat(memberMappings).hasSize(1);

    ApiRoleMemberMappingDTO apiRoleMemberMappingDTO = memberMappings.get(0);
    assertThat(apiRoleMemberMappingDTO.roleId).isEqualTo(DEVELOPER_ROLE_ID);
    assertThat(apiRoleMemberMappingDTO.members).hasSize(1);
    assertApiMemberDTO(apiRoleMemberMappingDTO.members.get(0), organization.getId(), OwnerType.ORGANIZATION,
        MemberType.USER, "a-user");
  }

  @Test
  public void testGetRoleMembershipsOmitEmpty_Global() {
    ApiRoleMemberMappingListDTO listDTO = membershipMappingService
        .getRoleMembershipsOmitEmpty(OwnerType.GLOBAL, MembershipMapping.GLOBAL_CONTEXT_ID);

    List<ApiRoleMemberMappingDTO> memberMappings = listDTO.memberMappings;
    assertThat(memberMappings).hasSize(2);

    ApiRoleMemberMappingDTO apiRoleMemberMappingDTO = memberMappings.get(0);
    assertThat(apiRoleMemberMappingDTO.roleId).isEqualTo(POLICY_ADMIN_ROLE_ID);
    assertThat(apiRoleMemberMappingDTO.members).extracting(dto -> dto.userOrGroupName).containsExactly("admin");

    apiRoleMemberMappingDTO = memberMappings.get(1);
    assertThat(apiRoleMemberMappingDTO.roleId).isEqualTo(SYSTEM_ADMIN_ROLE_ID);
    assertThat(apiRoleMemberMappingDTO.members).hasSize(1);
    assertApiMemberDTO(apiRoleMemberMappingDTO.members.get(0), MembershipMapping.GLOBAL_CONTEXT_ID, OwnerType.GLOBAL,
        MemberType.USER, "admin");
  }

  @Test
  public void testGetIdGlobalOrRepositoryContainer_Global() {
    String id = membershipMappingService.getIdGlobalOrRepositoryContainer(OwnerType.GLOBAL);
    assertThat(id).isEqualTo(MembershipMapping.GLOBAL_CONTEXT_ID);
  }

  @Test
  public void testGetIdGlobalOrRepositoryContainer_RepositoryContainer() {
    String id = membershipMappingService.getIdGlobalOrRepositoryContainer(OwnerType.REPOSITORY_CONTAINER);
    assertThat(id).isEqualTo(RepositoryContainer.REPOSITORY_CONTAINER_ID);
  }

  @Test
  public void testGetIdGlobalOrRepositoryContainer_UnsupportedOwnerType() {
    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(() -> membershipMappingService.getIdGlobalOrRepositoryContainer(OwnerType.APPLICATION))
        .withMessage("Only for global and repository_container");
  }

  @Test
  public void testGrantMembershipMappingsForGlobalContextNoAuthz_Success() throws Exception {
    handler = new TestEventHandler<>(new CountDownLatch(1), RoleEvent.class);
    eventBus.register(handler);

    String username = "username";
    Member member = new Member(MemberType.USER, username, username);

    Map<String, List<Member>> roleToMembers = new HashMap<>();
    roleToMembers.put(SYSTEM_ADMIN_ROLE_ID, Collections.singletonList(member));
    roleToMembers.put(POLICY_ADMIN_ROLE_ID, Collections.singletonList(member));

    membershipMappingService.grantMembershipMappingsForGlobalContextNoAuthz(roleToMembers);

    assertGlobalPermisionsAreGranted(handler, username);
  }

  @Test
  public void testGrantMembershipMappingsForGlobalContextNoAuthz_SkipIfAlreadyExists() throws Exception {
    handler = new TestEventHandler<>(new CountDownLatch(1), RoleEvent.class);
    eventBus.register(handler);

    String username = "username";
    Member member = new Member(MemberType.USER, username, username);
    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, SYSTEM_ADMIN_ROLE_ID, username);

    Map<String, List<Member>> roleToMembers = new HashMap<>();
    roleToMembers.put(SYSTEM_ADMIN_ROLE_ID, Collections.singletonList(member));
    roleToMembers.put(POLICY_ADMIN_ROLE_ID, Collections.singletonList(member));

    membershipMappingService.grantMembershipMappingsForGlobalContextNoAuthz(roleToMembers);

    assertGlobalPermisionsAreGranted(handler, username);
  }

  @Test
  public void testGetPermissionsForUserPrincipal() {
    String username = "username";
    Set<String> membership = new HashSet<>(Arrays.asList("developers", "qa"));
    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, SYSTEM_ADMIN_ROLE_ID, username);
    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, POLICY_ADMIN_ROLE_ID, username);
    tempEntity.newGroupMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, DEVELOPER_ROLE_ID, "developers");
    tempEntity.newGroupMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, APPLICATION_EVALUATOR_ROLE_ID, "qa");

    Set<String> actual = membershipMappingService.getPermissionsForUserPrincipal(username, membership);

    assertThat(actual).isNotNull();
    assertThat(actual).containsExactlyInAnyOrder("Add", "Change", "Claim", "Edit", "Evaluate", "Manage",
        "Review","View", "Waive");
  }

  @Test
  public void testGetApplicationIdsForUser_withApplicationMembershipMappings() {
    String username = "username";
    Set<String> membership = new HashSet<>(Arrays.asList("developers", "qa"));
    final Organization organization = tempEntity.newOrganization("Test Org");
    final Application application = tempEntity.newApplication("Some App", "SOME_APP", organization.getId());
    final Application application2 = tempEntity.newApplication("Some App 2", "SOME_APP2", organization.getId());
    final Application application3 = tempEntity.newApplication("Some App 3", "SOME_APP3", organization.getId());
    final Application application4 = tempEntity.newApplication("Some App 4", "SOME_APP4", organization.getId());
    tempEntity.newMembershipMapping(application.getId(), SYSTEM_ADMIN_ROLE_ID, username);
    tempEntity.newMembershipMapping(application2.getId(), SYSTEM_ADMIN_ROLE_ID, username);
    tempEntity.newMembershipMapping(application3.getId(), SYSTEM_ADMIN_ROLE_ID, username);
    tempEntity.newMembershipMapping(application4.getId(), SYSTEM_ADMIN_ROLE_ID, username);

    Set<String> userApplicationIds = membershipMappingService.getApplicationIdsForUser(username, membership);

    assertThat(userApplicationIds).containsExactlyInAnyOrder(application.getId(), application2.getId(),
        application3.getId(),
        application4.getId());
  }

  @Test
  public void testGetApplicationIdsForUser_withOrganizationMembershipMappings_noChildren() {
    String username = "username";
    Set<String> membership = new HashSet<>(Arrays.asList("developers", "qa"));
    final Organization organization = tempEntity.newOrganization("Test Org");
    final Application application = tempEntity.newApplication("Some App", "SOME_APP", organization.getId());
    final Application application2 = tempEntity.newApplication("Some App 2", "SOME_APP2", organization.getId());
    final Application application3 = tempEntity.newApplication("Some App 3", "SOME_APP3", organization.getId());
    final Application application4 = tempEntity.newApplication("Some App 4", "SOME_APP4", organization.getId());
    tempEntity.newMembershipMapping(organization.getId(), SYSTEM_ADMIN_ROLE_ID, username);

    Set<String> userApplicationIds = membershipMappingService.getApplicationIdsForUser(username, membership);

    assertThat(userApplicationIds).containsExactlyInAnyOrder(application.getId(), application2.getId(),
        application3.getId(),
        application4.getId());
  }

  @Test
  public void testGetApplicationIdsForUser_withMixedMembershipMappings_noChildren() {
    String username = "username";
    Set<String> membership = new HashSet<>(Arrays.asList("developers", "qa"));
    final Organization organization = tempEntity.newOrganization("Test Org");
    final Organization organization2 = tempEntity.newOrganization("Test Org 2");
    final Application application = tempEntity.newApplication("Some App", "SOME_APP", organization.getId());
    final Application application2 = tempEntity.newApplication("Some App 2", "SOME_APP2", organization.getId());
    final Application application3 = tempEntity.newApplication("Some App 3", "SOME_APP3", organization.getId());
    final Application application4 = tempEntity.newApplication("Some App 4", "SOME_APP4", organization.getId());
    final Application application5 = tempEntity.newApplication("Some App 5", "SOME_APP5", organization2.getId());
    tempEntity.newMembershipMapping(organization.getId(), SYSTEM_ADMIN_ROLE_ID, username);
    tempEntity.newMembershipMapping(application5.getId(), SYSTEM_ADMIN_ROLE_ID, username);

    Set<String> userApplicationIds = membershipMappingService.getApplicationIdsForUser(username, membership);

    assertThat(userApplicationIds).containsExactlyInAnyOrder(application.getId(), application2.getId(),
        application3.getId(),
        application4.getId(),
        application5.getId());
  }

  @Test
  public void testGetApplicationIdsForUser_withMixedMembershipMappings_withChildren() {
    String username = "username";
    Set<String> membership = new HashSet<>(Arrays.asList("developers", "qa"));
    final Organization organization = tempEntity.newOrganization("Test Org");
    final Organization organization2 = tempEntity.newOrganization("Test Org 2");
    final Application application = tempEntity.newApplication("Some App", "SOME_APP", organization.getId());
    final Application application2 = tempEntity.newApplication("Some App 2", "SOME_APP2", organization.getId());
    final Application application3 = tempEntity.newApplication("Some App 3", "SOME_APP3", organization.getId());
    final Application application4 = tempEntity.newApplication("Some App 4", "SOME_APP4", organization.getId());
    final Application application5 = tempEntity.newApplication("Some App 5", "SOME_APP5", organization2.getId());

    final Organization childOrg = tempEntity.newOrganization("Child Org", organization);
    final Application childApp1 = tempEntity.newApplication("Child App 1", "CHILD_APP", childOrg.getId());
    final Application childApp2 = tempEntity.newApplication("Child App 2", "CHILD_APP2", childOrg.getId());

    final Organization grandChildOrg = tempEntity.newOrganization("Grand Child Org", childOrg);
    final Application grandChildApp1 =
        tempEntity.newApplication("Grand Child App 1", "GRAND_CHILD_APP", grandChildOrg.getId());

    tempEntity.newMembershipMapping(organization.getId(), SYSTEM_ADMIN_ROLE_ID, username);
    tempEntity.newMembershipMapping(application5.getId(), SYSTEM_ADMIN_ROLE_ID, username);

    Set<String> userApplicationIds = membershipMappingService.getApplicationIdsForUser(username, membership);

    assertThat(userApplicationIds).containsExactlyInAnyOrder(application.getId(), application2.getId(),
        application3.getId(), application4.getId(), application5.getId(), childApp1.getId(),
        childApp2.getId(), grandChildApp1.getId());
  }

  private void assertGlobalPermisionsAreGranted(final TestEventHandler<RoleEvent> handler, final String username)
      throws InterruptedException
  {
    ApiRoleMemberMappingListDTO listDTO = membershipMappingService
        .getRoleMembershipsOmitEmpty(OwnerType.GLOBAL, MembershipMapping.GLOBAL_CONTEXT_ID);

    List<ApiRoleMemberMappingDTO> memberMappings = listDTO.memberMappings;
    assertThat(memberMappings).hasSize(2);

    ApiRoleMemberMappingDTO apiRoleMemberMappingDTO = memberMappings.get(0);
    assertThat(apiRoleMemberMappingDTO.roleId).isEqualTo(POLICY_ADMIN_ROLE_ID);
    assertThat(apiRoleMemberMappingDTO.members).extracting(dto -> dto.userOrGroupName).contains(username);

    apiRoleMemberMappingDTO = memberMappings.get(1);
    assertThat(apiRoleMemberMappingDTO.roleId).isEqualTo(SYSTEM_ADMIN_ROLE_ID);
    assertThat(apiRoleMemberMappingDTO.members).extracting(dto -> dto.userOrGroupName).contains(username);

    assertThat(handler.getLatch().await(5, SECONDS)).isTrue();
    assertThat(handler.getEvent().action).isEqualTo(UPDATED);
  }

  private void setupLdapWithNonDynamicGroupType(String serverName, LdapGroupMappingType groupMappingType) {
    LdapServer ldapServer = tempEntity.newLdapServer(serverName);
    tempEntity.newLdapConnection(ldapServer.getId(), 389);

    LdapUserMapping ldapUserMapping = tempEntity.newLdapUserMapping(ldapServer.getId());
    ldapUserMapping.setGroupMappingType(groupMappingType);
    ldapUserMapping.setDynamicGroupSearchEnabled(false);

    ldapUserMappingDAO.update(ldapUserMapping);
  }

  private void setupLdapWithDynamicGroupType(String serverName, boolean isDynamicGroupSearchEnabled) {
    LdapServer ldapServer = tempEntity.newLdapServer(serverName);
    tempEntity.newLdapConnection(ldapServer.getId(), 389);

    LdapUserMapping ldapUserMapping = tempEntity.newLdapUserMapping(ldapServer.getId());
    ldapUserMapping.setGroupMappingType(LdapGroupMappingType.DYNAMIC);
    ldapUserMapping.setDynamicGroupSearchEnabled(isDynamicGroupSearchEnabled);

    ldapUserMappingDAO.update(ldapUserMapping);
  }

  private void assertApiMemberDTO(
      ApiMemberDTO actual,
      String expectedOwnerId,
      OwnerType expectedOwnerType,
      MemberType expectedMemberType,
      String expectedUserOrGroupName)
  {
    assertThat(actual.ownerId).isEqualTo(expectedOwnerId);
    assertThat(actual.ownerType).isEqualTo(expectedOwnerType.name());
    assertThat(actual.type).isEqualTo(expectedMemberType);
    assertThat(actual.userOrGroupName).isEqualTo(expectedUserOrGroupName);
  }
}
