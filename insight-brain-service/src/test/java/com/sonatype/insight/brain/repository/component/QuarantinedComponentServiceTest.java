/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;

import com.sonatype.clm.dto.model.component.ComponentCategory;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.NamedComponentDetails;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiPageResult;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.dataaccess.repository.QuarantinedComponentAccessDAO;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.hds.HdsClient.RelayResponse;
import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupConditionType;
import com.sonatype.insight.brain.model.repository.QuarantinedComponentAccess;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.repository.RepositoryPolicyViolationDTO;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.google.inject.Binder;
import org.apache.commons.lang3.time.DateUtils;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static com.sonatype.insight.brain.repository.component.QuarantinedComponentService.QUARANTINED_COMPONENT_REPORT_ANONYMOUS_ACCESS_ENABLED;
import static com.sonatype.insight.brain.repository.component.QuarantinedComponentService.QUARANTINED_COMPONENT_REPORT_GENERATE_TIME;
import static com.sonatype.insight.brain.repository.component.QuarantinedComponentService.QUARANTINED_COMPONENT_REPORT_OBFUSCATED_COMPONENT_HASH;
import static com.sonatype.insight.brain.repository.component.QuarantinedComponentService.QUARANTINED_COMPONENT_REPORT_OBFUSCATED_TOKEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class QuarantinedComponentServiceTest
    extends AbstractComponentTest
{
  @Inject
  private QuarantinedComponentAccessDAO quarantinedComponentAccessDAO;

  @Inject
  private QuarantinedComponentService quarantinedComponentService;

  @Inject
  private Configuration configuration;

  @Mock
  private TelemetrySender telemetrySenderMock;

  @Mock
  private HdsClient hdsClientMock;

  @Mock
  private HttpServletRequest httpRequestMock;

  private Repository repository;

  @Before
  public void setup() {
    repository = tempEntity.newRepository();
  }

  @Override
  public void configure(Binder binder) {
    binder.bind(TelemetrySender.class).toInstance(telemetrySenderMock);
    binder.bind(HdsClient.class).toInstance(hdsClientMock);
    super.configure(binder);
  }

  @Test
  public void testGetQuarantinedComponent() {
    RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repository.getId());
    QuarantinedComponentAccess quarantinedComponentAccess =
        tempEntity.newQuarantinedComponentAccess(repository.getId(), repositoryComponent.getId());
    String encodedToken = encodeToken(quarantinedComponentAccess);

    QuarantinedComponentDto quarantinedComponentDto = quarantinedComponentService.getQuarantinedComponent(encodedToken);

    assertThat(quarantinedComponentDto.success).isTrue();
    assertThat(quarantinedComponentDto.repositoryComponentId).isEqualTo(repositoryComponent.getId());
    verify(telemetrySenderMock, never()).send(any(TelemetryData.class));
  }

  @Test
  public void testGetQuarantinedComponent_ExpiredToken() {
    RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repository.getId());
    QuarantinedComponentAccess expiredQuarantinedComponentAccess = tempEntity.newQuarantinedComponentAccess(
        repository.getId(), repositoryComponent.getId(), DateUtils.addDays(new Date(), -3));
    String encodedToken = encodeToken(expiredQuarantinedComponentAccess);

    assertThatThrownBy(() -> quarantinedComponentService.getQuarantinedComponentOverview(encodedToken))
        .isInstanceOf(NotFoundException.class)
        .hasMessageStartingWith("This report expired on ")
        .hasMessageEndingWith("You may generate a new report by requesting the blocked component again.");
  }

  @Test
  public void testGetQuarantinedComponent_TokenDoesNotExist() {
    String encodedToken = encodeToken("token");

    assertThatThrownBy(() -> quarantinedComponentService.getQuarantinedComponentOverview(encodedToken)).isInstanceOf(
        NotFoundException.class).hasMessage(
        "The quarantined component view for the blocked component you are trying to view could not be found.");
  }

  @Test
  public void testGetQuarantinedComponent_InvalidToken() {
    // The token is not base64 encoded
    assertThatThrownBy(() -> quarantinedComponentService.getQuarantinedComponentOverview("token"))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("The quarantined component view cannot be retrieved because the URL contains invalid characters.");
  }

  @Test
  public void testGetQuarantinedComponentOverview_Quarantined() {
    //setup
    Date date = new Date();
    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("com.lingocoder", "abi.cli", "0.5.2", null /* classifier */, "jar");
    QuarantinedComponentAccess quarantinedComponentAccess = setupTestData(componentIdentifier, date, date, null);
    String encodedToken = encodeToken(quarantinedComponentAccess);
    DbQuarantinedComponentAccessManager dbQuarantinedComponentAccessManager =
        new DbQuarantinedComponentAccessManager(quarantinedComponentAccessDAO, configuration);
    Date expirationTime = dbQuarantinedComponentAccessManager.getTokenExpiryTime(date);

    //when
    QuarantinedComponentOverviewDto quarantinedComponentOverviewDto =
        quarantinedComponentService.getQuarantinedComponentOverview(encodedToken);

    //then
    assertThat(quarantinedComponentOverviewDto.componentIdentifier).usingRecursiveComparison().isEqualTo(
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier));
    assertThat(quarantinedComponentOverviewDto.componentHash).isEqualTo("testHash");
    assertThat(quarantinedComponentOverviewDto.matchState).isEqualTo(MatchState.EXACT.toString());
    assertThat(quarantinedComponentOverviewDto.pathname).isEqualTo("com/lingocoder/abi.cli/0.5.2/abi.cli-0.5.2.jar");
    assertThat(quarantinedComponentOverviewDto.componentDisplayName).isEqualTo("com.lingocoder : abi.cli : 0.5.2");
    assertThat(quarantinedComponentOverviewDto.isQuarantined).isTrue();
    assertThat(quarantinedComponentOverviewDto.quarantinedPolicyViolationsCount).isEqualTo(2);
    assertThat(quarantinedComponentOverviewDto.repositoryId).isEqualTo(repository.getId());
    assertThat(quarantinedComponentOverviewDto.repositoryName).isEqualTo(repository.getPublicId());
    assertThat(quarantinedComponentOverviewDto.quarantinedDate).isEqualTo(date);
    assertThat(quarantinedComponentOverviewDto.componentVersion).isEqualTo("0.5.2");
    assertThat(quarantinedComponentOverviewDto.tokenExpiryTime).isEqualTo(expirationTime);
    assertTelemetry(encodedToken, quarantinedComponentAccess.getGenerateTime(), "testHash");
  }

  @Test
  public void testGetQuarantinedComponentOverview_NotQuarantined() {
    //setup
    Date date = new Date();
    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("com.lingocoder", "abi.cli", "0.5.2", null /* classifier */, "jar");
    QuarantinedComponentAccess quarantinedComponentAccess = setupTestData(componentIdentifier, date, null, null);
    String encodedToken = encodeToken(quarantinedComponentAccess);
    DbQuarantinedComponentAccessManager dbQuarantinedComponentAccessManager =
        new DbQuarantinedComponentAccessManager(quarantinedComponentAccessDAO, configuration);
    Date expirationTime = dbQuarantinedComponentAccessManager.getTokenExpiryTime(date);

    //when
    QuarantinedComponentOverviewDto quarantinedComponentOverviewDto =
        quarantinedComponentService.getQuarantinedComponentOverview(encodedToken);

    //then
    assertThat(quarantinedComponentOverviewDto.componentIdentifier).usingRecursiveComparison().isEqualTo(
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier));
    assertThat(quarantinedComponentOverviewDto.componentHash).isEqualTo("testHash");
    assertThat(quarantinedComponentOverviewDto.matchState).isEqualTo(MatchState.EXACT.toString());
    assertThat(quarantinedComponentOverviewDto.pathname).isEqualTo("com/lingocoder/abi.cli/0.5.2/abi.cli-0.5.2.jar");
    assertThat(quarantinedComponentOverviewDto.componentDisplayName).isEqualTo("com.lingocoder : abi.cli : 0.5.2");
    assertThat(quarantinedComponentOverviewDto.isQuarantined).isFalse();
    assertThat(quarantinedComponentOverviewDto.quarantinedPolicyViolationsCount).isEqualTo(2);
    assertThat(quarantinedComponentOverviewDto.repositoryId).isEqualTo(repository.getId());
    assertThat(quarantinedComponentOverviewDto.repositoryName).isEqualTo(repository.getPublicId());
    assertThat(quarantinedComponentOverviewDto.quarantinedDate).isNull();
    assertThat(quarantinedComponentOverviewDto.componentVersion).isEqualTo("0.5.2");
    assertThat(quarantinedComponentOverviewDto.tokenExpiryTime).isEqualTo(expirationTime);
    assertTelemetry(encodedToken, quarantinedComponentAccess.getGenerateTime(), "testHash");
  }

  @Test
  public void testGetQuarantinedComponentOverview_Unquarantined() {
    //setup
    Date date = new Date();
    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("com.lingocoder", "abi.cli", "0.5.2", null /* classifier */, "jar");
    QuarantinedComponentAccess quarantinedComponentAccess = setupTestData(componentIdentifier, date, date, date);
    String encodedToken = encodeToken(quarantinedComponentAccess);
    DbQuarantinedComponentAccessManager dbQuarantinedComponentAccessManager =
        new DbQuarantinedComponentAccessManager(quarantinedComponentAccessDAO, configuration);
    Date expirationTime = dbQuarantinedComponentAccessManager.getTokenExpiryTime(date);

    //when
    QuarantinedComponentOverviewDto quarantinedComponentOverviewDto =
        quarantinedComponentService.getQuarantinedComponentOverview(encodedToken);

    //then
    assertThat(quarantinedComponentOverviewDto.componentIdentifier).usingRecursiveComparison().isEqualTo(
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier));
    assertThat(quarantinedComponentOverviewDto.componentHash).isEqualTo("testHash");
    assertThat(quarantinedComponentOverviewDto.matchState).isEqualTo(MatchState.EXACT.toString());
    assertThat(quarantinedComponentOverviewDto.pathname).isEqualTo("com/lingocoder/abi.cli/0.5.2/abi.cli-0.5.2.jar");
    assertThat(quarantinedComponentOverviewDto.componentDisplayName).isEqualTo("com.lingocoder : abi.cli : 0.5.2");
    assertThat(quarantinedComponentOverviewDto.isQuarantined).isFalse();
    assertThat(quarantinedComponentOverviewDto.quarantinedPolicyViolationsCount).isEqualTo(2);
    assertThat(quarantinedComponentOverviewDto.repositoryId).isEqualTo(repository.getId());
    assertThat(quarantinedComponentOverviewDto.repositoryName).isEqualTo(repository.getPublicId());
    assertThat(quarantinedComponentOverviewDto.quarantinedDate).isEqualTo(date);
    assertThat(quarantinedComponentOverviewDto.componentVersion).isEqualTo("0.5.2");
    assertThat(quarantinedComponentOverviewDto.tokenExpiryTime).isEqualTo(expirationTime);
    assertTelemetry(encodedToken, quarantinedComponentAccess.getGenerateTime(), "testHash");
  }

  @Test
  public void testGetQuarantinedComponentOverview_UnknownComponent() {
    Date date = new Date();
    QuarantinedComponentAccess quarantinedComponentAccess =
        setupTestData(null /* componentIdentifier */, date, date, null);
    String encodedToken = encodeToken(quarantinedComponentAccess);
    DbQuarantinedComponentAccessManager dbQuarantinedComponentAccessManager =
        new DbQuarantinedComponentAccessManager(quarantinedComponentAccessDAO, configuration);
    Date expirationTime = dbQuarantinedComponentAccessManager.getTokenExpiryTime(date);

    QuarantinedComponentOverviewDto quarantinedComponentOverviewDto =
        quarantinedComponentService.getQuarantinedComponentOverview(encodedToken);

    assertThat(quarantinedComponentOverviewDto.componentIdentifier).isNull();
    assertThat(quarantinedComponentOverviewDto.componentHash).isEqualTo("testHash");
    assertThat(quarantinedComponentOverviewDto.matchState).isEqualTo(MatchState.EXACT.toString());
    assertThat(quarantinedComponentOverviewDto.pathname).isEqualTo("testPathname");
    assertThat(quarantinedComponentOverviewDto.componentDisplayName).isEqualTo("testPathname (testPathname)");
    assertThat(quarantinedComponentOverviewDto.isQuarantined).isTrue();
    assertThat(quarantinedComponentOverviewDto.quarantinedPolicyViolationsCount).isEqualTo(2);
    assertThat(quarantinedComponentOverviewDto.repositoryId).isEqualTo(repository.getId());
    assertThat(quarantinedComponentOverviewDto.repositoryName).isEqualTo(repository.getPublicId());
    assertThat(quarantinedComponentOverviewDto.quarantinedDate).isEqualTo(date);
    assertThat(quarantinedComponentOverviewDto.componentVersion).isNull();
    assertThat(quarantinedComponentOverviewDto.tokenExpiryTime).isEqualTo(expirationTime);
    assertTelemetry(encodedToken, quarantinedComponentAccess.getGenerateTime(), "testHash");
  }

  @Test
  public void testGetQuarantinedComponentOverview_ExpiredToken() {
    RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repository.getId());
    QuarantinedComponentAccess expiredQuarantinedComponentAccess = tempEntity.newQuarantinedComponentAccess(
        repository.getId(), repositoryComponent.getId(), DateUtils.addDays(new Date(), -3));
    String encodedToken = encodeToken(expiredQuarantinedComponentAccess);

    assertThatThrownBy(() -> quarantinedComponentService.getQuarantinedComponentOverview(encodedToken))
        .isInstanceOf(NotFoundException.class)
        .hasMessageStartingWith("This report expired on ")
        .hasMessageEndingWith("You may generate a new report by requesting the blocked component again.");
  }

  @Test
  public void testGetQuarantinedComponentOverview_TokenDoesNotExist() {
    String encodedToken = encodeToken("token");

    assertThatThrownBy(() -> quarantinedComponentService.getQuarantinedComponentOverview(encodedToken))
        .isInstanceOf(NotFoundException.class)
        .hasMessage(
            "The quarantined component view for the blocked component you are trying to view could not be found.");
  }

  @Test
  public void testGetQuarantinedComponentOverview_InvalidToken() {
    // The token is not base64 encoded
    assertThatThrownBy(() -> quarantinedComponentService.getQuarantinedComponentOverview("token"))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("The quarantined component view cannot be retrieved because the URL contains invalid characters.");
  }

  private QuarantinedComponentAccess setupTestData(
      ComponentIdentifier componentIdentifier,
      Date time,
      Date quarantinedTime,
      Date unquarantinedTime)
  {
    String hash = "testHash";
    String pathname = "testPathname";
    if (componentIdentifier != null) {
      pathname = componentIdentifier.get(ComponentIdentifier.MAVEN_GROUP_ID).replace(".", "/") + "/"
          + componentIdentifier.get(ComponentIdentifier.MAVEN_ARTIFACT_ID) + "/"
          + componentIdentifier.get(ComponentIdentifier.VERSION) + "/"
          + componentIdentifier.get(ComponentIdentifier.MAVEN_ARTIFACT_ID) + "-"
          + componentIdentifier.get(ComponentIdentifier.VERSION) + "."
          + componentIdentifier.get(ComponentIdentifier.MAVEN_EXTENSION);
    }
    final RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repository.getId(),
        MatchState.EXACT, pathname, hash, componentIdentifier, time, quarantinedTime, unquarantinedTime);

    tempEntity.newRepositoryPolicyViolation(repository.getId(), 6,
        repositoryComponent.getPathname(), false, "fail", "policyId", "policyName",
        repositoryComponent.getComponentIdentifier(), time);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 6,
        repositoryComponent.getPathname(), false, "fail", "policyId", "policyName",
        repositoryComponent.getComponentIdentifier(), time);
    QuarantinedComponentAccess quarantinedComponentAccess =
        tempEntity.newQuarantinedComponentAccess(repository.getId(), repositoryComponent.getId(), time);
    return quarantinedComponentAccess;
  }

  @Test
  public void testGetQuarantinedComponentPolicyViolations() {
    // setup
    Date date = new Date();
    final RepositoryComponent repositoryComponent =
        tempEntity.newRepositoryComponent(repository.getId(), "path", date, null);
    List<ConstraintFact> constraintFacts = new ArrayList<>();
    ConstraintFact constraintFact =
        new ConstraintFact(UUID.randomUUID().toString(), "constraintName", "and");
    ConditionFact conditionFact = new ConditionFact(LicenseThreatGroupConditionType.ID,
        0, "some summary", "some reason");
    conditionFact.setTriggerJson("some trigger");
    constraintFact.addConditionFact(conditionFact);
    constraintFacts.add(constraintFact);
    Policy policy1 = tempEntity.newPolicy();
    RepositoryPolicyViolation violation1 = tempEntity.newRepositoryPolicyViolation(repository.getId(), 5,
        repositoryComponent.getPathname(), "hash", constraintFacts, false /* isWaived */, Action.ID_FAIL,
        policy1.getId(), policy1.getName(), repositoryComponent.getComponentIdentifier(), date, null, null, null);
    RepositoryPolicyViolation violation2 =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), 10, repositoryComponent.getPathname(),
            "hash", constraintFacts, false /* isWaived */, Action.ID_FAIL, "policyid_2",
            "policyname_2", repositoryComponent.getComponentIdentifier(), date, null,
            null, null);
    QuarantinedComponentAccess quarantinedComponentAccess =
        tempEntity
            .newQuarantinedComponentAccess(repository.getId(), repositoryComponent.getId(), date);
    String encodedToken = encodeToken(quarantinedComponentAccess);

    // when
    List<RepositoryPolicyViolationDTO> repositoryPolicyViolationDTOs =
        quarantinedComponentService.getQuarantinedComponentPolicyViolations(encodedToken);

    // then
    assertThat(repositoryPolicyViolationDTOs).hasSize(2);

    RepositoryPolicyViolationDTO policyViolationDTO = repositoryPolicyViolationDTOs.get(0);
    assertThat(policyViolationDTO.policyViolationId).isEqualTo(violation2.getId());
    assertThat(policyViolationDTO.policyId).isEqualTo(violation2.getPolicyId());
    assertThat(policyViolationDTO.policyName).isEqualTo(violation2.getPolicyName());
    assertThat(policyViolationDTO.policyOwner.ownerId).isNull();
    assertThat(policyViolationDTO.policyOwner.ownerName).isNull();
    assertThat(policyViolationDTO.policyOwner.ownerType).isNull();
    assertThat(policyViolationDTO.policyThreatLevel).isEqualTo(violation2.getThreatLevel());
    assertThat(policyViolationDTO.policyThreatCategory).isEqualTo(violation2.getThreatCategory());
    assertThat(policyViolationDTO.constraints).hasSize(1);
    assertThat(policyViolationDTO.constraints.get(0).constraintId).isEqualTo(constraintFact.getConstraintId());
    assertThat(policyViolationDTO.constraints.get(0).constraintName).isEqualTo(constraintFact.getConstraintName());
    assertThat(policyViolationDTO.constraints.get(0).conditions.get(0).conditionReason).isEqualTo(
        conditionFact.getReason());
    assertThat(policyViolationDTO.constraints.get(0).conditions.get(0).conditionSummary).isEqualTo(
        conditionFact.getSummary());
    assertThat(policyViolationDTO.constraintFactsJson).isEqualTo(violation2.getConstraintFactsJson());
    assertThat(policyViolationDTO.waived).isEqualTo(violation2.isWaived());
    assertThat(policyViolationDTO.policyActionTypeId).isEqualTo(Action.ID_FAIL);
    assertThat(policyViolationDTO.lastReported).isEqualTo(violation2.getTime());

    policyViolationDTO = repositoryPolicyViolationDTOs.get(1);
    assertThat(policyViolationDTO.policyViolationId).isEqualTo(violation1.getId());
    assertThat(policyViolationDTO.policyId).isEqualTo(violation1.getPolicyId());
    assertThat(policyViolationDTO.policyName).isEqualTo(violation1.getPolicyName());
    assertThat(policyViolationDTO.policyOwner.ownerId).isEqualTo(Organization.ROOT_ORGANIZATION_ID);
    assertThat(policyViolationDTO.policyOwner.ownerName).isEqualTo("Root Organization");
    assertThat(policyViolationDTO.policyOwner.ownerType).isEqualTo(OwnerType.ORGANIZATION.toString());
    assertThat(policyViolationDTO.policyThreatLevel).isEqualTo(violation1.getThreatLevel());
    assertThat(policyViolationDTO.policyThreatCategory).isEqualTo(violation1.getThreatCategory());
    assertThat(policyViolationDTO.constraints).hasSize(1);
    assertThat(policyViolationDTO.constraints.get(0).constraintId).isEqualTo(constraintFact.getConstraintId());
    assertThat(policyViolationDTO.constraints.get(0).constraintName).isEqualTo(constraintFact.getConstraintName());
    assertThat(policyViolationDTO.constraints.get(0).conditions.get(0).conditionReason)
        .isEqualTo(conditionFact.getReason());
    assertThat(policyViolationDTO.constraints.get(0).conditions.get(0).conditionSummary)
        .isEqualTo(conditionFact.getSummary());
    assertThat(policyViolationDTO.constraintFactsJson).isEqualTo(violation1.getConstraintFactsJson());
    assertThat(policyViolationDTO.waived).isEqualTo(violation1.isWaived());
    assertThat(policyViolationDTO.policyActionTypeId).isEqualTo(Action.ID_FAIL);
    assertThat(policyViolationDTO.lastReported).isEqualTo(violation1.getTime());

    verify(telemetrySenderMock, never()).send(any(TelemetryData.class));
  }

  @Test
  public void testGetQuarantinedComponentPolicyViolations_WaivedPolicyViolation() {
    // setup
    Date date = new Date();
    final RepositoryComponent repositoryComponent =
        tempEntity.newRepositoryComponent(repository.getId(), "path", date, null);
    List<ConstraintFact> constraintFacts = new ArrayList<>();
    ConstraintFact constraintFact = new ConstraintFact(UUID.randomUUID().toString(), "constraintName", "and");
    ConditionFact conditionFact =
        new ConditionFact(LicenseThreatGroupConditionType.ID, 0, "some summary", "some reason");
    conditionFact.setTriggerJson("some trigger");
    constraintFact.addConditionFact(conditionFact);
    constraintFacts.add(constraintFact);
    Policy policy = tempEntity.newPolicy();
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 5, repositoryComponent.getPathname(), "hash",
        constraintFacts, true /* isWaived */, Action.ID_FAIL, policy.getId(), policy.getName(),
        repositoryComponent.getComponentIdentifier(), date, null, null, null);
    QuarantinedComponentAccess quarantinedComponentAccess =
        tempEntity.newQuarantinedComponentAccess(repository.getId(), repositoryComponent.getId(), date);
    String encodedToken = encodeToken(quarantinedComponentAccess);

    // when
    List<RepositoryPolicyViolationDTO> repositoryPolicyViolationDTOs =
        quarantinedComponentService.getQuarantinedComponentPolicyViolations(encodedToken);

    // then
    assertThat(repositoryPolicyViolationDTOs).isEmpty();

    verify(telemetrySenderMock, never()).send(any(TelemetryData.class));
  }

  @Test
  public void testGetQuarantinedComponentPolicyViolations_ExpiredToken() {
    RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repository.getId());
    QuarantinedComponentAccess expiredQuarantinedComponentAccess = tempEntity.newQuarantinedComponentAccess(
        repository.getId(), repositoryComponent.getId(), DateUtils.addDays(new Date(), -3));
    String encodedToken = encodeToken(expiredQuarantinedComponentAccess);

    assertThatThrownBy(() -> quarantinedComponentService.getQuarantinedComponentPolicyViolations(encodedToken))
        .isInstanceOf(NotFoundException.class)
        .hasMessageStartingWith("This report expired on ")
        .hasMessageEndingWith("You may generate a new report by requesting the blocked component again.");
  }

  @Test
  public void testGetQuarantinedComponentPolicyViolations_TokenDoesNotExist() {
    String encodedToken = encodeToken("token");

    assertThatThrownBy(() -> quarantinedComponentService.getQuarantinedComponentPolicyViolations(encodedToken))
        .isInstanceOf(NotFoundException.class)
        .hasMessage(
            "The quarantined component view for the blocked component you are trying to view could not be found.");
  }

  @Test
  public void testGetQuarantinedComponentPolicyViolations_InvalidToken() {
    // The token is not base64 encoded
    assertThatThrownBy(() -> quarantinedComponentService.getQuarantinedComponentPolicyViolations("token"))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("The quarantined component view cannot be retrieved because the URL contains invalid characters.");
  }

  @Test
  public void testGetQuarantinedComponentOtherVersions() {
    // setup
    Date date = new Date();
    // Quarantined component
    RepositoryComponent repositoryComponent =
        tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
            "com/lingocoder/abi.cli/0.5.3/abi.cli-0.5.1.jar", "hash", ComponentIdentifier
                .createMavenCoordinates("com.lingocoder", "abi.cli", "0.5.1", null /* classifier */, "jar"),
            date, new DateTime(date).minusDays(1).toDate(), null);
    // Never quarantined component
    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
        "com/lingocoder/abi.cli/0.5.3/abi.cli-0.5.2.jar", "hash",
        ComponentIdentifier.createMavenCoordinates("com.lingocoder", "abi.cli", "0.5.2", null /* classifier */, "jar"),
        date, null);
    // Quarantined component
    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
        "com/lingocoder/abi.cli/0.5.3/abi.cli-0.5.3.jar", "hash",
        ComponentIdentifier.createMavenCoordinates("com.lingocoder", "abi.cli", "0.5.3", null /* classifier */, "jar"),
        date, date, null);
    // Unquarantined component
    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
        "com/lingocoder/abi.cli/0.5.3/abi.cli-0.5.4.jar", "hash",
        ComponentIdentifier.createMavenCoordinates("com.lingocoder", "abi.cli", "0.5.4", null /* classifier */, "jar"),
        date, new DateTime(date).minusDays(1).toDate(), date);
    // Unrelated never quarantined component
    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "g/a/v/a-v.jar", "hash",
        ComponentIdentifier.createMavenCoordinates("g", "a", "v", null /* classifier */, "jar"), date, null, null);
    QuarantinedComponentAccess quarantinedComponentAccess =
        tempEntity.newQuarantinedComponentAccess(repository.getId(), repositoryComponent.getId(), date);

    String encodedToken = encodeToken(quarantinedComponentAccess);

    // when - ascending order
    ApiPageResult<String> result =
        quarantinedComponentService.getQuarantinedComponentOtherVersions(encodedToken, 1, 5, true /* asc */);

    // then
    assertThat(result.getTotal()).isEqualTo(2);
    assertThat(result.getPage()).isEqualTo(1);
    assertThat(result.getPageSize()).isEqualTo(5);
    assertThat(result.getPageCount()).isEqualTo(1);
    assertThat(result.getResults()).containsExactly("com.lingocoder : abi.cli : 0.5.2",
        "com.lingocoder : abi.cli : 0.5.4");
    verify(telemetrySenderMock, never()).send(any(TelemetryData.class));

    // when - descending order
    result = quarantinedComponentService.getQuarantinedComponentOtherVersions(encodedToken, 1, 5, false /* asc */);

    // then
    assertThat(result.getTotal()).isEqualTo(2);
    assertThat(result.getPage()).isEqualTo(1);
    assertThat(result.getPageSize()).isEqualTo(5);
    assertThat(result.getPageCount()).isEqualTo(1);
    assertThat(result.getResults()).containsExactly("com.lingocoder : abi.cli : 0.5.4",
        "com.lingocoder : abi.cli : 0.5.2");
    verify(telemetrySenderMock, never()).send(any(TelemetryData.class));

    // when - other page size
    result = quarantinedComponentService.getQuarantinedComponentOtherVersions(encodedToken, 1, 1, true /* asc */);

    // then
    assertThat(result.getTotal()).isEqualTo(2);
    assertThat(result.getPage()).isEqualTo(1);
    assertThat(result.getPageSize()).isEqualTo(1);
    assertThat(result.getPageCount()).isEqualTo(2);
    assertThat(result.getResults()).containsExactly("com.lingocoder : abi.cli : 0.5.2");
    verify(telemetrySenderMock, never()).send(any(TelemetryData.class));

    // when - not first page
    result = quarantinedComponentService.getQuarantinedComponentOtherVersions(encodedToken, 2, 1, true /* asc */);

    // then
    assertThat(result.getTotal()).isEqualTo(2);
    assertThat(result.getPage()).isEqualTo(2);
    assertThat(result.getPageSize()).isEqualTo(1);
    assertThat(result.getPageCount()).isEqualTo(2);
    assertThat(result.getResults()).containsExactly("com.lingocoder : abi.cli : 0.5.4");
    verify(telemetrySenderMock, never()).send(any(TelemetryData.class));
  }

  @Test
  public void testGetQuarantinedComponentOtherVersions_NoOtherVersions() {
    // setup
    Date date = new Date();
    // Quarantined component
    RepositoryComponent repositoryComponent =
        tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
            "com/lingocoder/abi.cli/0.5.3/abi.cli-0.5.1.jar", "hash", ComponentIdentifier
                .createMavenCoordinates("com.lingocoder", "abi.cli", "0.5.1", null /* classifier */, "jar"),
            date, new DateTime(date).minusDays(1).toDate(), null);
    QuarantinedComponentAccess quarantinedComponentAccess =
        tempEntity.newQuarantinedComponentAccess(repository.getId(), repositoryComponent.getId(), date);

    String encodedToken = encodeToken(quarantinedComponentAccess);

    // when
    ApiPageResult<String> result =
        quarantinedComponentService.getQuarantinedComponentOtherVersions(encodedToken, 1, 5, true /* asc */);

    // then
    assertThat(result.getTotal()).isEqualTo(0);
    assertThat(result.getPage()).isEqualTo(1);
    assertThat(result.getPageSize()).isEqualTo(5);
    assertThat(result.getPageCount()).isEqualTo(0);
    assertThat(result.getResults()).isEmpty();
    verify(telemetrySenderMock, never()).send(any(TelemetryData.class));
  }

  @Test
  public void testGetQuarantinedComponentOtherVersions_MatchAllCoordinates() {
    // setup
    Date date = new Date();
    // Quarantined component
    RepositoryComponent repositoryComponent =
            tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
                    "com/lingocoder/abi.cli/0.5.1/abi.cli-0.5.1.jar", "hash", ComponentIdentifier
                            .createMavenCoordinates("com.lingocoder", "abi.cli", "0.5.1", null /* classifier */, "jar"),
                    date, new DateTime(date).minusDays(1).toDate(), null);
    // Never quarantined component
    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
            "com/lingocoder/abi.cli/0.5.3/abi.cli-0.5.3-sources.jar", "hash",
            ComponentIdentifier
                    .createMavenCoordinates("com.lingocoder", "abi.cli", "0.5.3", "source" /* classifier */, "jar"),
            date, null);
    QuarantinedComponentAccess quarantinedComponentAccess =
            tempEntity.newQuarantinedComponentAccess(repository.getId(), repositoryComponent.getId(), date);

    String encodedToken = encodeToken(quarantinedComponentAccess);

    // when
    ApiPageResult<String> result =
            quarantinedComponentService.getQuarantinedComponentOtherVersions(encodedToken, 1, 5, true /* asc */);

    // then
    assertThat(result.getTotal()).isEqualTo(0);
    assertThat(result.getPage()).isEqualTo(1);
    assertThat(result.getPageSize()).isEqualTo(5);
    assertThat(result.getPageCount()).isEqualTo(0);
    assertThat(result.getResults()).isEmpty();
    verify(telemetrySenderMock, never()).send(any(TelemetryData.class));
  }

  @Test
  public void testGetQuarantinedComponentOtherVersions_npm() {
    //given
    Date date = new Date();
    // Quarantined component
    RepositoryComponent repositoryComponent =
            tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
                    "comp1/-/comp1-1.tgz", "hash1-1", ComponentIdentifier
                            .createNpmCoordinates("comp1", "3"), true);
    // Never quarantined component
    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
            "comp1/-/comp1-2.tgz", "hash1-2", ComponentIdentifier
                    .createNpmCoordinates("comp1", "2"), false);

    QuarantinedComponentAccess quarantinedComponentAccess =
            tempEntity.newQuarantinedComponentAccess(repository.getId(), repositoryComponent.getId(), date);

    String encodedToken = encodeToken(quarantinedComponentAccess);

    // when
    ApiPageResult<String> result =
            quarantinedComponentService.getQuarantinedComponentOtherVersions(encodedToken, 1, 5, true /* asc */);

    // then
    assertThat(result.getTotal()).isEqualTo(1);
    assertThat(result.getPage()).isEqualTo(1);
    assertThat(result.getPageSize()).isEqualTo(5);
    assertThat(result.getPageCount()).isEqualTo(1);
    assertThat(result.getResults()).containsExactly("comp1 : 2");
    verify(telemetrySenderMock, never()).send(any(TelemetryData.class));
  }

  @Test
  public void testGetQuarantinedComponentOtherVersions_InvalidPage() {
    Date date = new Date();
    // Quarantined component
    RepositoryComponent repositoryComponent =
        tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
            "com/lingocoder/abi.cli/0.5.3/abi.cli-0.5.1.jar", "hash", ComponentIdentifier
                .createMavenCoordinates("com.lingocoder", "abi.cli", "0.5.1", null /* classifier */, "jar"),
            date, new DateTime(date).minusDays(1).toDate(), null);
    QuarantinedComponentAccess quarantinedComponentAccess =
        tempEntity.newQuarantinedComponentAccess(repository.getId(), repositoryComponent.getId(), date);

    String encodedToken = encodeToken(quarantinedComponentAccess);

    assertThatThrownBy(() -> quarantinedComponentService.getQuarantinedComponentOtherVersions(encodedToken, 0, 5,
        true /* asc */)).isInstanceOf(BadRequestException.class)
        .hasMessage("Page and Page size must be greater than 0");
  }

  @Test
  public void testGetQuarantinedComponentOtherVersions_InvalidPageSize() {
    Date date = new Date();
    // Quarantined component
    RepositoryComponent repositoryComponent =
        tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
            "com/lingocoder/abi.cli/0.5.3/abi.cli-0.5.1.jar", "hash", ComponentIdentifier
                .createMavenCoordinates("com.lingocoder", "abi.cli", "0.5.1", null /* classifier */, "jar"),
            date, new DateTime(date).minusDays(1).toDate(), null);
    QuarantinedComponentAccess quarantinedComponentAccess =
        tempEntity.newQuarantinedComponentAccess(repository.getId(), repositoryComponent.getId(), date);

    String encodedToken = encodeToken(quarantinedComponentAccess);

    assertThatThrownBy(() -> quarantinedComponentService.getQuarantinedComponentOtherVersions(encodedToken, 1, 0,
        true /* asc */)).isInstanceOf(BadRequestException.class)
        .hasMessage("Page and Page size must be greater than 0");
  }

  @Test
  public void testGetQuarantinedComponentOtherVersions_ExpiredToken() {
    RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repository.getId());
    QuarantinedComponentAccess expiredQuarantinedComponentAccess = tempEntity.newQuarantinedComponentAccess(
        repository.getId(), repositoryComponent.getId(), DateUtils.addDays(new Date(), -3));
    String encodedToken = encodeToken(expiredQuarantinedComponentAccess);

    assertThatThrownBy(() -> quarantinedComponentService.getQuarantinedComponentOtherVersions(encodedToken, 1, 5,
        true /* asc */)).isInstanceOf(NotFoundException.class).hasMessageStartingWith("This report expired on ")
        .hasMessageEndingWith("You may generate a new report by requesting the blocked component again.");
  }

  @Test
  public void testGetQuarantinedComponentOtherVersions_TokenDoesNotExist() {
    String encodedToken = encodeToken("token");

    assertThatThrownBy(() -> quarantinedComponentService.getQuarantinedComponentOtherVersions(encodedToken, 1, 5,
        true /* asc */)).isInstanceOf(NotFoundException.class)
        .hasMessage(
            "The quarantined component view for the blocked component you are trying to view could not be found.");
  }

  @Test
  public void testGetQuarantinedComponentOtherVersions_InvalidToken() {
    // The token is not base64 encoded
    assertThatThrownBy(() -> quarantinedComponentService.getQuarantinedComponentOtherVersions("token", 1, 5,
        true /* asc */)).isInstanceOf(BadRequestException.class)
        .hasMessage("The quarantined component view cannot be retrieved because the URL contains invalid characters.");
  }

  @Test
  public void testGetPathnamePrefix() {
    assertThat(quarantinedComponentService.getPathnamePrefix("a")).isEqualTo("a");
    assertThat(quarantinedComponentService.getPathnamePrefix("a/b")).isEqualTo("a/b");
    assertThat(quarantinedComponentService.getPathnamePrefix("a/b/c/d/version/file")).isEqualTo("a/b/c/d/");
    assertThat(quarantinedComponentService.getPathnamePrefix("a/b/c/-/file")).isEqualTo("a/b/c/");
  }

  @Test
  public void testGetQuarantinedComponentVersionDetails() throws Exception {
    RepositoryComponent repositoryComponent =
        tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "testPathname", "testHash",
            ComponentIdentifier.createMavenCoordinates("g", "a", "v", "", "jar"), true /* quarantined */);
    QuarantinedComponentAccess quarantinedComponentAccess =
        tempEntity.newQuarantinedComponentAccess(repository.getId(), repositoryComponent.getId());
    String encodedToken = encodeToken(quarantinedComponentAccess);

    NamedComponentDetails namedComponentDetails = new NamedComponentDetails();
    namedComponentDetails.setHash("testHash");
    namedComponentDetails.setComponentIdentifier(repositoryComponent.getComponentIdentifier());

    when(hdsClientMock.relay(httpRequestMock, NamedComponentDetails.class, "rest/ci/componentDetails",
        newCoordinatesQueryParam(namedComponentDetails))).thenReturn(new RelayResponse<>(namedComponentDetails));

    namedComponentDetails =
        quarantinedComponentService.getQuarantinedComponentVersionDetails(encodedToken, httpRequestMock, "v");

    assertThat(namedComponentDetails.getDisplayName().toString()).isEqualTo("g : a : v");
    assertThat(namedComponentDetails.getHash()).isEqualTo(repositoryComponent.getHash());
    assertThat(namedComponentDetails.getMatchState()).isEqualTo(repositoryComponent.getMatchStateId());
    assertThat(namedComponentDetails.getDeclaredLicenseIds()).containsExactly(License.UNSPECIFIED_ID);
    assertThat(namedComponentDetails.getObservedLicenseIds()).containsExactly(License.UNSPECIFIED_ID);
    assertThat(namedComponentDetails.getEffectiveLicenses())
        .extracting(com.sonatype.clm.dto.model.License::getLicenseId)
        .containsExactly(License.UNSPECIFIED_ID);
    assertThat(namedComponentDetails.getOverriddenLicenses()).isEmpty();
    assertThat(namedComponentDetails.getPolicyMaxThreatLevelsByCategory()).isEmpty();
    assertThat(namedComponentDetails.getEffectiveLicenseStatus()).isNull();
    assertThat(namedComponentDetails.getCatalogDate()).isNull();
    assertThat(namedComponentDetails.getRelativePopularity()).isNull();
    assertThat(namedComponentDetails.getSecurityVulnerabilities()).isEmpty();
    assertThat(namedComponentDetails.getWebsite()).isNull();
    assertThat(namedComponentDetails.getPolicyAlerts()).isEmpty();
    assertThat(namedComponentDetails.getLicenseThreatLevel()).isEqualTo(5);
    assertThat(namedComponentDetails.getLicenseThreatGroupNames()).containsExactly("Sonatype Special Licenses");
    assertThat(namedComponentDetails.getIdentificationSource()).isEqualTo(IdentificationSource.SONATYPE.getId());
    assertThat(namedComponentDetails.getIdentificationSourceComment()).isNull();
    assertThat(namedComponentDetails.getComponentIdentifier()).isEqualTo(repositoryComponent.getComponentIdentifier());
    assertThat(namedComponentDetails.getComponentCategories()).extracting(ComponentCategory::getPath)
        .containsExactly("Other");
    assertThat(namedComponentDetails.getHygieneRating()).isNull();
    assertThat(namedComponentDetails.getIntegrityRating()).isNull();
    assertThat(namedComponentDetails.getBreakingChangesCount()).isNull();
    assertThat(namedComponentDetails.getAnalyzerFeatures()).isNull();
  }

  @Test
  public void testGetQuarantinedComponentVersionDetails_DifferentVersion() throws Exception {
    RepositoryComponent repositoryComponent =
        tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "testPathname", "testHash",
            ComponentIdentifier.createMavenCoordinates("g", "a", "v", "", "jar"), true /* quarantined */);
    QuarantinedComponentAccess quarantinedComponentAccess =
        tempEntity.newQuarantinedComponentAccess(repository.getId(), repositoryComponent.getId());
    String encodedToken = encodeToken(quarantinedComponentAccess);

    String otherVersion = "otherVersion";
    ComponentIdentifier otherVersionComponentIdentifier =
        repositoryComponent.getComponentIdentifier().createAlternativeVersion(otherVersion);

    NamedComponentDetails namedComponentDetails = new NamedComponentDetails();
    namedComponentDetails.setComponentIdentifier(otherVersionComponentIdentifier);
    when(hdsClientMock.relay(httpRequestMock, NamedComponentDetails.class, "rest/ci/componentDetails",
        newCoordinatesQueryParam(namedComponentDetails))).thenReturn(new RelayResponse<>(namedComponentDetails));

    namedComponentDetails =
        quarantinedComponentService.getQuarantinedComponentVersionDetails(encodedToken, httpRequestMock, otherVersion);

    assertThat(namedComponentDetails.getDisplayName().toString()).isEqualTo("g : a : " + otherVersion);
    assertThat(namedComponentDetails.getHash()).isNull();
    assertThat(namedComponentDetails.getMatchState()).isEqualTo(repositoryComponent.getMatchStateId());
    assertThat(namedComponentDetails.getDeclaredLicenseIds()).containsExactly(License.UNSPECIFIED_ID);
    assertThat(namedComponentDetails.getObservedLicenseIds()).containsExactly(License.UNSPECIFIED_ID);
    assertThat(namedComponentDetails.getEffectiveLicenses())
        .extracting(com.sonatype.clm.dto.model.License::getLicenseId)
        .containsExactly(License.UNSPECIFIED_ID);
    assertThat(namedComponentDetails.getOverriddenLicenses()).isEmpty();
    assertThat(namedComponentDetails.getPolicyMaxThreatLevelsByCategory()).isEmpty();
    assertThat(namedComponentDetails.getEffectiveLicenseStatus()).isNull();
    assertThat(namedComponentDetails.getCatalogDate()).isNull();
    assertThat(namedComponentDetails.getRelativePopularity()).isNull();
    assertThat(namedComponentDetails.getSecurityVulnerabilities()).isEmpty();
    assertThat(namedComponentDetails.getWebsite()).isNull();
    assertThat(namedComponentDetails.getPolicyAlerts()).isEmpty();
    assertThat(namedComponentDetails.getLicenseThreatLevel()).isEqualTo(5);
    assertThat(namedComponentDetails.getLicenseThreatGroupNames()).containsExactly("Sonatype Special Licenses");
    assertThat(namedComponentDetails.getIdentificationSource()).isEqualTo(IdentificationSource.SONATYPE.getId());
    assertThat(namedComponentDetails.getIdentificationSourceComment()).isNull();
    assertThat(namedComponentDetails.getComponentIdentifier()).isEqualTo(otherVersionComponentIdentifier);
    assertThat(namedComponentDetails.getComponentCategories()).extracting(ComponentCategory::getPath)
        .containsExactly("Other");
    assertThat(namedComponentDetails.getHygieneRating()).isNull();
    assertThat(namedComponentDetails.getIntegrityRating()).isNull();
    assertThat(namedComponentDetails.getBreakingChangesCount()).isNull();
    assertThat(namedComponentDetails.getAnalyzerFeatures()).isNull();
  }

  @Test
  public void testGetQuarantinedComponentVersionDetails_ExpiredToken() {
    RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repository.getId());
    QuarantinedComponentAccess expiredQuarantinedComponentAccess = tempEntity.newQuarantinedComponentAccess(
        repository.getId(), repositoryComponent.getId(), DateUtils.addDays(new Date(), -3));
    String encodedToken = encodeToken(expiredQuarantinedComponentAccess);

    assertThatThrownBy(
        () -> quarantinedComponentService.getQuarantinedComponentVersionDetails(encodedToken, httpRequestMock, "1.0.0"))
        .isInstanceOf(NotFoundException.class).hasMessageStartingWith("This report expired on ")
        .hasMessageEndingWith("You may generate a new report by requesting the blocked component again.");
  }

  @Test
  public void testGetQuarantinedComponentVersionDetails_TokenDoesNotExist() {
    String encodedToken = encodeToken("token");

    assertThatThrownBy(
        () -> quarantinedComponentService.getQuarantinedComponentVersionDetails(encodedToken, httpRequestMock, "1.0.0"))
        .isInstanceOf(NotFoundException.class)
        .hasMessage(
            "The quarantined component view for the blocked component you are trying to view could not be found.");
  }

  @Test
  public void testGetQuarantinedComponentVersionDetails_InvalidToken() {
    // The token is not base64 encoded
    assertThatThrownBy(
        () -> quarantinedComponentService.getQuarantinedComponentVersionDetails("token", httpRequestMock, "1.0.0"))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("The quarantined component view cannot be retrieved because the URL contains invalid characters.");
  }

  private Map<String, String> newCoordinatesQueryParam(NamedComponentDetails componentDetails) {
    Map<String, String> queryParams = new HashMap<>();
    if (componentDetails.getHash() != null) {
      queryParams.put("hash", componentDetails.getHash());
    }
    queryParams.put("componentIdentifier",
        ComponentIdentifierAdapter.toJson(componentDetails.getComponentIdentifier()));
    return queryParams;
  }

  private void assertTelemetry(final String token, final Date tokenGenerateTime, final String componentHash) {
    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySenderMock, times(1)).send(telemetryDataArgumentCaptor.capture());
    final TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();

    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.QUARANTINED_COMPONENT_REPORT_USAGE);

    assertThat(telemetryData.getAttributes()).hasSize(5);
    assertThat(telemetryData.getAttributes()).contains(
        entry(QUARANTINED_COMPONENT_REPORT_OBFUSCATED_COMPONENT_HASH, HdsClientAnalytics.obfuscate(componentHash)),
        entry(QUARANTINED_COMPONENT_REPORT_OBFUSCATED_TOKEN, HdsClientAnalytics.obfuscate(token)),
        entry(QUARANTINED_COMPONENT_REPORT_GENERATE_TIME, tokenGenerateTime.getTime()),
        entry(QUARANTINED_COMPONENT_REPORT_ANONYMOUS_ACCESS_ENABLED, true));
  }

  private static String encodeToken(QuarantinedComponentAccess quarantinedComponentAccess) {
    return encodeToken(quarantinedComponentAccess.getId());
  }

  private static String encodeToken(String token) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(token.getBytes(StandardCharsets.UTF_8));
  }
}
