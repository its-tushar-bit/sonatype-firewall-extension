/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sast;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.sast.SastFinding;
import com.sonatype.insight.brain.model.sast.SastFindingConfidence;
import com.sonatype.insight.brain.model.sast.SastFindingSeverity;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import static java.lang.String.format;

public class SastFindingDAO extends AbstractOperationalSqlDAO<SastFinding>
{
  private final SastRemediationDAO sastRemediationDAO = new SastRemediationDAO();

  @Override
  public void insert(final TransactionContext tx, final SastFinding entity) {
    validateEnumOrdinalValue(SastFindingConfidence.class, entity.getConfidence());
    validateEnumOrdinalValue(SastFindingSeverity.class, entity.getSeverity());
    super.insert(tx, entity);
  }

  @Override
  public void update(final TransactionContext tx, final SastFinding entity) {
    throw new UnsupportedOperationException("The SastFinding table does not support update operations");
  }

  @Override
  public void delete(final TransactionContext tx, final SastFinding entity) {
    /*
     * Do not use this method for deleting SastFinding entities since it does not cascade to SastRemediations.
     * Instead, use the deleteBySastScanId method to delete all SastFindings and SastRemediations for a SastScan.
     * For now, there is no usecase to delete some SastFindings from a SastScan
     */
    super.delete(tx, entity);
  }

  @Override
  public void delete(final SastFinding entity) {
    /*
     * Do not use this method for deleting SastFinding entities since it does not cascade to SastRemediations.
     * Instead, use the deleteBySastScanId method to delete all SastFindings and SastRemediations for a SastScan.
     * For now, there is no usecase to delete some SastFindings from a SastScan
     */
    super.delete(entity);
  }

  public List<SastFinding> getBySastScanId(final String sastScanId) {
    try (final TransactionContext tx = createTransactionContext()) {
      return getBySastScanId(tx, sastScanId);
    }
  }

  public List<SastFinding> getBySastScanId(final TransactionContext tx, final String sastScanId) {
    final String sQuery = "SELECT entity FROM SastFinding entity WHERE entity.sastScanId=?1";
    return getList(tx, sQuery, sastScanId);
  }

  public void deleteBySastScanId(final String sastScanId) {
    try (final TransactionContext tx = createTransactionContext()) {
      tx.begin();
      deleteBySastScanId(tx, sastScanId);
      tx.commit();
    }
  }

  public void deleteBySastScanId(final TransactionContext tx, final String sastScanId) {
    getBySastScanId(tx, sastScanId)
        .stream()
        .map(SastFinding::getId)
        .forEach(sastFindingId -> sastRemediationDAO.deleteBySastFindingId(tx, sastFindingId));

    final String sQuery = "DELETE FROM SastFinding entity WHERE entity.sastScanId=?1";
    createQuery(sQuery, sastScanId).executeUpdate(tx);
  }

  private static <E extends Enum<E>> void validateEnumOrdinalValue(final Class<E> enumClass, final int ordinal) {
    int numEnumValues = enumClass.getEnumConstants().length;
    if (ordinal < 0 || ordinal >= numEnumValues) {
      throw new BadRequestException(format("The ordinal value '%s' is outside the range [0, %d) for '%s'",
          ordinal, numEnumValues, enumClass.getSimpleName()));

    }
  }
}
