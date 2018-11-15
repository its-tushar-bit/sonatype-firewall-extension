/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.license;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

import com.sonatype.insight.brain.model.license.License;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class LicenseDAOTest
    extends AbstractLicenseDAOTest
{
  @Test
  public void testGetAll() {
    LicenseDAO dao = new LicenseDAO();
    List<License> licenses = dao.getAll();
    assertNotNull(licenses);
    assertTrue(licenses.size() > 0);
    for (int i = 0; i < licenses.size() - 1; i++) {
      License license1 = licenses.get(i);
      License license2 = licenses.get(i + 1);
      assertTrue(
          license1.getShortDisplayName() + " >= " + license2.getShortDisplayName(),
          license1.getShortDisplayName().toLowerCase(Locale.ENGLISH)
              .compareTo(license2.getShortDisplayName().toLowerCase(Locale.ENGLISH)) < 0);
    }
  }

  @Test
  public void testLicenseDataRefresh() {
    String newId = "new license id";
    LicenseDAO dao = new LicenseDAO();
    assertNull(dao.getById(newId));
    int count = dao.getAll().size();

    License newLicense = new License();
    newLicense.setId(newId);
    newLicense.setShortDisplayName("New short name");
    newLicense.setLongDisplayName("New long name");
    dao.insert(newLicense);
    assertNull(dao.getById(newId));

    LicenseDataUpdater.setUpdater(new DummyLicenseDataUpdater());

    assertNotNull(dao.getById(newId));
    assertEquals(count + 1, dao.getAll().size());

    dao.delete(newLicense);
    dao.load();
  }

  @Test
  public void testGetLicenseThreatLevelByOwnerAndLicenseId() {
    tempEntity.newLicenseThreatGroup(applicationId, "My group 1", 0, "Apache-2.0");
    tempEntity.newLicenseThreatGroup(organization.getId(), "My group 2", 5, "GPL-2.0");
    tempEntity.newLicenseThreatGroup(organization.getParentOrganizationId(), "My group 3", 9, "GPL-3.0");

    LicenseDAO dao = new LicenseDAO();
    Collection<License> licenses = dao.getAll();

    for (License license : licenses) {
      Integer threat = dao.getLicenseThreatLevelByOwnerAndLicenseId(application, license.getId());
      assertTrue("License Threat Level between null and 10", threat == null || (threat >= 0 && threat <= 10));
    }

    assertEquals(Integer.valueOf(0), dao.getLicenseThreatLevelByOwnerAndLicenseId(application, "Apache-2.0"));
    assertEquals(Integer.valueOf(5), dao.getLicenseThreatLevelByOwnerAndLicenseId(application, "GPL-2.0"));
    assertEquals(Integer.valueOf(9), dao.getLicenseThreatLevelByOwnerAndLicenseId(application, "GPL-3.0"));
  }
}
