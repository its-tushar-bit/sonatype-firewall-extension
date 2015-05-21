/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.security;

import java.util.List;

import com.sonatype.clm.dto.model.policy.NotifyAction;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.DescriptionHelper;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelperTest;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.actions.NotifyActionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.RolePermission;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.fail;

public class RoleDAOTest
    extends AbstractDbDAOTest
{
  private RoleDAO roleDAO = new RoleDAO();

  private Role newRole(String name) {
    return tempEntity.newRole(name, name + " description", false /* global */);
  }

  @Test
  public void testGetGlobalRoles() throws Exception {
    List<Role> roles = roleDAO.getGlobalRoles();
    int roleCount = roles.size();
    assertThat(roleCount, is(greaterThanOrEqualTo(2)));
    for (Role role : roles) {
      assertThat(role.isGlobal(), is(true));
    }
  }

  @Test
  public void testGetApplicationRoles() throws Exception {
    List<Role> roles = roleDAO.getApplicationRoles();
    assertThat(roles, is(notNullValue()));
    assertThat(roles, hasSize(4));
    Role role = roles.get(0);
    assertThat(role.getName(), is("Application Evaluator"));
    assertThat(role.isGlobal(), is(false));
    role = roles.get(1);
    assertThat(role.getName(), is("Component Evaluator"));
    assertThat(role.isGlobal(), is(false));
    role = roles.get(2);
    assertThat(role.getName(), is("Developer"));
    assertThat(role.isGlobal(), is(false));
    role = roles.get(3);
    assertThat(role.getName(), is("Owner"));
    assertThat(role.isGlobal(), is(false));
  }

  @Test
  public void testDeleteCascadesToRolePermissions() throws Exception {
    RolePermissionDAO rolePermissionDAO = new RolePermissionDAO();
    Role role = newRole("cascade");
    rolePermissionDAO.insert(new RolePermission(role.getId(), Permission.values()[0]));
    roleDAO.delete(role);
    assertThat(rolePermissionDAO.getPermissionsForRole(role.getId()), is(empty()));
  }

  @Test
  public void testDeleteCascadesToMembershipMappings() throws Exception {
    Role role = newRole("cascade");
    MembershipMapping membershipMapping = tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID,
        role.getId(), "username");
    roleDAO.delete(role);
    assertThat(new MembershipMappingDAO().getById(membershipMapping.getId()), nullValue());
  }

  @Test
  public void testDeleteCascadesToPolicyNotifyActions() throws Exception {
    Role role = newRole("cascade");
    tempEntity.newPolicy(organization.getId(), "Test Policy without Actions");
    Policy policyWithNotifyActions = tempEntity.newPolicy(organization.getId(), "Test Policy with Notify Actions");
    NotifyAction notifyAction1 = new NotifyAction(role.getId(), NotifyActionType.TARGET_TYPE_ROLE);
    policyWithNotifyActions.addAction(BuildStageType.ID, notifyAction1);
    NotifyAction notifyAction2 = new NotifyAction(role.getId(), NotifyActionType.TARGET_TYPE_ROLE);
    policyWithNotifyActions.addMonitorNotifyAction(notifyAction2);
    PolicyDAO policyDAO = new PolicyDAO();
    policyDAO.update(policyWithNotifyActions);

    roleDAO.delete(role);

    policyWithNotifyActions = policyDAO.getById(policyWithNotifyActions.getId());
    assertThat(policyWithNotifyActions.getActions(BuildStageType.ID), hasSize(0));
    assertThat(policyWithNotifyActions.getMonitorNotifyActions(), hasSize(0));
  }

  @Test
  public void testGetAll() {
    List<Role> roles = roleDAO.getAll();
    int roleCount = roles.size();
    assertThat(roleCount, is(greaterThanOrEqualTo(6)));

    Role roleNonGlobal = tempEntity.newRole("AAA Non Global", false /* global */);
    roles = roleDAO.getAll();
    assertThat(roles, hasSize(roleCount + 1));
    assertThat(roles.get(0).getName(), is(roleNonGlobal.getName()));
  }

  @Test
  public void testCRUD() {
    Role role = newRole("custom");
    role = roleDAO.getByIdNotNull(role.getId());
    assertThat(role.isBuiltIn(), is(false));
    assertThat(role.getName(), is("custom"));
    assertThat(role.getDescription(), is("custom description"));

    role.setName("Updated Name");
    role.setDescription("Updated Description");
    roleDAO.update(role);
    role = roleDAO.getByIdNotNull(role.getId());
    assertThat(role.getName(), is("Updated Name"));
    assertThat(role.getDescription(), is("Updated Description"));

    roleDAO.delete(role);
    assertThat(roleDAO.getById(role.getId()), is(nullValue()));
  }

  @Test
  public void testValidateEmptyName_Insert() {
    Role role = new Role();
    role.setName("");
    try {
      roleDAO.insert(role);
      fail("Expected exception");
    }
    catch (InvalidNameException e) {
      assertThat(e.getMessage(), is("Name is required."));
    }
  }

  @Test
  public void testValidateNameInvalidChars_Insert() {
    Role role = new Role();
    for (String name : NameHelperTest.INVALID_CHARACTERS) {
      role.setName(name);
      try {
        roleDAO.insert(role);
        fail("Expected exception");
      }
      catch (InvalidNameException e) {
        assertThat(e.getMessage(), is("Name contains an invalid character: '" + name + "'."));
      }
    }
  }

  @Test
  public void testDuplicateName_Insert() {
    Role role = new Role();
    role.setName("applicationEVALUATOR");
    try {
      roleDAO.insert(role);
      fail("Expected exception");
    }
    catch (InvalidNameException e) {
      assertThat(e.getMessage(), is("A role with the same name already exists."));
    }
  }

  @Test
  public void testValidateEmptyName_Update() {
    Role role = newRole("Test");
    role.setName("");
    try {
      roleDAO.update(role);
      fail("Expected exception");
    }
    catch (InvalidNameException e) {
      assertThat(e.getMessage(), is("Name is required."));
    }
  }

  @Test
  public void testValidateNameInvalidChars_Update() {
    Role role = newRole("Test");
    for (String name : NameHelperTest.INVALID_CHARACTERS) {
      role.setName(name);
      try {
        roleDAO.update(role);
        fail("Expected exception");
      }
      catch (InvalidNameException e) {
        assertThat(e.getMessage(), is("Name contains an invalid character: '" + name + "'."));
      }
    }
  }

  @Test
  public void testDuplicateName_Update() {
    Role role = newRole("Test");
    role.setName("applicationEVALUATOR");
    try {
      roleDAO.update(role);
      fail("Expected exception");
    }
    catch (InvalidNameException e) {
      assertThat(e.getMessage(), is("A role with the same name already exists."));
    }
  }

  @Test
  public void testBuiltInRoles() {
    List<Role> roles = roleDAO.getAll();
    assertThat(roles, hasSize(6));
    for (Role role : roles) {
      assertThat(role.toString(), role.isBuiltIn(), is(true));
    }
  }

  @Test
  public void testBuiltInRoleCannotBeDeleted() {
    Role builtInRole = roleDAO.getByName("Owner");
    assertThat(builtInRole.isBuiltIn(), is(true));
    Role role = new Role("Name", "Description");
    // the protection must be based on the identifier, all other properties can be fudged
    role.setId(builtInRole.getId());
    assertThat(role.isBuiltIn(), is(false));
    try {
      roleDAO.delete(role);
      fail("Expected exception");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), is("Cannot delete built-in role 'Owner'."));
    }
  }

  @Test
  public void testBuiltInRoleCannotBeUpdated() {
    Role builtInRole = roleDAO.getByName("Owner");
    assertThat(builtInRole.isBuiltIn(), is(true));
    Role role = new Role("Name", "Description");
    // the protection must be based on the identifier, all other properties can be fudged
    role.setId(builtInRole.getId());
    assertThat(role.isBuiltIn(), is(false));
    try {
      roleDAO.update(role);
      fail("Expected exception");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), is("Cannot change built-in role 'Owner'."));
    }
  }

  @Test
  public void testBuiltInRoleCannotBeInserted() {
    Role role = new Role("Test", "Description");
    role.setBuiltIn(true);
    try {
      roleDAO.insert(role);
      fail("Expected exception");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), is("Cannot add built-in role 'Test'."));
    }
  }

  @Test
  public void testCustomRoleCannotBeGlobal_Insert() {
    Role role = new Role("Name", "Description");
    role.setGlobal(true);
    try {
      roleDAO.insert(role);
      fail("Expected exception");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), is("Cannot add custom role 'Name' at global scope."));
    }
  }

  @Test
  public void testCustomRoleCannotBeGlobal_Update() {
    Role role = newRole("Name");
    role.setGlobal(true);
    try {
      roleDAO.update(role);
      fail("Expected exception");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), is("Cannot change custom role 'Name' to global scope."));
    }
  }

  @Test
  public void testCustomRoleCannotBeChangedToBuiltIn() {
    Role role = newRole("Name");
    role.setBuiltIn(true);
    try {
      roleDAO.update(role);
      fail("Expected exception");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), is("Cannot change custom role 'Name' to built-in."));
    }
  }

  @Test
  public void testValidateDescriptionLength_Insert() {
    String description = StringUtils.repeat("a", DescriptionHelper.MAX_DESC_LENGTH);
    Role role = new Role("name", description + "a");
    try {
      roleDAO.insert(role);
      fail("Expected BadRequestException");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(),
          is("The description cannot be longer than 255 characters, the one supplied has 256 characters."));
    }

    role.setDescription(description);
    roleDAO.insert(role);
    roleDAO.delete(role);
  }

  @Test
  public void testValidateDescriptionLength_Update() {
    Role role = tempEntity.newRole(false /* global */);

    String description = StringUtils.repeat("a", DescriptionHelper.MAX_DESC_LENGTH);
    role.setDescription(description + "a");
    try {
      roleDAO.update(role);
      fail("Expected BadRequestException");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(),
          is("The description cannot be longer than 255 characters, the one supplied has 256 characters."));
    }

    role.setDescription(description);
    roleDAO.update(role);
  }

  @Test
  public void testValidateEmptyDescription_Insert() {
    Role role = new Role("name", " " /* description */);
    try {
      roleDAO.insert(role);
      fail("Expected BadRequestException");
    }
    catch (BadRequestException expected) {
      assertThat(expected.getMessage(), is("The description is required."));
    }
  }

  @Test
  public void testValidateEmptyDescription_Update() {
    Role role = tempEntity.newRole(false /* global */);

    role.setDescription(" ");
    try {
      roleDAO.update(role);
      fail("Expected BadRequestException");
    }
    catch (BadRequestException expected) {
      assertThat(expected.getMessage(), is("The description is required."));
    }
  }

  @Test
  public void testValidateNullDescription_Insert() {
    Role role = new Role("name", null /* description */);
    try {
      roleDAO.insert(role);
      fail("Expected BadRequestException");
    }
    catch (BadRequestException expected) {
      assertThat(expected.getMessage(), is("The description is required."));
    }
  }

  @Test
  public void testValidateNullDescription_Update() {
    Role role = tempEntity.newRole(false /* global */);

    role.setDescription(null);
    try {
      roleDAO.update(role);
      fail("Expected BadRequestException");
    }
    catch (BadRequestException expected) {
      assertThat(expected.getMessage(), is("The description is required."));
    }
  }
}
