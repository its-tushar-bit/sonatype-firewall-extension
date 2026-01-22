/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.license;

import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.license.LicenseOverrideLicenseInternal;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * @since 1.13
 */
@Named
@Singleton
public class LicenseOverrideLicenseInternalDAO
    extends AbstractOperationalSqlDAO<LicenseOverrideLicenseInternal>
{
  @Inject
  public LicenseOverrideLicenseInternalDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public List<LicenseOverrideLicenseInternal> getByLicenseOverrideId(String licenseOverrideId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByLicenseOverrideId(tx, licenseOverrideId);
    }
  }

  public List<LicenseOverrideLicenseInternal> getByLicenseOverrideId(TransactionContext tx, String licenseOverrideId) {
    String sQuery = "SELECT entity FROM LicenseOverrideLicenseInternal entity" + //
        " WHERE entity.licenseOverrideId=?1";

    return getList(tx, sQuery, licenseOverrideId);
  }

  List<LicenseOverrideLicenseInternal> getByOwnerId(TransactionContext tx, String ownerId) {
    String sQuery = "SELECT licenseOverrideLicense" + //
        " FROM LicenseOverrideLicenseInternal licenseOverrideLicense, LicenseOverrideInternal licenseOverride" + //
        " WHERE licenseOverrideLicense.licenseOverrideId = licenseOverride.id AND licenseOverride.ownerId = ?1";

    return getList(tx, sQuery, ownerId);
  }
}
