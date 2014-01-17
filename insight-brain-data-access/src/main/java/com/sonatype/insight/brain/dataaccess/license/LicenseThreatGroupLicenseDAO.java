/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.license;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupLicense;

public class LicenseThreatGroupLicenseDAO
    extends AbstractOperationalSqlDAO<LicenseThreatGroupLicense>
{
  @Override
  protected LicenseThreatGroupLicense getById(EntityManager em, String id) {
    String sQuery = "SELECT entity FROM LicenseThreatGroupLicense entity" + //
        " WHERE entity.id=?1";
    return get(em, sQuery, id);
  }

  private LicenseThreatGroupLicense getByGroupIdAndLicenseId(EntityManager em, String ownerId,
      String licenseThreatGroupId)
  {
    String sQuery = "SELECT entity FROM LicenseThreatGroupLicense entity" + //
        " WHERE entity.licenseThreatGroupId=?1 AND entity.licenseId=?2";
    return get(em, sQuery, ownerId, licenseThreatGroupId);
  }

  public List<LicenseThreatGroupLicense> getByOwnerId(String ownerId) {
    String sQuery = "SELECT entity FROM LicenseThreatGroupLicense entity" + //
        " WHERE entity.ownerId=?1" + //
        " ORDER BY entity.licenseId";
    return getList(sQuery, ownerId);
  }

  List<LicenseThreatGroupLicense> getByLicenseThreatGroupId(EntityManager em, String licenseThreatGroupId) {
    String sQuery = "SELECT entity FROM LicenseThreatGroupLicense entity" + //
        " WHERE entity.licenseThreatGroupId=?1" + //
        " ORDER BY entity.licenseId";
    return getList(em, sQuery, licenseThreatGroupId);
  }

  public List<LicenseThreatGroupLicense> getByLicenseThreatGroupId(String licenseThreatGroupId) {
    EntityManager em = createEntityManager();
    try {
      return getByLicenseThreatGroupId(em, licenseThreatGroupId);
    }
    finally {
      close(em);
    }
  }

  @Override
  public void insert(EntityManager em, LicenseThreatGroupLicense entity) {
    new LicenseDAO().getByIdNotNull(entity.getLicenseId());

    LicenseThreatGroupLicense other = getByGroupIdAndLicenseId(em, entity.getLicenseThreatGroupId(),
        entity.getLicenseId());
    if (other != null) {
      throw new InvalidLicenseThreatGroupLicenseException("The license is already in the license threat group");
    }
    super.insert(em, entity);
  }

  public void setLicenses(String licenseThreatGroupId, Set<String> licenseIds) {
    EntityManager em = createEntityManager();
    try {
      em.getTransaction().begin();

      LicenseThreatGroup licenseThreatGroup = new LicenseThreatGroupDAO().getByIdNotNull(em, licenseThreatGroupId);
      String ownerId = licenseThreatGroup.getOwnerId();

      LicenseDAO licenseDAO = new LicenseDAO();

      List<LicenseThreatGroupLicense> oldLicenses = new ArrayList<LicenseThreatGroupLicense>();
      oldLicenses.addAll(getByLicenseThreatGroupId(em, licenseThreatGroupId));
      for (String licenseId : licenseIds) {
        licenseDAO.getByIdNotNull(licenseId);

        boolean alreadyInGroup = false;
        for (LicenseThreatGroupLicense oldLicense : oldLicenses) {
          if (licenseId.equals(oldLicense.getLicenseId())) {
            alreadyInGroup = true;
            oldLicenses.remove(oldLicense);
            break;
          }
        }
        if (alreadyInGroup) {
          continue;
        }

        LicenseThreatGroupLicense newLicense = new LicenseThreatGroupLicense();
        newLicense.setOwnerId(ownerId);
        newLicense.setLicenseThreatGroupId(licenseThreatGroupId);
        newLicense.setLicenseId(licenseId);
        insert(em, newLicense);
      }

      for (LicenseThreatGroupLicense oldLicense : oldLicenses) {
        delete(em, oldLicense);
      }

      em.getTransaction().commit();
    }
    finally {
      close(em);
    }
  }
}
