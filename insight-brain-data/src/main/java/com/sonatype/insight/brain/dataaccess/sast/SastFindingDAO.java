/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sast;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.sast.SastFinding;
import com.sonatype.insight.brain.model.sast.SastFindingConfidence;
import com.sonatype.insight.brain.model.sast.SastFindingSeverity;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.SastFinding.SAST_FINDING;
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
  public int insert(final TransactionContext tx, final SastFinding entity) {
    validateEnumOrdinalValue(SastFindingConfidence.class, entity.getConfidence());
    validateSeverityId(entity.getSeverityId());
    return super.insert(tx, entity);
  }

  @Override
  public int update(final TransactionContext tx, final SastFinding entity) {
    throw new UnsupportedOperationException("The SastFinding table does not support update operations");
  }

  public List<SastFinding> getBySastScanIdOrderBySeverityDesc(final String sastScanId) {
    try (final TransactionContext tx = createTransactionContext()) {
      return getBySastScanIdOrderBySeverityDesc(tx, sastScanId);
    }
  }

  public List<SastFinding> getBySastScanIdOrderBySeverityDesc(final TransactionContext tx, final String sastScanId) {
    return tx.dsl()
        .selectFrom(SAST_FINDING)
        .where(SAST_FINDING.SAST_SCAN_ID.eq(sastScanId))
        .orderBy(SAST_FINDING.SEVERITY.desc())
        .fetch(this::toEntity);
  }

  public void deleteBySastScanId(final String sastScanId) {
    try (final TransactionContext tx = createTransactionContext()) {
      tx.begin();
      deleteBySastScanId(tx, sastScanId);
      tx.commit();
    }
  }

  public void deleteBySastScanId(final TransactionContext tx, final String sastScanId) {
    tx.dsl()
        .deleteFrom(SAST_FINDING)
        .where(SAST_FINDING.SAST_SCAN_ID.eq(sastScanId))
        .execute();
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

  @Override
  public Table<?> getJooqTable() {
    return SAST_FINDING;
  }

  @Override
  public Class<SastFinding> getEntityClass() {
    return SastFinding.class;
  }
}
