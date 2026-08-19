/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.innersource;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.innersource.InnerSourceApplicationDAO;
import com.sonatype.insight.brain.dataaccess.innersource.InnerSourceCleanupPendingDAO;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles one-time cleanup of stale InnerSource records created by the HDS DependencyGraphDeriver bug (CLM-39800).
 * On the first new scan after upgrade, deletes all IS records for the app so DependencyResolver can rewrite them
 * from corrected HDS data. Skips CM re-evaluations (same scanId) to avoid false triggers.
 */
@Named
@Singleton
public class InnerSourceCleanupPendingService
{
  private static final Logger log = LoggerFactory.getLogger(InnerSourceCleanupPendingService.class);

  private final InnerSourceCleanupPendingDAO cleanupPendingDAO;

  private final InnerSourceApplicationDAO innerSourceApplicationDAO;

  @Inject
  public InnerSourceCleanupPendingService(
      final InnerSourceCleanupPendingDAO cleanupPendingDAO,
      final InnerSourceApplicationDAO innerSourceApplicationDAO)
  {
    this.cleanupPendingDAO = cleanupPendingDAO;
    this.innerSourceApplicationDAO = innerSourceApplicationDAO;
  }

  /**
   * Called during scan processing. Checks whether this app's InnerSource records need to be wiped and rebuilt.
   *
   * Cleanup triggers only if the scan being processed is NOT the same scan that was the latest when the
   * {@code innersource_cleanup_pending} table was populated at upgrade time. This distinction prevents
   * CM re-evaluations (which reuse the same scanId) from triggering a false cleanup.
   *
   * @param applicationId the app being scanned
   * @param currentScanId the scanId of the scan currently being processed; if null, cleanup always triggers
   * @return true if cleanup was performed (caller should expect DependencyResolver to rewrite fresh records)
   */
  public boolean cleanupRecordsIfPending(String applicationId, String currentScanId) {
    if (!cleanupPendingDAO.isPendingNewScan(applicationId, currentScanId)) {
      return false;
    }

    log.info("Performing InnerSource cleanup for app {} — new scan detected (currentScanId={})",
        applicationId, currentScanId);
    try (TransactionContext tx = innerSourceApplicationDAO.createTransactionContext()) {
      tx.begin();
      innerSourceApplicationDAO.deleteByApplicationId(tx, applicationId);
      cleanupPendingDAO.deleteByApplicationId(tx, applicationId);
      tx.commit();
    }
    return true;
  }
}
