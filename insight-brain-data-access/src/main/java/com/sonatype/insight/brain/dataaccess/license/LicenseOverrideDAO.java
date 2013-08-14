/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.license;

import java.util.List;

import javax.persistence.EntityManager;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.error.exception.NotFoundException;

/**
 * @since 1.6
 */
public class LicenseOverrideDAO
    extends AbstractOperationalSqlDAO<LicenseOverride>
{
  @Override
  protected LicenseOverride getById(EntityManager em, String id) {
    String sQuery = "SELECT entity FROM LicenseOverride entity" + //
        " WHERE entity.id=?1";
    return get(em, sQuery, id);
  }

  public LicenseOverride getByOwnerIdAndGAV(String ownerId, String groupId, String artifactId, String version) {
    String sQuery = "SELECT entity FROM LicenseOverride entity" + //
        " WHERE entity.ownerId=?1 AND entity.groupId=?2 AND entity.artifactId=?3 AND entity.version=?4";
    return get(sQuery, ownerId, groupId, artifactId, version);
  }

  public List<LicenseOverride> getByOwnerId(EntityManager em, String ownerId) {
    String sQuery = "SELECT entity FROM LicenseOverride entity" + //
        " WHERE entity.ownerId=?1" + //
        " ORDER BY entity.groupId, entity.artifactId, entity.version";
    return getList(em, sQuery, ownerId);
  }

  public List<LicenseOverride> getByOwnerId(String ownerId) {
    EntityManager em = createEntityManager();
    try {
      return getByOwnerId(em, ownerId);
    }
    finally {
      close(em);
    }
  }

  private LicenseOverride getByIdNotNull(EntityManager em, String id) {
    LicenseOverride licenseOverride = getById(em, id);
    if (licenseOverride == null) {
      throw new NotFoundException("Cannot find a license override with id " + id);
    }
    return licenseOverride;
  }

  public LicenseOverride getByIdNotNull(String id) {
    EntityManager em = createEntityManager();
    try {
      return getByIdNotNull(em, id);
    }
    finally {
      close(em);
    }
  }
}
