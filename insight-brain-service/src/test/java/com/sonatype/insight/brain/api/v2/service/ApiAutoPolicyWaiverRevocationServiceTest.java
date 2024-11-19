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
import com.sonatype.insight.brain.api.v2.ApiAutoPolicyWaiverRevocationAdapter;
import com.sonatype.insight.brain.api.v2.dto.ApiAutoPolicyWaiverRevocationDTO;
import com.sonatype.insight.brain.dataaccess.policy.AutoPolicyWaiverRevocationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverRevocation;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverRevocation.ComponentMatcherStrategyForRevocation;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.conditions.LicenseConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityStatusConditionType;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import static com.sonatype.clm.dto.model.policy.TriggerReference.Type.SECURITY_VULNERABILITY_REFID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class ApiAutoPolicyWaiverRevocationServiceTest
    extends AbstractComponentTest
{
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
    tempEntity.newPolicyViolation(eval, policy, identifier, "fake", "fake");
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverRevocation revocation = new AutoPolicyWaiverRevocation(
        app.getId(),
        "fakeCreatorName",
        "fakeCreatorId",
        new Date(),
        waiver.getId(),
        "scanId",
        "fakeHash",
        "fakeAssociatedPackageUrl",
        ComponentMatcherStrategyForRevocation.EXACT_COMPONENT
    );
    ApiAutoPolicyWaiverRevocationDTO dto = ApiAutoPolicyWaiverRevocationAdapter.convertToDTO(revocation);
    ApiAutoPolicyWaiverRevocationDTO resultingDto = apiAutoPolicyWaiverRevocationService
        .addAutoPolicyWaiverRevocation(OwnerType.APPLICATION, app.getId(), dto);
    assertThat(resultingDto).isNotNull();
    assertThat(resultingDto.ownerId).isEqualTo(revocation.getOwnerId());
    assertThat(resultingDto.autoPolicyWaiverId).isEqualTo(revocation.getAutoPolicyWaiverId());
    assertThat(resultingDto.hash).isEqualTo(revocation.getHash());
    assertThat(resultingDto.scanId).isEqualTo(revocation.getScanId());
    assertThat(resultingDto.associatedPackageUrl).isEqualTo(revocation.getAssociatedPackageUrl());
    assertThat(resultingDto.componentMatchStrategy).isEqualTo(revocation.getComponentMatchStrategy());
  }

  @Test
  public void testAddAutoPolicyWaiverRevocation_Organization() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getOrganizationId());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), "stageId", "scanId", new Date());
    tempEntity.newPolicyViolation(eval, policy, identifier, "fake", "fake");

    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getOrganizationId());
    AutoPolicyWaiverRevocation revocation = new AutoPolicyWaiverRevocation(
        app.getOrganizationId(),
        "fakeCreatorName",
        "fakeCreatorId",
        new Date(),
        waiver.getId(),
        "scanId",
        "fakeHash",
        "fakeAssociatedPackageUrl",
        ComponentMatcherStrategyForRevocation.EXACT_COMPONENT
    );
    ApiAutoPolicyWaiverRevocationDTO dto = ApiAutoPolicyWaiverRevocationAdapter.convertToDTO(revocation);
    ApiAutoPolicyWaiverRevocationDTO resultingDto = apiAutoPolicyWaiverRevocationService
        .addAutoPolicyWaiverRevocation(OwnerType.ORGANIZATION, app.getOrganizationId(), dto);
    assertThat(resultingDto).isNotNull();
    assertThat(resultingDto.ownerId).isEqualTo(revocation.getOwnerId());
    assertThat(resultingDto.autoPolicyWaiverId).isEqualTo(revocation.getAutoPolicyWaiverId());
    assertThat(resultingDto.hash).isEqualTo(revocation.getHash());
    assertThat(resultingDto.scanId).isEqualTo(revocation.getScanId());
    assertThat(resultingDto.associatedPackageUrl).isEqualTo(revocation.getAssociatedPackageUrl());
    assertThat(resultingDto.componentMatchStrategy).isEqualTo(revocation.getComponentMatchStrategy());
  }

  @Test
  public void testAddAutoPolicyWaiverRevocation_InvalidOwnerType() {
    ApiAutoPolicyWaiverRevocationDTO dto = new ApiAutoPolicyWaiverRevocationDTO();
    assertThatThrownBy(() ->
        apiAutoPolicyWaiverRevocationService.addAutoPolicyWaiverRevocation(OwnerType.REPOSITORY, "ownerId", dto)
    ).isInstanceOf(IllegalStateException.class).hasMessage("Unknown owner type: repository");
  }

  @Test
  public void testAddAutoPolicyWaiverRevocation_InvalidOwnerId() {
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverRevocation revocation = tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());
    ApiAutoPolicyWaiverRevocationDTO dto = ApiAutoPolicyWaiverRevocationAdapter.convertToDTO(revocation);
    dto.ownerId = "invalid";
    assertThatThrownBy(() ->
        apiAutoPolicyWaiverRevocationService.addAutoPolicyWaiverRevocation(OwnerType.APPLICATION, app.getId(), dto)
    ).isInstanceOf(BadRequestException.class)
        .hasMessage("combination of ownerId and autoPolicyWaiverId is invalid");
  }

  @Test
  public void testAddAutoPolicyWaiverRevocation_InvalidAutoPolicyWaiverId() {
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverRevocation revocation = tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());
    ApiAutoPolicyWaiverRevocationDTO dto = ApiAutoPolicyWaiverRevocationAdapter.convertToDTO(revocation);
    dto.autoPolicyWaiverId = "invalid";
    assertThatThrownBy(() ->
        apiAutoPolicyWaiverRevocationService.addAutoPolicyWaiverRevocation(OwnerType.APPLICATION, app.getId(), dto)
    ).isInstanceOf(BadRequestException.class)
        .hasMessage("combination of ownerId and autoPolicyWaiverId is invalid");
  }

  @Test
  public void testAddAutoPolicyWaiverRevocation_WrongWaiverOwner() {
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverRevocation revocation = tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());
    ApiAutoPolicyWaiverRevocationDTO dto = ApiAutoPolicyWaiverRevocationAdapter.convertToDTO(revocation);
    dto.ownerId = app.getOrganizationId();
    assertThatThrownBy(() ->
        apiAutoPolicyWaiverRevocationService.addAutoPolicyWaiverRevocation(OwnerType.APPLICATION, app.getId(), dto)
    ).isInstanceOf(BadRequestException.class)
        .hasMessage("combination of ownerId and autoPolicyWaiverId is invalid");
  }

  @Test
  public void testAddAutoPolicyWaiverRevocation_InvalidScanId() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getOrganizationId());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), "stageId", "scanId", new Date());
    tempEntity.newPolicyViolation(eval, policy, identifier, "fake", "fake");
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverRevocation revocation =
        tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());
    ApiAutoPolicyWaiverRevocationDTO dto = ApiAutoPolicyWaiverRevocationAdapter.convertToDTO(revocation);
    dto.scanId = null;
    assertThatThrownBy(() ->
        apiAutoPolicyWaiverRevocationService.addAutoPolicyWaiverRevocation(OwnerType.APPLICATION, app.getId(), dto)
    ).isInstanceOf(BadRequestException.class).hasMessage("scanId is required");
  }

  @Test
  public void testAddAutoPolicyWaiverRevocation_MissingOwnerId() {
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverRevocation revocation = tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());
    ApiAutoPolicyWaiverRevocationDTO dto = ApiAutoPolicyWaiverRevocationAdapter.convertToDTO(revocation);
    dto.ownerId = null;
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

    AutoPolicyWaiverRevocation revocation = new AutoPolicyWaiverRevocation(
        app.getId(),
        "fakeCreatorName",
        "fakeCreatorId",
        new Date(),
        waiver.getId(),
        "scanId",
        "fakeHash",
        "fakeAssociatedPackageUrl",
        ComponentMatcherStrategyForRevocation.EXACT_COMPONENT
    );
    revocation.setPolicyViolationId(violation.getId());
    ApiAutoPolicyWaiverRevocationDTO dto = ApiAutoPolicyWaiverRevocationAdapter.convertToDTO(revocation);
    ApiAutoPolicyWaiverRevocationDTO resultingDto = apiAutoPolicyWaiverRevocationService
        .addAutoPolicyWaiverRevocation(OwnerType.APPLICATION, app.getId(), dto);

    assertThat(resultingDto).isNotNull();
    assertThat(resultingDto.ownerId).isEqualTo(revocation.getOwnerId());
    assertThat(resultingDto.autoPolicyWaiverId).isEqualTo(revocation.getAutoPolicyWaiverId());
    assertThat(resultingDto.hash).isEqualTo(revocation.getHash());
    assertThat(resultingDto.scanId).isEqualTo(revocation.getScanId());
    assertThat(resultingDto.associatedPackageUrl).isEqualTo(revocation.getAssociatedPackageUrl());
    assertThat(resultingDto.componentMatchStrategy).isEqualTo(revocation.getComponentMatchStrategy());
    assertThat(resultingDto.policyViolationId).isEqualTo(revocation.getPolicyViolationId());
    assertThat(resultingDto.threatLevel).isEqualTo(violation.getThreatLevel());
    assertThat(resultingDto.policyName).isEqualTo(violation.getPolicyName());
    assertThat(resultingDto.componentDisplayName).isEqualTo("a1 v1");
    assertThat(resultingDto.vulnerabilityIdentifiers).isEmpty();
  }

  @Test
  public void testAddAutoPolicyWaiverRevocation_WithAdditionalInfo() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getOrganizationId());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), "stageId", "scanId", new Date());
    PolicyViolation violation = tempEntity.newPolicyViolation(eval, policy, identifier, "fake", "fake");
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());

    AutoPolicyWaiverRevocation revocation = new AutoPolicyWaiverRevocation(
        app.getId(),
        "fakeCreatorName",
        "fakeCreatorId",
        new Date(),
        waiver.getId(),
        "scanId",
        "fakeHash",
        "fakeAssociatedPackageUrl",
        ComponentMatcherStrategyForRevocation.EXACT_COMPONENT
    );
    revocation.setPolicyViolationId(violation.getId());
    revocation.setThreatLevel(5);
    revocation.setPolicyName("fakePolicyName");
    revocation.setComponentDisplayName("fakeComponentDisplayName");
    revocation.setVulnerabilityIdentifiers("CVE-123-456");
    ApiAutoPolicyWaiverRevocationDTO dto = ApiAutoPolicyWaiverRevocationAdapter.convertToDTO(revocation);
    ApiAutoPolicyWaiverRevocationDTO resultingDto = apiAutoPolicyWaiverRevocationService
        .addAutoPolicyWaiverRevocation(OwnerType.APPLICATION, app.getId(), dto);

    assertThat(resultingDto).isNotNull();
    assertThat(resultingDto.ownerId).isEqualTo(revocation.getOwnerId());
    assertThat(resultingDto.autoPolicyWaiverId).isEqualTo(revocation.getAutoPolicyWaiverId());
    assertThat(resultingDto.hash).isEqualTo(revocation.getHash());
    assertThat(resultingDto.scanId).isEqualTo(revocation.getScanId());
    assertThat(resultingDto.associatedPackageUrl).isEqualTo(revocation.getAssociatedPackageUrl());
    assertThat(resultingDto.componentMatchStrategy).isEqualTo(revocation.getComponentMatchStrategy());
    assertThat(resultingDto.policyViolationId).isEqualTo(revocation.getPolicyViolationId());
    assertThat(resultingDto.threatLevel).isEqualTo(revocation.getThreatLevel());
    assertThat(resultingDto.policyName).isEqualTo(revocation.getPolicyName());
    assertThat(resultingDto.componentDisplayName).isEqualTo(revocation.getComponentDisplayName());
    assertThat(resultingDto.vulnerabilityIdentifiers).isEqualTo(revocation.getVulnerabilityIdentifiers());
  }

  @Test
  public void testAddAutoPolicyWaiverRevocation_WithPolicyViolationIdOnly_ViolationNotInDb() {
    Application app = tempEntity.newApplicationWithParent();
    tempEntity.newPolicy(app.getOrganizationId());
    ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    tempEntity.newPolicyEvaluation(app.getId(), "stageId", "scanId", new Date());
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());

    AutoPolicyWaiverRevocation revocation = new AutoPolicyWaiverRevocation(
        app.getId(),
        "fakeCreatorName",
        "fakeCreatorId",
        new Date(),
        waiver.getId(),
        "scanId",
        "fakeHash",
        "fakeAssociatedPackageUrl",
        ComponentMatcherStrategyForRevocation.EXACT_COMPONENT
    );
    revocation.setPolicyViolationId("fakeViolationId");

    ApiAutoPolicyWaiverRevocationDTO dto = ApiAutoPolicyWaiverRevocationAdapter.convertToDTO(revocation);
    ApiAutoPolicyWaiverRevocationDTO resultingDto = apiAutoPolicyWaiverRevocationService
        .addAutoPolicyWaiverRevocation(OwnerType.APPLICATION, app.getId(), dto);

    assertThat(resultingDto).isNotNull();
    assertThat(resultingDto.ownerId).isEqualTo(revocation.getOwnerId());
    assertThat(resultingDto.autoPolicyWaiverId).isEqualTo(revocation.getAutoPolicyWaiverId());
    assertThat(resultingDto.hash).isEqualTo(revocation.getHash());
    assertThat(resultingDto.scanId).isEqualTo(revocation.getScanId());
    assertThat(resultingDto.associatedPackageUrl).isEqualTo(revocation.getAssociatedPackageUrl());
    assertThat(resultingDto.componentMatchStrategy).isEqualTo(revocation.getComponentMatchStrategy());
    assertThat(resultingDto.policyViolationId).isEqualTo(revocation.getPolicyViolationId());
    assertThat(resultingDto.threatLevel).isNull();
    assertThat(resultingDto.policyName).isNull();
    assertThat(resultingDto.componentDisplayName).isNull();
    assertThat(resultingDto.vulnerabilityIdentifiers).isNull();
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
    ).isInstanceOf(IllegalStateException.class).hasMessage("Unknown owner type: repository");
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
  public void testAddAutoPolicyWaiverRevocation_EXACT_COMPONENT_MissingHash() {
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverRevocation revocation = new AutoPolicyWaiverRevocation(
        app.getId(),
        "fakeCreatorName",
        "fakeCreatorId",
        new Date(),
        waiver.getId(),
        "scanId",
        null,
        "fakeAssociatedPackageUrl",
        ComponentMatcherStrategyForRevocation.EXACT_COMPONENT
    );
    ApiAutoPolicyWaiverRevocationDTO dto = ApiAutoPolicyWaiverRevocationAdapter.convertToDTO(revocation);
    assertThatThrownBy(() ->
        apiAutoPolicyWaiverRevocationService.addAutoPolicyWaiverRevocation(OwnerType.APPLICATION, app.getId(), dto)
    ).isInstanceOf(BadRequestException.class).hasMessage("hash is required");
  }

  @Test
  public void testAddAutoPolicyWaiverRevocation_ALL_VERSIONS_MissingAssociatedPackageUrl() {
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverRevocation revocation = new AutoPolicyWaiverRevocation(
        app.getId(),
        "fakeCreatorName",
        "fakeCreatorId",
        new Date(),
        waiver.getId(),
        "scanId",
        "fakeHash",
        null,
        ComponentMatcherStrategyForRevocation.ALL_VERSIONS
    );
    ApiAutoPolicyWaiverRevocationDTO dto = ApiAutoPolicyWaiverRevocationAdapter.convertToDTO(revocation);
    assertThatThrownBy(() ->
        apiAutoPolicyWaiverRevocationService.addAutoPolicyWaiverRevocation(OwnerType.APPLICATION, app.getId(), dto)
    ).isInstanceOf(BadRequestException.class).hasMessage("associatedPackageUrl is required");
  }

  @Test
  public void testBuildReportUrl() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getOrganizationId());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), "stageId", "scanId", new Date());
    tempEntity.newPolicyViolation(eval, policy, identifier, "fake", "fake");
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverRevocation revocation = new AutoPolicyWaiverRevocation(
        app.getId(),
        "fakeCreatorName",
        "fakeCreatorId",
        new Date(),
        waiver.getId(),
        "scanId",
        "hash",
        "purl"
    );
    String result = apiAutoPolicyWaiverRevocationService.buildReportUrl(app.getId(), revocation.getScanId());
    assertThat(result).isNotNull();
    assertThat(result).isEqualTo("/applicationReport/" + app.getId() + "/" + revocation.getScanId() + "/policy");
  }

  @Test
  public void testBuildReportUrl_null() {
    String result = apiAutoPolicyWaiverRevocationService.buildReportUrl(null, null);
    assertThat(result).isNull();
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
    String result = apiAutoPolicyWaiverRevocationService.getCveIdentifiers(violation);
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
    String results = apiAutoPolicyWaiverRevocationService.getCveIdentifiers(violation);
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
    String results = apiAutoPolicyWaiverRevocationService.getCveIdentifiers(violation);
    assertThat(results).isEmpty();
  }

  @Test
  public void testGetCveIdentifiers_Null() {
    Assertions.assertThatThrownBy(() -> apiAutoPolicyWaiverRevocationService.getCveIdentifiers(null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("PolicyViolation cannot be null");
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
    List<ApiAutoPolicyWaiverRevocationDTO> result =
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
    List<ApiAutoPolicyWaiverRevocationDTO> result =
        apiAutoPolicyWaiverRevocationService.getAutoPolicyWaiverRevocations(OwnerType.ORGANIZATION,
            app.getOrganizationId(), waiverTwo.getId(), 1, 10);
    assertThat(result).isEmpty();
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
}
