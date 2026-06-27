/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.continuousmonitoring;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.continuousmonitoring.EligibilityCursor;
import com.sonatype.insight.brain.dataaccess.continuousmonitoring.Page;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link HostedRepoEligibilitySelector} (CLM-40039 §6.1, CLM-41005 keyset).
 * Verifies the selector forwards the cursor + cycleStart to the DAO, builds the next cursor from
 * the last row of the returned page, sets {@code hasMore} on saturated pages, and propagates the
 * end-of-stream {@code Page.empty()} that stops the producer.
 */
@RunWith(MockitoJUnitRunner.class)
public class HostedRepoEligibilitySelectorTest
{
  @Mock
  private RepositoryComponentDAO repositoryComponentDAO;

  @Mock
  private TransactionContext tx;

  private HostedRepoEligibilitySelector underTest;

  @Before
  public void setup() {
    when(repositoryComponentDAO.createTransactionContext()).thenReturn(tx);
    underTest = new HostedRepoEligibilitySelector(repositoryComponentDAO);
  }

  @Test
  public void testFetchPage_passesCursorAndCycleStartToDao() {
    // Millisecond-truncated Instant so the round-trip through Date.from() preserves equality
    // (Date.from(Instant) truncates nanoseconds — breaks Date.equals on nanosecond clocks).
    Instant cycleStart = Instant.ofEpochMilli(System.currentTimeMillis());
    EligibilityCursor cursorIn = new EligibilityCursor(new Date(1000L), "in-id");
    RepositoryComponent row = newComponent("row-id", new Date(2000L));
    when(repositoryComponentDAO.getMonitoringEligiblePage(any(TransactionContext.class), any(Date.class), anyInt(),
        any(EligibilityCursor.class))).thenReturn(List.of(row));

    Page<RepositoryComponent> page = underTest.fetchPage(cursorIn, 100, cycleStart);

    assertThat(page.rows()).containsExactly(row);
    ArgumentCaptor<EligibilityCursor> cursorCaptor = ArgumentCaptor.forClass(EligibilityCursor.class);
    ArgumentCaptor<Date> cycleStartCaptor = ArgumentCaptor.forClass(Date.class);
    verify(repositoryComponentDAO).getMonitoringEligiblePage(any(TransactionContext.class),
        cycleStartCaptor.capture(), eq(100), cursorCaptor.capture());
    assertThat(cursorCaptor.getValue()).isEqualTo(cursorIn);
    assertThat(cycleStartCaptor.getValue()).isEqualTo(Date.from(cycleStart));
  }

  @Test
  public void testFetchPage_nextCursorIsLastRowKey() {
    RepositoryComponent first = newComponent("id-A", new Date(3000L));
    RepositoryComponent last = newComponent("id-B", new Date(2000L));
    when(repositoryComponentDAO.getMonitoringEligiblePage(any(), any(), anyInt(), any()))
        .thenReturn(List.of(first, last));

    Page<RepositoryComponent> page = underTest.fetchPage(null, 100, Instant.now());

    assertThat(page.nextCursor())
        .isEqualTo(new EligibilityCursor(new Date(2000L), "id-B"));
  }

  @Test
  public void testFetchPage_hasMoreTrueWhenPageIsSaturated() {
    // size == limit ⇒ probably more rows; the next fetch returns empty if not.
    when(repositoryComponentDAO.getMonitoringEligiblePage(any(), any(), anyInt(), any()))
        .thenReturn(List.of(newComponent("id-1", new Date(1L)), newComponent("id-2", new Date(2L))));

    Page<RepositoryComponent> page = underTest.fetchPage(null, 2, Instant.now());

    assertThat(page.hasMore()).isTrue();
  }

  @Test
  public void testFetchPage_hasMoreFalseOnShortPage() {
    when(repositoryComponentDAO.getMonitoringEligiblePage(any(), any(), anyInt(), any()))
        .thenReturn(List.of(newComponent("id-1", new Date(1L))));

    Page<RepositoryComponent> page = underTest.fetchPage(null, 100, Instant.now());

    assertThat(page.hasMore()).isFalse();
  }

  @Test
  public void testFetchPage_returnsEmptyPageWhenDaoReturnsEmpty() {
    when(repositoryComponentDAO.getMonitoringEligiblePage(any(), any(), anyInt(), any()))
        .thenReturn(List.of());

    Page<RepositoryComponent> page = underTest.fetchPage(null, 1000, Instant.now());

    assertThat(page.rows()).isEmpty();
    assertThat(page.nextCursor()).isNull();
    assertThat(page.hasMore()).isFalse();
  }

  private static RepositoryComponent newComponent(final String id, final Date time) {
    RepositoryComponent c = new RepositoryComponent();
    c.setId(id);
    c.setTime(time);
    return c;
  }
}
