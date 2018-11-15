/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.license;

import java.util.Collection;

import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.brain.model.license.MultiLicense;
import com.sonatype.insight.brain.model.license.MultiLicenseLicenseInternal;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MultiLicenseDAOTest
    extends AbstractLicenseDAOTest
{
  private static String MOCK_REMOTE_LICENSE_ID = "test";

  @Test
  public void testCRUD() throws Exception {
    MultiLicenseDAO dao = new MultiLicenseDAO();

    String shortName = "SDN";
    assertNull(dao.getByName(shortName));
    MultiLicense multiLicense = new MultiLicense();
    multiLicense.setShortDisplayName(shortName);
    multiLicense.setLongDisplayName("Long Display Name");
    dao.insert(multiLicense);
    assertNotNull(multiLicense.getId());
    dao.load();

    multiLicense = dao.getById(multiLicense.getId());
    assertNotNull(multiLicense);
    assertEquals("SDN", multiLicense.getShortDisplayName());
    assertEquals("Long Display Name", multiLicense.getLongDisplayName());

    multiLicense.setLongDisplayName("New Long Display Name");
    dao.update(multiLicense);
    dao.load();

    dao.getById(multiLicense.getId());
    assertNotNull(multiLicense);
    assertEquals("New Long Display Name", multiLicense.getLongDisplayName());

    dao.delete(multiLicense);
    dao.load();

    multiLicense = dao.getById(multiLicense.getId());
    assertNull(multiLicense);
  }

  @Test
  public void testGetAll() {
    MultiLicenseDAO dao = new MultiLicenseDAO();
    Collection<MultiLicense> multiLicenses = dao.getAll();

    assertNotNull(multiLicenses);
    assertFalse(multiLicenses.isEmpty());
  }

  @Test
  public void testGetLicenseThreatLevelByApplicationAndMultiLicenseId() {
    tempEntity.newLicenseThreatGroup(applicationId, "My group 1", 0, "Apache-2.0");
    tempEntity.newLicenseThreatGroup(organization.getId(), "My group 2", 5, "GPL-2.0");
    tempEntity.newLicenseThreatGroup(organization.getParentOrganizationId(), "My group 3", 9, "GPL-3.0");

    MultiLicenseDAO dao = new MultiLicenseDAO();
    Collection<MultiLicense> multiLicenses = dao.getAll();

    for (MultiLicense multiLicense : multiLicenses) {
      Integer threat = dao.getLicenseThreatLevelByApplicationAndMultiLicenseId(application, multiLicense.getId());
      assertTrue("Multilicense Threat Level between null and 10", threat == null || (threat >= 0 && threat <= 10));
    }

    assertEquals(Integer.valueOf(0), dao.getLicenseThreatLevelByApplicationAndMultiLicenseId(application, "Apache-2.0"));
    assertEquals(Integer.valueOf(5), dao.getLicenseThreatLevelByApplicationAndMultiLicenseId(application, "GPL-2.0"));
    assertEquals(Integer.valueOf(9), dao.getLicenseThreatLevelByApplicationAndMultiLicenseId(application, "GPL-3.0"));
  }

  @Test(expected = NotFoundException.class)
  public void testGetLicensesByMultiLicenseIdNotFound() {
    MultiLicenseDAO dao = new MultiLicenseDAO();
    dao.getLicensesByMultiLicenseIdNotNull("Not-To-Be-Found");
  }

  @Test
  public void testGetLicensesByMultiLicenseIdRefreshedRemotely() {
    MultiLicenseDAO dao = new MultiLicenseDAO();

    try {
      dao.getLicensesByMultiLicenseIdNotNull(MOCK_REMOTE_LICENSE_ID);
      fail("Expected a NotFoundException to be thrown");
    }
    catch (NotFoundException e) {
      assertThat(e.getMessage(), is("A multi-license with ID '" + MOCK_REMOTE_LICENSE_ID
          + "' does not exist locally or remotely."));
    }

    MockLicenseDataUpdater updater = new MockLicenseDataUpdater();
    LicenseDataUpdater.setUpdater(updater);

    assertThat(dao.getLicensesByMultiLicenseIdNotNull(MOCK_REMOTE_LICENSE_ID), notNullValue());
    updater.cleanup();
  }

  @Test
  public void testLicenseDataRefresh() {
    String newId = "new multi license id";
    MultiLicenseDAO dao = new MultiLicenseDAO();
    assertNull(dao.getById(newId));
    int count = dao.getAll().size();

    MultiLicense newMultiLicense = new MultiLicense();
    newMultiLicense.setId(newId);
    newMultiLicense.setShortDisplayName("New short name");
    newMultiLicense.setLongDisplayName("New long name");
    dao.insert(newMultiLicense);
    MultiLicenseLicenseInternal multiLicenseLicense = new MultiLicenseLicenseInternal();
    multiLicenseLicense.setMultiLicenseId(newMultiLicense.getId());
    multiLicenseLicense.setLicenseId("GPL-2.0");
    MultiLicenseLicenseInternalDAO multiLicenseLicenseDAO = new MultiLicenseLicenseInternalDAO();
    multiLicenseLicenseDAO.insert(multiLicenseLicense);
    assertNull(dao.getById(newId));

    LicenseDataUpdater.setUpdater(new DummyLicenseDataUpdater());

    assertNotNull(dao.getById(newId));
    assertEquals(count + 1, dao.getAll().size());

    multiLicenseLicenseDAO.delete(multiLicenseLicense);
    dao.delete(newMultiLicense);
    dao.load();
  }

  /**
   * Inserts License/Multilicense records locally to mock out updates from HDS
   */
  private class MockLicenseDataUpdater
      extends LicenseDataUpdater
  {
    LicenseDAO licenseDAO = new LicenseDAO();

    MultiLicenseDAO multiLicenseDAO = new MultiLicenseDAO();

    MultiLicenseLicenseInternalDAO multiLicenseLicenseInternalDAO = new MultiLicenseLicenseInternalDAO();

    License license;

    MultiLicense multiLicense;

    MultiLicenseLicenseInternal multiLicenseLicense;

    @Override
    public void doUpdate() {
      try (TransactionContext tx = multiLicenseDAO.createTransactionContext()) {
        tx.begin();

        license = new License();

        license.setId(MOCK_REMOTE_LICENSE_ID);
        license.setShortDisplayName(MOCK_REMOTE_LICENSE_ID);
        licenseDAO.insert(tx, license);

        multiLicense = new MultiLicense();
        multiLicense.setId(MOCK_REMOTE_LICENSE_ID);
        multiLicense.setShortDisplayName(MOCK_REMOTE_LICENSE_ID);
        multiLicenseDAO.insert(tx, multiLicense);

        multiLicenseLicense = new MultiLicenseLicenseInternal();
        multiLicenseLicense.setMultiLicenseId(multiLicense.getId());
        multiLicenseLicense.setLicenseId(MOCK_REMOTE_LICENSE_ID);
        multiLicenseLicense.setMultiLicenseId(MOCK_REMOTE_LICENSE_ID);
        multiLicenseLicenseInternalDAO.insert(tx, multiLicenseLicense);

        tx.commit();
      }
      catch (Exception e) {
        throw new RuntimeException("Could not simulate retrieval of license data from Sonatype HDS: " + e.getMessage(),
            e);
      }
    }

    /**
     * Remove all data introduced during doUpdate method
     */
    public void cleanup() {
      multiLicenseLicenseInternalDAO.delete(multiLicenseLicense);
      licenseDAO.delete(license);
      multiLicenseDAO.delete(multiLicense);
      multiLicenseDAO.load();
    }
  }
}
