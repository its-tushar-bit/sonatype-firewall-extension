/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.continuousmonitoring;

import java.time.Instant;
import java.util.Date;
import java.util.List;

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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link HostedRepoEligibilitySelector} (CLM-40039 Section 6.1) — verifies the
 * selector forwards pagination and cycleStart to the DAO and propagates the empty page that ends
 * the producer's pagination.
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
  public void testFetchPage_delegatesPaginationAndCycleStartToDao() {
    // Use millisecond-truncated Instant so the round-trip through Date.from() preserves equality
    // (Date.from(Instant) truncates nanoseconds, breaking direct Instant comparison on systems
    // with nanosecond-precision clocks like Linux CI runners).
    Instant cycleStart = Instant.ofEpochMilli(System.currentTimeMillis());
    RepositoryComponent component = new RepositoryComponent();
    when(repositoryComponentDAO.getMonitoringEligiblePage(any(TransactionContext.class), any(Date.class), anyInt(),
        anyInt())).thenReturn(List.of(component));

    List<RepositoryComponent> page = underTest.fetchPage(50, 100, cycleStart);

    assertThat(page).containsExactly(component);
    ArgumentCaptor<Integer> limitCaptor = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<Integer> offsetCaptor = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<Date> cycleStartCaptor = ArgumentCaptor.forClass(Date.class);
    verify(repositoryComponentDAO).getMonitoringEligiblePage(any(TransactionContext.class),
        cycleStartCaptor.capture(), limitCaptor.capture(), offsetCaptor.capture());
    assertThat(limitCaptor.getValue()).isEqualTo(100);
    assertThat(offsetCaptor.getValue()).isEqualTo(50);
    assertThat(cycleStartCaptor.getValue()).isEqualTo(Date.from(cycleStart));
  }

  @Test
  public void testFetchPage_returnsEmptyWhenDaoReturnsEmpty() {
    when(repositoryComponentDAO.getMonitoringEligiblePage(any(TransactionContext.class), any(Date.class), anyInt(),
        anyInt())).thenReturn(List.of());

    assertThat(underTest.fetchPage(0, 1000, Instant.now())).isEmpty();
  }
}
