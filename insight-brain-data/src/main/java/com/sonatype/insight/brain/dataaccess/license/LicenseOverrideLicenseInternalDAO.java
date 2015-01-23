/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.license;

import java.util.List;

import javax.persistence.EntityManager;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.license.LicenseOverrideLicenseInternal;

/**
 * @since 1.13
 */
public class LicenseOverrideLicenseInternalDAO
    extends AbstractOperationalSqlDAO<LicenseOverrideLicenseInternal>
{
  public List<LicenseOverrideLicenseInternal> getByLicenseOverrideId(String licenseOverrideId) {
    EntityManager em = createEntityManager();
    try {
      return getByLicenseOverrideId(em, licenseOverrideId);
    }
    finally {
      close(em);
    }
  }

  public List<LicenseOverrideLicenseInternal> getByLicenseOverrideId(EntityManager em, String licenseOverrideId) {
    String sQuery = "SELECT entity FROM LicenseOverrideLicenseInternal entity" + //
        " WHERE entity.licenseOverrideId=?1";

    return getList(em, sQuery, licenseOverrideId);
  }
}
