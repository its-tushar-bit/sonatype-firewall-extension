/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.google.inject.Binder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

public class ApiPolicyWaiverServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ApiPolicyWaiverService apiPolicyWaiverService;

  private Policy policy;

  private PolicyViolation policyViolation;

  private Application app;

  private Organization org;

  @Mock
  private TelemetrySender telemetrySenderMock;

  @Override
  public void configure(Binder binder) {
    binder.bind(TelemetrySender.class).toInstance(telemetrySenderMock);
  }

  @Before
  public void setUpPolicyViolation() {
    org = tempEntity.newOrganization();
    app = tempEntity.newApplication(org.getId());
    policy = tempEntity.newPolicy(org.getId());

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId1App1");
    policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy, "g1", "a1", "v1", "h1", "r1");
  }

  @Test
  public void testAddPolicyWaiver_Application() {
    apiPolicyWaiverService.addPolicyWaiver(policyViolation.getId(), OwnerType.APPLICATION, "waiver comment");
    assertPolicyWaiver(app.getId(), "waiver comment");
    assertTelemetry(OwnerType.APPLICATION, app.getId());
  }

  @Test
  public void testAddPolicyWaiver_Organization() {
    apiPolicyWaiverService.addPolicyWaiver(policyViolation.getId(), OwnerType.ORGANIZATION, "waiver comment");
    assertPolicyWaiver(org.getId(), "waiver comment");
    assertTelemetry(OwnerType.ORGANIZATION, org.getId());
  }

  @Test
  public void testAddPolicyWaiver_AcceptsNoComment() {
    apiPolicyWaiverService.addPolicyWaiver(policyViolation.getId(), OwnerType.APPLICATION, null);
    assertPolicyWaiver(app.getId(), null);
  }

  @Test
  public void testAddPolicyWaiver_InvalidPolicyViolationId() {
    assertThatThrownBy(() ->
        apiPolicyWaiverService.addPolicyWaiver("invalid-policyViolationId", OwnerType.APPLICATION, "waiver comment")
    ).isInstanceOf(NotFoundException.class)
        .hasMessage("Could not find policy violation with ID invalid-policyViolationId.");
  }

  @Test
  public void testAddPolicyWaiver_InvalidOwnerType() {
    assertThatThrownBy(() ->
        apiPolicyWaiverService.addPolicyWaiver(policyViolation.getId(), OwnerType.REPOSITORY, "waiver comment")
    ).isInstanceOf(IllegalStateException.class)
        .hasMessage("Unknown owner type: repository");
  }

  private void assertPolicyWaiver(String ownerId, String comment) {
    List<PolicyWaiver> policyWaivers = new PolicyWaiverDAO().getByOwnerId(ownerId);
    assertThat(policyWaivers).hasSize(1);
    PolicyWaiver policyWaiver = policyWaivers.get(0);
    assertThat(policyWaiver).isNotNull();
    assertThat(policyWaiver.getId()).isNotNull();
    assertThat(policyWaiver.getOwnerId()).isEqualTo(ownerId);
    assertThat(policyWaiver.getHash()).isEqualTo(policyViolation.getHash());
    assertThat(policyWaiver.getComment()).isEqualTo(comment);
    assertThat(policyWaiver.getPolicyId()).isEqualTo(policy.getId());
    assertThat(policyWaiver.getCreateTime()).isNotNull();
    assertThat(policyWaiver.getConstraintFactsJson()).isEqualTo(policyViolation.getConstraintFactsJson());
  }

  private void assertTelemetry(final OwnerType ownerType,
                               final String ownerId)
  {
    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySenderMock).send(telemetryDataArgumentCaptor.capture());
    TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();
    Map<String, Object> expectedAttributes = new HashMap<>();
    expectedAttributes.put("owner_type", ownerType.toString());
    expectedAttributes.put("owner_id", HdsClientAnalytics.obfuscate(ownerId));

    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.POLICY_WAIVER_API);
    assertThat(telemetryData.getTimestamp()).isLessThanOrEqualTo(System.currentTimeMillis());
    assertThat(telemetryData.getAttributes()).isEqualTo(expectedAttributes);
  }
}
