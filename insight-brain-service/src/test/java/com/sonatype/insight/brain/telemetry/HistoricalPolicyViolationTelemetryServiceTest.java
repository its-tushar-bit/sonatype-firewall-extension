/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.Date;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.telemetry.HistoricalTelemetryStateDAO;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.HistoricalTelemetryService.Status;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;

public class HistoricalPolicyViolationTelemetryServiceTest
    extends AbstractComponentTest
{
  private HistoricalTelemetryStateDAO historicalTelemetryStateDAO;

  @Mock
  private TelemetrySender mockTelemetrySender;

  private PolicyViolationDAO policyViolationDAO;

  @Inject
  private TelemetryUtils telemetryUtils;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    historicalTelemetryStateDAO = daoFactory.createHistoricalTelemetryStateDAO();
    policyViolationDAO = daoFactory.createPolicyViolationDAO();
  }

  @Test
  @PostgresTest
  public void testCollectAndSendPolicyViolationTelemetry_invalidStatus() {
    var testSubject = new HistoricalPolicyViolationTelemetryService(
        historicalTelemetryStateDAO,
        policyViolationDAO,
        mockTelemetrySender,
        telemetryUtils
    );

    // given: a persisted policy violation
    var app = tempEntity.newApplication("app1", Organization.ROOT_ORGANIZATION_ID);
    var eval = tempEntity.newPolicyEvaluation(app.getId(), StageTypes.BUILD.getName(), "scan1", false, false,
        new Date());
    var component = ComponentIdentifier.createMavenCoordinates("group", "artifact", "1.0");
    var policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "test-policy");
    tempEntity.newPolicyViolation(eval, policy, component, "hash", "reason");

    // and given: historical telemetry setup for batches of 2, a violation to push telemetry for and an invalid
    //            starting status
    final var cutoffDate = new Date();
    final var batchSize = 2;
    final var minFreeMemoryMb = 0;
    var state = tempEntity.newHistoricalTelemetryState(TelemetryPurpose.HISTORICAL_POLICY_VIOLATION.name(),
        cutoffDate, batchSize, minFreeMemoryMb, Status.IN_PROGRESS.name());

    // when: we try to collect and send telemetry
    long count = testSubject.collectAndSendPolicyViolationTelemetry();

    // then: no telemetry processed
    assertThat(count).isZero();

    // when: we update the state to a valid status
    state.setStatus(Status.PENDING.name());
    historicalTelemetryStateDAO.update(state);
    count = testSubject.collectAndSendPolicyViolationTelemetry();

    // then: the telemetry was processed
    assertThat(count).isOne();
  }

  @Test
  @PostgresTest
  public void testCollectAndSendPolicyViolationTelemetry_insufficientMemory() {
    var testSubject = new HistoricalPolicyViolationTelemetryService(
        historicalTelemetryStateDAO,
        policyViolationDAO,
        mockTelemetrySender,
        telemetryUtils
    );

    // given: a persisted policy violation
    var app = tempEntity.newApplication("app1", Organization.ROOT_ORGANIZATION_ID);
    var eval = tempEntity.newPolicyEvaluation(app.getId(), StageTypes.BUILD.getName(), "scan1", false, false,
        new Date());
    var component = ComponentIdentifier.createMavenCoordinates("group", "artifact", "1.0");
    var policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "test-policy");
    tempEntity.newPolicyViolation(eval, policy, component, "hash", "reason");

    // and given: historical telemetry setup to fail on the memory check
    final var cutoffDate = new Date();
    final var batchSize = 2;
    final var minFreeMemoryMb = Integer.MAX_VALUE;

    var state = tempEntity.newHistoricalTelemetryState(TelemetryPurpose.HISTORICAL_POLICY_VIOLATION.name(),
        cutoffDate, batchSize, minFreeMemoryMb, Status.PENDING.name());

    // when: we try to collect and send telemetry
    long count = testSubject.collectAndSendPolicyViolationTelemetry();

    // then: processing was skipped
    assertThat(count).isZero();
    state = historicalTelemetryStateDAO.getById(state.getId());
    assertThat(state.getStatus()).isEqualTo(Status.SKIPPED.name());

    // when: reset status and remove the memory trigger
    state.setStatus(Status.PENDING.name());
    state.setMinFreeMemoryMb(0);
    historicalTelemetryStateDAO.update(state);
    count = testSubject.collectAndSendPolicyViolationTelemetry();

    // then: the telemetry was processed
    assertThat(count).isOne();
  }

  @Test
  public void testCollectAndSendPolicyViolationTelemetry_H2() {
    var testSubject = new HistoricalPolicyViolationTelemetryService(
        historicalTelemetryStateDAO,
        policyViolationDAO,
        mockTelemetrySender,
        telemetryUtils
    );

    // given: a persisted policy violation
    var app = tempEntity.newApplication("app1", Organization.ROOT_ORGANIZATION_ID);
    var eval = tempEntity.newPolicyEvaluation(app.getId(), StageTypes.BUILD.getName(), "scan1", false, false,
        new Date());
    var component = ComponentIdentifier.createMavenCoordinates("group", "artifact", "1.0");
    var policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "test-policy");
    tempEntity.newPolicyViolation(eval, policy, component, "hash", "reason");

    // and given: historical telemetry setup to allow processing
    final var cutoffDate = new Date();
    final var batchSize = 2;
    final var minFreeMemoryMb = 0;

    var state = tempEntity.newHistoricalTelemetryState(TelemetryPurpose.HISTORICAL_POLICY_VIOLATION.name(),
        cutoffDate, batchSize, minFreeMemoryMb, Status.PENDING.name());

    // when: we try to collect and send telemetry
    long count = testSubject.collectAndSendPolicyViolationTelemetry();

    // then: processing was skipped
    assertThat(count).isZero();
    state = historicalTelemetryStateDAO.getById(state.getId());
    assertThat(state.getStatus()).isEqualTo(Status.SKIPPED.name());
  }
}
