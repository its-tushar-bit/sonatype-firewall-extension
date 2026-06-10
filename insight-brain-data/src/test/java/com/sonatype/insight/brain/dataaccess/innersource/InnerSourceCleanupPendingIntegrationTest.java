/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.innersource;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.innersource.InnerSourceApplication;
import com.sonatype.insight.brain.model.innersource.InnerSourceCleanupPending;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test that validates the full InnerSource cleanup flow with a real database:
 * pending row detection, IS record deletion, and pending row removal.
 */
public class InnerSourceCleanupPendingIntegrationTest
    extends AbstractDbDAOTest
{
  private InnerSourceCleanupPendingDAO cleanupPendingDAO;

  private InnerSourceApplicationDAO innerSourceApplicationDAO;

  @Before
  @Override
  public void setup() {
    super.setup();
    cleanupPendingDAO = daoFactory.createInnerSourceCleanupPendingDAO();
    innerSourceApplicationDAO = daoFactory.createInnerSourceApplicationDAO();
  }

  @Test
  public void fullCleanupFlow_newScan_deletesIsRecordsAndPendingRow() {
    InnerSourceApplication isRecord1 = new InnerSourceApplication(application.getId(), "pkg:maven/com.example/a@1.0");
    InnerSourceApplication isRecord2 = new InnerSourceApplication(application.getId(), "pkg:maven/com.example/b@2.0");
    innerSourceApplicationDAO.insert(isRecord1);
    innerSourceApplicationDAO.insert(isRecord2);

    cleanupPendingDAO.insert(new InnerSourceCleanupPending(application.getId(), "scan-old"));

    assertThat(innerSourceApplicationDAO.getByApplicationId(application.getId())).hasSize(2);
    assertThat(cleanupPendingDAO.getById(application.getId())).isNotNull();

    // Simulate a new scan arriving with a different scanId
    boolean isPending = cleanupPendingDAO.isPendingNewScan(application.getId(), "scan-new");
    assertThat(isPending).isTrue();

    // Perform cleanup: delete IS records first, then pending row
    innerSourceApplicationDAO.deleteByApplicationId(application.getId());
    cleanupPendingDAO.deleteByApplicationId(application.getId());

    // Verify both are gone
    assertThat(innerSourceApplicationDAO.getByApplicationId(application.getId())).isEmpty();
    assertThat(cleanupPendingDAO.getById(application.getId())).isNull();
  }

  @Test
  public void fullCleanupFlow_sameScanId_noCleanup() {
    InnerSourceApplication isRecord = new InnerSourceApplication(application.getId(), "pkg:maven/com.example/a@1.0");
    innerSourceApplicationDAO.insert(isRecord);

    cleanupPendingDAO.insert(new InnerSourceCleanupPending(application.getId(), "scan-123"));

    // CM re-evaluation with same scanId — should NOT trigger cleanup
    boolean isPending = cleanupPendingDAO.isPendingNewScan(application.getId(), "scan-123");
    assertThat(isPending).isFalse();

    // IS records should still be there
    assertThat(innerSourceApplicationDAO.getByApplicationId(application.getId())).hasSize(1);
    assertThat(cleanupPendingDAO.getById(application.getId())).isNotNull();
  }

  @Test
  public void fullCleanupFlow_afterCleanup_subsequentScanIsNoOp() {
    innerSourceApplicationDAO.insert(new InnerSourceApplication(application.getId(), "pkg:maven/com.example/a@1.0"));
    cleanupPendingDAO.insert(new InnerSourceCleanupPending(application.getId(), "scan-old"));

    // First new scan: cleanup fires
    assertThat(cleanupPendingDAO.isPendingNewScan(application.getId(), "scan-new")).isTrue();
    innerSourceApplicationDAO.deleteByApplicationId(application.getId());
    cleanupPendingDAO.deleteByApplicationId(application.getId());

    // Subsequent scan: no pending row, no cleanup
    assertThat(cleanupPendingDAO.isPendingNewScan(application.getId(), "scan-newer")).isFalse();
  }

  @Test
  public void fullCleanupFlow_nullLastScanId_anyNewScanTriggersCleanup() {
    innerSourceApplicationDAO.insert(new InnerSourceApplication(application.getId(), "pkg:maven/com.example/a@1.0"));
    cleanupPendingDAO.insert(new InnerSourceCleanupPending(application.getId(), null));

    // Any scanId should trigger cleanup when lastScanId is null
    assertThat(cleanupPendingDAO.isPendingNewScan(application.getId(), "any-scan")).isTrue();

    innerSourceApplicationDAO.deleteByApplicationId(application.getId());
    cleanupPendingDAO.deleteByApplicationId(application.getId());

    assertThat(innerSourceApplicationDAO.getByApplicationId(application.getId())).isEmpty();
    assertThat(cleanupPendingDAO.getById(application.getId())).isNull();
  }

  @Test
  public void fullCleanupFlow_failureRecovery_pendingRowSurvivesForRetry() {
    innerSourceApplicationDAO.insert(new InnerSourceApplication(application.getId(), "pkg:maven/com.example/a@1.0"));
    cleanupPendingDAO.insert(new InnerSourceCleanupPending(application.getId(), "scan-old"));

    // Simulate: isPendingNewScan succeeds, IS records deleted, but pending-row delete "fails" (we skip it)
    assertThat(cleanupPendingDAO.isPendingNewScan(application.getId(), "scan-new")).isTrue();
    innerSourceApplicationDAO.deleteByApplicationId(application.getId());
    // intentionally NOT deleting pending row — simulates failure

    // Next scan: pending row still exists, IS records already gone, cleanup re-fires (harmless)
    assertThat(cleanupPendingDAO.isPendingNewScan(application.getId(), "scan-new")).isTrue();
    // IS records are already gone — delete is a no-op
    assertThat(innerSourceApplicationDAO.getByApplicationId(application.getId())).isEmpty();

    // This time cleanup completes
    cleanupPendingDAO.deleteByApplicationId(application.getId());
    assertThat(cleanupPendingDAO.getById(application.getId())).isNull();
  }
}
