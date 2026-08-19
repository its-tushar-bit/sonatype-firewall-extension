/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.util.prefs.Preferences;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ProductLicenseDAO;
import com.sonatype.insight.brain.model.configuration.ProductLicense;
import com.sonatype.insight.brain.product.license.DatabasePreferences;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.license.model.CLMLicenseBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class ProductLicenseMigrator
{
  private static final Logger log = LoggerFactory.getLogger(ProductLicenseMigrator.class);

  static final String MIGRATION_ID = "product-license";

  private final MigrationTrackerDAO migrationTrackerDAO;

  private final ProductLicenseDAO productLicenseDAO;

  @Inject
  public ProductLicenseMigrator(
      MigrationTrackerDAO migrationTrackerDAO,
      ProductLicenseDAO productLicenseDAO)
  {
    this.migrationTrackerDAO = migrationTrackerDAO;
    this.productLicenseDAO = productLicenseDAO;
  }

  void migrate() {
    if (migrationTrackerDAO.isTrackerPresent(MIGRATION_ID)) {
      log.debug("Product license already migrated.");
      return;
    }
    String absolutePath = CLMLicenseBuilder.PREFERENCES_PATH;
    Preferences licenseNode = userRoot().node(absolutePath);
    String licenseKeyValue = licenseNode.get(DatabasePreferences.LICENSE_KEY, null);
    String licenseDetailsValue = licenseNode.get(DatabasePreferences.LICENSE_DETAILS_KEY, null);
    try (TransactionContext tx = productLicenseDAO.createTransactionContext()) {
      tx.begin();
      if (licenseKeyValue != null) {
        log.debug("Migrating product license to the database.");
        ProductLicense productLicense = new ProductLicense();
        productLicense.setLicenseKey(licenseKeyValue);
        productLicense.setLicenseDetails(licenseDetailsValue);
        productLicenseDAO.update(tx, productLicense);
      }
      else {
        log.debug("No product license to migrate to the database.");
      }
      migrationTrackerDAO.insertTracker(tx, MIGRATION_ID);
      tx.commit();
    }
  }

  // Visible for testing
  Preferences userRoot() {
    return Preferences.userRoot();
  }
}
