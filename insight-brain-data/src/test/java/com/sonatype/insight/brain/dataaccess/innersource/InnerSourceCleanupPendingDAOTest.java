/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.innersource;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.innersource.InnerSourceCleanupPending;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class InnerSourceCleanupPendingDAOTest
    extends AbstractDbDAOTest
{
  private InnerSourceCleanupPendingDAO dao;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createInnerSourceCleanupPendingDAO();
  }

  @Test
  public void isPendingNewScan_noPendingRow_returnsFalse() {
    assertThat(dao.isPendingNewScan(application.getId(), "scan-123")).isFalse();
  }

  @Test
  public void isPendingNewScan_sameScanId_returnsFalse() {
    dao.insert(new InnerSourceCleanupPending(application.getId(), "scan-123"));

    assertThat(dao.isPendingNewScan(application.getId(), "scan-123")).isFalse();
  }

  @Test
  public void isPendingNewScan_differentScanId_returnsTrue() {
    dao.insert(new InnerSourceCleanupPending(application.getId(), "scan-old"));

    assertThat(dao.isPendingNewScan(application.getId(), "scan-new")).isTrue();
  }

  @Test
  public void isPendingNewScan_nullLastScanId_anyScanReturnsTrue() {
    dao.insert(new InnerSourceCleanupPending(application.getId(), null));

    assertThat(dao.isPendingNewScan(application.getId(), "scan-any")).isTrue();
  }

  @Test
  public void isPendingNewScan_nullCurrentScanId_returnsTrue() {
    dao.insert(new InnerSourceCleanupPending(application.getId(), "scan-stored"));

    assertThat(dao.isPendingNewScan(application.getId(), null)).isTrue();
  }

  @Test
  public void isPendingNewScan_bothNull_returnsTrue() {
    dao.insert(new InnerSourceCleanupPending(application.getId(), null));

    assertThat(dao.isPendingNewScan(application.getId(), null)).isTrue();
  }

  @Test
  public void deleteByApplicationId_removesRow() {
    dao.insert(new InnerSourceCleanupPending(application.getId(), "scan-123"));

    dao.deleteByApplicationId(application.getId());

    assertThat(dao.getById(application.getId())).isNull();
  }

  @Test
  public void deleteByApplicationId_noRow_noOp() {
    dao.deleteByApplicationId(application.getId());

    assertThat(dao.getById(application.getId())).isNull();
  }
}
