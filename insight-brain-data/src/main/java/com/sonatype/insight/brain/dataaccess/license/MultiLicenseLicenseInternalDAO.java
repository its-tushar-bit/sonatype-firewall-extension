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

import com.sonatype.insight.brain.dataaccess.AbstractDatamartSqlDAO;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.model.license.MultiLicenseLicenseInternal;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.jooq.Table;
import org.jooq.UpdatableRecord;

import static com.sonatype.insight.brain.jooq.generated.dm.tables.MultiLicenseLicense.MULTI_LICENSE_LICENSE;

@Named
@Singleton
public class MultiLicenseLicenseInternalDAO
    extends AbstractDatamartSqlDAO<MultiLicenseLicenseInternal>
{
  @Inject
  public MultiLicenseLicenseInternalDAO(final DataMartDataStore dataMartDataStore) {
    super(dataMartDataStore);
  }

  @Override
  @SuppressWarnings("unchecked")
  public int insert(final TransactionContext tx, final MultiLicenseLicenseInternal entity) {
    UpdatableRecord<?> record = fromEntity(tx.dsl().newRecord(MULTI_LICENSE_LICENSE), entity);
    return tx.dsl().insertInto(MULTI_LICENSE_LICENSE).set(record).execute();
  }

  @Override
  public void delete(final TransactionContext tx, final MultiLicenseLicenseInternal entity) {
    if (entity == null) {
      return;
    }
    tx.dsl()
        .deleteFrom(MULTI_LICENSE_LICENSE)
        .where(MULTI_LICENSE_LICENSE.MULTI_LICENSE_ID.eq(entity.getMultiLicenseId()))
        .and(MULTI_LICENSE_LICENSE.LICENSE_ID.eq(entity.getLicenseId()))
        .execute();
  }

  public List<MultiLicenseLicenseInternal> getByMultiLicenseId(TransactionContext tx, String multiLicenseId) {
    return tx.dsl()
        .selectFrom(MULTI_LICENSE_LICENSE)
        .where(MULTI_LICENSE_LICENSE.MULTI_LICENSE_ID.eq(multiLicenseId))
        .fetchInto(MultiLicenseLicenseInternal.class);
  }

  @Override
  public Table<?> getJooqTable() {
    return MULTI_LICENSE_LICENSE;
  }

  @Override
  public Class<MultiLicenseLicenseInternal> getEntityClass() {
    return MultiLicenseLicenseInternal.class;
  }
}
