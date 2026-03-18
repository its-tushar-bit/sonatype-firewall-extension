/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sast;

import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.sast.SastFindingSeverity;
import com.sonatype.insight.brain.model.sast.SastFinding;
import com.sonatype.insight.brain.model.sast.SastFindingConfidence;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import static java.lang.String.format;

@Named
@Singleton
public class SastFindingDAO
    extends AbstractOperationalSqlDAO<SastFinding>
{
  @Inject
  public SastFindingDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  @Override
  public void insert(final TransactionContext tx, final SastFinding entity) {
    validateEnumOrdinalValue(SastFindingConfidence.class, entity.getConfidence());
    validateSeverityId(entity.getSeverityId());
    super.insert(tx, entity);
  }

  @Override
  public void update(final TransactionContext tx, final SastFinding entity) {
    throw new UnsupportedOperationException("The SastFinding table does not support update operations");
  }

  public List<SastFinding> getBySastScanIdOrderBySeverityDesc(final String sastScanId) {
    try (final TransactionContext tx = createTransactionContext()) {
      return getBySastScanIdOrderBySeverityDesc(tx, sastScanId);
    }
  }

  public List<SastFinding> getBySastScanIdOrderBySeverityDesc(final TransactionContext tx, final String sastScanId) {
    final String sQuery =
        "SELECT entity FROM SastFinding entity WHERE entity.sastScanId=?1 ORDER BY entity.severityId DESC";
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

  private void validateSeverityId(final int severityId) {
    if (SastFindingSeverity.getById(severityId) == null) {
      throw new BadRequestException("Invalid id for SastFindingSeverity: " + severityId);
    }
  }
}
