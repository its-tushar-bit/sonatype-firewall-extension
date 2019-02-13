/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseDataUpdater;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseLicenseInternalDAO;
import com.sonatype.insight.brain.model.license.License;
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

  private final HdsClient client;

  @Inject
  public DefaultLicenseDataUpdater(HdsClient client) {
    this.client = client;
  }

  @Override
  public void doUpdate() {
    long start = System.currentTimeMillis();
    log.info("Updating license data...");
    try {
      LicenseData licenseData = client.get(LicenseData.class, HDS_LICENSE_PATH, null /* params */);

      LicenseDAO licenseDAO = new LicenseDAO();
      MultiLicenseDAO multiLicenseDAO = new MultiLicenseDAO();
      MultiLicenseLicenseInternalDAO multiLicenseLicenseInternalDAO = new MultiLicenseLicenseInternalDAO();
      try (TransactionContext tx = licenseDAO.createTransactionContext()) {
        tx.begin();
        for (License license : licenseData.licenses) {
          if (licenseDAO.getById(tx, license.getId()) == null) {
            licenseDAO.insert(tx, license);
          }
          else {
            licenseDAO.update(tx, license);
          }
        }
        for (MultiLicense multiLicense : licenseData.multiLicenses) {
          Set<String> storedMappedLicenseIds = new HashSet<>();
          Set<String> mappedLicenseIds = licenseData.multiLicenseMappings.get(multiLicense.getId());
          if (mappedLicenseIds == null) {
            mappedLicenseIds = new HashSet<>();
          }
          if (multiLicenseDAO.getById(tx, multiLicense.getId()) == null) {
            if (!mappedLicenseIds.isEmpty()) {
              multiLicenseDAO.insert(tx, multiLicense);
            }
          }
          else {
            multiLicenseDAO.update(tx, multiLicense);
            for (MultiLicenseLicenseInternal multiLicenseLicense : multiLicenseLicenseInternalDAO
                .getByMultiLicenseId(tx, multiLicense.getId())) {
              storedMappedLicenseIds.add(multiLicenseLicense.getLicenseId());
            }
          }
          for (String licenseId : getDifference(mappedLicenseIds, storedMappedLicenseIds)) {
            multiLicenseLicenseInternalDAO.insert(tx, new MultiLicenseLicenseInternal(multiLicense.getId(), licenseId));
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

  private Set<String> getDifference(final Set<String> setOne, final Set<String> setTwo) {
    final Set<String> difference = new HashSet<>(setOne);
    difference.removeAll(setTwo);
    return difference;
  }

  // TODO Move it to com.sonatype.clm.dto.model?
  public static class LicenseData
  {
    public Collection<License> licenses;

    public Collection<MultiLicense> multiLicenses;

    public Map<String, Set<String>> multiLicenseMappings;
  }
}
