/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiPermissionCategoryDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPermissionDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRoleDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Converted from the legacy {@code ApiRoleResourceAuditTest}.
 */
@IqH2Test
class IqH2ApiRoleResourceAuditTest
    implements AuditTestSupport
{
  private IqTestContext ctx;

  private User unauthorizedUser;

  private final TestLogOutput logOutput = new TestLogOutput(AuditRecorder.BASE_LOGGER_NAME);

  @BeforeEach
  void before() {
    logOutput.before();
    logOutput.clear();
    unauthorizedUser = ctx.tempEntity().newUser();
  }

  @AfterEach
  void after() {
    logOutput.tearDown();
  }

  @Override
  public LogOutput getLogOutput() {
    return logOutput;
  }

  @Override
  public String getUnauthorizedUsername() {
    return unauthorizedUser.getUsername();
  }

  @Override
  public PolicyDAO getPolicyDAO() {
    return ctx.lookup(PolicyDAO.class);
  }

  private Consumer<HttpRequest> unauthorizedUser() {
    return httpRequest -> httpRequest.auth(unauthorizedUser);
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(PublicApiPaths.ROLE_RESOURCE_PATH_V2);
  }

  @Test
  void testAddRole() throws Exception {
    ApiRoleDTO roleDTO = role(null, "Test Role", "Just testing", Permission.READ, Permission.VIEW_ROLES);
    roleDTO.id = restRequest().body(roleDTO).post().getBody(ApiRoleDTO.class).id;

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_ROLE, null);
    assertRoleData(auditDTO, roleDTO.id, roleDTO.name, roleDTO.description, Permission.READ, Permission.VIEW_ROLES);
  }

  @Test
  void testAddRole_Unauthorized() throws Exception {
    ApiRoleDTO roleDTO = role(null, "Test Role", "Just testing");
    restRequest().with(unauthorizedUser()).body(roleDTO).post();

    assertAuditLog(AuditEvent.CREATE_ROLE, "unauthorized");
  }

  @Test
  void testUpdateRole() throws Exception {
    ApiRoleDTO roleDTO = role(ctx.tempEntity().newRole(false).getId(), "Test Role", "Just testing", Permission.WRITE);
    restRequest().path(roleDTO.id).body(roleDTO).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_ROLE, null);
    assertRoleData(auditDTO, roleDTO.id, roleDTO.name, roleDTO.description, Permission.WRITE);
  }

  @Test
  void testUpdateRole_Unauthorized() throws Exception {
    ApiRoleDTO roleDTO = role(ctx.tempEntity().newRole(false).getId(), "Test Role", "Just testing");
    restRequest().path(roleDTO.id).with(unauthorizedUser()).body(roleDTO).put();

    assertAuditLog(AuditEvent.UPDATE_ROLE, "unauthorized");
  }

  @Test
  void testDeleteRole() throws Exception {
    Role role = ctx.tempEntity().newRole("Test Role", "Just testing", false, Permission.MANAGE_PROPRIETARY);
    restRequest().path(role.getId()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_ROLE, null);
    assertRoleData(auditDTO, role.getId(), role.getName(), role.getDescription(), Permission.MANAGE_PROPRIETARY);
  }

  @Test
  void testDeleteRole_Unauthorized() throws Exception {
    Role role = ctx.tempEntity().newRole(false);
    restRequest().path(role.getId()).with(unauthorizedUser()).delete();

    assertAuditLog(AuditEvent.DELETE_ROLE, "unauthorized");
  }

  private ApiRoleDTO role(
      final String id,
      final String name,
      final String description,
      final Permission... permissions)
  {
    ApiRoleDTO roleDTO = new ApiRoleDTO();
    roleDTO.id = id;
    roleDTO.name = name;
    roleDTO.description = description;
    ApiPermissionCategoryDTO permissionCategoryDTO = new ApiPermissionCategoryDTO();
    permissionCategoryDTO.permissions = new ArrayList<>();
    for (Permission permission : permissions) {
      ApiPermissionDTO permissionDTO = new ApiPermissionDTO();
      permissionDTO.id = permission;
      permissionDTO.displayName = permission.getDisplayName();
      permissionDTO.description = permission.getDescription();
      permissionDTO.allowed = true;
      permissionCategoryDTO.permissions.add(permissionDTO);
    }
    roleDTO.permissionCategories = Collections.singletonList(permissionCategoryDTO);
    return roleDTO;
  }

  private void assertRoleData(
      final AuditDTO auditDTO,
      final String id,
      final String name,
      final String description,
      final Permission... permissions)
  {
    List<String> permissionNames = Arrays.stream(permissions)
        .map(permission -> permission.getDisplayName() + ' ' + permission.getDescription())
        .sorted()
        .toList();
    assertCustomData(auditDTO, "roleId", id);
    assertCustomData(auditDTO, "roleName", name);
    assertCustomData(auditDTO, "roleDescription", description);
    assertCustomData(auditDTO, "grantedPermissions", permissionNames);
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
