/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentsInQuarantineDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyViolationDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryComponentDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryComponentPolicyViolationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryComponentsInQuarantineDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryDTO;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.ProxyRepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@ComponentH2Test
public class ApiComponentsInQuarantineReportingServiceTest
    extends AbstractComponentH2Test
{
  @Inject
  private ApiComponentsInQuarantineReportingService service;

  private Policy policy1;

  private Policy policy2;

  @BeforeEach
  public void setup() {
    Condition condition = new Condition("RelativePopularity", "<=", "10");

    Constraint constraint = new Constraint("c1", "constraint1", LogicalOperator.OR);
    constraint.addCondition(condition);
    policy1 = tempEntity.newPolicy("policy1", constraint);

    constraint = new Constraint("c2", "constraint2", LogicalOperator.AND);
    constraint.addCondition(condition);
    policy2 = tempEntity.newPolicy("policy2", constraint);
  }

  @Test
  public void testGetComponentsInQuarantine_NoRepositories() {
    ApiComponentsInQuarantineDTO componentsInQuarantineDTO = service.getComponentsInQuarantine();
    assertThat(componentsInQuarantineDTO.componentsInQuarantine).isEmpty();
  }

  @Test
  public void testGetComponentsInQuarantine_ComponentNotInQuarantine() {
    Repository repository = tempEntity.newRepository();
    tempEntity.newRepositoryComponent(repository.getId());
    ApiComponentsInQuarantineDTO componentsInQuarantineDTO = service.getComponentsInQuarantine();
    assertThat(componentsInQuarantineDTO.componentsInQuarantine).isEmpty();
  }

  @Test
  public void testGetComponentsInQuarantine_ComponentInQuarantine_PolicyViolation_Fail() {
    Repository repository = tempEntity.newRepository("repositoryManager1", "repo1", "maven3");
    ProxyRepositoryComponent component =
        tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "pathname1",
            "hash1", ComponentIdentifier.createMavenCoordinates("g", "a1", "v"), true);
    ProxyRepositoryPolicyViolation policyViolation = PolicyViolationTestHelper
        .createPolicyViolationFail(policy1, component, tempEntity);

    ApiComponentsInQuarantineDTO componentsInQuarantineDTO = service.getComponentsInQuarantine();

    assertThereIsOnlyOneRepositoryAndOnlyOneComponentAndOnlyOnePolicyViolation(componentsInQuarantineDTO, repository,
        component, policyViolation);
  }

  @Test
  public void testGetComponentsInQuarantine_UnknownComponent() {
    Repository repository = tempEntity.newRepository();
    ProxyRepositoryComponent component = tempEntity.newRepositoryComponent(repository.getId(), MatchState.UNKNOWN,
        "testPathname", "testHash", null /* componentIdentifier */, new Date(), new Date());
    ProxyRepositoryPolicyViolation policyViolation =
        PolicyViolationTestHelper.createPolicyViolationFail(policy1, component, tempEntity);

    ApiComponentsInQuarantineDTO componentsInQuarantineDTO = service.getComponentsInQuarantine();

    assertThereIsOnlyOneRepositoryAndOnlyOneComponentAndOnlyOnePolicyViolation(componentsInQuarantineDTO, repository,
        component, policyViolation);
  }

  @Test
  public void testGetComponentsInQuarantine_ComponentInQuarantine_PolicyViolation_Waived() {
    Repository repository = tempEntity.newRepository("repositoryManager1", "repo1", "maven3");
    ProxyRepositoryComponent component =
        tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "pathname1",
            "hash1", ComponentIdentifier.createMavenCoordinates("g", "a1", "v"), true);
    PolicyViolationTestHelper.createPolicyViolationWaived(policy1, component, tempEntity);

    ApiComponentsInQuarantineDTO componentsInQuarantineDTO = service.getComponentsInQuarantine();

    assertThereIsOnlyOneRepositoryAndOnlyOneComponent(componentsInQuarantineDTO, repository, component);
  }

  @Test
  public void testGetComponentsInQuarantine_ComponentInQuarantine_PolicyViolation_WarnAndFailAndWaived() {
    Repository repository = tempEntity.newRepository("repositoryManager1", "repo1", "maven3");
    ProxyRepositoryComponent component =
        tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "pathname1",
            "hash1", ComponentIdentifier.createMavenCoordinates("g", "a1", "v"), true);
    PolicyViolationTestHelper.createPolicyViolationWarn(policy1, component, tempEntity);
    ProxyRepositoryPolicyViolation policyViolation = PolicyViolationTestHelper
        .createPolicyViolationFail(policy1, component, tempEntity);
    PolicyViolationTestHelper.createPolicyViolationWaived(policy1, component, tempEntity);

    ApiComponentsInQuarantineDTO componentsInQuarantineDTO = service.getComponentsInQuarantine();

    assertThereIsOnlyOneRepositoryAndOnlyOneComponentAndOnlyOnePolicyViolation(componentsInQuarantineDTO, repository,
        component, policyViolation);
  }

  @Test
  public void testGetComponentsInQuarantine_ComponentReleasedFromQuarantine() {
    Repository repository = tempEntity.newRepository("repositoryManager1", "repo1", "maven3");
    tempEntity.newRepositoryComponent(repository.getId(), "pathname", new Date(), new Date());

    ApiComponentsInQuarantineDTO componentsInQuarantineDTO = service.getComponentsInQuarantine();

    assertThat(componentsInQuarantineDTO.componentsInQuarantine).isEmpty();
  }

  @Test
  public void testGetComponentsInQuarantine_ComponentNotInQuarantineAndInQuarantineAndReleasedFromQuarantine() {
    Repository repository = tempEntity.newRepository("repositoryManager1", "repo1", "maven3");
    // component not in quarantine
    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "pathname1", "hash1",
        ComponentIdentifier.createMavenCoordinates("g", "a1", "v"), false);
    // component in quarantine
    ProxyRepositoryComponent component =
        tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "pathname2",
            "hash2", ComponentIdentifier.createMavenCoordinates("g", "a2", "v"), true);
    // component released from quarantine
    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "pathname3",
        "hash3", ComponentIdentifier.createMavenCoordinates("g", "a3", "v"), new Date(), new Date(), new Date());

    ApiComponentsInQuarantineDTO componentsInQuarantineDTO = service.getComponentsInQuarantine();

    assertThereIsOnlyOneRepositoryAndOnlyOneComponent(componentsInQuarantineDTO, repository, component);
  }

  @Test
  public void testGetComponentsInQuarantine_MultipleRepositoriesWithMultipleComponentsWithMultiplePolicyViolations() {
    Repository repository1 = tempEntity.newRepository("repositoryManager1", "repo1", "maven3");
    ProxyRepositoryComponent componentInQuarantine1 = tempEntity.newRepositoryComponent(repository1.getId(),
        MatchState.EXACT, "pathname1", "hash1", ComponentIdentifier.createMavenCoordinates("g", "a1", "v"), true);
    ProxyRepositoryPolicyViolation policyViolation1 = PolicyViolationTestHelper
        .createPolicyViolationFail(policy1, componentInQuarantine1, tempEntity);
    ProxyRepositoryPolicyViolation policyViolation2 = PolicyViolationTestHelper
        .createPolicyViolationFail(policy2, componentInQuarantine1, tempEntity);
    ProxyRepositoryComponent componentInQuarantine2 = tempEntity.newRepositoryComponent(repository1.getId(),
        MatchState.EXACT, "pathname2", "hash2", ComponentIdentifier.createMavenCoordinates("g", "a2", "v"), true);
    ProxyRepositoryPolicyViolation policyViolation3 = PolicyViolationTestHelper
        .createPolicyViolationFail(policy1, componentInQuarantine2, tempEntity);
    ProxyRepositoryPolicyViolation policyViolation4 = PolicyViolationTestHelper
        .createPolicyViolationFail(policy2, componentInQuarantine2, tempEntity);

    Repository repository2 = tempEntity.newRepository("repositoryManager2", "repo2", "maven2");
    ProxyRepositoryComponent componentInQuarantine3 = tempEntity.newRepositoryComponent(repository2.getId(),
        MatchState.EXACT, "pathname3", "hash3", ComponentIdentifier.createMavenCoordinates("g", "a3", "v"), true);
    ProxyRepositoryPolicyViolation policyViolation5 = PolicyViolationTestHelper
        .createPolicyViolationFail(policy1, componentInQuarantine3, tempEntity);
    ProxyRepositoryPolicyViolation policyViolation6 = PolicyViolationTestHelper
        .createPolicyViolationFail(policy2, componentInQuarantine3, tempEntity);
    ProxyRepositoryComponent componentInQuarantine4 = tempEntity.newRepositoryComponent(repository2.getId(),
        MatchState.EXACT, "pathname4", "hash4", ComponentIdentifier.createMavenCoordinates("g", "a4", "v"), true);
    ProxyRepositoryPolicyViolation policyViolation7 = PolicyViolationTestHelper
        .createPolicyViolationFail(policy1, componentInQuarantine4, tempEntity);
    ProxyRepositoryPolicyViolation policyViolation8 = PolicyViolationTestHelper
        .createPolicyViolationFail(policy2, componentInQuarantine4, tempEntity);

    ApiComponentsInQuarantineDTO componentsInQuarantineDTO = service.getComponentsInQuarantine();
    // sort to guarantee order for assertions
    sortApiComponentsInQuarantineDTO(componentsInQuarantineDTO);

    assertThat(componentsInQuarantineDTO.componentsInQuarantine).hasSize(2);

    assertOneRepositoryAndTwoComponentsAndTwoPolicyViolationsForEachComponent(
        componentsInQuarantineDTO.componentsInQuarantine.get(0), repository1, componentInQuarantine1,
        componentInQuarantine2, Arrays.asList(policyViolation1, policyViolation2),
        Arrays.asList(policyViolation3, policyViolation4));

    assertOneRepositoryAndTwoComponentsAndTwoPolicyViolationsForEachComponent(
        componentsInQuarantineDTO.componentsInQuarantine.get(1), repository2, componentInQuarantine3,
        componentInQuarantine4, Arrays.asList(policyViolation5, policyViolation6),
        Arrays.asList(policyViolation7, policyViolation8));
  }

  private void sortApiComponentsInQuarantineDTO(ApiComponentsInQuarantineDTO componentsInQuarantineDTO) {
    componentsInQuarantineDTO.componentsInQuarantine.sort(Comparator.comparing(rciq -> rciq.repository.publicId));

    for (ApiRepositoryComponentsInQuarantineDTO repositoryComponentsInQuarantineDTO : componentsInQuarantineDTO.componentsInQuarantine) {
      repositoryComponentsInQuarantineDTO.components.sort(Comparator.comparing(rcpv -> rcpv.component.hash));

      for (ApiRepositoryComponentPolicyViolationDTO repositoryComponentPolicyViolationDTO : repositoryComponentsInQuarantineDTO.components) {
        repositoryComponentPolicyViolationDTO.policyViolations.sort(Comparator.comparing(
            policyViolationDTOV2 -> policyViolationDTOV2.policyName));
      }
    }
  }

  private void assertThereIsOnlyOneRepositoryAndOnlyOneComponent(
      ApiComponentsInQuarantineDTO componentsInQuarantineDTO,
      Repository expectedRepository,
      ProxyRepositoryComponent expectedComponent)
  {
    assertThat(componentsInQuarantineDTO.componentsInQuarantine).hasSize(1);

    ApiRepositoryComponentsInQuarantineDTO repositoryComponentsInQuarantineDTO =
        componentsInQuarantineDTO.componentsInQuarantine.get(0);

    assertApiRepositoryDTO(repositoryComponentsInQuarantineDTO.repository, expectedRepository);

    assertThat(repositoryComponentsInQuarantineDTO.components).hasSize(1);

    ApiRepositoryComponentPolicyViolationDTO repositoryComponentPolicyViolationDTO =
        repositoryComponentsInQuarantineDTO.components.get(0);

    assertApiRepositoryComponentDTO(repositoryComponentPolicyViolationDTO.component, expectedComponent);

    assertThat(repositoryComponentPolicyViolationDTO.policyViolations).isEmpty();
  }

  private void assertThereIsOnlyOneRepositoryAndOnlyOneComponentAndOnlyOnePolicyViolation(
      ApiComponentsInQuarantineDTO componentsInQuarantineDTO,
      Repository expectedRepository,
      ProxyRepositoryComponent expectedComponent,
      ProxyRepositoryPolicyViolation expectedPolicyViolation)
  {
    assertThat(componentsInQuarantineDTO.componentsInQuarantine).hasSize(1);

    ApiRepositoryComponentsInQuarantineDTO repositoryComponentsInQuarantineDTO =
        componentsInQuarantineDTO.componentsInQuarantine.get(0);

    assertApiRepositoryDTO(repositoryComponentsInQuarantineDTO.repository, expectedRepository);

    assertThat(repositoryComponentsInQuarantineDTO.components).hasSize(1);

    ApiRepositoryComponentPolicyViolationDTO repositoryComponentPolicyViolationDTO =
        repositoryComponentsInQuarantineDTO.components.get(0);

    assertApiRepositoryComponentDTO(repositoryComponentPolicyViolationDTO.component, expectedComponent);

    assertThat(repositoryComponentPolicyViolationDTO.policyViolations).hasSize(1);

    ApiPolicyViolationDTOV2 policyViolationDTOV2 = repositoryComponentPolicyViolationDTO.policyViolations.get(0);

    PolicyViolationTestHelper.assertApiPolicyViolationDTOV2(policyViolationDTOV2, expectedPolicyViolation);
  }

  private void assertOneRepositoryAndTwoComponentsAndTwoPolicyViolationsForEachComponent(
      ApiRepositoryComponentsInQuarantineDTO repositoryComponentsInQuarantineDTO,
      Repository expectedRepository,
      ProxyRepositoryComponent firstExpectedComponent,
      ProxyRepositoryComponent secondExpectedComponent,
      List<ProxyRepositoryPolicyViolation> firstComponentExpectedPolicyViolations,
      List<ProxyRepositoryPolicyViolation> secondComponentExpectedPolicyViolations)
  {
    assertApiRepositoryDTO(repositoryComponentsInQuarantineDTO.repository, expectedRepository);

    assertThat(repositoryComponentsInQuarantineDTO.components).hasSize(2);

    ApiRepositoryComponentPolicyViolationDTO repositoryComponentPolicyViolationDTO =
        repositoryComponentsInQuarantineDTO.components.get(0);
    assertApiRepositoryComponentDTO(repositoryComponentPolicyViolationDTO.component, firstExpectedComponent);
    assertThat(repositoryComponentPolicyViolationDTO.policyViolations).hasSize(2);
    PolicyViolationTestHelper
        .assertApiPolicyViolationDTOV2(repositoryComponentPolicyViolationDTO.policyViolations.get(0),
            firstComponentExpectedPolicyViolations.get(0));
    PolicyViolationTestHelper
        .assertApiPolicyViolationDTOV2(repositoryComponentPolicyViolationDTO.policyViolations.get(1),
            firstComponentExpectedPolicyViolations.get(1));

    repositoryComponentPolicyViolationDTO = repositoryComponentsInQuarantineDTO.components.get(1);
    assertApiRepositoryComponentDTO(repositoryComponentPolicyViolationDTO.component, secondExpectedComponent);
    assertThat(repositoryComponentPolicyViolationDTO.policyViolations).hasSize(2);
    PolicyViolationTestHelper
        .assertApiPolicyViolationDTOV2(repositoryComponentPolicyViolationDTO.policyViolations.get(0),
            secondComponentExpectedPolicyViolations.get(0));
    PolicyViolationTestHelper
        .assertApiPolicyViolationDTOV2(repositoryComponentPolicyViolationDTO.policyViolations.get(1),
            secondComponentExpectedPolicyViolations.get(1));
  }

  private void assertApiRepositoryDTO(ApiRepositoryDTO repositoryDTO, Repository expectedRepository) {
    assertThat(repositoryDTO).isNotNull();
    assertThat(repositoryDTO.repositoryId).isEqualTo(expectedRepository.getId());
    assertThat(repositoryDTO.publicId).isEqualTo(expectedRepository.getPublicId());
    assertThat(repositoryDTO.format).isEqualTo(expectedRepository.getFormat());
  }

  private void assertApiRepositoryComponentDTO(
      ApiRepositoryComponentDTO repositoryComponentDTO,
      ProxyRepositoryComponent expectedComponent)
  {
    assertThat(repositoryComponentDTO).isNotNull();

    assertThat(repositoryComponentDTO.packageUrl).isEqualTo(PackageUrlIdentifier.toPackageUrl(
        expectedComponent.getComponentIdentifier()));
    assertThat(repositoryComponentDTO.hash).isEqualTo(expectedComponent.getHash());

    if (expectedComponent.getComponentIdentifier() == null) {
      assertThat(repositoryComponentDTO.componentIdentifier).isNull();
    }
    else {
      assertThat(repositoryComponentDTO.componentIdentifier.toComponentIdentifier())
          .isEqualTo(expectedComponent.getComponentIdentifier());
    }
    assertThat(repositoryComponentDTO.displayName).isEqualTo(expectedComponent.getDisplayName());

    assertThat(repositoryComponentDTO.quarantineId).isEqualTo(expectedComponent.getId());
    assertThat(repositoryComponentDTO.quarantineTime).isEqualTo(expectedComponent.getQuarantineTime());
    assertThat(repositoryComponentDTO.quarantineReleaseTime).isNull();
  }
}
