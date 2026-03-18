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
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Test;

import static com.sonatype.insight.brain.api.v2.ApiRoleMembershipResource.APPLICATION_OR_ORGANIZATION;
import static com.sonatype.insight.brain.api.v2.ApiRoleMembershipResource.GLOBAL_OR_REPOSITORY_CONTAINER;
import static com.sonatype.insight.brain.model.security.MembershipMapping.*;
import static com.sonatype.insight.brain.model.security.Role.DEVELOPER_ROLE_ID;
import static com.sonatype.insight.brain.model.security.Role.SYSTEM_ADMIN_ROLE_ID;
import static org.assertj.core.api.Assertions.assertThat;

public class ApiRoleMembershipResourceAuditTest
    extends AbstractAuditTest
{
  @Test
  public void testGrantRoleMembership_Application() throws Exception {
    Application app = tempEntity.newApplicationWithParent();

    restRequest().path(APPLICATION_OR_ORGANIZATION)
        .parameter("application", app.getId(), DEVELOPER_ROLE_ID, "user", "username")
        .put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.GRANT_ROLE_MEMBERSHIP, null);
    assertRoleMembershipData(auditDTO, DEVELOPER_ROLE_ID);
    assertApplicationData(auditDTO, app);
  }

  @Test
  public void testGrantRoleMembership_Organization() throws Exception {
    Organization org = tempEntity.newOrganization();

    restRequest().path(APPLICATION_OR_ORGANIZATION)
        .parameter("organization", org.getId(), DEVELOPER_ROLE_ID, "user", "username")
        .put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.GRANT_ROLE_MEMBERSHIP, null);
    assertRoleMembershipData(auditDTO, DEVELOPER_ROLE_ID);
    assertOrganizationData(auditDTO, org);
  }

  @Test
  public void testGrantRoleMembership_Global() throws Exception {
    restRequest().path(GLOBAL_OR_REPOSITORY_CONTAINER)
        .parameter("global", SYSTEM_ADMIN_ROLE_ID, "user", "username")
        .put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.GRANT_ROLE_MEMBERSHIP, null);
    assertRoleMembershipData(auditDTO, SYSTEM_ADMIN_ROLE_ID);
    assertGlobalData(auditDTO);
  }

  @Test
  public void testGrantRoleMembership_Application_Unauthorized() throws Exception {
    Application app = tempEntity.newApplicationWithParent();

    restRequest().with(unauthorizedUser())
        .path(APPLICATION_OR_ORGANIZATION)
        .parameter("application", app.getId(), DEVELOPER_ROLE_ID, "user", "username")
        .put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.GRANT_ROLE_MEMBERSHIP, "unauthorized");
    assertRoleMembershipData(auditDTO, DEVELOPER_ROLE_ID);
    assertApplicationData(auditDTO, app);
  }

  @Test
  public void testGrantRoleMembership_Organization_Unauthorized() throws Exception {
    Organization org = tempEntity.newOrganization();

    restRequest().with(unauthorizedUser())
        .path(APPLICATION_OR_ORGANIZATION)
        .parameter("organization", org.getId(), DEVELOPER_ROLE_ID, "user", "username")
        .put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.GRANT_ROLE_MEMBERSHIP, "unauthorized");
    assertRoleMembershipData(auditDTO, DEVELOPER_ROLE_ID);
    assertOrganizationData(auditDTO, org);
  }

  @Test
  public void testRevokeRoleMembership_Application() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    tempEntity.newMembershipMapping(app.getId(), DEVELOPER_ROLE_ID, "username", MemberType.USER);

    restRequest().path(APPLICATION_OR_ORGANIZATION)
        .parameter("application", app.getId(), DEVELOPER_ROLE_ID, "user", "username")
        .delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.REVOKE_ROLE_MEMBERSHIP, null);
    assertRoleMembershipData(auditDTO, DEVELOPER_ROLE_ID);
    assertApplicationData(auditDTO, app);
  }

  @Test
  public void testRevokeRoleMembership_Global() throws Exception {
    tempEntity.newMembershipMapping(GLOBAL_CONTEXT_ID, SYSTEM_ADMIN_ROLE_ID, "username", MemberType.USER);

    restRequest().path(GLOBAL_OR_REPOSITORY_CONTAINER)
        .parameter("global", SYSTEM_ADMIN_ROLE_ID, "user", "username")
        .delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.REVOKE_ROLE_MEMBERSHIP, null);
    assertRoleMembershipData(auditDTO, SYSTEM_ADMIN_ROLE_ID);
    assertGlobalData(auditDTO);
  }

  @Test
  public void testRevokeRoleMembership_Organization() throws Exception {
    Organization org = tempEntity.newOrganization();
    tempEntity.newMembershipMapping(org.getId(), DEVELOPER_ROLE_ID, "username", MemberType.USER);

    restRequest().path(APPLICATION_OR_ORGANIZATION)
        .parameter("organization", org.getId(), DEVELOPER_ROLE_ID, "user", "username")
        .delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.REVOKE_ROLE_MEMBERSHIP, null);
    assertRoleMembershipData(auditDTO, DEVELOPER_ROLE_ID);
    assertOrganizationData(auditDTO, org);
  }

  @Test
  public void testRevokeRoleMembership_Application_Unauthorized() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    tempEntity.newMembershipMapping(app.getId(), DEVELOPER_ROLE_ID, "username", MemberType.USER);

    restRequest().with(unauthorizedUser())
        .path(APPLICATION_OR_ORGANIZATION)
        .parameter("application", app.getId(), DEVELOPER_ROLE_ID, "user", "username")
        .delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.REVOKE_ROLE_MEMBERSHIP, "unauthorized");
    assertRoleMembershipData(auditDTO, DEVELOPER_ROLE_ID);
    assertApplicationData(auditDTO, app);
  }

  @Test
  public void testRevokeRoleMembership_Organization_Unauthorized() throws Exception {
    Organization org = tempEntity.newOrganization();
    tempEntity.newMembershipMapping(org.getId(), DEVELOPER_ROLE_ID, "username", MemberType.USER);

    restRequest().with(unauthorizedUser())
        .path(APPLICATION_OR_ORGANIZATION)
        .parameter("organization", org.getId(), DEVELOPER_ROLE_ID, "user", "username")
        .delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.REVOKE_ROLE_MEMBERSHIP, "unauthorized");
    assertRoleMembershipData(auditDTO, DEVELOPER_ROLE_ID);
    assertOrganizationData(auditDTO, org);
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.ROLE_MEMBERSHIP_PATH_V2);
  }

  private void assertRoleMembershipData(AuditDTO auditDTO, String roleId) {
    assertThat(auditDTO.data).containsEntry("roleId", roleId);
    assertThat(auditDTO.data).containsKey("roleMember");
    // The roleMember is a MemberDTO (UserMemberDTO or GroupMemberDTO) with username/groupName field
    Object roleMember = auditDTO.data.get("roleMember");
    assertThat(roleMember).isNotNull();
  }
}
