/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Date;
import java.util.List;
import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.TriggerReference;
import com.sonatype.insight.brain.api.v2.dto.ApiAutoPolicyWaiverRevocationRequestDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiAutoPolicyWaiverRevocationResponseDTO;
import com.sonatype.insight.brain.dataaccess.policy.AutoPolicyWaiverRevocationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverRevocation;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.conditions.LicenseConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityStatusConditionType;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import com.google.common.collect.Lists;
import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.Mock;

import static com.sonatype.clm.dto.model.policy.TriggerReference.Type.SECURITY_VULNERABILITY_REFID;
import static com.sonatype.insight.brain.model.policy.AutoPolicyWaiverRevocation.ComponentMatcherStrategyForRevocation.ALL_VERSIONS;
import static com.sonatype.insight.brain.model.policy.AutoPolicyWaiverRevocation.ComponentMatcherStrategyForRevocation.EXACT_COMPONENT;
import static com.sonatype.insight.brain.model.policy.AutoPolicyWaiverRevocation.ComponentMatcherStrategyForRevocation.POLICY_VIOLATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class ApiAutoPolicyWaiverRevocationServiceTest
    extends AbstractComponentTest
{
  @Mock
  private ReportService reportService;

  @Override
  public void configure(Binder binder) {
    binder.bind(ReportService.class).toInstance(reportService);
    super.configure(binder);
  }

  @Inject
  private AutoPolicyWaiverRevocationDAO autoPolicyWaiverRevocationDAO;

  @Inject
  private ApiAutoPolicyWaiverRevocationService apiAutoPolicyWaiverRevocationService;

  @Test
  public void testAddAutoPolicyWaiverRevocation_Application() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getOrganizationId());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), "stageId", "scanId", new Date());
    PolicyViolation violation = tempEntity.newPolicyViolation(eval, policy, identifier, "fake", "fake");
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());

    ApiAutoPolicyWaiverRevocationRequestDTO dto = new ApiAutoPolicyWaiverRevocationRequestDTO();
    dto.applicationPublicId = app.getPublicId();
    dto.ownerId = app.getId();
    dto.scanId = eval.getScanId();
    dto.policyViolationId = violation.getId();
    dto.autoPolicyWaiverId = waiver.getId();
    dto.matchStrategy = EXACT_COMPONENT;

    final PolicyThreats.Component component1Threats = createPolicyThreatsComponents(
        identifier,
        violation
    );

    when(reportService.getPolicyThreats(anyString(), anyString())).thenReturn(
        createPolicyThreats(Lists.newArrayList(component1Threats)));

    ApiAutoPolicyWaiverRevocationResponseDTO resultingDto = apiAutoPolicyWaiverRevocationService
        .addAutoPolicyWaiverRevocation(OwnerType.APPLICATION, app.getId(), dto);
    assertThat(resultingDto).isNotNull();
    assertThat(resultingDto.ownerId).isEqualTo(app.getId());
    assertThat(resultingDto.autoPolicyWaiverId).isEqualTo(waiver.getId());
    assertThat(resultingDto.hash).isEqualTo("fake");
    assertThat(resultingDto.scanId).isEqualTo(eval.getScanId());
    assertThat(resultingDto.componentIdentifier).isEqualTo(identifier);
    assertThat(resultingDto.componentMatchStrategy).isEqualTo(EXACT_COMPONENT);
  }

  @Test
  public void testAddAutoPolicyWaiverRevocation_Organization() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getOrganizationId());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), "stageId", "scanId", new Date());
    PolicyViolation violation = tempEntity.newPolicyViolation(eval, policy, identifier, "fake", "fake");

    final PolicyThreats.Component component1Threats = createPolicyThreatsComponents(
        identifier,
        violation
    );

    when(reportService.getPolicyThreats(anyString(), anyString())).thenReturn(
        createPolicyThreats(Lists.newArrayList(component1Threats)));

    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getOrganizationId());

    ApiAutoPolicyWaiverRevocationRequestDTO dto = new ApiAutoPolicyWaiverRevocationRequestDTO();
    dto.applicationPublicId = app.getPublicId();
    dto.ownerId = app.getOrganizationId();
    dto.scanId = eval.getScanId();
    dto.policyViolationId = violation.getId();
    dto.autoPolicyWaiverId = waiver.getId();
    dto.matchStrategy = EXACT_COMPONENT;

    ApiAutoPolicyWaiverRevocationResponseDTO resultingDto = apiAutoPolicyWaiverRevocationService
        .addAutoPolicyWaiverRevocation(OwnerType.ORGANIZATION, app.getOrganizationId(), dto);
    assertThat(resultingDto).isNotNull();
    assertThat(resultingDto.ownerId).isEqualTo(app.getOrganizationId());
    assertThat(resultingDto.autoPolicyWaiverId).isEqualTo(waiver.getId());
    assertThat(resultingDto.hash).isEqualTo("fake");
    assertThat(resultingDto.scanId).isEqualTo(eval.getScanId());
    assertThat(resultingDto.componentIdentifier).isEqualTo(identifier);
    assertThat(resultingDto.componentMatchStrategy).isEqualTo(EXACT_COMPONENT);
  }

  @Test
  public void testAddAutoPolicyWaiverRevocation_InvalidOwnerType() {
    ApiAutoPolicyWaiverRevocationRequestDTO dto = new ApiAutoPolicyWaiverRevocationRequestDTO();
    assertThatThrownBy(() ->
        apiAutoPolicyWaiverRevocationService.addAutoPolicyWaiverRevocation(OwnerType.REPOSITORY, "ownerId", dto)
    ).isInstanceOf(BadRequestException.class).hasMessage("Unknown owner type: repository");
  }

  @Test
  public void testAddAutoPolicyWaiverRevocation_InvalidAutoPolicyWaiverId() {
    Application app = tempEntity.newApplicationWithParent();

    ApiAutoPolicyWaiverRevocationRequestDTO dto = new ApiAutoPolicyWaiverRevocationRequestDTO();
    dto.applicationPublicId = app.getPublicId();
    dto.ownerId = app.getId();
    dto.scanId = "scanId";
    dto.policyViolationId = "violationId";
    dto.autoPolicyWaiverId = "fakeWaiverId";
    dto.matchStrategy = EXACT_COMPONENT;

    assertThatThrownBy(() ->
        apiAutoPolicyWaiverRevocationService.addAutoPolicyWaiverRevocation(OwnerType.APPLICATION, app.getId(), dto)
    ).isInstanceOf(BadRequestException.class)
        .hasMessage("Auto policy waiver with ID fakeWaiverId not found for application with ID " + app.getId());
  }

  @Test
  public void testAddAutoPolicyWaiverRevocation_NullScanId() {
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());

    ApiAutoPolicyWaiverRevocationRequestDTO dto = new ApiAutoPolicyWaiverRevocationRequestDTO();
    dto.applicationPublicId = app.getPublicId();
    dto.ownerId = app.getId();
    dto.scanId = null;
    dto.policyViolationId = "violationId";
    dto.autoPolicyWaiverId = waiver.getId();
    dto.matchStrategy = EXACT_COMPONENT;

    assertThatThrownBy(() ->
        apiAutoPolicyWaiverRevocationService.addAutoPolicyWaiverRevocation(OwnerType.APPLICATION, app.getId(), dto)
    ).isInstanceOf(BadRequestException.class).hasMessage("scanId is required");
  }

  @Test
  public void testAddAutoPolicyWaiverRevocation_MissingOwnerId() {
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    ApiAutoPolicyWaiverRevocationRequestDTO dto = new ApiAutoPolicyWaiverRevocationRequestDTO();
    dto.applicationPublicId = app.getPublicId();
    dto.ownerId = null;
    dto.scanId = "scanId";
    dto.policyViolationId = "violationId";
    dto.autoPolicyWaiverId = waiver.getId();
    dto.matchStrategy = EXACT_COMPONENT;
    assertThatThrownBy(() ->
        apiAutoPolicyWaiverRevocationService.addAutoPolicyWaiverRevocation(OwnerType.APPLICATION, app.getId(), dto)
    ).isInstanceOf(BadRequestException.class).hasMessage("ownerId is required");
  }

  @Test
  public void testAddAutoPolicyWaiverRevocation_WithPolicyViolationIdOnly() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getOrganizationId());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), "stageId", "scanId", new Date());
    PolicyViolation violation = tempEntity.newPolicyViolation(eval, policy, identifier, "fake", "fake");
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());

    ApiAutoPolicyWaiverRevocationRequestDTO dto = new ApiAutoPolicyWaiverRevocationRequestDTO();
    dto.applicationPublicId = app.getPublicId();
    dto.ownerId = app.getId();
    dto.scanId = eval.getScanId();
    dto.policyViolationId = violation.getId();
    dto.autoPolicyWaiverId = waiver.getId();
    dto.matchStrategy = EXACT_COMPONENT;

    final PolicyThreats.Component component1Threats = createPolicyThreatsComponents(
        identifier,
        violation
    );

    when(reportService.getPolicyThreats(anyString(), anyString())).thenReturn(
        createPolicyThreats(Lists.newArrayList(component1Threats)));

    ApiAutoPolicyWaiverRevocationResponseDTO resultingDto = apiAutoPolicyWaiverRevocationService
        .addAutoPolicyWaiverRevocation(OwnerType.APPLICATION, app.getId(), dto);

    assertThat(resultingDto).isNotNull();
    assertThat(resultingDto.ownerId).isEqualTo(app.getId());
    assertThat(resultingDto.autoPolicyWaiverId).isEqualTo(waiver.getId());
    assertThat(resultingDto.hash).isEqualTo("fake");
    assertThat(resultingDto.scanId).isEqualTo(eval.getScanId());
    assertThat(resultingDto.componentIdentifier).isEqualTo(identifier);
    assertThat(resultingDto.componentMatchStrategy).isEqualTo(EXACT_COMPONENT);
    assertThat(resultingDto.policyViolationId).isEqualTo(violation.getId());
    assertThat(resultingDto.threatLevel).isEqualTo(violation.getThreatLevel());
    assertThat(resultingDto.policyName).isEqualTo(violation.getPolicyName());
    assertThat(resultingDto.componentDisplayName).isEqualTo("g1 : a1 : jar : c1 : v1");
    assertThat(resultingDto.vulnerabilityIdentifiers).isEmpty();
  }

  @Test
  public void testDeleteAutoPolicyWaiverRevocation_Application() {
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverRevocation revocation = tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());
    apiAutoPolicyWaiverRevocationService.deleteAutoPolicyWaiverRevocation(OwnerType.APPLICATION, app.getId(),
        revocation.getId());
    assertThat(autoPolicyWaiverRevocationDAO.getByOwnerIdAndAutoPolicyWaiverId(app.getId(), waiver.getId())).isEmpty();
  }

  @Test
  public void testDeleteAutoPolicyWaiverRevocation_Organization() {
    Organization org = tempEntity.newOrganization();
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(org.getId());
    AutoPolicyWaiverRevocation revocation = tempEntity.newAutoPolicyWaiverRevocation(org.getId(), waiver.getId());
    apiAutoPolicyWaiverRevocationService.deleteAutoPolicyWaiverRevocation(OwnerType.ORGANIZATION, org.getId(),
        revocation.getId());
    assertThat(autoPolicyWaiverRevocationDAO.getByOwnerIdAndAutoPolicyWaiverId(org.getId(), waiver.getId())).isEmpty();
  }

  @Test
  public void testDeleteAutoPolicyWaiverRevocation_InvalidOwnerType() {
    assertThatThrownBy(() ->
        apiAutoPolicyWaiverRevocationService.deleteAutoPolicyWaiverRevocation(OwnerType.REPOSITORY, "ownerId",
            "revocationId")
    ).isInstanceOf(BadRequestException.class).hasMessage("Unknown owner type: repository");
  }

  @Test
  public void testDeleteAutoPolicyWaiverRevocation_InvalidOwnerId() {
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverRevocation revocation = tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());
    assertThatThrownBy(() ->
        apiAutoPolicyWaiverRevocationService.deleteAutoPolicyWaiverRevocation(OwnerType.APPLICATION, "invalid",
            revocation.getId())
    ).isInstanceOf(NotFoundException.class).hasMessage(
        "Cannot find an auto policy waiver revocation with ID " +
            revocation.getId() + " for application with ID invalid");
  }

  @Test
  public void testDeleteAutoPolicyWaiverRevocation_InvalidAutoPolicyWaiverRevocationId() {
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());
    assertThatThrownBy(() ->
        apiAutoPolicyWaiverRevocationService.deleteAutoPolicyWaiverRevocation(OwnerType.APPLICATION, app.getId(),
            "invalid")
    ).isInstanceOf(NotFoundException.class).hasMessage(
        "AutoPolicyWaiverRevocation with ID invalid does not exist."
    );
  }

  @Test
  public void testDeleteAutoPolicyWaiverRevocation_OwnerIdMismatch() {
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverRevocation revocation = tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());
    assertThatThrownBy(() ->
        apiAutoPolicyWaiverRevocationService.deleteAutoPolicyWaiverRevocation(OwnerType.APPLICATION,
            app.getOrganizationId(),
            revocation.getId())
    ).isInstanceOf(NotFoundException.class).hasMessage(
        "Cannot find an auto policy waiver revocation with ID " +
            revocation.getId() + " for application with ID " + app.getOrganizationId()
    );
  }

  @Test
  public void testAddAutoPolicyWaiverRevocation_POLICY_VIOLATION() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getOrganizationId());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), "stageId", "scanId", new Date());
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(eval, policy, identifier, "fakeHash", "fake");
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());

    ApiAutoPolicyWaiverRevocationRequestDTO dto = new ApiAutoPolicyWaiverRevocationRequestDTO();
    dto.applicationPublicId = app.getPublicId();
    dto.ownerId = app.getId();
    dto.scanId = eval.getScanId();
    dto.policyViolationId = policyViolation.getId();
    dto.autoPolicyWaiverId = waiver.getId();
    dto.matchStrategy = POLICY_VIOLATION;

    final PolicyThreats.Component component1Threats = createPolicyThreatsComponents(
        identifier,
        policyViolation
    );

    when(reportService.getPolicyThreats(anyString(), anyString())).thenReturn(
        createPolicyThreats(Lists.newArrayList(component1Threats)));

    ApiAutoPolicyWaiverRevocationResponseDTO resultingDto = apiAutoPolicyWaiverRevocationService
        .addAutoPolicyWaiverRevocation(OwnerType.APPLICATION, app.getId(), dto);

    assertThat(resultingDto).isNotNull();
    assertThat(resultingDto.ownerId).isEqualTo(app.getId());
    assertThat(resultingDto.autoPolicyWaiverId).isEqualTo(waiver.getId());
    assertThat(resultingDto.hash).isEqualTo(policyViolation.getHash());
    assertThat(resultingDto.scanId).isEqualTo(eval.getScanId());
    assertThat(resultingDto.componentIdentifier).isEqualTo(identifier);
    assertThat(resultingDto.componentMatchStrategy).isEqualTo(POLICY_VIOLATION);
    assertThat(resultingDto.policyViolationId).isEqualTo(policyViolation.getId());
    assertThat(resultingDto.threatLevel).isEqualTo(policyViolation.getThreatLevel());
    assertThat(resultingDto.policyName).isEqualTo(policy.getName());
    assertThat(resultingDto.componentDisplayName).isEqualTo("g1 : a1 : jar : c1 : v1");
    assertThat(resultingDto.vulnerabilityIdentifiers).isEmpty();
    assertThat(resultingDto.policyId).isEqualTo(policy.getId());
  }

  @Test
  public void testAddAutoPolicyWaiverRevocation_POLICY_VIOLATION_invalidOwnerType() {
    ApiAutoPolicyWaiverRevocationRequestDTO dto = new ApiAutoPolicyWaiverRevocationRequestDTO();
    assertThatThrownBy(() ->
        apiAutoPolicyWaiverRevocationService.addAutoPolicyWaiverRevocation(OwnerType.REPOSITORY, "ownerId", dto)
    ).isInstanceOf(BadRequestException.class).hasMessage("Unknown owner type: repository");
  }

  @Test
  public void testAddAutoPolicyWaiverRevocation_POLICY_VIOLATION_LongOwnerId() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getOrganizationId());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), "stageId", "scanId", new Date());
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(eval, policy, identifier, "fakeHash", "fake");
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());

    ApiAutoPolicyWaiverRevocationRequestDTO dto = new ApiAutoPolicyWaiverRevocationRequestDTO();
    dto.applicationPublicId = app.getPublicId();
    dto.ownerId = "anOwnerIdthatisfarfartoolongforthefieldthatitisneededfor";
    dto.scanId = eval.getScanId();
    dto.policyViolationId = policyViolation.getId();
    dto.autoPolicyWaiverId = waiver.getId();
    dto.matchStrategy = POLICY_VIOLATION;

    assertThatThrownBy(() ->
        apiAutoPolicyWaiverRevocationService.addAutoPolicyWaiverRevocation(OwnerType.APPLICATION, app.getId(), dto)
    ).isInstanceOf(BadRequestException.class).hasMessage("ownerId exceeds maximum length of 50 characters");
  }

  @Test
  public void testAddAutoPolicyWaiverRevocation_POLICY_VIOLATION_nullScanId() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getOrganizationId());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), "stageId", "scanId", new Date());
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(eval, policy, identifier, "fakeHash", "fake");

    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    ApiAutoPolicyWaiverRevocationRequestDTO dto = new ApiAutoPolicyWaiverRevocationRequestDTO();
    dto.applicationPublicId = app.getPublicId();
    dto.ownerId = app.getId();
    dto.scanId = null;
    dto.policyViolationId = policyViolation.getId();
    dto.autoPolicyWaiverId = waiver.getId();
    dto.matchStrategy = POLICY_VIOLATION;

    assertThatThrownBy(() ->
        apiAutoPolicyWaiverRevocationService.addAutoPolicyWaiverRevocation(OwnerType.APPLICATION, app.getId(), dto)
    ).isInstanceOf(BadRequestException.class).hasMessage("scanId is required");
  }

  @Test
  public void testAddAutoPolicyWaiverRevocation_POLICY_VIOLATION_LongScanId() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getOrganizationId());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), "stageId", "scanId", new Date());
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(eval, policy, identifier, "fakeHash", "fake");

    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    ApiAutoPolicyWaiverRevocationRequestDTO dto = new ApiAutoPolicyWaiverRevocationRequestDTO();
    dto.applicationPublicId = app.getPublicId();
    dto.ownerId = app.getId();
    dto.scanId = "ascanIdthatisfarfartoolongforthefieldthatitisneededfor";
    dto.policyViolationId = policyViolation.getId();
    dto.autoPolicyWaiverId = waiver.getId();
    dto.matchStrategy = POLICY_VIOLATION;

    assertThatThrownBy(() ->
        apiAutoPolicyWaiverRevocationService.addAutoPolicyWaiverRevocation(OwnerType.APPLICATION, app.getId(), dto)
    ).isInstanceOf(BadRequestException.class).hasMessage("scanId exceeds maximum length of 50 characters");
  }

  @Test
  public void testAddAutoPolicyWaiverRevocation_POLICY_VIOLATION_invalidScanId() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getOrganizationId());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), "stageId", "scanId", new Date());
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(eval, policy, identifier, "fakeHash", "fake");

    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    ApiAutoPolicyWaiverRevocationRequestDTO dto = new ApiAutoPolicyWaiverRevocationRequestDTO();
    dto.applicationPublicId = app.getPublicId();
    dto.ownerId = app.getId();
    dto.scanId = "invalidScanId";
    dto.policyViolationId = policyViolation.getId();
    dto.autoPolicyWaiverId = waiver.getId();
    dto.matchStrategy = POLICY_VIOLATION;

    assertThatThrownBy(() ->
        apiAutoPolicyWaiverRevocationService.addAutoPolicyWaiverRevocation(OwnerType.APPLICATION, app.getId(), dto)
    ).isInstanceOf(BadRequestException.class)
        .hasMessage("Unable to load report file for provided application public ID & scan ID");
  }

  @Test
  public void testAddAutoPolicyWaiverRevocation_POLICY_VIOLATION_nullPolicyViolationId() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getOrganizationId());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), "stageId", "scanId", new Date());
    tempEntity.newPolicyViolation(eval, policy, identifier, "fakeHash", "fake");

    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    ApiAutoPolicyWaiverRevocationRequestDTO dto = new ApiAutoPolicyWaiverRevocationRequestDTO();
    dto.applicationPublicId = app.getPublicId();
    dto.ownerId = app.getId();
    dto.scanId = eval.getScanId();
    dto.policyViolationId = null;
    dto.autoPolicyWaiverId = waiver.getId();
    dto.matchStrategy = POLICY_VIOLATION;

    assertThatThrownBy(() ->
        apiAutoPolicyWaiverRevocationService.addAutoPolicyWaiverRevocation(OwnerType.APPLICATION, app.getId(), dto)
    ).isInstanceOf(BadRequestException.class).hasMessage("policyViolationId is required");
  }

  @Test
  public void testAddAutoPolicyWaiverRevocation_POLICY_VIOLATION_LongPolicyViolationId() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getOrganizationId());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), "stageId", "scanId", new Date());
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(eval, policy, identifier, "fakeHash", "fake");

    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    ApiAutoPolicyWaiverRevocationRequestDTO dto = new ApiAutoPolicyWaiverRevocationRequestDTO();
    dto.applicationPublicId = app.getPublicId();
    dto.ownerId = app.getId();
    dto.scanId = eval.getScanId();
    dto.policyViolationId = policyViolation.getId().repeat(50);
    dto.autoPolicyWaiverId = waiver.getId();
    dto.matchStrategy = POLICY_VIOLATION;

    assertThatThrownBy(() ->
        apiAutoPolicyWaiverRevocationService.addAutoPolicyWaiverRevocation(OwnerType.APPLICATION, app.getId(), dto)
    ).isInstanceOf(BadRequestException.class).hasMessage("policyViolationId exceeds maximum length of 50 characters");
  }

  @Test
  public void testAddAutoPolicyWaiverRevocation_POLICY_VIOLATION_nullAutoPolicyWaiverId() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getOrganizationId());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), "stageId", "scanId", new Date());
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(eval, policy, identifier, "fakeHash", "fake");

    ApiAutoPolicyWaiverRevocationRequestDTO dto = new ApiAutoPolicyWaiverRevocationRequestDTO();
    dto.applicationPublicId = app.getPublicId();
    dto.ownerId = app.getId();
    dto.scanId = eval.getScanId();
    dto.policyViolationId = policyViolation.getId().repeat(50);
    dto.autoPolicyWaiverId = null;
    dto.matchStrategy = POLICY_VIOLATION;

    assertThatThrownBy(() ->
        apiAutoPolicyWaiverRevocationService.addAutoPolicyWaiverRevocation(OwnerType.APPLICATION, app.getId(), dto)
    ).isInstanceOf(BadRequestException.class).hasMessage("autoPolicyWaiverId is required");
  }

  @Test
  public void testAddAutoPolicyWaiverRevocation_POLICY_VIOLATION_LongAutoPolicyWaiverId() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getOrganizationId());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), "stageId", "scanId", new Date());
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(eval, policy, identifier, "fakeHash", "fake");

    tempEntity.newAutoPolicyWaiver(app.getId());
    ApiAutoPolicyWaiverRevocationRequestDTO dto = new ApiAutoPolicyWaiverRevocationRequestDTO();
    dto.applicationPublicId = app.getPublicId();
    dto.ownerId = app.getId();
    dto.scanId = eval.getScanId();
    dto.policyViolationId = policyViolation.getId().repeat(50);
    dto.autoPolicyWaiverId = "autoPolicyWaiverId".repeat(50);
    dto.matchStrategy = POLICY_VIOLATION;

    assertThatThrownBy(() ->
        apiAutoPolicyWaiverRevocationService.addAutoPolicyWaiverRevocation(OwnerType.APPLICATION, app.getId(), dto)
    ).isInstanceOf(BadRequestException.class).hasMessage("autoPolicyWaiverId exceeds maximum length of 50 characters");
  }

  @Test
  public void testAddAutoPolicyWaiverRevocation_POLICY_VIOLATION_InvalidAutoPolicyWaiverId() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getOrganizationId());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), "stageId", "scanId", new Date());
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(eval, policy, identifier, "fakeHash", "fake");

    ApiAutoPolicyWaiverRevocationRequestDTO dto = new ApiAutoPolicyWaiverRevocationRequestDTO();
    dto.applicationPublicId = app.getPublicId();
    dto.ownerId = app.getId();
    dto.scanId = eval.getScanId();
    dto.policyViolationId = policyViolation.getId().repeat(50);
    dto.autoPolicyWaiverId = "invalidAutoPolicyWaiverId";
    dto.matchStrategy = POLICY_VIOLATION;

    assertThatThrownBy(() ->
        apiAutoPolicyWaiverRevocationService.addAutoPolicyWaiverRevocation(OwnerType.APPLICATION, app.getId(), dto)
    ).isInstanceOf(BadRequestException.class)
        .hasMessage(
            "Auto policy waiver with ID invalidAutoPolicyWaiverId not found for application with ID " + app.getId());
  }

  @Test
  public void testAddAutoPolicyWaiverRevocation_nullComponentMatchStrategy() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getOrganizationId());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), "stageId", "scanId", new Date());
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(eval, policy, identifier, "fakeHash", "fake");

    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    ApiAutoPolicyWaiverRevocationRequestDTO dto = new ApiAutoPolicyWaiverRevocationRequestDTO();
    dto.applicationPublicId = app.getPublicId();
    dto.ownerId = app.getId();
    dto.scanId = eval.getScanId();
    dto.policyViolationId = policyViolation.getId();
    dto.autoPolicyWaiverId = waiver.getId();
    dto.matchStrategy = null;

    assertThatThrownBy(() ->
        apiAutoPolicyWaiverRevocationService.addAutoPolicyWaiverRevocation(OwnerType.APPLICATION, app.getId(), dto)
    ).isInstanceOf(BadRequestException.class).hasMessage("matchStrategy is required");
  }

  @Test
  public void testMatchCVEs_WithCVEMatch() {
    ConditionFact fact =
        new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID, 1, "Security Severity Summary",
            "Security Severity Reason", new TriggerReference(SECURITY_VULNERABILITY_REFID, "CVE-123-456"));
    final List<String> result = apiAutoPolicyWaiverRevocationService.matchCVEs(fact);

    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo("CVE-123-456");
  }

  @Test
  public void testMatchVCEs_WithSonatypeCaps() {
    ConditionFact fact =
        new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID, 1, "Security Severity Summary",
            "Security Severity Reason", new TriggerReference(SECURITY_VULNERABILITY_REFID, "SONATYPE-987-654"));
    final List<String> result = apiAutoPolicyWaiverRevocationService.matchCVEs(fact);

    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo("SONATYPE-987-654");
  }

  @Test
  public void testMatchCVEs_WithSonatypeLower() {
    ConditionFact fact =
        new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID, 1, "Security Severity Summary",
            "Security Severity Reason", new TriggerReference(SECURITY_VULNERABILITY_REFID, "sonatype-555-432"));
    final List<String> result = apiAutoPolicyWaiverRevocationService.matchCVEs(fact);

    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo("sonatype-555-432");
  }

  @Test
  public void testMatchCVEs_NoMatch() {
    ConditionFact fact =
        new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID, 1, "Security Severity Summary",
            "Security Severity Reason", new TriggerReference(SECURITY_VULNERABILITY_REFID, "NotACVE"));
    final List<String> result = apiAutoPolicyWaiverRevocationService.matchCVEs(fact);

    assertThat(result).isEmpty();
  }

  @Test
  public void testMatchCVEs_null() {
    final ConditionFact statusConditionFact =
        new ConditionFact(SecurityVulnerabilityStatusConditionType.ID, 0, "Security Status Summary",
            "Security Reason Summary", new TriggerReference(SECURITY_VULNERABILITY_REFID, null));
    statusConditionFact.setReference(null);

    final List<String> result = apiAutoPolicyWaiverRevocationService.matchCVEs(statusConditionFact);

    assertThat(result).isEmpty();
  }

  @Test
  public void testGetCveIdentifiers_WithCVEs() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getOrganizationId());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), "stageId", "scanId", new Date());
    PolicyViolation violation = tempEntity.newPolicyViolation(eval, policy, identifier, "fake", "fake");

    final ConstraintFact constraintFact1 = createConstraintFact("1", "Constraint 1",
        createSecuritySeverityConditionFact("CVE-111-111"),
        createLicenseConditionFact("Another reason"));

    final ConstraintFact constraintFact2 = createConstraintFact("2", "Constraint 2",
        createLicenseConditionFact("A reason"),
        createLicenseConditionFact("Another reason"));

    final ConstraintFact constraintFact3 = createConstraintFact("3", "Constraint 3",
        createSecurityStatusConditionFact("CVE-222-333"),
        createLicenseConditionFact("Another reason"));

    final ConstraintFact constraintFact4 = createConstraintFact("1", "Constraint 1",
        createLicenseConditionFact("A reason"),
        createLicenseConditionFact("Another reason"));

    final List<ConstraintFact> constraintFacts =
        List.of(constraintFact1, constraintFact2, constraintFact3, constraintFact4);

    violation.setConstraintFacts(constraintFacts);
    String result = apiAutoPolicyWaiverRevocationService.getCveIdentifiers(constraintFacts);
    assertThat(result).isEqualTo("CVE-111-111,CVE-222-333");
  }

  @Test
  public void testGetCveIdentifiers_WithDuplicateCVEs() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getOrganizationId());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), "stageId", "scanId", new Date());
    PolicyViolation violation = tempEntity.newPolicyViolation(eval, policy, identifier, "fake", "fake");

    final ConstraintFact constraintFact1 = createConstraintFact("1", "Constraint 1",
        createSecuritySeverityConditionFact("CVE-111-111"),
        createLicenseConditionFact("Another reason"));

    final ConstraintFact constraintFact2 = createConstraintFact("2", "Constraint 2",
        createLicenseConditionFact("A reason"),
        createLicenseConditionFact("Another reason"));

    final ConstraintFact constraintFact3 = createConstraintFact("3", "Constraint 3",
        createSecurityStatusConditionFact("CVE-222-333"),
        createLicenseConditionFact("Another reason"));

    final ConstraintFact constraintFact4 = createConstraintFact("1", "Constraint 1",
        createSecurityStatusConditionFact("CVE-222-333"),
        createLicenseConditionFact("Another reason"));

    final List<ConstraintFact> constraintFacts =
        List.of(constraintFact1, constraintFact2, constraintFact3, constraintFact4);

    violation.setConstraintFacts(constraintFacts);
    String results = apiAutoPolicyWaiverRevocationService.getCveIdentifiers(constraintFacts);
    assertThat(results).isEqualTo("CVE-111-111,CVE-222-333");
  }

  @Test
  public void testGetCveIdentifiers_NoCVEs() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getOrganizationId());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), "stageId", "scanId", new Date());
    PolicyViolation violation = tempEntity.newPolicyViolation(eval, policy, identifier, "fake", "fake");

    final ConstraintFact constraintFact1 = createConstraintFact("1", "Constraint 1",
        createLicenseConditionFact("A reason"),
        createLicenseConditionFact("Another reason"));

    final ConstraintFact constraintFact2 = createConstraintFact("2", "Constraint 2",
        createLicenseConditionFact("A reason"),
        createLicenseConditionFact("Another reason"));

    final ConstraintFact constraintFact3 = createConstraintFact("3", "Constraint 3",
        createLicenseConditionFact("License reason"),
        createLicenseConditionFact("Another reason"));

    final ConstraintFact constraintFact4 = createConstraintFact("1", "Constraint 1",
        createLicenseConditionFact("A reason"),
        createLicenseConditionFact("Another reason"));

    final List<ConstraintFact> constraintFacts =
        List.of(constraintFact1, constraintFact2, constraintFact3, constraintFact4);

    violation.setConstraintFacts(constraintFacts);
    String results = apiAutoPolicyWaiverRevocationService.getCveIdentifiers(constraintFacts);
    assertThat(results).isEmpty();
  }

  @Test
  public void testGetSecurityConditions_WithSecurityConditions() {
    final ConstraintFact constraintFact1 = createConstraintFact("1", "Constraint 1",
        createSecuritySeverityConditionFact("CVE-111-111"),
        createLicenseConditionFact("Another reason"));

    final ConstraintFact constraintFact2 = createConstraintFact("2", "Constraint 2",
        createLicenseConditionFact("A reason"),
        createLicenseConditionFact("Another reason"));

    final ConstraintFact constraintFact3 = createConstraintFact("3", "Constraint 3",
        createSecuritySeverityConditionFact("CVE-222-333"),
        createLicenseConditionFact("Another reason"));

    final ConstraintFact constraintFact4 = createConstraintFact("1", "Constraint 1",
        createLicenseConditionFact("A reason"),
        createLicenseConditionFact("Another reason"));

    final List<ConstraintFact> constraintFacts =
        List.of(constraintFact1, constraintFact2, constraintFact3, constraintFact4);
    final List<ConditionFact> result = apiAutoPolicyWaiverRevocationService.getSecurityConditions(constraintFacts);
    assertThat(result).hasSize(2);
  }

  @Test
  public void testGetSecurityConditions_NoSecurityConditions() {
    final ConstraintFact constraintFact1 = createConstraintFact("1", "Constraint 1",
        createLicenseConditionFact("A reason"),
        createLicenseConditionFact("Another reason"));

    final ConstraintFact constraintFact2 = createConstraintFact("2", "Constraint 2",
        createLicenseConditionFact("A reason"),
        createLicenseConditionFact("Another reason"));

    final ConstraintFact constraintFact3 = createConstraintFact("3", "Constraint 3",
        createLicenseConditionFact("A reason"),
        createLicenseConditionFact("Another reason"));

    final ConstraintFact constraintFact4 = createConstraintFact("1", "Constraint 1",
        createLicenseConditionFact("A reason"),
        createLicenseConditionFact("Another reason"));

    final List<ConstraintFact> constraintFacts =
        List.of(constraintFact1, constraintFact2, constraintFact3, constraintFact4);
    final List<ConditionFact> result = apiAutoPolicyWaiverRevocationService.getSecurityConditions(constraintFacts);
    assertThat(result).isEmpty();
  }

  @Test
  public void testGetSecurityConditions_Null() {
    final List<ConditionFact> result = apiAutoPolicyWaiverRevocationService.getSecurityConditions(null);
    assertThat(result).isEmpty();
  }

  @Test
  public void testGetAutoPolicyWaiverRevocations() {
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    Policy policy = tempEntity.newPolicy(app.getOrganizationId());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), "stageId", "scanId", new Date());
    tempEntity.newPolicyViolation(eval, policy, identifier, "fake", "fake");
    AutoPolicyWaiverRevocation revocation =
        tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());
    List<ApiAutoPolicyWaiverRevocationResponseDTO> result =
        apiAutoPolicyWaiverRevocationService.getAutoPolicyWaiverRevocations(OwnerType.APPLICATION, app.getId(),
            waiver.getId(), 1, 10);
    assertThat(result).hasSize(1);
    assertThat(result.get(0).autoPolicyWaiverRevocationId).isEqualTo(revocation.getId());
    assertThat(result.get(0).ownerId).isEqualTo(app.getId());
    assertThat(result.get(0).autoPolicyWaiverId).isEqualTo(waiver.getId());
  }

  @Test
  public void testGetAutoPolicyWaiverRevocations_NoRevocationsApply() {
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver waiverOne = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiver waiverTwo = tempEntity.newAutoPolicyWaiver(app.getOrganizationId());
    Policy policy = tempEntity.newPolicy(app.getOrganizationId());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), "stageId", "scanId", new Date());
    tempEntity.newPolicyViolation(eval, policy, identifier, "fake", "fake");
    tempEntity.newPolicyViolation(eval, policy, identifier, "fakeTwo", "fakeTwo");
    tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiverOne.getId());
    List<ApiAutoPolicyWaiverRevocationResponseDTO> result =
        apiAutoPolicyWaiverRevocationService.getAutoPolicyWaiverRevocations(OwnerType.ORGANIZATION,
            app.getOrganizationId(), waiverTwo.getId(), 1, 10);
    assertThat(result).isEmpty();
  }

  @Test
  public void testAddAutoPolicyWaiverRevocation_EXACT_COMPONENT_Duplicate() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getOrganizationId());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), "stageId", "scanId", new Date());
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(eval, policy, identifier, "fakeHash", "fake");

    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());

    final PolicyThreats.Component component1Threats = createPolicyThreatsComponents(
        identifier,
        policyViolation
    );

    when(reportService.getPolicyThreats(anyString(), anyString())).thenReturn(
        createPolicyThreats(Lists.newArrayList(component1Threats)));

    ApiAutoPolicyWaiverRevocationRequestDTO dto = new ApiAutoPolicyWaiverRevocationRequestDTO();
    dto.applicationPublicId = app.getPublicId();
    dto.ownerId = app.getId();
    dto.scanId = eval.getScanId();
    dto.policyViolationId = policyViolation.getId();
    dto.autoPolicyWaiverId = waiver.getId();
    dto.matchStrategy = EXACT_COMPONENT;

    apiAutoPolicyWaiverRevocationService.addAutoPolicyWaiverRevocation(OwnerType.APPLICATION, app.getId(), dto);
    assertThatThrownBy(() ->
        apiAutoPolicyWaiverRevocationService.addAutoPolicyWaiverRevocation(OwnerType.APPLICATION, app.getId(), dto)
    ).isInstanceOf(BadRequestException.class).hasMessage("Revocation already exists for this policy violation");
  }

  @Test
  public void testAddAutoPolicyWaiverRevocation_ALL_VERSIONS_Duplicate() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getOrganizationId());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), "stageId", "scanId", new Date());
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(eval, policy, identifier, "fakeHash", "fake");
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());

    final PolicyThreats.Component component1Threats = createPolicyThreatsComponents(
        identifier,
        policyViolation
    );

    when(reportService.getPolicyThreats(anyString(), anyString())).thenReturn(
        createPolicyThreats(Lists.newArrayList(component1Threats)));

    ApiAutoPolicyWaiverRevocationRequestDTO dto = new ApiAutoPolicyWaiverRevocationRequestDTO();
    dto.applicationPublicId = app.getPublicId();
    dto.ownerId = app.getId();
    dto.scanId = eval.getScanId();
    dto.policyViolationId = policyViolation.getId();
    dto.autoPolicyWaiverId = waiver.getId();
    dto.matchStrategy = ALL_VERSIONS;

    apiAutoPolicyWaiverRevocationService.addAutoPolicyWaiverRevocation(OwnerType.APPLICATION, app.getId(), dto);
    assertThatThrownBy(() ->
        apiAutoPolicyWaiverRevocationService.addAutoPolicyWaiverRevocation(OwnerType.APPLICATION, app.getId(), dto)
    ).isInstanceOf(BadRequestException.class)
        .hasMessage("Revocation already exists for this policy violation");
  }

  @Test
  public void testAddAutoPolicyWaiverRevocation_POLICY_VIOLATION_Duplicate() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getOrganizationId());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), "stageId", "scanId", new Date());
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(eval, policy, identifier, "fakeHash", "fake");

    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());

    final PolicyThreats.Component component1Threats = createPolicyThreatsComponents(
        identifier,
        policyViolation
    );

    when(reportService.getPolicyThreats(anyString(), anyString())).thenReturn(
        createPolicyThreats(Lists.newArrayList(component1Threats)));

    ApiAutoPolicyWaiverRevocationRequestDTO dto = new ApiAutoPolicyWaiverRevocationRequestDTO();
    dto.applicationPublicId = app.getPublicId();
    dto.ownerId = app.getId();
    dto.scanId = eval.getScanId();
    dto.policyViolationId = policyViolation.getId();
    dto.autoPolicyWaiverId = waiver.getId();
    dto.matchStrategy = POLICY_VIOLATION;

    apiAutoPolicyWaiverRevocationService.addAutoPolicyWaiverRevocation(OwnerType.APPLICATION, app.getId(), dto);
    assertThatThrownBy(() ->
        apiAutoPolicyWaiverRevocationService.addAutoPolicyWaiverRevocation(OwnerType.APPLICATION, app.getId(), dto)
    ).isInstanceOf(BadRequestException.class).hasMessage("Revocation already exists for this policy violation");
  }

  private ConditionFact createSecurityStatusConditionFact(final String cve) {
    return new ConditionFact(SecurityVulnerabilityStatusConditionType.ID, 0, "Security Status Summary",
        "Security Reason Summary", new TriggerReference(SECURITY_VULNERABILITY_REFID, cve));
  }

  private ConditionFact createSecuritySeverityConditionFact(final String cve) {
    return new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID, 1, "Security Severity Summary",
        "Security Severity Reason", new TriggerReference(SECURITY_VULNERABILITY_REFID, cve));
  }

  private ConditionFact createLicenseConditionFact(final String reason) {
    return new ConditionFact(LicenseConditionType.ID, 1, "License Summary", reason, null);
  }

  private ConstraintFact createConstraintFact(
      final String constraintId,
      final String constraintName,
      final ConditionFact... conditionFacts)
  {
    return new ConstraintFact(constraintId, constraintName, null, conditionFacts);
  }

  private PolicyThreats createPolicyThreats(final List<PolicyThreats.Component> components) {
    final PolicyThreats policyThreats = new PolicyThreats();
    policyThreats.aaData.addAll(components);

    return policyThreats;
  }

  private PolicyThreats.Component createPolicyThreatsComponents(
      ComponentIdentifier componentIdentifier,
      PolicyViolation violation
  )
  {
    PolicyThreats.PolicyViolation policyViolation = new PolicyThreats.PolicyViolation();
    policyViolation.policyThreatLevel = violation.getThreatLevel();
    policyViolation.policyViolationId = violation.getId();
    policyViolation.policyName = violation.getPolicyName();
    policyViolation.policyId = violation.getPolicyId();
    policyViolation.actions = null;
    policyViolation.constraints = null;
    policyViolation.policyThreatCategory = null;
    policyViolation.reachabilityStatus = null;
    policyViolation.constraintFactsJson = violation.getConstraintFactsJson();

    final PolicyThreats.Component component = new PolicyThreats.Component();
    component.hash = violation.getHash();
    component.componentIdentifier = componentIdentifier;
    component.activeViolations.add(policyViolation);
    component.allViolations.add(policyViolation);
    return component;
  }
}
