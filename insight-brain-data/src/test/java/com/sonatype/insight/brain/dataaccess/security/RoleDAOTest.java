/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.security;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.NameableDAOTest;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.DescriptionHelper;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.notifications.Notification;
import com.sonatype.insight.brain.model.policy.notifications.Notifications;
import com.sonatype.insight.brain.model.policy.notifications.RoleNotification;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.policy.stages.StageReleaseStageType;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.RolePermission;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RoleDAOTest
    extends NameableDAOTest<Role>
{
  private RoleDAO roleDAO;

  private MembershipMappingDAO membershipMappingDAO;

  private PolicyDAO policyDAO;

  private RolePermissionDAO rolePermissionDAO;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    roleDAO = daoFactory.createRoleDAO();
    membershipMappingDAO = daoFactory.createMembershipMappingDAO();
    policyDAO = daoFactory.createPolicyDAO();
    rolePermissionDAO = daoFactory.createRolePermissionDAO();
  }

  private Role newRole(String name) {
    return tempEntity.newRole(name, name + " description", false /* global */);
  }

  @Override
  protected Role createNameable(String a) {
    Role role = newRole(a);
    return role;
  }

  @Override
  protected AbstractOperationalSqlDAO<Role> getDao() {
    return roleDAO;
  }

  @Override
  protected int getMaxNameLength() {
    return NameHelper.MAX_NAME_LENGTH;
  }

  @Override
  protected Role getEntityByName(String name) {
    return roleDAO.getByName(name);
  }

  @Test
  public void testGetGlobalRoles() {
    List<Role> roles = roleDAO.getGlobalRoles();
    assertThat(roles).allMatch(Role::isGlobal).hasSizeGreaterThanOrEqualTo(2);
  }

  @Test
  public void testGetApplicationRoles() {
    List<Role> roles = roleDAO.getApplicationRoles();
    assertThat(roles).noneMatch(Role::isGlobal)
        .extracting(Role::getName)
        .containsExactly("Application Evaluator",
            "Component Evaluator", "Developer", "Legal Reviewer", "Owner");
  }

  @Test
  public void testDeleteCascadesToRolePermissions() {
    Role role = newRole("cascade");
    rolePermissionDAO.insert(new RolePermission(role.getId(), Permission.values()[0]));
    roleDAO.delete(role);
    assertThat(rolePermissionDAO.getPermissionsForRole(role.getId())).isEmpty();
  }

  @Test
  public void testDeleteCascadesToMembershipMappings() {
    Role role = newRole("cascade");
    MembershipMapping membershipMapping = tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID,
        role.getId(), "username");
    roleDAO.delete(role);
    assertThat(membershipMappingDAO.getById(membershipMapping.getId())).isNull();
  }

  @Test
  public void testDeleteCascadesToPolicyNotifyActions() {
    Role role = newRole("cascade");
    Role other = newRole("other");
    tempEntity.newPolicy(organization);
    Policy policyWithNotifyActions = tempEntity.newPolicy(organization);
    policyWithNotifyActions.getNotifications()
        .add(
            new RoleNotification(role.getId(), role.getName(), BuildStageType.ID, Notification.CONTINUOUS_MONITORING));
    policyWithNotifyActions.getNotifications()
        .add(
            new RoleNotification(other.getId(), role.getName(), BuildStageType.ID, Notification.CONTINUOUS_MONITORING));
    Map<String, Notifications> policyNotificationsOverrides = new LinkedHashMap<>();
    Notifications orgNotificationsOverride = new Notifications();
    orgNotificationsOverride.add(new RoleNotification(role.getId(), role.getName(), ReleaseStageType.ID));
    orgNotificationsOverride.add(new RoleNotification(other.getId(), role.getName(), ReleaseStageType.ID));
    policyNotificationsOverrides.put("org2", orgNotificationsOverride);
    Notifications appNotificationsOverride = new Notifications();
    appNotificationsOverride.add(
        new RoleNotification(role.getId(), role.getName(), DevelopStageType.ID, StageReleaseStageType.ID));
    appNotificationsOverride.add(
        new RoleNotification(other.getId(), role.getName(), DevelopStageType.ID, StageReleaseStageType.ID));
    policyNotificationsOverrides.put("app", appNotificationsOverride);
    policyWithNotifyActions.setPolicyNotificationsOverrides(policyNotificationsOverrides);
    policyDAO.update(policyWithNotifyActions);

    roleDAO.delete(role);

    policyWithNotifyActions = policyDAO.getById(policyWithNotifyActions.getId());
    assertThat(policyWithNotifyActions.getNotifications().getRoleNotifications()).extracting(
        RoleNotification::getRoleId).containsExactly(other.getId());
    for (Notifications notifications : policyWithNotifyActions.getPolicyNotificationsOverrides().values()) {
      assertThat(notifications.getRoleNotifications()).extracting(RoleNotification::getRoleId)
          .containsExactly(other.getId());
    }
  }

  @Test
  public void testGetAll() {
    List<Role> roles = roleDAO.getAll();
    int roleCount = roles.size();
    assertThat(roleCount).isGreaterThanOrEqualTo(6);

    Role roleNonGlobal = tempEntity.newRole("AAA Non Global", false /* global */);
    roles = roleDAO.getAll();
    assertThat(roles).hasSize(roleCount + 1);
    assertThat(roles.get(0).getName()).isEqualTo(roleNonGlobal.getName());
  }

  @Test
  public void testCRUD() {
    Role role = newRole("custom");
    role = roleDAO.getByIdNotNull(role.getId());
    assertThat(role.isBuiltIn()).isFalse();
    assertThat(role.getName()).isEqualTo("custom");
    assertThat(role.getDescription()).isEqualTo("custom description");

    role.setName("Updated Name");
    role.setDescription("Updated Description");
    roleDAO.update(role);
    role = roleDAO.getByIdNotNull(role.getId());
    assertThat(role.getName()).isEqualTo("Updated Name");
    assertThat(role.getDescription()).isEqualTo("Updated Description");

    roleDAO.delete(role);
    assertThat(roleDAO.getById(role.getId())).isNull();
  }

  @Test
  @Override
  public void testInsert_DuplicateName() {
    Role role = new Role();
    role.setName("applicationEVALUATOR");
    assertThatThrownBy(() -> roleDAO.insert(role)).isInstanceOf(InvalidNameException.class)
        .hasMessage("A role with the same name already exists.");
  }

  @Test
  @Override
  public void testUpdate_DuplicateName() {
    Role role = newRole("Test");
    role.setName("applicationEVALUATOR");
    assertThatThrownBy(() -> roleDAO.update(role)).isInstanceOf(InvalidNameException.class)
        .hasMessage("A role with the same name already exists.");
  }

  @Test
  public void testBuiltInRoles() {
    List<Role> roles = roleDAO.getAll();
    assertThat(roles).hasSize(7).allMatch(Role::isBuiltIn);
  }

  @Test
  public void testBuiltInRoleCannotBeDeleted() {
    Role builtInRole = roleDAO.getByName("Owner");
    assertThat(builtInRole.isBuiltIn()).isTrue();
    Role role = new Role("Name", "Description");
    // the protection must be based on the identifier, all other properties can be fudged
    role.setId(builtInRole.getId());
    assertThat(role.isBuiltIn()).isFalse();
    assertThatThrownBy(() -> roleDAO.delete(role)).isInstanceOf(BadRequestException.class)
        .hasMessage("Cannot delete built-in role 'Owner'.");
  }

  @Test
  public void testBuiltInRoleCannotBeUpdated() {
    Role builtInRole = roleDAO.getByName("Owner");
    assertThat(builtInRole.isBuiltIn()).isTrue();
    Role role = new Role("Name", "Description");
    // the protection must be based on the identifier, all other properties can be fudged
    role.setId(builtInRole.getId());
    assertThat(role.isBuiltIn()).isFalse();
    assertThatThrownBy(() -> roleDAO.update(role)).isInstanceOf(BadRequestException.class)
        .hasMessage("Cannot change built-in role 'Owner'.");
  }

  @Test
  public void testBuiltInRoleCannotBeInserted() {
    Role role = new Role("Test", "Description");
    role.setBuiltIn(true);
    assertThatThrownBy(() -> roleDAO.insert(role)).isInstanceOf(BadRequestException.class)
        .hasMessage("Cannot add built-in role 'Test'.");
  }

  @Test
  public void testCustomRoleCannotBeGlobal_Insert() {
    // Creating proper RoleDAO
    RoleDAO roleDAO =
        new RoleDAO(false, databaseRule.getOperationalDataStore(), rolePermissionDAO, membershipMappingDAO, policyDAO);

    Role role = new Role("Name", "Description");
    role.setGlobal(true);
    assertThatThrownBy(() -> roleDAO.insert(role)).isInstanceOf(BadRequestException.class)
        .hasMessage("Cannot add custom role 'Name' at global scope.");
  }

  @Test
  public void testCustomRoleCannotBeGlobal_Update() {
    // Creating proper RoleDAO
    RoleDAO roleDAO =
        new RoleDAO(false, databaseRule.getOperationalDataStore(), rolePermissionDAO, membershipMappingDAO, policyDAO);

    Role role = newRole("Name");
    role.setGlobal(true);

    assertThatThrownBy(() -> roleDAO.update(role)).isInstanceOf(BadRequestException.class)
        .hasMessage("Cannot change custom role 'Name' to global scope.");
  }

  @Test
  public void testCustomRoleCannotBeChangedToBuiltIn() {
    Role role = newRole("Name");
    role.setBuiltIn(true);
    assertThatThrownBy(() -> roleDAO.update(role)).isInstanceOf(BadRequestException.class)
        .hasMessage("Cannot change custom role 'Name' to built-in.");
  }

  @Test
  public void testValidateDescriptionLength_Insert() {
    String description = StringUtils.repeat("a", DescriptionHelper.MAX_DESC_LENGTH);
    Role role = new Role("name", description + "a");
    assertThatThrownBy(() -> roleDAO.insert(role)).isInstanceOf(BadRequestException.class)
        .hasMessage("The description cannot be longer than 255 characters, the one supplied has 256 characters.");

    role.setDescription(description);
    roleDAO.insert(role);
    roleDAO.delete(role);
  }

  @Test
  public void testValidateDescriptionLength_Update() {
    Role role = tempEntity.newRole(false /* global */);

    String description = StringUtils.repeat("a", DescriptionHelper.MAX_DESC_LENGTH);
    role.setDescription(description + "a");
    assertThatThrownBy(() -> roleDAO.update(role)).isInstanceOf(BadRequestException.class)
        .hasMessage("The description cannot be longer than 255 characters, the one supplied has 256 characters.");

    role.setDescription(description);
    roleDAO.update(role);
  }

  @Test
  public void testValidateEmptyDescription_Insert() {
    Role role = new Role("name", " " /* description */);
    assertThatThrownBy(() -> roleDAO.insert(role)).isInstanceOf(BadRequestException.class)
        .hasMessage("The description is required.");
  }

  @Test
  public void testValidateEmptyDescription_Update() {
    Role role = tempEntity.newRole(false /* global */);

    role.setDescription(" ");
    assertThatThrownBy(() -> roleDAO.update(role)).isInstanceOf(BadRequestException.class)
        .hasMessage("The description is required.");
  }

  @Test
  public void testValidateNullDescription_Insert() {
    Role role = new Role("name", null /* description */);
    assertThatThrownBy(() -> roleDAO.insert(role)).isInstanceOf(BadRequestException.class)
        .hasMessage("The description is required.");
  }

  @Test
  public void testValidateNullDescription_Update() {
    Role role = tempEntity.newRole(false /* global */);

    role.setDescription(null);
    assertThatThrownBy(() -> roleDAO.update(role)).isInstanceOf(BadRequestException.class)
        .hasMessage("The description is required.");
  }

  @Test
  public void testGetOfuscatedRolesByUserCaseInsensitiveAndGroups() {
    String username = "username";
    String otherUsername = "otherUser";
    String group1 = "group1";
    String group2 = "group2";
    String otherGroup = "otherGroup";
    Role customRole1 = newRole("CustomRole1");
    Role customRole2 = newRole("CustomRole2");
    Role customRole3 = newRole("CustomRole3");

    Application app = tempEntity.newApplicationWithParent();

    // This user's mappings
    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, Role.SYSTEM_ADMIN_ROLE_ID, username);
    tempEntity.newMembershipMapping(app.getId(), Role.COMPONENT_EVALUATOR_ROLE_ID, "uSeRnAmE");
    tempEntity.newMembershipMapping(app.getId(), Role.OWNER_ROLE_ID, group1, MemberType.GROUP);
    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, customRole1.getId(), group2, MemberType.GROUP);
    tempEntity.newMembershipMapping(app.getOrganizationId(), customRole3.getId(), username);

    // Mappings for other users which should not affect the results
    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, Role.POLICY_ADMIN_ROLE_ID, otherGroup,
        MemberType.GROUP);
    tempEntity.newMembershipMapping(app.getId(), Role.APPLICATION_EVALUATOR_ROLE_ID, otherUsername);
    tempEntity.newMembershipMapping(app.getId(), customRole1.getId(), otherUsername);
    tempEntity.newMembershipMapping(app.getId(), customRole2.getId(), otherUsername);

    var results = roleDAO.getObfuscatedRolesByUserCaseInsensitiveAndGroups(username, Set.of(group1, group2));

    assertThat(results).containsExactlyInAnyOrder("System Administrator", "Component Evaluator", "Owner", "CUSTOM");
  }
}
