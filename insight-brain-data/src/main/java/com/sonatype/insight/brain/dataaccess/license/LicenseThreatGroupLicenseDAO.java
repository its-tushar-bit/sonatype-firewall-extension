/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.license;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupLicense;
import com.sonatype.insight.dataaccess.TransactionContext;

@Named
@Singleton
public class LicenseThreatGroupLicenseDAO
    extends AbstractOperationalSqlDAO<LicenseThreatGroupLicense>
{
  private final LicenseThreatGroupDAO licenseThreatGroupDAO;

  private final LicenseDAO licenseDAO;

  @Inject
  public LicenseThreatGroupLicenseDAO(
      final OperationalDataStore operationalDataStore,
      final LicenseDAO licenseDAO,
      final LicenseThreatGroupDAO licenseThreatGroupDAO)
  {
    super(operationalDataStore);
    this.licenseDAO = licenseDAO;
    this.licenseThreatGroupDAO = licenseThreatGroupDAO;
  }

  private LicenseThreatGroupLicense getByGroupIdAndLicenseId(TransactionContext tx,
                                                             String ownerId,
                                                             String licenseThreatGroupId)
  {
    String sQuery = "SELECT entity FROM LicenseThreatGroupLicense entity" + //
        " WHERE entity.licenseThreatGroupId=?1 AND entity.licenseId=?2";
    return get(tx, sQuery, ownerId, licenseThreatGroupId);
  }

  public List<LicenseThreatGroupLicense> getByOwnerId(String ownerId) {
    String sQuery = "SELECT entity FROM LicenseThreatGroupLicense entity" + //
        " WHERE entity.ownerId=?1" + //
        " ORDER BY entity.licenseId";
    return getList(sQuery, ownerId);
  }

  public List<LicenseThreatGroupLicense> getByLicenseThreatGroupId(TransactionContext tx, String licenseThreatGroupId) {
    String sQuery = "SELECT entity FROM LicenseThreatGroupLicense entity" + //
        " WHERE entity.licenseThreatGroupId=?1" + //
        " ORDER BY entity.licenseId";
    return getList(tx, sQuery, licenseThreatGroupId);
  }

  public List<LicenseThreatGroupLicense> getByLicenseThreatGroupId(String licenseThreatGroupId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByLicenseThreatGroupId(tx, licenseThreatGroupId);
    }
  }

  public List<LicenseThreatGroupLicense> getByLicenseThreatGroupIds(Set<String> licenseThreatGroupIds) {
    String sQuery = "SELECT entity FROM LicenseThreatGroupLicense entity" + //
        " WHERE entity.licenseThreatGroupId IN (?1)";
    return getList(sQuery, licenseThreatGroupIds);
  }

  @Override
  public void insert(TransactionContext tx, LicenseThreatGroupLicense entity) {
    licenseDAO.getByIdNotNull(entity.getLicenseId());

    LicenseThreatGroupLicense other = getByGroupIdAndLicenseId(tx, entity.getLicenseThreatGroupId(),
        entity.getLicenseId());
    if (other != null) {
      throw new InvalidLicenseThreatGroupLicenseException("The license is already in the license threat group");
    }
    super.insert(tx, entity);
  }

  public void setLicenses(String licenseThreatGroupId, Set<String> licenseIds) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();

      LicenseThreatGroup licenseThreatGroup = licenseThreatGroupDAO.getByIdNotNull(tx, licenseThreatGroupId);
      String ownerId = licenseThreatGroup.getOwnerId();

      List<LicenseThreatGroupLicense> oldLicenses = new ArrayList<>();
      oldLicenses.addAll(getByLicenseThreatGroupId(tx, licenseThreatGroupId));
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
        insert(tx, newLicense);
      }

      for (LicenseThreatGroupLicense oldLicense : oldLicenses) {
        delete(tx, oldLicense);
      }

      tx.commit();
    }
  }

  public List<LicenseThreatGroupLicense> getByOwnerIds(Collection<String> ownerIds) {
    String sQuery = "SELECT entity FROM LicenseThreatGroupLicense entity" + //
        " WHERE entity.ownerId IN (?1)";
    return getList(sQuery, ownerIds);
  }
}
