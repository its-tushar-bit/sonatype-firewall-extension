/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class LicenseThreatGroupLicenseDAOTest
    extends AbstractDbDAOTest
{
  @Before
  public void before() {
    createDefaultApplication();
  }

  private void testCRUD(String ownerId) throws Exception {
    LicenseThreatGroupDAO groupDAO = new LicenseThreatGroupDAO();
    LicenseThreatGroup group = new LicenseThreatGroup();
    group.setOwnerId(ownerId);
    group.setName("My group");
    group.setThreatLevel(4);
    groupDAO.insert(group);

    LicenseThreatGroupLicenseDAO dao = new LicenseThreatGroupLicenseDAO();

    // Create
    LicenseThreatGroupLicense licenseThreatGroupLicense = new LicenseThreatGroupLicense();
    licenseThreatGroupLicense.setOwnerId(ownerId);
    licenseThreatGroupLicense.setLicenseThreatGroupId(group.getId());
    licenseThreatGroupLicense.setLicenseId("UNSPECIFIED");
    dao.insert(licenseThreatGroupLicense);
    Assert.assertNotNull(licenseThreatGroupLicense.getId());

    licenseThreatGroupLicense = dao.getById(licenseThreatGroupLicense.getId());
    Assert.assertNotNull(licenseThreatGroupLicense);
    assertLicenseThreatGroupLicense(ownerId, group.getId(), "UNSPECIFIED", licenseThreatGroupLicense);

    // Update
    dao.update(licenseThreatGroupLicense);

    // Delete
    dao.delete(licenseThreatGroupLicense);

    licenseThreatGroupLicense = dao.getById(licenseThreatGroupLicense.getId());
    Assert.assertNull(licenseThreatGroupLicense);
  }

  @Test
  public void testCRUD_Application() throws Exception {
    testCRUD(applicationId);
  }

  @Test
  public void testCRUD_Organization() throws Exception {
    testCRUD(organization.getId());
  }

  @Test
  public void testAddSameLicenseToSameGroupTwice() throws Exception {
    LicenseThreatGroupDAO groupDAO = new LicenseThreatGroupDAO();
    LicenseThreatGroup group = new LicenseThreatGroup();
    group.setOwnerId(applicationId);
    group.setName("My group");
    groupDAO.insert(group);

    LicenseThreatGroupLicenseDAO dao = new LicenseThreatGroupLicenseDAO();
    LicenseThreatGroupLicense licenseThreatGroupLicense = new LicenseThreatGroupLicense();
    licenseThreatGroupLicense.setOwnerId(applicationId);
    licenseThreatGroupLicense.setLicenseThreatGroupId(group.getId());
    licenseThreatGroupLicense.setLicenseId("UNSPECIFIED");
    dao.insert(licenseThreatGroupLicense);

    licenseThreatGroupLicense = new LicenseThreatGroupLicense();
    licenseThreatGroupLicense.setOwnerId(applicationId);
    licenseThreatGroupLicense.setLicenseThreatGroupId(group.getId());
    licenseThreatGroupLicense.setLicenseId("UNSPECIFIED");
    try {
      dao.insert(licenseThreatGroupLicense);
      Assert.fail("Expected InvalidLicenseThreatGroupLicenseException");
    }
    catch (InvalidLicenseThreatGroupLicenseException expected) {
      if (!"The license is already in the license threat group".equals(expected.getMessage())) {
        throw expected;
      }
    }
  }

  @Test
  public void testInsertInvalidLicenseId() throws Exception {
    LicenseThreatGroupDAO groupDAO = new LicenseThreatGroupDAO();
    LicenseThreatGroup group = new LicenseThreatGroup();
    group.setOwnerId(applicationId);
    group.setName("My group");
    group.setThreatLevel(4);
    groupDAO.insert(group);

    LicenseThreatGroupLicenseDAO dao = new LicenseThreatGroupLicenseDAO();

    // Create
    LicenseThreatGroupLicense licenseThreatGroupLicense = new LicenseThreatGroupLicense();
    licenseThreatGroupLicense.setOwnerId(applicationId);
    licenseThreatGroupLicense.setLicenseThreatGroupId(group.getId());
    licenseThreatGroupLicense.setLicenseId("BAZINGAAA");
    try {
      dao.insert(licenseThreatGroupLicense);
      Assert.fail("Expected NotFoundException");
    }
    catch (NotFoundException expected) {
      if (!"A license with id 'BAZINGAAA' does not exist.".equals(expected.getMessage())) {
        throw expected;
      }
    }
  }

  @Test
  public void testSetLicenses() {
    LicenseThreatGroupDAO groupDAO = new LicenseThreatGroupDAO();

    // Delete all existing groups
    List<LicenseThreatGroup> groups = groupDAO.getByOwnerId(applicationId);
    for (LicenseThreatGroup group : groups) {
      groupDAO.delete(group);
    }

    // Add a new group
    LicenseThreatGroup group = new LicenseThreatGroup();
    group.setOwnerId(applicationId);
    group.setName("My group");
    group.setThreatLevel(4);
    groupDAO.insert(group);
    String groupId = group.getId();

    LicenseThreatGroupLicenseDAO dao = new LicenseThreatGroupLicenseDAO();

    // Set one license
    Set<String> licenseIds = new LinkedHashSet<String>();
    licenseIds.add("Apache-2.0");
    dao.setLicenses(groupId, licenseIds);
    List<LicenseThreatGroupLicense> licenseThreatGroupLicenses = dao.getByLicenseThreatGroupId(groupId);
    Assert.assertEquals(1, licenseThreatGroupLicenses.size());
    assertLicenseThreatGroupLicense(applicationId, groupId, "Apache-2.0", licenseThreatGroupLicenses.get(0));

    // Set a different license
    licenseIds.clear();
    licenseIds.add("GPL-2.0");
    dao.setLicenses(groupId, licenseIds);
    licenseThreatGroupLicenses = dao.getByLicenseThreatGroupId(groupId);
    Assert.assertEquals(1, licenseThreatGroupLicenses.size());
    assertLicenseThreatGroupLicense(applicationId, groupId, "GPL-2.0", licenseThreatGroupLicenses.get(0));

    // Set two licenses
    licenseIds.clear();
    licenseIds.add("GPL-2.0");
    licenseIds.add("Apache-2.0");
    dao.setLicenses(groupId, licenseIds);
    licenseThreatGroupLicenses = dao.getByLicenseThreatGroupId(groupId);
    Assert.assertEquals(2, licenseThreatGroupLicenses.size());
    assertLicenseThreatGroupLicense(applicationId, groupId, "Apache-2.0", licenseThreatGroupLicenses.get(0));
    assertLicenseThreatGroupLicense(applicationId, groupId, "GPL-2.0", licenseThreatGroupLicenses.get(1));

    // Set no licenses
    licenseIds.clear();
    dao.setLicenses(groupId, licenseIds);
    licenseThreatGroupLicenses = dao.getByLicenseThreatGroupId(groupId);
    Assert.assertEquals(0, licenseThreatGroupLicenses.size());
  }

  @Test
  public void testAddSameLicenseToTwoGroups_Application() throws Exception {
    testAddSameLicenseToTwoGroups(applicationId);
  }

  @Test
  public void testAddSameLicenseToTwoGroups_Organization() throws Exception {
    testAddSameLicenseToTwoGroups(organization.getId());
  }

  private void testAddSameLicenseToTwoGroups(String ownerId) throws Exception {
    LicenseThreatGroupDAO groupDAO = new LicenseThreatGroupDAO();
    LicenseThreatGroup group1 = new LicenseThreatGroup(ownerId, "My group 1", 4);
    groupDAO.insert(group1);
    LicenseThreatGroup group2 = new LicenseThreatGroup(ownerId, "My group 2", 4);
    groupDAO.insert(group2);

    LicenseThreatGroupLicenseDAO dao = new LicenseThreatGroupLicenseDAO();

    LicenseThreatGroupLicense licenseThreatGroupLicense1 = new LicenseThreatGroupLicense(ownerId, group1.getId(),
        "UNSPECIFIED");
    dao.insert(licenseThreatGroupLicense1);
    Assert.assertNotNull(licenseThreatGroupLicense1.getId());

    LicenseThreatGroupLicense licenseThreatGroupLicense2 = new LicenseThreatGroupLicense(ownerId, group2.getId(),
        "UNSPECIFIED");
    dao.insert(licenseThreatGroupLicense2);
    Assert.assertNotNull(licenseThreatGroupLicense2.getId());

    Assert.assertFalse(licenseThreatGroupLicense1.getId() == licenseThreatGroupLicense2.getId());
  }

  private void assertLicenseThreatGroupLicense(String ownerId, String licenseThreatGroupId, String licenseId,
      LicenseThreatGroupLicense actual)
  {
    Assert.assertEquals(ownerId, actual.getOwnerId());
    Assert.assertEquals(licenseThreatGroupId, actual.getLicenseThreatGroupId());
    Assert.assertEquals(licenseId, actual.getLicenseId());
  }
}
