/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentsInQuarantineDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiConstraintViolationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyViolationDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryComponentDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryComponentPolicyViolationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryComponentsInQuarantineDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryDTO;
import com.sonatype.insight.brain.model.component.IdentificationSource;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiComponentsInQuarantineReportingServiceTest
    extends AbstractComponentTest
{
  private static final String WITH_POLICY_VIOLATION_CAUSING_QUARANTINE = "a";

  private static final String WITH_POLICY_VIOLATION_CAUSING_QUARANTINE_AND_OTHER_THAT_IS_NOT = "b";

  private static final String WITH_2_POLICY_VIOLATIONS_CAUSING_QUARANTINE_AND_OTHER_2_THAT_ARE_NOT = "c";

  private static final String WITH_WAIVED_POLICY_VIOLATION_THAT_WAS_CAUSING_QUARANTINE = "d";

  @Inject
  private ApiComponentsInQuarantineReportingService service;

  private Repository repo1;

  private Repository repo2;

  private Policy policy1;

  private Policy policy2;

  @Before
  public void setup() {
    repo1 = tempEntity.newRepository("rm1", "r1", "maven2");
    repo2 = tempEntity.newRepository("rm2", "r2", "maven3");
    tempEntity.newRepository("rm3", "r3", "maven4");

    Condition condition = new Condition("RelativePopularity", "<=", "10");

    Constraint constraint = new Constraint("c1", "constraint1", LogicalOperator.OR);
    constraint.addCondition(condition);
    policy1 = tempEntity.newPolicy("policy1", constraint);

    constraint = new Constraint("c2", "constraint2", LogicalOperator.AND);
    constraint.addCondition(condition);
    policy2 = tempEntity.newPolicy("policy2", constraint);
  }

  @Test
  public void testGetComponentsInQuarantine() {
    Map<String, ComponentForAssertions> componentsInQuarantineForAssertions1 =
        createRepositoryComponentsForRepository(repo1, policy1);
    Map<String, ComponentForAssertions> componentsInQuarantineForAssertions2 =
        createRepositoryComponentsForRepository(repo2, policy2);

    ApiComponentsInQuarantineDTO componentsInQuarantineDTO = service.getComponentsInQuarantine();
    assertThat(componentsInQuarantineDTO.componentsInQuarantine).hasSize(2);

    // sorted in order to facilitate assertions
    componentsInQuarantineDTO.componentsInQuarantine.sort(Comparator.comparing(rcq -> rcq.repository.publicId));

    assertRepository(componentsInQuarantineDTO.componentsInQuarantine.get(0), repo1, policy1,
        componentsInQuarantineForAssertions1);
    assertRepository(componentsInQuarantineDTO.componentsInQuarantine.get(1), repo2, policy2,
        componentsInQuarantineForAssertions2);
  }

  private Map<String, ComponentForAssertions> createRepositoryComponentsForRepository(Repository repo, Policy policy) {
    createComponentsNeverInQuarantineBefore(repo, policy);
    Map<String, ComponentForAssertions> componentsInQuarantineForAssertion = createComponentsInQuarantine(repo, policy);
    createComponentsReleasedFromQuarantine(repo, policy);
    return componentsInQuarantineForAssertion;
  }

  private void createComponentsNeverInQuarantineBefore(Repository repo, Policy policy) {
    RepositoryComponent repositoryComponent = createRepositoryComponentNeverInQuarantine(repo, 0);
    tempEntity.newRepositoryComponent(repositoryComponent);

    // with a policy violation
    repositoryComponent = createRepositoryComponentNeverInQuarantine(repo, 1);
    tempEntity.newRepositoryComponent(repositoryComponent);
    RepositoryPolicyViolation repositoryPolicyViolation = createRepositoryPolicyViolationNotCausingQuarantine(
        repositoryComponent, policy);
    tempEntity.newRepositoryPolicyViolation(repositoryPolicyViolation);
  }

  private Map<String, ComponentForAssertions> createComponentsInQuarantine(Repository repo, Policy policy) {
    Map<String, ComponentForAssertions> componentsInQuarantineForAssertions = new HashMap<>();

    // with a policy violation causing quarantine
    ComponentForAssertions componentForAssertions = new ComponentForAssertions();
    RepositoryComponent repositoryComponent = createRepositoryComponentInQuarantine(repo, 2);
    tempEntity.newRepositoryComponent(repositoryComponent);
    componentForAssertions.setComponent(repositoryComponent);
    RepositoryPolicyViolation repositoryPolicyViolation = createRepositoryPolicyViolationCausingQuarantine(
        repositoryComponent, policy);
    tempEntity.newRepositoryPolicyViolation(repositoryPolicyViolation);
    componentForAssertions.addPolicyViolation(repositoryPolicyViolation);
    componentsInQuarantineForAssertions.put(WITH_POLICY_VIOLATION_CAUSING_QUARANTINE, componentForAssertions);

    // with a policy violation causing quarantine and other that isn't
    componentForAssertions = new ComponentForAssertions();
    repositoryComponent = createRepositoryComponentInQuarantine(repo, 3);
    tempEntity.newRepositoryComponent(repositoryComponent);
    componentForAssertions.setComponent(repositoryComponent);
    repositoryPolicyViolation = createRepositoryPolicyViolationCausingQuarantine(repositoryComponent, policy);
    tempEntity.newRepositoryPolicyViolation(repositoryPolicyViolation);
    componentForAssertions.addPolicyViolation(repositoryPolicyViolation);
    repositoryPolicyViolation = createRepositoryPolicyViolationNotCausingQuarantine(repositoryComponent, policy);
    tempEntity.newRepositoryPolicyViolation(repositoryPolicyViolation);
    componentsInQuarantineForAssertions.put(WITH_POLICY_VIOLATION_CAUSING_QUARANTINE_AND_OTHER_THAT_IS_NOT,
        componentForAssertions);

    // same as before but double
    componentForAssertions = new ComponentForAssertions();
    repositoryComponent = createRepositoryComponentInQuarantine(repo, 4);
    tempEntity.newRepositoryComponent(repositoryComponent);
    componentForAssertions.setComponent(repositoryComponent);
    repositoryPolicyViolation = createRepositoryPolicyViolationCausingQuarantine(repositoryComponent, policy);
    tempEntity.newRepositoryPolicyViolation(repositoryPolicyViolation);
    componentForAssertions.addPolicyViolation(repositoryPolicyViolation);
    repositoryPolicyViolation = createRepositoryPolicyViolationNotCausingQuarantine(repositoryComponent, policy);
    tempEntity.newRepositoryPolicyViolation(repositoryPolicyViolation);
    repositoryPolicyViolation = createRepositoryPolicyViolationCausingQuarantine(repositoryComponent, policy);
    tempEntity.newRepositoryPolicyViolation(repositoryPolicyViolation);
    componentForAssertions.addPolicyViolation(repositoryPolicyViolation);
    repositoryPolicyViolation = createRepositoryPolicyViolationNotCausingQuarantine(repositoryComponent, policy);
    tempEntity.newRepositoryPolicyViolation(repositoryPolicyViolation);
    componentsInQuarantineForAssertions.put(WITH_2_POLICY_VIOLATIONS_CAUSING_QUARANTINE_AND_OTHER_2_THAT_ARE_NOT,
        componentForAssertions);

    // with a waived policy violation that was causing quarantine
    componentForAssertions = new ComponentForAssertions();
    repositoryComponent = createRepositoryComponentInQuarantine(repo, 5);
    tempEntity.newRepositoryComponent(repositoryComponent);
    componentForAssertions.setComponent(repositoryComponent);
    repositoryPolicyViolation =
        createWaivedRepositoryPolicyViolationThatWasCausingQuarantine(repositoryComponent, policy, "waiver1");
    tempEntity.newRepositoryPolicyViolation(repositoryPolicyViolation);
    componentsInQuarantineForAssertions.put(WITH_WAIVED_POLICY_VIOLATION_THAT_WAS_CAUSING_QUARANTINE,
        componentForAssertions);

    return componentsInQuarantineForAssertions;
  }

  private void createComponentsReleasedFromQuarantine(Repository repo, Policy policy) {
    // with a waived policy violation that was causing quarantine
    RepositoryComponent repositoryComponent = createRepositoryComponentReleasedFromQuarantine(repo, 6);
    tempEntity.newRepositoryComponent(repositoryComponent);
    RepositoryPolicyViolation repositoryPolicyViolation =
        createWaivedRepositoryPolicyViolationThatWasCausingQuarantine(repositoryComponent, policy, "waiver2");
    tempEntity.newRepositoryPolicyViolation(repositoryPolicyViolation);

    // with a waived policy violation that was causing quarantine and other that wasn't
    repositoryComponent = createRepositoryComponentReleasedFromQuarantine(repo, 7);
    tempEntity.newRepositoryComponent(repositoryComponent);
    repositoryPolicyViolation =
        createWaivedRepositoryPolicyViolationThatWasCausingQuarantine(repositoryComponent, policy, "waiver3");
    tempEntity.newRepositoryPolicyViolation(repositoryPolicyViolation);
    repositoryPolicyViolation = createRepositoryPolicyViolationNotCausingQuarantine(repositoryComponent, policy);
    tempEntity.newRepositoryPolicyViolation(repositoryPolicyViolation);
  }

  private RepositoryComponent createRepositoryComponentNeverInQuarantine(Repository repo, int baseId) {
    String repoIdAndBaseId = String.format("-%s-%02d", repo.getPublicId(), baseId);
    RepositoryComponent repositoryComponent = new RepositoryComponent();
    repositoryComponent.setRepositoryId(repo.getId());
    repositoryComponent.setPathname("pathname" + repoIdAndBaseId);
    repositoryComponent.setTime(new Date());
    repositoryComponent.setHash("hash" + repoIdAndBaseId);
    repositoryComponent.setComponentIdentifier(
        ComponentIdentifier.createMavenCoordinates("g", "a" + repoIdAndBaseId, "v"));
    repositoryComponent.setMatchStateId(MatchState.EXACT.getId());
    repositoryComponent.setIdentificationSourceId(IdentificationSource.SONATYPE.getId());
    repositoryComponent.setLastEvaluationTime(new Date());
    return repositoryComponent;
  }

  private RepositoryComponent createRepositoryComponentInQuarantine(Repository repo, int baseId) {
    RepositoryComponent repositoryComponent = createRepositoryComponentNeverInQuarantine(repo, baseId);
    repositoryComponent.setQuarantineTime(new Date());
    return repositoryComponent;
  }

  private RepositoryComponent createRepositoryComponentReleasedFromQuarantine(Repository repo, int baseId) {
    RepositoryComponent repositoryComponent = createRepositoryComponentInQuarantine(repo, baseId);
    repositoryComponent.setUnquarantineTime(new Date(repositoryComponent.getQuarantineTime().getTime() + 1_000));
    return repositoryComponent;
  }

  private RepositoryPolicyViolation createRepositoryPolicyViolationNotCausingQuarantine(
      RepositoryComponent repositoryComponent, Policy policy)
  {
    RepositoryPolicyViolation repositoryPolicyViolation;
    repositoryPolicyViolation = new RepositoryPolicyViolation();
    repositoryPolicyViolation.setRepositoryId(repositoryComponent.getRepositoryId());
    repositoryPolicyViolation.setPathname(repositoryComponent.getPathname());
    repositoryPolicyViolation.setTime(repositoryComponent.getTime());
    repositoryPolicyViolation.setHash(repositoryComponent.getHash());
    repositoryPolicyViolation.setComponentIdentifier(repositoryComponent.getComponentIdentifier());
    repositoryPolicyViolation.setPolicyId(policy.getId());
    repositoryPolicyViolation.setPolicyName(policy.getName());
    repositoryPolicyViolation.setThreatLevel(policy.getThreatLevel());
    repositoryPolicyViolation.setThreatCategory(policy.getThreatCategory());

    Constraint constraint = policy.getConstraints().get(0);
    Condition condition = constraint.getConditions().get(0);
    ConstraintFact constraintFact = new ConstraintFact(constraint.getId(), constraint.getName(),
        constraint.getOperator().toString());
    constraintFact.addConditionFact(new ConditionFact("", 0, "", "random for condition "
        + condition.getConditionTypeId()));
    repositoryPolicyViolation.setConstraintFactsJson(
        JsonUtils.writeUnformatted(Collections.singleton(constraintFact)));

    return repositoryPolicyViolation;
  }

  private RepositoryPolicyViolation createRepositoryPolicyViolationCausingQuarantine(
      RepositoryComponent repositoryComponent, Policy policy)
  {
    RepositoryPolicyViolation repositoryPolicyViolation =
        createRepositoryPolicyViolationNotCausingQuarantine(repositoryComponent, policy);
    repositoryPolicyViolation.setActionTypeId(Action.ID_FAIL);
    return repositoryPolicyViolation;
  }

  private RepositoryPolicyViolation createWaivedRepositoryPolicyViolationThatWasCausingQuarantine(
      RepositoryComponent repositoryComponent, Policy policy, String waiverId)
  {
    RepositoryPolicyViolation repositoryPolicyViolation =
        createRepositoryPolicyViolationCausingQuarantine(repositoryComponent, policy);
    repositoryPolicyViolation.setWaived(true);
    repositoryPolicyViolation.setPolicyWaiverId(waiverId);
    repositoryPolicyViolation.setWaiveTime(new Date());
    return repositoryPolicyViolation;
  }

  private void assertRepository(
      ApiRepositoryComponentsInQuarantineDTO repositoryComponentsInQuarantineDTO, Repository repoForAssertion,
      Policy policyForAssertion, Map<String, ComponentForAssertions> componentsInQuarantineForAssertions)
  {
    ApiRepositoryDTO repositoryDTO = repositoryComponentsInQuarantineDTO.repository;
    assertThat(repositoryDTO.publicId).isEqualTo(repoForAssertion.getPublicId());
    assertThat(repositoryDTO.format).isEqualTo(repoForAssertion.getFormat());

    List<ApiRepositoryComponentPolicyViolationDTO> repositoryComponentPolicyViolationDTOs =
        repositoryComponentsInQuarantineDTO.components;
    assertThat(repositoryComponentPolicyViolationDTOs).hasSize(4);

    // sorted in order to facilitate assertions
    repositoryComponentPolicyViolationDTOs.sort(Comparator.comparing(rcpv -> rcpv.component.packageUrl));

    assertComponent(policyForAssertion, repositoryComponentPolicyViolationDTOs.get(0),
        componentsInQuarantineForAssertions.get(WITH_POLICY_VIOLATION_CAUSING_QUARANTINE));

    assertComponent(policyForAssertion, repositoryComponentPolicyViolationDTOs.get(1),
        componentsInQuarantineForAssertions.get(WITH_POLICY_VIOLATION_CAUSING_QUARANTINE_AND_OTHER_THAT_IS_NOT));

    assertComponent(policyForAssertion, repositoryComponentPolicyViolationDTOs.get(2),
        componentsInQuarantineForAssertions.get(WITH_2_POLICY_VIOLATIONS_CAUSING_QUARANTINE_AND_OTHER_2_THAT_ARE_NOT));

    assertComponent(policyForAssertion, repositoryComponentPolicyViolationDTOs.get(3),
        componentsInQuarantineForAssertions.get(WITH_WAIVED_POLICY_VIOLATION_THAT_WAS_CAUSING_QUARANTINE));
  }

  private void assertComponent(
      Policy policyForAssertion, ApiRepositoryComponentPolicyViolationDTO repositoryComponentPolicyViolationDTO,
      ComponentForAssertions componentForAssertions)
  {
    ApiRepositoryComponentDTO repositoryComponentDTO = repositoryComponentPolicyViolationDTO.component;

    assertThat(repositoryComponentDTO.packageUrl).isEqualTo(PackageUrlIdentifier.toPackageUrl(
        componentForAssertions.getComponent().getComponentIdentifier()));
    assertThat(repositoryComponentDTO.hash).isEqualTo(componentForAssertions.getComponent().getHash());

    ComponentIdentifier componentIdentifier = componentForAssertions.getComponent().getComponentIdentifier();
    assertThat(repositoryComponentDTO.componentIdentifier.getFormat()).isEqualTo(componentIdentifier.getFormat());
    assertThat(repositoryComponentDTO.componentIdentifier.getCoordinates())
        .isEqualTo(componentIdentifier.getCoordinates());

    assertThat(repositoryComponentDTO.quarantineTime).isNotNull();
    assertThat(repositoryComponentDTO.quarantineReleaseTime).isNull();

    List<ApiPolicyViolationDTOV2> policyViolationDTOV2List = repositoryComponentPolicyViolationDTO.policyViolations;

    assertThat(policyViolationDTOV2List).isNotNull();

    assertThat(policyViolationDTOV2List).hasSize(componentForAssertions.getPolicyViolations().size());

    for (ApiPolicyViolationDTOV2 policyViolationDTOV2 : policyViolationDTOV2List) {
      assertThat(policyViolationDTOV2.policyId).isEqualTo(policyForAssertion.getId());
      assertThat(policyViolationDTOV2.policyName).isEqualTo(policyForAssertion.getName());
      assertThat(policyViolationDTOV2.threatLevel).isEqualTo(policyForAssertion.getThreatLevel());
      assertThat(policyViolationDTOV2.policyViolationId).isNotNull();
      List<ApiConstraintViolationDTO> constraintViolationDTO = policyViolationDTOV2.constraintViolations;
      assertThat(constraintViolationDTO).hasSize(1);
      Constraint constraint = policyForAssertion.getConstraints().get(0);
      assertThat(constraintViolationDTO.get(0).constraintId).isEqualTo(constraint.getId());
      assertThat(constraintViolationDTO.get(0).constraintName).isEqualTo(constraint.getName());
      assertThat(constraintViolationDTO.get(0).reasons.get(0).reason).isEqualTo("random for condition "
          + constraint.getConditions().get(0).getConditionTypeId());
    }
  }

  private static class ComponentForAssertions
  {
    private RepositoryComponent component;

    private List<RepositoryPolicyViolation> policyViolations = new ArrayList<>();

    private RepositoryComponent getComponent() {
      return component;
    }

    private void setComponent(final RepositoryComponent component) {
      this.component = component;
    }

    private List<RepositoryPolicyViolation> getPolicyViolations() {
      return policyViolations;
    }

    private void addPolicyViolation(RepositoryPolicyViolation policyViolation) {
      policyViolations.add(policyViolation);
    }
  }
}
