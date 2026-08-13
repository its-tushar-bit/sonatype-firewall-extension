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
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.brain.variant.IqPostgresTest;
import com.sonatype.insight.brain.variant.IqTestContext;
import com.sonatype.insight.test.LogOutput;

import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.api.v2.ApiRoleMembershipResource.APPLICATION_OR_ORGANIZATION;
import static com.sonatype.insight.brain.api.v2.ApiRoleMembershipResource.GLOBAL_OR_REPOSITORY_CONTAINER;
import static com.sonatype.insight.brain.model.security.MembershipMapping.GLOBAL_CONTEXT_ID;
import static com.sonatype.insight.brain.model.security.Role.DEVELOPER_ROLE_ID;
import static com.sonatype.insight.brain.model.security.Role.SYSTEM_ADMIN_ROLE_ID;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * IQ Server on PostgreSQL — audit logging for {@link ApiRoleMembershipResource}, converted from the
 * legacy {@code ApiRoleMembershipResourceAuditTest}. No base class; state comes from the injected
 * {@link IqTestContext}. Lives in this package because {@code GLOBAL_OR_REPOSITORY_CONTAINER} is
 * package-private on {@link ApiRoleMembershipResource}.
 */
@IqPostgresTest
class IqPostgresApiRoleMembershipResourceAuditTest
    implements AuditTestSupport
{
  // Injected by IqPostgresServerExtension: the extension owns the shared, reused server.
  private IqTestContext ctx;

  private final TestLogOutput logOutput = new TestLogOutput(AuditRecorder.BASE_LOGGER_NAME);

  private User unauthorizedUserEntity;

  @BeforeEach
  void setUp() {
    logOutput.before();
    logOutput.clear();
    unauthorizedUserEntity = ctx.tempEntity().newUser();
  }

  @AfterEach
  void tearDown() {
    logOutput.tearDown();
  }

  @Override
  public LogOutput getLogOutput() {
    return logOutput;
  }

  @Override
  public String getUnauthorizedUsername() {
    return unauthorizedUserEntity.getUsername();
  }

  @Override
  public PolicyDAO getPolicyDAO() {
    return ctx.lookup(PolicyDAO.class);
  }

  private Consumer<HttpRequest> unauthorizedUser() {
    return httpRequest -> httpRequest.auth(unauthorizedUserEntity);
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(PublicApiPaths.ROLE_MEMBERSHIP_PATH_V2);
  }

  @Test
  void testGrantRoleMembership_Application() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent();

    restRequest().path(APPLICATION_OR_ORGANIZATION)
        .parameter("application", app.getId(), DEVELOPER_ROLE_ID, "user", "username")
        .put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.GRANT_ROLE_MEMBERSHIP, null);
    assertRoleMembershipData(auditDTO, DEVELOPER_ROLE_ID);
    assertApplicationData(auditDTO, app);
  }

  @Test
  void testGrantRoleMembership_Organization() throws Exception {
    Organization org = ctx.tempEntity().newOrganization();

    restRequest().path(APPLICATION_OR_ORGANIZATION)
        .parameter("organization", org.getId(), DEVELOPER_ROLE_ID, "user", "username")
        .put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.GRANT_ROLE_MEMBERSHIP, null);
    assertRoleMembershipData(auditDTO, DEVELOPER_ROLE_ID);
    assertOrganizationData(auditDTO, org);
  }

  @Test
  void testGrantRoleMembership_Global() throws Exception {
    restRequest().path(GLOBAL_OR_REPOSITORY_CONTAINER)
        .parameter("global", SYSTEM_ADMIN_ROLE_ID, "user", "username")
        .put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.GRANT_ROLE_MEMBERSHIP, null);
    assertRoleMembershipData(auditDTO, SYSTEM_ADMIN_ROLE_ID);
    assertGlobalData(auditDTO);
  }

  @Test
  void testGrantRoleMembership_Application_Unauthorized() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent();

    restRequest().with(unauthorizedUser())
        .path(APPLICATION_OR_ORGANIZATION)
        .parameter("application", app.getId(), DEVELOPER_ROLE_ID, "user", "username")
        .put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.GRANT_ROLE_MEMBERSHIP, "unauthorized");
    assertRoleMembershipData(auditDTO, DEVELOPER_ROLE_ID);
    assertApplicationData(auditDTO, app);
  }

  @Test
  void testGrantRoleMembership_Organization_Unauthorized() throws Exception {
    Organization org = ctx.tempEntity().newOrganization();

    restRequest().with(unauthorizedUser())
        .path(APPLICATION_OR_ORGANIZATION)
        .parameter("organization", org.getId(), DEVELOPER_ROLE_ID, "user", "username")
        .put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.GRANT_ROLE_MEMBERSHIP, "unauthorized");
    assertRoleMembershipData(auditDTO, DEVELOPER_ROLE_ID);
    assertOrganizationData(auditDTO, org);
  }

  @Test
  void testRevokeRoleMembership_Application() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent();
    ctx.tempEntity().newMembershipMapping(app.getId(), DEVELOPER_ROLE_ID, "username", MemberType.USER);

    restRequest().path(APPLICATION_OR_ORGANIZATION)
        .parameter("application", app.getId(), DEVELOPER_ROLE_ID, "user", "username")
        .delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.REVOKE_ROLE_MEMBERSHIP, null);
    assertRoleMembershipData(auditDTO, DEVELOPER_ROLE_ID);
    assertApplicationData(auditDTO, app);
  }

  @Test
  void testRevokeRoleMembership_Global() throws Exception {
    ctx.tempEntity().newMembershipMapping(GLOBAL_CONTEXT_ID, SYSTEM_ADMIN_ROLE_ID, "username", MemberType.USER);

    restRequest().path(GLOBAL_OR_REPOSITORY_CONTAINER)
        .parameter("global", SYSTEM_ADMIN_ROLE_ID, "user", "username")
        .delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.REVOKE_ROLE_MEMBERSHIP, null);
    assertRoleMembershipData(auditDTO, SYSTEM_ADMIN_ROLE_ID);
    assertGlobalData(auditDTO);
  }

  @Test
  void testRevokeRoleMembership_Organization() throws Exception {
    Organization org = ctx.tempEntity().newOrganization();
    ctx.tempEntity().newMembershipMapping(org.getId(), DEVELOPER_ROLE_ID, "username", MemberType.USER);

    restRequest().path(APPLICATION_OR_ORGANIZATION)
        .parameter("organization", org.getId(), DEVELOPER_ROLE_ID, "user", "username")
        .delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.REVOKE_ROLE_MEMBERSHIP, null);
    assertRoleMembershipData(auditDTO, DEVELOPER_ROLE_ID);
    assertOrganizationData(auditDTO, org);
  }

  @Test
  void testRevokeRoleMembership_Application_Unauthorized() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent();
    ctx.tempEntity().newMembershipMapping(app.getId(), DEVELOPER_ROLE_ID, "username", MemberType.USER);

    restRequest().with(unauthorizedUser())
        .path(APPLICATION_OR_ORGANIZATION)
        .parameter("application", app.getId(), DEVELOPER_ROLE_ID, "user", "username")
        .delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.REVOKE_ROLE_MEMBERSHIP, "unauthorized");
    assertRoleMembershipData(auditDTO, DEVELOPER_ROLE_ID);
    assertApplicationData(auditDTO, app);
  }

  @Test
  void testRevokeRoleMembership_Organization_Unauthorized() throws Exception {
    Organization org = ctx.tempEntity().newOrganization();
    ctx.tempEntity().newMembershipMapping(org.getId(), DEVELOPER_ROLE_ID, "username", MemberType.USER);

    restRequest().with(unauthorizedUser())
        .path(APPLICATION_OR_ORGANIZATION)
        .parameter("organization", org.getId(), DEVELOPER_ROLE_ID, "user", "username")
        .delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.REVOKE_ROLE_MEMBERSHIP, "unauthorized");
    assertRoleMembershipData(auditDTO, DEVELOPER_ROLE_ID);
    assertOrganizationData(auditDTO, org);
  }

  private void assertRoleMembershipData(AuditDTO auditDTO, String roleId) {
    assertThat(auditDTO.data).containsEntry("roleId", roleId);
    assertThat(auditDTO.data).containsKey("roleMember");
    // The roleMember is a MemberDTO (UserMemberDTO or GroupMemberDTO) with username/groupName field
    Object roleMember = auditDTO.data.get("roleMember");
    assertThat(roleMember).isNotNull();
  }

  private static final class TestLogOutput
      extends LogOutput
  {
    TestLogOutput(String... loggerNames) {
      super(loggerNames);
    }

    void tearDown() {
      after();
    }
  }
}
