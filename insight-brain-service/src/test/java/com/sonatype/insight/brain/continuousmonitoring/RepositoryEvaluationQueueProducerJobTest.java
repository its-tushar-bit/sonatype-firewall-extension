/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.continuousmonitoring;

import java.time.Instant;
import java.util.List;

import com.sonatype.insight.brain.continuousmonitoring.AbstractContinuousMonitoringProducerJob.CycleResult;
import com.sonatype.insight.brain.dataaccess.continuousmonitoring.ContinuousMonitoringHostedRepoItemDAO;
import com.sonatype.insight.brain.dataaccess.continuousmonitoring.ContinuousMonitoringQueueItemDAO;
import com.sonatype.insight.brain.dataaccess.continuousmonitoring.EligibilityCursor;
import com.sonatype.insight.brain.dataaccess.continuousmonitoring.Page;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.continuousmonitoring.ContinuousMonitoringFlowType;
import com.sonatype.insight.brain.model.continuousmonitoring.ContinuousMonitoringHostedRepoItem;
import com.sonatype.insight.brain.model.continuousmonitoring.ContinuousMonitoringQueueItem;
import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RepositoryEvaluationQueueProducerJob} (CLM-40039 Section 6.1).
 * Verifies the producer wires the correct flow type/ordering, gates on the feature flag, and
 * produces parent + satellite rows aligned 1:1 with the eligibility page.
 */
@RunWith(MockitoJUnitRunner.class)
public class RepositoryEvaluationQueueProducerJobTest
{
  @Mock
  private HostedRepoEligibilitySelector eligibilitySelector;

  @Mock
  private ContinuousMonitoringQueueItemDAO queueItemDAO;

  @Mock
  private ContinuousMonitoringHostedRepoItemDAO hostedRepoItemDAO;

  @Mock
  private TransactionContext tx;

  private RepositoryEvaluationQueueProducerJob underTest;

  @BeforeClass
  public static void installFeatureFlagShim() {
    HostedRepositoryEvaluationFeatureFlagTestRule.install();
  }

  @AfterClass
  public static void uninstallFeatureFlagShim() {
    HostedRepositoryEvaluationFeatureFlagTestRule.uninstall();
  }

  @Before
  public void setup() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(true);
    when(queueItemDAO.createTransactionContext()).thenReturn(tx);
    underTest = new RepositoryEvaluationQueueProducerJob(eligibilitySelector, queueItemDAO, hostedRepoItemDAO);
  }

  @After
  public void tearDown() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(false);
  }

  @Test
  public void runCycleSkipsWhenFeatureFlagDisabled() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(false);

    CycleResult result = underTest.runCycle();

    assertThat(result.getEnqueued()).isEqualTo(0);
    assertThat(result.isSuccess()).isTrue();
    verifyNoInteractions(eligibilitySelector);
    verifyNoInteractions(queueItemDAO);
    verifyNoInteractions(hostedRepoItemDAO);
  }

  @Test
  public void testEnqueueBatchProducesAlignedParentAndSatelliteRowsAndCommits() {
    ProxyRepositoryComponent c1 = new ProxyRepositoryComponent();
    c1.setRepositoryId("repo-A");
    c1.setHash("hash-A");
    ProxyRepositoryComponent c2 = new ProxyRepositoryComponent();
    c2.setRepositoryId("repo-B");
    c2.setHash("hash-B");
    when(eligibilitySelector.fetchPage(any(), any(Integer.class), any(Instant.class)))
        .thenReturn(new Page<>(List.of(c1, c2), new EligibilityCursor(new java.util.Date(2L), "id-2"), false));

    CycleResult result = underTest.runCycle();

    assertThat(result.getEnqueued()).isEqualTo(2);
    assertThat(result.isSuccess()).isTrue();
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<ContinuousMonitoringQueueItem>> parentsCaptor = ArgumentCaptor.forClass(List.class);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<ContinuousMonitoringHostedRepoItem>> satellitesCaptor = ArgumentCaptor.forClass(List.class);
    // Producer-side orchestration: parent insert + satellite insert + orphan-parent cleanup
    // — all on the caller's tx, not a single helper call on the queue DAO.
    verify(queueItemDAO).insertBatch(any(TransactionContext.class), parentsCaptor.capture(),
        org.mockito.ArgumentMatchers.eq(false));
    verify(hostedRepoItemDAO).insertIgnoreDuplicateKey(any(TransactionContext.class), satellitesCaptor.capture());
    verify(queueItemDAO).deleteOrphanParentsForSatelliteTable(
        any(TransactionContext.class),
        anyList(),
        any(org.jooq.TableField.class));
    verify(tx).begin();
    verify(tx).commit();

    List<ContinuousMonitoringQueueItem> parents = parentsCaptor.getValue();
    List<ContinuousMonitoringHostedRepoItem> satellites = satellitesCaptor.getValue();
    assertThat(parents).hasSize(2);
    assertThat(satellites).hasSize(2);

    assertThat(parents.get(0).getFlowType()).isEqualTo(ContinuousMonitoringFlowType.HOSTED_REPO);
    assertThat(parents.get(1).getFlowType()).isEqualTo(ContinuousMonitoringFlowType.HOSTED_REPO);

    // FIFO ordering: every parent row carries the model's DEFAULT_PRIORITY (the consumer orders
    // strictly by create_time ASC; the priority column is vestigial but NOT NULL).
    assertThat(parents.get(0).getPriority()).isEqualTo(ContinuousMonitoringQueueItem.DEFAULT_PRIORITY);
    assertThat(parents.get(1).getPriority()).isEqualTo(ContinuousMonitoringQueueItem.DEFAULT_PRIORITY);

    // Satellite IDs match parent IDs and carry the natural key forward
    assertThat(satellites.get(0).getQueueId()).isEqualTo(parents.get(0).getId());
    assertThat(satellites.get(0).getRepositoryId()).isEqualTo("repo-A");
    assertThat(satellites.get(0).getComponentHash()).isEqualTo("hash-A");
    assertThat(satellites.get(1).getQueueId()).isEqualTo(parents.get(1).getId());
    assertThat(satellites.get(1).getRepositoryId()).isEqualTo("repo-B");
    assertThat(satellites.get(1).getComponentHash()).isEqualTo("hash-B");
  }

  @Test
  public void runCycleEmitsEmptyWhenSelectorReturnsEmpty() {
    when(eligibilitySelector.fetchPage(any(), any(Integer.class), any(Instant.class)))
        .thenReturn(Page.empty());

    CycleResult result = underTest.runCycle();

    assertThat(result.getEnqueued()).isEqualTo(0);
    assertThat(result.isSuccess()).isTrue();
    // No batch was emitted, so the orchestration sequence must not have fired on either DAO.
    verify(queueItemDAO, org.mockito.Mockito.never())
        .insertBatch(any(TransactionContext.class), anyList(), org.mockito.ArgumentMatchers.anyBoolean());
    verify(hostedRepoItemDAO, org.mockito.Mockito.never())
        .insertIgnoreDuplicateKey(any(TransactionContext.class), anyList());
  }

}
