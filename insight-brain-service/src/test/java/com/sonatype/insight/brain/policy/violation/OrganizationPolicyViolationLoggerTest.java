/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.violation;

import java.util.Date;
import java.util.List;

import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.test.LogOutput;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

public class OrganizationPolicyViolationLoggerTest
    extends AbstractComponentTest
{
  @Rule
  public LogOutput logOutput = new LogOutput(AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME);

  @Test
  public void testLog() throws Exception {
    Date time = new Date();
    Organization organization = tempEntity.newOrganization();
    OrganizationPolicyViolationLogger organizationPolicyViolationLogger = new OrganizationPolicyViolationLogger(true,
        time, organization);
    PolicyViolationLogEvent policyViolationLogEvent = PolicyViolationLogEvent.CLEAR;
    organizationPolicyViolationLogger.add(policyViolationLogEvent, null);

    organizationPolicyViolationLogger.log();

    List<PolicyViolationLogDTO> policyViolationLogDTOs = PolicyViolationLogDTOAssert
        .assertPolicyViolationLogDTOs(logOutput, 1);
    PolicyViolationLogDTOAssert
        .assertOrganizationPolicyViolationData(policyViolationLogDTOs.get(0), policyViolationLogEvent, organization,
            time, time);
  }

  @Test
  public void testLog_NoLogMessagesWithoutInfoEnabled() {
    OrganizationPolicyViolationLogger organizationPolicyViolationLogger = new OrganizationPolicyViolationLogger(true,
        new Date(), tempEntity.newOrganization());
    Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory
        .getLogger(AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME);
    Level level = logger.getLevel();
    try {
      logger.setLevel(Level.OFF);
      organizationPolicyViolationLogger.add(PolicyViolationLogEvent.CLEAR, null);

      organizationPolicyViolationLogger.log();

      assertThat(logOutput.getInfoMessages(AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME)).isEmpty();
    }
    finally {
      logger.setLevel(level);
    }
  }

  @Test
  public void testLog_NoLogMessagesWithoutLicensedFeature() {
    OrganizationPolicyViolationLogger organizationPolicyViolationLogger = new OrganizationPolicyViolationLogger(false,
        new Date(), tempEntity.newOrganization());
    organizationPolicyViolationLogger.add(PolicyViolationLogEvent.CLEAR, null);

    organizationPolicyViolationLogger.log();

    assertThat(logOutput.getInfoMessages(AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME)).isEmpty();
  }
}
