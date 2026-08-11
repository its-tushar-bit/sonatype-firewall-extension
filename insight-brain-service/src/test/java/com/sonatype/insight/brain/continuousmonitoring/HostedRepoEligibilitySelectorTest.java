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
import com.sonatype.insight.brain.dataaccess.repository.HostedRepositoryComponentDAO;
import com.sonatype.insight.brain.model.repository.HostedRepositoryComponent;
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
 * Verifies the selector forwards the cursor to the DAO, builds the next cursor from the last row of
 * the returned page, sets {@code hasMore} on saturated pages, and propagates the end-of-stream
 * {@code Page.empty()} that stops the producer.
 */
@RunWith(MockitoJUnitRunner.class)
public class HostedRepoEligibilitySelectorTest
{
  @Mock
  private HostedRepositoryComponentDAO hostedRepositoryComponentDAO;

  @Mock
  private TransactionContext tx;

  private HostedRepoEligibilitySelector underTest;

  @Before
  public void setup() {
    when(hostedRepositoryComponentDAO.createTransactionContext()).thenReturn(tx);
    underTest = new HostedRepoEligibilitySelector(hostedRepositoryComponentDAO);
  }

  /**
   * The cursor reaches the DAO unchanged. {@code cycleStart} is deliberately not forwarded: candidates
   * carry no evaluation timestamp, so there is nothing on this table to filter against it.
   */
  @Test
  public void testFetchPage_passesCursorToDao() {
    EligibilityCursor cursorIn = new EligibilityCursor(new Date(1000L), "in-id");
    HostedRepositoryComponent row = newComponent("row-id");
    when(hostedRepositoryComponentDAO.getMonitoringEligiblePage(any(TransactionContext.class), anyInt(),
        any(EligibilityCursor.class))).thenReturn(List.of(row));

    Page<HostedRepositoryComponent> page = underTest.fetchPage(cursorIn, 100, Instant.now());

    assertThat(page.rows()).containsExactly(row);
    ArgumentCaptor<EligibilityCursor> cursorCaptor = ArgumentCaptor.forClass(EligibilityCursor.class);
    verify(hostedRepositoryComponentDAO).getMonitoringEligiblePage(any(TransactionContext.class), eq(100),
        cursorCaptor.capture());
    assertThat(cursorCaptor.getValue()).isEqualTo(cursorIn);
  }

  /**
   * The keyset is the primary key alone — this table has no timestamp — so the next cursor carries the
   * last row's id and a constant filler time (EligibilityCursor requires a non-null Date).
   */
  @Test
  public void testFetchPage_nextCursorIsLastRowId() {
    when(hostedRepositoryComponentDAO.getMonitoringEligiblePage(any(), anyInt(), any()))
        .thenReturn(List.of(newComponent("id-A"), newComponent("id-B")));

    Page<HostedRepositoryComponent> page = underTest.fetchPage(null, 100, Instant.now());

    assertThat(page.nextCursor()).isEqualTo(new EligibilityCursor(new Date(0L), "id-B"));
  }

  @Test
  public void testFetchPage_hasMoreTrueWhenPageIsSaturated() {
    // size == limit ⇒ probably more rows; the next fetch returns empty if not.
    when(hostedRepositoryComponentDAO.getMonitoringEligiblePage(any(), anyInt(), any()))
        .thenReturn(List.of(newComponent("id-1"), newComponent("id-2")));

    Page<HostedRepositoryComponent> page = underTest.fetchPage(null, 2, Instant.now());

    assertThat(page.hasMore()).isTrue();
  }

  @Test
  public void testFetchPage_hasMoreFalseOnShortPage() {
    when(hostedRepositoryComponentDAO.getMonitoringEligiblePage(any(), anyInt(), any()))
        .thenReturn(List.of(newComponent("id-1")));

    Page<HostedRepositoryComponent> page = underTest.fetchPage(null, 100, Instant.now());

    assertThat(page.hasMore()).isFalse();
  }

  @Test
  public void testFetchPage_returnsEmptyPageWhenDaoReturnsEmpty() {
    when(hostedRepositoryComponentDAO.getMonitoringEligiblePage(any(), anyInt(), any()))
        .thenReturn(List.of());

    Page<HostedRepositoryComponent> page = underTest.fetchPage(null, 1000, Instant.now());

    assertThat(page.rows()).isEmpty();
    assertThat(page.nextCursor()).isNull();
    assertThat(page.hasMore()).isFalse();
  }

  private static HostedRepositoryComponent newComponent(final String id) {
    HostedRepositoryComponent c = new HostedRepositoryComponent();
    c.setId(id);
    return c;
  }
}
