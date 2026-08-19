/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.innersource;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.jooq.Condition;
import org.jooq.Table;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.innersource.InnerSourceCleanupPending;
import com.sonatype.insight.dataaccess.TransactionContext;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.InnersourceCleanupPending.INNERSOURCE_CLEANUP_PENDING;

@Named
@Singleton
public class InnerSourceCleanupPendingDAO
    extends AbstractOperationalSqlDAO<InnerSourceCleanupPending>
{
  @Inject
  public InnerSourceCleanupPendingDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  /**
   * Returns true if a pending cleanup row exists for this app and the scan being processed is different
   * from the one recorded at upgrade time. This ensures cleanup fires only on genuinely new scans, not
   * CM re-evaluations of the same scan.
   *
   * @param applicationId the app being scanned
   * @param currentScanId the scanId of the scan being processed; if null, any pending row matches
   */
  public boolean isPendingNewScan(String applicationId, String currentScanId) {
    try (TransactionContext tx = createTransactionContext()) {
      Condition condition = INNERSOURCE_CLEANUP_PENDING.APPLICATION_ID.eq(applicationId);
      if (currentScanId != null) {
        condition = condition.and(
            INNERSOURCE_CLEANUP_PENDING.LAST_SCAN_ID.isNull()
                .or(INNERSOURCE_CLEANUP_PENDING.LAST_SCAN_ID.ne(currentScanId)));
      }
      return tx.dsl()
          .fetchExists(
              tx.dsl()
                  .selectOne()
                  .from(INNERSOURCE_CLEANUP_PENDING)
                  .where(condition));
    }
  }

  public void deleteByApplicationId(TransactionContext tx, String applicationId) {
    tx.dsl()
        .deleteFrom(INNERSOURCE_CLEANUP_PENDING)
        .where(INNERSOURCE_CLEANUP_PENDING.APPLICATION_ID.eq(applicationId))
        .execute();
  }

  public void deleteByApplicationId(String applicationId) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      deleteByApplicationId(tx, applicationId);
      tx.commit();
    }
  }

  @Override
  public Table<?> getJooqTable() {
    return INNERSOURCE_CLEANUP_PENDING;
  }

  @Override
  public Class<InnerSourceCleanupPending> getEntityClass() {
    return InnerSourceCleanupPending.class;
  }
}
