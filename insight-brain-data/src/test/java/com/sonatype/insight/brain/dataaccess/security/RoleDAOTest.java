/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.security;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.DescriptionHelper;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelperTest;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.notifications.Notification;
import com.sonatype.insight.brain.model.policy.notifications.RoleNotification;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.RolePermission;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RoleDAOTest
    extends AbstractDbDAOTest
{
  private final RoleDAO roleDAO = new RoleDAO();

  private Role newRole(String name) {
    return tempEntity.newRole(name, name + " description", false /* global */);
  }

  @Test
  public void testGetGlobalRoles() {
    List<Role> roles = roleDAO.getGlobalRoles();
    assertThat(roles).allMatch(Role::isGlobal).hasSizeGreaterThanOrEqualTo(2);
  }

  @Test
  public void testGetApplicationRoles() {
    List<Role> roles = roleDAO.getApplicationRoles();
    assertThat(roles).noneMatch(Role::isGlobal).extracting(Role::getName).containsExactly("Application Evaluator",
        "Component Evaluator", "Developer", "Legal Reviewer", "Owner");
  }

  @Test
  public void testDeleteCascadesToRolePermissions() {
    RolePermissionDAO rolePermissionDAO = new RolePermissionDAO();
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
    assertThat(new MembershipMappingDAO().getById(membershipMapping.getId())).isNull();
  }

  @Test
  public void testDeleteCascadesToPolicyNotifyActions() {
    Role role = newRole("cascade");
    tempEntity.newPolicy(organization);
    Policy policyWithNotifyActions = tempEntity.newPolicy(organization);
    policyWithNotifyActions.getNotifications().add(
        new RoleNotification(role.getId(), BuildStageType.ID, Notification.CONTINUOUS_MONITORING));
    PolicyDAO policyDAO = new PolicyDAO();
    policyDAO.update(policyWithNotifyActions);

    roleDAO.delete(role);

    policyWithNotifyActions = policyDAO.getById(policyWithNotifyActions.getId());
    assertThat(policyWithNotifyActions.getNotifications().getRoleNotifications()).isEmpty();
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
  public void testValidateEmptyName_Insert() {
    Role role = new Role();
    role.setName("");
    assertThatThrownBy(() -> roleDAO.insert(role)).isInstanceOf(InvalidNameException.class)
        .hasMessage("Name is required.");
  }

  @Test
  public void testValidateNameInvalidChars_Insert() {
    Role role = new Role();
    for (String name : NameHelperTest.INVALID_CHARACTERS) {
      role.setName(name);
      assertThatThrownBy(() -> roleDAO.insert(role)).isInstanceOf(InvalidNameException.class)
          .hasMessage("Name contains an invalid character: '" + name + "'.");
    }
  }

  @Test
  public void testDuplicateName_Insert() {
    Role role = new Role();
    role.setName("applicationEVALUATOR");
    assertThatThrownBy(() -> roleDAO.insert(role)).isInstanceOf(InvalidNameException.class)
        .hasMessage("A role with the same name already exists.");
  }

  @Test
  public void testValidateEmptyName_Update() {
    Role role = newRole("Test");
    role.setName("");
    assertThatThrownBy(() -> roleDAO.update(role)).isInstanceOf(InvalidNameException.class)
        .hasMessage("Name is required.");
  }

  @Test
  public void testValidateNameInvalidChars_Update() {
    Role role = newRole("Test");
    for (String name : NameHelperTest.INVALID_CHARACTERS) {
      role.setName(name);
      assertThatThrownBy(() -> roleDAO.update(role)).isInstanceOf(InvalidNameException.class)
          .hasMessage("Name contains an invalid character: '" + name + "'.");
    }
  }

  @Test
  public void testDuplicateName_Update() {
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
    Role role = new Role("Name", "Description");
    role.setGlobal(true);
    assertThatThrownBy(() -> roleDAO.insert(role)).isInstanceOf(BadRequestException.class)
        .hasMessage("Cannot add custom role 'Name' at global scope.");
  }

  @Test
  public void testCustomRoleCannotBeGlobal_Update() {
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
}
