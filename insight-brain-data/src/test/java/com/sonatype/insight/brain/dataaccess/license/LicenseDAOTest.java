/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.license;

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
}
