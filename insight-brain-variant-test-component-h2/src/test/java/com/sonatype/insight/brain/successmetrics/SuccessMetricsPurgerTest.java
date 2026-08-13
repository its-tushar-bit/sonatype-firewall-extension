/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.successmetrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.configuration.DataRetentionPolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.DataRetentionPolicy;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.repository.HostedRepositoryComponent;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.jooq.exception.DataAccessException;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.quartz.JobBuilder;
import org.quartz.JobExecutionContext;

@ComponentH2Test
public class SuccessMetricsPurgerTest
    extends AbstractComponentH2Test
{
  @Inject
  private SuccessMetricsPurger successMetricsPurger;

  @Inject
  private DataRetentionPolicyDAO dataRetentionPolicyDAO;

  @Inject
  private PolicyViolationDAO policyViolationDAO;

  @Mock
  private TaskScheduler taskSchedulerMock;

  private final ZonedDateTime now = ZonedDateTime.now();

  private Date monthsAgo(int months) {
    return Date.from(now.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS).minusMonths(months).toInstant());
  }

  private void fixViolation(PolicyViolation violation, PolicyEvaluation evaluation) {
    violation.setFixTime(evaluation.getTime());
    policyViolationDAO.update(violation);
  }

  @Test
  public void testPurgeSuccessMetrics() {
    Policy policy = tempEntity.newPolicy();

    Organization org1 = tempEntity.newOrganization();
    dataRetentionPolicyDAO.insert(
        new DataRetentionPolicy(org1.getId(), DataRetentionPolicy.CONTEXT_ID_SUCCESS_METRICS, false, null, null));
    Organization org2 = tempEntity.newOrganization();
    dataRetentionPolicyDAO
        .insert(new DataRetentionPolicy(org2.getId(), DataRetentionPolicy.CONTEXT_ID_SUCCESS_METRICS, true, null, 365));
    Organization org3 = tempEntity.newOrganization();
    dataRetentionPolicyDAO.insert(
        new DataRetentionPolicy(org3.getId(), DataRetentionPolicy.CONTEXT_ID_SUCCESS_METRICS, true, null, 365 * 2));

    Application app1 = tempEntity.newApplication(org1.getId());
    Application app2 = tempEntity.newApplication(org2.getId());
    Application app3 = tempEntity.newApplication(org3.getId());

    Map<String, PolicyViolation> namedViolations = new HashMap<>();
    for (Application app : Arrays.asList(app1, app2, app3)) {
      PolicyEvaluation evaluation1 =
          tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scan-1", monthsAgo(26));
      PolicyViolation violation1 = tempEntity.newPolicyViolation(evaluation1, policy);
      namedViolations.put(app.getId() + "-fixed1", violation1);
      namedViolations.put(app.getId() + "-open", tempEntity.newPolicyViolation(evaluation1, policy));
      namedViolations.put(app.getId() + "-grandfathered",
          tempEntity.newLegacyPolicyViolation(evaluation1, policy));
      namedViolations.put(app.getId() + "-waived",
          tempEntity.newWaivedPolicyViolation(evaluation1, policy, tempEntity.newWaiver(policy.getId(), app.getId())));

      PolicyEvaluation evaluation2 =
          tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scan-2", monthsAgo(25));
      fixViolation(violation1, evaluation2);

      PolicyEvaluation evaluation3 =
          tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_RELEASE, "scan-3", monthsAgo(25));
      PolicyViolation violation2 = tempEntity.newPolicyViolation(evaluation3, policy);
      namedViolations.put(app.getId() + "-fixed2", violation2);
      PolicyViolation violation3 = tempEntity.newPolicyViolation(evaluation3, policy);
      namedViolations.put(app.getId() + "-fixed3", violation3);

      PolicyEvaluation evaluation4 =
          tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_RELEASE, "scan-4", monthsAgo(24));
      fixViolation(violation2, evaluation4);

      PolicyEvaluation evaluation5 =
          tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_RELEASE, "scan-5", monthsAgo(12));
      fixViolation(violation3, evaluation5);
      PolicyViolation violation4 = tempEntity.newPolicyViolation(evaluation5, policy);
      namedViolations.put(app.getId() + "-fixed4", violation4);

      PolicyEvaluation evaluation6 =
          tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_RELEASE, "scan-6", monthsAgo(11));
      fixViolation(violation4, evaluation6);
    }

    successMetricsPurger.purgeSuccessMetrics();

    assertThat(policyViolationDAO.getByOwnerId(app1.getId()))
        .usingElementComparator(Comparator.comparing(PolicyViolation::getId))
        .containsExactlyInAnyOrder( //
            namedViolations.get(app1.getId() + "-open"), //
            namedViolations.get(app1.getId() + "-waived"), //
            namedViolations.get(app1.getId() + "-grandfathered"), //
            namedViolations.get(app1.getId() + "-fixed1"), //
            namedViolations.get(app1.getId() + "-fixed2"), //
            namedViolations.get(app1.getId() + "-fixed3"), //
            namedViolations.get(app1.getId() + "-fixed4"));

    assertThat(policyViolationDAO.getByOwnerId(app2.getId()))
        .usingElementComparator(Comparator.comparing(PolicyViolation::getId))
        .containsExactlyInAnyOrder( //
            namedViolations.get(app2.getId() + "-open"), //
            namedViolations.get(app2.getId() + "-waived"), //
            namedViolations.get(app2.getId() + "-grandfathered"), //
            namedViolations.get(app2.getId() + "-fixed3"), //
            namedViolations.get(app2.getId() + "-fixed4"));

    assertThat(policyViolationDAO.getByOwnerId(app3.getId()))
        .usingElementComparator(Comparator.comparing(PolicyViolation::getId))
        .containsExactlyInAnyOrder( //
            namedViolations.get(app3.getId() + "-open"), //
            namedViolations.get(app3.getId() + "-waived"), //
            namedViolations.get(app3.getId() + "-grandfathered"), //
            namedViolations.get(app3.getId() + "-fixed2"), //
            namedViolations.get(app3.getId() + "-fixed3"), //
            namedViolations.get(app3.getId() + "-fixed4"));
  }

  @Test
  public void testPurgeSuccessMetrics_HostedRepositoryComponent() {
    Policy policy = tempEntity.newPolicy();

    Repository repository = tempEntity.newRepository();
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repository);
    dataRetentionPolicyDAO.insert(
        new DataRetentionPolicy(hrc.getId(), DataRetentionPolicy.CONTEXT_ID_SUCCESS_METRICS, true, null, 365));

    PolicyEvaluation evaluation1 =
        tempEntity.newPolicyEvaluation(hrc.getId(), Stage.ID_BUILD, "hrc-scan-1", monthsAgo(26));
    PolicyViolation oldFixed = tempEntity.newPolicyViolation(evaluation1, policy);
    PolicyViolation open = tempEntity.newPolicyViolation(evaluation1, policy);
    PolicyViolation grandfathered = tempEntity.newLegacyPolicyViolation(evaluation1, policy);
    PolicyViolation waived =
        tempEntity.newWaivedPolicyViolation(evaluation1, policy, tempEntity.newWaiver(policy.getId(), hrc.getId()));

    PolicyEvaluation evaluation2 =
        tempEntity.newPolicyEvaluation(hrc.getId(), Stage.ID_BUILD, "hrc-scan-2", monthsAgo(25));
    fixViolation(oldFixed, evaluation2);

    PolicyEvaluation evaluation3 =
        tempEntity.newPolicyEvaluation(hrc.getId(), Stage.ID_RELEASE, "hrc-scan-3", monthsAgo(12));
    PolicyViolation recentFixed = tempEntity.newPolicyViolation(evaluation3, policy);

    PolicyEvaluation evaluation4 =
        tempEntity.newPolicyEvaluation(hrc.getId(), Stage.ID_RELEASE, "hrc-scan-4", monthsAgo(11));
    fixViolation(recentFixed, evaluation4);

    successMetricsPurger.purgeSuccessMetrics();

    assertThat(policyViolationDAO.getByOwnerId(hrc.getId()))
        .usingElementComparator(Comparator.comparing(PolicyViolation::getId))
        .containsExactlyInAnyOrder(open, recentFixed, waived, grandfathered);
  }

  @Test
  public void testPurgeSuccessMetrics_HostedRepositoryComponents_BoundedEnumeration() {
    Policy policy = tempEntity.newPolicy();

    applyBeanFieldOverride(SuccessMetricsPurger.class, "hrcPageSize", 2);

    Repository repository = tempEntity.newRepository();
    HostedRepositoryComponent hrc1 = tempEntity.newHostedRepositoryComponent(repository);
    HostedRepositoryComponent hrc2 = tempEntity.newHostedRepositoryComponent(repository);
    HostedRepositoryComponent hrc3 = tempEntity.newHostedRepositoryComponent(repository);

    for (HostedRepositoryComponent hrc : Arrays.asList(hrc1, hrc2, hrc3)) {
      dataRetentionPolicyDAO.insert(
          new DataRetentionPolicy(hrc.getId(), DataRetentionPolicy.CONTEXT_ID_SUCCESS_METRICS, true, null, 365));
      PolicyEvaluation evaluation1 =
          tempEntity.newPolicyEvaluation(hrc.getId(), Stage.ID_BUILD, hrc.getId() + "-scan-1", monthsAgo(26));
      PolicyViolation oldFixed = tempEntity.newPolicyViolation(evaluation1, policy);
      PolicyEvaluation evaluation2 =
          tempEntity.newPolicyEvaluation(hrc.getId(), Stage.ID_BUILD, hrc.getId() + "-scan-2", monthsAgo(25));
      fixViolation(oldFixed, evaluation2);
    }

    successMetricsPurger.purgeSuccessMetrics();

    for (HostedRepositoryComponent hrc : Arrays.asList(hrc1, hrc2, hrc3)) {
      assertThat(policyViolationDAO.getByOwnerId(hrc.getId())).isEmpty();
    }
  }

  @Test
  public void testPurgeSuccessMetrics_HostedRepositoryComponents_PageBoundary_EachOwnerVisitedExactlyOnce() {
    assertEachHostedRepositoryComponentVisitedExactlyOnce(2);
  }

  @Test
  public void testPurgeSuccessMetrics_HostedRepositoryComponents_ExactFitPage_EachOwnerVisitedExactlyOnce() {
    assertEachHostedRepositoryComponentVisitedExactlyOnce(3);
  }

  private void assertEachHostedRepositoryComponentVisitedExactlyOnce(int pageSize) {
    applyBeanFieldOverride(SuccessMetricsPurger.class, "hrcPageSize", pageSize);

    Repository repository = tempEntity.newRepository();
    HostedRepositoryComponent hrc1 = tempEntity.newHostedRepositoryComponent(repository);
    HostedRepositoryComponent hrc2 = tempEntity.newHostedRepositoryComponent(repository);
    HostedRepositoryComponent hrc3 = tempEntity.newHostedRepositoryComponent(repository);

    for (HostedRepositoryComponent hrc : Arrays.asList(hrc1, hrc2, hrc3)) {
      dataRetentionPolicyDAO.insert(
          new DataRetentionPolicy(hrc.getId(), DataRetentionPolicy.CONTEXT_ID_SUCCESS_METRICS, true, null, 365));
    }

    PolicyViolationDAO spyPolicyViolationDAO = spy(policyViolationDAO);
    applyBeanFieldOverride(SuccessMetricsPurger.class, "policyViolationDAO", spyPolicyViolationDAO);

    successMetricsPurger.purgeSuccessMetrics();

    List<String> idsInAscendingOrder = Stream.of(hrc1, hrc2, hrc3)
        .map(HostedRepositoryComponent::getId)
        .sorted()
        .toList();
    InOrder inOrder = inOrder(spyPolicyViolationDAO);
    for (String hrcId : idsInAscendingOrder) {
      inOrder.verify(spyPolicyViolationDAO).deleteFixedByOwnerIdAndDate(eq(hrcId), any(Date.class));
    }
  }

  @Test
  public void testPurgeSuccessMetrics_ApplicationAndHostedRepositoryComponentInSameRun() {
    Policy policy = tempEntity.newPolicy();

    Organization org = tempEntity.newOrganization();
    dataRetentionPolicyDAO.insert(
        new DataRetentionPolicy(org.getId(), DataRetentionPolicy.CONTEXT_ID_SUCCESS_METRICS, true, null, 365));
    Application app = tempEntity.newApplication(org.getId());
    PolicyEvaluation appEval1 =
        tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "app-scan-1", monthsAgo(26));
    PolicyViolation appOldFixed = tempEntity.newPolicyViolation(appEval1, policy);
    PolicyEvaluation appEval2 =
        tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "app-scan-2", monthsAgo(25));
    fixViolation(appOldFixed, appEval2);

    Repository repository = tempEntity.newRepository();
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repository);
    dataRetentionPolicyDAO.insert(
        new DataRetentionPolicy(hrc.getId(), DataRetentionPolicy.CONTEXT_ID_SUCCESS_METRICS, true, null, 365));
    PolicyEvaluation hrcEval1 =
        tempEntity.newPolicyEvaluation(hrc.getId(), Stage.ID_BUILD, "hrc-scan-1", monthsAgo(26));
    PolicyViolation hrcOldFixed = tempEntity.newPolicyViolation(hrcEval1, policy);
    PolicyEvaluation hrcEval2 =
        tempEntity.newPolicyEvaluation(hrc.getId(), Stage.ID_BUILD, "hrc-scan-2", monthsAgo(25));
    fixViolation(hrcOldFixed, hrcEval2);

    successMetricsPurger.purgeSuccessMetrics();

    assertThat(policyViolationDAO.getByOwnerId(app.getId())).isEmpty();
    assertThat(policyViolationDAO.getByOwnerId(hrc.getId())).isEmpty();
  }

  @Test
  public void testPurgeSuccessMetrics_NoHostedRepositoryComponents_AppPathStillPurges() {
    Policy policy = tempEntity.newPolicy();

    Organization org = tempEntity.newOrganization();
    dataRetentionPolicyDAO.insert(
        new DataRetentionPolicy(org.getId(), DataRetentionPolicy.CONTEXT_ID_SUCCESS_METRICS, true, null, 365));
    Application app = tempEntity.newApplication(org.getId());
    PolicyEvaluation evaluation1 =
        tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "app-scan-1", monthsAgo(26));
    PolicyViolation oldFixed = tempEntity.newPolicyViolation(evaluation1, policy);
    PolicyEvaluation evaluation2 =
        tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "app-scan-2", monthsAgo(25));
    fixViolation(oldFixed, evaluation2);

    successMetricsPurger.purgeSuccessMetrics();

    assertThat(policyViolationDAO.getByOwnerId(app.getId())).isEmpty();
  }

  @Test
  public void testPurgeSuccessMetrics_HostedRepositoryComponent_InheritsAncestorPolicy() {
    Policy policy = tempEntity.newPolicy();

    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager);
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repository);
    dataRetentionPolicyDAO.insert(new DataRetentionPolicy(
        repositoryManager.getId(), DataRetentionPolicy.CONTEXT_ID_SUCCESS_METRICS, true, null, 365));

    PolicyEvaluation evaluation1 =
        tempEntity.newPolicyEvaluation(hrc.getId(), Stage.ID_BUILD, "hrc-scan-1", monthsAgo(26));
    PolicyViolation oldFixed = tempEntity.newPolicyViolation(evaluation1, policy);
    PolicyViolation open = tempEntity.newPolicyViolation(evaluation1, policy);
    PolicyEvaluation evaluation2 =
        tempEntity.newPolicyEvaluation(hrc.getId(), Stage.ID_BUILD, "hrc-scan-2", monthsAgo(25));
    fixViolation(oldFixed, evaluation2);

    successMetricsPurger.purgeSuccessMetrics();

    assertThat(policyViolationDAO.getByOwnerId(hrc.getId()))
        .usingElementComparator(Comparator.comparing(PolicyViolation::getId))
        .containsExactly(open);
  }

  @Test
  public void testPurgeSuccessMetrics_HostedRepositoryComponent_DisabledPolicy_NoOp() {
    Policy policy = tempEntity.newPolicy();

    Repository repository = tempEntity.newRepository();
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repository);
    dataRetentionPolicyDAO.insert(
        new DataRetentionPolicy(hrc.getId(), DataRetentionPolicy.CONTEXT_ID_SUCCESS_METRICS, false, null, 365));

    PolicyEvaluation evaluation1 =
        tempEntity.newPolicyEvaluation(hrc.getId(), Stage.ID_BUILD, "hrc-scan-1", monthsAgo(26));
    PolicyViolation oldFixed = tempEntity.newPolicyViolation(evaluation1, policy);
    PolicyEvaluation evaluation2 =
        tempEntity.newPolicyEvaluation(hrc.getId(), Stage.ID_BUILD, "hrc-scan-2", monthsAgo(25));
    fixViolation(oldFixed, evaluation2);

    successMetricsPurger.purgeSuccessMetrics();

    assertThat(policyViolationDAO.getByOwnerId(hrc.getId()))
        .usingElementComparator(Comparator.comparing(PolicyViolation::getId))
        .containsExactly(oldFixed);
  }

  @Test
  public void testPurgeSuccessMetrics_RetryAfterLockTimeout() {
    Organization org = tempEntity.newOrganization();
    dataRetentionPolicyDAO.insert(
        new DataRetentionPolicy(org.getId(), DataRetentionPolicy.CONTEXT_ID_SUCCESS_METRICS, true, null, 365));
    Application app = tempEntity.newApplication(org.getId());
    PolicyEvaluation evaluation =
        tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scan-1", monthsAgo(26));
    tempEntity.newPolicyViolation(evaluation, tempEntity.newPolicy(app));

    PolicyViolationDAO spyPolicyViolationDAO = spy(policyViolationDAO);
    DataAccessException lockTimeout = new DataAccessException("lock timeout");
    AtomicInteger deleteCalls = new AtomicInteger();
    doAnswer(invocation -> {
      if (deleteCalls.getAndIncrement() == 0) {
        throw lockTimeout;
      }
      return invocation.callRealMethod();
    }).when(spyPolicyViolationDAO).deleteFixedByOwnerIdAndDate(eq(app.getId()), any(Date.class));
    applyBeanFieldOverride(SuccessMetricsPurger.class, "policyViolationDAO", spyPolicyViolationDAO);

    successMetricsPurger = spy(successMetricsPurger);
    doReturn(Duration.ZERO).when(successMetricsPurger).getDelayForRetry(anyInt());

    successMetricsPurger.purgeSuccessMetrics();

    assertThat(policyViolationDAO.getByOwnerId(app.getId())).hasSize(1);
    verify(successMetricsPurger).getDelayForRetry(0);
    verify(spyPolicyViolationDAO, times(2)).deleteFixedByOwnerIdAndDate(eq(app.getId()), any(Date.class));
  }

  @Test
  public void testDisallowConcurrentExecution() {
    assertThat(JobBuilder.newJob(SuccessMetricsPurger.class).build().isConcurrentExectionDisallowed()).isTrue();
  }

  @Test
  public void testExecute_QuartzJob() {
    SuccessMetricsPurger successMetricsPurgerSpy = spy(successMetricsPurger);
    JobExecutionContext mockJobExecutionContext = mock(JobExecutionContext.class);
    successMetricsPurgerSpy.execute(mockJobExecutionContext);
    verify(successMetricsPurgerSpy).purgeSuccessMetrics();
  }

  @SuppressWarnings("deprecation")
  @Test
  public void testExecute_AdminTask() throws Exception {
    SuccessMetricsPurger successMetricsPurgerSpy = spy(successMetricsPurger);
    JobExecutionContext mockJobExecutionContext = mock(JobExecutionContext.class);
    successMetricsPurgerSpy.executeForTest(mockJobExecutionContext, null);
    verify(successMetricsPurgerSpy).purgeSuccessMetrics();
    verifyNoInteractions(taskSchedulerMock);
  }
}
