/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.license;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import com.sonatype.insight.brain.model.license.License;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LicenseDAOTest
    extends AbstractLicenseDAOTest
{
  @Test
  public void testGetAll() {
    LicenseDAO dao = new LicenseDAO();
    List<License> licenses = dao.getAll();
    assertThat(licenses).isNotEmpty()
        .isSortedAccordingTo(Comparator.comparing(License::getShortDisplayName, String.CASE_INSENSITIVE_ORDER));
  }

  @Test
  public void testLicenseDataRefresh() {
    String newId = "new license id";
    LicenseDAO dao = new LicenseDAO();
    assertThat(dao.getById(newId)).isNull();
    int count = dao.getAll().size();

    License newLicense = new License();
    newLicense.setId(newId);
    newLicense.setShortDisplayName("New short name");
    newLicense.setLongDisplayName("New long name");
    dao.insert(newLicense);
    assertThat(dao.getById(newId)).isNull();

    LicenseDataUpdater.setUpdater(new DummyLicenseDataUpdater());

    assertThat(dao.getById(newId)).isNotNull();
    assertThat(dao.getAll()).hasSize(count + 1);

    dao.delete(newLicense);
    dao.load();
  }

  @Test
  public void testGetLicenseThreatLevelByOwnerAndLicenseIdWithHierarchy() {
    tempEntity.newLicenseThreatGroup(applicationId, "My group 1", 0, "Apache-2.0");
    tempEntity.newLicenseThreatGroup(organization.getId(), "My group 2", 5, "GPL-2.0");
    tempEntity.newLicenseThreatGroup(organization.getParentOrganizationId(), "My group 3", 9, "GPL-3.0");

    LicenseDAO dao = new LicenseDAO();
    Collection<License> licenses = dao.getAll();

    for (License license : licenses) {
      Integer threat = dao.getLicenseThreatLevelByOwnerAndLicenseIdWithHierarchy(application, license.getId());
      if (threat != null) {
        assertThat(threat).isBetween(0, 10);
      }
    }

    assertThat(dao.getLicenseThreatLevelByOwnerAndLicenseIdWithHierarchy(application, "Apache-2.0")).isEqualTo(0);
    assertThat(dao.getLicenseThreatLevelByOwnerAndLicenseIdWithHierarchy(application, "GPL-2.0")).isEqualTo(5);
    assertThat(dao.getLicenseThreatLevelByOwnerAndLicenseIdWithHierarchy(application, "GPL-3.0")).isEqualTo(9);
  }
}
