/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.license;

import java.util.Collection;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.license.LicenseOverrideLicenseInternal;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.commons.collections4.CollectionUtils;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.LicenseOverride.LICENSE_OVERRIDE;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.LicenseOverrideLicense.LICENSE_OVERRIDE_LICENSE;

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

  public List<LicenseOverrideLicenseInternal> getByLicenseOverrideId(TransactionContext tx, String licenseOverrideId) {
    return tx.dsl()
        .selectFrom(LICENSE_OVERRIDE_LICENSE)
        .where(LICENSE_OVERRIDE_LICENSE.LICENSE_OVERRIDE_ID.eq(licenseOverrideId))
        .fetch(this::toEntity);
  }

  public List<LicenseOverrideLicenseInternal> getByLicenseOverrideIds(
      TransactionContext tx,
      Collection<String> licenseOverrideIds)
  {
    return getListWithSqlInClause(licenseOverrideIds,
        chunk -> tx.dsl()
            .selectFrom(LICENSE_OVERRIDE_LICENSE)
            .where(LICENSE_OVERRIDE_LICENSE.LICENSE_OVERRIDE_ID.in(chunk))
            .fetch(this::toEntity));
  }

  public void deleteByLicenseOverrideIds(TransactionContext tx, Collection<String> licenseOverrideIds) {
    if (CollectionUtils.isEmpty(licenseOverrideIds)) {
      return;
    }
    getListWithSqlInClause(licenseOverrideIds, idChunk -> List.of(tx.dsl()
        .deleteFrom(LICENSE_OVERRIDE_LICENSE)
        .where(LICENSE_OVERRIDE_LICENSE.LICENSE_OVERRIDE_ID.in(idChunk))
        .execute()), getDataStore());
  }

  List<LicenseOverrideLicenseInternal> getByOwnerId(TransactionContext tx, String ownerId) {
    return tx.dsl()
        .select(LICENSE_OVERRIDE_LICENSE.fields())
        .from(LICENSE_OVERRIDE_LICENSE)
        .join(LICENSE_OVERRIDE)
        .on(LICENSE_OVERRIDE_LICENSE.LICENSE_OVERRIDE_ID.eq(LICENSE_OVERRIDE.LICENSE_OVERRIDE_ID))
        .where(LICENSE_OVERRIDE.OWNER_ID.eq(ownerId))
        .fetch(r -> toEntity(r.into(LICENSE_OVERRIDE_LICENSE)));
  }

  @Override
  public Table<?> getJooqTable() {
    return LICENSE_OVERRIDE_LICENSE;
  }

  @Override
  public Class<LicenseOverrideLicenseInternal> getEntityClass() {
    return LicenseOverrideLicenseInternal.class;
  }
}
