/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.index;

import java.util.Date;
import java.util.Optional;

import com.sonatype.insight.brain.dataaccess.SearchIndexChangeDAO;
import com.sonatype.insight.brain.dataaccess.searchindex.SearchIndexEstateSnapshotDAO;
import com.sonatype.insight.brain.dataaccess.searchindex.SearchIndexGenerationDAO;
import com.sonatype.insight.brain.dataaccess.searchindex.SearchIndexHealthDAO;
import com.sonatype.insight.brain.dataaccess.searchindex.SearchIndexJobDAO;
import com.sonatype.insight.brain.model.searchindex.SearchIndexHealth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SearchIndexHealthServiceTest
{
  @Mock
  private SearchIndexHealthDAO healthDAO;

  @Mock
  private SearchIndexJobDAO jobDAO;

  @Mock
  private SearchIndexGenerationDAO generationDAO;

  @Mock
  private SearchIndexEstateSnapshotDAO estateSnapshotDAO;

  @Mock
  private SearchIndexChangeDAO changeDAO;

  private SearchIndexHealthService service;

  @BeforeEach
  public void setUp() {
    service = new SearchIndexHealthService(healthDAO, jobDAO, generationDAO, estateSnapshotDAO, changeDAO);
    lenient().when(healthDAO.getOrSeedCurrent()).thenReturn(health(0L));
    lenient().when(jobDAO.findActiveJob()).thenReturn(Optional.empty());
  }

  /**
   * Queue depth comes off the outbox rather than a stored counter, so a health row whose gauge has
   * gone stale is corrected on the next refresh instead of being trusted.
   */
  @Test
  public void refreshDerivedStatus_takesQueueDepthFromTheOutboxNotTheStoredCounter() {
    when(healthDAO.getOrSeedCurrent()).thenReturn(health(999L));
    when(changeDAO.countPending()).thenReturn(3L);
    when(changeDAO.findOldestPendingCreatedAt()).thenReturn(new Date());

    service.refreshDerivedStatus();

    assertThat(capturePendingCount()).isEqualTo(3L);
  }

  /**
   * A drained queue has to write a null pointer, otherwise lag keeps being measured from a change
   * that was applied long ago and the tenant never returns to healthy.
   */
  @Test
  public void refreshDerivedStatus_clearsThePointerAndLagOnADrainedQueue() {
    when(changeDAO.countPending()).thenReturn(0L);

    service.refreshDerivedStatus();

    ArgumentCaptor<Date> oldest = ArgumentCaptor.forClass(Date.class);
    verify(healthDAO).updateDerivedStatus(anyString(), anyString(), anyLong(), nullable(String.class), anyLong(),
        oldest.capture());
    assertThat(oldest.getValue()).isNull();
    verify(changeDAO, never()).findOldestPendingCreatedAt();
  }

  @Test
  public void refreshDerivedStatus_measuresLagFromTheOldestPendingChange() {
    when(changeDAO.countPending()).thenReturn(1L);
    when(changeDAO.findOldestPendingCreatedAt()).thenReturn(new Date(System.currentTimeMillis() - 600_000L));

    service.refreshDerivedStatus();

    ArgumentCaptor<Long> lag = ArgumentCaptor.forClass(Long.class);
    verify(healthDAO).updateDerivedStatus(anyString(), anyString(), lag.capture(), nullable(String.class), anyLong(),
        nullable(Date.class));
    assertThat(lag.getValue()).isBetween(595L, 605L);
  }

  @Test
  public void recordOutboxBatch_addsAbandonedChangesToTheFailedTally() {
    when(changeDAO.countPending()).thenReturn(0L);

    service.recordOutboxBatch(4L, 2L);

    verify(healthDAO).recordAbandonedChanges(2L);
  }

  /**
   * A batch that applied everything cleanly still has to refresh, because the queue it drained is
   * what the gauges describe.
   */
  @Test
  public void recordOutboxBatch_refreshesWithoutTouchingTheFailedTallyWhenNothingWasAbandoned() {
    when(changeDAO.countPending()).thenReturn(0L);

    service.recordOutboxBatch(4L, 0L);

    verify(healthDAO).recordAbandonedChanges(0L);
    verify(healthDAO).updateDerivedStatus(anyString(), anyString(), anyLong(), nullable(String.class), anyLong(),
        nullable(Date.class));
  }

  /**
   * An empty batch must not cost a recount. The indexer polls on a timer, so most wake-ups find
   * nothing and would otherwise scan the outbox for no reason.
   */
  @Test
  public void recordOutboxBatch_doesNoWorkForAnEmptyBatch() {
    service.recordOutboxBatch(0L, 0L);

    verify(changeDAO, never()).countPending();
    verify(healthDAO, never()).recordAbandonedChanges(anyLong());
    verify(healthDAO, never()).updateDerivedStatus(anyString(), anyString(), anyLong(), nullable(String.class),
        anyLong(), nullable(Date.class));
  }

  private long capturePendingCount() {
    ArgumentCaptor<Long> pending = ArgumentCaptor.forClass(Long.class);
    verify(healthDAO).updateDerivedStatus(anyString(), anyString(), anyLong(), nullable(String.class),
        pending.capture(), nullable(Date.class));
    return pending.getValue();
  }

  private static SearchIndexHealth health(final long pendingChangeCount) {
    SearchIndexHealth health = new SearchIndexHealth();
    health.setPendingChangeCount(pendingChangeCount);
    health.setFailedChangeCount(0L);
    health.setHealthStatus(SearchIndexHealth.STATUS_HEALTHY);
    health.setRecommendedOp(SearchIndexHealth.OP_NONE);
    return health;
  }
}
