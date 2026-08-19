/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.thirdpartyscans;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractThirdPartyScansSqlDAO;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import static com.sonatype.insight.brain.jooq.generated.thirdpartyscans.tables.ThirdPartyScan.THIRD_PARTY_SCAN;

@Named
@Singleton
public class ThirdPartyScanDAO
    extends AbstractThirdPartyScansSqlDAO<ThirdPartyScan>
{
  @Inject
  public ThirdPartyScanDAO(final ThirdPartyScansDataStore thirdPartyScansDataStore) {
    super(thirdPartyScansDataStore);
  }

  public ThirdPartyScan getByThirdPartyFileIdAndScanId(String thirdPartyFileId, String scanId) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(THIRD_PARTY_SCAN)
          .where(THIRD_PARTY_SCAN.THIRD_PARTY_FILE_ID.eq(thirdPartyFileId)
              .and(THIRD_PARTY_SCAN.SCAN_ID.eq(scanId)))
          .fetchOneInto(ThirdPartyScan.class);
    }
  }

  public List<ThirdPartyScan> getByScanId(String scanId) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(THIRD_PARTY_SCAN)
          .where(THIRD_PARTY_SCAN.SCAN_ID.eq(scanId))
          .fetchInto(ThirdPartyScan.class);
    }
  }

  public ThirdPartyScan getByThirdPartyFileId(String thirdPartyFileId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByThirdPartyFileId(tx, thirdPartyFileId);
    }
  }

  public ThirdPartyScan getByThirdPartyFileId(TransactionContext tx, String thirdPartyFileId) {
    return tx.dsl()
        .selectFrom(THIRD_PARTY_SCAN)
        .where(THIRD_PARTY_SCAN.THIRD_PARTY_FILE_ID.eq(thirdPartyFileId))
        .fetchOneInto(ThirdPartyScan.class);
  }

  public List<ThirdPartyScan> getByScanRequestId(String scanRequestId) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(THIRD_PARTY_SCAN)
          .where(THIRD_PARTY_SCAN.SCAN_REQUEST_ID.eq(scanRequestId))
          .fetchInto(ThirdPartyScan.class);
    }
  }

  public ThirdPartyScan getSingleByScanRequestId(String scanRequestId) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(THIRD_PARTY_SCAN)
          .where(THIRD_PARTY_SCAN.SCAN_REQUEST_ID.eq(scanRequestId))
          .limit(1)
          .fetchOneInto(ThirdPartyScan.class);
    }
  }

  public void updateScanIdForScanRequest(String scanRequestId, String scanId) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      tx.dsl()
          .update(THIRD_PARTY_SCAN)
          .set(THIRD_PARTY_SCAN.SCAN_ID, scanId)
          .where(THIRD_PARTY_SCAN.SCAN_REQUEST_ID.eq(scanRequestId))
          .execute();
      tx.commit();
    }
  }

  public int deleteByThirdPartyFileId(TransactionContext tx, String thirdPartyFileId) {
    return tx.dsl()
        .deleteFrom(THIRD_PARTY_SCAN)
        .where(THIRD_PARTY_SCAN.THIRD_PARTY_FILE_ID.eq(thirdPartyFileId))
        .execute();
  }

  @Override
  public org.jooq.Table<?> getJooqTable() {
    return THIRD_PARTY_SCAN;
  }

  @Override
  public Class<ThirdPartyScan> getEntityClass() {
    return ThirdPartyScan.class;
  }
}
