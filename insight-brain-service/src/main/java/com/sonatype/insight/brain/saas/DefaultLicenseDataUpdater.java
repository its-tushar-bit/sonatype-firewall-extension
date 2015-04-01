/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.saas;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.license.LicenseCategoryDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseDataUpdater;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseLicenseInternalDAO;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.brain.model.license.LicenseCategory;
import com.sonatype.insight.brain.model.license.MultiLicense;
import com.sonatype.insight.brain.model.license.MultiLicenseLicenseInternal;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class DefaultLicenseDataUpdater
    extends LicenseDataUpdater
{
  public static final String HDS_LICENSE_PATH = "rest/license";

  private static final Logger log = LoggerFactory.getLogger(DefaultLicenseDataUpdater.class);

  private final SaasClient client;

  @Inject
  public DefaultLicenseDataUpdater(SaasClient client) {
    this.client = client;
  }

  @Override
  public void doUpdate() {
    long start = System.currentTimeMillis();
    log.info("Updating license data...");
    try {
      LicenseData licenseData = client.get(LicenseData.class, HDS_LICENSE_PATH, null /* params */);

      LicenseCategoryDAO licenseCategoryDAO = new LicenseCategoryDAO();
      LicenseDAO licenseDAO = new LicenseDAO();
      MultiLicenseDAO multiLicenseDAO = new MultiLicenseDAO();
      MultiLicenseLicenseInternalDAO multiLicenseLicenseInternalDAO = new MultiLicenseLicenseInternalDAO();
      try (TransactionContext tx = licenseCategoryDAO.createTransactionContext()) {
        tx.begin();
        for (LicenseCategory licenseCategory : licenseData.categories) {
          if (licenseCategoryDAO.getById(tx, licenseCategory.getId()) == null) {
            licenseCategoryDAO.insert(tx, licenseCategory);
          }
          else {
            licenseCategoryDAO.update(tx, licenseCategory);
          }
        }
        for (License license : licenseData.licenses) {
          if (licenseDAO.getById(tx, license.getId()) == null) {
            licenseDAO.insert(tx, license);
          }
          else {
            licenseDAO.update(tx, license);
          }
        }
        for (MultiLicense multiLicense : licenseData.multiLicenses) {
          if (multiLicenseDAO.getById(tx, multiLicense.getId()) == null) {
            multiLicenseDAO.insert(tx, multiLicense);
            for (String licenseId : licenseData.multiLicenseMappings.get(multiLicense.getId())) {
              MultiLicenseLicenseInternal multiLicenseLicense = new MultiLicenseLicenseInternal();
              multiLicenseLicense.setMultiLicenseId(multiLicense.getId());
              multiLicenseLicense.setLicenseId(licenseId);
              multiLicenseLicenseInternalDAO.insert(tx, multiLicenseLicense);
            }
          }
          else {
            multiLicenseDAO.update(tx, multiLicense);
            // Do not update the multi-license to license associations as those should never change.
          }
        }
        tx.commit();
      }
    }
    catch (Exception e) {
      throw new RuntimeException("Could not retrieve license data from Sonatype HDS: " + e.getMessage(), e);
    }
    log.debug("Updated license data in {} ms.", System.currentTimeMillis() - start);
  }

  // TODO Move it to com.sonatype.clm.dto.model?
  public static class LicenseData
  {
    public Collection<LicenseCategory> categories;

    public Collection<License> licenses;

    public Collection<MultiLicense> multiLicenses;

    public Map<String, Set<String>> multiLicenseMappings;
  }
}
