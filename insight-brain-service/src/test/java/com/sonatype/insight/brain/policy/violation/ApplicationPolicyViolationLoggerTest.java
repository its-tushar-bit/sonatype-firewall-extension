/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.violation;

import java.util.Date;
import java.util.List;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.tenancy.TenantTestHelper;
import com.sonatype.insight.test.LogOutput;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class ApplicationPolicyViolationLoggerTest
    extends AbstractComponentTest
{
  @Mock
  private CurrentUser currentUser;

  @Rule
  public LogOutput logOutput = new LogOutput(AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME);

  private Organization organization;

  private Application application;

  private Policy policy;

  private PolicyEvaluation policyEvaluation;

  @Before
  public void before() {
    organization = tempEntity.newOrganization();
    application = tempEntity.newApplication(organization.getId());
    policy = tempEntity.newPolicy(application);
    policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scanId");

  }

  @Test
  public void testLog() throws Exception {
    when(currentUser.getUsernameOrSystem()).thenReturn(USERNAME);
    ApplicationPolicyViolationLogger policyViolationLogger =
        new ApplicationPolicyViolationLogger(true, policyEvaluation.getTime(), application, organization, currentUser);
    PolicyViolationLogEvent policyViolationLogEvent = PolicyViolationLogEvent.CREATE;
    PolicyViolation policyViolationOne = createPolicyViolation();
    policyViolationLogger.add(policyViolationLogEvent, policyViolationOne);
    PolicyViolation policyViolationTwo = createPolicyViolation();
    policyViolationLogger.add(policyViolationLogEvent, policyViolationTwo);

    policyViolationLogger.log();

    List<PolicyViolationLogDTO> policyViolationLogDTOs = assertPolicyViolationLogDTOs(2);
    assertApplicationPolicyViolationData(policyViolationLogDTOs.get(0), policyViolationLogEvent,
        policyViolationOne.getOpenTime(), policyViolationOne, currentUser.getUsernameOrSystem());
    assertApplicationPolicyViolationData(policyViolationLogDTOs.get(1), policyViolationLogEvent,
        policyViolationTwo.getOpenTime(), policyViolationTwo, currentUser.getUsernameOrSystem());
    assertThat(policyViolationLogDTOs.get(0).tenant).isNull();
    assertThat(policyViolationLogDTOs.get(1).tenant).isNull();

  }

  @Test
  public void testLog_Multitenant() throws Exception {
    try {
      TenantTestHelper.initMultiTenantMode();
      TenantTestHelper.testAsNewTenant(testName, tenant -> {
        when(currentUser.getUsernameOrSystem()).thenReturn(USERNAME);
        ApplicationPolicyViolationLogger policyViolationLogger =
            new ApplicationPolicyViolationLogger(true, policyEvaluation.getTime(), application, organization,
                currentUser);
        PolicyViolationLogEvent policyViolationLogEvent = PolicyViolationLogEvent.CREATE;
        PolicyViolation policyViolationOne = createPolicyViolation();
        policyViolationLogger.add(policyViolationLogEvent, policyViolationOne);
        PolicyViolation policyViolationTwo = createPolicyViolation();
        policyViolationLogger.add(policyViolationLogEvent, policyViolationTwo);

        policyViolationLogger.log();

        List<PolicyViolationLogDTO> policyViolationLogDTOs = assertPolicyViolationLogDTOs(2);
        assertApplicationPolicyViolationData(policyViolationLogDTOs.get(0), policyViolationLogEvent,
            policyViolationOne.getOpenTime(), policyViolationOne, currentUser.getUsernameOrSystem());
        assertApplicationPolicyViolationData(policyViolationLogDTOs.get(1), policyViolationLogEvent,
            policyViolationTwo.getOpenTime(), policyViolationTwo, currentUser.getUsernameOrSystem());
        assertThat(policyViolationLogDTOs.get(0).tenant).isEqualTo(tenant.tenantSlug);
        assertThat(policyViolationLogDTOs.get(1).tenant).isEqualTo(tenant.tenantSlug);
      });
    }
    finally {
      TenantTestHelper.resetAfterTest();
    }
  }

  @Test
  public void testLog_NoComponentIdentifier() throws Exception {
    when(currentUser.getUsernameOrSystem()).thenReturn(USERNAME);
    ApplicationPolicyViolationLogger policyViolationLogger =
        new ApplicationPolicyViolationLogger(true, policyEvaluation.getTime(), application, organization, currentUser);
    PolicyViolationLogEvent policyViolationLogEvent = PolicyViolationLogEvent.CREATE;
    PolicyViolation policyViolation = createPolicyViolation();
    policyViolation.setComponentIdentifier(null);
    policyViolationLogger.add(policyViolationLogEvent, policyViolation);

    policyViolationLogger.log();

    assertApplicationPolicyViolationData(assertPolicyViolationLogDTOs(1).get(0), policyViolationLogEvent,
        policyViolation.getOpenTime(), policyViolation, currentUser.getUsernameOrSystem());
  }

  @Test
  public void testLog_NoLogMessagesWithoutInfoEnabled() {
    ApplicationPolicyViolationLogger policyViolationLogger =
        new ApplicationPolicyViolationLogger(true, policyEvaluation.getTime(), application, organization, currentUser);
    Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory
        .getLogger(AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME);
    Level level = logger.getLevel();
    try {
      logger.setLevel(Level.OFF);
      policyViolationLogger.add(PolicyViolationLogEvent.CREATE, createPolicyViolation());

      policyViolationLogger.log();

      assertThat(logOutput.getInfoMessages(AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME)).isEmpty();
    }
    finally {
      logger.setLevel(level);
    }
  }

  @Test
  public void testLog_NoLogMessagesWithoutLicensedFeature() {
    ApplicationPolicyViolationLogger policyViolationLogger =
        new ApplicationPolicyViolationLogger(false, policyEvaluation.getTime(), application, organization, currentUser);
    policyViolationLogger.add(PolicyViolationLogEvent.CREATE, createPolicyViolation());

    policyViolationLogger.log();

    assertThat(logOutput.getInfoMessages(AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME)).isEmpty();
  }

  @Test
  public void testLog_NoStagePolicyActionForCreateEventWithLegacyViolation() throws Exception {
    when(currentUser.getUsernameOrSystem()).thenReturn(USERNAME);
    ApplicationPolicyViolationLogger policyViolationLogger =
        new ApplicationPolicyViolationLogger(true, policyEvaluation.getTime(), application, organization, currentUser);
    PolicyViolationLogEvent policyViolationLogEvent = PolicyViolationLogEvent.CREATE;
    PolicyViolation policyViolation = createPolicyViolation();
    policyViolation.setLegacyViolationTime(new Date());
    policyViolationLogger.add(policyViolationLogEvent, policyViolation);

    policyViolationLogger.log();

    assertApplicationPolicyViolationData(assertPolicyViolationLogDTOs(1).get(0), policyViolationLogEvent,
        policyViolation.getOpenTime(), policyViolation, currentUser.getUsernameOrSystem());
  }

  @Test
  public void testLog_NoStagePolicyActionForCreateEventWithWaivedViolation() throws Exception {
    when(currentUser.getUsernameOrSystem()).thenReturn(USERNAME);
    ApplicationPolicyViolationLogger policyViolationLogger =
        new ApplicationPolicyViolationLogger(true, policyEvaluation.getTime(), application, organization, currentUser);
    PolicyViolationLogEvent policyViolationLogEvent = PolicyViolationLogEvent.CREATE;
    PolicyViolation policyViolation = createPolicyViolation();
    policyViolation.setWaiveTime(new Date());
    policyViolationLogger.add(policyViolationLogEvent, policyViolation);

    policyViolationLogger.log();

    assertApplicationPolicyViolationData(assertPolicyViolationLogDTOs(1).get(0), policyViolationLogEvent,
        policyViolation.getOpenTime(), policyViolation, currentUser.getUsernameOrSystem());
  }

  @Test
  public void testLog_NoStagePolicyActionForFixEvent() throws Exception {
    Date fixTime = new Date();
    when(currentUser.getUsernameOrSystem()).thenReturn(USERNAME);
    ApplicationPolicyViolationLogger policyViolationLogger =
        new ApplicationPolicyViolationLogger(true, fixTime, application, organization, currentUser);
    PolicyViolationLogEvent policyViolationLogEvent = PolicyViolationLogEvent.FIX;
    PolicyViolation policyViolation = createPolicyViolation();
    policyViolation.setFixTime(fixTime);
    policyViolationLogger.add(policyViolationLogEvent, policyViolation);

    policyViolationLogger.log();

    assertApplicationPolicyViolationData(assertPolicyViolationLogDTOs(1).get(0), policyViolationLogEvent,
        fixTime, policyViolation, currentUser.getUsernameOrSystem());
  }

  private PolicyViolation createPolicyViolation() {
    return tempEntity.newPolicyViolation(policyEvaluation, policy, "g", "a", "v", "componentHash");
  }

  private List<PolicyViolationLogDTO> assertPolicyViolationLogDTOs(int expected) throws Exception {
    return PolicyViolationLogDTOAssert.assertPolicyViolationLogDTOs(logOutput, expected);
  }

  private void assertApplicationPolicyViolationData(
      PolicyViolationLogDTO policyViolationLogDTO,
      PolicyViolationLogEvent policyViolationLogEvent,
      Date eventTime,
      PolicyViolation policyViolation,
      String userName) throws Exception
  {
    PolicyViolationLogDTOAssert.assertApplicationPolicyViolationData(policyViolationLogDTO, policyViolationLogEvent,
        organization, application, eventTime, policyViolation, userName);
  }
}
