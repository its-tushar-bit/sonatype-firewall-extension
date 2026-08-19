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

import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.LicenseThreatGroupLicense.LICENSE_THREAT_GROUP_LICENSE;

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

  private LicenseThreatGroupLicense getByGroupIdAndLicenseId(
      TransactionContext tx,
      String licenseThreatGroupId,
      String licenseId)
  {
    return toEntity(tx.dsl()
        .selectFrom(LICENSE_THREAT_GROUP_LICENSE)
        .where(LICENSE_THREAT_GROUP_LICENSE.LICENSE_THREAT_GROUP_ID.eq(licenseThreatGroupId))
        .and(LICENSE_THREAT_GROUP_LICENSE.LICENSE_ID.eq(licenseId))
        .fetchOne());
  }

  public List<LicenseThreatGroupLicense> getByOwnerId(String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(LICENSE_THREAT_GROUP_LICENSE)
          .where(LICENSE_THREAT_GROUP_LICENSE.OWNER_ID.eq(ownerId))
          .orderBy(LICENSE_THREAT_GROUP_LICENSE.LICENSE_ID)
          .fetch(this::toEntity);
    }
  }

  public List<LicenseThreatGroupLicense> getByLicenseThreatGroupId(TransactionContext tx, String licenseThreatGroupId) {
    return tx.dsl()
        .selectFrom(LICENSE_THREAT_GROUP_LICENSE)
        .where(LICENSE_THREAT_GROUP_LICENSE.LICENSE_THREAT_GROUP_ID.eq(licenseThreatGroupId))
        .orderBy(LICENSE_THREAT_GROUP_LICENSE.LICENSE_ID)
        .fetch(this::toEntity);
  }

  public List<LicenseThreatGroupLicense> getByLicenseThreatGroupId(String licenseThreatGroupId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByLicenseThreatGroupId(tx, licenseThreatGroupId);
    }
  }

  public List<LicenseThreatGroupLicense> getByLicenseThreatGroupIds(Set<String> licenseThreatGroupIds) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(LICENSE_THREAT_GROUP_LICENSE)
          .where(LICENSE_THREAT_GROUP_LICENSE.LICENSE_THREAT_GROUP_ID.in(licenseThreatGroupIds))
          .fetch(this::toEntity);
    }
  }

  @Override
  public int insert(TransactionContext tx, LicenseThreatGroupLicense entity) {
    licenseDAO.getByIdNotNull(entity.getLicenseId());

    LicenseThreatGroupLicense other = getByGroupIdAndLicenseId(tx, entity.getLicenseThreatGroupId(),
        entity.getLicenseId());
    if (other != null) {
      throw new InvalidLicenseThreatGroupLicenseException("The license is already in the license threat group");
    }
    return super.insert(tx, entity);
  }

  @Override
  public void delete(TransactionContext tx, LicenseThreatGroupLicense entity) {
    tx.dsl()
        .deleteFrom(LICENSE_THREAT_GROUP_LICENSE)
        .where(LICENSE_THREAT_GROUP_LICENSE.LICENSE_THREAT_GROUP_LICENSE_ID.eq(entity.getId()))
        .execute();
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
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(LICENSE_THREAT_GROUP_LICENSE)
          .where(LICENSE_THREAT_GROUP_LICENSE.OWNER_ID.in(ownerIds))
          .fetch(this::toEntity);
    }
  }

  @Override
  public Table<?> getJooqTable() {
    return LICENSE_THREAT_GROUP_LICENSE;
  }

  @Override
  public Class<LicenseThreatGroupLicense> getEntityClass() {
    return LicenseThreatGroupLicense.class;
  }
}
