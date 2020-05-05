/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationValueDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.actions.ApiComponentChangeActionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.CoordinatesConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.dependency.ComponentDependenciesDTO;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.google.inject.Binder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType.NEXT_NON_FAILING;
import static com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType.NEXT_NON_FAILING_WITH_DEPENDENCIES;
import static com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS;
import static com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;

public class ComponentRemediationServiceTest
    extends AbstractComponentTest
{
  @Mock
  private HdsClient hdsClientMock;

  @Inject
  private ComponentRemediationService componentRemediationService;

  @Override
  public void configure(Binder binder) {
    binder.bind(HdsClient.class).toInstance(hdsClientMock);
    super.configure(binder);
  }

  private static final ComponentIdentifier MAVEN_COORDINATES_A1_V1 = ComponentIdentifier.createMavenCoordinates(
      "g1", "a1", "v1", "", "jar");

  private static final ComponentIdentifier MAVEN_COORDINATES_A1_V2 = ComponentIdentifier.createMavenCoordinates(
      "g1", "a1", "v2", "", "jar");

  private static final ComponentIdentifier MAVEN_COORDINATES_A1_V3 = ComponentIdentifier.createMavenCoordinates(
      "g1", "a1", "v3", "", "jar");

  private static final ComponentIdentifier MAVEN_COORDINATES_A1_V4 = ComponentIdentifier.createMavenCoordinates(
      "g1", "a1", "v4", "", "jar");

  private static final ComponentIdentifier MAVEN_COORDINATES_A1_V5 = ComponentIdentifier.createMavenCoordinates(
      "g1", "a1", "v5", "", "jar");

  private static final ComponentIdentifier MAVEN_COORDINATES_A1_V6 = ComponentIdentifier.createMavenCoordinates(
      "g1", "a1", "v6", "", "jar");

  private static final ComponentIdentifier MAVEN_COORDINATES_A1_V11 = ComponentIdentifier.createMavenCoordinates(
      "g1", "a1", "v11", "", "jar");

  private static final ComponentIdentifier MAVEN_COORDINATES_A2_V1 = ComponentIdentifier.createMavenCoordinates(
      "g1", "a2", "v1", "", "jar");

  private static final ComponentIdentifier MAVEN_COORDINATES_A2_V2 = ComponentIdentifier.createMavenCoordinates(
      "g1", "a2", "v2", "", "jar");

  private static final ComponentIdentifier MAVEN_COORDINATES_A2_V3 = ComponentIdentifier.createMavenCoordinates(
      "g1", "a2", "v3", "", "jar");

  private static final ComponentIdentifier MAVEN_COORDINATES_A2_V4 = ComponentIdentifier.createMavenCoordinates(
      "g1", "a2", "v4", "", "jar");

  private static final ComponentIdentifier MAVEN_COORDINATES_A2_V5 = ComponentIdentifier.createMavenCoordinates(
      "g1", "a2", "v5", "", "jar");

  private static final ComponentIdentifier MAVEN_COORDINATES_A2_V11 = ComponentIdentifier.createMavenCoordinates(
      "g1", "a2", "v11", "", "jar");

  private static final PackageUrlIdentifier purlA1V1 = PackageUrlIdentifier.fromComponentIdentifier(
      MAVEN_COORDINATES_A1_V1);

  private static final PackageUrlIdentifier purlA1V2 = PackageUrlIdentifier.fromComponentIdentifier(
      MAVEN_COORDINATES_A1_V2);

  private static final PackageUrlIdentifier purlA1V3 = PackageUrlIdentifier.fromComponentIdentifier(
      MAVEN_COORDINATES_A1_V3);

  private static final PackageUrlIdentifier purlA1V4 = PackageUrlIdentifier.fromComponentIdentifier(
      MAVEN_COORDINATES_A1_V4);

  private static final PackageUrlIdentifier purlA1V5 = PackageUrlIdentifier.fromComponentIdentifier(
      MAVEN_COORDINATES_A1_V5);

  private static final PackageUrlIdentifier purlA1V6 = PackageUrlIdentifier.fromComponentIdentifier(
      MAVEN_COORDINATES_A1_V6);

  private static final PackageUrlIdentifier purlA2V1 = PackageUrlIdentifier.fromComponentIdentifier(
      MAVEN_COORDINATES_A2_V1);

  private static final PackageUrlIdentifier purlA2V2 = PackageUrlIdentifier.fromComponentIdentifier(
      MAVEN_COORDINATES_A2_V2);

  private static final PackageUrlIdentifier purlA2V3 = PackageUrlIdentifier.fromComponentIdentifier(
      MAVEN_COORDINATES_A2_V3);

  private static final PackageUrlIdentifier purlA2V4 = PackageUrlIdentifier.fromComponentIdentifier(
      MAVEN_COORDINATES_A2_V4);

  private static final PackageUrlIdentifier purlA2V5 = PackageUrlIdentifier.fromComponentIdentifier(
      MAVEN_COORDINATES_A2_V5);

  private static final PolicyAlert warnAlert = new PolicyAlert(new PolicyFact("policyId", "Policy Name", 10),
      Collections.singletonList(new Action(Action.ID_WARN)));

  private static final PolicyAlert failAlert = new PolicyAlert(new PolicyFact("policyId", "Policy Name", 10),
      Collections.singletonList(new Action(Action.ID_FAIL)));

  private Organization org;

  private final ApiComponentDTOV2 componentDtoA1V1 = new ApiComponentDTOV2();

  private final ApiComponentDTOV2 componentDtoA1V2 = new ApiComponentDTOV2();

  private final ApiComponentDTOV2 componentDtoA1V3 = new ApiComponentDTOV2();

  private final ApiComponentDTOV2 componentDtoA1V4 = new ApiComponentDTOV2();

  private final ApiComponentDTOV2 componentDtoA1V5 = new ApiComponentDTOV2();

  private final ApiComponentDTOV2 componentDtoA1V6 = new ApiComponentDTOV2();

  private final ApiComponentDTOV2 componentDtoA1V11 = new ApiComponentDTOV2();

  private final ComponentDetailsDTO detailsDtoA1V1 = new ComponentDetailsDTO();

  private final ComponentDetailsDTO detailsDtoA1V2 = new ComponentDetailsDTO();

  private final ComponentDetailsDTO detailsDtoA1V3 = new ComponentDetailsDTO();

  private final ComponentDetailsDTO detailsDtoA1V4 = new ComponentDetailsDTO();

  private final ComponentDetailsDTO detailsDtoA1V5 = new ComponentDetailsDTO();

  private final ComponentDetailsDTO detailsDtoA1V6 = new ComponentDetailsDTO();

  private final ComponentDetailsDTO detailsDtoA1V11 = new ComponentDetailsDTO();

  private ComponentDetails detailsA2V1 = new ComponentDetails();

  private ComponentDetails detailsA2V2 = new ComponentDetails();

  private ComponentDetails detailsA2V3 = new ComponentDetails();

  private ComponentDetails detailsA2V4 = new ComponentDetails();

  private ComponentDetails detailsA2V5 = new ComponentDetails();

  private ComponentDetails detailsA2V11 = new ComponentDetails();

  @Before
  public void before() {
    org = tempEntity.newOrganization();

    componentDtoA1V1.packageUrl = "pkg:maven/g1/a1@v1?type=jar";
    componentDtoA1V2.packageUrl = "pkg:maven/g1/a1@v2?type=jar";
    componentDtoA1V3.packageUrl = "pkg:maven/g1/a1@v3?type=jar";
    componentDtoA1V4.packageUrl = "pkg:maven/g1/a1@v4?type=jar";
    componentDtoA1V5.packageUrl = "pkg:maven/g1/a1@v5?type=jar";
    componentDtoA1V6.packageUrl = "pkg:maven/g1/a1@v6?type=jar";
    componentDtoA1V11.packageUrl = "pkg:maven/g1/a1@v11?type=jar";

    // A1
    detailsDtoA1V1.componentIdentifier = MAVEN_COORDINATES_A1_V1;
    detailsDtoA1V1.violatedPolicyCount = 2;
    detailsDtoA1V1.policyAlerts = Arrays.asList(warnAlert, failAlert);

    detailsDtoA1V2.componentIdentifier = MAVEN_COORDINATES_A1_V2;
    detailsDtoA1V2.violatedPolicyCount = 1;
    detailsDtoA1V2.policyAlerts = Collections.singletonList(warnAlert);

    detailsDtoA1V3.componentIdentifier = MAVEN_COORDINATES_A1_V3;
    detailsDtoA1V3.violatedPolicyCount = 0;

    detailsDtoA1V4.componentIdentifier = MAVEN_COORDINATES_A1_V4;
    detailsDtoA1V4.violatedPolicyCount = 0;

    detailsDtoA1V5.componentIdentifier = MAVEN_COORDINATES_A1_V5;
    detailsDtoA1V5.violatedPolicyCount = 1;
    detailsDtoA1V5.policyAlerts = Collections.singletonList(warnAlert);

    detailsDtoA1V6.componentIdentifier = MAVEN_COORDINATES_A1_V6;
    detailsDtoA1V6.violatedPolicyCount = 1;
    detailsDtoA1V6.policyAlerts = Collections.singletonList(failAlert);

    detailsDtoA1V11.componentIdentifier = MAVEN_COORDINATES_A1_V11;
    detailsDtoA1V11.violatedPolicyCount = 0;

    // A2
    detailsA2V1 = buildComponentDetails(MAVEN_COORDINATES_A2_V1, Arrays.asList(warnAlert, failAlert));
    detailsA2V2 = buildComponentDetails(MAVEN_COORDINATES_A2_V2, Collections.singletonList(warnAlert));
    detailsA2V3 = buildComponentDetails(MAVEN_COORDINATES_A2_V3, null);
    detailsA2V4 = buildComponentDetails(MAVEN_COORDINATES_A2_V4, null);
    detailsA2V5 = buildComponentDetails(MAVEN_COORDINATES_A2_V5, Collections.singletonList(warnAlert));
    detailsA2V11 = buildComponentDetails(MAVEN_COORDINATES_A2_V11, Collections.emptyList());

    Policy policy = new Policy("policyG1A2V1", "policyG1A2V1");
    policy.setOwnerId(org.getParentOwnerId());
    policy.setThreatLevel(10);
    Constraint constraint = new Constraint(null, "Test Constraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(CoordinatesConditionType.ID, "match", "maven:g1:a2:v1"));
    policy.addConstraint(constraint);
    policy.setAction(DevelopStageType.ID, Action.ID_FAIL);
    policy.setAction(BuildStageType.ID, Action.ID_WARN);
    tempEntity.newPolicy(policy);

    policy = new Policy("policyG1A2V2", "policyG1A2V2");
    policy.setOwnerId(org.getParentOwnerId());
    policy.setThreatLevel(5);
    constraint = new Constraint(null, "Test Constraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(CoordinatesConditionType.ID, "match", "maven:g1:a2:v2"));
    policy.addConstraint(constraint);
    policy.setAction(DevelopStageType.ID, Action.ID_WARN);
    tempEntity.newPolicy(policy);

    policy = new Policy("policyG1A2V5", "policyG1A2V5");
    policy.setOwnerId(org.getParentOwnerId());
    policy.setThreatLevel(5);
    constraint = new Constraint(null, "Test Constraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(CoordinatesConditionType.ID, "match", "maven:g1:a2:v5"));
    policy.addConstraint(constraint);
    policy.setAction(DevelopStageType.ID, Action.ID_WARN);
    tempEntity.newPolicy(policy);
  }

  private ComponentDetails buildComponentDetails(ComponentIdentifier identifier, List<PolicyAlert> alerts) {
    ComponentDetails details = new ComponentDetails(identifier);
    details.setPolicyAlerts(alerts);
    return details;
  }

  private void mockHdsGetComponentDependencies(ComponentDependenciesDTO returnDto) {
    lenient().when(hdsClientMock.post(eq(ComponentDependenciesDTO.class), eq("rest/component/dependencies"),
        anyCollection())).thenReturn(returnDto);
  }

  /*
   --- Advanced strategies = false ---
   */

  /**
   * Test with advanced strategies flag as false to verify we are not getting back "with dependencies" remedies.
   * Looking up version a1v1, a1v2, and a1v3 with a1v1 being the current version.
   * None of the versions have dependencies.
   * a1v1 has failing alert, a1v2 has warning alert, and a1v3 has no alerts.
   */
  @Test
  public void testNoAdvanced_NoDependencies() {
    ComponentDependenciesDTO returnDto = new ComponentDependenciesDTO(new HashMap<>(), new HashMap<>());
    List<ComponentDetailsDTO> allVersions = Arrays.asList(detailsDtoA1V1, detailsDtoA1V2, detailsDtoA1V3);
    mockHdsGetComponentDependencies(returnDto);
    ApiComponentRemediationValueDTO dto = componentRemediationService.getSuggestedRemediation(MAVEN_COORDINATES_A1_V1,
        allVersions, org.getType(), org.getId(), DevelopStageType.ID, false);

    assertRemediations(dto, buildChangeDto(NEXT_NO_VIOLATIONS, componentDtoA1V3.packageUrl));
    assertThat(dto.versionChanges).hasSize(1);
  }

  /**
   * Test with advanced strategies flag as false but also with dependencies included
   * to verify we are not getting back "with dependencies" remedies.
   * Looking up versions a1v1, a1v2, a1v3, and a1v4 with a1v2 being the current version.
   * a1v1 has no dependencies
   * a1v2 dependencies: a2v1, a2v2
   * a1v3 dependencies: a2v3, a2v4
   * a1v4 dependencies: a2v3, a2v4
   * a1v1 has failing alert, a1v2 has warning alert, a1v3 has no alerts, and a1v4 has no alerts.
   * a2v1 has failing alert, a2v2 has warning alert, a2v3 has no alerts, and a2v4 has no alerts.
   */
  @Test
  public void testNoAdvanced_WithDependencies() {
    Map<PackageUrlIdentifier, Collection<PackageUrlIdentifier>> dependenciesMap = new HashMap<>();
    Map<PackageUrlIdentifier, ComponentDetails> detailsMap = new HashMap<>();

    dependenciesMap.put(purlA1V1, Collections.emptyList());
    dependenciesMap.put(purlA1V2, Arrays.asList(purlA2V1, purlA2V2));
    dependenciesMap.put(purlA1V3, Arrays.asList(purlA2V3, purlA2V4));
    dependenciesMap.put(purlA1V4, Arrays.asList(purlA2V3, purlA2V4));

    detailsMap.put(purlA2V1, detailsA2V1);
    detailsMap.put(purlA2V2, detailsA2V2);
    detailsMap.put(purlA2V3, detailsA2V3);
    detailsMap.put(purlA2V4, detailsA2V4);

    ComponentDependenciesDTO returnDto = new ComponentDependenciesDTO(dependenciesMap, detailsMap);
    List<ComponentDetailsDTO> allVersions = Arrays.asList(detailsDtoA1V1, detailsDtoA1V2, detailsDtoA1V3,
        detailsDtoA1V4);
    mockHdsGetComponentDependencies(returnDto);
    ApiComponentRemediationValueDTO dto = componentRemediationService.getSuggestedRemediation(MAVEN_COORDINATES_A1_V2,
        allVersions, org.getType(), org.getId(), DevelopStageType.ID, false);

    assertRemediations(dto, buildChangeDto(NEXT_NO_VIOLATIONS, componentDtoA1V3.packageUrl));
    assertThat(dto.versionChanges).hasSize(1);
  }

  /*
   --- Advanced strategies = true ---
   */

  /**
   * Test with advanced strategies flag as true with no dependencies.
   * Looking up versions a1v1, a1v2, and a1v3 with a1v1 being the current version.
   * None of the versions have dependencies.
   * a1v1 has failing alert, a1v2 has warning alert, and a1v3 has no alerts.
   */
  @Test
  public void testAdvanced_NoDependencies() {
    ComponentDependenciesDTO returnDto = new ComponentDependenciesDTO(new HashMap<>(), new HashMap<>());
    List<ComponentDetailsDTO> allVersions = Arrays.asList(detailsDtoA1V1, detailsDtoA1V2, detailsDtoA1V3);
    mockHdsGetComponentDependencies(returnDto);
    ApiComponentRemediationValueDTO dto = componentRemediationService.getSuggestedRemediation(MAVEN_COORDINATES_A1_V1,
        allVersions, org.getType(), org.getId(), DevelopStageType.ID, true);

    assertRemediations(dto, buildChangeDto(NEXT_NO_VIOLATIONS, componentDtoA1V3.packageUrl));
    assertRemediations(dto, buildChangeDto(NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES, componentDtoA1V3.packageUrl));
    assertRemediations(dto, buildChangeDto(NEXT_NON_FAILING, componentDtoA1V2.packageUrl));
    assertRemediations(dto, buildChangeDto(NEXT_NON_FAILING_WITH_DEPENDENCIES, componentDtoA1V2.packageUrl));
    assertThat(dto.versionChanges).hasSize(4);
  }

  /**
   * Test with advanced strategies flag as true, with dependencies.
   * Looking up versions a1v1, a1v2, a1v3, and a1v4 with a1v1 being the current version.
   * all versions have no dependencies
   * a1v1 has failing alert, a1v2 has warning alert, a1v3 has no alerts, and a1v4 has no alerts.
   * a2v1 has failing alert, a2v2 has warning alert, a2v3 has no alerts, and a2v4 has no alerts.
   */
  @Test
  public void testAdvanced_EmptyDependencies() {
    Map<PackageUrlIdentifier, Collection<PackageUrlIdentifier>> dependenciesMap = new HashMap<>();

    dependenciesMap.put(purlA1V1, Collections.emptyList());
    dependenciesMap.put(purlA1V2, Collections.emptyList());
    dependenciesMap.put(purlA1V3, Collections.emptyList());
    dependenciesMap.put(purlA1V4, Collections.emptyList());

    ComponentDependenciesDTO returnDto = new ComponentDependenciesDTO(dependenciesMap, new HashMap<>());
    List<ComponentDetailsDTO> allVersions = Arrays.asList(detailsDtoA1V1, detailsDtoA1V2, detailsDtoA1V3,
        detailsDtoA1V4);
    mockHdsGetComponentDependencies(returnDto);
    ApiComponentRemediationValueDTO dto = componentRemediationService.getSuggestedRemediation(MAVEN_COORDINATES_A1_V1,
        allVersions, org.getType(), org.getId(), DevelopStageType.ID, true);

    assertRemediations(dto, buildChangeDto(NEXT_NO_VIOLATIONS, componentDtoA1V3.packageUrl));
    assertRemediations(dto, buildChangeDto(NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES, componentDtoA1V3.packageUrl));
    assertRemediations(dto, buildChangeDto(NEXT_NON_FAILING, componentDtoA1V2.packageUrl));
    assertRemediations(dto, buildChangeDto(NEXT_NON_FAILING_WITH_DEPENDENCIES, componentDtoA1V2.packageUrl));
    assertThat(dto.versionChanges).hasSize(4);
  }

  /**
   * Test with advanced strategies flag as true, with dependencies, no stage information.
   * Looking up versions a1v1, a1v2, a1v3, and a1v4 with a1v1 being the current version.
   * a1v1 has no dependencies
   * a1v2 dependencies: a2v1, a2v2
   * a1v3 dependencies: a2v2, a2v3
   * a1v4 dependencies: a2v3, a2v4
   * a1v1 has failing alert, a1v2 has warning alert, a1v3 has no alerts, and a1v4 has no alerts.
   * a2v1 has failing alert, a2v2 has warning alert, a2v3 has no alerts, and a2v4 has no alerts.
   */
  @Test
  public void testAdvanced_NoStage() {
    Map<PackageUrlIdentifier, Collection<PackageUrlIdentifier>> dependenciesMap = new HashMap<>();
    Map<PackageUrlIdentifier, ComponentDetails> detailsMap = new HashMap<>();

    dependenciesMap.put(purlA1V1, Collections.emptyList());
    dependenciesMap.put(purlA1V2, Arrays.asList(purlA2V1, purlA2V2));
    dependenciesMap.put(purlA1V3, Arrays.asList(purlA2V2, purlA2V3));
    dependenciesMap.put(purlA1V4, Arrays.asList(purlA2V3, purlA2V4));

    detailsMap.put(purlA2V1,  detailsA2V1);
    detailsMap.put(purlA2V2,  detailsA2V2);
    detailsMap.put(purlA2V3,  detailsA2V3);
    detailsMap.put(purlA2V4,  detailsA2V4);

    ComponentDependenciesDTO returnDto = new ComponentDependenciesDTO(dependenciesMap, detailsMap);
    List<ComponentDetailsDTO> allVersions = Arrays.asList(detailsDtoA1V1, detailsDtoA1V2, detailsDtoA1V3,
        detailsDtoA1V4);
    mockHdsGetComponentDependencies(returnDto);
    ApiComponentRemediationValueDTO dto = componentRemediationService.getSuggestedRemediation(MAVEN_COORDINATES_A1_V1,
        allVersions, org.getType(), org.getId(), null, true);

    assertRemediations(dto, buildChangeDto(NEXT_NO_VIOLATIONS, componentDtoA1V3.packageUrl));
    assertRemediations(dto, buildChangeDto(NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES, componentDtoA1V4.packageUrl));
    assertThat(dto.versionChanges).hasSize(2);
  }

  /**
   * Test with advanced strategies flag as true, with dependencies.
   * Looking up versions a1v1, a1v2, a1v3, a1v4 and a1v5 with a1v3 being the current version.
   * a1v1 dependencies: a2v3, a2v4
   * a1v2 dependencies: a2v1, a2v2
   * a1v3 dependencies: a2v2, a2v3
   * a1v4 dependencies: a2v4
   * a1v5 dependencies: a2v5
   * a1v1 has failing alert, a1v2 has warning alert, a1v3 has no alerts, a1v4 has no alerts, and a1v5 has warning alert.
   * a2v1 has failing alert, a2v2 has warning alert, a2v3 has no alerts, a2v4 has no alerts, and a2v5 has warning alert.
   */
  @Test
  public void testAdvanced_CurrentVersionNotFirstInAllVersions() {
    Map<PackageUrlIdentifier, Collection<PackageUrlIdentifier>> dependenciesMap = new HashMap<>();
    Map<PackageUrlIdentifier, ComponentDetails> detailsMap = new HashMap<>();

    dependenciesMap.put(purlA1V1, Arrays.asList(purlA2V3, purlA2V4));
    dependenciesMap.put(purlA1V2, Arrays.asList(purlA2V1, purlA2V2));
    dependenciesMap.put(purlA1V3, Arrays.asList(purlA2V2, purlA2V3));
    dependenciesMap.put(purlA1V4, Collections.singletonList(purlA2V4));
    dependenciesMap.put(purlA1V5, Collections.singletonList(purlA2V5));

    detailsMap.put(purlA2V1,  detailsA2V1);
    detailsMap.put(purlA2V2,  detailsA2V2);
    detailsMap.put(purlA2V3,  detailsA2V3);
    detailsMap.put(purlA2V4,  detailsA2V4);
    detailsMap.put(purlA2V5,  detailsA2V5);

    ComponentDependenciesDTO returnDto = new ComponentDependenciesDTO(dependenciesMap, detailsMap);
    List<ComponentDetailsDTO> allVersions = Arrays.asList(detailsDtoA1V1, detailsDtoA1V2, detailsDtoA1V3,
        detailsDtoA1V4, detailsDtoA1V5);
    mockHdsGetComponentDependencies(returnDto);
    ApiComponentRemediationValueDTO dto = componentRemediationService.getSuggestedRemediation(MAVEN_COORDINATES_A1_V3,
        allVersions, org.getType(), org.getId(), DevelopStageType.ID, true);

    assertRemediations(dto, buildChangeDto(NEXT_NO_VIOLATIONS, componentDtoA1V3.packageUrl));
    assertRemediations(dto, buildChangeDto(NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES, componentDtoA1V4.packageUrl));
    assertRemediations(dto, buildChangeDto(NEXT_NON_FAILING, componentDtoA1V3.packageUrl));
    assertRemediations(dto, buildChangeDto(NEXT_NON_FAILING_WITH_DEPENDENCIES, componentDtoA1V3.packageUrl));
    assertThat(dto.versionChanges).hasSize(4);
  }

  /**
   * Test with advanced strategies flag as true, with dependencies.
   * Looking up versions a1v1, a1v2, a1v3, a1v4 and a1v5 with a1v5 being the current version.
   * a1v1 dependencies: a2v1, a2v2
   * a1v2 dependencies: none
   * a1v3 dependencies: a2v3
   * a1v4 dependencies: a2v4
   * a1v5 dependencies: a2v5
   * a1v1 has failing alert, a1v2 has warning alert, a1v3 has no alerts, a1v4 has no alerts, and a1v5 has warning alert.
   * a2v1 has failing alert, a2v2 has warning alert, a2v3 has no alerts, a2v4 has no alerts, and a2v5 has warning alert.
   */
  @Test
  public void testAdvanced_CurrentVersionLastInAllVersions() {
    Map<PackageUrlIdentifier, Collection<PackageUrlIdentifier>> dependenciesMap = new HashMap<>();
    Map<PackageUrlIdentifier, ComponentDetails> detailsMap = new HashMap<>();

    dependenciesMap.put(purlA1V1, Arrays.asList(purlA2V1, purlA2V2));
    dependenciesMap.put(purlA1V2, Collections.emptyList());
    dependenciesMap.put(purlA1V3, Collections.singletonList(purlA2V3));
    dependenciesMap.put(purlA1V4, Collections.singletonList(purlA2V4));
    dependenciesMap.put(purlA1V5, Collections.singletonList(purlA2V5));

    detailsMap.put(purlA2V1,  detailsA2V1);
    detailsMap.put(purlA2V2,  detailsA2V2);
    detailsMap.put(purlA2V3,  detailsA2V3);
    detailsMap.put(purlA2V4,  detailsA2V4);
    detailsMap.put(purlA2V5,  detailsA2V5);

    ComponentDependenciesDTO returnDto = new ComponentDependenciesDTO(dependenciesMap, detailsMap);
    List<ComponentDetailsDTO> allVersions = Arrays.asList(detailsDtoA1V1, detailsDtoA1V2, detailsDtoA1V3,
        detailsDtoA1V4, detailsDtoA1V5);
    mockHdsGetComponentDependencies(returnDto);
    ApiComponentRemediationValueDTO dto = componentRemediationService.getSuggestedRemediation(MAVEN_COORDINATES_A1_V5,
        allVersions, org.getType(), org.getId(), DevelopStageType.ID, true);

    assertRemediations(dto, buildChangeDto(NEXT_NON_FAILING, componentDtoA1V5.packageUrl));
    assertRemediations(dto, buildChangeDto(NEXT_NON_FAILING_WITH_DEPENDENCIES, componentDtoA1V5.packageUrl));
    assertThat(dto.versionChanges).hasSize(2);
  }

  /**
   * Test with advanced strategies flag as true, with dependencies.
   * Looking up versions a1v3, and a1v4 with a1v3 being the current version.
   * a1v3 dependencies: a2v3
   * a1v4 dependencies: none
   * a1v3 has no alerts, and a1v4 has no alerts.
   * a2v3 has no alerts.
   */
  @Test
  public void testAdvanced_CurrentVersionNonViolatingWithDependencies() {
    Map<PackageUrlIdentifier, Collection<PackageUrlIdentifier>> dependenciesMap = new HashMap<>();
    Map<PackageUrlIdentifier, ComponentDetails> detailsMap = new HashMap<>();

    dependenciesMap.put(purlA1V3, Collections.singletonList(purlA2V3));
    dependenciesMap.put(purlA1V4, Collections.emptyList());

    detailsMap.put(purlA2V3,  detailsA2V3);

    ComponentDependenciesDTO returnDto = new ComponentDependenciesDTO(dependenciesMap, detailsMap);
    List<ComponentDetailsDTO> allVersions = Arrays.asList(detailsDtoA1V3, detailsDtoA1V4);
    mockHdsGetComponentDependencies(returnDto);
    ApiComponentRemediationValueDTO dto = componentRemediationService.getSuggestedRemediation(MAVEN_COORDINATES_A1_V3,
        allVersions, org.getType(), org.getId(), DevelopStageType.ID, true);

    assertRemediations(dto, buildChangeDto(NEXT_NO_VIOLATIONS, componentDtoA1V3.packageUrl));
    assertRemediations(dto, buildChangeDto(NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES, componentDtoA1V3.packageUrl));
    assertRemediations(dto, buildChangeDto(NEXT_NON_FAILING, componentDtoA1V3.packageUrl));
    assertRemediations(dto, buildChangeDto(NEXT_NON_FAILING_WITH_DEPENDENCIES, componentDtoA1V3.packageUrl));
    assertThat(dto.versionChanges).hasSize(4);
  }

  /**
   * Test with advanced strategies flag as true, with dependencies.
   * Looking up versions a1v2 with a1v2 being the current version.
   * a1v2 dependencies: a2v2
   * a1v2 has warning alert.
   * a2v2 has warning alert.
   */
  @Test
  public void testAdvanced_CurrentVersionNonFailingWithDependencies() {
    Map<PackageUrlIdentifier, Collection<PackageUrlIdentifier>> dependenciesMap = new HashMap<>();
    Map<PackageUrlIdentifier, ComponentDetails> detailsMap = new HashMap<>();

    dependenciesMap.put(purlA1V2, Collections.singletonList(purlA2V2));

    detailsMap.put(purlA2V2,  detailsA2V2);

    ComponentDependenciesDTO returnDto = new ComponentDependenciesDTO(dependenciesMap, detailsMap);
    List<ComponentDetailsDTO> allVersions = Arrays.asList(detailsDtoA1V2);
    mockHdsGetComponentDependencies(returnDto);
    ApiComponentRemediationValueDTO dto = componentRemediationService.getSuggestedRemediation(MAVEN_COORDINATES_A1_V2,
        allVersions, org.getType(), org.getId(), DevelopStageType.ID, true);

    assertRemediations(dto, buildChangeDto(NEXT_NON_FAILING, componentDtoA1V2.packageUrl));
    assertRemediations(dto, buildChangeDto(NEXT_NON_FAILING_WITH_DEPENDENCIES, componentDtoA1V2.packageUrl));
    assertThat(dto.versionChanges).hasSize(2);
  }

  /**
   * Test with advanced strategies flag as true, with dependencies.
   * Looking up versions a1v1, a1v2 and a1v5 with a1v1 being the current version.
   * a1v1 dependencies: a2v2
   * a1v2 dependencies: a2v1
   * a1v5 dependencies: a2v2, a2v5
   * a1v1 has failing alert, a1v2 has warning alert, and a1v5 has warning alert.
   * a2v1 has failing alert, a2v2 has warning alert, and a2v5 has warning alert.
   */
  @Test
  public void testAdvanced_AllViolating() {
    Map<PackageUrlIdentifier, Collection<PackageUrlIdentifier>> dependenciesMap = new HashMap<>();
    Map<PackageUrlIdentifier, ComponentDetails> detailsMap = new HashMap<>();

    dependenciesMap.put(purlA1V1, Collections.singletonList(purlA2V2));
    dependenciesMap.put(purlA1V2, Collections.singletonList(purlA2V1));
    dependenciesMap.put(purlA1V5, Arrays.asList(purlA2V2, purlA2V5));

    detailsMap.put(purlA2V1,  detailsA2V1);
    detailsMap.put(purlA2V2,  detailsA2V2);
    detailsMap.put(purlA2V5,  detailsA2V5);

    ComponentDependenciesDTO returnDto = new ComponentDependenciesDTO(dependenciesMap, detailsMap);
    List<ComponentDetailsDTO> allVersions = Arrays.asList(detailsDtoA1V1, detailsDtoA1V2, detailsDtoA1V5);
    mockHdsGetComponentDependencies(returnDto);
    ApiComponentRemediationValueDTO dto = componentRemediationService.getSuggestedRemediation(MAVEN_COORDINATES_A1_V1,
        allVersions, org.getType(), org.getId(), DevelopStageType.ID, true);

    assertRemediations(dto, buildChangeDto(NEXT_NON_FAILING, componentDtoA1V2.packageUrl));
    assertRemediations(dto, buildChangeDto(NEXT_NON_FAILING_WITH_DEPENDENCIES, componentDtoA1V5.packageUrl));
    assertThat(dto.versionChanges).hasSize(2);
  }

  /**
   * Test with advanced strategies flag as true, with dependencies.
   * Looking up versions a1v1, and a1v6 with a1v1 being the current version.
   * a1v1 dependencies: a2v3
   * a1v6 dependencies: none
   * a1v1 has failing alert, and a1v6 has failing alert.
   * a2v3 has no alerts.
   */
  @Test
  public void testAdvanced_AllFailing() {
    Map<PackageUrlIdentifier, Collection<PackageUrlIdentifier>> dependenciesMap = new HashMap<>();
    Map<PackageUrlIdentifier, ComponentDetails> detailsMap = new HashMap<>();

    dependenciesMap.put(purlA1V1, Collections.singletonList(purlA2V3));
    dependenciesMap.put(purlA1V6, Collections.emptyList());

    detailsMap.put(purlA2V3,  detailsA2V3);

    ComponentDependenciesDTO returnDto = new ComponentDependenciesDTO(dependenciesMap, detailsMap);
    List<ComponentDetailsDTO> allVersions = Arrays.asList(detailsDtoA1V1, detailsDtoA1V6);
    mockHdsGetComponentDependencies(returnDto);
    ApiComponentRemediationValueDTO dto = componentRemediationService.getSuggestedRemediation(MAVEN_COORDINATES_A1_V1,
        allVersions, org.getType(), org.getId(), DevelopStageType.ID, true);

    assertRemediations(dto, null);
    assertThat(dto.versionChanges).hasSize(0);
  }

  /**
   * Test with advanced strategies flag as true, with dependencies.
   * Looking up single version a1v3 with a1v3 being the current version.
   * a1v3 dependencies: a2v1
   * a1v3 has no alerts, and a2v1 has failing alert.
   */
  @Test
  public void testAdvanced_SingleVersion_DependenciesFailing() {
    Map<PackageUrlIdentifier, Collection<PackageUrlIdentifier>> dependenciesMap = new HashMap<>();
    Map<PackageUrlIdentifier, ComponentDetails> detailsMap = new HashMap<>();

    dependenciesMap.put(purlA1V3, Collections.singletonList(purlA2V1));

    detailsMap.put(purlA2V1,  detailsA2V1);

    ComponentDependenciesDTO returnDto = new ComponentDependenciesDTO(dependenciesMap, detailsMap);
    List<ComponentDetailsDTO> allVersions = Arrays.asList(detailsDtoA1V3);
    mockHdsGetComponentDependencies(returnDto);
    ApiComponentRemediationValueDTO dto = componentRemediationService.getSuggestedRemediation(MAVEN_COORDINATES_A1_V3,
        allVersions, org.getType(), org.getId(), DevelopStageType.ID, true);

    assertRemediations(dto, buildChangeDto(NEXT_NO_VIOLATIONS, componentDtoA1V3.packageUrl));
    assertRemediations(dto, buildChangeDto(NEXT_NON_FAILING, componentDtoA1V3.packageUrl));
    assertThat(dto.versionChanges).hasSize(2);
  }

  /**
   * Test with advanced strategies flag as true, with dependencies.
   * Looking up versions a1v1, and a1v11 with a1v1 being the current version, test with classifiers.
   * a1v1 dependencies: a2v1, a2v2
   * a1v11 (with classifier) dependencies: a2v11 (with classifier)
   * a1v1 has failing alert, and a1v11 has no alert.
   * a2v1 has failing alert, a2v2 has warning alert, and a2v11 has no alert.
   */
  @Test
  public void testAdvanced_WithClassifier() {
    PackageUrlIdentifier purlA1V11WithClassifier = PackageUrlIdentifier.fromComponentIdentifier(
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v11", "c1", "jar"));
    PackageUrlIdentifier purlA2V11WithClassifier = PackageUrlIdentifier.fromComponentIdentifier(
        ComponentIdentifier.createMavenCoordinates("g1", "a2", "v11", "c1", "jar"));

    Map<PackageUrlIdentifier, Collection<PackageUrlIdentifier>> dependenciesMap = new HashMap<>();
    Map<PackageUrlIdentifier, ComponentDetails> detailsMap = new HashMap<>();

    dependenciesMap.put(purlA1V1, Arrays.asList(purlA2V1, purlA2V2));
    dependenciesMap.put(purlA1V11WithClassifier, Collections.singletonList(purlA2V11WithClassifier));

    detailsMap.put(purlA2V1,  detailsA2V1);
    detailsMap.put(purlA2V2,  detailsA2V2);
    detailsMap.put(purlA2V11WithClassifier, detailsA2V11);

    ComponentDependenciesDTO returnDto = new ComponentDependenciesDTO(dependenciesMap, detailsMap);
    List<ComponentDetailsDTO> allVersions = Arrays.asList(detailsDtoA1V1, detailsDtoA1V11);
    mockHdsGetComponentDependencies(returnDto);
    ApiComponentRemediationValueDTO dto = componentRemediationService.getSuggestedRemediation(MAVEN_COORDINATES_A1_V1,
        allVersions, org.getType(), org.getId(), DevelopStageType.ID, true);

    assertRemediations(dto, buildChangeDto(NEXT_NO_VIOLATIONS, componentDtoA1V11.packageUrl));
    assertRemediations(dto, buildChangeDto(NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES, componentDtoA1V11.packageUrl));
    assertRemediations(dto, buildChangeDto(NEXT_NON_FAILING, componentDtoA1V11.packageUrl));
    assertRemediations(dto, buildChangeDto(NEXT_NON_FAILING_WITH_DEPENDENCIES, componentDtoA1V11.packageUrl));
    assertThat(dto.versionChanges).hasSize(4);
  }

  /**
   * Test with advanced strategies flag as true, with dependencies.
   * Looking up versions a1v1, a1v3, and a1v11 with a1v1 being the current version, test with classifiers.
   * a1v1 dependencies: a2v1, a2v2
   * a1v11 (null classifier) dependencies: a2v11 (null classifier)
   * a1v3 dependencies: a2v3
   * a1v1 has failing alert, a1v3 has no alert, and a1v11 has no alert.
   * a2v1 has failing alert, a2v2 has warning alert, a2v3 has no alert, and a2v11 has no alert.
   */
  @Test
  public void testAdvanced_NullClassifier() {
    PackageUrlIdentifier purlA1V11NullClassifier = PackageUrlIdentifier.fromComponentIdentifier(
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v11", null, "jar"));
    PackageUrlIdentifier purlA2V11NullClassifier = PackageUrlIdentifier.fromComponentIdentifier(
        ComponentIdentifier.createMavenCoordinates("g1", "a2", "v11", null, "jar"));

    Map<PackageUrlIdentifier, Collection<PackageUrlIdentifier>> dependenciesMap = new HashMap<>();
    Map<PackageUrlIdentifier, ComponentDetails> detailsMap = new HashMap<>();

    dependenciesMap.put(purlA1V1, Arrays.asList(purlA2V2, purlA2V1));
    dependenciesMap.put(purlA1V11NullClassifier, Collections.singletonList(purlA2V11NullClassifier));
    dependenciesMap.put(purlA1V3, Collections.singletonList(purlA2V3));

    detailsMap.put(purlA2V1,  detailsA2V1);
    detailsMap.put(purlA2V2,  detailsA2V2);
    detailsMap.put(purlA2V11NullClassifier, detailsA2V11);
    detailsMap.put(purlA2V3,  detailsA2V3);

    ComponentDependenciesDTO returnDto = new ComponentDependenciesDTO(dependenciesMap, detailsMap);
    List<ComponentDetailsDTO> allVersions = Arrays.asList(detailsDtoA1V1, detailsDtoA1V11, detailsDtoA1V3);
    mockHdsGetComponentDependencies(returnDto);
    ApiComponentRemediationValueDTO dto = componentRemediationService.getSuggestedRemediation(MAVEN_COORDINATES_A1_V1,
        allVersions, org.getType(), org.getId(), DevelopStageType.ID, true);

    assertRemediations(dto, buildChangeDto(NEXT_NO_VIOLATIONS, componentDtoA1V11.packageUrl));
    assertRemediations(dto, buildChangeDto(NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES, componentDtoA1V11.packageUrl));
    assertRemediations(dto, buildChangeDto(NEXT_NON_FAILING, componentDtoA1V11.packageUrl));
    assertRemediations(dto, buildChangeDto(NEXT_NON_FAILING_WITH_DEPENDENCIES, componentDtoA1V11.packageUrl));
    assertThat(dto.versionChanges).hasSize(4);
  }

  private ApiVersionChangeOptionDTO buildChangeDto(ApiVersionChangeOptionType type, String purl) {
    ApiVersionChangeOptionDTO changeDto = new ApiVersionChangeOptionDTO();
    changeDto.setType(type);

    ApiComponentChangeActionDTO data = new ApiComponentChangeActionDTO();
    ApiComponentDTOV2 component = new ApiComponentDTOV2();
    component.packageUrl = purl;
    data.setComponent(component);
    changeDto.setData(data);
    return changeDto;
  }

  private void assertRemediations(ApiComponentRemediationValueDTO remediationDto, ApiVersionChangeOptionDTO expected) {
    assertThat(remediationDto).isNotNull();
    assertThat(remediationDto.versionChanges).isNotNull();
    if (expected != null && expected.getType() != null) {
      boolean found = false;
      for (ApiVersionChangeOptionDTO dto : remediationDto.versionChanges) {
        if (dto.getType().equals(expected.getType())) {
          assertThat(dto.getData().getComponent().packageUrl).isEqualTo(expected.getData().getComponent().packageUrl);
          found = true;
          break;
        }
      }
      // we expected to find that the remediationDto purl matches with expected purl, but did not find one.
      assertTrue(expected.getType() + " does not exist in remediation result!", found);
    }
  }
}
