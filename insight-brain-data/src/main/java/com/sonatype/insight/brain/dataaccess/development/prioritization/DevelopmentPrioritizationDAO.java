/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.dataaccess.development.prioritization;

import java.util.Date;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.prioritization.DevelopmentPrioritization;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.Table;
import org.jooq.UpdatableRecord;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.DevelopmentPrioritization.DEVELOPMENT_PRIORITIZATION;

@Named
@Singleton
public class DevelopmentPrioritizationDAO
    extends AbstractOperationalSqlDAO<DevelopmentPrioritization>
{
  private final DevelopmentPrioritizationComponentInfoDAO developmentPrioritizationComponentInfoDAO;

  @Inject
  public DevelopmentPrioritizationDAO(
      final OperationalDataStore operationalDataStore,
      final DevelopmentPrioritizationComponentInfoDAO developmentPrioritizationComponentInfoDAO)
  {
    super(operationalDataStore);
    this.developmentPrioritizationComponentInfoDAO = developmentPrioritizationComponentInfoDAO;
  }

  @Override
  protected UpdatableRecord<?> fromEntity(final UpdatableRecord<?> record, final DevelopmentPrioritization entity) {
    super.fromEntity(record, entity);
    Date now = new Date();
    record.set(DEVELOPMENT_PRIORITIZATION.CREATED_AT, entity.getCreatedAt() != null
        ? entity.getCreatedAt()
        : now);
    record.set(DEVELOPMENT_PRIORITIZATION.UPDATED_AT, entity.getUpdatedAt() != null
        ? entity.getUpdatedAt()
        : now);
    return record;
  }

  public DevelopmentPrioritization getByScanId(final String scanId) {
    try (final TransactionContext tx = createTransactionContext()) {
      return getByScanId(tx, scanId);
    }
  }

  public DevelopmentPrioritization getByScanId(final TransactionContext tx, final String scanId) {
    return toEntity(tx.dsl()
        .selectFrom(DEVELOPMENT_PRIORITIZATION)
        .where(DEVELOPMENT_PRIORITIZATION.SCAN_ID.eq(scanId))
        .fetchOne());
  }

  public void deleteByScanIdCascade(final TransactionContext tx, final String scanId) {
    // This method would cascade-delete all children DevelopmentPrioritizationComponentInfo entities
    developmentPrioritizationComponentInfoDAO.deleteAllByScanId(tx, scanId);

    tx.dsl()
        .deleteFrom(DEVELOPMENT_PRIORITIZATION)
        .where(DEVELOPMENT_PRIORITIZATION.SCAN_ID.eq(scanId))
        .execute();
  }

  @Override
  public Table<?> getJooqTable() {
    return DEVELOPMENT_PRIORITIZATION;
  }

  @Override
  public Class<DevelopmentPrioritization> getEntityClass() {
    return DevelopmentPrioritization.class;
  }
}
