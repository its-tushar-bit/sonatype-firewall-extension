/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sast;

import java.util.Collections;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.sast.SastScan;
import com.sonatype.insight.brain.model.sast.SastScmScanContext;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.SastScan.SAST_SCAN;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.SastScmScanContext.SAST_SCM_SCAN_CONTEXT;

@Named
@Singleton
public class SastScanDAO
    extends AbstractOperationalSqlDAO<SastScan>
{
  private final SastScmScanContextDAO sastScmScanContextDAO;

  @Inject
  public SastScanDAO(
      final OperationalDataStore operationalDataStore,
      final SastScmScanContextDAO sastScmScanContextDAO)
  {
    super(operationalDataStore);
    this.sastScmScanContextDAO = sastScmScanContextDAO;
  }

  @Override
  public void update(final TransactionContext tx, final SastScan entity) {
    throw new UnsupportedOperationException("The SastScan table does not support update operations");
  }

  @Override
  public void delete(final TransactionContext tx, final SastScan entity) {
    final String sastScmScanContextId = entity.getSastScmScanContextId();
    if (sastScmScanContextId != null) {
      final SastScmScanContext sastScmScanContext = sastScmScanContextDAO.getById(tx, sastScmScanContextId);
      if (sastScmScanContext != null) {
        sastScmScanContextDAO.delete(tx, sastScmScanContext);
      }
    }
    super.delete(tx, entity);
  }

  public List<SastScan> getByApplicationId(final String applicationId) {
    try (final TransactionContext tx = createTransactionContext()) {
      return getByApplicationId(tx, applicationId);
    }
  }

  public List<SastScan> getByApplicationId(final TransactionContext tx, final String applicationId) {
    return tx.dsl()
        .selectFrom(SAST_SCAN)
        .where(SAST_SCAN.APPLICATION_ID.eq(applicationId))
        .fetch(this::toEntity);
  }

  public List<SastScan> getByApplicationIdAndBranchName(final String applicationId, final String branchName) {
    if (StringUtils.isEmpty(branchName)) {
      return Collections.emptyList();
    }
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .select(SAST_SCAN.fields())
          .from(SAST_SCAN)
          .innerJoin(SAST_SCM_SCAN_CONTEXT)
          .on(SAST_SCAN.SAST_SCM_SCAN_CONTEXT_ID.eq(SAST_SCM_SCAN_CONTEXT.SAST_SCM_SCAN_CONTEXT_ID))
          .where(SAST_SCAN.APPLICATION_ID.eq(applicationId))
          .and(SAST_SCM_SCAN_CONTEXT.BRANCH_NAME.eq(branchName))
          .fetch(r -> toEntity(r.into(SAST_SCAN)));
    }
  }

  public void deleteByApplicationId(final String applicationId) {
    try (final TransactionContext tx = createTransactionContext()) {
      tx.begin();
      deleteByApplicationId(tx, applicationId);
      tx.commit();
    }
  }

  public void deleteByApplicationId(final TransactionContext tx, final String applicationId) {
    getByApplicationId(tx, applicationId).forEach(sastScan -> delete(tx, sastScan));
  }

  @Override
  public Table<?> getJooqTable() {
    return SAST_SCAN;
  }

  @Override
  public Class<SastScan> getEntityClass() {
    return SastScan.class;
  }
}
