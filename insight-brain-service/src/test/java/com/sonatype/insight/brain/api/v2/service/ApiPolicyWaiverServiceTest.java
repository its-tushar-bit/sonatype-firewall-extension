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

import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverDTO;
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
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.google.inject.Binder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static com.sonatype.insight.brain.model.OwnerType.REPOSITORY_CONTAINER;
import static com.sonatype.insight.brain.model.repository.RepositoryContainer.REPOSITORY_CONTAINER_ID;
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

  private final PolicyWaiverDAO policyWaiverDAO = new PolicyWaiverDAO();

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
    assertPolicyWaiver(app.getId(), "waiver comment", policyViolation.getHash());
    assertTelemetry(OwnerType.APPLICATION, app.getId());
  }

  @Test
  public void testAddPolicyWaiver_Organization() {
    apiPolicyWaiverService.addPolicyWaiver(policyViolation.getId(), OwnerType.ORGANIZATION, "waiver comment");
    assertPolicyWaiver(org.getId(), "waiver comment", policyViolation.getHash());
    assertTelemetry(OwnerType.ORGANIZATION, org.getId());
  }

  @Test
  public void testAddPolicyWaiver_AcceptsNoComment() {
    apiPolicyWaiverService.addPolicyWaiver(policyViolation.getId(), OwnerType.APPLICATION, null);
    assertPolicyWaiver(app.getId(), null, policyViolation.getHash());
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

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_Application() {
    apiPolicyWaiverService.addPolicyWaiverByPolicyViolationId(OwnerType.APPLICATION, app.getId(),
        policyViolation.getId(), "waiver comment", false);

    assertPolicyWaiver(app.getId(), "waiver comment", policyViolation.getHash());
    assertTelemetry(OwnerType.APPLICATION, app.getId());
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_ApplicationPublicId() {
    apiPolicyWaiverService.addPolicyWaiverByPolicyViolationId(OwnerType.APPLICATION, app.getPublicId(),
        policyViolation.getId(), "waiver comment", false);

    assertPolicyWaiver(app.getId(), "waiver comment", policyViolation.getHash());
    assertTelemetry(OwnerType.APPLICATION, app.getId());
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_NonParentApplication() {
    Application otherApp = tempEntity.newApplication(org.getId());

    assertThatThrownBy(() ->
        apiPolicyWaiverService.addPolicyWaiverByPolicyViolationId(OwnerType.APPLICATION, otherApp.getId(),
            policyViolation.getId(), "waiver comment", false)
    ).isInstanceOf(BadRequestException.class)
        .hasMessage("Invalid owner id: " + otherApp.getId());
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_NonParentApplicationPublicId() {
    Application otherApp = tempEntity.newApplication(org.getId());

    assertThatThrownBy(() ->
        apiPolicyWaiverService.addPolicyWaiverByPolicyViolationId(OwnerType.APPLICATION, otherApp.getPublicId(),
            policyViolation.getId(), "waiver comment", false)
    ).isInstanceOf(BadRequestException.class)
        .hasMessage("Invalid owner id: " + otherApp.getPublicId());
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_Organization() {
    apiPolicyWaiverService.addPolicyWaiverByPolicyViolationId(OwnerType.ORGANIZATION, org.getId(),
        policyViolation.getId(), "waiver comment", false);

    assertPolicyWaiver(org.getId(), "waiver comment", policyViolation.getHash());
    assertTelemetry(OwnerType.ORGANIZATION, org.getId());
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_NonParentOrganization() {
    Organization otherOrg = tempEntity.newOrganization();

    assertThatThrownBy(() ->
    apiPolicyWaiverService.addPolicyWaiverByPolicyViolationId(OwnerType.ORGANIZATION, otherOrg.getId(),
        policyViolation.getId(), "waiver comment", false)
    ).isInstanceOf(BadRequestException.class)
        .hasMessage("Invalid owner id: " + otherOrg.getId());
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_RootOrganization() {
    apiPolicyWaiverService.addPolicyWaiverByPolicyViolationId(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID,
        policyViolation.getId(), "waiver comment", false);

    assertPolicyWaiver(Organization.ROOT_ORGANIZATION_ID, "waiver comment", policyViolation.getHash());
    assertTelemetry(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID);
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_NullComment() {
    apiPolicyWaiverService.addPolicyWaiverByPolicyViolationId(OwnerType.APPLICATION, app.getId(),
        policyViolation.getId(), null, false);

    assertPolicyWaiver(app.getId(), null, policyViolation.getHash());
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_ApplyToAllComponents() {
    apiPolicyWaiverService.addPolicyWaiverByPolicyViolationId(OwnerType.APPLICATION, app.getId(),
        policyViolation.getId(), "waiver comment", true);

    assertPolicyWaiver(app.getId(), "waiver comment", null);
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_InvalidPolicyViolationId() {
    assertThatThrownBy(() ->
        apiPolicyWaiverService.addPolicyWaiverByPolicyViolationId(OwnerType.APPLICATION, app.getId(),
            "invalid-policyViolationId", null, false)
    ).isInstanceOf(NotFoundException.class)
        .hasMessage("Could not find policy violation with ID invalid-policyViolationId.");
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_InvalidOwnerType() {
    assertThatThrownBy(() ->
        apiPolicyWaiverService.addPolicyWaiverByPolicyViolationId(REPOSITORY_CONTAINER, REPOSITORY_CONTAINER_ID,
            policyViolation.getId(), null, false)
    ).isInstanceOf(BadRequestException.class)
        .hasMessage("Invalid owner type: repository_container");
  }

  @Test
  public void testDeletePolicyWaiver_Application() {
    Application application = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(application);
    PolicyWaiver policyWaiver = tempEntity.newWaiver(policy.getId(), application.getId());

    apiPolicyWaiverService.deletePolicyWaiver(OwnerType.APPLICATION, application.getId(), policyWaiver.getId());

    assertThat(policyWaiverDAO.getById(policyWaiver.getId())).isNull();
  }

  @Test
  public void testDeletePolicyWaiver_Application_UsePublicId() {
    Application application = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(application);
    PolicyWaiver policyWaiver = tempEntity.newWaiver(policy.getId(), application.getId());

    apiPolicyWaiverService.deletePolicyWaiver(OwnerType.APPLICATION, application.getPublicId(), policyWaiver.getId());

    assertThat(policyWaiverDAO.getById(policyWaiver.getId())).isNull();
  }

  @Test
  public void testDeletePolicyWaiver_Organization() {
    Organization organization = tempEntity.newOrganization();
    Policy policy = tempEntity.newPolicy(organization);
    PolicyWaiver policyWaiver = tempEntity.newWaiver(policy.getId(), organization.getId());

    apiPolicyWaiverService.deletePolicyWaiver(OwnerType.ORGANIZATION, organization.getId(), policyWaiver.getId());

    assertThat(policyWaiverDAO.getById(policyWaiver.getId())).isNull();
  }

  @Test
  public void testDeletePolicyWaiver_Repository() {
    Repository repository = tempEntity.newRepository();
    Policy policy = tempEntity.newPolicy();
    PolicyWaiver policyWaiver = tempEntity.newWaiver(policy.getId(), repository.getId());

    apiPolicyWaiverService.deletePolicyWaiver(OwnerType.REPOSITORY, repository.getId(), policyWaiver.getId());

    assertThat(policyWaiverDAO.getById(policyWaiver.getId())).isNull();
  }

  @Test
  public void testDeletePolicyWaiver_RepositoryContainer() {
    Policy policy = tempEntity.newPolicy();
    PolicyWaiver policyWaiver = tempEntity.newWaiver(policy.getId(), REPOSITORY_CONTAINER_ID);

    apiPolicyWaiverService.deletePolicyWaiver(REPOSITORY_CONTAINER, REPOSITORY_CONTAINER_ID, policyWaiver.getId());

    assertThat(policyWaiverDAO.getById(policyWaiver.getId())).isNull();
  }

  @Test
  public void testDeletePolicyWaiver_OwnerIdMismatch() {
    Application application = tempEntity.newApplicationWithParent();
    Organization organization = tempEntity.newOrganization();
    Policy policy = tempEntity.newPolicy(application);
    PolicyWaiver policyWaiver = tempEntity.newWaiver(policy.getId(), application.getId());

    assertThatThrownBy(() -> apiPolicyWaiverService
        .deletePolicyWaiver(OwnerType.ORGANIZATION, organization.getId(), policyWaiver.getId()))
        .isInstanceOf(NotFoundException.class)
        .hasMessage("Cannot find a policy waiver with ID " + policyWaiver.getId() + " for " + OwnerType.ORGANIZATION +
            " with ID " + organization.getId());
  }

  @Test
  public void testGetPolicyWaivers_Application() {
    Application application = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(application);
    PolicyWaiver policyWaiver = tempEntity.newWaiver("hash", policy.getId(), application.getId(), "comment");

    List<ApiPolicyWaiverDTO> policyWaiverDtoList =
        apiPolicyWaiverService.getPolicyWaivers(OwnerType.APPLICATION, application.getId());

    assertThat(policyWaiverDtoList).hasSize(1);
    ApiPolicyWaiverDTO actual = policyWaiverDtoList.get(0);
    assertThat(actual.policyWaiverId).isEqualTo(policyWaiver.getId());
    assertThat(actual.comment).isEqualTo(policyWaiver.getComment());
    assertThat(actual.createTime).isEqualTo(policyWaiver.getCreateTime());
    assertThat(actual.hash).isEqualTo(policyWaiver.getHash());
    assertThat(actual.policyId).isEqualTo(policyWaiver.getPolicyId());
    assertThat(actual.scopeOwnerId).isEqualTo(application.getId());
    assertThat(actual.scopeOwnerName).isEqualTo(application.getName());
    assertThat(actual.scopeOwnerType).isEqualTo("application");
  }

  @Test
  public void testGetPolicyWaivers_Application_UsePublicId() {
    Application application = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(application);
    PolicyWaiver policyWaiver = tempEntity.newWaiver("hash", policy.getId(), application.getId(), "comment");

    List<ApiPolicyWaiverDTO> policyWaiverDtoList =
        apiPolicyWaiverService.getPolicyWaivers(OwnerType.APPLICATION, application.getPublicId());

    assertThat(policyWaiverDtoList).hasSize(1);
    ApiPolicyWaiverDTO actual = policyWaiverDtoList.get(0);
    assertThat(actual.policyWaiverId).isEqualTo(policyWaiver.getId());
    assertThat(actual.comment).isEqualTo(policyWaiver.getComment());
    assertThat(actual.createTime).isEqualTo(policyWaiver.getCreateTime());
    assertThat(actual.hash).isEqualTo(policyWaiver.getHash());
    assertThat(actual.policyId).isEqualTo(policyWaiver.getPolicyId());
    assertThat(actual.scopeOwnerId).isEqualTo(application.getId());
    assertThat(actual.scopeOwnerName).isEqualTo(application.getName());
    assertThat(actual.scopeOwnerType).isEqualTo("application");
  }

  @Test
  public void testGetPolicyWaivers_Organization() {
    Organization organization = tempEntity.newOrganization();
    Policy policy = tempEntity.newPolicy(organization);
    PolicyWaiver policyWaiver = tempEntity.newWaiver("hash", policy.getId(), organization.getId(), "comment");

    List<ApiPolicyWaiverDTO> policyWaiverDtoList =
        apiPolicyWaiverService.getPolicyWaivers(OwnerType.ORGANIZATION, organization.getId());

    assertThat(policyWaiverDtoList).hasSize(1);
    ApiPolicyWaiverDTO actual = policyWaiverDtoList.get(0);
    assertThat(actual.policyWaiverId).isEqualTo(policyWaiver.getId());
    assertThat(actual.comment).isEqualTo(policyWaiver.getComment());
    assertThat(actual.createTime).isEqualTo(policyWaiver.getCreateTime());
    assertThat(actual.hash).isEqualTo(policyWaiver.getHash());
    assertThat(actual.policyId).isEqualTo(policyWaiver.getPolicyId());
    assertThat(actual.scopeOwnerId).isEqualTo(organization.getId());
    assertThat(actual.scopeOwnerName).isEqualTo(organization.getName());
    assertThat(actual.scopeOwnerType).isEqualTo("organization");
  }

  @Test
  public void testGetPolicyWaivers_Repository() {
    Repository repository = tempEntity.newRepository();
    Policy policy = tempEntity.newPolicy();
    PolicyWaiver policyWaiver = tempEntity.newWaiver("hash", policy.getId(), repository.getId(), "comment");

    List<ApiPolicyWaiverDTO> policyWaiverDtoList =
        apiPolicyWaiverService.getPolicyWaivers(OwnerType.REPOSITORY, repository.getId());

    assertThat(policyWaiverDtoList).hasSize(1);
    ApiPolicyWaiverDTO actual = policyWaiverDtoList.get(0);
    assertThat(actual.policyWaiverId).isEqualTo(policyWaiver.getId());
    assertThat(actual.comment).isEqualTo(policyWaiver.getComment());
    assertThat(actual.createTime).isEqualTo(policyWaiver.getCreateTime());
    assertThat(actual.hash).isEqualTo(policyWaiver.getHash());
    assertThat(actual.policyId).isEqualTo(policyWaiver.getPolicyId());
    assertThat(actual.scopeOwnerId).isEqualTo(repository.getId());
    assertThat(actual.scopeOwnerName).isEqualTo(repository.getName());
    assertThat(actual.scopeOwnerType).isEqualTo("repository");
  }

  @Test
  public void testGetPolicyWaivers_RepositoryContainer() {
    Policy policy = tempEntity.newPolicy();
    PolicyWaiver policyWaiver = tempEntity.newWaiver("hash", policy.getId(), REPOSITORY_CONTAINER_ID, "comment");

    List<ApiPolicyWaiverDTO> policyWaiverDtoList =
        apiPolicyWaiverService.getPolicyWaivers(REPOSITORY_CONTAINER, REPOSITORY_CONTAINER_ID);

    assertThat(policyWaiverDtoList).hasSize(1);
    ApiPolicyWaiverDTO actual = policyWaiverDtoList.get(0);
    assertThat(actual.policyWaiverId).isEqualTo(policyWaiver.getId());
    assertThat(actual.comment).isEqualTo(policyWaiver.getComment());
    assertThat(actual.createTime).isEqualTo(policyWaiver.getCreateTime());
    assertThat(actual.hash).isEqualTo(policyWaiver.getHash());
    assertThat(actual.policyId).isEqualTo(policyWaiver.getPolicyId());
    assertThat(actual.scopeOwnerId).isEqualTo(REPOSITORY_CONTAINER_ID);
    assertThat(actual.scopeOwnerName).isEqualTo("All Repositories");
    assertThat(actual.scopeOwnerType).isEqualTo("all_repositories");
  }

  private void assertPolicyWaiver(String ownerId, String comment, String hash) {
    List<PolicyWaiver> policyWaivers = new PolicyWaiverDAO().getByOwnerId(ownerId);
    assertThat(policyWaivers).hasSize(1);
    PolicyWaiver policyWaiver = policyWaivers.get(0);
    assertThat(policyWaiver).isNotNull();
    assertThat(policyWaiver.getId()).isNotNull();
    assertThat(policyWaiver.getOwnerId()).isEqualTo(ownerId);
    assertThat(policyWaiver.getHash()).isEqualTo(hash);
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
