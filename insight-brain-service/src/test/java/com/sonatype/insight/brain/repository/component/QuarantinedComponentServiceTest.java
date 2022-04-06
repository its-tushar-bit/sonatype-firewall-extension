/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.api.v2.dto.ApiPageResult;
import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupConditionType;
import com.sonatype.insight.brain.model.repository.QuarantinedComponentAccess;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.repository.RepositoryPolicyThreatDTO;
import com.sonatype.insight.brain.repository.RepositoryPolicyViolationDTO;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.google.inject.Binder;
import org.joda.time.DateTime;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static com.sonatype.insight.brain.repository.component.QuarantinedComponentService.QUARANTINED_COMPONENT_REPORT_ANONYMOUS_ACCESS_ENABLED;
import static com.sonatype.insight.brain.repository.component.QuarantinedComponentService.QUARANTINED_COMPONENT_REPORT_COMPONENT_HASH;
import static com.sonatype.insight.brain.repository.component.QuarantinedComponentService.QUARANTINED_COMPONENT_REPORT_GENERATE_TIME;
import static com.sonatype.insight.brain.repository.component.QuarantinedComponentService.QUARANTINED_COMPONENT_REPORT_TOKEN;
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
  @Mock
  private DbQuarantinedComponentAccessManager quarantinedComponentAccessManager;

  @Inject
  private QuarantinedComponentService quarantinedComponentService;

  @Mock
  private TelemetrySender telemetrySenderMock;

  @Override
  public void configure(Binder binder) {
    binder.bind(DbQuarantinedComponentAccessManager.class).toInstance(quarantinedComponentAccessManager);
    binder.bind(TelemetrySender.class).toInstance(telemetrySenderMock);
    super.configure(binder);
  }

  @Test
  public void testGetQuarantinedComponent() {
    QuarantinedComponentAccess quarantinedComponentAccess =
        new QuarantinedComponentAccess("repositoryId", "repositoryComponentId", new Date());
    when(quarantinedComponentAccessManager.getQuarantinedComponentAccessFromToken("token")).thenReturn(
        quarantinedComponentAccess);

    QuarantinedComponentDto quarantinedComponentDto = quarantinedComponentService.getQuarantinedComponent("token");

    assertThat(quarantinedComponentDto).isNotNull();
    assertThat(quarantinedComponentDto.success).isTrue();
    assertThat(quarantinedComponentDto.repositoryComponentId).isEqualTo("repositoryComponentId");
    verify(telemetrySenderMock, never()).send(any(TelemetryData.class));
  }

  @Test
  public void testGetQuarantinedComponentOverview() {
    //setup
    Date date = new Date();
    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("com.lingocoder", "abi.cli", "0.5.2");
    final String token = "token";
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, "repositoryPublicId");
    RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
        "com/lingocoder/abi.cli/0.5.2/abi.cli-0.5.2.jar",
        "hash", componentIdentifier, date, date, null);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 6,
        repositoryComponent.getPathname(), false, "fail", "policyId", "policyName",
        repositoryComponent.getComponentIdentifier(), date);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 6,
        repositoryComponent.getPathname(), false, "fail", "policyId", "policyName",
        repositoryComponent.getComponentIdentifier(), date);
    QuarantinedComponentAccess quarantinedComponentAccess =
        tempEntity.newQuarantinedComponentAccess(repository.getId(), repositoryComponent.getId(), date);

    when(quarantinedComponentAccessManager.getQuarantinedComponentAccessFromToken(token)).thenReturn(
        quarantinedComponentAccess);
    when(quarantinedComponentAccessManager.getTokenExpiryTime(date)).thenReturn(date);

    //when
    QuarantinedComponentOverviewDto quarantinedComponentOverviewDto =
        quarantinedComponentService.getQuarantinedComponentOverview("token");

    //then
    assertThat(quarantinedComponentOverviewDto).isNotNull();
    assertThat(quarantinedComponentOverviewDto.componentDisplayName).isEqualTo("com.lingocoder : abi.cli : 0.5.2");
    assertThat(quarantinedComponentOverviewDto.isQuarantined).isEqualTo(true);
    assertThat(quarantinedComponentOverviewDto.quarantinedPolicyViolationsCount).isEqualTo(2);
    assertThat(quarantinedComponentOverviewDto.repositoryName).isEqualTo("repositoryPublicId");
    assertThat(quarantinedComponentOverviewDto.quarantinedDate).isEqualTo(date);
    assertThat(quarantinedComponentOverviewDto.cataloguedDate).isEqualTo(date);
    assertThat(quarantinedComponentOverviewDto.componentVersion).isEqualTo("0.5.2");
    assertThat(quarantinedComponentOverviewDto.tokenExpiryTime).isEqualTo(date);
    assertTelemetry(token, quarantinedComponentAccess.getGenerateTime(), repositoryComponent.getHash());
  }

  @Test
  public void testGetQuarantinedComponentOverview_quarantinedTimeNull() {
    //setup
    Date date = new Date();
    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("com.lingocoder", "abi.cli", "0.5.2");
    when(quarantinedComponentAccessManager.getQuarantinedComponentAccessFromToken("token")).thenReturn(
        setupTestData(componentIdentifier, date, null, null));

    //when
    QuarantinedComponentOverviewDto quarantinedComponentOverviewDto =
        quarantinedComponentService.getQuarantinedComponentOverview("token");

    //then
    assertThat(quarantinedComponentOverviewDto).isNotNull();
    assertThat(quarantinedComponentOverviewDto.isQuarantined).isEqualTo(false);
  }

  @Test
  public void testGetQuarantinedComponentOverview_unquarantinedTimeNotNull() {
    //setup
    Date date = new Date();
    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("com.lingocoder", "abi.cli", "0.5.2");
    when(quarantinedComponentAccessManager.getQuarantinedComponentAccessFromToken("token")).thenReturn(
        setupTestData(componentIdentifier, date, date, date));

    //when
    QuarantinedComponentOverviewDto quarantinedComponentOverviewDto =
        quarantinedComponentService.getQuarantinedComponentOverview("token");

    //then
    assertThat(quarantinedComponentOverviewDto).isNotNull();
    assertThat(quarantinedComponentOverviewDto.isQuarantined).isEqualTo(false);
  }

  @Test
  public void testGetQuarantinedComponentOverview_componentIdentifierNull() {
    //setup
    Date date = new Date();
    when(quarantinedComponentAccessManager.getQuarantinedComponentAccessFromToken("token")).thenReturn(
        setupTestData(null, date, date, null));

    assertThatThrownBy(() -> {
      quarantinedComponentService.getQuarantinedComponentOverview("token");
    }).isInstanceOf(BadRequestException.class)
        .hasMessage("The component identifier for the requested component does not exist.");
  }

  private QuarantinedComponentAccess setupTestData(
      ComponentIdentifier componentIdentifier,
      Date time,
      Date quarantinedTime,
      Date unquarantinedTime)
  {
    final RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    final Repository repository = tempEntity.newRepository(repositoryManager, "repositoryPublicId");
    final RepositoryComponent repositoryComponent =
        tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
            "com/lingocoder/abi.cli/0.5.2/abi.cli-0.5.2.jar",
            "hash", componentIdentifier, time, quarantinedTime, unquarantinedTime);
    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
        "com/lingocoder/abi.cli/0.5.3/abi.cli-0.5.3.jar",
        "hash", ComponentIdentifier.createMavenCoordinates("com.lingocoder", "abi.cli", "0.5.3"), time, null, null);

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
  public void testGetQuarantinedComponentPolicyViolations() throws Exception {
    // setup
    Date date = new Date();
    final RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    final Repository repository = tempEntity.newRepository(repositoryManager, "repositoryPublicId");
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
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 5, repositoryComponent.getPathname(),
        "hash", constraintFacts, false, "fail", "policyid_1",
        "policyname_1", repositoryComponent.getComponentIdentifier(), date, null,
        null, null);
    RepositoryPolicyViolation violation2 =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), 10, repositoryComponent.getPathname(),
            "hash", constraintFacts, false, "fail", "policyid_2",
            "policyname_2", repositoryComponent.getComponentIdentifier(), date, null,
            null, null);
    QuarantinedComponentAccess quarantinedComponentAccess =
        tempEntity
            .newQuarantinedComponentAccess(repository.getId(), repositoryComponent.getId(), date);
    when(quarantinedComponentAccessManager.getQuarantinedComponentAccessFromToken("token"))
        .thenReturn(quarantinedComponentAccess);

    // when
    RepositoryPolicyThreatDTO dto =
        quarantinedComponentService.getQuarantinedComponentPolicyViolations("token");

    // then
    assertThat(dto).isNotNull();
    assertThat(dto.activePolicyViolations).hasSize(2);

    RepositoryPolicyViolationDTO policyViolationDTO = dto.activePolicyViolations.get(0);
    assertThat(policyViolationDTO.policyId).isEqualTo(violation2.getPolicyId());
    assertThat(policyViolationDTO.policyThreatLevel).isEqualTo(violation2.getThreatLevel());
    assertThat(policyViolationDTO.policyName).isEqualTo(violation2.getPolicyName());
    assertThat(policyViolationDTO.blocksUnquarantine).isEqualTo(true);
    assertThat(policyViolationDTO.constraints.get(0).constraintId).isEqualTo(constraintFact.getConstraintId());
    assertThat(policyViolationDTO.constraints.get(0).constraintName).isEqualTo(constraintFact.getConstraintName());
    assertThat(policyViolationDTO.constraints.get(0).conditions.get(0).conditionReason).isEqualTo(
        conditionFact.getReason());
    assertThat(policyViolationDTO.constraints.get(0).conditions.get(0).conditionSummary).isEqualTo(
        conditionFact.getSummary());
    verify(telemetrySenderMock, never()).send(any(TelemetryData.class));
  }

  @Test
  public void testGetQuarantinedComponentOtherVersions() throws Exception {
    // setup
    Date date = new Date();
    final RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    final Repository repository = tempEntity.newRepository(repositoryManager, "repositoryPublicId");
    final RepositoryComponent repositoryComponent =
        tempEntity.newRepositoryComponent(repository.getId(), "com/lingocoder/abi.cli/0.5.1/abi.cli-0.5.1.jar",
            new DateTime(date).minusDays(1).toDate(), date);
    tempEntity.newRepositoryComponent(repository.getId(), "com/lingocoder/abi.cli/0.5.2/abi.cli-0.5.2.jar", null, null);
    tempEntity.newRepositoryComponent(repository.getId(), "com/lingocoder/abi.cli/0.5.3/abi.cli-0.5.3.jar", date, null);
    tempEntity.newRepositoryComponent(repository.getId(), "com/lingocoder/abi.cli/0.5.4/abi.cli-0.5.4.jar",
        new DateTime(date).minusDays(1).toDate(), date);
    QuarantinedComponentAccess quarantinedComponentAccess =
        tempEntity
            .newQuarantinedComponentAccess(repository.getId(), repositoryComponent.getId(), date);

    when(quarantinedComponentAccessManager.getQuarantinedComponentAccessFromToken("token"))
        .thenReturn(quarantinedComponentAccess);

    // when
    ApiPageResult<String> result =
        quarantinedComponentService.getQuarantinedComponentOtherVersions("token", 1, 5, false);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getTotal()).isEqualTo(2);
    assertThat(result.getPage()).isEqualTo(1);
    assertThat(result.getPageSize()).isEqualTo(5);
    assertThat(result.getPageCount()).isEqualTo(1);
    assertThat(result.getResults()).hasSize(2);
    verify(telemetrySenderMock, never()).send(any(TelemetryData.class));
  }

  @Test
  public void testGetPathnamePrefix() throws Exception {
    assertThat(quarantinedComponentService.getPathnamePrefix("a")).isEqualTo("a");
    assertThat(quarantinedComponentService.getPathnamePrefix("a/b")).isEqualTo("a/b");
    assertThat(quarantinedComponentService.getPathnamePrefix("a/b/c/d/version/file")).isEqualTo("a/b/c/d/");
    assertThat(quarantinedComponentService.getPathnamePrefix("a/b/c/-/file")).isEqualTo("a/b/c/");
  }

  private void assertTelemetry(final String token, final Date tokenGenerateTime, final String componentHash) {
    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySenderMock, times(1)).send(telemetryDataArgumentCaptor.capture());
    final TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();

    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.QUARANTINED_COMPONENT_REPORT_USAGE);

    assertThat(telemetryData.getAttributes()).hasSize(5);
    assertThat(telemetryData.getAttributes()).contains(
        entry(QUARANTINED_COMPONENT_REPORT_COMPONENT_HASH, HdsClientAnalytics.obfuscate(componentHash)),
        entry(QUARANTINED_COMPONENT_REPORT_TOKEN, HdsClientAnalytics.obfuscate(token)),
        entry(QUARANTINED_COMPONENT_REPORT_GENERATE_TIME, tokenGenerateTime.getTime()),
        entry(QUARANTINED_COMPONENT_REPORT_ANONYMOUS_ACCESS_ENABLED, true));
  }
}
