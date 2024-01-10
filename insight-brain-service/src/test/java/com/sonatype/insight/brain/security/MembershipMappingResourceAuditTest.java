/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Role;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MembershipMappingResourceAuditTest
    extends AbstractMembershipMappingAuditTest
{
  private List<Member> members;

  @Before
  public void before() {
    members = Arrays.asList(member(MemberType.USER), member(MemberType.GROUP));
  }

  @Test
  public void testSetMembershipMappingForRole_GlobalOwner() throws Exception {
    setMembershipMappingRequest(OwnerType.GLOBAL, MembershipMapping.GLOBAL_CONTEXT_ID, Role.SYSTEM_ADMIN_ROLE_ID,
        members).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_ROLE_MEMBERSHIP, null);
    assertGlobalData(auditDTO);
    assertRoleMembershipData(auditDTO, Role.SYSTEM_ADMIN_ROLE_ID, members);
  }

  @Test
  public void testSetMembershipMappingForRole_GlobalOwner_Unauthorized() throws Exception {
    setMembershipMappingRequest(OwnerType.GLOBAL, MembershipMapping.GLOBAL_CONTEXT_ID, Role.SYSTEM_ADMIN_ROLE_ID,
        members).with(unauthorizedUser()).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_ROLE_MEMBERSHIP, "unauthorized");
    assertGlobalData(auditDTO);
  }

  @Test
  public void testSetMembershipMappingForRole_ApplicationOwner() throws Exception {
    Application application = tempEntity.newApplicationWithParent();

    setMembershipMappingRequest(OwnerType.APPLICATION, application.getPublicId(), Role.OWNER_ROLE_ID, members).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_ROLE_MEMBERSHIP, null);
    assertApplicationData(auditDTO, application);
    assertRoleMembershipData(auditDTO, Role.OWNER_ROLE_ID, members);
  }

  @Test
  public void testSetMembershipMappingForRole_ApplicationOwner_Unauthorized() throws Exception {
    Application application = tempEntity.newApplicationWithParent();

    setMembershipMappingRequest(OwnerType.APPLICATION, application.getPublicId(), Role.OWNER_ROLE_ID, members)
        .with(unauthorizedUser()).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_ROLE_MEMBERSHIP, "unauthorized");
    assertApplicationData(auditDTO, application);
  }

  @Test
  public void testSetMembershipMappingForRole_OrganizationOwner() throws Exception {
    Organization organization = tempEntity.newOrganization();

    setMembershipMappingRequest(OwnerType.ORGANIZATION, organization.getId(), Role.OWNER_ROLE_ID, members).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_ROLE_MEMBERSHIP, null);
    assertOrganizationData(auditDTO, organization);
    assertRoleMembershipData(auditDTO, Role.OWNER_ROLE_ID, members);
  }

  @Test
  public void testSetMembershipMappingForRole_OrganizationOwner_Unauthorized() throws Exception {
    Organization organization = tempEntity.newOrganization();

    setMembershipMappingRequest(OwnerType.ORGANIZATION, organization.getId(), Role.OWNER_ROLE_ID, members)
        .with(unauthorizedUser()).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_ROLE_MEMBERSHIP, "unauthorized");
    assertOrganizationData(auditDTO, organization);
  }

  @Test
  public void testSetMembershipMappingForRole_RepositoryContainerOwner() throws Exception {
    setMembershipMappingRequest(OwnerType.REPOSITORY_CONTAINER, null, Role.OWNER_ROLE_ID, members).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_ROLE_MEMBERSHIP, null);
    assertRepositoryContainerData(auditDTO);
    assertRoleMembershipData(auditDTO, Role.OWNER_ROLE_ID, members);
  }

  @Test
  public void testSetMembershipMappingForRole_RepositoryContainerOwner_Unauthorized() throws Exception {
    setMembershipMappingRequest(OwnerType.REPOSITORY_CONTAINER, null, Role.OWNER_ROLE_ID, members)
        .with(unauthorizedUser()).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_ROLE_MEMBERSHIP, "unauthorized");
    assertRepositoryContainerData(auditDTO);
  }

  @Test
  public void testSetMembershipMappingForRole_DependentSubEvents() throws Exception {
    setMembershipMappingRequest(OwnerType.GLOBAL, MembershipMapping.GLOBAL_CONTEXT_ID, Role.SYSTEM_ADMIN_ROLE_ID,
        Collections.singletonList(member(null))).put();

    List<AuditDTO> auditDTOs = assertAuditLogs(AuditEvent.CONFIGURE_ROLE_MEMBERSHIP, 1, "bad-request");
    assertThat(auditDTOs).hasSize(1);
    assertGlobalData(auditDTOs.get(0));
  }

  @Test
  public void testSetMembershipMappingForRole_RepositoryOwner() throws Exception {
    Repository repo = tempEntity.newRepository();

    setMembershipMappingRequest(OwnerType.REPOSITORY, repo.getId(), Role.OWNER_ROLE_ID, members).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_ROLE_MEMBERSHIP, null);
    assertRepositoryData(auditDTO, repo);
    assertRoleMembershipData(auditDTO, Role.OWNER_ROLE_ID, members);
  }

  @Test
  public void testSetMembershipMappingForRole_RepositoryOwner_Unauthorized() throws Exception {
    Repository repo = tempEntity.newRepository();

    setMembershipMappingRequest(OwnerType.REPOSITORY, repo.getId(), Role.OWNER_ROLE_ID, members)
        .with(unauthorizedUser()).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_ROLE_MEMBERSHIP, "unauthorized");
    assertRepositoryData(auditDTO, repo);
  }

  @Test
  public void testSetMembershipMappingForRole_RepositoryManagerOwner() throws Exception {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();

    setMembershipMappingRequest(OwnerType.REPOSITORY_MANAGER, repoManager.getId(), Role.OWNER_ROLE_ID, members).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_ROLE_MEMBERSHIP, null);
    assertRepositoryManagerData(auditDTO, repoManager);
    assertRoleMembershipData(auditDTO, Role.OWNER_ROLE_ID, members);
  }

  @Test
  public void testSetMembershipMappingForRole_RepositoryManagerOwner_Unauthorized() throws Exception {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();

    setMembershipMappingRequest(OwnerType.REPOSITORY_MANAGER, repoManager.getId(), Role.OWNER_ROLE_ID, members)
        .with(unauthorizedUser()).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_ROLE_MEMBERSHIP, "unauthorized");
    assertRepositoryManagerData(auditDTO, repoManager);
  }

  private Member member(MemberType memberType) {
    Member member = new Member();
    member.setInternalName(TemporaryEntity.uuid());
    member.setType(memberType);
    return member;
  }

  private HttpRequest setMembershipMappingRequest(OwnerType ownerType,
                                                  String ownerId,
                                                  String roleId,
                                                  List<Member> members)
  {
    HttpRequest request = restRequest().path(MembershipMappingResource.RESOURCE_PATH);
    return ownerType == OwnerType.REPOSITORY_CONTAINER ?
        request.path(MembershipMappingResource.SINGLETON_ROLE_PATH).parameter(ownerType, roleId).body(members) :
        request.path(MembershipMappingResource.ROLE_PATH).parameter(ownerType, ownerId, roleId).body(members);
  }
}
