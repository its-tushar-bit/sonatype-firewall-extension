/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.violation;

import java.util.Date;
import java.util.List;

import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
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

public class RepositoryPolicyViolationLoggerTest
    extends AbstractComponentTest
{
  @Rule
  public LogOutput logOutput = new LogOutput(AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME);

  private Repository repository;

  private Date evaluationTime = new Date();

  @Before
  public void before() {
    repository = tempEntity.newRepository();
  }

  @Test
  public void testLog() throws Exception {
    RepositoryPolicyViolationLogger policyViolationLogger =
        new RepositoryPolicyViolationLogger(true /* licensed */, evaluationTime, repository);
    PolicyViolationLogEvent policyViolationLogEvent = PolicyViolationLogEvent.CREATE;
    RepositoryPolicyViolation policyViolationOne =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), evaluationTime);
    policyViolationLogger.add(policyViolationLogEvent, policyViolationOne);
    RepositoryPolicyViolation policyViolationTwo =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), evaluationTime);
    policyViolationLogger.add(policyViolationLogEvent, policyViolationTwo);

    policyViolationLogger.log();

    List<ObjectNode> policyViolationLogDTOObjectNodes = assertPolicyViolationLogDTOObjectNodes(2);
    assertPolicyViolationData(policyViolationLogDTOObjectNodes.get(0), policyViolationLogEvent, policyViolationOne);
    assertPolicyViolationData(policyViolationLogDTOObjectNodes.get(1), policyViolationLogEvent, policyViolationTwo);
  }

  @Test
  public void testLog_NoComponentIdentifier() throws Exception {
    RepositoryPolicyViolationLogger policyViolationLogger =
        new RepositoryPolicyViolationLogger(true /* licensed */, evaluationTime, repository);
    PolicyViolationLogEvent policyViolationLogEvent = PolicyViolationLogEvent.CREATE;
    RepositoryPolicyViolation policyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), evaluationTime);
    policyViolation.setComponentIdentifier(null);
    policyViolationLogger.add(policyViolationLogEvent, policyViolation);

    policyViolationLogger.log();

    assertPolicyViolationData(assertPolicyViolationLogDTOObjectNodes(1).get(0), policyViolationLogEvent,
        policyViolation);
  }

  @Test
  public void testLog_NoLogMessagesWithoutInfoEnabled() {
    RepositoryPolicyViolationLogger policyViolationLogger =
        new RepositoryPolicyViolationLogger(true /* licensed */, evaluationTime, repository);
    Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory
        .getLogger(AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME);
    Level level = logger.getLevel();
    try {
      logger.setLevel(Level.OFF);
      RepositoryPolicyViolation policyViolation =
          tempEntity.newRepositoryPolicyViolation(repository.getId(), evaluationTime);
      policyViolationLogger.add(PolicyViolationLogEvent.CREATE, policyViolation);

      policyViolationLogger.log();

      assertThat(logOutput.getInfoMessages(AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME)).isEmpty();
    }
    finally {
      logger.setLevel(level);
    }
  }

  @Test
  public void testLog_NoLogMessagesWithoutLicensedFeature() {
    RepositoryPolicyViolationLogger policyViolationLogger =
        new RepositoryPolicyViolationLogger(false /* licensed */, evaluationTime, repository);
    RepositoryPolicyViolation policyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), evaluationTime);
    policyViolationLogger.add(PolicyViolationLogEvent.CREATE, policyViolation);

    policyViolationLogger.log();

    assertThat(logOutput.getInfoMessages(AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME)).isEmpty();
  }

  @Test
  public void testLog_NoStagePolicyActionForCreateEventWithWaivedViolation() throws Exception {
    RepositoryPolicyViolationLogger policyViolationLogger =
        new RepositoryPolicyViolationLogger(true, evaluationTime, repository);
    PolicyViolationLogEvent policyViolationLogEvent = PolicyViolationLogEvent.CREATE;
    RepositoryPolicyViolation policyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), evaluationTime);
    policyViolation.setWaived(true);
    policyViolationLogger.add(policyViolationLogEvent, policyViolation);

    policyViolationLogger.log();

    assertPolicyViolationData(assertPolicyViolationLogDTOObjectNodes(1).get(0), policyViolationLogEvent,
        policyViolation);
  }

  @Test
  public void testLog_NoStagePolicyActionForFixEvent() throws Exception {
    RepositoryPolicyViolationLogger policyViolationLogger =
        new RepositoryPolicyViolationLogger(true, evaluationTime, repository);
    PolicyViolationLogEvent policyViolationLogEvent = PolicyViolationLogEvent.FIX;
    RepositoryPolicyViolation policyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), evaluationTime);
    policyViolationLogger.add(policyViolationLogEvent, policyViolation);

    policyViolationLogger.log();

    assertPolicyViolationData(assertPolicyViolationLogDTOObjectNodes(1).get(0), policyViolationLogEvent,
        policyViolation);
  }

  private List<ObjectNode> assertPolicyViolationLogDTOObjectNodes(int expected) throws Exception {
    return PolicyViolationLogDTOAssert.assertPolicyViolationLogDTOObjectNodes(logOutput, expected);
  }

  private void assertPolicyViolationData(ObjectNode policyViolationLogDTOObjectNode,
                                         PolicyViolationLogEvent policyViolationLogEvent,
                                         RepositoryPolicyViolation policyViolation) throws Exception
  {
    PolicyViolationLogDTOAssert.assertRepositoryPolicyViolationData(policyViolationLogDTOObjectNode,
        policyViolationLogEvent, repository, evaluationTime, evaluationTime, policyViolation);
  }
}
