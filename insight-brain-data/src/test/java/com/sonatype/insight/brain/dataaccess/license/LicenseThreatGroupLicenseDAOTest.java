/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.license;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupLicense;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class LicenseThreatGroupLicenseDAOTest
    extends AbstractDbDAOTest
{
  private LicenseThreatGroupLicenseDAO dao;

  private LicenseThreatGroupDAO groupDAO;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createLicenseThreatGroupLicenseDAO();
    groupDAO = daoFactory.createLicenseThreatGroupDAO();
  }

  private void testCRUD(String ownerId) {
    LicenseThreatGroup group = new LicenseThreatGroup();
    group.setOwnerId(ownerId);
    group.setName("My group");
    group.setThreatLevel(4);
    groupDAO.insert(group);

    // Create
    LicenseThreatGroupLicense licenseThreatGroupLicense = new LicenseThreatGroupLicense();
    licenseThreatGroupLicense.setOwnerId(ownerId);
    licenseThreatGroupLicense.setLicenseThreatGroupId(group.getId());
    licenseThreatGroupLicense.setLicenseId("UNSPECIFIED");
    dao.insert(licenseThreatGroupLicense);
    assertThat(licenseThreatGroupLicense.getId()).isNotNull();

    licenseThreatGroupLicense = dao.getById(licenseThreatGroupLicense.getId());
    assertThat(licenseThreatGroupLicense).isNotNull();
    assertLicenseThreatGroupLicense(ownerId, group.getId(), "UNSPECIFIED", licenseThreatGroupLicense);

    // Update
    dao.update(licenseThreatGroupLicense);

    // Delete
    dao.delete(licenseThreatGroupLicense);

    licenseThreatGroupLicense = dao.getById(licenseThreatGroupLicense.getId());
    assertThat(licenseThreatGroupLicense).isNull();
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
  public void testAddSameLicenseToSameGroupTwice() {
    LicenseThreatGroup group = new LicenseThreatGroup();
    group.setOwnerId(application.getId());
    group.setName("My group");
    groupDAO.insert(group);

    LicenseThreatGroupLicense licenseThreatGroupLicense = new LicenseThreatGroupLicense();
    licenseThreatGroupLicense.setOwnerId(application.getId());
    licenseThreatGroupLicense.setLicenseThreatGroupId(group.getId());
    licenseThreatGroupLicense.setLicenseId("UNSPECIFIED");
    dao.insert(licenseThreatGroupLicense);

    assertThatThrownBy(() -> dao.insert(
        new LicenseThreatGroupLicense(application.getId(), group.getId(), licenseThreatGroupLicense.getLicenseId())))
            .isInstanceOf(InvalidLicenseThreatGroupLicenseException.class)
            .hasMessage("The license is already in the license threat group");
  }

  @Test
  public void testInsertInvalidLicenseId() {
    LicenseThreatGroup group = new LicenseThreatGroup();
    group.setOwnerId(application.getId());
    group.setName("My group");
    group.setThreatLevel(4);
    groupDAO.insert(group);

    // Create
    LicenseThreatGroupLicense licenseThreatGroupLicense = new LicenseThreatGroupLicense();
    licenseThreatGroupLicense.setOwnerId(application.getId());
    licenseThreatGroupLicense.setLicenseThreatGroupId(group.getId());
    licenseThreatGroupLicense.setLicenseId("BAZINGAAA");
    assertThatThrownBy(() -> dao.insert(licenseThreatGroupLicense)).isInstanceOf(NotFoundException.class)
        .hasMessage("A license with ID 'BAZINGAAA' does not exist.");
  }

  @Test
  public void testSetLicenses() {
    // Delete all existing groups
    List<LicenseThreatGroup> groups = groupDAO.getByOwnerId(application.getId());
    for (LicenseThreatGroup group : groups) {
      groupDAO.delete(group);
    }

    // Add a new group
    LicenseThreatGroup group = new LicenseThreatGroup();
    group.setOwnerId(application.getId());
    group.setName("My group");
    group.setThreatLevel(4);
    groupDAO.insert(group);
    String groupId = group.getId();

    // Set one license
    Set<String> licenseIds = new LinkedHashSet<>();
    licenseIds.add("Apache-2.0");
    dao.setLicenses(groupId, licenseIds);
    List<LicenseThreatGroupLicense> licenseThreatGroupLicenses = dao.getByLicenseThreatGroupId(groupId);
    assertThat(licenseThreatGroupLicenses).hasSize(1);
    assertLicenseThreatGroupLicense(application.getId(), groupId, "Apache-2.0", licenseThreatGroupLicenses.get(0));

    // Set a different license
    licenseIds.clear();
    licenseIds.add("GPL-2.0");
    dao.setLicenses(groupId, licenseIds);
    licenseThreatGroupLicenses = dao.getByLicenseThreatGroupId(groupId);
    assertThat(licenseThreatGroupLicenses).hasSize(1);
    assertLicenseThreatGroupLicense(application.getId(), groupId, "GPL-2.0", licenseThreatGroupLicenses.get(0));

    // Set two licenses
    licenseIds.clear();
    licenseIds.add("GPL-2.0");
    licenseIds.add("Apache-2.0");
    dao.setLicenses(groupId, licenseIds);
    licenseThreatGroupLicenses = dao.getByLicenseThreatGroupId(groupId);
    assertThat(licenseThreatGroupLicenses).hasSize(2);
    assertLicenseThreatGroupLicense(application.getId(), groupId, "Apache-2.0", licenseThreatGroupLicenses.get(0));
    assertLicenseThreatGroupLicense(application.getId(), groupId, "GPL-2.0", licenseThreatGroupLicenses.get(1));

    // Set no licenses
    licenseIds.clear();
    dao.setLicenses(groupId, licenseIds);
    licenseThreatGroupLicenses = dao.getByLicenseThreatGroupId(groupId);
    assertThat(licenseThreatGroupLicenses).isEmpty();
  }

  @Test
  public void testAddSameLicenseToTwoGroups_Application() throws Exception {
    testAddSameLicenseToTwoGroups(application.getId());
  }

  @Test
  public void testAddSameLicenseToTwoGroups_Organization() throws Exception {
    testAddSameLicenseToTwoGroups(organization.getId());
  }

  private void testAddSameLicenseToTwoGroups(String ownerId) {
    LicenseThreatGroup group1 = new LicenseThreatGroup(ownerId, "My group 1", 4);
    groupDAO.insert(group1);
    LicenseThreatGroup group2 = new LicenseThreatGroup(ownerId, "My group 2", 4);
    groupDAO.insert(group2);

    LicenseThreatGroupLicense licenseThreatGroupLicense1 = new LicenseThreatGroupLicense(ownerId, group1.getId(),
        "UNSPECIFIED");
    dao.insert(licenseThreatGroupLicense1);
    assertThat(licenseThreatGroupLicense1.getId()).isNotNull();

    LicenseThreatGroupLicense licenseThreatGroupLicense2 = new LicenseThreatGroupLicense(ownerId, group2.getId(),
        "UNSPECIFIED");
    dao.insert(licenseThreatGroupLicense2);
    assertThat(licenseThreatGroupLicense2.getId()).isNotNull();

    assertThat(licenseThreatGroupLicense1.getId()).isNotEqualTo(licenseThreatGroupLicense2.getId());
  }

  private void assertLicenseThreatGroupLicense(
      String ownerId,
      String licenseThreatGroupId,
      String licenseId,
      LicenseThreatGroupLicense actual)
  {
    assertThat(actual.getOwnerId()).isEqualTo(ownerId);
    assertThat(actual.getLicenseThreatGroupId()).isEqualTo(licenseThreatGroupId);
    assertThat(actual.getLicenseId()).isEqualTo(licenseId);
  }
}
