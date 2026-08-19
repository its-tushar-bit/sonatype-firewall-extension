/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.thirdpartyscans;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractThirdPartyScansSqlDAO;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import static com.sonatype.insight.brain.jooq.generated.thirdpartyscans.tables.ThirdPartyFile.THIRD_PARTY_FILE;

@Named
@Singleton
public class ThirdPartyFileDAO
    extends AbstractThirdPartyScansSqlDAO<ThirdPartyFile>
{
  private final ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO;

  private final ThirdPartyScanDAO thirdPartyScanDAO;

  private final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  private final ThirdPartyUnknownComponentDAO thirdPartyUnknownComponentDAO;

  @Inject
  public ThirdPartyFileDAO(
      final ThirdPartyScansDataStore thirdPartyScansDataStore,
      final ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO,
      final ThirdPartyScanDAO thirdPartyScanDAO,
      final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO,
      final ThirdPartyUnknownComponentDAO thirdPartyUnknownComponentDAO)
  {
    super(thirdPartyScansDataStore);
    this.thirdPartyFileCoordinateDAO = thirdPartyFileCoordinateDAO;
    this.thirdPartyScanDAO = thirdPartyScanDAO;
    this.thirdPartySbomMetadataDAO = thirdPartySbomMetadataDAO;
    this.thirdPartyUnknownComponentDAO = thirdPartyUnknownComponentDAO;
  }

  public List<ThirdPartyFile> getAll() {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(THIRD_PARTY_FILE)
          .fetchInto(ThirdPartyFile.class);
    }
  }

  public List<ThirdPartyFile> getByScanId(String scanId) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .select(THIRD_PARTY_FILE.fields())
          .from(THIRD_PARTY_FILE)
          .join(com.sonatype.insight.brain.jooq.generated.thirdpartyscans.tables.ThirdPartyScan.THIRD_PARTY_SCAN)
          .on(com.sonatype.insight.brain.jooq.generated.thirdpartyscans.tables.ThirdPartyScan.THIRD_PARTY_SCAN.THIRD_PARTY_FILE_ID
              .eq(THIRD_PARTY_FILE.THIRD_PARTY_FILE_ID))
          .where(
              com.sonatype.insight.brain.jooq.generated.thirdpartyscans.tables.ThirdPartyScan.THIRD_PARTY_SCAN.SCAN_ID
                  .eq(scanId))
          .fetchInto(ThirdPartyFile.class);
    }
  }

  public void deleteByScanId(String scanId) {
    getByScanId(scanId).forEach(this::delete);
  }

  public void delete(TransactionContext tx, String thirdPartyFileId) {
    delete(tx, getById(thirdPartyFileId));
  }

  @Override
  public void delete(TransactionContext tx, ThirdPartyFile thirdPartyFile) {
    // cascade delete file coordinates
    thirdPartyFileCoordinateDAO.deleteByThirdPartyFileId(tx, thirdPartyFile.getId());

    // cascade delete sbom metadata
    thirdPartySbomMetadataDAO.deleteByThirdPartyFileId(tx, thirdPartyFile.getId());

    // cascade delete scanned files
    thirdPartyScanDAO.deleteByThirdPartyFileId(tx, thirdPartyFile.getId());

    // cascade delete unknown components
    thirdPartyUnknownComponentDAO.deleteByThirdPartyFileId(tx, thirdPartyFile.getId());

    // Delete this entity using jOOQ
    tx.dsl()
        .deleteFrom(THIRD_PARTY_FILE)
        .where(THIRD_PARTY_FILE.THIRD_PARTY_FILE_ID.eq(thirdPartyFile.getId()))
        .execute();

    // Call super for search index changes
    super.delete(tx, thirdPartyFile);
  }

  @Override
  public org.jooq.Table<?> getJooqTable() {
    return THIRD_PARTY_FILE;
  }

  @Override
  public Class<ThirdPartyFile> getEntityClass() {
    return ThirdPartyFile.class;
  }
}
