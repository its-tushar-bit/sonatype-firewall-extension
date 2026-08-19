/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.violation;

import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import java.util.Date;
import java.util.List;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.model.policy.ProxyRepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.test.LogOutput;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ComponentH2Test
public class ProxyRepositoryPolicyViolationLoggerTest
    extends AbstractComponentH2Test
{
  @Mock
  private CurrentUser currentUser;

  public LogOutput logOutput = new LogOutput(AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME);

  @Inject
  private RepositoryManagerDAO repositoryManagerDAO;

  private Repository repository;

  private final Date evaluationTime = new Date();

  private PolicyViolationLogDTOAssert policyViolationLogDTOAssert;

  @BeforeEach
  public void before() {
    policyViolationLogDTOAssert = new PolicyViolationLogDTOAssert(repositoryManagerDAO);
    repository = tempEntity.newRepository();
  }

  @Test
  public void testLog() throws Exception {
    when(currentUser.getUsernameOrSystem()).thenReturn(USERNAME);

    ProxyRepositoryPolicyViolationLogger policyViolationLogger =
        new ProxyRepositoryPolicyViolationLogger(true /* licensed */, evaluationTime, repository, currentUser,
            repositoryManagerDAO);
    PolicyViolationLogEvent policyViolationLogEvent = PolicyViolationLogEvent.CREATE;
    ProxyRepositoryPolicyViolation policyViolationOne =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), evaluationTime);
    policyViolationLogger.add(policyViolationLogEvent, policyViolationOne);
    ProxyRepositoryPolicyViolation policyViolationTwo =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), evaluationTime);
    policyViolationLogger.add(policyViolationLogEvent, policyViolationTwo);

    policyViolationLogger.log();

    List<PolicyViolationLogDTO> policyViolationLogDTOs = assertPolicyViolationLogDTOs(2);
    assertPolicyViolationData(policyViolationLogDTOs.get(0), policyViolationLogEvent, policyViolationOne,
        currentUser.getUsernameOrSystem());
    assertPolicyViolationData(policyViolationLogDTOs.get(1), policyViolationLogEvent, policyViolationTwo,
        currentUser.getUsernameOrSystem());
  }

  @Test
  public void testLog_NoComponentIdentifier() throws Exception {
    when(currentUser.getUsernameOrSystem()).thenReturn(USERNAME);

    ProxyRepositoryPolicyViolationLogger policyViolationLogger =
        new ProxyRepositoryPolicyViolationLogger(true /* licensed */, evaluationTime, repository, currentUser,
            repositoryManagerDAO);
    PolicyViolationLogEvent policyViolationLogEvent = PolicyViolationLogEvent.CREATE;
    ProxyRepositoryPolicyViolation policyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), evaluationTime);
    policyViolation.setComponentIdentifier(null);
    policyViolationLogger.add(policyViolationLogEvent, policyViolation);

    policyViolationLogger.log();

    assertPolicyViolationData(assertPolicyViolationLogDTOs(1).get(0), policyViolationLogEvent, policyViolation,
        currentUser.getUsernameOrSystem());
  }

  @Test
  public void testLog_NoLogMessagesWithoutInfoEnabled() {
    when(currentUser.getUsernameOrSystem()).thenReturn(USERNAME);

    ProxyRepositoryPolicyViolationLogger policyViolationLogger =
        new ProxyRepositoryPolicyViolationLogger(true /* licensed */, evaluationTime, repository, currentUser,
            repositoryManagerDAO);
    Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory
        .getLogger(AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME);
    Level level = logger.getLevel();
    try {
      logger.setLevel(Level.OFF);
      ProxyRepositoryPolicyViolation policyViolation =
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
    ProxyRepositoryPolicyViolationLogger policyViolationLogger =
        new ProxyRepositoryPolicyViolationLogger(false /* licensed */, evaluationTime, repository, currentUser,
            repositoryManagerDAO);
    ProxyRepositoryPolicyViolation policyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), evaluationTime);
    policyViolationLogger.add(PolicyViolationLogEvent.CREATE, policyViolation);

    policyViolationLogger.log();

    assertThat(logOutput.getInfoMessages(AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME)).isEmpty();
  }

  @Test
  public void testLog_NoStagePolicyActionForCreateEventWithWaivedViolation() throws Exception {
    when(currentUser.getUsernameOrSystem()).thenReturn(USERNAME);

    ProxyRepositoryPolicyViolationLogger policyViolationLogger =
        new ProxyRepositoryPolicyViolationLogger(true, evaluationTime, repository, currentUser, repositoryManagerDAO);
    PolicyViolationLogEvent policyViolationLogEvent = PolicyViolationLogEvent.CREATE;
    ProxyRepositoryPolicyViolation policyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), evaluationTime);
    policyViolation.setWaived(true);
    policyViolationLogger.add(policyViolationLogEvent, policyViolation);

    policyViolationLogger.log();

    assertPolicyViolationData(assertPolicyViolationLogDTOs(1).get(0), policyViolationLogEvent, policyViolation,
        currentUser.getUsernameOrSystem());
  }

  @Test
  public void testLog_NoStagePolicyActionForFixEvent() throws Exception {
    when(currentUser.getUsernameOrSystem()).thenReturn(USERNAME);

    ProxyRepositoryPolicyViolationLogger policyViolationLogger =
        new ProxyRepositoryPolicyViolationLogger(true, evaluationTime, repository, currentUser, repositoryManagerDAO);
    PolicyViolationLogEvent policyViolationLogEvent = PolicyViolationLogEvent.FIX;
    ProxyRepositoryPolicyViolation policyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), evaluationTime);
    policyViolationLogger.add(policyViolationLogEvent, policyViolation);

    policyViolationLogger.log();

    assertPolicyViolationData(assertPolicyViolationLogDTOs(1).get(0), policyViolationLogEvent, policyViolation,
        currentUser.getUsernameOrSystem());
  }

  private List<PolicyViolationLogDTO> assertPolicyViolationLogDTOs(int expected) throws Exception {
    return PolicyViolationLogDTOAssert.assertPolicyViolationLogDTOs(logOutput, expected);
  }

  private void assertPolicyViolationData(
      PolicyViolationLogDTO policyViolationLogDTO,
      PolicyViolationLogEvent policyViolationLogEvent,
      ProxyRepositoryPolicyViolation policyViolation,
      String userName) throws Exception
  {
    policyViolationLogDTOAssert.assertRepositoryPolicyViolationData(policyViolationLogDTO, policyViolationLogEvent,
        repository, evaluationTime, evaluationTime, policyViolation, userName);
  }
}
