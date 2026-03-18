/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sast;

import java.util.Collections;
import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.sast.SastScan;
import com.sonatype.insight.brain.model.sast.SastScmScanContext;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.apache.commons.lang3.StringUtils;

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
    final SastScmScanContext sastScmScanContext = sastScmScanContextDAO.getById(tx, entity.getSastScmScanContextId());
    sastScmScanContextDAO.delete(tx, sastScmScanContext);
    super.delete(tx, entity);
  }

  public List<SastScan> getByApplicationId(final String applicationId) {
    try (final TransactionContext tx = createTransactionContext()) {
      return getByApplicationId(tx, applicationId);
    }
  }

  public List<SastScan> getByApplicationId(final TransactionContext tx, final String applicationId) {
    final String sQuery = "SELECT entity FROM SastScan entity WHERE entity.applicationId=?1";
    return getList(tx, sQuery, applicationId);
  }

  @SuppressWarnings("unchecked")
  public List<SastScan> getByApplicationIdAndBranchName(final String applicationId, final String branchName) {
    if (StringUtils.isEmpty(branchName)) {
      return Collections.emptyList();
    }
    final String sQuery = "SELECT * FROM " + getDatabaseSchema() + ".sast_scan scanEntity" +
        " INNER JOIN " + getDatabaseSchema() + ".sast_scm_scan_context scmEntity" +
        " ON scanEntity.sast_scm_scan_context_id = scmEntity.sast_scm_scan_context_id" +
        " WHERE scanEntity.application_id = ?1" +
        " AND scmEntity.branch_name = ?2";

    try (TransactionContext tx = createTransactionContext()) {
      jakarta.persistence.Query query = tx.createNativeQuery(sQuery, SastScan.class);
      query.setParameter(1, applicationId);
      query.setParameter(2, branchName);
      return query.getResultList();
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
}
