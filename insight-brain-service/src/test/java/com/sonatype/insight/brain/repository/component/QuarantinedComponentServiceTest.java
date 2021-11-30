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
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupConditionType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.repository.RepositoryPolicyThreatDTO;
import com.sonatype.insight.brain.repository.RepositoryPolicyViolationDTO;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

public class QuarantinedComponentServiceTest
    extends AbstractComponentTest
{
  @Mock
  private DbQuarantinedComponentAccessManager quarantinedComponentAccessManager;

  @Inject
  private QuarantinedComponentService quarantinedComponentService;

  @Override
  public void configure(Binder binder) {
    binder.bind(DbQuarantinedComponentAccessManager.class).toInstance(quarantinedComponentAccessManager);
    super.configure(binder);
  }

  @Test
  public void testGetQuarantinedComponent() {
    when(quarantinedComponentAccessManager.getRepositoryComponentIdFromToken("token")).thenReturn("id");

    QuarantinedComponentDto quarantinedComponentDto = quarantinedComponentService.getQuarantinedComponent("token");

    assertThat(quarantinedComponentDto).isNotNull();
    assertThat(quarantinedComponentDto.success).isTrue();
    assertThat(quarantinedComponentDto.repositoryComponentId).isEqualTo("id");
  }

  @Test
  public void testGetQuarantinedComponentOverview() {
    //setup
    Date date = new Date();
    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    when(quarantinedComponentAccessManager.getRepositoryComponentIdFromToken("token")).thenReturn(
        setupTestData(componentIdentifier, date, date, null));

    //when
    QuarantinedComponentOverviewDto quarantinedComponentOverviewDto =
        quarantinedComponentService.getQuarantinedComponentOverview("token");

    //then
    assertThat(quarantinedComponentOverviewDto).isNotNull();
    assertThat(quarantinedComponentOverviewDto.componentDisplayName).isEqualTo("g : a : v");
    assertThat(quarantinedComponentOverviewDto.isQuarantined).isEqualTo(true);
    assertThat(quarantinedComponentOverviewDto.quarantinedPolicyViolationsCount).isEqualTo(2);
    assertThat(quarantinedComponentOverviewDto.repositoryName).isEqualTo("repositoryPublicId");
    assertThat(quarantinedComponentOverviewDto.quarantinedDate).isEqualTo(date);
    assertThat(quarantinedComponentOverviewDto.cataloguedDate).isEqualTo(date);
  }

  @Test
  public void testGetQuarantinedComponentOverview_quarantinedTimeNull() {
    //setup
    Date date = new Date();
    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    when(quarantinedComponentAccessManager.getRepositoryComponentIdFromToken("token")).thenReturn(
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
        ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    when(quarantinedComponentAccessManager.getRepositoryComponentIdFromToken("token")).thenReturn(
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
    when(quarantinedComponentAccessManager.getRepositoryComponentIdFromToken("token")).thenReturn(
        setupTestData(null, date, date, null));

    assertThatThrownBy(() -> {
      quarantinedComponentService.getQuarantinedComponentOverview("token");
    }).isInstanceOf(BadRequestException.class)
        .hasMessage("The component identifier for the requested component does not exist.");
  }

  private String setupTestData(
      ComponentIdentifier componentIdentifier,
      Date time,
      Date quarantinedTime,
      Date unquarantinedTime)
  {
    final RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    final Repository repository = tempEntity.newRepository(repositoryManager, "repositoryPublicId");
    final RepositoryComponent repositoryComponent =
        tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "path",
            "hash", componentIdentifier, time, quarantinedTime, unquarantinedTime);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 6,
        repositoryComponent.getPathname(), false, "fail", "policyId", "policyName",
        repositoryComponent.getComponentIdentifier(), time);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 6,
        repositoryComponent.getPathname(), false, "fail", "policyId", "policyName",
        repositoryComponent.getComponentIdentifier(), time);
    return repositoryComponent.getId();
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
    when(quarantinedComponentAccessManager.getRepositoryComponentIdFromToken("token"))
        .thenReturn(repositoryComponent.getId());

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
  }

  @Test
  public void testGetQuarantinedComponentPolicyViolations_noPolicyViolations() {
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
        "hash", constraintFacts, true, "fail", "policyid", "policyname",
        repositoryComponent.getComponentIdentifier(), date, null, null, null);
    when(quarantinedComponentAccessManager.getRepositoryComponentIdFromToken("token"))
        .thenReturn(repositoryComponent.getId());

    assertThatThrownBy(() -> {
      quarantinedComponentService.getQuarantinedComponentPolicyViolations("token");
    }).isInstanceOf(NotFoundException.class)
        .hasMessage("No policy violations causing quarantine exist for the requested component.");
  }
}
