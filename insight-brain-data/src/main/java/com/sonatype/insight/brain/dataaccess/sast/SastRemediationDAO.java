/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sast;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.sast.SastRemediation;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.SastRemediation.SAST_REMEDIATION;

@Named
@Singleton
public class SastRemediationDAO
    extends AbstractOperationalSqlDAO<SastRemediation>
{
  @Inject
  public SastRemediationDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  @Override
  public int update(final TransactionContext tx, final SastRemediation entity) {
    throw new UnsupportedOperationException("The SastRemediation table does not support update operations");
  }

  public List<SastRemediation> getBySastFindingId(final String sastFindingId) {
    try (final TransactionContext tx = createTransactionContext()) {
      return getBySastFindingId(tx, sastFindingId);
    }
  }

  public List<SastRemediation> getBySastFindingId(final TransactionContext tx, final String sastFindingId) {
    return tx.dsl()
        .selectFrom(SAST_REMEDIATION)
        .where(SAST_REMEDIATION.SAST_FINDING_ID.eq(sastFindingId))
        .fetch(this::toEntity);
  }

  public void deleteBySastFindingId(final String sastFindingId) {
    try (final TransactionContext tx = createTransactionContext()) {
      tx.begin();
      deleteBySastFindingId(tx, sastFindingId);
      tx.commit();
    }
  }

  public void deleteBySastFindingId(final TransactionContext tx, final String sastFindingId) {
    tx.dsl()
        .deleteFrom(SAST_REMEDIATION)
        .where(SAST_REMEDIATION.SAST_FINDING_ID.eq(sastFindingId))
        .execute();
  }

  @Override
  public Table<?> getJooqTable() {
    return SAST_REMEDIATION;
  }

  @Override
  public Class<SastRemediation> getEntityClass() {
    return SastRemediation.class;
  }
}
