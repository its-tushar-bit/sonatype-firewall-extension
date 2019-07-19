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
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.ldap.LdapGroupMappingType;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.model.configuration.ldap.LdapUserMapping;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.webhook.ManagementEvent.RoleEvent;
import com.sonatype.insight.brain.webhook.TestEventHandler;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Test;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
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

    eventBus.unregister(handler);
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

