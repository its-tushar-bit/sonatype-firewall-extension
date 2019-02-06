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
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.test.LogOutput;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

public class ApplicationPolicyViolationLoggerTest
    extends AbstractComponentTest
{
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
    ApplicationPolicyViolationLogger policyViolationLogger =
        new ApplicationPolicyViolationLogger(true, policyEvaluation.getTime(), application);
    PolicyViolationLogEvent policyViolationLogEvent = PolicyViolationLogEvent.CREATE;
    PolicyViolation policyViolationOne = createPolicyViolation();
    policyViolationLogger.add(policyViolationLogEvent, policyViolationOne);
    PolicyViolation policyViolationTwo = createPolicyViolation();
    policyViolationLogger.add(policyViolationLogEvent, policyViolationTwo);

    policyViolationLogger.log();

    List<ObjectNode> policyViolationLogDTOObjectNodes = assertPolicyViolationLogDTOObjectNodes(2);
    assertApplicationPolicyViolationData(policyViolationLogDTOObjectNodes.get(0), policyViolationLogEvent,
        policyViolationOne);
    assertApplicationPolicyViolationData(policyViolationLogDTOObjectNodes.get(1), policyViolationLogEvent,
        policyViolationTwo);
  }

  @Test
  public void testLog_NoComponentIdentifier() throws Exception {
    ApplicationPolicyViolationLogger policyViolationLogger =
        new ApplicationPolicyViolationLogger(true, policyEvaluation.getTime(), application);
    PolicyViolationLogEvent policyViolationLogEvent = PolicyViolationLogEvent.CREATE;
    PolicyViolation policyViolation = createPolicyViolation();
    policyViolation.setComponentIdentifier(null);
    policyViolationLogger.add(policyViolationLogEvent, policyViolation);

    policyViolationLogger.log();

    assertApplicationPolicyViolationData(assertPolicyViolationLogDTOObjectNodes(1).get(0), policyViolationLogEvent,
        policyViolation);
  }

  @Test
  public void testLog_NoLogMessagesWithoutInfoEnabled() {
    ApplicationPolicyViolationLogger policyViolationLogger =
        new ApplicationPolicyViolationLogger(true, policyEvaluation.getTime(), application);
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
        new ApplicationPolicyViolationLogger(false, policyEvaluation.getTime(), application);
    policyViolationLogger.add(PolicyViolationLogEvent.CREATE, createPolicyViolation());

    policyViolationLogger.log();

    assertThat(logOutput.getInfoMessages(AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME)).isEmpty();
  }

  @Test
  public void testLog_NoStagePolicyActionForCreateEventWithGrandfatheredViolation() throws Exception {
    ApplicationPolicyViolationLogger policyViolationLogger =
        new ApplicationPolicyViolationLogger(true, policyEvaluation.getTime(), application);
    PolicyViolationLogEvent policyViolationLogEvent = PolicyViolationLogEvent.CREATE;
    PolicyViolation policyViolation = createPolicyViolation();
    policyViolation.setGrandfatherTime(new Date());
    policyViolationLogger.add(policyViolationLogEvent, policyViolation);

    policyViolationLogger.log();

    assertApplicationPolicyViolationData(assertPolicyViolationLogDTOObjectNodes(1).get(0), policyViolationLogEvent,
        policyViolation);
  }

  @Test
  public void testLog_NoStagePolicyActionForCreateEventWithWaivedViolation() throws Exception {
    ApplicationPolicyViolationLogger policyViolationLogger =
        new ApplicationPolicyViolationLogger(true, policyEvaluation.getTime(), application);
    PolicyViolationLogEvent policyViolationLogEvent = PolicyViolationLogEvent.CREATE;
    PolicyViolation policyViolation = createPolicyViolation();
    policyViolation.setWaiveTime(new Date());
    policyViolationLogger.add(policyViolationLogEvent, policyViolation);

    policyViolationLogger.log();

    assertApplicationPolicyViolationData(assertPolicyViolationLogDTOObjectNodes(1).get(0), policyViolationLogEvent,
        policyViolation);
  }

  @Test
  public void testLog_NoStagePolicyActionForFixEvent() throws Exception {
    Date fixTime = new Date();
    ApplicationPolicyViolationLogger policyViolationLogger =
        new ApplicationPolicyViolationLogger(true, fixTime, application);
    PolicyViolationLogEvent policyViolationLogEvent = PolicyViolationLogEvent.FIX;
    PolicyViolation policyViolation = createPolicyViolation();
    policyViolation.setFixTime(fixTime);
    policyViolationLogger.add(policyViolationLogEvent, policyViolation);

    policyViolationLogger.log();

    assertApplicationPolicyViolationData(assertPolicyViolationLogDTOObjectNodes(1).get(0), policyViolationLogEvent,
        policyViolation);
  }

  private PolicyViolation createPolicyViolation() {
    return tempEntity.newPolicyViolation(policyEvaluation, policy, "g", "a", "v", "componentHash");
  }

  private List<ObjectNode> assertPolicyViolationLogDTOObjectNodes(int expected) throws Exception {
    return PolicyViolationLogDTOAssert.assertPolicyViolationLogDTOObjectNodes(logOutput, expected);
  }

  private void assertApplicationPolicyViolationData(ObjectNode policyViolationLogDTOObjectNode,
                                                    PolicyViolationLogEvent policyViolationLogEvent,
                                                    PolicyViolation policyViolation) throws Exception
  {
    PolicyViolationLogDTOAssert
        .assertApplicationPolicyViolationData(policyViolationLogDTOObjectNode, policyViolationLogEvent, organization,
            application, policyViolation);
  }
}
