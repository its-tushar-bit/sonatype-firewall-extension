/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.security.AbstractMembershipMappingAuditTest;
import com.sonatype.insight.brain.security.Member;

import org.junit.Test;

import static com.sonatype.insight.brain.api.v2.ApiRoleMembershipResource.APPLICATION_OR_ORGANIZATION;
import static com.sonatype.insight.brain.api.v2.ApiRoleMembershipResource.GLOBAL_OR_REPOSITORY_CONTAINER;
import static com.sonatype.insight.brain.model.security.MembershipMapping.*;
import static com.sonatype.insight.brain.model.security.Role.DEVELOPER_ROLE_ID;
import static com.sonatype.insight.brain.model.security.Role.SYSTEM_ADMIN_ROLE_ID;

public class ApiRoleMembershipResourceAuditTest
    extends AbstractMembershipMappingAuditTest
{
  @Test
  public void testGrantMembershipMapping_Application() throws Exception {
    Application app = tempEntity.newApplicationWithParent();

    restRequest().path(APPLICATION_OR_ORGANIZATION)
        .parameter("application", app.getId(), DEVELOPER_ROLE_ID, "user", "username").put();

    Member member = new Member();
    member.setInternalName("username");
    member.setType(MemberType.USER);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.GRANT_ROLE_MEMBERSHIP, null);
    assertRoleMembershipData(auditDTO, DEVELOPER_ROLE_ID, member);
    assertApplicationData(auditDTO, app);
  }

  @Test
  public void testGrantMembershipMapping_Organization() throws Exception {
    Organization org = tempEntity.newOrganization();

    restRequest().path(APPLICATION_OR_ORGANIZATION)
        .parameter("organization", org.getId(), DEVELOPER_ROLE_ID, "user", "username").put();

    Member member = new Member();
    member.setInternalName("username");
    member.setType(MemberType.USER);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.GRANT_ROLE_MEMBERSHIP, null);
    assertRoleMembershipData(auditDTO, DEVELOPER_ROLE_ID, member);
    assertOrganizationData(auditDTO, org);
  }

  @Test
  public void testGrantMembershipMapping_Global() throws Exception {
    restRequest().path(GLOBAL_OR_REPOSITORY_CONTAINER)
        .parameter("global", SYSTEM_ADMIN_ROLE_ID, "user", "username").put();

    Member member = new Member();
    member.setInternalName("username");
    member.setType(MemberType.USER);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.GRANT_ROLE_MEMBERSHIP, null);
    assertRoleMembershipData(auditDTO, SYSTEM_ADMIN_ROLE_ID, member);
    assertGlobalData(auditDTO);
  }

  @Test
  public void testGrantMembershipMapping_Application_Unauthorized() throws Exception {
    Application app = tempEntity.newApplicationWithParent();

    Member member = new Member();
    member.setInternalName("username");
    member.setType(MemberType.USER);

    restRequest().with(unauthorizedUser()).path(APPLICATION_OR_ORGANIZATION)
        .parameter("application", app.getId(), DEVELOPER_ROLE_ID, "user", "username").put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.GRANT_ROLE_MEMBERSHIP, "unauthorized");
    assertRoleMembershipData(auditDTO, DEVELOPER_ROLE_ID, member);
    assertApplicationData(auditDTO, app);
  }

  @Test
  public void testGrantMembershipMapping_Organization_Unauthorized() throws Exception {
    Organization org = tempEntity.newOrganization();

    Member member = new Member();
    member.setInternalName("username");
    member.setType(MemberType.USER);

    restRequest().with(unauthorizedUser()).path(APPLICATION_OR_ORGANIZATION)
        .parameter("organization", org.getId(), DEVELOPER_ROLE_ID, "user", "username").put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.GRANT_ROLE_MEMBERSHIP, "unauthorized");
    assertRoleMembershipData(auditDTO, DEVELOPER_ROLE_ID, member);
    assertOrganizationData(auditDTO, org);
  }

  @Test
  public void testRevokeMembershipMapping_Application() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    tempEntity.newMembershipMapping(app.getId(), DEVELOPER_ROLE_ID, "username", MemberType.USER);

    Member member = new Member();
    member.setInternalName("username");
    member.setType(MemberType.USER);

    restRequest().path(APPLICATION_OR_ORGANIZATION)
        .parameter("application", app.getId(), DEVELOPER_ROLE_ID, "user", "username").delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.REVOKE_ROLE_MEMBERSHIP, null);
    assertRoleMembershipData(auditDTO, DEVELOPER_ROLE_ID, member);
    assertApplicationData(auditDTO, app);
  }

  @Test
  public void testRevokeMembershipMapping_Global() throws Exception {
    tempEntity.newMembershipMapping(GLOBAL_CONTEXT_ID, SYSTEM_ADMIN_ROLE_ID, "username", MemberType.USER);

    restRequest().path(GLOBAL_OR_REPOSITORY_CONTAINER)
        .parameter("global", SYSTEM_ADMIN_ROLE_ID, "user", "username").delete();

    Member member = new Member();
    member.setInternalName("username");
    member.setType(MemberType.USER);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.REVOKE_ROLE_MEMBERSHIP, null);
    assertRoleMembershipData(auditDTO, SYSTEM_ADMIN_ROLE_ID, member);
    assertGlobalData(auditDTO);
  }

  @Test
  public void testRevokeMembershipMapping_Organization() throws Exception {
    Organization org = tempEntity.newOrganization();
    tempEntity.newMembershipMapping(org.getId(), DEVELOPER_ROLE_ID, "username", MemberType.USER);

    Member member = new Member();
    member.setInternalName("username");
    member.setType(MemberType.USER);

    restRequest().path(APPLICATION_OR_ORGANIZATION)
        .parameter("organization", org.getId(), DEVELOPER_ROLE_ID, "user", "username").delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.REVOKE_ROLE_MEMBERSHIP, null);
    assertRoleMembershipData(auditDTO, DEVELOPER_ROLE_ID, member);
    assertOrganizationData(auditDTO, org);
  }

  @Test
  public void testRevokeMembershipMapping_Application_Unauthorized() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    tempEntity.newMembershipMapping(app.getId(), DEVELOPER_ROLE_ID, "username", MemberType.USER);

    Member member = new Member();
    member.setInternalName("username");
    member.setType(MemberType.USER);

    restRequest().with(unauthorizedUser()).path(APPLICATION_OR_ORGANIZATION)
        .parameter("application", app.getId(), DEVELOPER_ROLE_ID, "user", "username").delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.REVOKE_ROLE_MEMBERSHIP, "unauthorized");
    assertRoleMembershipData(auditDTO, DEVELOPER_ROLE_ID, member);
    assertApplicationData(auditDTO, app);
  }

  @Test
  public void testRevokeMembershipMapping_Organization_Unauthorized() throws Exception {
    Organization org = tempEntity.newOrganization();
    tempEntity.newMembershipMapping(org.getId(), DEVELOPER_ROLE_ID, "username", MemberType.USER);

    Member member = new Member();
    member.setInternalName("username");
    member.setType(MemberType.USER);

    restRequest().with(unauthorizedUser()).path(APPLICATION_OR_ORGANIZATION)
        .parameter("organization", org.getId(), DEVELOPER_ROLE_ID, "user", "username").delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.REVOKE_ROLE_MEMBERSHIP, "unauthorized");
    assertRoleMembershipData(auditDTO, DEVELOPER_ROLE_ID, member);
    assertOrganizationData(auditDTO, org);
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.ROLE_MEMBERSHIP_PATH_V2);
  }
}
