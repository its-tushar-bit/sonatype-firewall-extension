/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationBaseDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationWaiverDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentPolicyViolationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentWaiversDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiConstraintViolationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiConstraintViolationReasonDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyViolationStageDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiWaivedPolicyViolationDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.OperateStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.ReportType;
import com.sonatype.insight.brain.telemetry.ReportsTelemetry;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.google.inject.Binder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

public class ApiComponentsWithWaiversReportingServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ApiComponentsWithWaiversReportingService service;

  @Mock
  private TelemetrySender telemetrySenderMock;

  private Organization org1;

  private Organization org2;

  private Application app1;

  private Application app2;

  private Application app3;

  private Policy org1Policy;

  private Policy org2Policy;

  private PolicyEvaluation app1PolicyEvaluationBuild;

  private PolicyEvaluation app1PolicyEvaluationRelease;

  private PolicyEvaluation app2PolicyEvaluationOperate;

  private PolicyEvaluation app3PolicyEvaluationBuild;

  @Before
  public void setup() {
    org1 = tempEntity.newOrganization();
    org2 = tempEntity.newOrganization();

    app1 = tempEntity.newApplication("app1", org1.getId());
    app2 = tempEntity.newApplication("app2", org1.getId());
    app3 = tempEntity.newApplication("app3", org2.getId());

    org1Policy = tempEntity.newPolicy(org1.getId());
    org2Policy = tempEntity.newPolicy(org2.getId());

    Date date1 = new Date(System.currentTimeMillis() - 1000);
    Date date2 = new Date(System.currentTimeMillis());

    app1PolicyEvaluationBuild =
        tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "test scan app1 id (build)", date1);
    app1PolicyEvaluationRelease =
        tempEntity.newPolicyEvaluation(app1.getId(), ReleaseStageType.ID, "test scan app1 id (release)", date2);
    app2PolicyEvaluationOperate =
        tempEntity.newPolicyEvaluation(app2.getId(), OperateStageType.ID, "test scan app2 id (operate)", date2);
    app3PolicyEvaluationBuild =
        tempEntity.newPolicyEvaluation(app3.getId(), BuildStageType.ID, "test scan app3 id (build)", date2);
  }

  @Override
  public void configure(Binder binder) {
    binder.bind(TelemetrySender.class).toInstance(telemetrySenderMock);
  }

  @Test
  public void testGetComponentsWithWaivers_ValidateTelemetry() {
    service.getComponentsWithWaivers();
    assertTelemetry(ReportType.COMPONENTS_WITH_WAIVERS);
  }

  private void assertTelemetry(ReportType reportType) {
    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySenderMock).send(telemetryDataArgumentCaptor.capture());
    TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();
    Map<String, Object> expectedAttributes = new HashMap<>();
    expectedAttributes.put(ReportsTelemetry.REPORT_TYPE_ATTR, reportType.toString());

    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.REPORT_API);
    assertThat(telemetryData.getTimestamp()).isLessThanOrEqualTo(System.currentTimeMillis());
    assertThat(telemetryData.getAttributes()).isEqualTo(expectedAttributes);
  }

  @Test
  public void testGetComponentsWithWaivers_NoWaivers() {
    ApiComponentWaiversDTO result = service.getComponentsWithWaivers();
    assertThat(result.applicationWaivers).hasSize(0);
    assertThat(result.repositoryWaivers).hasSize(0);
  }

  @Test
  public void testGetComponentsWithWaivers_Applications() {
    PolicyWaiver policyWaiver1 = tempEntity.newWaiver("h1", org1Policy.getId(), app1.getId(), "Some comments here");
    PolicyWaiver policyWaiver2 = tempEntity.newWaiver("h2", org1Policy.getId(), app1.getId(), "Some comments here2");
    PolicyWaiver policyWaiver3 = tempEntity.newWaiver("h3", org1Policy.getId(), app1.getId(), "Some comments here3");
    PolicyWaiver policyWaiver4 = tempEntity.newWaiver("h4", org2Policy.getId(), app3.getId(), "Some comments here4");

    PolicyViolation waivedViolation1 = tempEntity.newWaivedPolicyViolation(app1PolicyEvaluationBuild, org1Policy,
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"), "h1", policyWaiver1);
    PolicyViolation waivedViolation2 = tempEntity.newWaivedPolicyViolation(app1PolicyEvaluationBuild, org1Policy,
        ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"), "h2", policyWaiver2);
    PolicyViolation waivedViolation3 = tempEntity.newWaivedPolicyViolation(app1PolicyEvaluationRelease, org1Policy,
        ComponentIdentifier.createMavenCoordinates("g3", "a3", "v3"), "h3", policyWaiver3);
    PolicyViolation waivedViolation4 = tempEntity.newWaivedPolicyViolation(app3PolicyEvaluationBuild, org2Policy,
        ComponentIdentifier.createMavenCoordinates("g4", "a4", "v4"), "h4", policyWaiver4);

    // add a policy violation that we should not pickup
    tempEntity.newPolicyViolation(app2PolicyEvaluationOperate, org1Policy,
        ComponentIdentifier.createMavenCoordinates("g5", "a5", "v5"), "h5");

    ApiComponentWaiversDTO result = service.getComponentsWithWaivers();
    assertThat(result.applicationWaivers).hasSize(2); // note applications are ordered by public id
    assertThat(result.repositoryWaivers).hasSize(0);
    ApiApplicationWaiverDTO applicationWaiverDTO = result.applicationWaivers.get(0);

    assertThat(applicationWaiverDTO.stages).hasSize(2);

    // first waived violation app 1 build stage
    ApiPolicyViolationStageDTO policyViolationStageDTO = applicationWaiverDTO.stages.get(0);
    assertThat(policyViolationStageDTO.stageId).isEqualTo(BuildStageType.ID);

    assertThat(policyViolationStageDTO.componentPolicyViolations).hasSize(2);

    ApiComponentPolicyViolationDTO componentPolicyViolationDTO =
        policyViolationStageDTO.componentPolicyViolations.get(0);
    assertComponentDTOV2(componentPolicyViolationDTO.component, waivedViolation1);

    assertThat(componentPolicyViolationDTO.waivedPolicyViolations).hasSize(1);
    ApiWaivedPolicyViolationDTO waivedPolicyViolationDTO = componentPolicyViolationDTO.waivedPolicyViolations.get(0);
    assertWaivedPolicyViolationDTO(waivedPolicyViolationDTO, waivedViolation1);

    assertPolicyWaiverDTO(waivedPolicyViolationDTO.policyWaiver, policyWaiver1, waivedViolation1);

    assertApplicationWaiverDTO(applicationWaiverDTO, app1);

    // second waived violation app1 build stage
    componentPolicyViolationDTO = policyViolationStageDTO.componentPolicyViolations.get(1);
    assertComponentDTOV2(componentPolicyViolationDTO.component, waivedViolation2);

    assertThat(componentPolicyViolationDTO.waivedPolicyViolations).hasSize(1);
    waivedPolicyViolationDTO = componentPolicyViolationDTO.waivedPolicyViolations.get(0);
    assertWaivedPolicyViolationDTO(waivedPolicyViolationDTO, waivedViolation2);

    assertPolicyWaiverDTO(waivedPolicyViolationDTO.policyWaiver, policyWaiver2, waivedViolation2);

    assertApplicationWaiverDTO(applicationWaiverDTO, app1);

    // third waived violation app1 release stage
    policyViolationStageDTO = applicationWaiverDTO.stages.get(1);
    assertThat(policyViolationStageDTO.stageId).isEqualTo(ReleaseStageType.ID);

    componentPolicyViolationDTO = policyViolationStageDTO.componentPolicyViolations.get(0);
    assertComponentDTOV2(componentPolicyViolationDTO.component, waivedViolation3);

    assertThat(componentPolicyViolationDTO.waivedPolicyViolations).hasSize(1);
    waivedPolicyViolationDTO = componentPolicyViolationDTO.waivedPolicyViolations.get(0);
    assertWaivedPolicyViolationDTO(waivedPolicyViolationDTO, waivedViolation3);

    assertPolicyWaiverDTO(waivedPolicyViolationDTO.policyWaiver, policyWaiver3, waivedViolation3);

    assertApplicationWaiverDTO(applicationWaiverDTO, app1);

    // fourth waived violation app3 release build
    applicationWaiverDTO = result.applicationWaivers.get(1);
    policyViolationStageDTO = applicationWaiverDTO.stages.get(0);
    assertThat(policyViolationStageDTO.stageId).isEqualTo(BuildStageType.ID);

    componentPolicyViolationDTO = policyViolationStageDTO.componentPolicyViolations.get(0);
    assertComponentDTOV2(componentPolicyViolationDTO.component, waivedViolation4);

    assertThat(componentPolicyViolationDTO.waivedPolicyViolations).hasSize(1);
    waivedPolicyViolationDTO = componentPolicyViolationDTO.waivedPolicyViolations.get(0);
    assertWaivedPolicyViolationDTO(waivedPolicyViolationDTO, waivedViolation4);

    assertPolicyWaiverDTO(waivedPolicyViolationDTO.policyWaiver, policyWaiver4, waivedViolation4);

    assertApplicationWaiverDTO(applicationWaiverDTO, app3);
  }

  private void assertComponentDTOV2(ApiComponentDTOV2 componentDTOV2, PolicyViolation policyViolation) {
    assertThat(componentDTOV2.hash).isEqualTo(policyViolation.getHash());
    assertThat(componentDTOV2.componentIdentifier.getFormat())
        .isEqualTo(policyViolation.getComponentIdentifier().getFormat());
    assertThat(componentDTOV2.componentIdentifier.getCoordinates())
        .isEqualTo(policyViolation.getComponentIdentifier().getCoordinates());
    assertThat(componentDTOV2.packageUrl)
        .isEqualTo(PackageUrlIdentifier.toPackageUrl(policyViolation.getComponentIdentifier()));
    assertThat(componentDTOV2.proprietary).isNull();
  }

  private void assertWaivedPolicyViolationDTO(
      ApiWaivedPolicyViolationDTO waivedPolicyViolationDTO,
      PolicyViolation policyViolation)
  {
    assertThat(waivedPolicyViolationDTO.policyId).isEqualTo(policyViolation.getPolicyId());
    assertThat(waivedPolicyViolationDTO.policyName).isEqualTo(policyViolation.getPolicyName());
    assertThat(waivedPolicyViolationDTO.policyViolationId).isEqualTo(policyViolation.getId());
    assertThat(waivedPolicyViolationDTO.threatLevel).isEqualTo(policyViolation.getThreatLevel());

    assertThat(waivedPolicyViolationDTO.constraintViolations).hasSize(1);
    ApiConstraintViolationDTO apiConstraintViolationDTO = waivedPolicyViolationDTO.constraintViolations.get(0);
    assertThat(apiConstraintViolationDTO.constraintId)
        .isEqualTo(policyViolation.getConstraintFacts().get(0).getConstraintId());
    assertThat(apiConstraintViolationDTO.constraintName)
        .isEqualTo(policyViolation.getConstraintFacts().get(0).getConstraintName());
    assertThat(apiConstraintViolationDTO.reasons).hasSize(1);
    ApiConstraintViolationReasonDTO apiConstraintViolationReasonDTO = apiConstraintViolationDTO.reasons.get(0);
    assertThat(apiConstraintViolationReasonDTO.reason)
        .isEqualTo(policyViolation.getConstraintFacts().get(0).getConditionFacts().get(0).getReason());
  }

  private void assertPolicyWaiverDTO(
      ApiPolicyWaiverDTO policyWaiver,
      PolicyWaiver waiver,
      PolicyViolation policyViolation)
  {
    ApiPolicyWaiverDTO policyWaiverDTO = policyWaiver;
    assertThat(policyWaiverDTO.comment).isEqualTo(waiver.getComment());
    assertThat(policyWaiverDTO.createTime).isEqualTo(policyViolation.getWaiveTime());
    assertThat(policyWaiverDTO.policyWaiverId).isEqualTo(waiver.getId());
  }

  private void assertApplicationWaiverDTO(ApiApplicationWaiverDTO actual, Application app) {
    ApiApplicationBaseDTO applicationDTO = actual.application;
    assertThat(applicationDTO.id).isEqualTo(app.getId());
    assertThat(applicationDTO.contactUserName).isNull();
    assertThat(applicationDTO.name).isEqualTo(app.getName());
    assertThat(applicationDTO.organizationId).isEqualTo(app.getOrganizationId());
    assertThat(applicationDTO.publicId).isEqualTo(app.getPublicId());
  }
}
