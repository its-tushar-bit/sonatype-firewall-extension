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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class MultiLicenseDAOTest
    extends AbstractLicenseDAOTest
{
  private static String MOCK_REMOTE_LICENSE_ID = "test";

  @Test
  public void testCRUD() throws Exception {
    MultiLicenseDAO dao = new MultiLicenseDAO();

    String shortName = "SDN";
    assertThat(dao.getByName(shortName)).isNull();
    MultiLicense multiLicense = new MultiLicense();
    multiLicense.setShortDisplayName(shortName);
    multiLicense.setLongDisplayName("Long Display Name");
    dao.insert(multiLicense);
    assertThat(multiLicense.getId()).isNotNull();
    dao.load();

    multiLicense = dao.getById(multiLicense.getId());
    assertThat(multiLicense).isNotNull();
    assertThat(multiLicense.getShortDisplayName()).isEqualTo("SDN");
    assertThat(multiLicense.getLongDisplayName()).isEqualTo("Long Display Name");

    multiLicense.setLongDisplayName("New Long Display Name");
    dao.update(multiLicense);
    dao.load();

    dao.getById(multiLicense.getId());
    assertThat(multiLicense).isNotNull();
    assertThat(multiLicense.getLongDisplayName()).isEqualTo("New Long Display Name");

    dao.delete(multiLicense);
    dao.load();

    multiLicense = dao.getById(multiLicense.getId());
    assertThat(multiLicense).isNull();
  }

  @Test
  public void testGetAll() {
    MultiLicenseDAO dao = new MultiLicenseDAO();
    Collection<MultiLicense> multiLicenses = dao.getAll();

    assertThat(multiLicenses).isNotEmpty();
  }

  @Test(expected = NotFoundException.class)
  public void testGetLicensesByMultiLicenseIdNotFound() {
    MultiLicenseDAO dao = new MultiLicenseDAO();
    dao.getLicensesByMultiLicenseIdNotNull("Not-To-Be-Found");
  }

  @Test
  public void testGetLicensesByMultiLicenseIdRefreshedRemotely() {
    MultiLicenseDAO dao = new MultiLicenseDAO();

    assertThatThrownBy(() -> {
      dao.getLicensesByMultiLicenseIdNotNull(MOCK_REMOTE_LICENSE_ID);
    }).isInstanceOf(NotFoundException.class)
        .hasMessage("A multi-license with ID '" + MOCK_REMOTE_LICENSE_ID + "' does not exist locally or remotely.");

    MockLicenseDataUpdater updater = new MockLicenseDataUpdater();
    LicenseDataUpdater.setUpdater(updater);

    assertThat(dao.getLicensesByMultiLicenseIdNotNull(MOCK_REMOTE_LICENSE_ID)).isNotNull();
    updater.cleanup();
  }

  @Test
  public void testLicenseDataRefresh() {
    String newId = "new multi license id";
    MultiLicenseDAO dao = new MultiLicenseDAO();
    assertThat(dao.getById(newId)).isNull();
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
    assertThat(dao.getById(newId)).isNull();

    LicenseDataUpdater.setUpdater(new DummyLicenseDataUpdater());

    assertThat(dao.getById(newId)).isNotNull();
    assertThat(dao.getAll()).hasSize(count + 1);

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
