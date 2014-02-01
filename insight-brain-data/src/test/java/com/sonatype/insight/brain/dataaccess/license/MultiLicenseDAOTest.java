/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.license;

import java.util.Collection;

import javax.persistence.EntityManager;

import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.brain.model.license.MultiLicense;
import com.sonatype.insight.brain.model.license.MultiLicenseLicenseInternal;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class MultiLicenseDAOTest
    extends AbstractLicenseDAOTest
{
  private static String MOCK_REMOTE_LICENSE_ID = "test";

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
  public void testGetLicenseThreatLevelByApplicationAndMultiLicenseId() {
    MultiLicenseDAO dao = new MultiLicenseDAO();
    Collection<MultiLicense> multiLicenses = dao.getAll();

    for (MultiLicense multiLicense : multiLicenses) {
      Integer threat = dao.getLicenseThreatLevelByApplicationAndMultiLicenseId(application, multiLicense.getId());
      Assert.assertTrue("Multilicense Threat Level between null and 10", threat == null
          || (threat >= 0 && threat <= 10));
    }

    assertEquals(Integer.valueOf(0), dao.getLicenseThreatLevelByApplicationAndMultiLicenseId(application, "Apache-2.0"));
    assertEquals(Integer.valueOf(9), dao.getLicenseThreatLevelByApplicationAndMultiLicenseId(application, "GPL-2.0"));
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
      assertThat(e.getMessage(), is("A multi-license with id '" + MOCK_REMOTE_LICENSE_ID
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

  /**
   * Inserts License/Multilicense records locally to mock out updates from SaaS
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
      EntityManager em = multiLicenseDAO.createEntityManager();
      try {
        em.getTransaction().begin();

        license = new License();

        license.setId(MOCK_REMOTE_LICENSE_ID);
        license.setShortDisplayName(MOCK_REMOTE_LICENSE_ID);
        licenseDAO.insert(em, license);

        multiLicense = new MultiLicense();
        multiLicense.setId(MOCK_REMOTE_LICENSE_ID);
        multiLicense.setShortDisplayName(MOCK_REMOTE_LICENSE_ID);
        multiLicenseDAO.insert(em, multiLicense);

        multiLicenseLicense = new MultiLicenseLicenseInternal();
        multiLicenseLicense.setMultiLicenseId(multiLicense.getId());
        multiLicenseLicense.setLicenseId(MOCK_REMOTE_LICENSE_ID);
        multiLicenseLicense.setMultiLicenseId(MOCK_REMOTE_LICENSE_ID);
        multiLicenseLicenseInternalDAO.insert(em, multiLicenseLicense);

        em.getTransaction().commit();
      }
      catch (Exception e) {
        throw new RuntimeException("Could not simulate retrieval of license data from SaaS: " + e.getMessage(), e);
      }
      finally {
        LicenseCategoryDAO.close(em);
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
