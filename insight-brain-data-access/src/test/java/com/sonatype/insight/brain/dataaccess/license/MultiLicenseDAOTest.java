/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.license;

import java.util.Collection;

import com.sonatype.insight.brain.model.license.MultiLicense;
import com.sonatype.insight.brain.model.license.MultiLicenseLicenseInternal;

import org.junit.Assert;
import org.junit.Test;

public class MultiLicenseDAOTest
    extends AbstractLicenseDAOTest
{
  @Test
  public void testCRUD() throws Exception {
    MultiLicenseDAO dao = new MultiLicenseDAO();

    String shortName = "SDN";
    Assert.assertNull(dao.getByName(shortName));
    MultiLicense multiLicense = new MultiLicense();
    multiLicense.setDescription("Description");
    multiLicense.setLicenseUrl("License Url");
    multiLicense.setShortDisplayName(shortName);
    multiLicense.setLongDisplayName("Long Display Name");
    dao.insert(multiLicense);
    Assert.assertNotNull(multiLicense.getId());
    dao.load();

    multiLicense = dao.getById(multiLicense.getId());
    Assert.assertNotNull(multiLicense);
    Assert.assertEquals("Description", multiLicense.getDescription());
    Assert.assertEquals("License Url", multiLicense.getLicenseUrl());
    Assert.assertEquals("SDN", multiLicense.getShortDisplayName());
    Assert.assertEquals("Long Display Name", multiLicense.getLongDisplayName());

    multiLicense.setLongDisplayName("New Long Display Name");
    dao.update(multiLicense);
    dao.load();

    dao.getById(multiLicense.getId());
    Assert.assertNotNull(multiLicense);
    Assert.assertEquals("New Long Display Name", multiLicense.getLongDisplayName());

    dao.delete(multiLicense);
    dao.load();

    multiLicense = dao.getById(multiLicense.getId());
    Assert.assertNull(multiLicense);
  }

  @Test
  public void testGetAll() {
    MultiLicenseDAO dao = new MultiLicenseDAO();
    Collection<MultiLicense> multiLicenses = dao.getAll();

    Assert.assertNotNull(multiLicenses);
    Assert.assertFalse(multiLicenses.isEmpty());
  }

  @Test
  public void testGetLicenseThreatLevelByApplicationIdAndMultiLicenseId() {
    createDefaultApplication();

    MultiLicenseDAO dao = new MultiLicenseDAO();
    Collection<MultiLicense> multiLicenses = dao.getAll();

    for (MultiLicense multiLicense : multiLicenses) {
      Integer threat = dao.getLicenseThreatLevelByApplicationIdAndMultiLicenseId(applicationId, multiLicense.getId());
      Assert.assertTrue("Multilicense Threat Level between null and 10", threat == null
          || (threat >= 0 && threat <= 10));
    }
  }

  @Test
  public void testLicenseDataRefresh() {
    String newId = "new multi license id";
    MultiLicenseDAO dao = new MultiLicenseDAO();
    Assert.assertNull(dao.getById(newId));
    int count = dao.getAll().size();

    MultiLicense newMultiLicense = new MultiLicense();
    newMultiLicense.setId(newId);
    newMultiLicense.setShortDisplayName("New short name");
    newMultiLicense.setLongDisplayName("New long name");
    newMultiLicense.setDescription("New description");
    dao.insert(newMultiLicense);
    MultiLicenseLicenseInternal multiLicenseLicense = new MultiLicenseLicenseInternal();
    multiLicenseLicense.setMultiLicenseId(newMultiLicense.getId());
    multiLicenseLicense.setLicenseId("GPL-2.0");
    MultiLicenseLicenseInternalDAO multiLicenseLicenseDAO = new MultiLicenseLicenseInternalDAO();
    multiLicenseLicenseDAO.insert(multiLicenseLicense);
    Assert.assertNull(dao.getById(newId));

    LicenseDataUpdater.setUpdater(new DummyLicenseDataUpdater());

    Assert.assertNotNull(dao.getById(newId));
    Assert.assertEquals(count + 1, dao.getAll().size());

    multiLicenseLicenseDAO.delete(multiLicenseLicense);
    dao.delete(newMultiLicense);
    dao.load();
  }
}
