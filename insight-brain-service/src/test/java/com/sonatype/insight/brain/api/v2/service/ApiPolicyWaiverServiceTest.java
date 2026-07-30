/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import static com.sonatype.insight.brain.api.v2.service.ApiPolicyWaiverDTOTestUtils.assertApiPolicyWaiverDTO;
import static com.sonatype.insight.brain.api.v2.service.ApiPolicyWaiverService.MAX_BULK_WAIVER_VIOLATIONS;
import static com.sonatype.insight.brain.model.OwnerType.REPOSITORY_CONTAINER;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.ALL_COMPONENTS;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.ALL_VERSIONS;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.EXACT_COMPONENT;
import static com.sonatype.insight.brain.model.repository.RepositoryContainer.REPOSITORY_CONTAINER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.TriggerReference;
import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.api.v2.dto.ApiBulkWaiversDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiversApplicableToViolationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiWaiverOptionsDTO;
import com.sonatype.insight.brain.api.v2.dto.containerimagewaiver.ApiContainerImageWaiverDTO;
import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiComponentPolicyWaiversDTO;
import com.sonatype.insight.brain.api.v2.FirewallPermissionGate;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverReasonDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverRequestDAO;
import com.sonatype.insight.brain.dataaccess.policy.ProxyRepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.owner.OwnerService;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.AbstractPolicyViolation;
import com.sonatype.insight.brain.model.policy.InvalidStageException;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiverReason;
import com.sonatype.insight.brain.model.policy.ProxyRepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.actions.WarnActionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.PolicyWaiverTelemetryCreator;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.utils.ScopeOwnerUtils;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import com.sonatype.insight.test.LogOutput;
import jakarta.inject.Inject;
import java.time.Duration;
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
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class ApiPolicyWaiverServiceTest
    extends AbstractComponentTest
{
  private static final String ACKNOWLEDGED_VIOLATION_REASON_ID = "9b704ef5bc064fc29d7fe08a251ee9a6";

  private static final String ACKNOWLEDGED_VIOLATION_REASON_TEXT = "Acknowledged violation";

  @Inject
  private PolicyViolationDAO policyViolationDAO;

  @Inject
  private ProxyRepositoryPolicyViolationDAO proxyRepositoryPolicyViolationDAO;

  @Inject
  private PolicyWaiverDAO policyWaiverDAO;

  @Inject
  private PolicyWaiverReasonDAO policyWaiverReasonDAO;

  @Inject
  private OrganizationDAO organizationDAO;

  @Inject
  private ApiPolicyWaiverService apiPolicyWaiverService;

  private Policy policy;

  private PolicyEvaluation policyEvaluation;

  private PolicyViolation policyViolation;

  private PackageUrlIdentifier componentPurl;

  private Application app;

  private Organization org;

  @Inject
  private PolicyDAO policyDAO;

  @Inject
  private OwnerDAO ownerDAO;

  @Inject
  private RepositoryDAO repositoryDAO;

  @Mock
  private TelemetrySender telemetrySenderMock;

  @Mock
  private ApiPolicyViolationServiceV2 apiPolicyViolationServiceV2Mock;

  @Mock
  private PolicyWaiverTelemetryCreator policyWaiverTelemetryCreator;

  @Rule
  public LogOutput logOutput = new LogOutput(ApiPolicyWaiverService.class);

  @Before
  public void setUpPolicyViolation() {
    org = tempEntity.newOrganization();
    app = tempEntity.newApplication(org.getId());
    policy = tempEntity.newPolicy(org.getId());

    policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId1App1");

    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "java");
    policyViolation =
        tempEntity.newPolicyViolation(policyEvaluation, policy, componentIdentifier, "h1", "r1");
    componentPurl = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier);
  }

  /**
   * @deprecated The tested method is deprecated
   */
  @Deprecated
  @Test
  public void testAddPolicyWaiver_Application() {
    apiPolicyWaiverService.addPolicyWaiver(policyViolation.getId(), OwnerType.APPLICATION, "waiver comment");
    assertNotExpiringPolicyWaiver(app.getId(), "waiver comment", "testuser", "Test User", policyViolation.getHash(),
        policyViolation.getConstraintFactsJson(), EXACT_COMPONENT, componentPurl.getPackageUrl(), null);
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
    assertNotExpiringPolicyWaiver(org.getId(), "waiver comment", "testuser", "Test User", policyViolation.getHash(),
        policyViolation.getConstraintFactsJson(), EXACT_COMPONENT, componentPurl.getPackageUrl(), null);
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
    assertNotExpiringPolicyWaiver(app.getId(), null, "testuser", "Test User", policyViolation.getHash(),
        policyViolation.getConstraintFactsJson(), EXACT_COMPONENT, componentPurl.getPackageUrl(), null);
  }

  /**
   * @deprecated The tested method is deprecated
   */
  @Deprecated
  @Test
  public void testAddPolicyWaiver_InvalidPolicyViolationId() {
    assertThatThrownBy(() -> apiPolicyWaiverService.addPolicyWaiver("invalid-policyViolationId", OwnerType.APPLICATION,
        "waiver comment")).isInstanceOf(NotFoundException.class)
            .hasMessage("Could not find policy violation with ID invalid-policyViolationId.");
  }

  /**
   * @deprecated The tested method is deprecated
   */
  @Deprecated
  @Test
  public void testAddPolicyWaiver_InvalidOwnerType() {
    assertThatThrownBy(
        () -> apiPolicyWaiverService.addPolicyWaiver(policyViolation.getId(), OwnerType.REPOSITORY, "waiver comment"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Unknown owner type: repository");
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_Application() {
    apiPolicyWaiverService.addPolicyWaiverByPolicyViolationId(OwnerType.APPLICATION, app.getId(),
        policyViolation.getId(), new ApiWaiverOptionsDTO("waiver comment", EXACT_COMPONENT, null, null, false));
    assertNotExpiringPolicyWaiver(app.getId(), "waiver comment", "testuser", "Test User", policyViolation.getHash(),
        policyViolation.getConstraintFactsJson(), EXACT_COMPONENT, componentPurl.getPackageUrl(), null);
    assertTelemetry(OwnerType.APPLICATION, app.getId());
    assertWaiverTelemetry(OwnerType.APPLICATION, policyViolation, policyWaiverDAO.getByOwnerId(app.getId()).get(0));
  }

  @Test
  public void testAddPolicyWaiverWithWaiverReason() {
    String waiverReasonId = policyWaiverReasonDAO.getAll().get(0).getId();
    apiPolicyWaiverService.addPolicyWaiverByPolicyViolationId(OwnerType.APPLICATION, app.getId(),
        policyViolation.getId(),
        new ApiWaiverOptionsDTO("waiver comment", EXACT_COMPONENT, null, waiverReasonId, false));
    assertNotExpiringPolicyWaiver(app.getId(), "waiver comment", "testuser", "Test User", policyViolation.getHash(),
        policyViolation.getConstraintFactsJson(), EXACT_COMPONENT, componentPurl.getPackageUrl(), waiverReasonId);
    assertTelemetry(OwnerType.APPLICATION, app.getId());
    assertWaiverTelemetry(OwnerType.APPLICATION, policyViolation, policyWaiverDAO.getByOwnerId(app.getId()).get(0));
  }

  @Test
  public void testAddPolicyWaiverWithExpireWhenRemediationAvailable() {
    apiPolicyWaiverService.addPolicyWaiverByPolicyViolationId(OwnerType.APPLICATION, app.getId(),
        policyViolation.getId(), new ApiWaiverOptionsDTO("waiver comment", EXACT_COMPONENT, null, null, true));
    assertNotExpiringPolicyWaiver(app.getId(), "waiver comment", "testuser", "Test User", policyViolation.getHash(),
        policyViolation.getConstraintFactsJson(), EXACT_COMPONENT, componentPurl.getPackageUrl(), null, true);
    assertTelemetry(OwnerType.APPLICATION, app.getId());
    assertWaiverTelemetry(OwnerType.APPLICATION, policyViolation, policyWaiverDAO.getByOwnerId(app.getId()).get(0));
  }

  @Test
  public void testAddPolicyWaiverWithWrongWaiverReason() {
    assertThatThrownBy(() -> apiPolicyWaiverService.addPolicyWaiverByPolicyViolationId(OwnerType.APPLICATION,
        app.getId(),
        policyViolation.getId(), new ApiWaiverOptionsDTO("waiver comment", EXACT_COMPONENT, null, "WrongId", false)))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Waiver reason not found");
  }

  @Test
  public void testAddPolicyWaiverWithExpireWhenRemediationAvailableAndAllComponents() {
    assertThatThrownBy(
        () -> apiPolicyWaiverService.addPolicyWaiverByPolicyViolationId(OwnerType.APPLICATION, app.getId(),
            policyViolation.getId(), new ApiWaiverOptionsDTO("waiver comment", ALL_COMPONENTS, null, null, true)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Expire When Remediation Available Waivers can only be applied to Exact Components.");
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_ApplicationPublicId() {
    apiPolicyWaiverService.addPolicyWaiverByPolicyViolationId(OwnerType.APPLICATION, app.getPublicId(),
        policyViolation.getId(), new ApiWaiverOptionsDTO("waiver comment", EXACT_COMPONENT, null, null, false));

    assertNotExpiringPolicyWaiver(app.getId(), "waiver comment", "testuser", "Test User", policyViolation.getHash(),
        policyViolation.getConstraintFactsJson(), EXACT_COMPONENT, componentPurl.getPackageUrl(), null);
    assertTelemetry(OwnerType.APPLICATION, app.getId());
    assertWaiverTelemetry(OwnerType.APPLICATION, policyViolation, policyWaiverDAO.getByOwnerId(app.getId()).get(0));
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_NonParentApplication() {
    Application otherApp = tempEntity.newApplication(org.getId());

    assertThatThrownBy(
        () -> apiPolicyWaiverService.addPolicyWaiverByPolicyViolationId(OwnerType.APPLICATION, otherApp.getId(),
            policyViolation.getId(), new ApiWaiverOptionsDTO("waiver comment", EXACT_COMPONENT,
                null, null, false))).isInstanceOf(BadRequestException.class)
                    .hasMessage("Invalid owner id: " + otherApp.getId());
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_NonParentApplicationPublicId() {
    Application otherApp = tempEntity.newApplication(org.getId());

    assertThatThrownBy(
        () -> apiPolicyWaiverService.addPolicyWaiverByPolicyViolationId(OwnerType.APPLICATION, otherApp.getPublicId(),
            policyViolation.getId(), new ApiWaiverOptionsDTO("waiver comment", EXACT_COMPONENT,
                null, null, false))).isInstanceOf(BadRequestException.class)
                    .hasMessage("Invalid owner id: " + otherApp.getPublicId());
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_Organization() {
    apiPolicyWaiverService.addPolicyWaiverByPolicyViolationId(OwnerType.ORGANIZATION, org.getId(),
        policyViolation.getId(), new ApiWaiverOptionsDTO("waiver comment", EXACT_COMPONENT, null, null, false));

    assertNotExpiringPolicyWaiver(org.getId(), "waiver comment", "testuser", "Test User", policyViolation.getHash(),
        policyViolation.getConstraintFactsJson(), EXACT_COMPONENT, componentPurl.getPackageUrl(), null);
    assertTelemetry(OwnerType.ORGANIZATION, org.getId());
    assertWaiverTelemetry(OwnerType.ORGANIZATION, policyViolation, policyWaiverDAO.getByOwnerId(org.getId()).get(0));
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_NonParentOrganization() {
    Organization otherOrg = tempEntity.newOrganization();

    assertThatThrownBy(
        () -> apiPolicyWaiverService.addPolicyWaiverByPolicyViolationId(OwnerType.ORGANIZATION, otherOrg.getId(),
            policyViolation.getId(), new ApiWaiverOptionsDTO("waiver comment", EXACT_COMPONENT,
                null, null, false))).isInstanceOf(BadRequestException.class)
                    .hasMessage("Invalid owner id: " + otherOrg.getId());
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_RootOrganization() {
    apiPolicyWaiverService.addPolicyWaiverByPolicyViolationId(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID,
        policyViolation.getId(), new ApiWaiverOptionsDTO("waiver comment", EXACT_COMPONENT, null, null, false));

    assertNotExpiringPolicyWaiver(Organization.ROOT_ORGANIZATION_ID, "waiver comment", "testuser",
        "Test User", policyViolation.getHash(), policyViolation.getConstraintFactsJson(), EXACT_COMPONENT,
        componentPurl.getPackageUrl(), null);
    assertTelemetry(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID);
    assertWaiverTelemetry(OwnerType.ORGANIZATION, policyViolation,
        policyWaiverDAO.getByOwnerId(Organization.ROOT_ORGANIZATION_ID).get(0));
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_NullComment() {
    apiPolicyWaiverService.addPolicyWaiverByPolicyViolationId(OwnerType.APPLICATION, app.getId(),
        policyViolation.getId(), new ApiWaiverOptionsDTO(null, EXACT_COMPONENT, null, null, false));

    assertNotExpiringPolicyWaiver(app.getId(), null, "testuser", "Test User", policyViolation.getHash(),
        policyViolation.getConstraintFactsJson(), EXACT_COMPONENT, componentPurl.getPackageUrl(), null);
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_ApplyToAllComponents() {
    apiPolicyWaiverService.addPolicyWaiverByPolicyViolationId(OwnerType.APPLICATION, app.getId(),
        policyViolation.getId(), new ApiWaiverOptionsDTO("waiver comment", ALL_COMPONENTS, null, null, false));

    assertNotExpiringPolicyWaiver(app.getId(), "waiver comment", "testuser", "Test User", null,
        policyViolation.getConstraintFactsJson(), ALL_COMPONENTS, null,
        null /* all components waivers don't have purl */);
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_InvalidPolicyViolationId() {
    assertThatThrownBy(() -> apiPolicyWaiverService.addPolicyWaiverByPolicyViolationId(OwnerType.APPLICATION,
        app.getId(), "invalid-policyViolationId", new ApiWaiverOptionsDTO(null, EXACT_COMPONENT, null, null, false)))
            .isInstanceOf(NotFoundException.class)
            .hasMessage("Could not find policy violation with ID invalid-policyViolationId.");
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_WithExpiry_InTheFuture() {
    Date expiryTime = DateUtils.addDays(new Date(), 1);

    apiPolicyWaiverService.addPolicyWaiverByPolicyViolationId(OwnerType.APPLICATION, app.getId(),
        policyViolation.getId(), new ApiWaiverOptionsDTO("waiver comment", ALL_COMPONENTS, expiryTime, null, false));

    List<PolicyWaiver> policyWaivers = policyWaiverDAO.getActiveByOwnerId(app.getId());
    assertThat(policyWaivers).isNotEmpty().hasSize(1);
    assertPolicyWaiver(policyWaivers.get(0), app.getId(), "waiver comment", "testuser", "Test User", null,
        policyViolation.getConstraintFactsJson(), expiryTime,
        ALL_COMPONENTS, null, null, false);
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_WithExpiry_Null() {
    apiPolicyWaiverService.addPolicyWaiverByPolicyViolationId(OwnerType.APPLICATION, app.getId(),
        policyViolation.getId(), new ApiWaiverOptionsDTO("waiver comment", ALL_COMPONENTS, null, null, false));

    assertNotExpiringPolicyWaiver(app.getId(), "waiver comment", "testuser", "Test User", null,
        policyViolation.getConstraintFactsJson(), ALL_COMPONENTS, null, null);
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_WithPackageUrl() {
    apiPolicyWaiverService.addPolicyWaiverByPolicyViolationId(OwnerType.APPLICATION, app.getId(),
        policyViolation.getId(), new ApiWaiverOptionsDTO("waiver comment", ALL_VERSIONS, null, null, false));

    List<PolicyWaiver> policyWaivers = policyWaiverDAO.getActiveByOwnerId(app.getId());
    assertThat(policyWaivers).isNotEmpty().hasSize(1);
    PolicyWaiver policyWaiver = policyWaivers.get(0);
    assertThat(policyWaiver.getAssociatedPackageUrl()).isNotBlank();
    assertPolicyWaiver(policyWaiver, app.getId(), "waiver comment", "testuser", "Test User", policyWaiver.getHash(),
        policyViolation.getConstraintFactsJson(), null, policyWaiver.getComponentMatchStrategy(),
        policyWaiver.getAssociatedPackageUrl(), null, false);
    assertThat(policyWaiver.getComponentIdentifier().getCoordinates())
        .hasSize(5)
        .isEqualTo(new TreeMap<String, String>()
        {
          {
            this.put("artifactId", "a1");
            this.put("groupId", "g1");
            this.put("version", "v1");
            this.put("classifier", "c1");
            this.put("extension", "java");
          }
        });
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_AllVersionsWithoutComponentIdentifier() {
    policyViolation.setComponentIdentifier(null);
    policyViolationDAO.update(policyViolation);

    assertThatThrownBy(() -> apiPolicyWaiverService.addPolicyWaiverByPolicyViolationId(OwnerType.APPLICATION,
        app.getId(), policyViolation.getId(), new ApiWaiverOptionsDTO("waiver comment", ALL_VERSIONS, null, null,
            false)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Cannot create an ALL_VERSIONS waiver for a component that could not be identified.");

    assertThat(policyWaiverDAO.getActiveByOwnerId(app.getId())).isEmpty();
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_WithoutPackageUrl() {
    apiPolicyWaiverService.addPolicyWaiverByPolicyViolationId(OwnerType.APPLICATION, app.getId(),
        policyViolation.getId(), new ApiWaiverOptionsDTO("waiver comment", ALL_COMPONENTS, null, null, false));

    List<PolicyWaiver> policyWaivers = policyWaiverDAO.getActiveByOwnerId(app.getId());
    assertThat(policyWaivers).isNotEmpty().hasSize(1);
    PolicyWaiver policyWaiver = policyWaivers.get(0);
    assertThat(policyWaiver.getAssociatedPackageUrl()).isNull();
    assertPolicyWaiver(policyWaiver, app.getId(), "waiver comment", "testuser", "Test User", null,
        policyViolation.getConstraintFactsJson(), null, policyWaiver.getComponentMatchStrategy(), null, null, false);
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_DuplicatedAllVersions() {
    apiPolicyWaiverService.addPolicyWaiverByPolicyViolationId(OwnerType.APPLICATION, app.getId(),
        policyViolation.getId(), new ApiWaiverOptionsDTO("waiver comment", ALL_VERSIONS, null, null, false));

    PolicyViolation policyViolation1 =
        tempEntity.newPolicyViolation(policyEvaluation, policy, "g1", "a1", "v2", "c1", "java", "h1", "r1");
    PolicyViolation policyViolation2 =
        tempEntity.newPolicyViolation(policyEvaluation, policy, "g2", "a2", "v1", "c1", "java", "h1", "r1");

    assertThatThrownBy(
        () -> apiPolicyWaiverService.addPolicyWaiverByPolicyViolationId(OwnerType.APPLICATION, app.getId(),
            policyViolation1.getId(), new ApiWaiverOptionsDTO("waiver comment", ALL_VERSIONS, null, null, false)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("This policy waiver already exists.");

    apiPolicyWaiverService.addPolicyWaiverByPolicyViolationId(OwnerType.APPLICATION, app.getId(),
        policyViolation2.getId(), new ApiWaiverOptionsDTO("waiver comment", ALL_VERSIONS, null, null, false));

    List<PolicyWaiver> policyWaivers = policyWaiverDAO.getActiveByOwnerId(app.getId());
    assertThat(policyWaivers).isNotEmpty().hasSize(2);
    assertThat(policyWaivers.get(0).getAssociatedPackageUrl())
        .isNotEqualTo(policyWaivers.get(1).getAssociatedPackageUrl());
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_WithExpiry_InThePast() {
    Date yesterday = DateUtils.addDays(new Date(), -1);

    assertThatThrownBy(() -> {
      apiPolicyWaiverService.addPolicyWaiverByPolicyViolationId(OwnerType.APPLICATION, app.getId(),
          policyViolation.getId(), new ApiWaiverOptionsDTO("waiver comment", ALL_VERSIONS, yesterday, null, false));
    }).isInstanceOf(BadRequestException.class).hasMessage("Expiration date must be in the future.");
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_WithExpiry_Today() {
    assertThatThrownBy(() -> {
      apiPolicyWaiverService.addPolicyWaiverByPolicyViolationId(OwnerType.APPLICATION, app.getId(),
          policyViolation.getId(), new ApiWaiverOptionsDTO("waiver comment", ALL_VERSIONS, new Date(), null, false));
    }).isInstanceOf(BadRequestException.class).hasMessage("Expiration date must be in the future.");
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
            .hasMessage(
                "Cannot find a policy waiver with ID " + policyWaiver.getId() + " for " + OwnerType.ORGANIZATION +
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
  public void testGetPolicyWaivers_Application_MultipleWaiversSharePolicyData() {
    Application application = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(application);
    PolicyWaiver waiver1 = tempEntity.newWaiver("hash1", policy.getId(), application.getId(), "comment1");
    PolicyWaiver waiver2 = tempEntity.newWaiver("hash2", policy.getId(), application.getId(), "comment2");
    PolicyWaiver waiver3 = tempEntity.newWaiver("hash3", policy.getId(), application.getId(), "comment3");

    List<ApiPolicyWaiverDTO> policyWaiverDtoList =
        apiPolicyWaiverService.getPolicyWaivers(OwnerType.APPLICATION, application.getId());

    assertThat(policyWaiverDtoList).hasSize(3);
    Map<String, ApiPolicyWaiverDTO> dtosByWaiverId = policyWaiverDtoList.stream()
        .collect(Collectors.toMap(dto -> dto.policyWaiverId, dto -> dto));
    for (PolicyWaiver waiver : List.of(waiver1, waiver2, waiver3)) {
      ApiPolicyWaiverDTO actual = dtosByWaiverId.get(waiver.getId());
      assertThat(actual).isNotNull();
      assertThat(actual.policyName).isEqualTo(policy.getName());
      assertThat(actual.threatLevel).isEqualTo(policy.getThreatLevel());
    }
  }

  @Test
  public void testGetPolicyWaivers_Application_DoesNotQueryPolicyPerWaiver() {
    Application application = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(application);
    tempEntity.newWaiver("hash1", policy.getId(), application.getId(), "comment1");
    tempEntity.newWaiver("hash2", policy.getId(), application.getId(), "comment2");
    tempEntity.newWaiver("hash3", policy.getId(), application.getId(), "comment3");

    PolicyDAO policyDAOSpy = spy(policyDAO);
    ApiPolicyWaiverService serviceWithSpiedPolicyDAO = newServiceWithPolicyDAO(policyDAOSpy);

    List<ApiPolicyWaiverDTO> policyWaiverDtoList =
        serviceWithSpiedPolicyDAO.getPolicyWaivers(OwnerType.APPLICATION, application.getId());

    assertThat(policyWaiverDtoList).hasSize(3);
    verify(policyDAOSpy, times(1)).getByIds(any());
    verify(policyDAOSpy, never()).getById(any(TransactionContext.class), any());
  }

  private ApiPolicyWaiverService newServiceWithPolicyDAO(PolicyDAO policyDAOOverride) {
    return new ApiPolicyWaiverService(
        telemetrySenderMock,
        policyWaiverDAO,
        policyDAOOverride,
        lookup(ApplicationDAO.class),
        ownerDAO,
        lookup(PolicyEvaluationDAO.class),
        apiPolicyViolationServiceV2Mock,
        policyWaiverTelemetryCreator,
        lookup(CurrentUser.class),
        lookup(OwnerService.class),
        proxyRepositoryPolicyViolationDAO,
        policyViolationDAO,
        lookup(PolicyWaiverRequestDAO.class),
        organizationDAO,
        policyWaiverReasonDAO,
        repositoryDAO,
        lookup(IdUtils.class),
        lookup(TelemetryUtils.class),
        lookup(FirewallPermissionGate.class));
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
    // Post NEXUS-53680: the virtual REPOSITORY_CONTAINER_ID scope is a query-key only. Container-image
    // waivers are stored under their container-image APPLICATION id and each DTO renders with the
    // underlying application scope (not "Repository Managers"). See buildPolicyWaiverDTOsPerOwner.
    Application containerImageApp = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy();
    PolicyWaiver policyWaiver = tempEntity.newWaiver(
        new PolicyWaiver("hash", policy.getId(), containerImageApp.getId(), "comment")
            .setForContainerImage(true));

    List<ApiPolicyWaiverDTO> policyWaiverDtoList =
        apiPolicyWaiverService.getPolicyWaivers(REPOSITORY_CONTAINER, REPOSITORY_CONTAINER_ID);

    assertThat(policyWaiverDtoList).hasSize(1);
    ApiPolicyWaiverDTO actual = policyWaiverDtoList.get(0);
    assertThat(actual.policyWaiverId).isEqualTo(policyWaiver.getId());
    assertThat(actual.comment).isEqualTo(policyWaiver.getComment());
    assertThat(actual.createTime).isEqualTo(policyWaiver.getCreateTime());
    assertThat(actual.hash).isEqualTo(policyWaiver.getHash());
    assertThat(actual.policyId).isEqualTo(policyWaiver.getPolicyId());
    assertThat(actual.scopeOwnerId).isEqualTo(containerImageApp.getId());
    assertThat(actual.scopeOwnerName).isEqualTo(containerImageApp.getName());
    assertThat(actual.scopeOwnerType).isEqualTo("application");
  }

  @Test
  public void testGetPolicyWaivers_RepositoryContainer_ExcludesExpiredWaiver() {
    Instant now = Instant.now();
    Date today = Date.from(now);
    Date yesterday = Date.from(now.minus(1, ChronoUnit.DAYS));
    Date aWeekFromNow = Date.from(now.plus(7, ChronoUnit.DAYS));
    Date aWeekAgo = Date.from(now.minus(7, ChronoUnit.DAYS));

    // Post NEXUS-53680: container-image waivers surface via REPOSITORY_CONTAINER but are stored
    // under (and rendered against) their container-image APPLICATION.
    Application containerImageApp = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy();
    PolicyWaiver policyWaiver = tempEntity.newWaiver(
        new PolicyWaiver("hash", policy.getId(), containerImageApp.getId(), "comment")
            .setForContainerImage(true)
            .setCreateTime(today)
            .setExpiryTime(aWeekFromNow));
    tempEntity.newWaiver(
        new PolicyWaiver(null, policy.getId(), containerImageApp.getId(), "comment")
            .setForContainerImage(true)
            .setCreateTime(aWeekAgo)
            .setExpiryTime(yesterday));

    List<ApiPolicyWaiverDTO> policyWaiverDtoList =
        apiPolicyWaiverService.getPolicyWaivers(REPOSITORY_CONTAINER, REPOSITORY_CONTAINER_ID);

    assertThat(policyWaiverDtoList).hasSize(1);
    ApiPolicyWaiverDTO actual = policyWaiverDtoList.get(0);
    assertThat(actual.policyWaiverId).isEqualTo(policyWaiver.getId());
    assertThat(actual.comment).isEqualTo(policyWaiver.getComment());
    assertThat(actual.createTime).isEqualTo(policyWaiver.getCreateTime());
    assertThat(actual.hash).isEqualTo(policyWaiver.getHash());
    assertThat(actual.policyId).isEqualTo(policyWaiver.getPolicyId());
    assertThat(actual.scopeOwnerId).isEqualTo(containerImageApp.getId());
    assertThat(actual.scopeOwnerName).isEqualTo(containerImageApp.getName());
    assertThat(actual.scopeOwnerType).isEqualTo("application");
    assertThat(actual.expiryTime).isEqualTo(aWeekFromNow);
  }

  @Test
  public void testGetPolicyWaivers_RepositoryContainer_MultipleWaiversSharePolicyData() {
    Application containerImageApp = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy();
    PolicyWaiver waiver1 = tempEntity.newWaiver(
        new PolicyWaiver("hash1", policy.getId(), containerImageApp.getId(), "comment1")
            .setForContainerImage(true));
    PolicyWaiver waiver2 = tempEntity.newWaiver(
        new PolicyWaiver("hash2", policy.getId(), containerImageApp.getId(), "comment2")
            .setForContainerImage(true));

    List<ApiPolicyWaiverDTO> policyWaiverDtoList =
        apiPolicyWaiverService.getPolicyWaivers(REPOSITORY_CONTAINER, REPOSITORY_CONTAINER_ID);

    assertThat(policyWaiverDtoList).hasSize(2);
    Map<String, ApiPolicyWaiverDTO> dtosByWaiverId = policyWaiverDtoList.stream()
        .collect(Collectors.toMap(dto -> dto.policyWaiverId, dto -> dto));
    for (PolicyWaiver waiver : List.of(waiver1, waiver2)) {
      ApiPolicyWaiverDTO actual = dtosByWaiverId.get(waiver.getId());
      assertThat(actual).isNotNull();
      assertThat(actual.policyName).isEqualTo(policy.getName());
      assertThat(actual.threatLevel).isEqualTo(policy.getThreatLevel());
    }
  }

  @Test
  public void testGetPolicyWaivers_RepositoryContainer_DoesNotQueryPolicyPerWaiver() {
    Application containerImageApp = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy();
    tempEntity.newWaiver(
        new PolicyWaiver("hash1", policy.getId(), containerImageApp.getId(), "comment1")
            .setForContainerImage(true));
    tempEntity.newWaiver(
        new PolicyWaiver("hash2", policy.getId(), containerImageApp.getId(), "comment2")
            .setForContainerImage(true));
    tempEntity.newWaiver(
        new PolicyWaiver("hash3", policy.getId(), containerImageApp.getId(), "comment3")
            .setForContainerImage(true));

    PolicyDAO policyDAOSpy = spy(policyDAO);
    ApiPolicyWaiverService serviceWithSpiedPolicyDAO = newServiceWithPolicyDAO(policyDAOSpy);

    List<ApiPolicyWaiverDTO> policyWaiverDtoList =
        serviceWithSpiedPolicyDAO.getPolicyWaivers(REPOSITORY_CONTAINER, REPOSITORY_CONTAINER_ID);

    assertThat(policyWaiverDtoList).hasSize(3);
    verify(policyDAOSpy, times(1)).getByIds(any());
    verify(policyDAOSpy, never()).getById(any(TransactionContext.class), any());
  }

  @Test
  public void testGetPolicyWaivers_Expired() {
    Date now = new Date();

    Application application = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(application);
    tempEntity.newWaiver("hash", policy.getId(), application.getId(), null, "comment", now, now);

    List<ApiPolicyWaiverDTO> policyWaiverDtoList =
        apiPolicyWaiverService.getPolicyWaivers(OwnerType.APPLICATION, application.getId());

    assertThat(policyWaiverDtoList).isEmpty();
  }

  @Test
  public void testGetPolicyWaivers_ExpiringInFuture() {
    Date now = new Date();
    Date oneHourLater = DateUtils.addHours(now, 1);

    Application application = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(application);
    PolicyWaiver policyWaiver = tempEntity.newWaiver("hash", policy.getId(), application.getId(), null, "comment",
        now, oneHourLater); // expiring in future

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
  public void testGetPolicyWaivers_WithWaiverReason() {
    Application application = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(application);
    PolicyWaiver policyWaiver = tempEntity.newWaiverWithReason("hash", policy.getId(), application.getId(), null,
        "comment", "system", "because reasons"); // expiring in future

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
    assertThat(actual.reasonText).isEqualTo("because reasons");
  }

  @Test
  public void testGetApplicableWaivers_NullId() {
    assertThatThrownBy(() -> apiPolicyWaiverService.getApplicableWaivers(null)).isInstanceOf(NotFoundException.class)
        .hasMessage("Could not find policy violation with ID null.");
  }

  @Test
  public void testGetApplicableWaivers_InvalidId() {
    assertThatThrownBy(() -> apiPolicyWaiverService.getApplicableWaivers("InvalidPolicyViolationId"))
        .isInstanceOf(NotFoundException.class)
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
  public void testGetApplicableWaivers_Application() {
    Date now = new Date();
    List<ConstraintFact> constraintFacts = tempEntity.createArbitraryConstraintFacts();
    List<ConstraintFact> constraintFacts2 = tempEntity.createArbitraryConstraintFacts();
    Organization newOrg = tempEntity.newOrganization("NewOrg");
    Application newApp = tempEntity.newApplication("NewApp", "AppPublicId", newOrg.getId());
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(newApp.getId(), BuildStageType.ID, "scanId");
    Policy policy = tempEntity.newPolicy(newOrg);
    Policy policy2 = tempEntity.newPolicy(newApp);
    ComponentIdentifier identifier =
        ComponentIdentifier.createMavenCoordinates("group", "artifact", "1.0", "c1", "jar");
    ComponentIdentifier identifierAllVersions =
        ComponentIdentifier.createMavenCoordinates("group", "artifact", "2.0", "c1", "jar");
    ComponentIdentifier identifierAllVersions2 =
        ComponentIdentifier.createMavenCoordinates("group", "otherArtifact", "3.0", "c1", "jar");
    String packageUrlAllVersions = PackageUrlIdentifier.toPackageUrl(identifierAllVersions);
    String packageUrlAllVersions2 = PackageUrlIdentifier.toPackageUrl(identifierAllVersions2);
    PolicyViolation violation = tempEntity.newPolicyViolation(evaluation, policy, identifier, "hash");
    violation.setConstraintFacts(constraintFacts);
    policyViolationDAO.update(violation);

    String policyId = policy.getId();
    String policy2Id = policy2.getId();
    String orgId = newOrg.getId();
    String appId = newApp.getId();

    Date expiredExpiryTime = DateUtils.addMilliseconds(now, -1);
    Date expiringInFutureExpiryTime = DateUtils.addMinutes(now, 1);

    // applicable waivers that the service should return for the given violation
    PolicyWaiverReason policyWaiverReason = tempEntity.newWaiverReason("reasonType", "some reason");
    tempEntity.newWaiver("hashX", policyId, orgId, constraintFacts, packageUrlAllVersions, ALL_VERSIONS, "",
        policyWaiverReason, DateUtils.addDays(now, -10));
    tempEntity.newWaiver(null, policyId, orgId, constraintFacts, ALL_COMPONENTS, "", DateUtils.addDays(now, -9), null);
    tempEntity.newWaiver("hash", policyId, appId, constraintFacts, packageUrlAllVersions2, EXACT_COMPONENT, "",
        DateUtils.addDays(now, -8), expiredExpiryTime); // expired
    tempEntity.newWaiver(null, policyId, appId, constraintFacts, ALL_COMPONENTS, "A comment",
        DateUtils.addDays(now, -7), expiringInFutureExpiryTime); // expiring in the future
    // add more waivers with different attributes — for diversity
    tempEntity.newWaiver("hash", policyId, appId, null, EXACT_COMPONENT, "", DateUtils.addDays(now, -6));
    tempEntity.newWaiver(null, policyId, appId, null, packageUrlAllVersions2, ALL_VERSIONS, "",
        DateUtils.addDays(now, -5));
    tempEntity.newWaiver("hashX", policyId, appId, constraintFacts, EXACT_COMPONENT, "", DateUtils.addDays(now, -4));
    tempEntity.newWaiver("hash", policyId, appId, constraintFacts2, EXACT_COMPONENT, "", DateUtils.addDays(now, -3));
    tempEntity.newWaiver("hash2", policy2Id, appId, null, EXACT_COMPONENT, "", DateUtils.addDays(now, -2), null);
    tempEntity.newWaiver(null, policy2Id, appId, null, ALL_COMPONENTS, "", DateUtils.addDays(now, -1),
        expiringInFutureExpiryTime);
    tempEntity.newWaiver("hash", policy2Id, appId, constraintFacts, EXACT_COMPONENT, "", now, expiredExpiryTime);

    String policyViolationId = violation.getId();

    ApiPolicyWaiversApplicableToViolationDTO dto = apiPolicyWaiverService.getApplicableWaivers(policyViolationId);

    // activeWaivers - results sorted to have deterministic ordering in the test
    List<ApiPolicyWaiverDTO> activeApplicableWaivers = dto.activeWaivers.stream()
        .sorted(Comparator.comparing(apiPolicyWaiverDTO -> apiPolicyWaiverDTO.createTime))
        .collect(Collectors.toList());

    assertThat(activeApplicableWaivers.size()).isEqualTo(3);
    assertApiPolicyWaiverDTO("hashX", policyId, orgId, "NewOrg", "", policyViolationId,
        null, "testuser", "Test User", ALL_VERSIONS, packageUrlAllVersions, policyWaiverReason.getReasonText(),
        policyWaiverReason.getId(), activeApplicableWaivers.get(0));
    assertApiPolicyWaiverDTO(null, policyId, orgId, "NewOrg", "", policyViolationId,
        null, "testuser", "Test User", ALL_COMPONENTS, null, null, null, activeApplicableWaivers.get(1));
    assertApiPolicyWaiverDTO(null, policyId, appId, "NewApp", "A comment", policyViolationId,
        expiringInFutureExpiryTime, "testuser", "Test User", ALL_COMPONENTS, null, null, null,
        activeApplicableWaivers.get(2));

    // expiredWaivers
    List<ApiPolicyWaiverDTO> expiredApplicableWaivers = dto.expiredWaivers;

    assertThat(expiredApplicableWaivers.size()).isEqualTo(1);
    assertApiPolicyWaiverDTO("hash", policyId, appId, "NewApp", "", policyViolationId, expiredExpiryTime, "testuser",
        "Test User", EXACT_COMPONENT, packageUrlAllVersions2, null, null, expiredApplicableWaivers.get(0));
  }

  @Test
  public void testGetApplicableWaivers_Repository() {
    Date now = new Date();
    List<ConstraintFact> constraintFacts = tempEntity.createArbitraryConstraintFacts();
    List<ConstraintFact> constraintFacts2 = tempEntity.createArbitraryConstraintFacts();
    Repository repository = tempEntity.newRepository();
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);
    Policy policy2 = tempEntity.newPolicy(REPOSITORY_CONTAINER_ID);
    ComponentIdentifier identifier =
        ComponentIdentifier.createMavenCoordinates("group", "artifact", "1.0", "c1", "jar");
    ComponentIdentifier identifierAllVersions =
        ComponentIdentifier.createMavenCoordinates("group", "artifact", "*", "c1", "jar");
    ComponentIdentifier identifierAllVersions2 =
        ComponentIdentifier.createMavenCoordinates("group", "otherArtifact", "*", "c1", "jar");
    String packageUrlAllVersions = PackageUrlIdentifier.toPackageUrl(identifierAllVersions);
    String packageUrlAllVersions2 = PackageUrlIdentifier.toPackageUrl(identifierAllVersions2);
    ProxyRepositoryPolicyViolation violation =
        tempEntity.newRepositoryPolicyViolation(repository, policy, "testPathname", identifier, "hash");
    violation.setConstraintFacts(constraintFacts);
    proxyRepositoryPolicyViolationDAO.update(violation);

    String policyId = policy.getId();
    String policy2Id = policy2.getId();
    String orgId = Organization.ROOT_ORGANIZATION_ID;
    String repoId = repository.getId();

    Date expiredExpiryTime = DateUtils.addMilliseconds(now, -1);
    Date expiringInFutureExpiryTime = DateUtils.addMinutes(now, 1);

    // applicable waivers that the service should return for the given violation
    PolicyWaiverReason policyWaiverReason = tempEntity.newWaiverReason("reasonType", "some reason");
    tempEntity.newWaiver("hashX", policyId, orgId, constraintFacts, packageUrlAllVersions, ALL_VERSIONS, "",
        policyWaiverReason, DateUtils.addDays(now, -10));
    tempEntity.newWaiver(null, policyId, orgId, constraintFacts, ALL_COMPONENTS, "", DateUtils.addDays(now, -9), null);
    tempEntity.newWaiver("hash", policyId, repoId, constraintFacts, packageUrlAllVersions2, EXACT_COMPONENT, "",
        DateUtils.addDays(now, -8), expiredExpiryTime); // expired
    tempEntity.newWaiver(null, policyId, repoId, constraintFacts, ALL_COMPONENTS, "A comment",
        DateUtils.addDays(now, -7), expiringInFutureExpiryTime); // expiring in the future
    // add more waivers with different attributes — for diversity
    tempEntity.newWaiver("hash", policyId, repoId, null, EXACT_COMPONENT, "", DateUtils.addDays(now, -6));
    tempEntity.newWaiver(null, policyId, repoId, null, packageUrlAllVersions2, ALL_VERSIONS, "",
        DateUtils.addDays(now, -5));
    tempEntity.newWaiver("hashX", policyId, repoId, constraintFacts, EXACT_COMPONENT, "", DateUtils.addDays(now, -4));
    tempEntity.newWaiver("hash", policyId, repoId, constraintFacts2, EXACT_COMPONENT, "", DateUtils.addDays(now, -3));
    tempEntity.newWaiver("hash2", policy2Id, repoId, null, EXACT_COMPONENT, "", DateUtils.addDays(now, -2), null);
    tempEntity.newWaiver(null, policy2Id, repoId, null, ALL_COMPONENTS, "", DateUtils.addDays(now, -1),
        expiringInFutureExpiryTime);
    tempEntity.newWaiver("hash", policy2Id, repoId, constraintFacts, EXACT_COMPONENT, "", now, expiredExpiryTime);

    String policyViolationId = violation.getId();

    ApiPolicyWaiversApplicableToViolationDTO dto = apiPolicyWaiverService.getApplicableWaivers(policyViolationId);

    // activeWaivers - results sorted to have deterministic ordering in the test
    List<ApiPolicyWaiverDTO> activeApplicableWaivers = dto.activeWaivers.stream()
        .sorted(Comparator.comparing(apiPolicyWaiverDTO -> apiPolicyWaiverDTO.createTime))
        .collect(Collectors.toList());

    assertThat(activeApplicableWaivers.size()).isEqualTo(3);
    assertApiPolicyWaiverDTO("hashX", policyId, orgId, "Root Organization", "", policyViolationId,
        null, "testuser", "Test User", ALL_VERSIONS, packageUrlAllVersions, policyWaiverReason.getReasonText(),
        policyWaiverReason.getId(), activeApplicableWaivers.get(0));
    assertApiPolicyWaiverDTO(null, policyId, orgId, "Root Organization", "", policyViolationId,
        null, "testuser", "Test User", ALL_COMPONENTS, null, null, null, activeApplicableWaivers.get(1));
    assertApiPolicyWaiverDTO(null, policyId, repoId, repository.getName(), "A comment", policyViolationId,
        expiringInFutureExpiryTime, "testuser", "Test User", ALL_COMPONENTS, null, null, null,
        activeApplicableWaivers.get(2));

    // expiredWaivers
    List<ApiPolicyWaiverDTO> expiredApplicableWaivers = dto.expiredWaivers;

    assertThat(expiredApplicableWaivers.size()).isEqualTo(1);
    assertApiPolicyWaiverDTO(
        "hash", policyId, repoId, repository.getName(), "", policyViolationId, expiredExpiryTime,
        "testuser", "Test User", EXACT_COMPONENT, packageUrlAllVersions2, null, null, expiredApplicableWaivers.get(0));
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
        app.getId(), policyViolation.getConstraintFacts(), componentPurl.getPackageUrl(), EXACT_COMPONENT, "comment");
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
            "doesNotExist", BuildStageType.ID, null, null, null, null))
        .withMessageContaining("Application with ID doesNotExist does not exist.");
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(
        () -> apiPolicyWaiverService.addWaiverToTransitivePolicyViolationsByOwnerStageComponent(OwnerType.ORGANIZATION,
            "doesNotExist", BuildStageType.ID, null, null, null, null))
        .withMessageContaining("Organization with ID doesNotExist does not exist.");
  }

  @Test
  public void testAddWaiverToTransitivePolicyViolationsByOwnerStageComponent_UnknownStageId() {
    assertThatExceptionOfType(InvalidStageException.class).isThrownBy(
        () -> apiPolicyWaiverService.addWaiverToTransitivePolicyViolationsByOwnerStageComponent(OwnerType.APPLICATION,
            app.getPublicId(), "doesNotExist", null, null, null, null))
        .withMessageContaining("Invalid stage id=doesNotExist");
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
      String hash,
      String constraintFactsJson,
      ComponentMatcherStrategyForWaiver matcherStrategy,
      String componentPurl,
      String waiverReason)
  {
    assertNotExpiringPolicyWaiver(ownerId, comment, creatorId, creatorName, hash, constraintFactsJson, matcherStrategy,
        componentPurl, waiverReason, false);
  }

  private void assertNotExpiringPolicyWaiver(
      String ownerId,
      String comment,
      String creatorId,
      String creatorName,
      String hash,
      String constraintFactsJson,
      ComponentMatcherStrategyForWaiver matcherStrategy,
      String componentPurl,
      String waiverReason,
      boolean expireWhenRemediationAvailable)
  {
    List<PolicyWaiver> policyWaivers = policyWaiverDAO.getActiveByOwnerId(ownerId);
    assertThat(policyWaivers).isNotEmpty().hasSize(1);
    assertPolicyWaiver(policyWaivers.get(0), ownerId, comment, creatorId, creatorName, hash, constraintFactsJson, null,
        matcherStrategy, componentPurl, waiverReason, expireWhenRemediationAvailable);
  }

  private void assertPolicyWaiver(
      PolicyWaiver policyWaiver,
      String ownerId,
      String comment,
      String creatorId,
      String creatorName,
      String hash,
      String constraintFactsJson,
      Date expiryTime,
      ComponentMatcherStrategyForWaiver matcherStrategy,
      String packageUrl,
      String waiverReasonId,
      boolean expireWhenRemediationAvailable)
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
    assertThat(policyWaiver.getConstraintFactsJson()).isEqualTo(constraintFactsJson);
    assertThat(policyWaiver.getCreatorId()).isNotNull();
    assertThat(policyWaiver.getCreatorId()).isEqualTo("testuser");
    assertThat(policyWaiver.getCreatorName()).isEqualTo("Test User");
    assertThat(policyWaiver.getComponentMatchStrategy()).isEqualTo(matcherStrategy);
    assertThat(policyWaiver.getAssociatedPackageUrl()).isEqualTo(packageUrl);
    assertThat(policyWaiver.getWaiverReasonId()).isEqualTo(waiverReasonId);
    assertThat(policyWaiver.isExpireWhenRemediationAvailable()).isEqualTo(expireWhenRemediationAvailable);
  }

  private void assertTelemetry(
      final OwnerType ownerType,
      final String ownerId)
  {
    assertTelemetry(ownerType, ownerId, 1);
  }

  private void assertTelemetry(
      final OwnerType ownerType,
      final String ownerId,
      final int expectedInvocations)
  {
    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySenderMock, times(expectedInvocations)).send(telemetryDataArgumentCaptor.capture());
    Map<String, Object> expectedAttributes = new HashMap<>();
    expectedAttributes.put("owner_type", ownerType.toString());

    expectedAttributes.put("owner_id", HdsClientAnalytics.obfuscate(ownerId));
    expectedAttributes.put("real_owner_id", ownerId);

    for (TelemetryData telemetryData : telemetryDataArgumentCaptor.getAllValues()) {
      assertThat(telemetryData).isNotNull();
      assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.POLICY_WAIVER_API);
      assertThat(telemetryData.getTimestamp()).isLessThanOrEqualTo(System.currentTimeMillis());
      assertThat(telemetryData.getAttributes()).isEqualTo(expectedAttributes);
    }
  }

  private void assertWaiverTelemetry(
      final OwnerType ownerType,
      final AbstractPolicyViolation abstractPolicyViolation,
      final PolicyWaiver policyWaiver)
  {
    assertWaiverTelemetry(ownerType, abstractPolicyViolation, policyWaiver, 0, 1);
  }

  private void assertWaiverTelemetry(
      final OwnerType ownerType,
      final AbstractPolicyViolation abstractPolicyViolation,
      final PolicyWaiver policyWaiver,
      int index,
      int invocations)
  {
    final ArgumentCaptor<PolicyWaiver> policyWaiverArgumentCaptor = ArgumentCaptor.forClass(PolicyWaiver.class);
    final ArgumentCaptor<OwnerType> ownerTypeArgumentCaptor = ArgumentCaptor.forClass(OwnerType.class);
    final ArgumentCaptor<AbstractPolicyViolation> policyViolationArgumentCaptor =
        ArgumentCaptor.forClass(AbstractPolicyViolation.class);
    verify(policyWaiverTelemetryCreator, times(invocations))
        .sendWaiverTelemetryForOwnerType(policyWaiverArgumentCaptor.capture(), ownerTypeArgumentCaptor.capture(),
            policyViolationArgumentCaptor.capture());

    final PolicyWaiver policyWaiverValue = policyWaiverArgumentCaptor.getAllValues().get(index);
    final OwnerType ownerTypeValue = ownerTypeArgumentCaptor.getAllValues().get(index);
    final AbstractPolicyViolation policyViolationValue = policyViolationArgumentCaptor.getAllValues().get(index);
    assertThat(ownerTypeValue).isNotNull().isEqualTo(ownerType);
    assertThat(policyViolationValue).isNotNull();
    assertThat(policyViolationValue.getId()).isEqualTo(abstractPolicyViolation.getId());
    assertThat(policyWaiverValue).isNotNull();
    assertThat(policyWaiverValue.getId()).isEqualTo(policyWaiver.getId());
  }

  @Test
  public void testGetTransitivePolicyWaiversByAppScanComponent_NotAnApp() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> apiPolicyWaiverService.getTransitivePolicyWaiversByAppScanComponent(OwnerType.ORGANIZATION, null, null,
            null, null, null))
        .withMessageContaining("scanId can only be specified for an application.");
  }

  @Test
  public void testGetTransitivePolicyWaiversByAppScanComponent_NoComponentIdentifier_NoPackageUrl_NoHash() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> apiPolicyWaiverService.getTransitivePolicyWaiversByAppScanComponent(OwnerType.APPLICATION, null,
            null, null, null, null))
        .withMessageContaining("componentIdentifier or packageUrl or hash must be specified.");
  }

  @Test
  public void testGetTransitivePolicyWaiversByAppScanComponent_AppNotFound() {
    String appId = "unknown";
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(
        () -> apiPolicyWaiverService.getTransitivePolicyWaiversByAppScanComponent(OwnerType.APPLICATION, appId, null,
            null, null, "hash"))
        .withMessageContaining("Application with ID " + appId + " does not exist.");
  }

  @Test
  public void testGetTransitivePolicyWaiversByAppScanComponent_ScanNotFound() {
    String scanId = "unknown";
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(
        () -> apiPolicyWaiverService.getTransitivePolicyWaiversByAppScanComponent(OwnerType.APPLICATION, app.getId(),
            scanId, null, null, "hash"))
        .withMessageContaining("scanId " + scanId + " not found for application " + app.getPublicId() + ".");
  }

  @Test
  public void testGetTransitivePolicyWaiversByAppScanComponent() {
    Application app = tempEntity.newApplicationWithParent();
    String scanId = "scanId";
    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, scanId);
    Policy policy = tempEntity.newPolicy();
    PolicyWaiver appWaiver = tempEntity.newWaiver("hash1", policy.getId(), app.getId(), null, EXACT_COMPONENT, null);
    PolicyWaiver orgWaiver =
        tempEntity.newWaiver("hash2", policy.getId(), app.getParentOwnerId(), null, EXACT_COMPONENT, null);
    PolicyWaiver rootOrgWaiver =
        tempEntity.newWaiver("hash3", policy.getId(), Organization.ROOT_ORGANIZATION_ID, null, EXACT_COMPONENT, null);
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
    Organization rootOrg = organizationDAO.getByIdNotNull(Organization.ROOT_ORGANIZATION_ID);
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
    Organization rootOrg = organizationDAO.getByIdNotNull(Organization.ROOT_ORGANIZATION_ID);
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
    if (hash != null) {
      return tempEntity.newWaiver(hash, tempEntity.newPolicy().getId(), owner.getId(), null, EXACT_COMPONENT, "");
    }
    else {
      return tempEntity.newWaiver(null, tempEntity.newPolicy().getId(), owner.getId(), null, ALL_COMPONENTS, "");
    }
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

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_Repository() {
    Repository repository = tempEntity.newRepository();
    policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);
    ProxyRepositoryPolicyViolation proxyRepositoryPolicyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());

    apiPolicyWaiverService.addPolicyWaiverByPolicyViolationId(OwnerType.REPOSITORY, repository.getId(),
        proxyRepositoryPolicyViolation.getId(),
        new ApiWaiverOptionsDTO("waiver comment", EXACT_COMPONENT, null, null, false));

    assertNotExpiringPolicyWaiver(repository.getId(), "waiver comment", "testuser", "Test User",
        proxyRepositoryPolicyViolation.getHash(), proxyRepositoryPolicyViolation.getConstraintFactsJson(),
        EXACT_COMPONENT,
        PackageUrlIdentifier.toPackageUrl(proxyRepositoryPolicyViolation.getComponentIdentifier()), null);
    assertTelemetry(OwnerType.REPOSITORY, repository.getId());
    assertWaiverTelemetry(OwnerType.REPOSITORY, proxyRepositoryPolicyViolation,
        policyWaiverDAO.getByOwnerId(repository.getId()).get(0));
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_RepositoryContainer() {
    Repository repository = tempEntity.newRepository();
    policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);
    ProxyRepositoryPolicyViolation proxyRepositoryPolicyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());

    apiPolicyWaiverService.addPolicyWaiverByPolicyViolationId(OwnerType.REPOSITORY_CONTAINER,
        RepositoryContainer.REPOSITORY_CONTAINER_ID, proxyRepositoryPolicyViolation.getId(),
        new ApiWaiverOptionsDTO("waiver comment", EXACT_COMPONENT, null, null, false));

    assertNotExpiringPolicyWaiver(RepositoryContainer.REPOSITORY_CONTAINER_ID, "waiver comment", "testuser",
        "Test User", proxyRepositoryPolicyViolation.getHash(), proxyRepositoryPolicyViolation.getConstraintFactsJson(),
        EXACT_COMPONENT, PackageUrlIdentifier.toPackageUrl(proxyRepositoryPolicyViolation.getComponentIdentifier()),
        null);
    assertTelemetry(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID);
    assertWaiverTelemetry(OwnerType.REPOSITORY_CONTAINER, proxyRepositoryPolicyViolation,
        policyWaiverDAO.getByOwnerId(RepositoryContainer.REPOSITORY_CONTAINER_ID).get(0));
  }

  @Test
  public void testGetPolicyWaiver_Application() {
    Application application = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(application);
    Date expirationDate = Date.from(Instant.now().plus(Duration.ofDays(2)));
    TriggerReference triggerReference = new TriggerReference(TriggerReference.Type.SECURITY_VULNERABILITY_REFID,
        "vulnerability-1");
    ConditionFact conditionFact = new ConditionFact("condition type id", 0, "summary", "reason", triggerReference);
    ConstraintFact constraintFact = new ConstraintFact("constraint id", "constraint name", "operator", conditionFact);
    PolicyWaiver policyWaiver =
        tempEntity.newWaiver("hash", policy.getId(), application.getId(), Collections.singletonList(constraintFact),
            "comment", Date.from(Instant.now()), expirationDate, ACKNOWLEDGED_VIOLATION_REASON_ID);

    ApiPolicyWaiverDTO savedWaiver =
        apiPolicyWaiverService.getPolicyWaiver(OwnerType.APPLICATION, application.getId(), policyWaiver.getId());

    assertWaivers(savedWaiver, policyWaiver, policy, application, ACKNOWLEDGED_VIOLATION_REASON_TEXT);
  }

  @Test
  public void testGetPolicyWaiver_Application_UsePublicId() {
    Application application = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(application);
    PolicyWaiver policyWaiver = tempEntity.newWaiver(
        "hash",
        policy.getId(),
        application.getId(),
        "comment",
        null,
        null,
        ACKNOWLEDGED_VIOLATION_REASON_ID);

    ApiPolicyWaiverDTO savedWaiver =
        apiPolicyWaiverService.getPolicyWaiver(OwnerType.APPLICATION, application.getPublicId(), policyWaiver.getId());

    assertWaivers(savedWaiver, policyWaiver, policy, application, ACKNOWLEDGED_VIOLATION_REASON_TEXT);
  }

  @Test
  public void testGetPolicyWaiver_Organization() {
    Organization organization = tempEntity.newOrganization();
    Policy policy = tempEntity.newPolicy(organization);
    PolicyWaiver policyWaiver = tempEntity.newWaiver(
        "hash",
        policy.getId(),
        organization.getId(),
        "comment",
        null,
        null,
        ACKNOWLEDGED_VIOLATION_REASON_ID);

    ApiPolicyWaiverDTO savedWaiver =
        apiPolicyWaiverService.getPolicyWaiver(OwnerType.ORGANIZATION, organization.getId(), policyWaiver.getId());

    assertWaivers(savedWaiver, policyWaiver, policy, organization, ACKNOWLEDGED_VIOLATION_REASON_TEXT);
  }

  @Test
  public void testGetPolicyWaiver_ComponentUpgradeAvailable() {
    Application application = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(application);
    PolicyWaiver policyWaiver = new PolicyWaiver()
        .setHash("hash")
        .setPolicyId(policy.getId())
        .setOwnerId(application.getId())
        .setComponentUpgradeAvailable(true)
        .setWaiverReasonId(ACKNOWLEDGED_VIOLATION_REASON_ID);

    tempEntity.newWaiver(policyWaiver);

    ApiPolicyWaiverDTO savedWaiver =
        apiPolicyWaiverService.getPolicyWaiver(OwnerType.APPLICATION, application.getId(), policyWaiver.getId());

    assertWaivers(savedWaiver, policyWaiver, policy, application, ACKNOWLEDGED_VIOLATION_REASON_TEXT);
  }

  @Test
  public void testGetPolicyWaiver_Repository() {
    Repository repository = tempEntity.newRepository();
    Policy policy = tempEntity.newPolicy();
    PolicyWaiver policyWaiver = tempEntity.newWaiver(
        "hash",
        policy.getId(),
        repository.getId(),
        "comment",
        null,
        null,
        ACKNOWLEDGED_VIOLATION_REASON_ID);

    ApiPolicyWaiverDTO savedWaiver =
        apiPolicyWaiverService.getPolicyWaiver(OwnerType.REPOSITORY, repository.getId(), policyWaiver.getId());

    assertWaivers(savedWaiver, policyWaiver, policy, repository, ACKNOWLEDGED_VIOLATION_REASON_TEXT);
  }

  @Test
  public void testGetPolicyWaiver_RepositoryContainer() {
    Policy policy = tempEntity.newPolicy();
    PolicyWaiver policyWaiver = tempEntity.newWaiver(
        "hash",
        policy.getId(),
        REPOSITORY_CONTAINER_ID,
        "comment",
        null,
        null,
        ACKNOWLEDGED_VIOLATION_REASON_ID);

    ApiPolicyWaiverDTO savedWaiver =
        apiPolicyWaiverService.getPolicyWaiver(REPOSITORY_CONTAINER, REPOSITORY_CONTAINER_ID, policyWaiver.getId());

    assertWaivers(savedWaiver, policyWaiver, policy, null, ACKNOWLEDGED_VIOLATION_REASON_TEXT);
    assertThat(savedWaiver.scopeOwnerId).isEqualTo(REPOSITORY_CONTAINER_ID);
    assertThat(savedWaiver.scopeOwnerName).isEqualTo("Repository Managers");
    assertThat(savedWaiver.scopeOwnerType).isEqualTo("all_repositories");
  }

  @Test
  public void testGetPolicyWaiver_WithRenewalFields() {
    Application application = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(application);
    PolicyWaiverReason renewalReason = tempEntity.newWaiverReason("renewalType", "renewal reason text");

    PolicyWaiver policyWaiver = new PolicyWaiver()
        .setHash("hash")
        .setPolicyId(policy.getId())
        .setOwnerId(application.getId())
        .setWaiverReasonId(ACKNOWLEDGED_VIOLATION_REASON_ID)
        .setLastRenewedBy("admin")
        .setLastRenewedAt(Date.from(Instant.now()))
        .setLastRenewalComment("renewal comment text")
        .setLastRenewalReasonId(renewalReason.getId());
    tempEntity.newWaiver(policyWaiver);

    ApiPolicyWaiverDTO savedWaiver =
        apiPolicyWaiverService.getPolicyWaiver(OwnerType.APPLICATION, application.getId(), policyWaiver.getId());

    assertThat(savedWaiver.lastRenewedBy).isEqualTo("admin");
    assertThat(savedWaiver.lastRenewedAt).isNotNull();
    assertThat(savedWaiver.lastRenewalComment).isEqualTo("renewal comment text");
    assertThat(savedWaiver.lastRenewalReasonText).isEqualTo("renewal reason text");
  }

  @Test
  public void testGetPolicyWaiver_WithRenewalFields_NullReason() {
    Application application = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(application);

    PolicyWaiver policyWaiver = new PolicyWaiver()
        .setHash("hash")
        .setPolicyId(policy.getId())
        .setOwnerId(application.getId())
        .setWaiverReasonId(ACKNOWLEDGED_VIOLATION_REASON_ID)
        .setLastRenewedBy("admin")
        .setLastRenewedAt(Date.from(Instant.now()))
        .setLastRenewalComment("renewal comment text");
    tempEntity.newWaiver(policyWaiver);

    ApiPolicyWaiverDTO savedWaiver =
        apiPolicyWaiverService.getPolicyWaiver(OwnerType.APPLICATION, application.getId(), policyWaiver.getId());

    assertThat(savedWaiver.lastRenewalComment).isEqualTo("renewal comment text");
    assertThat(savedWaiver.lastRenewalReasonText).isNull();
  }

  @Test
  public void testGetPolicyWaiver_Expired() {
    Application application = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(application);
    Date expirationDate = Date.from(Instant.now().minus(Duration.ofDays(2)));

    PolicyWaiver policyWaiver =
        tempEntity.newWaiver(
            "hash",
            policy.getId(),
            application.getId(),
            null,
            "comment",
            expirationDate,
            expirationDate,
            ACKNOWLEDGED_VIOLATION_REASON_ID);

    ApiPolicyWaiverDTO savedExpiredWaiver =
        apiPolicyWaiverService.getPolicyWaiver(OwnerType.APPLICATION, application.getId(), policyWaiver.getId());

    assertThat(savedExpiredWaiver).isNotNull();
    assertThat(savedExpiredWaiver.expiryTime).isEqualTo(expirationDate);
    assertThat(savedExpiredWaiver.policyWaiverReasonId).isEqualTo(ACKNOWLEDGED_VIOLATION_REASON_ID);
    assertThat(savedExpiredWaiver.reasonText).isEqualTo(ACKNOWLEDGED_VIOLATION_REASON_TEXT);
  }

  @Test
  public void testGetPolicyWaiver_WithWaiverReason() {
    Application application = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(application);

    PolicyWaiver policyWaiver =
        tempEntity.newWaiverWithReason("hash", policy.getId(), application.getId(), null, "comment",
            "system", "because reasons");

    ApiPolicyWaiverDTO savedWaiver =
        apiPolicyWaiverService.getPolicyWaiver(OwnerType.APPLICATION, application.getId(), policyWaiver.getId());

    assertWaivers(savedWaiver, policyWaiver, policy, application, "because reasons");
  }

  @Test
  public void testGetPolicyWaiver_nonExistentId() {
    Application application = tempEntity.newApplicationWithParent();
    String expectedErrorMessage =
        String.format("Cannot find a waiver with ID %s for owner %s", "nonExistingId", application.getId());

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(
        () -> apiPolicyWaiverService.getPolicyWaiver(OwnerType.APPLICATION, application.getId(), "nonExistingId"))
        .withMessageContaining(expectedErrorMessage);
  }

  @Test
  public void testAddPolicyWaiverByPolicyViolationId_Application_WithEnabled() {
    // When
    apiPolicyWaiverService.addPolicyWaiverByPolicyViolationId(OwnerType.APPLICATION, app.getId(),
        policyViolation.getId(), new ApiWaiverOptionsDTO("waiver comment", EXACT_COMPONENT, null, null, false));

    // Then
    assertNotExpiringPolicyWaiver(app.getId(), "waiver comment", "testuser", "Test User", policyViolation.getHash(),
        policyViolation.getConstraintFactsJson(), EXACT_COMPONENT, componentPurl.getPackageUrl(), null);
    assertTelemetry(OwnerType.APPLICATION, app.getId());
    assertWaiverTelemetry(OwnerType.APPLICATION, policyViolation, policyWaiverDAO.getByOwnerId(app.getId()).get(0));
  }

  @Test
  public void testGetSimilarWaivers_null() {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(
        () -> apiPolicyWaiverService.getSimilarWaivers(null))
        .withMessageContaining("Could not find policy violation with ID null.");
  }

  @Test
  public void testGetSimilarWaivers_AllFiltersValid() {
    List<ConstraintFact> constraintFacts = new ArrayList<>(policyViolation.getConstraintFacts());
    constraintFacts.add(new ConstraintFact("id", "Test Constraint 2", null));

    PolicyWaiverReason policyWaiverReason = tempEntity.newWaiverReason("reasonType", "some reason");
    PolicyWaiver policyWaiver = tempEntity.newWaiver(
        policyViolation.getHash(),
        policyViolation.getPolicyId(),
        policyViolation.getOwnerId(),
        constraintFacts,
        null,
        null,
        null,
        policyWaiverReason.getId());

    List<ApiPolicyWaiverDTO> similarWaivers = apiPolicyWaiverService.getSimilarWaivers(policyViolation.getId());

    assertThat(similarWaivers).isNotEmpty();
    assertThat(similarWaivers).hasSize(1);

    assertWaivers(similarWaivers.get(0), policyWaiver, null, app, "some reason");
  }

  @Test
  public void testGetSimilarWaivers_AllFiltersValid_NoSecurityViolation() {
    PolicyViolation policyViolation2 =
        tempEntity.newPolicyViolation(policyEvaluation, policy, 0, PolicyThreatCategory.LICENSE);
    List<ConstraintFact> constraintFacts = new ArrayList<>(policyViolation2.getConstraintFacts());
    constraintFacts.add(new ConstraintFact("id", "Test Constraint 2", null));
    PolicyWaiver policyWaiver = tempEntity.newWaiver(
        policyViolation2.getHash(),
        policyViolation2.getPolicyId(),
        policyViolation2.getOwnerId(),
        constraintFacts,
        null,
        null,
        null,
        ACKNOWLEDGED_VIOLATION_REASON_ID);

    List<ApiPolicyWaiverDTO> similarWaivers = apiPolicyWaiverService.getSimilarWaivers(policyViolation2.getId());

    assertThat(similarWaivers).isNotEmpty();
    assertThat(similarWaivers).hasSize(1);

    assertWaivers(similarWaivers.get(0), policyWaiver, null, app, ACKNOWLEDGED_VIOLATION_REASON_TEXT);
  }

  @Test
  public void testGetSimilarWaivers_NoViewPermissionOnWaiver() {
    tempEntity.newWaiver(policyViolation.getHash(),
        policyViolation.getPolicyId(),
        "",
        policyViolation.getConstraintFacts());

    List<ApiPolicyWaiverDTO> similarWaivers = apiPolicyWaiverService.getSimilarWaivers(policyViolation.getId());

    assertThat(similarWaivers).isEmpty();
  }

  @Test
  public void testGetSimilarWaivers_IsAnApplicableWaiver() {
    tempEntity.newWaiver(policyViolation.getHash(),
        policyViolation.getPolicyId(),
        policyViolation.getOwnerId(),
        policyViolation.getConstraintFacts());

    List<ApiPolicyWaiverDTO> similarWaivers = apiPolicyWaiverService.getSimilarWaivers(policyViolation.getId());

    assertThat(similarWaivers).isEmpty();
  }

  @Test
  public void testGetSimilarWaivers_WaiverDoesNotMatchComponent() {
    List<ConstraintFact> constraintFacts = new ArrayList<>(policyViolation.getConstraintFacts());
    constraintFacts.add(new ConstraintFact("id", "Test Constraint 2", null));
    tempEntity.newWaiver("",
        policyViolation.getPolicyId(),
        policyViolation.getOwnerId(),
        constraintFacts);

    List<ApiPolicyWaiverDTO> similarWaivers = apiPolicyWaiverService.getSimilarWaivers(policyViolation.getId());

    assertThat(similarWaivers).isEmpty();
  }

  @Test
  public void testGetSimilarWaivers_VulnerabilityDoesNotMatch() {
    tempEntity.newWaiver(policyViolation.getHash(),
        policyViolation.getPolicyId(),
        policyViolation.getOwnerId(),
        Collections.singletonList(new ConstraintFact("id", "Test Constraint 2", null)));

    List<ApiPolicyWaiverDTO> similarWaivers = apiPolicyWaiverService.getSimilarWaivers(policyViolation.getId());

    assertThat(similarWaivers).isEmpty();
  }

  @Test
  public void testGetSimilarWaivers_shouldHandleNullConstraintFactJsonPolicyWaiver() {
    tempEntity.newWaiverWithNoConstraintFact(policyViolation.getHash(),
        policyViolation.getPolicyId(),
        policyViolation.getOwnerId());

    List<ApiPolicyWaiverDTO> similarWaivers = apiPolicyWaiverService.getSimilarWaivers(policyViolation.getId());

    assertThat(similarWaivers).isEmpty();
  }

  @Test
  public void testUpdatePolicyWaiver_OrganizationDoesNotExist() {
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);
    PolicyWaiver policyWaiver = tempEntity.newWaiver(policy.getId(), Organization.ROOT_ORGANIZATION_ID);
    ApiWaiverOptionsDTO dto = new ApiWaiverOptionsDTO(policyWaiver);

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> apiPolicyWaiverService.updatePolicyWaiver(
            OwnerType.ORGANIZATION,
            "doesNotExist",
            policyWaiver.getId(),
            dto))
        .withMessageContaining("Organization with ID doesNotExist does not exist.");
  }

  @Test
  public void testUpdatePolicyWaiver_ApplicationDoesNotExist() {
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);
    PolicyWaiver policyWaiver = tempEntity.newWaiver(policy.getId(), Organization.ROOT_ORGANIZATION_ID);
    ApiWaiverOptionsDTO dto = new ApiWaiverOptionsDTO(policyWaiver);

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> apiPolicyWaiverService.updatePolicyWaiver(
            OwnerType.APPLICATION,
            "doesNotExist",
            policyWaiver.getId(),
            dto))
        .withMessageContaining("Application with ID doesNotExist does not exist.");
  }

  @Test
  public void testUpdatePolicyWaiver_RepositoryManagerDoesNotExist() {
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);
    PolicyWaiver policyWaiver = tempEntity.newWaiver(policy.getId(), Organization.ROOT_ORGANIZATION_ID);
    ApiWaiverOptionsDTO dto = new ApiWaiverOptionsDTO(policyWaiver);

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> apiPolicyWaiverService.updatePolicyWaiver(
            OwnerType.REPOSITORY_MANAGER,
            "doesNotExist",
            policyWaiver.getId(),
            dto))
        .withMessageContaining("RepositoryManager with ID doesNotExist does not exist.");
  }

  @Test
  public void testUpdatePolicyWaiver_RepositoryDoesNotExist() {
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);
    PolicyWaiver policyWaiver = tempEntity.newWaiver(policy.getId(), Organization.ROOT_ORGANIZATION_ID);
    ApiWaiverOptionsDTO dto = new ApiWaiverOptionsDTO(policyWaiver);

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> apiPolicyWaiverService.updatePolicyWaiver(
            OwnerType.REPOSITORY,
            "doesNotExist",
            policyWaiver.getId(),
            dto))
        .withMessageContaining("Repository with ID doesNotExist does not exist.");
  }

  @Test
  public void testUpdatePolicyWaiver_WaiverDoesNotExist() {
    Application application = tempEntity.newApplicationWithParent();
    ApiWaiverOptionsDTO dto = new ApiWaiverOptionsDTO();

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> apiPolicyWaiverService.updatePolicyWaiver(
            OwnerType.APPLICATION,
            application.getId(),
            "doesNotExist",
            dto))
        .withMessageContaining("Cannot find a waiver with ID doesNotExist for owner " + application.getId() + ".");
  }

  @Test
  public void testUpdatePolicyWaiver_MatcherStrategy_NotSupported() {
    Application application = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(application);
    PolicyWaiver policyWaiver = tempEntity.newWaiver(policy.getId(), application.getId());
    ApiWaiverOptionsDTO dto = new ApiWaiverOptionsDTO(policyWaiver);
    dto.matcherStrategy = null;

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> apiPolicyWaiverService.updatePolicyWaiver(
            application.getType(),
            application.getId(),
            policyWaiver.getId(),
            dto))
        .withMessageContaining("Matcher strategy cannot be updated.");
  }

  @Test
  public void testUpdatePolicyWaiver_MatcherStrategy_NotChanged() {
    Application application = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(application);
    PolicyWaiver policyWaiver = tempEntity.newWaiver(policy.getId(), application.getId());
    ApiWaiverOptionsDTO dto = new ApiWaiverOptionsDTO(policyWaiver);
    dto.comment = "new comment";

    apiPolicyWaiverService.updatePolicyWaiver(
        application.getType(),
        application.getId(),
        policyWaiver.getId(),
        dto);

    assertThat(policyWaiverDAO.getById(policyWaiver.getId()).getComment()).isEqualTo(dto.comment);
  }

  @Test
  public void testUpdatePolicyWaiver_ExpiryTimeInPast() {
    Application application = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(application);
    PolicyWaiver policyWaiver = tempEntity.newWaiver(policy.getId(), application.getId());
    ApiWaiverOptionsDTO dto = new ApiWaiverOptionsDTO(policyWaiver);
    dto.expiryTime = DateUtils.addDays(new Date(), -1);

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> apiPolicyWaiverService.updatePolicyWaiver(
            application.getType(),
            application.getId(),
            policyWaiver.getId(),
            dto))
        .withMessageContaining("Expiration date must be in the future.");
  }

  @Test
  public void testUpdatePolicyWaiver_WaiverReasonDoesNotExist() {
    Application application = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(application);
    PolicyWaiver policyWaiver = tempEntity.newWaiver(policy.getId(), application.getId());
    ApiWaiverOptionsDTO dto = new ApiWaiverOptionsDTO(policyWaiver);
    dto.waiverReasonId = "doesNotExist";

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> apiPolicyWaiverService.updatePolicyWaiver(
            application.getType(),
            application.getId(),
            policyWaiver.getId(),
            dto))
        .withMessageContaining("Waiver reason not found");
  }

  @Test
  public void testUpdatePolicyWaiver_ExpireWhenRemediationAvailableNotExact() {
    Application application = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(application);
    PolicyWaiver policyWaiver = tempEntity.newWaiver(policy.getId(), application.getId());
    policyWaiver.setComponentMatchStrategy(ALL_COMPONENTS);
    policyWaiver.setExpireWhenRemediationAvailable(false);
    policyWaiverDAO.update(policyWaiver);
    ApiWaiverOptionsDTO dto = new ApiWaiverOptionsDTO(policyWaiver);
    dto.expireWhenRemediationAvailable = true;

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> apiPolicyWaiverService.updatePolicyWaiver(
            application.getType(),
            application.getId(),
            policyWaiver.getId(),
            dto))
        .withMessageContaining("Expire When Remediation Available Waivers can only be applied to Exact Components.");
  }

  @Test
  public void testUpdatePolicyWaiver() {
    Application application = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(application);
    PolicyWaiverReason policyWaiverReason1 = tempEntity.newWaiverReason("type1", "reason1");
    String hash = "hash";
    Date expiry = DateUtils.addDays(new Date(), 1);
    PolicyWaiver policyWaiver = tempEntity.newWaiver(hash, policy.getId(), application.getId(), "comment1", expiry);
    policyWaiver.setExpireWhenRemediationAvailable(false);
    policyWaiver.setWaiverReasonId(policyWaiverReason1.getId());
    policyWaiverDAO.update(policyWaiver);
    ApiWaiverOptionsDTO dto = new ApiWaiverOptionsDTO(policyWaiver);
    dto.comment = "new comment";
    dto.expiryTime = DateUtils.addDays(expiry, 1);
    PolicyWaiverReason policyWaiverReason2 = tempEntity.newWaiverReason("type2", "reason2");
    dto.waiverReasonId = policyWaiverReason2.getId();
    dto.expireWhenRemediationAvailable = true;

    apiPolicyWaiverService.updatePolicyWaiver(
        application.getType(),
        application.getId(),
        policyWaiver.getId(),
        dto);

    assertThat(new ApiWaiverOptionsDTO(policyWaiverDAO.getById(policyWaiver.getId())))
        .usingRecursiveComparison(JPA.RECURSIVE_COMPARISON_CONFIG)
        .isEqualTo(dto);
  }

  private void assertWaivers(
      ApiPolicyWaiverDTO savedWaiver,
      PolicyWaiver policyWaiver,
      Policy policy,
      Owner owner,
      String expectedWaiverReasonText)
  {
    assertThat(savedWaiver.policyWaiverId).isEqualTo(policyWaiver.getId());
    assertThat(savedWaiver.comment).isEqualTo(policyWaiver.getComment());
    assertThat(savedWaiver.createTime).isEqualTo(policyWaiver.getCreateTime());
    assertThat(savedWaiver.hash).isEqualTo(policyWaiver.getHash());
    assertThat(savedWaiver.policyId).isEqualTo(policyWaiver.getPolicyId());
    assertThat(savedWaiver.constraintFactsJson).isEqualTo(policyWaiver.getConstraintFactsJson());
    assertThat(savedWaiver.creatorName).isEqualTo(policyWaiver.getCreatorName());
    assertThat(savedWaiver.componentUpgradeAvailable).isEqualTo(policyWaiver.isComponentUpgradeAvailable());
    if (owner != null) {
      assertThat(savedWaiver.scopeOwnerId).isEqualTo(owner.getId());
      assertThat(savedWaiver.scopeOwnerName).isEqualTo(owner.getName());
      assertThat(savedWaiver.scopeOwnerType).isEqualTo(owner.getType().toString());
    }
    if (policy != null) {
      assertThat(savedWaiver.policyName).isEqualTo(policy.getName());
      assertThat(savedWaiver.threatLevel).isEqualTo(policy.getThreatLevel());
    }

    assertThat(savedWaiver.policyWaiverReasonId).isEqualTo(policyWaiver.getWaiverReasonId());
    assertThat(savedWaiver.reasonText).isEqualTo(expectedWaiverReasonText);
  }

  @Test
  public void testAddContainerImageWaiver_notAContainerImage() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> apiPolicyWaiverService.addContainerImageWaiver(app.getId(), new ApiContainerImageWaiverDTO()))
        .withMessageContaining("No container image was found with the given ID");
  }

  @Test
  public void testAddContainerImageWaiver_invalidExpiryTime() {
    relateOrganizationWithRepository();
    ApiContainerImageWaiverDTO waiverDTO = new ApiContainerImageWaiverDTO();
    waiverDTO.expiryTime = DateUtils.addDays(new Date(), -1);

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> apiPolicyWaiverService.addContainerImageWaiver(app.getId(), waiverDTO))
        .withMessageContaining("Expiration date must be in the future.");
  }

  @Test
  public void testAddContainerImageWaiver_invalidWaiverReason() {
    relateOrganizationWithRepository();
    ApiContainerImageWaiverDTO waiverDTO = new ApiContainerImageWaiverDTO();
    waiverDTO.waiverReasonId = "invalidWaiverReasonId";

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> apiPolicyWaiverService.addContainerImageWaiver(app.getId(), waiverDTO))
        .withMessageContaining("Waiver reason not found");
  }

  @Test
  public void testAddContainerImageWaiver_invalidRelatedRepositoryType() {
    Repository repository =
        tempEntity.newRepository(tempEntity.newRepositoryManager(), "docker-repo", RepositoryType.hosted, "docker");
    repository.setRelatedOrganizationId(org.getId());
    repositoryDAO.update(repository);
    org.setRelatedRepositoryId(repository.getId());
    organizationDAO.update(org);

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> apiPolicyWaiverService.addContainerImageWaiver(app.getId(), new ApiContainerImageWaiverDTO()))
        .withMessageContaining("The related repository must be of type proxy and format docker");
  }

  @Test
  public void testAddContainerImageWaiver_invalidRelatedRepositoryFormat() {
    Repository repository =
        tempEntity.newRepository(tempEntity.newRepositoryManager(), "docker-repo", RepositoryType.proxy,
            "invalidFormat");
    repository.setRelatedOrganizationId(org.getId());
    repositoryDAO.update(repository);
    org.setRelatedRepositoryId(repository.getId());
    organizationDAO.update(org);

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> apiPolicyWaiverService.addContainerImageWaiver(app.getId(), new ApiContainerImageWaiverDTO()))
        .withMessageContaining("The related repository must be of type proxy and format docker");
  }

  @Test
  public void testAddContainerImageWaiver_noApplicablePolicyViolations() {
    relateOrganizationWithRepository();
    policy = tempEntity.newPolicy(app.getId());
    policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), ProxyStageType.ID, "scanId1App1");
    tempEntity.newPolicyViolation(policyEvaluation, policy, 10, PolicyThreatCategory.LICENSE,
        "g", "a", "v", "hash", WarnActionType.ID);

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> apiPolicyWaiverService.addContainerImageWaiver(app.getId(), null))
        .withMessageContaining(
            "No applicable policy violations found to waive for container image with the given ID");
  }

  @Test
  public void testAddContainerImageWaiver() {
    relateOrganizationWithRepository();
    policy = tempEntity.newPolicy(app.getId());
    policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), ProxyStageType.ID, "scanId1App1");
    PolicyViolation policyViolation =
        tempEntity.newPolicyViolation(policyEvaluation, policy, 5, PolicyThreatCategory.SECURITY,
            "g1", "a1", "v1", "hash1", FailActionType.ID);
    // policyViolation with different action type at proxy stage, should be ignored
    tempEntity.newPolicyViolation(policyEvaluation, policy, 10, PolicyThreatCategory.LICENSE,
        "g2", "a2", "v2", "hash2", WarnActionType.ID);

    // policy evaluation at different stage, should be ignored
    PolicyEvaluation policyEvaluationForOtherStage =
        tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId1App1");
    tempEntity.newPolicyViolation(policyEvaluationForOtherStage, policy, 10, PolicyThreatCategory.LICENSE,
        "g3", "a3", "v3", "hash3", FailActionType.ID);

    // policy evaluation and violation for different application, should be ignored
    Application otherApplication = tempEntity.newApplicationWithParent();
    Policy policyForOtherApplication = tempEntity.newPolicy(otherApplication);
    PolicyEvaluation policyEvaluationForOtherApplication =
        tempEntity.newPolicyEvaluation(otherApplication.getId(), ProxyStageType.ID, "scanId1App1");
    tempEntity.newPolicyViolation(policyEvaluationForOtherApplication,
        policyForOtherApplication, policyViolation.getComponentIdentifier(), policyViolation.getHash(), "r1");

    ApiContainerImageWaiverDTO waiverDTO = new ApiContainerImageWaiverDTO();
    waiverDTO.comment = "Test comment";
    waiverDTO.expiryTime = DateUtils.addDays(new Date(), 1);
    waiverDTO.waiverReasonId = policyWaiverReasonDAO.getAll().get(0).getId();

    apiPolicyWaiverService.addContainerImageWaiver(app.getId(), waiverDTO);

    List<PolicyWaiver> policyWaivers = policyWaiverDAO.getActiveByOwnerId(app.getId());
    assertThat(policyWaivers).hasSize(2);

    policyWaivers = new ArrayList<>(policyWaivers);
    policyWaivers.sort(Comparator.comparing(PolicyWaiver::getHash, Comparator.nullsLast(Comparator.naturalOrder())));

    PolicyWaiver createdWaiver = policyWaivers.get(0);
    assertThat(createdWaiver.getHash()).isEqualTo(policyViolation.getHash());
    assertThat(createdWaiver.getPolicyId()).isEqualTo(policyViolation.getPolicyId());
    assertThat(createdWaiver.getComment()).isEqualTo(waiverDTO.comment);
    assertThat(createdWaiver.getExpiryTime()).isEqualTo(waiverDTO.expiryTime);
    assertThat(createdWaiver.getComponentMatchStrategy()).isEqualTo(EXACT_COMPONENT);
    assertThat(createdWaiver.isExpireWhenRemediationAvailable()).isFalse();
    assertThat(createdWaiver.isForContainerImageComponent()).isTrue();
    assertThat(createdWaiver.isForContainerImage()).isFalse();

    createdWaiver = policyWaivers.get(1);
    assertThat(createdWaiver.getHash()).isNull();
    assertThat(createdWaiver.getPolicyId()).isEqualTo(policyViolation.getPolicyId());
    assertThat(createdWaiver.getComment()).isEqualTo(waiverDTO.comment);
    assertThat(createdWaiver.getExpiryTime()).isEqualTo(waiverDTO.expiryTime);
    assertThat(createdWaiver.getComponentMatchStrategy()).isEqualTo(ALL_COMPONENTS);
    assertThat(createdWaiver.isExpireWhenRemediationAvailable()).isFalse();
    assertThat(createdWaiver.isForContainerImageComponent()).isFalse();
    assertThat(createdWaiver.isForContainerImage()).isTrue();
  }

  @Test
  public void testDeleteContainerImageWaiver_invalidRelatedRepositoryType() {
    Repository repository =
        tempEntity.newRepository(tempEntity.newRepositoryManager(), "docker-repo", RepositoryType.hosted, "docker");
    repository.setRelatedOrganizationId(org.getId());
    repositoryDAO.update(repository);
    org.setRelatedRepositoryId(repository.getId());
    organizationDAO.update(org);

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> apiPolicyWaiverService.deleteContainerImageWaiver(app.getId()))
        .withMessageContaining("The related repository must be of type proxy and format docker");
  }

  @Test
  public void testDeleteContainerImageWaiver_invalidRelatedRepositoryFormat() {
    Repository repository =
        tempEntity.newRepository(tempEntity.newRepositoryManager(), "docker-repo", RepositoryType.proxy,
            "invalidFormat");
    repository.setRelatedOrganizationId(org.getId());
    repositoryDAO.update(repository);
    org.setRelatedRepositoryId(repository.getId());
    organizationDAO.update(org);

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> apiPolicyWaiverService.deleteContainerImageWaiver(app.getId()))
        .withMessageContaining("The related repository must be of type proxy and format docker");
  }

  @Test
  public void testDeleteContainerImageWaiver() {
    relateOrganizationWithRepository();

    policy = tempEntity.newPolicy(app.getId());
    policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), ProxyStageType.ID, "scanId1App1");
    policyViolation =
        tempEntity.newPolicyViolation(policyEvaluation, policy, 5, PolicyThreatCategory.SECURITY,
            "g1", "a1", "v1", "hash1", FailActionType.ID);

    ApiContainerImageWaiverDTO containerWaiversDTO = new ApiContainerImageWaiverDTO();
    containerWaiversDTO.comment = "Test comment";
    containerWaiversDTO.expiryTime = DateUtils.addDays(new Date(), 1);
    containerWaiversDTO.waiverReasonId = policyWaiverReasonDAO.getAll().get(0).getId();

    apiPolicyWaiverService.addContainerImageWaiver(app.getId(), containerWaiversDTO);

    List<PolicyWaiver> policyWaivers = policyWaiverDAO.getActiveByOwnerId(app.getId());
    assertThat(policyWaivers).isNotEmpty();

    apiPolicyWaiverService.deleteContainerImageWaiver(app.getId());
    policyWaivers = policyWaiverDAO.getActiveByOwnerId(app.getId());
    assertThat(policyWaivers).isEmpty();
  }

  private void relateOrganizationWithRepository() {
    Repository repository =
        tempEntity.newRepository(tempEntity.newRepositoryManager(), "docker-repo", RepositoryType.proxy, "docker");
    repository.setRelatedOrganizationId(org.getId());
    repositoryDAO.update(repository);
    org.setRelatedRepositoryId(repository.getId());
    organizationDAO.update(org);
  }

  @Test
  public void testAddBulkPolicyWaivers_Application_Success() {
    Policy policy2 = tempEntity.newPolicy(app);
    PolicyViolation violation2 = tempEntity.newPolicyViolation(policyEvaluation, policy2,
        5, PolicyThreatCategory.SECURITY, "g2", "a2", "v2", "hash2", FailActionType.ID);

    ApiWaiverOptionsDTO waiverOptions = new ApiWaiverOptionsDTO();
    waiverOptions.comment = "Bulk waiver comment";
    waiverOptions.matcherStrategy = EXACT_COMPONENT;
    ApiBulkWaiversDTO bulkWaiversDTO = new ApiBulkWaiversDTO(
        Arrays.asList(policyViolation.getId(), violation2.getId()),
        waiverOptions);

    apiPolicyWaiverService.addBulkPolicyWaivers(OwnerType.APPLICATION, app.getId(), bulkWaiversDTO);

    List<PolicyWaiver> waivers = policyWaiverDAO.getActiveByOwnerId(app.getId());
    assertThat(waivers)
        .hasSize(2)
        .allSatisfy(waiver -> {
          assertThat(waiver.getComment()).isEqualTo("Bulk waiver comment");
          assertThat(waiver.getComponentMatchStrategy()).isEqualTo(EXACT_COMPONENT);
          assertThat(waiver.getCreatorId()).isEqualTo("testuser");
        });

    assertTelemetry(OwnerType.APPLICATION, app.getPublicId());
  }

  @Test
  public void testAddBulkPolicyWaivers_Organization_Success() {
    ApiWaiverOptionsDTO waiverOptions = new ApiWaiverOptionsDTO();
    waiverOptions.comment = "Org bulk waiver";
    waiverOptions.matcherStrategy = ALL_VERSIONS;
    ApiBulkWaiversDTO bulkWaiversDTO = new ApiBulkWaiversDTO(
        Collections.singletonList(policyViolation.getId()),
        waiverOptions);

    apiPolicyWaiverService.addBulkPolicyWaivers(OwnerType.ORGANIZATION, org.getId(), bulkWaiversDTO);

    List<PolicyWaiver> waivers = policyWaiverDAO.getActiveByOwnerId(org.getId());
    assertThat(waivers).hasSize(1);
    assertThat(waivers.get(0).getComment()).isEqualTo("Org bulk waiver");
    assertThat(waivers.get(0).getComponentMatchStrategy()).isEqualTo(ALL_VERSIONS);

    assertTelemetry(OwnerType.ORGANIZATION, org.getPublicId());
  }

  @Test
  public void testAddBulkPolicyWaivers_Repository_Success() {
    Repository repository = tempEntity.newRepository();
    ProxyRepositoryPolicyViolation repoPolicyViolation = tempEntity.newRepositoryPolicyViolation(
        repository.getId(), policy.getId(), policy.getThreatLevel());

    ApiWaiverOptionsDTO waiverOptions = new ApiWaiverOptionsDTO();
    waiverOptions.comment = "Repo bulk waiver";
    waiverOptions.matcherStrategy = EXACT_COMPONENT;
    ApiBulkWaiversDTO bulkWaiversDTO = new ApiBulkWaiversDTO(
        Collections.singletonList(repoPolicyViolation.getId()),
        waiverOptions);

    apiPolicyWaiverService.addBulkPolicyWaivers(OwnerType.REPOSITORY, repository.getId(), bulkWaiversDTO);

    List<PolicyWaiver> waivers = policyWaiverDAO.getActiveByOwnerId(repository.getId());
    assertThat(waivers).hasSize(1);
    assertThat(waivers.get(0).getComment()).isEqualTo("Repo bulk waiver");
    assertThat(waivers.get(0).getComponentMatchStrategy()).isEqualTo(EXACT_COMPONENT);

    assertTelemetry(OwnerType.REPOSITORY, repository.getPublicId());
  }

  @Test
  public void testAddBulkPolicyWaivers_RepositoryManager_Success() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager);
    ProxyRepositoryPolicyViolation repoPolicyViolation = tempEntity.newRepositoryPolicyViolation(
        repository.getId(), policy.getId(), policy.getThreatLevel());

    ApiWaiverOptionsDTO waiverOptions = new ApiWaiverOptionsDTO();
    waiverOptions.comment = "Repository Manager bulk waiver";
    waiverOptions.matcherStrategy = EXACT_COMPONENT;
    ApiBulkWaiversDTO bulkWaiversDTO = new ApiBulkWaiversDTO(
        Collections.singletonList(repoPolicyViolation.getId()),
        waiverOptions);

    apiPolicyWaiverService.addBulkPolicyWaivers(
        OwnerType.REPOSITORY_MANAGER, repositoryManager.getId(), bulkWaiversDTO);

    List<PolicyWaiver> waivers = policyWaiverDAO.getActiveByOwnerId(repositoryManager.getId());
    assertThat(waivers).hasSize(1);
    assertThat(waivers.get(0).getComment()).isEqualTo("Repository Manager bulk waiver");
    assertThat(waivers.get(0).getComponentMatchStrategy()).isEqualTo(EXACT_COMPONENT);

    assertTelemetry(OwnerType.REPOSITORY_MANAGER, repositoryManager.getPublicId());
  }

  @Test
  public void testAddBulkPolicyWaivers_RepositoryContainer_Success() {
    // Post NEXUS-53680: container-image bulk waivers go through applyContainerImageWaivers
    // (invoked by addContainerImageWaiver), which stores per-violation waivers plus a summary
    // marker under the container-image APPLICATION id. The virtual REPOSITORY_CONTAINER_ID is
    // a query-key only and is no longer a valid write target.
    relateOrganizationWithRepository();
    policy = tempEntity.newPolicy(app.getId());
    policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), ProxyStageType.ID, "scanId1App1");
    tempEntity.newPolicyViolation(policyEvaluation, policy, 5, PolicyThreatCategory.SECURITY,
        "g1", "a1", "v1", "hash1", FailActionType.ID);

    ApiContainerImageWaiverDTO waiverDTO = new ApiContainerImageWaiverDTO();
    waiverDTO.comment = "Container bulk waiver";

    apiPolicyWaiverService.addContainerImageWaiver(app.getId(), waiverDTO);

    List<PolicyWaiver> waivers = policyWaiverDAO.getActiveByOwnerId(app.getId());
    assertThat(waivers).hasSize(2);
    assertThat(waivers).allSatisfy(w -> assertThat(w.getComment()).isEqualTo("Container bulk waiver"));
    // Per-violation waiver stored with EXACT_COMPONENT + isForContainerImageComponent;
    // summary marker stored with ALL_COMPONENTS + isForContainerImage.
    assertThat(waivers).extracting(PolicyWaiver::isForContainerImage).containsExactlyInAnyOrder(true, false);
    assertThat(waivers).extracting(PolicyWaiver::isForContainerImageComponent)
        .containsExactlyInAnyOrder(true, false);

    assertTelemetry(OwnerType.APPLICATION, app.getPublicId(), 2);
  }

  @Test
  public void testAddBulkPolicyWaivers_WithExpiryTime() {
    Date futureDate = Date.from(Instant.now().plus(7, ChronoUnit.DAYS));

    ApiWaiverOptionsDTO waiverOptions = new ApiWaiverOptionsDTO();
    waiverOptions.comment = "Expiring waiver";
    waiverOptions.matcherStrategy = EXACT_COMPONENT;
    waiverOptions.expiryTime = futureDate;
    ApiBulkWaiversDTO bulkWaiversDTO = new ApiBulkWaiversDTO(
        Collections.singletonList(policyViolation.getId()),
        waiverOptions);

    apiPolicyWaiverService.addBulkPolicyWaivers(OwnerType.APPLICATION, app.getId(), bulkWaiversDTO);

    List<PolicyWaiver> waivers = policyWaiverDAO.getActiveByOwnerId(app.getId());
    assertThat(waivers).hasSize(1);
    assertThat(waivers.get(0).getExpiryTime()).isEqualTo(futureDate);
  }

  @Test
  public void testAddBulkPolicyWaivers_WithWaiverReason() {
    PolicyWaiverReason reason = policyWaiverReasonDAO.getAll().get(0);

    ApiWaiverOptionsDTO waiverOptions = new ApiWaiverOptionsDTO();
    waiverOptions.comment = "Waiver with reason";
    waiverOptions.matcherStrategy = EXACT_COMPONENT;
    waiverOptions.waiverReasonId = reason.getId();
    ApiBulkWaiversDTO bulkWaiversDTO = new ApiBulkWaiversDTO(
        Collections.singletonList(policyViolation.getId()),
        waiverOptions);

    apiPolicyWaiverService.addBulkPolicyWaivers(OwnerType.APPLICATION, app.getId(), bulkWaiversDTO);

    List<PolicyWaiver> waivers = policyWaiverDAO.getActiveByOwnerId(app.getId());
    assertThat(waivers).hasSize(1);
    assertThat(waivers.get(0).getWaiverReasonId()).isEqualTo(reason.getId());
  }

  @Test
  public void testAddBulkPolicyWaivers_WithExpireWhenRemediationAvailable() {
    ApiWaiverOptionsDTO waiverOptions = new ApiWaiverOptionsDTO();
    waiverOptions.comment = "Auto-expire waiver";
    waiverOptions.matcherStrategy = EXACT_COMPONENT;
    waiverOptions.expireWhenRemediationAvailable = true;
    ApiBulkWaiversDTO bulkWaiversDTO = new ApiBulkWaiversDTO(
        Collections.singletonList(policyViolation.getId()),
        waiverOptions);

    apiPolicyWaiverService.addBulkPolicyWaivers(OwnerType.APPLICATION, app.getId(), bulkWaiversDTO);

    List<PolicyWaiver> waivers = policyWaiverDAO.getActiveByOwnerId(app.getId());
    assertThat(waivers).hasSize(1);
    assertThat(waivers.get(0).isExpireWhenRemediationAvailable()).isTrue();
  }

  @Test
  public void testAddBulkPolicyWaivers_ThrowsErrorOnInvalidViolationIds() {
    ApiWaiverOptionsDTO waiverOptions = new ApiWaiverOptionsDTO();
    waiverOptions.comment = "Partial success";
    waiverOptions.matcherStrategy = EXACT_COMPONENT;
    ApiBulkWaiversDTO bulkWaiversDTO = new ApiBulkWaiversDTO(
        Arrays.asList(policyViolation.getId(), "invalid-id-123"),
        waiverOptions);

    assertThatThrownBy(
        () -> apiPolicyWaiverService.addBulkPolicyWaivers(OwnerType.APPLICATION, app.getId(), bulkWaiversDTO))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Error processing policy violation with ID: invalid-id-123");

    List<PolicyWaiver> waivers = policyWaiverDAO.getActiveByOwnerId(app.getId());
    assertThat(waivers).hasSize(0);
  }

  @Test
  public void testAddBulkPolicyWaivers_SkipsExistingWaivers() {
    // First create a regular waiver
    ApiWaiverOptionsDTO waiverOptions = new ApiWaiverOptionsDTO();
    waiverOptions.comment = "Original waiver";
    waiverOptions.matcherStrategy = EXACT_COMPONENT;
    apiPolicyWaiverService.addPolicyWaiverByPolicyViolationId(
        OwnerType.APPLICATION, app.getId(), policyViolation.getId(), waiverOptions);

    // Now try to create bulk waiver with same violation
    waiverOptions = new ApiWaiverOptionsDTO();
    waiverOptions.comment = "Duplicate attempt";
    waiverOptions.matcherStrategy = EXACT_COMPONENT;
    ApiBulkWaiversDTO bulkWaiversDTO = new ApiBulkWaiversDTO(
        Collections.singletonList(policyViolation.getId()),
        waiverOptions);

    apiPolicyWaiverService.addBulkPolicyWaivers(OwnerType.APPLICATION, app.getId(), bulkWaiversDTO);

    // Should still have only 1 waiver (the original one)
    List<PolicyWaiver> waivers = policyWaiverDAO.getActiveByOwnerId(app.getId());
    assertThat(waivers).hasSize(1);
    assertThat(waivers.get(0).getComment()).isEqualTo("Original waiver");
  }

  @Test
  public void testAddBulkPolicyWaivers_NullRequest() {
    assertThatThrownBy(() -> apiPolicyWaiverService.addBulkPolicyWaivers(OwnerType.APPLICATION, app.getId(), null))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Waivers request cannot be null");
  }

  @Test
  public void testAddBulkPolicyWaivers_NullViolationIds() {
    ApiBulkWaiversDTO bulkWaiversDTO = new ApiBulkWaiversDTO(
        null,
        new ApiWaiverOptionsDTO());

    assertThatThrownBy(
        () -> apiPolicyWaiverService.addBulkPolicyWaivers(OwnerType.APPLICATION, app.getId(), bulkWaiversDTO))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Violation IDs list cannot be null or empty");
  }

  @Test
  public void testAddBulkPolicyWaivers_EmptyViolationIds() {
    ApiBulkWaiversDTO bulkWaiversDTO = new ApiBulkWaiversDTO(
        Collections.emptyList(),
        new ApiWaiverOptionsDTO());

    assertThatThrownBy(
        () -> apiPolicyWaiverService.addBulkPolicyWaivers(OwnerType.APPLICATION, app.getId(), bulkWaiversDTO))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Violation IDs list cannot be null or empty");
  }

  @Test
  public void testAddBulkPolicyWaivers_TooManyViolations() {
    List<String> tooManyViolations = new ArrayList<>();
    for (int i = 0; i < 1001; i++) {
      tooManyViolations.add("violation-" + i);
    }

    ApiWaiverOptionsDTO waiverOptions = new ApiWaiverOptionsDTO();
    waiverOptions.matcherStrategy = EXACT_COMPONENT;
    ApiBulkWaiversDTO bulkWaiversDTO = new ApiBulkWaiversDTO(
        tooManyViolations,
        waiverOptions);

    assertThatThrownBy(
        () -> apiPolicyWaiverService.addBulkPolicyWaivers(OwnerType.APPLICATION, app.getId(), bulkWaiversDTO))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Maximum " + MAX_BULK_WAIVER_VIOLATIONS + " violations allowed per waiver request");

    List<PolicyWaiver> waivers = policyWaiverDAO.getActiveByOwnerId(app.getId());
    assertThat(waivers).hasSize(0);
  }

  @Test
  public void testAddBulkPolicyWaivers_NullWaiverOptions() {
    ApiBulkWaiversDTO bulkWaiversDTO = new ApiBulkWaiversDTO(
        Collections.singletonList(policyViolation.getId()),
        null);

    assertThatThrownBy(
        () -> apiPolicyWaiverService.addBulkPolicyWaivers(OwnerType.APPLICATION, app.getId(), bulkWaiversDTO))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Waiver options cannot be null");
  }

  @Test
  public void testAddBulkPolicyWaivers_NullMatcherStrategy() {
    ApiWaiverOptionsDTO waiverOptions = new ApiWaiverOptionsDTO();
    waiverOptions.matcherStrategy = null;
    ApiBulkWaiversDTO bulkWaiversDTO = new ApiBulkWaiversDTO(
        Collections.singletonList(policyViolation.getId()),
        waiverOptions);

    assertThatThrownBy(
        () -> apiPolicyWaiverService.addBulkPolicyWaivers(OwnerType.APPLICATION, app.getId(), bulkWaiversDTO))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Matcher strategy is required");
  }

  @Test
  public void testAddBulkPolicyWaivers_InvalidMatcherStrategy() {
    ApiWaiverOptionsDTO waiverOptions = new ApiWaiverOptionsDTO();
    waiverOptions.matcherStrategy = ALL_COMPONENTS;
    ApiBulkWaiversDTO bulkWaiversDTO = new ApiBulkWaiversDTO(
        Collections.singletonList(policyViolation.getId()),
        waiverOptions);

    assertThatThrownBy(
        () -> apiPolicyWaiverService.addBulkPolicyWaivers(OwnerType.APPLICATION, app.getId(), bulkWaiversDTO))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Only EXACT_COMPONENT and ALL_VERSIONS matcher strategies are supported for bulk waivers");
  }

  @Test
  public void testAddBulkPolicyWaivers_ExpiryTimeInPast() {
    Date pastDate = Date.from(Instant.now().minus(1, ChronoUnit.DAYS));

    ApiWaiverOptionsDTO waiverOptions = new ApiWaiverOptionsDTO();
    waiverOptions.matcherStrategy = EXACT_COMPONENT;
    waiverOptions.expiryTime = pastDate;
    ApiBulkWaiversDTO bulkWaiversDTO = new ApiBulkWaiversDTO(
        Collections.singletonList(policyViolation.getId()),
        waiverOptions);

    assertThatThrownBy(
        () -> apiPolicyWaiverService.addBulkPolicyWaivers(OwnerType.APPLICATION, app.getId(), bulkWaiversDTO))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Expiration date must be in the future.");
  }

  @Test
  public void testAddBulkPolicyWaivers_ExpireWhenRemediationWithAllVersions() {
    ApiWaiverOptionsDTO waiverOptions = new ApiWaiverOptionsDTO();
    waiverOptions.matcherStrategy = ALL_VERSIONS;
    waiverOptions.expireWhenRemediationAvailable = true;
    ApiBulkWaiversDTO bulkWaiversDTO = new ApiBulkWaiversDTO(
        Collections.singletonList(policyViolation.getId()),
        waiverOptions);

    assertThatThrownBy(
        () -> apiPolicyWaiverService.addBulkPolicyWaivers(OwnerType.APPLICATION, app.getId(), bulkWaiversDTO))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Expire When Remediation Available Waivers can only be applied to Exact Components.");
  }

  @Test
  public void testAddBulkPolicyWaivers_AllVersionsWithoutComponentIdentifier() {
    policyViolation.setComponentIdentifier(null);
    policyViolationDAO.update(policyViolation);

    ApiWaiverOptionsDTO waiverOptions = new ApiWaiverOptionsDTO();
    waiverOptions.matcherStrategy = ALL_VERSIONS;
    ApiBulkWaiversDTO bulkWaiversDTO = new ApiBulkWaiversDTO(
        Collections.singletonList(policyViolation.getId()),
        waiverOptions);

    assertThatThrownBy(
        () -> apiPolicyWaiverService.addBulkPolicyWaivers(OwnerType.APPLICATION, app.getId(), bulkWaiversDTO))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Cannot create an ALL_VERSIONS waiver for a component that could not be identified.");

    assertThat(policyWaiverDAO.getActiveByOwnerId(app.getId())).isEmpty();
  }

  @Test
  public void testAddBulkPolicyWaivers_InvalidWaiverReason() {
    ApiWaiverOptionsDTO waiverOptions = new ApiWaiverOptionsDTO();
    waiverOptions.matcherStrategy = EXACT_COMPONENT;
    waiverOptions.waiverReasonId = "invalid-reason-id";
    ApiBulkWaiversDTO bulkWaiversDTO = new ApiBulkWaiversDTO(
        Collections.singletonList(policyViolation.getId()),
        waiverOptions);

    assertThatThrownBy(
        () -> apiPolicyWaiverService.addBulkPolicyWaivers(OwnerType.APPLICATION, app.getId(), bulkWaiversDTO))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Waiver reason not found");
  }

  @Test
  public void testAddBulkPolicyWaivers_NoValidViolations() {
    ApiWaiverOptionsDTO waiverOptions = new ApiWaiverOptionsDTO();
    waiverOptions.matcherStrategy = EXACT_COMPONENT;
    ApiBulkWaiversDTO bulkWaiversDTO = new ApiBulkWaiversDTO(
        Arrays.asList("invalid-1", "invalid-2", "invalid-3"),
        waiverOptions);

    assertThatThrownBy(
        () -> apiPolicyWaiverService.addBulkPolicyWaivers(OwnerType.APPLICATION, app.getId(), bulkWaiversDTO))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Error processing policy violation with ID: invalid-1");

    List<PolicyWaiver> waivers = policyWaiverDAO.getActiveByOwnerId(app.getId());
    assertThat(waivers).hasSize(0);
  }

  @Test
  public void testAddBulkPolicyWaivers_MaximumAllowedViolations() {
    // Create maximum allowed violation IDs (just at the limit)
    List<String> maxViolations = new ArrayList<>();
    for (int i = 0; i < MAX_BULK_WAIVER_VIOLATIONS; i++) {
      maxViolations.add(policyViolation.getId()); // Reuse same valid ID
    }

    ApiWaiverOptionsDTO waiverOptions = new ApiWaiverOptionsDTO();
    waiverOptions.comment = "Maximum violations test";
    waiverOptions.matcherStrategy = EXACT_COMPONENT;
    ApiBulkWaiversDTO bulkWaiversDTO = new ApiBulkWaiversDTO(
        maxViolations,
        waiverOptions);

    // Should not throw exception (but will only create 1 waiver due to duplicates)
    apiPolicyWaiverService.addBulkPolicyWaivers(OwnerType.APPLICATION, app.getId(), bulkWaiversDTO);

    List<PolicyWaiver> waivers = policyWaiverDAO.getActiveByOwnerId(app.getId());
    assertThat(waivers).hasSize(1); // Only 1 because all IDs are the same
  }

  @Test
  public void testCreateBulkWaiversInternal_SuccessfulTelemetryAndAudit() {
    // Given: Create multiple violations
    Policy policy2 = tempEntity.newPolicy(app);
    PolicyViolation violation2 = tempEntity.newPolicyViolation(policyEvaluation, policy2,
        5, PolicyThreatCategory.SECURITY, "g2", "a2", "v2", "hash2", FailActionType.ID);

    ApiWaiverOptionsDTO waiverOptions = new ApiWaiverOptionsDTO();
    waiverOptions.comment = "Bulk telemetry test";
    waiverOptions.matcherStrategy = EXACT_COMPONENT;
    ApiBulkWaiversDTO bulkWaiversDTO = new ApiBulkWaiversDTO(
        Arrays.asList(policyViolation.getId(), violation2.getId()),
        waiverOptions);

    // When: Create bulk waivers
    apiPolicyWaiverService.addBulkPolicyWaivers(OwnerType.APPLICATION, app.getId(), bulkWaiversDTO);

    // Then: Verify waivers were created successfully
    List<PolicyWaiver> waivers = policyWaiverDAO.getActiveByOwnerId(app.getId());
    assertThat(waivers).hasSize(2);

    // Verify telemetry was sent exactly once (for the entire bulk operation)
    verify(telemetrySenderMock, times(1)).send(any(TelemetryData.class));
    assertTelemetry(OwnerType.APPLICATION, app.getPublicId());

    // Verify waiver telemetry was sent for each individual waiver
    verify(policyWaiverTelemetryCreator, times(2)).sendWaiverTelemetryForOwnerType(
        any(PolicyWaiver.class), any(OwnerType.class), any(AbstractPolicyViolation.class));
  }

  @Test
  public void testCreateBulkWaiversInternal_FailureNoTelemetryOrAudit() {
    // Given: Create a violation with invalid waiver reason to cause failure
    ApiWaiverOptionsDTO waiverOptions = new ApiWaiverOptionsDTO();
    waiverOptions.comment = "Test failure";
    waiverOptions.matcherStrategy = EXACT_COMPONENT;
    waiverOptions.waiverReasonId = "invalid-reason-id";
    ApiBulkWaiversDTO bulkWaiversDTO = new ApiBulkWaiversDTO(
        Arrays.asList(policyViolation.getId()),
        waiverOptions);

    // When: Attempt to create bulk waivers (should fail)
    assertThatThrownBy(
        () -> apiPolicyWaiverService.addBulkPolicyWaivers(OwnerType.APPLICATION, app.getId(), bulkWaiversDTO))
            .isInstanceOf(BadRequestException.class);

    // Then: Verify no waivers were created
    List<PolicyWaiver> waivers = policyWaiverDAO.getActiveByOwnerId(app.getId());
    assertThat(waivers).hasSize(0);

    // Verify NO telemetry was sent (entire transaction failed)
    verifyNoInteractions(telemetrySenderMock);
    verifyNoInteractions(policyWaiverTelemetryCreator);
  }

  @Test
  public void testCreateBulkWaiversInternal_PartialFailure_NoTelemetryForFailedBatch() {
    // Given: Create mixed violations where one will fail
    Policy policy2 = tempEntity.newPolicy(app);
    PolicyViolation violation2 = tempEntity.newPolicyViolation(policyEvaluation, policy2,
        5, PolicyThreatCategory.SECURITY, "g2", "a2", "v2", "hash2", FailActionType.ID);

    ApiWaiverOptionsDTO waiverOptions = new ApiWaiverOptionsDTO();
    waiverOptions.comment = "Partial failure test";
    waiverOptions.matcherStrategy = EXACT_COMPONENT;
    waiverOptions.waiverReasonId = "invalid-reason-id"; // This will cause failure
    ApiBulkWaiversDTO bulkWaiversDTO = new ApiBulkWaiversDTO(
        Arrays.asList(policyViolation.getId(), violation2.getId()),
        waiverOptions);

    // When: Attempt to create bulk waivers (should fail completely)
    assertThatThrownBy(
        () -> apiPolicyWaiverService.addBulkPolicyWaivers(OwnerType.APPLICATION, app.getId(), bulkWaiversDTO))
            .isInstanceOf(BadRequestException.class);

    // Then: Verify NO waivers were created (atomic operation)
    List<PolicyWaiver> waivers = policyWaiverDAO.getActiveByOwnerId(app.getId());
    assertThat(waivers).hasSize(0);

    // Verify NO telemetry was sent (entire batch failed)
    verifyNoInteractions(telemetrySenderMock);
    verifyNoInteractions(policyWaiverTelemetryCreator);
  }

  @Test
  public void testCreateBulkWaiversInternal_DatabaseRollback_NoWaiversPersisted() {
    // Given: Multiple violations to create waivers for
    Policy policy2 = tempEntity.newPolicy(app);
    PolicyViolation violation2 = tempEntity.newPolicyViolation(policyEvaluation, policy2,
        5, PolicyThreatCategory.SECURITY, "g2", "a2", "v2", "hash2", FailActionType.ID);

    ApiWaiverOptionsDTO waiverOptions = new ApiWaiverOptionsDTO();
    waiverOptions.comment = "Transaction rollback test";
    waiverOptions.matcherStrategy = EXACT_COMPONENT;
    // Set invalid waiver reason to cause transaction failure
    waiverOptions.waiverReasonId = "invalid-reason-id";
    ApiBulkWaiversDTO bulkWaiversDTO = new ApiBulkWaiversDTO(
        Arrays.asList(policyViolation.getId(), violation2.getId()),
        waiverOptions);

    // When: Attempt bulk waiver creation that will fail
    assertThatThrownBy(
        () -> apiPolicyWaiverService.addBulkPolicyWaivers(OwnerType.APPLICATION, app.getId(), bulkWaiversDTO))
            .isInstanceOf(BadRequestException.class);

    // Then: Verify database state is clean (transaction rolled back)
    List<PolicyWaiver> waivers = policyWaiverDAO.getActiveByOwnerId(app.getId());
    assertThat(waivers).hasSize(0);

    // Verify no partial state was persisted to database
    List<PolicyWaiver> allWaivers = policyWaiverDAO.getAll();
    assertThat(allWaivers)
        .filteredOn(waiver -> waiver.getComment() != null && waiver.getComment().equals("Transaction rollback test"))
        .hasSize(0);
  }
}
