/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiversApplicableToViolationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiWaiverOptionsDTO;
import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiComponentPolicyWaiversDTO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.InvalidStageException;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.PolicyWaiverTelemetryCreator;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.utils.ScopeOwnerUtils;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import com.sonatype.insight.test.LogOutput;

import com.google.inject.Binder;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static com.sonatype.insight.brain.api.v2.service.ApiPolicyWaiverDTOTestUtils.assertApiPolicyWaiverDTO;
import static com.sonatype.insight.brain.model.OwnerType.REPOSITORY_CONTAINER;
import static com.sonatype.insight.brain.model.repository.RepositoryContainer.REPOSITORY_CONTAINER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class ApiPolicyWaiverServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ApiPolicyWaiverService apiPolicyWaiverService;

  private Policy policy;

  private PolicyEvaluation policyEvaluation;

  private PolicyViolation policyViolation;

  private Application app;

  private Organization org;

  private final PolicyWaiverDAO policyWaiverDAO = new PolicyWaiverDAO();

  @Inject
  private PolicyDAO policyDAO;

  @Inject
  private OwnerDAO ownerDAO;

  @Mock
  private TelemetrySender telemetrySenderMock;

  @Mock
  private ApiPolicyViolationServiceV2 apiPolicyViolationServiceV2Mock;

  @Mock
  private PolicyWaiverTelemetryCreator policyWaiverTelemetryCreator;

  @Rule
  public LogOutput logOutput = new LogOutput(ApiPolicyWaiverService.class);

  @Override
  public void configure(Binder binder) {
    binder.bind(TelemetrySender.class).toInstance(telemetrySenderMock);
    binder.bind(ApiPolicyViolationServiceV2.class).toInstance(apiPolicyViolationServiceV2Mock);
    binder.bind(PolicyWaiverTelemetryCreator.class).toInstance(policyWaiverTelemetryCreator);
  }

  @Before
  public void setUpPolicyViolation() {
    org = tempEntity.newOrganization();
    app = tempEntity.newApplication(org.getId());
    policy = tempEntity.newPolicy(org.getId());

    policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId1App1");
    policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy, "g1", "a1", "v1", "h1", "r1");
  }

  /**
   * @deprecated The tested method is deprecated
   */
  @Deprecated
  @Test
  public void testAddPolicyWaiver_Application() {
    apiPolicyWaiverService.addPolicyWaiver(policyViolation.getId(), OwnerType.APPLICATION, "waiver comment");
    assertNotExpiringPolicyWaiver(app.getId(), "waiver comment", "testuser", "Test User", policyViolation.getHash());
    assertTelemetry(OwnerType.APPLICATION, app.getId());
    assertWaiverTelemetry(OwnerType.APPLICATION, policyViolation, policyWaiverDAO.getByOwnerId(app.getId()).get(0));
  }

  /**
   * @deprecated The tested method is deprecated
   */
  @Deprecated
  @Test
  public void testAddPolicyWaiver_Organization() {
    apiPolicyWaiverService.addPolicyWaiver(policyViolation.getId(), OwnerType.ORGANIZATION, "waiver comment");
    assertNotExpiringPolicyWaiver(org.getId(), "waiver comment", "testuser", "Test User", policyViolation.getHash());
    assertTelemetry(OwnerType.ORGANIZATION, org.getId());
    assertWaiverTelemetry(OwnerType.ORGANIZATION, policyViolation, policyWaiverDAO.getByOwnerId(org.getId()).get(0));
  }

  /**
   * @deprecated The tested method is deprecated
   */
  @Deprecated
  @Test
  public void testAddPolicyWaiver_AcceptsNoComment() {
    apiPolicyWaiverService.addPolicyWaiver(policyViolation.getId(), OwnerType.APPLICATION, null);
    assertNotExpiringPolicyWaiver(app.getId(), null, "testuser", "Test User", policyViolation.getHash());
  }

  /**
   * @deprecated The tested method is deprecated
   */
  @SuppressWarnings("deprecation")
  @Deprecated
  @Test
  public void testAddPolicyWaiver_InvalidPolicyViolationId() {
    assertThatThrownBy(() ->
        apiPolicyWaiverService.addPolicyWaiver("invalid-policyViolationId", OwnerType.APPLICATION, "waiver comment")
    ).isInstanceOf(NotFoundException.class)
        .hasMessage("Could not find policy violation with ID invalid-policyViolationId.");
  }

  /**
   * @deprecated The tested method is deprecated
   */
  @SuppressWarnings("deprecation")
  @Deprecated
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
        policyViolation.getId(), "waiver comment", false, null);
    assertNotExpiringPolicyWaiver(app.getId(), "waiver comment", "testuser", "Test User", policyViolation.getHash());
    assertTelemetry(OwnerType.APPLICATION, app.getId());
    assertWaiverTelemetry(OwnerType.APPLICATION, policyViolation, policyWaiverDAO.getByOwnerId(app.getId()).get(0));
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_ApplicationPublicId() {
    apiPolicyWaiverService.addPolicyWaiverByPolicyViolationId(OwnerType.APPLICATION, app.getPublicId(),
        policyViolation.getId(), "waiver comment", false, null);

    assertNotExpiringPolicyWaiver(app.getId(), "waiver comment", "testuser", "Test User", policyViolation.getHash());
    assertTelemetry(OwnerType.APPLICATION, app.getId());
    assertWaiverTelemetry(OwnerType.APPLICATION, policyViolation, policyWaiverDAO.getByOwnerId(app.getId()).get(0));
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_NonParentApplication() {
    Application otherApp = tempEntity.newApplication(org.getId());

    assertThatThrownBy(() ->
        apiPolicyWaiverService.addPolicyWaiverByPolicyViolationId(OwnerType.APPLICATION, otherApp.getId(),
            policyViolation.getId(), "waiver comment", false, null)
    ).isInstanceOf(BadRequestException.class)
        .hasMessage("Invalid owner id: " + otherApp.getId());
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_NonParentApplicationPublicId() {
    Application otherApp = tempEntity.newApplication(org.getId());

    assertThatThrownBy(() ->
        apiPolicyWaiverService.addPolicyWaiverByPolicyViolationId(OwnerType.APPLICATION, otherApp.getPublicId(),
            policyViolation.getId(), "waiver comment", false, null)
    ).isInstanceOf(BadRequestException.class)
        .hasMessage("Invalid owner id: " + otherApp.getPublicId());
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_Organization() {
    apiPolicyWaiverService.addPolicyWaiverByPolicyViolationId(OwnerType.ORGANIZATION, org.getId(),
        policyViolation.getId(), "waiver comment", false, null);

    assertNotExpiringPolicyWaiver(org.getId(), "waiver comment", "testuser", "Test User", policyViolation.getHash());
    assertTelemetry(OwnerType.ORGANIZATION, org.getId());
    assertWaiverTelemetry(OwnerType.ORGANIZATION, policyViolation, policyWaiverDAO.getByOwnerId(org.getId()).get(0));
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_NonParentOrganization() {
    Organization otherOrg = tempEntity.newOrganization();

    assertThatThrownBy(() ->
    apiPolicyWaiverService.addPolicyWaiverByPolicyViolationId(OwnerType.ORGANIZATION, otherOrg.getId(),
        policyViolation.getId(), "waiver comment", false, null)
    ).isInstanceOf(BadRequestException.class)
        .hasMessage("Invalid owner id: " + otherOrg.getId());
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_RootOrganization() {
    apiPolicyWaiverService.addPolicyWaiverByPolicyViolationId(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID,
        policyViolation.getId(), "waiver comment", false, null);

    assertNotExpiringPolicyWaiver(Organization.ROOT_ORGANIZATION_ID, "waiver comment", "testuser",
        "Test User", policyViolation.getHash());
    assertTelemetry(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID);
    assertWaiverTelemetry(OwnerType.ORGANIZATION, policyViolation,
        policyWaiverDAO.getByOwnerId(Organization.ROOT_ORGANIZATION_ID).get(0));
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_NullComment() {
    apiPolicyWaiverService.addPolicyWaiverByPolicyViolationId(OwnerType.APPLICATION, app.getId(),
        policyViolation.getId(), null, false, null);

    assertNotExpiringPolicyWaiver(app.getId(), null, "testuser", "Test User", policyViolation.getHash());
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_ApplyToAllComponents() {
    apiPolicyWaiverService.addPolicyWaiverByPolicyViolationId(OwnerType.APPLICATION, app.getId(),
        policyViolation.getId(), "waiver comment", true, null);

    assertNotExpiringPolicyWaiver(app.getId(), "waiver comment", "testuser", "Test User", null);
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_InvalidPolicyViolationId() {
    assertThatThrownBy(() ->
        apiPolicyWaiverService.addPolicyWaiverByPolicyViolationId(OwnerType.APPLICATION, app.getId(),
            "invalid-policyViolationId", null, false, null)
    ).isInstanceOf(NotFoundException.class)
        .hasMessage("Could not find policy violation with ID invalid-policyViolationId.");
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_InvalidOwnerType() {
    assertThatThrownBy(() ->
        apiPolicyWaiverService.addPolicyWaiverByPolicyViolationId(REPOSITORY_CONTAINER, REPOSITORY_CONTAINER_ID,
            policyViolation.getId(), null, false, null)
    ).isInstanceOf(BadRequestException.class)
        .hasMessage("Invalid owner type: repository_container");
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_WithExpiry_InPast() {
    Date expiryTime = DateTime.now().minusHours(1).toDate();

    apiPolicyWaiverService.addPolicyWaiverByPolicyViolationId(OwnerType.APPLICATION, app.getId(),
        policyViolation.getId(), "waiver comment", true, expiryTime);

    List<PolicyWaiver> policyWaivers = new PolicyWaiverDAO().getActiveByOwnerId(app.getId());
    assertThat(policyWaivers).isEmpty();

    policyWaivers = new PolicyWaiverDAO().getByOwnerId(app.getId());
    assertThat(policyWaivers).isNotEmpty().hasSize(1);
    assertPolicyWaiver(policyWaivers.get(0), app.getId(), "waiver comment", "testuser", "Test User", null, expiryTime);
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_WithExpiry_InFuture() {
    Date expiryTime = DateTime.now().plusHours(1).toDate();

    apiPolicyWaiverService.addPolicyWaiverByPolicyViolationId(OwnerType.APPLICATION, app.getId(),
        policyViolation.getId(), "waiver comment", true, expiryTime);

    List<PolicyWaiver> policyWaivers = new PolicyWaiverDAO().getActiveByOwnerId(app.getId());
    assertThat(policyWaivers).isNotEmpty().hasSize(1);
    assertPolicyWaiver(policyWaivers.get(0), app.getId(), "waiver comment", "testuser", "Test User", null, expiryTime);
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_WithExpiry_Null() {
    apiPolicyWaiverService.addPolicyWaiverByPolicyViolationId(OwnerType.APPLICATION, app.getId(),
        policyViolation.getId(), "waiver comment", true, null);

    assertNotExpiringPolicyWaiver(app.getId(), "waiver comment", "testuser", "Test User", null);
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
  public void testGetPolicyWaivers_Application_ExcludesExpiredWaivers() {
    Instant now = Instant.now();
    Date today = Date.from(now);
    Date yesterday = Date.from(now.minus(1, ChronoUnit.DAYS));
    Date aWeekFromNow = Date.from(now.plus(7, ChronoUnit.DAYS));
    Date aWeekAgo = Date.from(now.minus(7, ChronoUnit.DAYS));

    Application application = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(application);
    PolicyWaiver policyWaiver = tempEntity.newWaiver("hash", policy.getId(), application.getId(),
        null, "comment", today, aWeekFromNow);
    tempEntity.newWaiver(null, policy.getId(), application.getId(),
        null, "the expired waiver", aWeekAgo, yesterday);

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
    assertThat(actual.expiryTime).isEqualTo(aWeekFromNow);
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
  public void testGetPolicyWaivers_Organization_ExcludesExpiredWaivers() {
    Instant now = Instant.now();
    Date today = Date.from(now);
    Date yesterday = Date.from(now.minus(1, ChronoUnit.DAYS));
    Date aWeekFromNow = Date.from(now.plus(7, ChronoUnit.DAYS));
    Date aWeekAgo = Date.from(now.minus(7, ChronoUnit.DAYS));

    Organization organization = tempEntity.newOrganization();
    Policy policy = tempEntity.newPolicy(organization);
    PolicyWaiver policyWaiver = tempEntity.newWaiver("hash", policy.getId(), organization.getId(),
        null, "comment", today, aWeekFromNow);
    // an expired waiver
    tempEntity.newWaiver(null, policy.getId(), organization.getId(), null,
        "expired waiver", aWeekAgo, yesterday);

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
    assertThat(actual.expiryTime).isEqualTo(aWeekFromNow);
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
  public void testGetPolicyWaivers_Repository_ExcludesExpiredWaiver() {
    Instant now = Instant.now();
    Date today = Date.from(now);
    Date yesterday = Date.from(now.minus(1, ChronoUnit.DAYS));
    Date aWeekFromNow = Date.from(now.plus(7, ChronoUnit.DAYS));
    Date aWeekAgo = Date.from(now.minus(7, ChronoUnit.DAYS));

    Repository repository = tempEntity.newRepository();
    Policy policy = tempEntity.newPolicy();
    PolicyWaiver policyWaiver = tempEntity.newWaiver("hash", policy.getId(), repository.getId(),
        null, "comment", today, aWeekFromNow);
    tempEntity.newWaiver(null, policy.getId(), repository.getId(),
        null, "comment", aWeekAgo, yesterday);

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
    assertThat(actual.expiryTime).isEqualTo(aWeekFromNow);
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

  @Test
  public void testGetPolicyWaivers_RepositoryContainer_ExcludesExpiredWaiver() {
    Instant now = Instant.now();
    Date today = Date.from(now);
    Date yesterday = Date.from(now.minus(1, ChronoUnit.DAYS));
    Date aWeekFromNow = Date.from(now.plus(7, ChronoUnit.DAYS));
    Date aWeekAgo = Date.from(now.minus(7, ChronoUnit.DAYS));

    Policy policy = tempEntity.newPolicy();
    PolicyWaiver policyWaiver = tempEntity.newWaiver("hash", policy.getId(), REPOSITORY_CONTAINER_ID,
        null, "comment", today, aWeekFromNow);
    tempEntity.newWaiver(null, policy.getId(), REPOSITORY_CONTAINER_ID,
        null, "comment", aWeekAgo, yesterday);

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
    assertThat(actual.expiryTime).isEqualTo(aWeekFromNow);
  }

  @Test
  public void testGetPolicyWaivers_Expired() {
    DateTime now = DateTime.now();

    Application application = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(application);
    tempEntity.newWaiver("hash", policy.getId(), application.getId(), null, "comment", now.toDate(), now.toDate());

    List<ApiPolicyWaiverDTO> policyWaiverDtoList =
        apiPolicyWaiverService.getPolicyWaivers(OwnerType.APPLICATION, application.getId());

    assertThat(policyWaiverDtoList).isEmpty();
  }

  @Test
  public void testGetPolicyWaivers_ExpiringInFuture() {
    DateTime now = DateTime.now();

    Application application = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(application);
    PolicyWaiver policyWaiver = tempEntity.newWaiver("hash", policy.getId(), application.getId(), null, "comment",
        now.toDate(), now.plusHours(1).toDate()); // expiring in future

    List<ApiPolicyWaiverDTO> policyWaiverDtoList =
        apiPolicyWaiverService.getPolicyWaivers(OwnerType.APPLICATION, application.getId());

    assertThat(policyWaiverDtoList).hasSize(1);
    ApiPolicyWaiverDTO actual = policyWaiverDtoList.get(0);
    assertThat(actual.policyWaiverId).isEqualTo(policyWaiver.getId());
    assertThat(actual.comment).isEqualTo(policyWaiver.getComment());
    assertThat(actual.createTime).isEqualTo(policyWaiver.getCreateTime());
    assertThat(actual.expiryTime).isEqualTo(policyWaiver.getExpiryTime());
    assertThat(actual.hash).isEqualTo(policyWaiver.getHash());
    assertThat(actual.policyId).isEqualTo(policyWaiver.getPolicyId());
    assertThat(actual.scopeOwnerId).isEqualTo(application.getId());
    assertThat(actual.scopeOwnerName).isEqualTo(application.getName());
    assertThat(actual.scopeOwnerType).isEqualTo("application");
  }

  @Test
  public void testGetApplicableWaivers_NullId() {
    assertThatThrownBy(() ->
        apiPolicyWaiverService.getApplicableWaivers(null)
    ).isInstanceOf(NotFoundException.class)
        .hasMessage("Could not find policy violation with ID null.");
  }

  @Test
  public void testGetApplicableWaivers_InvalidId() {
    assertThatThrownBy(() ->
        apiPolicyWaiverService.getApplicableWaivers("InvalidPolicyViolationId")
    ).isInstanceOf(NotFoundException.class)
        .hasMessage("Could not find policy violation with ID InvalidPolicyViolationId.");
  }

  @Test
  public void testGetApplicableWaivers_NoWaivers() {
    ApiPolicyWaiversApplicableToViolationDTO results =
        apiPolicyWaiverService.getApplicableWaivers(policyViolation.getId());
    assertThat(results.activeWaivers).isEmpty();
    assertThat(results.expiredWaivers).isEmpty();
  }

  @Test
  public void testGetApplicableWaivers() {
    DateTime now = DateTime.now();
    List<ConstraintFact> constraintFacts = tempEntity.createArbitraryConstraintFacts();
    List<ConstraintFact> constraintFacts2 = tempEntity.createArbitraryConstraintFacts();
    Organization newOrg = tempEntity.newOrganization("NewOrg");
    Application newApp = tempEntity.newApplication("NewApp", "AppPublicId", newOrg.getId());
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(newApp.getId(), BuildStageType.ID, "scanId");
    Policy policy = tempEntity.newPolicy(newOrg);
    Policy policy2 = tempEntity.newPolicy(newApp);
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("group", "artifact", "id");
    PolicyViolation violation = tempEntity.newPolicyViolation(evaluation, policy, identifier, "hash");
    violation.setConstraintFacts(constraintFacts);
    new PolicyViolationDAO().update(violation);

    String policyId = policy.getId();
    String policy2Id = policy2.getId();
    String orgId = newOrg.getId();
    String appId = newApp.getId();

    Date expiredExpiryTime = now.minusMillis(1).toDate();
    Date expiringInFutureExpiryTime = now.plusMinutes(1).toDate();

    // applicable waivers that the service should return for the given violation
    tempEntity.newWaiver("hash", policyId, orgId, constraintFacts, "", now.minusDays(10).toDate());
    tempEntity.newWaiver(null, policyId, orgId, constraintFacts, "", now.minusDays(9).toDate(), null);
    tempEntity.newWaiver("hash", policyId, appId, constraintFacts, "", now.minusDays(8).toDate(),
        expiredExpiryTime); // expired
    tempEntity.newWaiver(null, policyId, appId, constraintFacts, "A comment", now.minusDays(7).toDate(),
        expiringInFutureExpiryTime); // expiring in the future
    // add more waivers with different attributes — for diversity
    tempEntity.newWaiver("hash", policyId, appId, null, "", now.minusDays(6).toDate());
    tempEntity.newWaiver(null, policyId, appId, null, "", now.minusDays(5).toDate());
    tempEntity.newWaiver("hashX", policyId, appId, constraintFacts, "", now.minusDays(4).toDate());
    tempEntity.newWaiver("hash", policyId, appId, constraintFacts2, "", now.minusDays(3).toDate());
    tempEntity.newWaiver("hash2", policy2Id, appId, null, "", now.minusDays(2).toDate(), null);
    tempEntity.newWaiver(null, policy2Id, appId, null, "", now.minusDays(1).toDate(), now.plusMinutes(1).toDate());
    tempEntity.newWaiver("hash", policy2Id, appId, constraintFacts, "", now.toDate(), now.minusMillis(1).toDate());

    String policyViolationId = violation.getId();

    ApiPolicyWaiversApplicableToViolationDTO dto = apiPolicyWaiverService.getApplicableWaivers(policyViolationId);

    // activeWaivers - results sorted to have deterministic ordering in the test
    List<ApiPolicyWaiverDTO> activeApplicableWaivers = dto.activeWaivers.stream()
        .sorted(Comparator.comparing(apiPolicyWaiverDTO -> apiPolicyWaiverDTO.createTime))
        .collect(Collectors.toList());

    assertThat(activeApplicableWaivers.size()).isEqualTo(3);
    assertApiPolicyWaiverDTO("hash", policyId, orgId, "NewOrg", "", policyViolationId,
        null, activeApplicableWaivers.get(0));
    assertApiPolicyWaiverDTO(null, policyId, orgId, "NewOrg", "", policyViolationId,
        null, activeApplicableWaivers.get(1));
    assertApiPolicyWaiverDTO(null, policyId, appId, "NewApp", "A comment", policyViolationId,
        expiringInFutureExpiryTime, activeApplicableWaivers.get(2));

    // expiredWaivers
    List<ApiPolicyWaiverDTO> expiredApplicableWaivers = dto.expiredWaivers;

    assertThat(expiredApplicableWaivers.size()).isEqualTo(1);
    assertApiPolicyWaiverDTO(
        "hash", policyId, appId, "NewApp", "", policyViolationId, expiredExpiryTime, expiredApplicableWaivers.get(0));
  }

  @Test
  public void testAddWaiverToTransitivePolicyViolationsByAppScanComponent_EmptyPolicyViolations() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    Component component = new Component(componentIdentifier);

    when(apiPolicyViolationServiceV2Mock.getTransitivePolicyViolationsForLastEvaluation(app.getId(),
        policyEvaluation.getScanId(), componentIdentifier, null, null))
            .thenReturn(Pair.of(component, Collections.emptyList()));

    apiPolicyWaiverService.addWaiverToTransitivePolicyViolationsByAppScanComponent(OwnerType.APPLICATION,
        app.getPublicId(), policyEvaluation.getScanId(), componentIdentifier, null, null, null);

    verify(apiPolicyViolationServiceV2Mock).getTransitivePolicyViolationsForLastEvaluation(app.getId(),
        policyEvaluation.getScanId(), componentIdentifier, null, null);
    assertThat(policyWaiverDAO.getByOwnerId(app.getId())).isEmpty();
    assertTelemetry(OwnerType.APPLICATION, app.getPublicId());
    verifyNoInteractions(policyWaiverTelemetryCreator);
  }

  @Test
  public void testAddWaiverToTransitivePolicyViolationsByAppScanComponent_WithPolicyViolationsNoDateNoComment() {
    testAddWaiverToTransitivePolicyViolationsByAppScanComponent(null, null);
  }

  @Test
  public void testAddWaiverToTransitivePolicyViolationsByAppScanComponent_WithPolicyViolationsDateAndComment() {
    testAddWaiverToTransitivePolicyViolationsByAppScanComponent(new Date(), "this is a test");
  }

  private void testAddWaiverToTransitivePolicyViolationsByAppScanComponent(Date expiryTime, String comment) {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    Component component = new Component(componentIdentifier);
    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e");
    Component component1 = new Component(componentIdentifier1);
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "c2", "e2");
    Component component2 = new Component(componentIdentifier2);

    Policy policy2 = tempEntity.newPolicy(org.getId());
    PolicyViolation policyViolation2 =
        tempEntity.newPolicyViolation(policyEvaluation, policy2, componentIdentifier2, "hash");

    Pair<Component, List<Pair<PolicyViolation, Component>>> pair =
        Pair.of(component, Arrays.asList(Pair.of(policyViolation, component1), Pair.of(policyViolation2, component2)));

    ApiWaiverOptionsDTO apiWaiverOptionsDTO = new ApiWaiverOptionsDTO();
    apiWaiverOptionsDTO.expiryTime = expiryTime;
    apiWaiverOptionsDTO.comment = comment;

    when(apiPolicyViolationServiceV2Mock.getTransitivePolicyViolationsForLastEvaluation(app.getId(),
        policyEvaluation.getScanId(), componentIdentifier, null, null))
            .thenReturn(pair);

    apiPolicyWaiverService.addWaiverToTransitivePolicyViolationsByAppScanComponent(OwnerType.APPLICATION,
        app.getPublicId(), policyEvaluation.getScanId(), componentIdentifier, null, null, apiWaiverOptionsDTO);

    verify(apiPolicyViolationServiceV2Mock).getTransitivePolicyViolationsForLastEvaluation(app.getId(),
        policyEvaluation.getScanId(), componentIdentifier, null, null);

    List<PolicyWaiver> policyWaivers = policyWaiverDAO.getByOwnerId(app.getId());
    assertThat(policyWaivers).hasSize(2);

    PolicyWaiver policyWaiver =
        policyWaivers.stream().filter(waiver -> waiver.getPolicyId().equals(policy.getId())).findFirst().get();
    assertThat(policyWaiver.getPolicyId()).isEqualTo(policy.getId());
    assertThat(policyWaiver.getConstraintFactsJson()).isEqualTo(policyViolation.getConstraintFactsJson());
    assertThat(policyWaiver.getHash()).isEqualTo(policyViolation.getHash());
    assertThat(policyWaiver.getExpiryTime()).isEqualTo(expiryTime);
    assertThat(policyWaiver.getComment()).isEqualTo(comment);
    assertWaiverTelemetry(OwnerType.APPLICATION, policyViolation, policyWaiver, 0, 2);

    policyWaiver =
        policyWaivers.stream().filter(waiver -> waiver.getPolicyId().equals(policy2.getId())).findFirst().get();
    assertThat(policyWaiver.getPolicyId()).isEqualTo(policy2.getId());
    assertThat(policyWaiver.getConstraintFactsJson()).isEqualTo(policyViolation2.getConstraintFactsJson());
    assertThat(policyWaiver.getHash()).isEqualTo(policyViolation2.getHash());
    assertThat(policyWaiver.getExpiryTime()).isEqualTo(expiryTime);
    assertThat(policyWaiver.getComment()).isEqualTo(comment);
    assertWaiverTelemetry(OwnerType.APPLICATION, policyViolation2, policyWaiver, 1, 2);

    assertTelemetry(OwnerType.APPLICATION, app.getPublicId());
  }

  @Test
  public void testAddWaiverToTransitivePolicyViolationsByAppScanComponent_RepeatedWaiver() {
    PolicyWaiver policyWaiver = tempEntity.newWaiver(policyViolation.getHash(), policyViolation.getPolicyId(),
        app.getId(), policyViolation.getConstraintFacts(), "comment");
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    Component component = new Component(componentIdentifier);
    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e");
    Component component1 = new Component(componentIdentifier1);

    Pair<Component, List<Pair<PolicyViolation, Component>>> pair =
        Pair.of(component, Collections.singletonList(Pair.of(policyViolation, component1)));

    when(apiPolicyViolationServiceV2Mock.getTransitivePolicyViolationsForLastEvaluation(app.getId(),
        policyEvaluation.getScanId(), componentIdentifier, null, null)).thenReturn(pair);

    apiPolicyWaiverService.addWaiverToTransitivePolicyViolationsByAppScanComponent(OwnerType.APPLICATION,
        app.getPublicId(), policyEvaluation.getScanId(), componentIdentifier, null, null, null);

    verify(apiPolicyViolationServiceV2Mock).getTransitivePolicyViolationsForLastEvaluation(app.getId(),
        policyEvaluation.getScanId(), componentIdentifier, null, null);

    List<PolicyWaiver> policyWaivers = policyWaiverDAO.getByOwnerId(app.getId());
    assertThat(policyWaivers).hasSize(1);
    assertThat(policyWaivers.get(0).getId()).isEqualTo(policyWaiver.getId());
    assertThat(logOutput).atWarnLevel()
        .contains("Unable to add waiver for PolicyViolation ID " + policyViolation.getId());

    assertTelemetry(OwnerType.APPLICATION, app.getPublicId());
    verifyNoInteractions(policyWaiverTelemetryCreator);
  }

  @Test
  public void testAddWaiverToTransitivePolicyViolationsByOwnerStageComponent_UnknownOwnerId() {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(
        () -> apiPolicyWaiverService.addWaiverToTransitivePolicyViolationsByOwnerStageComponent(OwnerType.APPLICATION,
            "doesNotExist", BuildStageType.ID, null, null, null, null)
    ).withMessageContaining("Could not find an application with ID doesNotExist.");
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(
        () -> apiPolicyWaiverService.addWaiverToTransitivePolicyViolationsByOwnerStageComponent(OwnerType.ORGANIZATION,
            "doesNotExist", BuildStageType.ID, null, null, null, null)
    ).withMessageContaining("Cannot find organization with ID doesNotExist.");
  }

  @Test
  public void testAddWaiverToTransitivePolicyViolationsByOwnerStageComponent_UnknownStageId() {
    assertThatExceptionOfType(InvalidStageException.class).isThrownBy(
        () -> apiPolicyWaiverService.addWaiverToTransitivePolicyViolationsByOwnerStageComponent(OwnerType.APPLICATION,
            app.getPublicId(), "doesNotExist", null, null, null, null)
    ).withMessageContaining("Invalid stage id=doesNotExist");
  }

  @Test
  public void testAddWaiverToTransitivePolicyViolationsByOwnerStageComponent_NoWaiverOptions() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    Component component = new Component(componentIdentifier);
    component.setHash("hash");
    Policy policy = tempEntity.newPolicy(org.getId());
    PolicyViolation policyViolation =
        tempEntity.newPolicyViolation(policyEvaluation, policy, componentIdentifier, component.getHash());
    Pair<Component, List<Pair<PolicyViolation, Component>>> pair =
        Pair.of(component, Collections.singletonList(Pair.of(policyViolation, component)));
    when(apiPolicyViolationServiceV2Mock.getTransitivePolicyViolationsByComponent(any(), any(), any(), any(), any()))
        .thenReturn(pair);

    apiPolicyWaiverService.addWaiverToTransitivePolicyViolationsByOwnerStageComponent(OwnerType.APPLICATION,
        app.getPublicId(), BuildStageType.ID, componentIdentifier, null, null, null);

    List<PolicyWaiver> policyWaivers = policyWaiverDAO.getByOwnerId(app.getId());
    assertThat(policyWaivers).hasSize(1);
    PolicyWaiver policyWaiver = policyWaivers.get(0);
    assertThat(policyWaiver.getPolicyId()).isEqualTo(policy.getId());
    assertThat(policyWaiver.getConstraintFactsJson()).isEqualTo(policyViolation.getConstraintFactsJson());
    assertThat(policyWaiver.getHash()).isEqualTo(policyViolation.getHash());
    assertThat(policyWaiver.getComment()).isNull();
    assertThat(policyWaiver.getExpiryTime()).isNull();
  }

  @Test
  public void testAddWaiverToTransitivePolicyViolationsByOwnerStageComponent() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    Component component = new Component(componentIdentifier);
    component.setHash("hash");
    Policy policy = tempEntity.newPolicy(org.getId());
    PolicyViolation policyViolation =
        tempEntity.newPolicyViolation(policyEvaluation, policy, componentIdentifier, component.getHash());
    Pair<Component, List<Pair<PolicyViolation, Component>>> pair =
        Pair.of(component, Collections.singletonList(Pair.of(policyViolation, component)));
    when(apiPolicyViolationServiceV2Mock.getTransitivePolicyViolationsByComponent(any(), any(), any(), any(), any()))
        .thenReturn(pair);
    ApiWaiverOptionsDTO apiWaiverOptionsDTO = new ApiWaiverOptionsDTO();
    apiWaiverOptionsDTO.comment = "comment";
    apiWaiverOptionsDTO.expiryTime = DateUtils.addDays(new Date(), 1);

    apiPolicyWaiverService.addWaiverToTransitivePolicyViolationsByOwnerStageComponent(OwnerType.APPLICATION,
        app.getPublicId(), BuildStageType.ID, componentIdentifier, null, null, apiWaiverOptionsDTO);

    List<PolicyWaiver> policyWaivers = policyWaiverDAO.getByOwnerId(app.getId());
    assertThat(policyWaivers).hasSize(1);
    PolicyWaiver policyWaiver = policyWaivers.get(0);
    assertThat(policyWaiver.getPolicyId()).isEqualTo(policy.getId());
    assertThat(policyWaiver.getConstraintFactsJson()).isEqualTo(policyViolation.getConstraintFactsJson());
    assertThat(policyWaiver.getHash()).isEqualTo(policyViolation.getHash());
    assertThat(policyWaiver.getComment()).isEqualTo(apiWaiverOptionsDTO.comment);
    assertThat(policyWaiver.getExpiryTime()).isEqualTo(apiWaiverOptionsDTO.expiryTime);
  }

  @Test
  public void testAddWaiverToTransitivePolicyViolationsByOwnerStageComponent_MultiplePolicyViolations_SameApp() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    Component component = new Component(componentIdentifier);
    component.setHash("hash");
    Policy policy1 = tempEntity.newPolicy(org.getId());
    Policy policy2 = tempEntity.newPolicy(org.getId());
    PolicyViolation policyViolation1 =
        tempEntity.newPolicyViolation(policyEvaluation, policy1, componentIdentifier, component.getHash());
    PolicyViolation policyViolation2 =
        tempEntity.newPolicyViolation(policyEvaluation, policy2, componentIdentifier, component.getHash());
    Pair<Component, List<Pair<PolicyViolation, Component>>> pair =
        Pair.of(component, Arrays.asList(Pair.of(policyViolation1, component), Pair.of(policyViolation2, component)));
    when(apiPolicyViolationServiceV2Mock.getTransitivePolicyViolationsByComponent(any(), any(), any(), any(), any()))
        .thenReturn(pair);
    ApiWaiverOptionsDTO apiWaiverOptionsDTO = new ApiWaiverOptionsDTO();
    apiWaiverOptionsDTO.comment = "comment";
    apiWaiverOptionsDTO.expiryTime = DateUtils.addDays(new Date(), 1);

    apiPolicyWaiverService.addWaiverToTransitivePolicyViolationsByOwnerStageComponent(OwnerType.APPLICATION,
        app.getPublicId(), BuildStageType.ID, componentIdentifier, null, null, apiWaiverOptionsDTO);

    List<PolicyWaiver> policyWaivers = policyWaiverDAO.getByOwnerId(app.getId());
    assertThat(policyWaivers).hasSize(2);
    PolicyWaiver policyWaiver1 =
        policyWaivers.stream().filter(p -> p.getPolicyId().equals(policy1.getId())).findFirst().orElse(null);
    assertThat(policyWaiver1).isNotNull();
    assertThat(policyWaiver1.getConstraintFactsJson()).isEqualTo(policyViolation1.getConstraintFactsJson());
    assertThat(policyWaiver1.getHash()).isEqualTo(policyViolation1.getHash());
    assertThat(policyWaiver1.getComment()).isEqualTo(apiWaiverOptionsDTO.comment);
    assertThat(policyWaiver1.getExpiryTime()).isEqualTo(apiWaiverOptionsDTO.expiryTime);
    PolicyWaiver policyWaiver2 =
        policyWaivers.stream().filter(p -> p.getPolicyId().equals(policy2.getId())).findFirst().orElse(null);
    assertThat(policyWaiver2).isNotNull();
    assertThat(policyWaiver2.getConstraintFactsJson()).isEqualTo(policyViolation2.getConstraintFactsJson());
    assertThat(policyWaiver2.getHash()).isEqualTo(policyViolation2.getHash());
    assertThat(policyWaiver2.getComment()).isEqualTo(apiWaiverOptionsDTO.comment);
    assertThat(policyWaiver2.getExpiryTime()).isEqualTo(apiWaiverOptionsDTO.expiryTime);
  }

  @Test
  public void testAddWaiverToTransitivePolicyViolationsByOwnerStageComponent_MultiplePolicyViolations_DifferentApps() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    Component component = new Component(componentIdentifier);
    component.setHash("hash");
    Policy policy1 = tempEntity.newPolicy(org.getId());
    Policy policy2 = tempEntity.newPolicy(org.getId());
    Application application1 = tempEntity.newApplication(org.getId());
    Application application2 = tempEntity.newApplication(org.getId());
    PolicyEvaluation policyEvaluation1 =
        tempEntity.newPolicyEvaluation(application1.getId(), BuildStageType.ID, "scanId1", new Date());
    PolicyEvaluation policyEvaluation2 =
        tempEntity.newPolicyEvaluation(application2.getId(), BuildStageType.ID, "scanId2", new Date());
    PolicyViolation policyViolation1 =
        tempEntity.newPolicyViolation(policyEvaluation1, policy1, componentIdentifier, component.getHash());
    PolicyViolation policyViolation2 =
        tempEntity.newPolicyViolation(policyEvaluation2, policy2, componentIdentifier, component.getHash());
    Pair<Component, List<Pair<PolicyViolation, Component>>> pair =
        Pair.of(component, Arrays.asList(Pair.of(policyViolation1, component), Pair.of(policyViolation2, component)));
    when(apiPolicyViolationServiceV2Mock.getTransitivePolicyViolationsByComponent(any(), any(), any(), any(), any()))
        .thenReturn(pair);
    ApiWaiverOptionsDTO apiWaiverOptionsDTO = new ApiWaiverOptionsDTO();
    apiWaiverOptionsDTO.comment = "comment";
    apiWaiverOptionsDTO.expiryTime = DateUtils.addDays(new Date(), 1);

    apiPolicyWaiverService.addWaiverToTransitivePolicyViolationsByOwnerStageComponent(OwnerType.ORGANIZATION,
        org.getPublicId(), BuildStageType.ID, componentIdentifier, null, null, apiWaiverOptionsDTO);

    List<PolicyWaiver> policyWaivers = policyWaiverDAO.getByOwnerId(org.getId());
    assertThat(policyWaivers).hasSize(2);
    PolicyWaiver policyWaiver1 =
        policyWaivers.stream().filter(p -> p.getPolicyId().equals(policy1.getId())).findFirst().orElse(null);
    assertThat(policyWaiver1).isNotNull();
    assertThat(policyWaiver1.getConstraintFactsJson()).isEqualTo(policyViolation1.getConstraintFactsJson());
    assertThat(policyWaiver1.getHash()).isEqualTo(policyViolation1.getHash());
    assertThat(policyWaiver1.getComment()).isEqualTo(apiWaiverOptionsDTO.comment);
    assertThat(policyWaiver1.getExpiryTime()).isEqualTo(apiWaiverOptionsDTO.expiryTime);
    PolicyWaiver policyWaiver2 =
        policyWaivers.stream().filter(p -> p.getPolicyId().equals(policy2.getId())).findFirst().orElse(null);
    assertThat(policyWaiver2).isNotNull();
    assertThat(policyWaiver2.getConstraintFactsJson()).isEqualTo(policyViolation2.getConstraintFactsJson());
    assertThat(policyWaiver2.getHash()).isEqualTo(policyViolation2.getHash());
    assertThat(policyWaiver2.getComment()).isEqualTo(apiWaiverOptionsDTO.comment);
    assertThat(policyWaiver2.getExpiryTime()).isEqualTo(apiWaiverOptionsDTO.expiryTime);
  }

  private void assertNotExpiringPolicyWaiver(
      String ownerId,
      String comment,
      String creatorId,
      String creatorName,
      String hash)
  {
    List<PolicyWaiver> policyWaivers = new PolicyWaiverDAO().getActiveByOwnerId(ownerId);
    assertThat(policyWaivers).isNotEmpty().hasSize(1);
    assertPolicyWaiver(policyWaivers.get(0), ownerId, comment, creatorId, creatorName, hash, null);
  }

  private void assertPolicyWaiver(PolicyWaiver policyWaiver,
                                  String ownerId,
                                  String comment,
                                  String creatorId,
                                  String creatorName,
                                  String hash,
                                  Date expiryTime)
  {
    assertThat(policyWaiver).isNotNull();
    assertThat(policyWaiver.getId()).isNotNull();
    assertThat(policyWaiver.getOwnerId()).isEqualTo(ownerId);
    assertThat(policyWaiver.getHash()).isEqualTo(hash);
    assertThat(policyWaiver.getComment()).isEqualTo(comment);
    assertThat(policyWaiver.getPolicyId()).isEqualTo(policy.getId());
    assertThat(policyWaiver.getCreateTime()).isNotNull();
    assertThat(policyWaiver.getExpiryTime()).isEqualTo(expiryTime);
    assertThat(policyWaiver.getCreatorId()).isEqualTo(creatorId);
    assertThat(policyWaiver.getCreatorName()).isEqualTo(creatorName);
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

  private void assertWaiverTelemetry(
      final OwnerType ownerType,
      final PolicyViolation policyViolation,
      final PolicyWaiver policyWaiver)
  {
    assertWaiverTelemetry(ownerType, policyViolation, policyWaiver, 0, 1);
  }

  private void assertWaiverTelemetry(
      final OwnerType ownerType,
      final PolicyViolation policyViolation,
      final PolicyWaiver policyWaiver,
      int index,
      int invocations)
  {
    final ArgumentCaptor<PolicyWaiver> policyWaiverArgumentCaptor = ArgumentCaptor.forClass(PolicyWaiver.class);
    final ArgumentCaptor<OwnerType> ownerTypeArgumentCaptor = ArgumentCaptor.forClass(OwnerType.class);
    final ArgumentCaptor<PolicyViolation> policyViolationArgumentCaptor =
        ArgumentCaptor.forClass(PolicyViolation.class);
    verify(policyWaiverTelemetryCreator, times(invocations))
        .sendWaiverTelemetryForOwnerType(policyWaiverArgumentCaptor.capture(), ownerTypeArgumentCaptor.capture(),
            policyViolationArgumentCaptor.capture());

    final PolicyWaiver policyWaiverValue = policyWaiverArgumentCaptor.getAllValues().get(index);
    final OwnerType ownerTypeValue = ownerTypeArgumentCaptor.getAllValues().get(index);
    final PolicyViolation policyViolationValue = policyViolationArgumentCaptor.getAllValues().get(index);
    assertThat(ownerTypeValue).isNotNull().isEqualTo(ownerType);
    assertThat(policyViolationValue).isNotNull();
    assertThat(policyViolationValue.getId()).isEqualTo(policyViolation.getId());
    assertThat(policyWaiverValue).isNotNull();
    assertThat(policyWaiverValue.getId()).isEqualTo(policyWaiver.getId());
  }

  @Test
  public void testGetTransitivePolicyWaiversByAppScanComponent_NotAnApp() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> apiPolicyWaiverService.getTransitivePolicyWaiversByAppScanComponent(OwnerType.ORGANIZATION, null, null,
            null, null, null)
    ).withMessageContaining("scanId can only be specified for an application.");
  }

  @Test
  public void testGetTransitivePolicyWaiversByAppScanComponent_NoComponentIdentifier_NoPackageUrl_NoHash() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> apiPolicyWaiverService.getTransitivePolicyWaiversByAppScanComponent(OwnerType.APPLICATION, null,
            null, null, null, null)
    ).withMessageContaining("componentIdentifier or packageUrl or hash must be specified.");
  }

  @Test
  public void testGetTransitivePolicyWaiversByAppScanComponent_AppNotFound() {
    String appId = "unknown";
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(
        () -> apiPolicyWaiverService.getTransitivePolicyWaiversByAppScanComponent(OwnerType.APPLICATION, appId, null,
            null, null, "hash")
    ).withMessageContaining("Could not find an application with ID " + appId + ".");
  }

  @Test
  public void testGetTransitivePolicyWaiversByAppScanComponent_ScanNotFound() {
    String scanId = "unknown";
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(
        () -> apiPolicyWaiverService.getTransitivePolicyWaiversByAppScanComponent(OwnerType.APPLICATION, app.getId(),
            scanId, null, null, "hash")
    ).withMessageContaining("scanId " + scanId + " not found for application " + app.getPublicId() + ".");
  }

  @Test
  public void testGetTransitivePolicyWaiversByAppScanComponent() {
    Application app = tempEntity.newApplicationWithParent();
    String scanId = "scanId";
    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, scanId);
    Policy policy = tempEntity.newPolicy();
    PolicyWaiver appWaiver = tempEntity.newWaiver("hash1", policy.getId(), app.getId());
    PolicyWaiver orgWaiver = tempEntity.newWaiver("hash2", policy.getId(), app.getParentOwnerId());
    PolicyWaiver rootOrgWaiver = tempEntity.newWaiver("hash3", policy.getId(), Organization.ROOT_ORGANIZATION_ID);
    List<PolicyWaiver> expectedWaivers = Arrays.asList(appWaiver, orgWaiver, rootOrgWaiver);
    List<Component> components = createComponents("hash1", "hash2", "hash3", "hash4");
    List<Component> expectedComponents = new ArrayList<>();
    expectedComponents.add(new Component());
    expectedComponents.addAll(components);
    when(apiPolicyViolationServiceV2Mock.getTransitiveComponentsByAppScanComponent(app.getId(), scanId, null, null,
        "hash")).thenReturn(components);

    ApiComponentPolicyWaiversDTO result =
        apiPolicyWaiverService.getTransitivePolicyWaiversByAppScanComponent(OwnerType.APPLICATION, app.getId(), scanId,
            null, null, "hash");

    assertApiComponentPolicyWaiversDTO(result, expectedWaivers, expectedComponents);
  }

  private List<Component> createComponents(String... hashes) {
    return Arrays.stream(hashes).map(this::createComponent).collect(Collectors.toList());
  }

  private Component createComponent(String hash) {
    Component component = new Component();
    component.setHash(hash);
    component.setDisplayName(hash + " name");
    return component;
  }

  @Test
  public void testGetPolicyWaivers_ByComponentHashes_EmptyComponentHashes() {
    Organization rootOrg = new OrganizationDAO().getByIdNotNull(Organization.ROOT_ORGANIZATION_ID);
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    List<PolicyWaiver> expectedWaivers = createPolicyWaivers(null, rootOrg, org, app);
    List<Component> expectedComponents = Collections.singletonList(new Component());
    createPolicyWaivers("hash", rootOrg, org, app);
    ApiComponentPolicyWaiversDTO result;

    result = apiPolicyWaiverService.getPolicyWaivers(app, Stream.empty());
    assertApiComponentPolicyWaiversDTO(result, expectedWaivers, expectedComponents);
    result = apiPolicyWaiverService.getPolicyWaivers(org, Stream.empty());
    assertApiComponentPolicyWaiversDTO(result, expectedWaivers.subList(0, 2), expectedComponents);
    result = apiPolicyWaiverService.getPolicyWaivers(rootOrg, Stream.empty());
    assertApiComponentPolicyWaiversDTO(result, expectedWaivers.subList(0, 1), expectedComponents);
  }

  @Test
  public void testGetPolicyWaivers_ByComponentHashes() {
    Organization rootOrg = new OrganizationDAO().getByIdNotNull(Organization.ROOT_ORGANIZATION_ID);
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    List<PolicyWaiver> waiversNullHash = createPolicyWaivers(null, rootOrg, org, app);
    List<PolicyWaiver> waiversHash = createPolicyWaivers("hash", rootOrg, org, app);
    PolicyWaiver waiverRootOrg = createPolicyWaiver("hashRootOrg", rootOrg);
    PolicyWaiver waiverOrg = createPolicyWaiver("hashOrg", org);
    PolicyWaiver waiverApp = createPolicyWaiver("hashApp", app);
    createPolicyWaivers("hash2", rootOrg, org, app);
    List<Component> components = createComponents("hash", "hashRootOrg", "hashOrg", "hashApp");
    List<Component> expectedComponents = new ArrayList<>();
    expectedComponents.add(new Component());
    expectedComponents.addAll(components);
    ApiComponentPolicyWaiversDTO result;
    List<PolicyWaiver> expectedWaivers;

    result = apiPolicyWaiverService.getPolicyWaivers(app, components.stream());
    expectedWaivers = new ArrayList<>();
    expectedWaivers.addAll(waiversNullHash);
    expectedWaivers.addAll(waiversHash);
    expectedWaivers.add(waiverRootOrg);
    expectedWaivers.add(waiverOrg);
    expectedWaivers.add(waiverApp);
    assertApiComponentPolicyWaiversDTO(result, expectedWaivers, expectedComponents);

    result = apiPolicyWaiverService.getPolicyWaivers(org, components.stream());
    expectedWaivers = new ArrayList<>();
    expectedWaivers.addAll(waiversNullHash.subList(0, 2));
    expectedWaivers.addAll(waiversHash.subList(0, 2));
    expectedWaivers.add(waiverRootOrg);
    expectedWaivers.add(waiverOrg);
    assertApiComponentPolicyWaiversDTO(result, expectedWaivers, expectedComponents);

    result = apiPolicyWaiverService.getPolicyWaivers(rootOrg, components.stream());
    expectedWaivers = new ArrayList<>();
    expectedWaivers.addAll(waiversNullHash.subList(0, 1));
    expectedWaivers.addAll(waiversHash.subList(0, 1));
    expectedWaivers.add(waiverRootOrg);
    assertApiComponentPolicyWaiversDTO(result, expectedWaivers, expectedComponents);
  }

  private List<PolicyWaiver> createPolicyWaivers(String hash, Owner... owners) {
    return Arrays.stream(owners).map(owner -> createPolicyWaiver(hash, owner)).collect(Collectors.toList());
  }

  private PolicyWaiver createPolicyWaiver(String hash, Owner owner) {
    return tempEntity.newWaiver(hash, tempEntity.newPolicy().getId(), owner.getId());
  }

  private void assertApiComponentPolicyWaiversDTO(
      ApiComponentPolicyWaiversDTO actual,
      List<PolicyWaiver> expectedWaivers,
      List<Component> expectedComponents)
  {
    assertThat(actual).isNotNull();
    assertThat(actual.componentPolicyWaivers).hasSize(expectedWaivers.size());
    for (PolicyWaiver policyWaiver : expectedWaivers) {
      ApiPolicyWaiverDTO policyWaiverDTO = actual.componentPolicyWaivers.stream()
          .filter(a -> a.policyWaiverId.equals(policyWaiver.getId()))
          .findFirst()
          .orElse(null);
      assertThat(policyWaiverDTO).isNotNull();
      assertThat(policyWaiverDTO.policyId).isEqualTo(policyWaiver.getPolicyId());
      assertThat(policyWaiverDTO.policyName).isEqualTo(policyDAO.getById(policyWaiver.getPolicyId()).getName());
      Owner owner = ownerDAO.getById(policyWaiver.getOwnerId());
      assertThat(policyWaiverDTO.scopeOwnerId).isEqualTo(owner.getId());
      assertThat(policyWaiverDTO.scopeOwnerName).isEqualTo(owner.getName());
      assertThat(policyWaiverDTO.scopeOwnerType).isEqualTo(
          ScopeOwnerUtils.getScopeOwnerType(owner.getType(), owner.getId()));
      assertThat(policyWaiverDTO.hash).isEqualTo(policyWaiver.getHash());
      assertThat(policyWaiverDTO.createTime).isEqualTo(policyWaiver.getCreateTime());
      assertThat(policyWaiverDTO.comment).isEqualTo(policyWaiver.getComment());
      assertThat(policyWaiverDTO.constraintFacts).isEqualTo(policyWaiver.getConstraintFacts());
      assertThat(policyWaiverDTO.constraintFactsJson).isEqualTo(policyWaiver.getConstraintFactsJson());
      Component expectedComponent = expectedComponents.stream()
          .filter(component -> Objects.equals(component.getHash(), policyWaiver.getHash()))
          .findFirst()
          .orElse(null);
      assertThat(expectedComponent).isNotNull();
      assertThat(policyWaiverDTO.componentName).isEqualTo(expectedComponent.getDisplayName());
    }
  }
}
