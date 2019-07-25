/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapUserMappingDAO;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.ldap.LdapGroupMappingType;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.model.configuration.ldap.LdapUserMapping;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.webhook.ManagementEvent.RoleEvent;
import com.sonatype.insight.brain.webhook.TestEventHandler;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Test;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
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

  @Test
  public void testLoadMembersByRoleForNonGlobalContext_GlobalContext() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      membershipMappingService.loadMembersByRoleForNonGlobalContext(OwnerType.GLOBAL, "ownerId",
          null /* memberAttributeResolver */, null /* roles */, null/* membersByRoleByRoleId */);
    }).withMessage("The 'global' context is not allowed.");
  }

  @Test
  public void testSetMembershipMappingsForNonGlobalContext_GlobalContext() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      membershipMappingService.setMembershipMappingsForNonGlobalContext(OwnerType.GLOBAL, "ownerId",
          null /* roleToMembers */);
    }).withMessage("The 'global' context is not allowed.");
  }

  @Test
  public void testSetMembershipMappings_PostsEvent() throws Exception {
    TestEventHandler<RoleEvent> handler = new TestEventHandler<>(new CountDownLatch(1));
    eventBus.register(handler);

    Role role = tempEntity.newRole(false, Permission.READ);
    tempEntity.newMembershipMapping(ROOT_ORGANIZATION_ID, role.getId(), "username");
    Member member = new Member(MemberType.USER, "username", "username");

    Map<String, List<Member>> roleToMembers = Collections.singletonMap(role.getId(), Arrays.asList(member));
    membershipMappingService.setMembershipMappings(OwnerType.ORGANIZATION, ROOT_ORGANIZATION_ID, roleToMembers);

    assertThat(handler.getLatch().await(5, SECONDS)).isTrue();
    assertThat(handler.getEvent().action).isEqualTo(UPDATED);
  }

  @Test
  public void testGetApplicableMembershipMappings_DynamicGroupSearchAllEnabled() {
    setupLdapWithDynamicGroupType("test server 1", true);
    setupLdapWithDynamicGroupType("test server 2", true);

    ApplicableMembershipMappings actual = membershipMappingService
        .getApplicableMembershipMappings(OwnerType.ORGANIZATION, "ROOT_ORGANIZATION_ID");

    assertThat(actual.groupSearchEnabled).isTrue();
  }

  @Test
  public void testGetApplicableMembershipMappings_MixedDynamicGroupSearch() {
    setupLdapWithDynamicGroupType("test server 1", false);
    setupLdapWithDynamicGroupType("test server 2", true);

    ApplicableMembershipMappings actual = membershipMappingService
        .getApplicableMembershipMappings(OwnerType.ORGANIZATION, "ROOT_ORGANIZATION_ID");

    assertThat(actual.groupSearchEnabled).isFalse();
  }

  @Test
  public void testGetApplicableMembershipMappings_MixedGroupSearch() {
    setupLdapWithNonDynamicGroupType("test server 1", LdapGroupMappingType.STATIC);
    setupLdapWithNonDynamicGroupType("test server 2", LdapGroupMappingType.NONE);
    setupLdapWithDynamicGroupType("test server 3", false);

    ApplicableMembershipMappings actual = membershipMappingService
        .getApplicableMembershipMappings(OwnerType.ORGANIZATION, "ROOT_ORGANIZATION_ID");

    assertThat(actual.groupSearchEnabled).isFalse();
  }

  @Test
  public void testGrantMembershipMapping_NonGlobal() throws InterruptedException {
    TestEventHandler<RoleEvent> handler = new TestEventHandler<>(new CountDownLatch(1));
    eventBus.register(handler);

    String username = tempEntity.newUser("a-user").getUsername();
    String applicationId = tempEntity.newApplicationWithParent().getId();

    membershipMappingService
        .grantMembershipMapping(OwnerType.APPLICATION, applicationId, Role.DEVELOPER_ROLE_ID, MemberType.USER,
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
  public void testGrantMembershipMapping_Global() {
    String username = tempEntity.newUser("a-user").getUsername();

    membershipMappingService
        .grantMembershipMapping(OwnerType.GLOBAL, MembershipMapping.GLOBAL_CONTEXT_ID, Role.SYSTEM_ADMIN_ROLE_ID,
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
  public void testGrantMembershipMapping_AlreadyExisting() {
    Application application = tempEntity.newApplicationWithParent();
    String username = tempEntity.newUser("a-user").getUsername();

    String contextId = application.getId();
    String memberName = username;
    MemberType memberType = MemberType.USER;

    tempEntity.newMembershipMapping(contextId, Role.DEVELOPER_ROLE_ID, memberName, memberType);

    membershipMappingService
        .grantMembershipMapping(OwnerType.APPLICATION, contextId, Role.DEVELOPER_ROLE_ID, memberType, memberName);
  }

  @Test
  public void testGrantMembershipMapping_NoMemberName() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(
            () -> membershipMappingService
                .grantMembershipMapping(OwnerType.APPLICATION, "owner-id", Role.DEVELOPER_ROLE_ID, MemberType.USER, ""))
        .withMessageContaining("Internal name of role member has not been specified");
  }

  @Test
  public void testGrantMembershipMapping_NoMemberType() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> membershipMappingService
            .grantMembershipMapping(OwnerType.APPLICATION, "owner-id", Role.DEVELOPER_ROLE_ID, null, "username"))
        .withMessageContaining("Type of role member has not been specified");
  }

  @Test
  public void testGrantMembershipMapping_UnknownContextId() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> membershipMappingService
            .grantMembershipMapping(OwnerType.APPLICATION, "owner-id", Role.DEVELOPER_ROLE_ID, MemberType.USER,
                "username"))
        .withMessageContaining("Could not find an application with ID owner-id.");
  }

  @Test
  public void testGrantMembershipMapping_ContextTypeAndIdMismatch() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> membershipMappingService
            .grantMembershipMapping(OwnerType.ORGANIZATION, "no-such-application", Role.DEVELOPER_ROLE_ID,
                MemberType.USER, "username"))
        .withMessageContaining("Cannot find organization with ID no-such-application");
  }

  @Test
  public void testGrantMembershipMapping_RoleValidationOwnerNotGlobalRoleGlobal() {
    String username = tempEntity.newUser("a-user").getUsername();
    String applicationId = tempEntity.newApplicationWithParent().getId();

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> membershipMappingService
            .grantMembershipMapping(OwnerType.APPLICATION, applicationId, Role.SYSTEM_ADMIN_ROLE_ID, MemberType.USER,
                username))
        .withMessageContaining("Cannot map members to global role in context of application.");
  }

  @Test
  public void testGrantMembershipMapping_RoleValidationOwnerGlobalRoleNotGlobal() {
    String username = tempEntity.newUser("a-user").getUsername();

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> membershipMappingService
            .grantMembershipMapping(OwnerType.GLOBAL, MembershipMapping.GLOBAL_CONTEXT_ID, Role.DEVELOPER_ROLE_ID,
                MemberType.USER, username))
        .withMessageContaining("Cannot map members to application role in global context.");
  }

  @Test
  public void testRevokeMembershipMapping() throws InterruptedException {
    TestEventHandler<RoleEvent> handler = new TestEventHandler<>(new CountDownLatch(1));
    eventBus.register(handler);

    String contextId = tempEntity.newApplicationWithParent().getId();
    String memberName = tempEntity.newUser("a-user").getUsername();
    MemberType memberType = MemberType.USER;

    MembershipMapping membershipMapping =
        tempEntity.newMembershipMapping(contextId, Role.DEVELOPER_ROLE_ID, memberName, memberType);

    membershipMappingService
        .revokeMembershipMapping(OwnerType.APPLICATION, contextId, Role.DEVELOPER_ROLE_ID, memberType, memberName);
    assertThat(membershipMappingDAO.getById(membershipMapping.getId())).isNull();

    assertThat(handler.getLatch().await(5, SECONDS)).isTrue();
    assertThat(handler.getEvent().action).isEqualTo(DELETED);

    Member member = handler.getEvent().roleIdToMemberMap.entrySet().iterator().next().getValue().get(0);
    assertThat(member.getInternalName()).isEqualTo(membershipMapping.getMemberName());
    assertThat(member.getType()).isEqualTo(membershipMapping.getMemberType());
  }

  @Test
  public void testRevokeMembershipMapping_Global() {
    String contextId = MembershipMapping.GLOBAL_CONTEXT_ID;
    String memberName = tempEntity.newUser("a-user").getUsername();
    MemberType memberType = MemberType.USER;

    MembershipMapping membershipMapping =
        tempEntity.newMembershipMapping(contextId, Role.POLICY_ADMIN_ROLE_ID, memberName, memberType);

    membershipMappingService
        .revokeMembershipMapping(OwnerType.GLOBAL, contextId, Role.POLICY_ADMIN_ROLE_ID, memberType, memberName);
    assertThat(membershipMappingDAO.getById(membershipMapping.getId())).isNull();
  }

  @Test
  public void testRevokeMembershipMapping_NotExisting() {
    String contextId = tempEntity.newApplicationWithParent().getId();
    String memberName = tempEntity.newUser("a-user").getUsername();
    MemberType memberType = MemberType.USER;

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> membershipMappingService
            .revokeMembershipMapping(OwnerType.APPLICATION, contextId, Role.DEVELOPER_ROLE_ID, memberType, memberName))
        .withMessageContaining("Membership mapping not found.");
  }

  private void setupLdapWithNonDynamicGroupType(String serverName, LdapGroupMappingType groupMappingType) {
    LdapServer ldapServer = tempEntity.newLdapServer(serverName);
    tempEntity.newLdapConnection(ldapServer.getId(), 389);

    LdapUserMapping umap = tempEntity.newLdapUserMapping(ldapServer.getId());
    umap.setGroupMappingType(groupMappingType);
    umap.setDynamicGroupSearchEnabled(false);

    new LdapUserMappingDAO().update(umap);
  }

  private void setupLdapWithDynamicGroupType(String serverName, boolean isDynamicGroupSearchEnabled) {
    LdapServer ldapServer = tempEntity.newLdapServer(serverName);
    tempEntity.newLdapConnection(ldapServer.getId(), 389);

    LdapUserMapping umap = tempEntity.newLdapUserMapping(ldapServer.getId());
    umap.setGroupMappingType(LdapGroupMappingType.DYNAMIC);
    umap.setDynamicGroupSearchEnabled(isDynamicGroupSearchEnabled);

    new LdapUserMappingDAO().update(umap);
  }
}
