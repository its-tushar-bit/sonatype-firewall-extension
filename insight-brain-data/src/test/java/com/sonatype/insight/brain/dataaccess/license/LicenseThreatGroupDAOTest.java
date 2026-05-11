/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.license;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.NameableDAOTest;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupLicense;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class LicenseThreatGroupDAOTest
    extends NameableDAOTest<LicenseThreatGroup>
{
  private OrganizationDAO organizationDAO;

  private LicenseThreatGroupDAO licenseThreatGroupDAO;

  private LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO;

  private LicenseDAO licenseDAO;

  @Before
  @Override
  public void setup() {
    super.setup();
    organizationDAO = daoFactory.createOrganizationDAO();
    licenseThreatGroupDAO = daoFactory.createLicenseThreatGroupDAO();
    licenseThreatGroupLicenseDAO = daoFactory.createLicenseThreatGroupLicenseDAO();
    licenseDAO = daoFactory.createLicenseDAO();
  }

  @Override
  protected LicenseThreatGroup createNameable(String a) {
    return tempEntity.newLicenseThreatGroup(organization.getId(), a, 4);
  }

  @Override
  protected AbstractOperationalSqlDAO<LicenseThreatGroup> getDao() {
    return licenseThreatGroupDAO;
  }

  @Override
  protected int getMaxNameLength() {
    return NameHelper.MAX_NAME_LENGTH;
  }

  @Override
  protected LicenseThreatGroup getEntityByName(String name) {
    return licenseThreatGroupDAO.getByOwnerIdAndName(organization.getId(), name);
  }

  private void testCRUD(String ownerId) {
    // Create
    LicenseThreatGroup group = new LicenseThreatGroup();
    group.setOwnerId(ownerId);
    group.setName("My group");
    group.setThreatLevel(4);
    licenseThreatGroupDAO.insert(group);
    assertThat(group.getId()).isNotNull();

    group = licenseThreatGroupDAO.getById(group.getId());
    assertThat(group).isNotNull();
    assertLicenseThreatGroup(ownerId, "My group", 4, group);

    // Update
    group.setName("My updated name");
    licenseThreatGroupDAO.update(group);

    group = licenseThreatGroupDAO.getById(group.getId());
    assertThat(group).isNotNull();
    assertLicenseThreatGroup(ownerId, "My updated name", 4, group);

    // Delete
    licenseThreatGroupDAO.delete(group);

    group = licenseThreatGroupDAO.getById(group.getId());
    assertThat(group).isNull();
  }

  @Test
  public void testCRUD_Application() throws Exception {
    testCRUD(application.getId());
  }

  @Test
  public void testCRUD_Organization() throws Exception {
    testCRUD(organization.getId());
  }

  @Test
  public void testCascadeDelete() {
    // Create
    LicenseThreatGroup group = new LicenseThreatGroup();
    group.setOwnerId(application.getId());
    group.setName("My group");
    group.setThreatLevel(4);
    licenseThreatGroupDAO.insert(group);
    assertThat(group.getId()).isNotNull();

    LicenseThreatGroupLicense licenseThreatGroupLicense = new LicenseThreatGroupLicense();
    licenseThreatGroupLicense.setOwnerId(application.getId());
    licenseThreatGroupLicense.setLicenseThreatGroupId(group.getId());
    licenseThreatGroupLicense.setLicenseId("UNSPECIFIED");
    licenseThreatGroupLicenseDAO.insert(licenseThreatGroupLicense);

    // Delete
    licenseThreatGroupDAO.delete(group);

    group = licenseThreatGroupDAO.getById(group.getId());
    assertThat(group).isNull();
  }

  @Test
  public void testInsertLTGInApplication_ClashesWithApplication() {
    // Add a group
    tempEntity.newLicenseThreatGroup(application.getId(), "My group", 4);

    // Add another group with the same name
    LicenseThreatGroup group = newLicenseThreatGroup(application.getId(), "mygroup");
    assertThatThrownBy(() -> licenseThreatGroupDAO.insert(group)).isInstanceOf(InvalidLicenseThreatGroupException.class)
        .hasMessage("A license threat group with the same name already exists.");
  }

  @Test
  public void testInsertLTGInApplication_ClashesWithOrganization() {
    // Add a group to the organization
    tempEntity.newLicenseThreatGroup(organization.getId(), "My group", 4);

    // Add another group with a case-/whitespace-equivalent name at application level
    assertInsertLicenseThreatGroupWithDuplicateName(application.getId(), "My group", organization);
  }

  @Test
  public void testInsertLTGInOrganization_ClashesWithApplication() {
    // Add a group to the application
    tempEntity.newLicenseThreatGroup(application.getId(), "My group", 4);

    // Add another group with a case-/whitespace-equivalent name at organization level
    assertInsertLicenseThreatGroupWithDuplicateName(organization.getId(), "My group", application);
  }

  @Test
  public void testInsertLTGInApplication_ClashesWithParentOrganization() {
    Organization parentOrganization = organizationDAO.getById(organization.getParentOrganizationId());

    // Add a group to the parent organization
    tempEntity.newLicenseThreatGroup(parentOrganization.getId(), "My group", 4);

    // Add another group with a case-/whitespace-equivalent name at application level
    assertInsertLicenseThreatGroupWithDuplicateName(application.getId(), "My group", parentOrganization);
  }

  @Test
  public void testInsertLTGInParentOrganization_ClashesWithApplication() {
    // Add a group to the application
    tempEntity.newLicenseThreatGroup(application.getId(), "My group", 4);

    // Add another group with a case-/whitespace-equivalent name at parent owner level
    assertInsertLicenseThreatGroupWithDuplicateName(organization.getParentOrganizationId(), "My group", application);
  }

  @Test
  public void testInsertLTGInOrganization_ClashesWithParentOrganization() {
    Organization parentOrganization = organizationDAO.getById(organization.getParentOrganizationId());

    // Add a group to the parent organization
    tempEntity.newLicenseThreatGroup(parentOrganization.getId(), "My group", 4);

    // Add another group with a case-/whitespace-equivalent name at organization level
    assertInsertLicenseThreatGroupWithDuplicateName(organization.getId(), "My group", parentOrganization);
  }

  @Test
  public void testInsertLTGInParentOrganization_ClashesWithOrganization() {
    // Add a group to the organization
    tempEntity.newLicenseThreatGroup(organization.getId(), "My group", 4);

    // Add another group with a case-/whitespace-equivalent name at parent owner level
    assertInsertLicenseThreatGroupWithDuplicateName(organization.getParentOrganizationId(), "My group", organization);
  }

  @Test
  public void testUpdateLTGInApplication_ClashesWithApplication() {
    // Add a group
    LicenseThreatGroup group1 = tempEntity.newLicenseThreatGroup(application.getId(), "My group 1", 4);

    // Add another group
    LicenseThreatGroup group2 = tempEntity.newLicenseThreatGroup(application.getId(), "My group 2", 4);

    // Update without changing the name
    group2.setThreatLevel(6);
    licenseThreatGroupDAO.update(group2);
    assertLicenseThreatGroup(application.getId(), "My group 2", 6, group2);

    // Update with a conflicting name
    group2.setName(group1.getName());
    assertThatThrownBy(() -> licenseThreatGroupDAO.update(group2))
        .isInstanceOf(InvalidLicenseThreatGroupException.class)
        .hasMessage("A license threat group with the same name already exists.");
  }

  @Test
  public void testUpdateLTGInApplication_ClashesWithOrganization() {
    // Add a group to the organization
    LicenseThreatGroup group1 = tempEntity.newLicenseThreatGroup(organization.getId(), "My group 1", 4);

    // Add another group to the application
    LicenseThreatGroup group2 = tempEntity.newLicenseThreatGroup(application.getId(), "My group 2", 4);

    assertUpdateLicenseThreatGroupWithDuplicateName(application.getId(), group2, group1.getName(), organization);
  }

  @Test
  public void testUpdateLTGInOrganization_ClashesWithApplication() {
    // Add a group to the organization
    LicenseThreatGroup group1 = tempEntity.newLicenseThreatGroup(organization.getId(), "My group 1", 4);

    // Add another group to the application
    LicenseThreatGroup group2 = tempEntity.newLicenseThreatGroup(application.getId(), "My group 2", 4);

    assertUpdateLicenseThreatGroupWithDuplicateName(organization.getId(), group1, group2.getName(), application);
  }

  @Test
  public void testUpdateLTGInApplication_ClashesWithParentOrganization() {
    Organization parentOrganization = organizationDAO.getById(organization.getParentOrganizationId());

    // Add a group to the parent organization
    LicenseThreatGroup group1 = tempEntity.newLicenseThreatGroup(parentOrganization.getId(), "My group 1", 4);

    // Add another group to the application
    LicenseThreatGroup group2 = tempEntity.newLicenseThreatGroup(application.getId(), "My group 2", 4);

    assertUpdateLicenseThreatGroupWithDuplicateName(application.getId(), group2, group1.getName(), parentOrganization);
  }

  @Test
  public void testUpdateLTGInParentOrganization_ClashesWithApplication() {
    String parentOrganizationId = organization.getParentOrganizationId();

    // Add a group to the parent organization
    LicenseThreatGroup group1 = tempEntity.newLicenseThreatGroup(parentOrganizationId, "My group 1", 4);

    // Add another group to the application
    LicenseThreatGroup group2 = tempEntity.newLicenseThreatGroup(application.getId(), "My group 2", 4);

    assertUpdateLicenseThreatGroupWithDuplicateName(parentOrganizationId, group1, group2.getName(), application);
  }

  //
  @Test
  public void testUpdateLTGInOrganization_ClashesWithParentOrganization() {
    Organization parentOrganization = organizationDAO.getById(organization.getParentOrganizationId());

    // Add a group to the parent organization
    LicenseThreatGroup group1 = tempEntity.newLicenseThreatGroup(parentOrganization.getId(), "My group 1", 4);

    // Add another group to the application
    LicenseThreatGroup group2 = tempEntity.newLicenseThreatGroup(organization.getId(), "My group 2", 4);

    assertUpdateLicenseThreatGroupWithDuplicateName(organization.getId(), group2, group1.getName(), parentOrganization);
  }

  @Test
  public void testUpdateLTGInParentOrganization_ClashesWithOrganization() {
    String parentOrganizationId = organization.getParentOrganizationId();

    // Add a group to the parent organization
    LicenseThreatGroup group1 = tempEntity.newLicenseThreatGroup(parentOrganizationId, "My group 1", 4);

    // Add another group to the application
    LicenseThreatGroup group2 = tempEntity.newLicenseThreatGroup(organization.getId(), "My group 2", 4);

    assertUpdateLicenseThreatGroupWithDuplicateName(parentOrganizationId, group1, group2.getName(), organization);
  }

  @Test
  public void testInsertInvalidThreatLevel() {
    LicenseThreatGroup group = new LicenseThreatGroup();
    group.setOwnerId(application.getId());
    group.setName("My group");
    group.setThreatLevel(-1);
    assertThatThrownBy(() -> licenseThreatGroupDAO.insert(group)).isInstanceOf(InvalidLicenseThreatGroupException.class)
        .hasMessage("The threat level must be a number between 0 and 10.");

    group.setThreatLevel(11);
    assertThatThrownBy(() -> licenseThreatGroupDAO.insert(group)).isInstanceOf(InvalidLicenseThreatGroupException.class)
        .hasMessage("The threat level must be a number between 0 and 10.");
  }

  @Test
  public void testUpdateInvalidThreatLevel() {
    LicenseThreatGroup group = new LicenseThreatGroup();
    group.setOwnerId(application.getId());
    group.setName("My group");
    group.setThreatLevel(1);
    licenseThreatGroupDAO.insert(group);
    group.setThreatLevel(-1);
    assertThatThrownBy(() -> licenseThreatGroupDAO.update(group)).isInstanceOf(InvalidLicenseThreatGroupException.class)
        .hasMessage("The threat level must be a number between 0 and 10.");

    group.setThreatLevel(11);
    assertThatThrownBy(() -> licenseThreatGroupDAO.update(group)).isInstanceOf(InvalidLicenseThreatGroupException.class)
        .hasMessage("The threat level must be a number between 0 and 10.");
  }

  private void assertLicenseThreatGroup(String applicationId, String name, int threatLevel, LicenseThreatGroup actual) {
    assertThat(actual.getOwnerId()).isEqualTo(applicationId);
    assertThat(actual.getName()).isEqualTo(name);
    assertThat(actual.getThreatLevel()).isEqualTo(threatLevel);
  }

  @Test
  @Override
  public void testInsert_DuplicateName() {
    createNameable("testFilterName");
    assertThatThrownBy(() -> createNameable("testFilterName")).isInstanceOf(InvalidLicenseThreatGroupException.class)
        .hasMessageContaining("A license threat group with the same name already exists.");
  }

  @Test
  public void testGetByIdNotNull() {
    assertThatThrownBy(() -> licenseThreatGroupDAO.getByIdNotNull("fake id")).isInstanceOf(NotFoundException.class)
        .hasMessage("LicenseThreatGroup with ID fake id does not exist.");
  }

  @Test
  public void testGetLicenseThreatLevelByOwnerAndLicenseIdWithHierarchy() {
    tempEntity.newLicenseThreatGroup(application.getId(), "My group 1", 0, "Apache-2.0");
    tempEntity.newLicenseThreatGroup(organization.getId(), "My group 2", 5, "GPL-2.0");
    tempEntity.newLicenseThreatGroup(organization.getParentOrganizationId(), "My group 3", 9, "GPL-3.0");

    Collection<License> licenses = licenseDAO.getAll();
    for (License license : licenses) {
      Integer threat =
          licenseThreatGroupDAO.getLicenseThreatLevelByOwnerAndLicenseIdWithHierarchy(application, license.getId());
      if (threat != null) {
        assertThat(threat).isBetween(0, 10);
      }
    }

    assertThat(licenseThreatGroupDAO.getLicenseThreatLevelByOwnerAndLicenseIdWithHierarchy(application, "Apache-2.0"))
        .isEqualTo(0);
    assertThat(licenseThreatGroupDAO.getLicenseThreatLevelByOwnerAndLicenseIdWithHierarchy(application, "GPL-2.0"))
        .isEqualTo(5);
    assertThat(licenseThreatGroupDAO.getLicenseThreatLevelByOwnerAndLicenseIdWithHierarchy(application, "GPL-3.0"))
        .isEqualTo(9);
  }

  @Test
  public void testGetLicenseThreatLevelsByApplication() {
    tempEntity.newLicenseThreatGroup(application.getId(), "My group 1", 0, "Apache-2.0", "GPL-2.0");
    tempEntity.newLicenseThreatGroup(organization.getId(), "My group 2", 5, "GPL-2.0");
    tempEntity.newLicenseThreatGroup(organization.getParentOrganizationId(), "My group 3", 9, "GPL-3.0");

    Map<String, Integer> threatLevelsByLicenseId =
        licenseThreatGroupDAO.getLicenseThreatLevelsByApplication(application);

    assertThat(threatLevelsByLicenseId.get("Apache-2.0")).isEqualTo(0);
    assertThat(threatLevelsByLicenseId.get("GPL-2.0")).isEqualTo(5);
    assertThat(threatLevelsByLicenseId.get("GPL-3.0")).isEqualTo(9);
    assertThat(threatLevelsByLicenseId).hasSize(3);
  }

  @Test
  public void testGetLicenseIdThreatGroupsByOwnerIdsAndLicenseIds() {
    tempEntity.newLicenseThreatGroup(application.getId(), "Group 1", 0, "GPL-1.0", "GPL-2.0");
    tempEntity.newLicenseThreatGroup(organization.getId(), "Group 2", 5, "MIT");
    tempEntity.newLicenseThreatGroup(organization.getParentOrganizationId(), "Group 3", 9, "GPL-1.0", "GPL-3.0");

    List<String> ownerIds = List.of(application.getId(), organization.getId(), organization.getParentOrganizationId());
    Set<String> licenseIds = Sets.newHashSet("GPL-1.0", "GPL-2.0", "MIT", "GPL-3.0", "Apache-2.0");

    try (TransactionContext tx = licenseThreatGroupDAO.createTransactionContext()) {
      Map<String, List<LicenseThreatGroup>> result = licenseThreatGroupDAO
          .getLicenseIdThreatGroupsByOwnerIdsAndLicenseIds(tx, ownerIds, licenseIds);

      assertThat(result.keySet()).containsExactlyInAnyOrder("GPL-1.0", "GPL-2.0", "MIT", "GPL-3.0");

      List<LicenseThreatGroup> groups = result.get("GPL-1.0");
      assertThat(groups.stream()
          .map(LicenseThreatGroup::getName)
          .collect(Collectors.toList()))
              .containsExactlyInAnyOrder("Group 1", "Group 3");

      groups = result.get("GPL-2.0");
      assertThat(groups.stream()
          .map(LicenseThreatGroup::getName)
          .collect(Collectors.toList()))
              .containsExactly("Group 1");

      groups = result.get("MIT");
      assertThat(groups.stream()
          .map(LicenseThreatGroup::getName)
          .collect(Collectors.toList()))
              .containsExactly("Group 2");

      groups = result.get("GPL-3.0");
      assertThat(groups.stream()
          .map(LicenseThreatGroup::getName)
          .collect(Collectors.toList()))
              .containsExactly("Group 3");
    }
  }

  @Test
  public void testGetByOwnerIdAndLicenseIdWithHierarchy() {
    tempEntity.newLicenseThreatGroup(application.getId(), "Group 1", 0, "GPL-2.0");
    tempEntity.newLicenseThreatGroup(organization.getId(), "Group 2", 5, "MIT");
    tempEntity.newLicenseThreatGroup(organization.getParentOrganizationId(), "Group 3", 9, "GPL-3.0");

    try (TransactionContext tx = licenseThreatGroupDAO.createTransactionContext()) {
      List<LicenseThreatGroup> result = licenseThreatGroupDAO.getByOwnerIdAndLicenseIdsWithHierarchy(tx,
          application.getId(), Sets.newHashSet("GPL-2.0", "MIT", "GPL-3.0"));
      assertThat(result).hasSize(3);

      assertThat(result.stream()
          .map(LicenseThreatGroup::getName)
          .collect(Collectors.toList()))
              .containsExactlyInAnyOrder("Group 1", "Group 2", "Group 3");
    }
  }

  @Test
  public void testGetHighestLicenseTheatGroupWithHierarchy() {
    Organization root = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());

    assertThat(licenseThreatGroupDAO.getHighestLicenseThreatGroupWithHierarchy(app.getId(),
        Collections.singleton("Apache-2.0"))).isNull();

    LicenseThreatGroup ltg1 = tempEntity.newLicenseThreatGroup(null, root.getId(), "name1", 7, "Apache-2.0");

    assertThat(licenseThreatGroupDAO.getHighestLicenseThreatGroupWithHierarchy(app.getId(),
        Collections.singleton("Apache-2.0"))).extracting(LicenseThreatGroup::getId).isEqualTo(ltg1.getId());

    LicenseThreatGroup ltg2 =
        tempEntity.newLicenseThreatGroup(null, org.getId(), "name2", 8, "Apache-2.0");

    assertThat(licenseThreatGroupDAO.getHighestLicenseThreatGroupWithHierarchy(app.getId(),
        Collections.singleton("Apache-2.0"))).extracting(LicenseThreatGroup::getId).isEqualTo(ltg2.getId());

    LicenseThreatGroup ltg3 = tempEntity.newLicenseThreatGroup(null, app.getId(), "name3", 9, "Apache-2.0");

    assertThat(licenseThreatGroupDAO.getHighestLicenseThreatGroupWithHierarchy(app.getId(),
        Collections.singleton("Apache-2.0"))).extracting(LicenseThreatGroup::getId).isEqualTo(ltg3.getId());

    LicenseThreatGroup ltg4 = tempEntity.newLicenseThreatGroup(null, root.getId(), "name4", 10, "Beerware");

    assertThat(licenseThreatGroupDAO.getHighestLicenseThreatGroupWithHierarchy(app.getId(),
        new HashSet<>(Arrays.asList("Apache-2.0", "Beerware"))))
            .extracting(LicenseThreatGroup::getId)
            .isEqualTo(ltg4.getId());
  }

  private void assertUpdateLicenseThreatGroupWithDuplicateName(
      final String ownerId,
      final LicenseThreatGroup group,
      final String groupName,
      final Owner expectedOwner)
  {
    // Update without changing the name
    group.setThreatLevel(6);
    licenseThreatGroupDAO.update(group);
    assertLicenseThreatGroup(ownerId, group.getName(), 6, group);

    // Update the group with a case-/whitespace-equivalent name
    group.setName(groupName.replaceAll("\\s", "").toLowerCase(Locale.ENGLISH));
    assertThatThrownBy(() -> licenseThreatGroupDAO.update(group)).isInstanceOf(InvalidLicenseThreatGroupException.class)
        .hasMessage("A license threat group with the same name already exists for the " + expectedOwner.getType() + " '"
            + expectedOwner.getName() + "'.");
  }

  private void assertInsertLicenseThreatGroupWithDuplicateName(
      final String ownerId,
      final String groupName,
      final Owner expectedOwner)
  {
    // Add a group with a case-/whitespace-equivalent name
    LicenseThreatGroup group = newLicenseThreatGroup(ownerId,
        groupName.replaceAll("\\s", "").toLowerCase(Locale.ENGLISH));
    assertThatThrownBy(() -> licenseThreatGroupDAO.insert(group)).isInstanceOf(InvalidLicenseThreatGroupException.class)
        .hasMessage("A license threat group with the same name already exists for the " + expectedOwner.getType() + " '"
            + expectedOwner.getName() + "'.");
  }

  private LicenseThreatGroup newLicenseThreatGroup(final String ownerId, final String groupName) {
    LicenseThreatGroup group = new LicenseThreatGroup();
    group.setOwnerId(ownerId);
    group.setName(groupName);
    group.setThreatLevel(5);
    return group;
  }

  @Test
  @Override
  public void testUpdate_DuplicateName() {
    createNameable("testDuplicateName");
    LicenseThreatGroup nameable1 = createNameable("testDuplicateName1");

    nameable1.setName("Test Duplicate Name");
    assertThatThrownBy(() -> getDao().update(nameable1)).isInstanceOf(InvalidLicenseThreatGroupException.class)
        .hasMessageContaining("A license threat group with the same name already exists.");
  }

  @Test
  public void testGetLicenseIdThreatGroupsByLicenseIdsWithHierarchy_returnsGroupsFromAllLevels() {
    tempEntity.newLicenseThreatGroup(application.getId(), "App Group", 3, "Apache-2.0");
    tempEntity.newLicenseThreatGroup(organization.getId(), "Org Group", 7, "MIT", "Apache-2.0");
    tempEntity.newLicenseThreatGroup(organization.getParentOrganizationId(), "Root Group", 9, "GPL-3.0");

    Map<String, List<LicenseThreatGroup>> result =
        licenseThreatGroupDAO.getLicenseIdThreatGroupsByLicenseIdsWithHierarchy(
            application.getId(), Set.of("Apache-2.0", "MIT", "GPL-3.0", "Nonexistent"));

    // Apache-2.0 is in both App Group and Org Group
    assertThat(result.get("Apache-2.0")).extracting(LicenseThreatGroup::getName)
        .containsExactlyInAnyOrder("App Group", "Org Group");

    // MIT is only in Org Group
    assertThat(result.get("MIT")).extracting(LicenseThreatGroup::getName)
        .containsExactly("Org Group");

    // GPL-3.0 is only in Root Group
    assertThat(result.get("GPL-3.0")).extracting(LicenseThreatGroup::getName)
        .containsExactly("Root Group");

    // Nonexistent license has no entry
    assertThat(result).doesNotContainKey("Nonexistent");
  }

  @Test
  public void testGetLicenseIdThreatGroupsByLicenseIdsWithHierarchy_emptyInput() {
    Map<String, List<LicenseThreatGroup>> result =
        licenseThreatGroupDAO.getLicenseIdThreatGroupsByLicenseIdsWithHierarchy(
            application.getId(), Collections.emptySet());

    assertThat(result).isEmpty();
  }

  @Test
  public void testGetLicenseIdThreatGroupsByLicenseIdsWithHierarchy_noMatchingGroups() {
    // Create groups that don't match the queried license IDs
    tempEntity.newLicenseThreatGroup(application.getId(), "Unrelated Group", 5, "BSD-3-Clause");

    Map<String, List<LicenseThreatGroup>> result =
        licenseThreatGroupDAO.getLicenseIdThreatGroupsByLicenseIdsWithHierarchy(
            application.getId(), Set.of("Apache-2.0", "MIT"));

    assertThat(result).isEmpty();
  }
}
