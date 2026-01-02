/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.security;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sonatype.insight.brain.common.test.PostgresTestCategory;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.db.IdUtil;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.dataaccess.TransactionContext;

import com.google.common.collect.ImmutableSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class MembershipMappingDAOTest
    extends AbstractDbDAOTest
{
  private final String contextId = "some-app";

  Role roleDeveloper;

  private MembershipMappingDAO membershipDAO;

  private RoleDAO roleDAO;

  private static final Comparator<MembershipMapping> MEMBERSHIP_COMPARATOR =
      Comparator.comparing(MembershipMapping::getContextId).thenComparing(MembershipMapping::getRoleId)
          .thenComparing(MembershipMapping::getMemberName).thenComparing(MembershipMapping::getMemberType);

  @Before
  @Override
  public void setup() {
    super.setup();
    membershipDAO = daoFactory.createMembershipMappingDAO();
    roleDAO = daoFactory.createRoleDAO();
    roleDeveloper = roleDAO.getByName("Developer");
  }

  @After
  public void cleanup() {
    for (MembershipMapping membership : membershipDAO.getByContextId(contextId)) {
      membershipDAO.delete(membership);
    }
  }

  @Test
  public void testSetMembershipMappingsForContextAndRole() {
    String roleId1 = roleDAO.getByName("Owner").getId();
    String roleId2 = roleDeveloper.getId();

    // check initial state
    List<MembershipMapping> memberships = membershipDAO.getByContextId(contextId);
    assertThat(memberships).isEmpty();

    // add mapping for first role
    List<MembershipMapping> memberships1 = Arrays.asList(new MembershipMapping("john", MemberType.USER),
        new MembershipMapping("admins", MemberType.GROUP));
    membershipDAO.setMembershipMappingsForContextAndRole(contextId, roleId1, memberships1);
    memberships = membershipDAO.getByContextId(contextId);
    assertThat(memberships).usingElementComparator(MEMBERSHIP_COMPARATOR)
        .containsExactlyInAnyOrderElementsOf(memberships1);

    // add mapping for another role
    List<MembershipMapping> memberships2 = Arrays.asList(new MembershipMapping("jane", MemberType.USER),
        new MembershipMapping("ops", MemberType.GROUP));
    membershipDAO.setMembershipMappingsForContextAndRole(contextId, roleId2, memberships2);
    memberships = membershipDAO.getByContextId(contextId);
    assertThat(memberships).usingElementComparator(MEMBERSHIP_COMPARATOR).containsExactlyInAnyOrderElementsOf(
        Stream.concat(memberships1.stream(), memberships2.stream()).collect(toList()));

    // exercise update involving keeping, removing and adding new member for a role
    memberships1 = Arrays.asList(new MembershipMapping("john", MemberType.USER), new MembershipMapping("jane",
        MemberType.USER));
    memberships2 = Collections.emptyList();
    membershipDAO.setMembershipMappingsForContextAndRole(contextId, roleId1, memberships1);
    membershipDAO.setMembershipMappingsForContextAndRole(contextId, roleId2, memberships2);
    memberships = membershipDAO.getByContextId(contextId);
    assertThat(memberships).usingElementComparator(MEMBERSHIP_COMPARATOR)
        .containsExactlyInAnyOrderElementsOf(memberships1);

    // exercise update involving change of group flag
    memberships1 = Arrays.asList(new MembershipMapping("john", MemberType.USER), new MembershipMapping("jane",
        MemberType.GROUP));
    membershipDAO.setMembershipMappingsForContextAndRole(contextId, roleId1, memberships1);
    memberships = membershipDAO.getByContextId(contextId);
    assertThat(memberships).usingElementComparator(MEMBERSHIP_COMPARATOR)
        .containsExactlyInAnyOrderElementsOf(memberships1);
  }

  @Test
  public void testSetMembershipMappingsForContextAndRole_SetSemantic() {
    String roleId1 = roleDAO.getByName("Owner").getId();

    List<MembershipMapping> memberships = Arrays.asList(new MembershipMapping("john", MemberType.USER),
        new MembershipMapping("john", MemberType.USER));
    membershipDAO.setMembershipMappingsForContextAndRole(contextId, roleId1, memberships);
    memberships = membershipDAO.getByContextId(contextId);
    assertThat(memberships).hasSize(1);
  }

  @Test
  public void testAdminUserMappedToSystemAdminAndPolicyAdminRoles() {
    List<MembershipMapping> memberships = membershipDAO.getByContextIdAndUser(MembershipMapping.GLOBAL_CONTEXT_ID,
        User.ADMIN_USERNAME);
    assertThat(memberships).hasSize(2);
    List<String> globalRoleIds = new ArrayList<>();
    for (int i = 0; i < 2; i++) {
      MembershipMapping membership = memberships.get(i);
      assertThat(membership.getMemberType()).isEqualTo(MemberType.USER);
      Role role = roleDAO.getById(membership.getRoleId());
      assertThat(role).isNotNull();
      globalRoleIds.add(role.getId());
    }
    assertThat(globalRoleIds).containsExactlyInAnyOrder(Role.SYSTEM_ADMIN_ROLE_ID, Role.POLICY_ADMIN_ROLE_ID);
  }

  @Test
  public void testUpdateNotSupported() {
    String roleId1 = roleDAO.getApplicationRoles().get(0).getId();

    List<MembershipMapping> memberships = Collections.singletonList(new MembershipMapping("john", MemberType.USER));
    membershipDAO.setMembershipMappingsForContextAndRole(contextId, roleId1, memberships);
    memberships = membershipDAO.getByContextId(contextId);
    assertThat(memberships).hasSize(1);
    MembershipMapping membership = memberships.get(0);
    membership.setMemberName("jane");
    assertThatThrownBy(() -> membershipDAO.update(membership)).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  public void testGetByRoleIdsForTestsOnly() {
    Role role1 = tempEntity.newRole(true /* global */, Permission.CONFIGURE_SYSTEM);
    Role role2 = tempEntity.newRole(true /* global */, Permission.CONFIGURE_SYSTEM);
    MembershipMapping membership = tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, role1.getId(),
        "username");
    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, role2.getId(), "username");
    List<MembershipMapping> memberships = membershipDAO.getByRoleIdsForTestsOnly(Collections.singleton(role1.getId()));
    assertThat(memberships).hasSize(1);
    assertThat(memberships.get(0).getId()).isEqualTo(membership.getId());
  }

  @Test
  public void testGetByRoleIdsForTestsOnly_emptyRoleIds() {
    List<MembershipMapping> memberships = membershipDAO.getByRoleIdsForTestsOnly(Collections.emptySet());
    assertThat(memberships).isEmpty();
  }

  @Test
  public void testGetCountByRoleIdAndMemberType() {
    Role role1 = tempEntity.newRole(true /* global */, Permission.CONFIGURE_SYSTEM);
    Role role2 = tempEntity.newRole(true /* global */, Permission.CONFIGURE_SYSTEM);
    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, role2.getId(), "username");
    assertThat(membershipDAO.getCountByRoleIdAndMemberType(role1.getId(), MemberType.USER)).isEqualTo(0);
    assertThat(membershipDAO.getCountByRoleIdAndMemberType(role2.getId(), MemberType.USER)).isEqualTo(1);
  }

  @Test
  public void testGetByUserCaseInsensitiveAndGroups() {
    String username = "uSeRnAmEiıIİ";
    String groupName = "group";

    Role userRole = tempEntity.newRole(true, Permission.CONFIGURE_SYSTEM);
    Role groupRole = tempEntity.newRole(true, Permission.CONFIGURE_SYSTEM);

    // another role that does not get associated with a mapping and which shouldn't appear in the results
    tempEntity.newRole(true, Permission.CONFIGURE_SYSTEM);

    MembershipMapping membership1 = tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID,
        userRole.getId(), username);
    MembershipMapping membership2 = tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID,
        groupRole.getId(), groupName, MemberType.GROUP);
    MembershipMapping membership3 =
        tempEntity.newMembershipMapping(application.getId(), userRole.getId(), "USERNAMEIIIİ");
    MembershipMapping membership4 =
        tempEntity.newMembershipMapping(organization.getId(), userRole.getId(), "usernameiıii̇");

    List<MembershipMapping> memberships =
        membershipDAO.getByUserCaseInsensitiveAndGroups(username, Collections.singleton(groupName));
    List<String> membershipIds = memberships.stream().map(MembershipMapping::getId).collect(Collectors.toList());

    assertThat(membershipIds).containsExactlyInAnyOrder(membership1.getId(), membership2.getId(), membership3.getId(),
        membership4.getId());
  }

  @Test
  public void testGetByUserCaseInsensitiveAndGroupsAndRoles() {
    String username = "uSeRnAmEiıIİ";
    String groupName = "group";

    Role userRole = tempEntity.newRole(true, Permission.CONFIGURE_SYSTEM);
    Role groupRole = tempEntity.newRole(true, Permission.CONFIGURE_SYSTEM);

    // another role that does not get associated with a mapping and which shouldn't appear in the results
    Role unrelatedRole = tempEntity.newRole(true, Permission.CONFIGURE_SYSTEM);

    MembershipMapping membership1 = tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID,
        userRole.getId(), username);
    MembershipMapping membership2 = tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID,
        groupRole.getId(), groupName, MemberType.GROUP);
    MembershipMapping membership3 =
        tempEntity.newMembershipMapping(application.getId(), userRole.getId(), "USERNAMEIIIİ");
    MembershipMapping membership4 =
        tempEntity.newMembershipMapping(organization.getId(), userRole.getId(), "usernameiıii̇");

    tempEntity.newMembershipMapping(organization.getId(), unrelatedRole.getId(), "usernameiıii̇");

    Set<String> roleIds = ImmutableSet.of(userRole.getId(), groupRole.getId());

    List<MembershipMapping> memberships =
        membershipDAO.getByUserCaseInsensitiveAndGroupsAndRoles(username, Collections.singleton(groupName), roleIds);
    List<String> membershipIds = memberships.stream().map(MembershipMapping::getId).collect(Collectors.toList());

    assertThat(membershipIds).containsExactlyInAnyOrder(membership1.getId(), membership2.getId(), membership3.getId(),
        membership4.getId());
  }

  @Test
  public void testGetById() {
    Role role = tempEntity.newRole(true /* global */, Permission.CONFIGURE_SYSTEM);
    MembershipMapping membership = tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, role.getId(),
        "username");
    MembershipMapping foundMembership = membershipDAO.getById(membership.getId());
    assertThat(foundMembership).isNotNull();
    assertThat(foundMembership.getId()).isEqualTo(membership.getId());
  }

  @Test
  public void testGetByRoleId() {
    Role role1 = tempEntity.newRole(true /* global */, Permission.CONFIGURE_SYSTEM);
    Role role2 = tempEntity.newRole(true /* global */, Permission.CONFIGURE_SYSTEM);
    MembershipMapping membership = tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, role1.getId(),
        "username");
    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, role2.getId(), "username");
    try (TransactionContext tx = membershipDAO.createTransactionContext()) {
      List<MembershipMapping> foundMemberships = membershipDAO.getByRoleId(tx, role1.getId());
      assertThat(foundMemberships).hasSize(1);
      assertThat(foundMemberships.get(0).getId()).isEqualTo(membership.getId());
    }
  }

  @Test
  public void testIsSystemAdmin() {
    boolean isSystemAdmin = membershipDAO.isSystemAdmin("notAnAdmin");
    assertThat(isSystemAdmin).isFalse();

    isSystemAdmin = membershipDAO.isSystemAdmin(User.ADMIN_USERNAME); // built-in admin
    assertThat(isSystemAdmin).isTrue();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testInsertAll_onlyNewMembershipsPostgres() throws SQLException {
    doInsertAll_onlyNewMemberships();
  }

  @Test
  public void testInsertAll_onlyNewMembershipsH2() throws SQLException {
    doInsertAll_onlyNewMemberships();
  }

  private void doInsertAll_onlyNewMemberships() throws SQLException {
    List<String> usernames = new ArrayList<>();
    usernames.add("user1");
    usernames.add("user2");
    usernames.add("user3");
    List<MembershipMapping> membershipMappings = createUserMembershipMappings(usernames);

    MembershipMapping newMembership1 = new MembershipMapping(application.getId(), roleDeveloper.getId(),
        "user4", MemberType.USER);
    MembershipMapping newMembership2 = new MembershipMapping(application.getId(), roleDeveloper.getId(),
        "user5", MemberType.USER);

    List<MembershipMapping> membershipsToInsert = Arrays.asList(newMembership1, newMembership2);
    membershipDAO.insertAll(membershipsToInsert);

    membershipMappings.addAll(membershipsToInsert);

    List<MembershipMapping> storedMemberships = membershipDAO
        .getByRoleIdsForTestsOnly(Collections.singleton(roleDeveloper.getId()));
    assertThat(storedMemberships.stream()
        .map(MembershipMapping::getMemberName).collect(Collectors.toList()))
        .hasSize(5)
        .containsExactlyInAnyOrder(
            membershipMappings.stream()
                .map(MembershipMapping::getMemberName).toArray(String[]::new));
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testInsertAll_someMembershipsExist_notFailAndIgnorePostgres() throws SQLException {
    doInsertAll_someMembershipsExist();
  }

  @Test
  public void testInsertAll_someMembershipsExist_notFailAndIgnoreH2() throws SQLException {
    doInsertAll_someMembershipsExist();
  }

  private void doInsertAll_someMembershipsExist() throws SQLException {
    List<String> usernames = new ArrayList<>();
    usernames.add("user1");
    usernames.add("user2");
    usernames.add("user3");
    usernames.add("user4");
    usernames.add("user5");
    List<MembershipMapping> membershipMappings = createUserMembershipMappings(usernames);
    membershipMappings.sort(Comparator.comparing(MembershipMapping::getMemberName));

    MembershipMapping newMembership1 = new MembershipMapping(application.getId(), roleDeveloper.getId(),
        "user6", MemberType.USER);
    MembershipMapping existingMembership1 = membershipMappings.get(0);
    MembershipMapping existingMembership2 = membershipMappings.get(1);
    existingMembership1.setId(IdUtil.newUUID());
    existingMembership2.setId(IdUtil.newUUID());

    List<MembershipMapping> membershipsToInsert = Arrays.asList(newMembership1, existingMembership1,
        existingMembership2);
    membershipDAO.insertAll(membershipsToInsert);

    membershipMappings.add(newMembership1);

    List<MembershipMapping> storedMemberships = membershipDAO
        .getByRoleIdsForTestsOnly(Collections.singleton(roleDeveloper.getId()));
    assertThat(storedMemberships.stream()
        .map(MembershipMapping::getMemberName).collect(Collectors.toList()))
        .hasSize(6)
        .containsExactlyInAnyOrder(
            membershipMappings.stream()
                .map(MembershipMapping::getMemberName).toArray(String[]::new));
  }

  private List<MembershipMapping> createUserMembershipMappings(List<String> usernames) {
    List<MembershipMapping> createdMembershipMappings = new ArrayList<>(usernames.size());
    for (String username : usernames) {
      MembershipMapping membershipMapping = new MembershipMapping(application.getId(), roleDeveloper.getId(),
          username, MemberType.USER);
      membershipDAO.insert(membershipMapping);
      createdMembershipMappings.add(membershipMapping);
    }
    return createdMembershipMappings;
  }

  @Test
  public void testIsUserHavingRolesInAnyContext_ByUsername() {
    Role role1 = tempEntity.newRole(true /* global */, Permission.CONFIGURE_SYSTEM);
    Role role2 = tempEntity.newRole(true /* global */, Permission.CONFIGURE_SYSTEM);
    Set<String> noGroups = Collections.emptySet();
    Set<String> bothRoleIds = Set.of(role1.getId(), role2.getId());
    assertThat(membershipDAO.isUserHavingRolesInAnyContext(Collections.singleton(role1.getId()), "username",
        noGroups)).isFalse();
    assertThat(membershipDAO.isUserHavingRolesInAnyContext(Collections.singleton(role2.getId()), "username",
        noGroups)).isFalse();
    assertThat(membershipDAO.isUserHavingRolesInAnyContext(bothRoleIds, "username", noGroups)).isFalse();

    MembershipMapping membershipMappingRole1 =
        tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, role1.getId(), "username");
    assertThat(membershipDAO.isUserHavingRolesInAnyContext(Collections.singleton(role1.getId()), "username",
        noGroups)).isTrue();
    assertThat(membershipDAO.isUserHavingRolesInAnyContext(Collections.singleton(role2.getId()), "username",
        noGroups)).isFalse();
    assertThat(membershipDAO.isUserHavingRolesInAnyContext(bothRoleIds, "username", noGroups)).isTrue();

    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, role2.getId(), "username");
    assertThat(membershipDAO.isUserHavingRolesInAnyContext(Collections.singleton(role1.getId()), "username",
        noGroups)).isTrue();
    assertThat(membershipDAO.isUserHavingRolesInAnyContext(Collections.singleton(role2.getId()), "username",
        noGroups)).isTrue();
    assertThat(membershipDAO.isUserHavingRolesInAnyContext(bothRoleIds, "username", noGroups)).isTrue();

    membershipDAO.delete(membershipMappingRole1);
    assertThat(membershipDAO.isUserHavingRolesInAnyContext(Collections.singleton(role1.getId()), "username",
        noGroups)).isFalse();
    assertThat(membershipDAO.isUserHavingRolesInAnyContext(Collections.singleton(role2.getId()), "username",
        noGroups)).isTrue();
    assertThat(membershipDAO.isUserHavingRolesInAnyContext(bothRoleIds, "username", noGroups)).isTrue();
  }

  @Test
  public void testIsUserHavingRolesInAnyContext_ByGroups() {
    Role role1 = tempEntity.newRole(true /* global */, Permission.CONFIGURE_SYSTEM);
    Role role2 = tempEntity.newRole(true /* global */, Permission.CONFIGURE_SYSTEM);
    Set<String> bothRoleIds = Set.of(role1.getId(), role2.getId());
    Set<String> bothGroupIds = Set.of("group1", "group2");
    assertThat(membershipDAO.isUserHavingRolesInAnyContext(Collections.singleton(role1.getId()), "username",
        Collections.singleton("group1")))
        .isFalse();
    assertThat(membershipDAO.isUserHavingRolesInAnyContext(Collections.singleton(role2.getId()), "username",
        Collections.singleton("group1")))
        .isFalse();
    assertThat(membershipDAO.isUserHavingRolesInAnyContext(bothRoleIds, "username", bothGroupIds)).isFalse();

    MembershipMapping membershipMappingRole1 =
        tempEntity.newGroupMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, role1.getId(), "group1");
    assertThat(membershipDAO.isUserHavingRolesInAnyContext(Collections.singleton(role1.getId()), "username",
        Collections.singleton("group1"))).isTrue();
    assertThat(membershipDAO.isUserHavingRolesInAnyContext(Collections.singleton(role2.getId()), "username",
        Collections.singleton("group1"))).isFalse();
    assertThat(membershipDAO.isUserHavingRolesInAnyContext(Collections.singleton(role1.getId()), "username",
        Collections.singleton("group2"))).isFalse();
    assertThat(membershipDAO.isUserHavingRolesInAnyContext(Collections.singleton(role2.getId()), "username",
        Collections.singleton("group2"))).isFalse();
    assertThat(membershipDAO.isUserHavingRolesInAnyContext(bothRoleIds, "username", bothGroupIds)).isTrue();

    tempEntity.newGroupMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, role2.getId(), "group2");
    assertThat(membershipDAO.isUserHavingRolesInAnyContext(Collections.singleton(role1.getId()), "username",
        Collections.singleton("group1"))).isTrue();
    assertThat(membershipDAO.isUserHavingRolesInAnyContext(Collections.singleton(role2.getId()), "username",
        Collections.singleton("group1"))).isFalse();
    assertThat(membershipDAO.isUserHavingRolesInAnyContext(Collections.singleton(role1.getId()), "username",
        Collections.singleton("group2"))).isFalse();
    assertThat(membershipDAO.isUserHavingRolesInAnyContext(Collections.singleton(role2.getId()), "username",
        Collections.singleton("group2"))).isTrue();
    assertThat(membershipDAO.isUserHavingRolesInAnyContext(bothRoleIds, "username", bothGroupIds)).isTrue();

    membershipDAO.delete(membershipMappingRole1);
    assertThat(membershipDAO.isUserHavingRolesInAnyContext(Collections.singleton(role1.getId()), "username",
        Collections.singleton("group1"))).isFalse();
    assertThat(membershipDAO.isUserHavingRolesInAnyContext(Collections.singleton(role2.getId()), "username",
        Collections.singleton("group1"))).isFalse();
    assertThat(membershipDAO.isUserHavingRolesInAnyContext(Collections.singleton(role1.getId()), "username",
        Collections.singleton("group2"))).isFalse();
    assertThat(membershipDAO.isUserHavingRolesInAnyContext(Collections.singleton(role2.getId()), "username",
        Collections.singleton("group2"))).isTrue();
    assertThat(membershipDAO.isUserHavingRolesInAnyContext(bothRoleIds, "username", bothGroupIds)).isTrue();
  }
}
