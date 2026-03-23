/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.successmetrics;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.dataaccess.configuration.DataRetentionPolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.H2DiskTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.DataRetentionPolicy;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.dataaccess.TransactionContext;

import com.google.inject.Binder;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.quartz.JobBuilder;
import org.quartz.JobExecutionContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class SuccessMetricsPurgerTest
    extends AbstractComponentTest
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

  @Override
  public void configure(Binder binder) {
    binder.bind(TaskScheduler.class).toInstance(taskSchedulerMock);
    super.configure(binder);
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

    assertThat(policyViolationDAO.getByApplicationId(app1.getId()))
        .usingElementComparator(Comparator.comparing(PolicyViolation::getId))
        .containsExactlyInAnyOrder( //
            namedViolations.get(app1.getId() + "-open"), //
            namedViolations.get(app1.getId() + "-waived"), //
            namedViolations.get(app1.getId() + "-grandfathered"), //
            namedViolations.get(app1.getId() + "-fixed1"), //
            namedViolations.get(app1.getId() + "-fixed2"), //
            namedViolations.get(app1.getId() + "-fixed3"), //
            namedViolations.get(app1.getId() + "-fixed4"));

    assertThat(policyViolationDAO.getByApplicationId(app2.getId()))
        .usingElementComparator(Comparator.comparing(PolicyViolation::getId))
        .containsExactlyInAnyOrder( //
            namedViolations.get(app2.getId() + "-open"), //
            namedViolations.get(app2.getId() + "-waived"), //
            namedViolations.get(app2.getId() + "-grandfathered"), //
            namedViolations.get(app2.getId() + "-fixed3"), //
            namedViolations.get(app2.getId() + "-fixed4"));

    assertThat(policyViolationDAO.getByApplicationId(app3.getId()))
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
  @H2DiskTest(customSettings = "DATABASE_TO_UPPER=FALSE;LOCK_TIMEOUT=50;MV_STORE=FALSE")
  @Category(SlowTest.class)
  public void testPurgeSuccessMetrics_RetryAfterLockTimeout() throws Exception {
    Organization org = tempEntity.newOrganization();
    dataRetentionPolicyDAO.insert(
        new DataRetentionPolicy(org.getId(), DataRetentionPolicy.CONTEXT_ID_SUCCESS_METRICS, true, null, 365));
    Application app = tempEntity.newApplication(org.getId());
    PolicyEvaluation evaluation =
        tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scan-1", monthsAgo(26));
    tempEntity.newPolicyViolation(evaluation, tempEntity.newPolicy(app));

    CountDownLatch latchLocked = new CountDownLatch(1);
    CountDownLatch latchUnlock = new CountDownLatch(1);
    AtomicReference<Exception> error = new AtomicReference<>();
    Thread thread = new Thread(() -> {
      try (TransactionContext tx = policyViolationDAO.createTransactionContext()) {
        tx.begin();
        tx.dsl().execute("SELECT * FROM policy_violation FOR UPDATE");
        latchLocked.countDown();
        latchUnlock.await(10, TimeUnit.SECONDS);
        tx.commit();
      }
      catch (Exception e) {
        error.set(e);
      }
    });
    thread.start();

    successMetricsPurger = spy(successMetricsPurger);
    doAnswer(invocation -> {
      int retry = invocation.getArgument(0);
      if (retry > 0) {
        latchUnlock.countDown();
      }
      return invocation.callRealMethod();
    }).when(successMetricsPurger).getDelayForRetry(anyInt());

    assertThat(latchLocked.await(10, TimeUnit.SECONDS)).isTrue();
    successMetricsPurger.purgeSuccessMetrics();
    verify(successMetricsPurger).getDelayForRetry(1);
    assertThat(error).hasValue(null);
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

  @Test
  public void testExecute_AdminTask() {
    successMetricsPurger.execute(null, new PrintWriter(new StringWriter()));
    verify(taskSchedulerMock).triggerTaskNow(successMetricsPurger, null);
  }
}
