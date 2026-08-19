/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.successmetrics;

import java.util.Date;

import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

import org.joda.time.DateTimeUtils;
import org.joda.time.LocalDate;
import org.junit.rules.ExternalResource;

import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class SuccessMetricsTestUtils
{
  // one hour in milliseconds
  public static final long ONE_HOUR = MILLISECONDS.convert(1, HOURS);

  private final PolicyViolationDAO policyViolationDAO;

  public SuccessMetricsTestUtils(final PolicyViolationDAO policyViolationDAO) {
    this.policyViolationDAO = policyViolationDAO;
  }

  public static class FakeDateRule
      extends ExternalResource
  {
    private final long fakeDateTimestamp;

    public FakeDateRule(long fakeDateTimestamp) {
      this.fakeDateTimestamp = fakeDateTimestamp;
    }

    /**
     * Default timestamp is December 11 2017
     */
    public FakeDateRule() {
      this(1512996545000L);
    }

    public void fakeDate() {
      DateTimeUtils.setCurrentMillisFixed(fakeDateTimestamp);
    }

    @Override
    public void before() {
      fakeDate();
    }

    @Override
    public void after() {
      DateTimeUtils.setCurrentMillisSystem();
    }
  }

  /**
   * Create a PolicyViolation that is resolved one hour after its creation. It is created on the second day of
   * the previous month
   */
  public void createPolicyViolation(Application application, LocalDate today, TemporaryEntity tempEntity) {
    Date eval1Date = today.withDayOfMonth(2).minusMonths(1).toDateTimeAtStartOfDay().toDate();
    Date eval2Date = new Date(eval1Date.getTime() + ONE_HOUR);

    Policy policy = tempEntity.newPolicy(application);

    PolicyEvaluation eval1 = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "eval1", eval1Date);
    tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "eval2", eval2Date);

    // violation appears in eval1 but is resolved in eval2
    PolicyViolation violation = tempEntity.newPolicyViolation(eval1, policy);
    violation.setFixTime(eval2Date);
    policyViolationDAO.update(violation);
  }
}
