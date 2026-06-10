/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.innersource;

import com.sonatype.insight.brain.dataaccess.innersource.InnerSourceApplicationDAO;
import com.sonatype.insight.brain.dataaccess.innersource.InnerSourceCleanupPendingDAO;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class InnerSourceCleanupPendingServiceTest
{
  @Mock
  private InnerSourceCleanupPendingDAO cleanupPendingDAO;

  @Mock
  private InnerSourceApplicationDAO innerSourceApplicationDAO;

  @Mock
  private TransactionContext tx;

  private InnerSourceCleanupPendingService underTest;

  @Before
  public void setup() {
    when(innerSourceApplicationDAO.createTransactionContext()).thenReturn(tx);
    underTest = new InnerSourceCleanupPendingService(cleanupPendingDAO, innerSourceApplicationDAO);
  }

  @Test
  public void cleanupRecordsIfPending_notPending_returnsFalse() {
    when(cleanupPendingDAO.isPendingNewScan("app1", "scan-123")).thenReturn(false);

    boolean result = underTest.cleanupRecordsIfPending("app1", "scan-123");

    assertThat(result).isFalse();
    verifyNoInteractions(innerSourceApplicationDAO);
  }

  @Test
  public void cleanupRecordsIfPending_newScanDetected_deletesInSingleTransaction() {
    when(cleanupPendingDAO.isPendingNewScan("app1", "scan-new")).thenReturn(true);

    boolean result = underTest.cleanupRecordsIfPending("app1", "scan-new");

    assertThat(result).isTrue();
    InOrder order = inOrder(tx, innerSourceApplicationDAO, cleanupPendingDAO);
    order.verify(tx).begin();
    order.verify(innerSourceApplicationDAO).deleteByApplicationId(tx, "app1");
    order.verify(cleanupPendingDAO).deleteByApplicationId(tx, "app1");
    order.verify(tx).commit();
  }

  @Test
  public void cleanupRecordsIfPending_nullCurrentScanId_deletesAndReturnsTrue() {
    when(cleanupPendingDAO.isPendingNewScan("app1", null)).thenReturn(true);

    boolean result = underTest.cleanupRecordsIfPending("app1", null);

    assertThat(result).isTrue();
    InOrder order = inOrder(tx, innerSourceApplicationDAO, cleanupPendingDAO);
    order.verify(tx).begin();
    order.verify(innerSourceApplicationDAO).deleteByApplicationId(tx, "app1");
    order.verify(cleanupPendingDAO).deleteByApplicationId(tx, "app1");
    order.verify(tx).commit();
  }
}
