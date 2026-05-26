/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationValueDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.actions.ApiComponentChangeActionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiSuggestedVersionChangeOptionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.innersource.InnerSourceApplication;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.conditions.CoordinatesConditionType;
import com.sonatype.insight.brain.model.policy.conditions.DependencyTypeConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.report.InnerSourceUtils;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.NonBreakingRecommendationTelemetryStats.SourceEndpoint;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.dependency.ComponentDependenciesDTO;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import jakarta.inject.Inject;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class ComponentRemediationServiceTest
    extends AbstractComponentTest
{
  private static final String OWNER_TYPE_TELEMETRY_ATTR = "owner_type";

  private static final String OWNER_ID_TELEMETRY_ATTR = "owner_id";

  private static final String REAL_OWNER_ID_TELEMETRY_ATTR = "real_owner_id";

  @Inject
  private PolicyDAO policyDAO;

  @Mock
  private HdsClient hdsClientMock;

  @Mock
  private ProductLicense productLicense;

  @Mock
  private VersionScoringService versionScoringServiceMock;

  @Inject
  private ComponentRemediationService componentRemediationService;

  @Inject
  private ComponentDetailsLoaderFactory componentDetailsLoaderFactory;

  @Mock
  private TelemetrySender mockTelemetrySender;

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

  private static final PackageUrlIdentifier purlA1V11 = PackageUrlIdentifier.fromComponentIdentifier(
      MAVEN_COORDINATES_A1_V11);

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

  private static final int BREAKING_CHANGES_1 = getRandomRangedInt();

  private static final int BREAKING_CHANGES_2 = getRandomRangedInt();

  private static final int BREAKING_CHANGES_3 = getRandomRangedInt();

  private static final int BREAKING_CHANGES_4 = getRandomRangedInt();

  private static final int BREAKING_CHANGES_5 = getRandomRangedInt();

  private static final int BREAKING_CHANGES_6 = getRandomRangedInt();

  private static final int BREAKING_CHANGES_11 = getRandomRangedInt();

  private Organization org;

  private Application app;

  private final ApiComponentDTOV2 componentDtoA1V1 = new ApiComponentDTOV2();

  private final ApiComponentDTOV2 componentDtoA1V2 = new ApiComponentDTOV2();

  private final ApiComponentDTOV2 componentDtoA1V3 = new ApiComponentDTOV2();

  private final ApiComponentDTOV2 componentDtoA1V4 = new ApiComponentDTOV2();

  private final ApiComponentDTOV2 componentDtoA1V5 = new ApiComponentDTOV2();

  private final ApiComponentDTOV2 componentDtoA1V6 = new ApiComponentDTOV2();

  private final ApiComponentDTOV2 componentDtoA1V11 = new ApiComponentDTOV2();

  private final ApiComponentDTOV2 componentDtoA2V1 = new ApiComponentDTOV2();

  private final ComponentDetailsDTO detailsDtoA1V1 = new ComponentDetailsDTO();

  private final ComponentDetailsDTO detailsDtoA1V2 = new ComponentDetailsDTO();

  private final ComponentDetailsDTO detailsDtoA1V3 = new ComponentDetailsDTO();

  private final ComponentDetailsDTO detailsDtoA1V4 = new ComponentDetailsDTO();

  private final ComponentDetailsDTO detailsDtoA1V5 = new ComponentDetailsDTO();

  private final ComponentDetailsDTO detailsDtoA1V6 = new ComponentDetailsDTO();

  private final ComponentDetailsDTO detailsDtoA1V11 = new ComponentDetailsDTO();

  private final ComponentDetailsDTO detailsDtoA2V1 = new ComponentDetailsDTO();

  private ComponentDetails detailsA2V1 = new ComponentDetails();

  private ComponentDetails detailsA2V2 = new ComponentDetails();

  private ComponentDetails detailsA2V3 = new ComponentDetails();

  private ComponentDetails detailsA2V4 = new ComponentDetails();

  private ComponentDetails detailsA2V5 = new ComponentDetails();

  private ComponentDetails detailsA2V11 = new ComponentDetails();

  private Policy policyG1A2V1;

  @Before
  public void before() {
    org = tempEntity.newOrganization();
    app = tempEntity.newApplication("app", org.getId());

    componentDtoA1V1.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(MAVEN_COORDINATES_A1_V1);
    componentDtoA1V1.packageUrl = "pkg:maven/g1/a1@v1?type=jar";
    componentDtoA1V1.breakingChangesCount = BREAKING_CHANGES_1;
    componentDtoA1V2.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(MAVEN_COORDINATES_A1_V2);
    componentDtoA1V2.packageUrl = "pkg:maven/g1/a1@v2?type=jar";
    componentDtoA1V2.breakingChangesCount = BREAKING_CHANGES_2;
    componentDtoA1V3.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(MAVEN_COORDINATES_A1_V3);
    componentDtoA1V3.packageUrl = "pkg:maven/g1/a1@v3?type=jar";
    componentDtoA1V3.breakingChangesCount = BREAKING_CHANGES_3;
    componentDtoA1V4.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(MAVEN_COORDINATES_A1_V4);
    componentDtoA1V4.packageUrl = "pkg:maven/g1/a1@v4?type=jar";
    componentDtoA1V4.breakingChangesCount = BREAKING_CHANGES_4;
    componentDtoA1V5.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(MAVEN_COORDINATES_A1_V5);
    componentDtoA1V5.packageUrl = "pkg:maven/g1/a1@v5?type=jar";
    componentDtoA1V5.breakingChangesCount = BREAKING_CHANGES_5;
    componentDtoA1V6.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(MAVEN_COORDINATES_A1_V6);
    componentDtoA1V6.packageUrl = "pkg:maven/g1/a1@v6?type=jar";
    componentDtoA1V6.breakingChangesCount = BREAKING_CHANGES_6;
    componentDtoA1V11.componentIdentifier =
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(MAVEN_COORDINATES_A1_V11);
    componentDtoA1V11.packageUrl = "pkg:maven/g1/a1@v11?type=jar";
    componentDtoA1V11.breakingChangesCount = BREAKING_CHANGES_11;
    componentDtoA2V1.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(MAVEN_COORDINATES_A2_V1);
    componentDtoA2V1.packageUrl = "pkg:maven/g1/a2@v1?type=jar";
    componentDtoA2V1.breakingChangesCount = 0;

    // A1
    detailsDtoA1V1.componentIdentifier = MAVEN_COORDINATES_A1_V1;
    detailsDtoA1V1.violatedPolicyCount = 2;
    detailsDtoA1V1.policyAlerts = Arrays.asList(warnAlert, failAlert);
    detailsDtoA1V1.breakingChangesCount = BREAKING_CHANGES_1;
    detailsDtoA1V1.policyMaxThreatLevelsByCategory = Map.of(
        PolicyThreatCategory.SECURITY, 3,
        PolicyThreatCategory.LICENSE, 4);

    detailsDtoA1V2.componentIdentifier = MAVEN_COORDINATES_A1_V2;
    detailsDtoA1V2.violatedPolicyCount = 1;
    detailsDtoA1V2.policyAlerts = Collections.singletonList(warnAlert);
    detailsDtoA1V2.breakingChangesCount = BREAKING_CHANGES_2;
    detailsDtoA1V2.policyMaxThreatLevelsByCategory = Map.of(
        PolicyThreatCategory.QUALITY, 5);

    detailsDtoA1V3.componentIdentifier = MAVEN_COORDINATES_A1_V3;
    detailsDtoA1V3.violatedPolicyCount = 0;
    detailsDtoA1V3.breakingChangesCount = BREAKING_CHANGES_3;

    detailsDtoA1V4.componentIdentifier = MAVEN_COORDINATES_A1_V4;
    detailsDtoA1V4.violatedPolicyCount = 0;
    detailsDtoA1V4.breakingChangesCount = BREAKING_CHANGES_4;

    detailsDtoA1V5.componentIdentifier = MAVEN_COORDINATES_A1_V5;
    detailsDtoA1V5.violatedPolicyCount = 1;
    detailsDtoA1V5.policyAlerts = Collections.singletonList(warnAlert);
    detailsDtoA1V5.breakingChangesCount = BREAKING_CHANGES_5;
    detailsDtoA1V5.policyMaxThreatLevelsByCategory = Map.of(
        PolicyThreatCategory.OTHER, 6);

    detailsDtoA1V6.componentIdentifier = MAVEN_COORDINATES_A1_V6;
    detailsDtoA1V6.violatedPolicyCount = 1;
    detailsDtoA1V6.policyAlerts = Collections.singletonList(failAlert);
    detailsDtoA1V6.breakingChangesCount = BREAKING_CHANGES_6;
    detailsDtoA1V6.policyMaxThreatLevelsByCategory = Map.of(
        PolicyThreatCategory.OTHER, 6);

    detailsDtoA1V11.componentIdentifier = MAVEN_COORDINATES_A1_V11;
    detailsDtoA1V11.violatedPolicyCount = 0;
    detailsDtoA1V11.breakingChangesCount = BREAKING_CHANGES_11;

    detailsDtoA2V1.componentIdentifier = MAVEN_COORDINATES_A2_V1;
    detailsDtoA2V1.violatedPolicyCount = 0;
    detailsDtoA2V1.breakingChangesCount = 0;

    // A2
    detailsA2V1 = buildComponentDetails(MAVEN_COORDINATES_A2_V1, Arrays.asList(warnAlert, failAlert));
    detailsA2V2 = buildComponentDetails(MAVEN_COORDINATES_A2_V2, Collections.singletonList(warnAlert));
    detailsA2V3 = buildComponentDetails(MAVEN_COORDINATES_A2_V3, null);
    detailsA2V4 = buildComponentDetails(MAVEN_COORDINATES_A2_V4, null);
    detailsA2V5 = buildComponentDetails(MAVEN_COORDINATES_A2_V5, Collections.singletonList(warnAlert));
    detailsA2V11 = buildComponentDetails(MAVEN_COORDINATES_A2_V11, Collections.emptyList());

    detailsA2V1.setPolicyMaxThreatLevelsByCategory(Map.of(
        PolicyThreatCategory.SECURITY.getName(), 3,
        PolicyThreatCategory.LICENSE.getName(), 4));
    detailsA2V2.setPolicyMaxThreatLevelsByCategory(Map.of(
        PolicyThreatCategory.QUALITY.getName(), 5));
    detailsA2V5.setPolicyMaxThreatLevelsByCategory(Map.of(
        PolicyThreatCategory.OTHER.getName(), 6));

    policyG1A2V1 = new Policy("policyG1A2V1", "policyG1A2V1");
    policyG1A2V1.setOwnerId(org.getParentOwnerId());
    policyG1A2V1.setThreatLevel(10);
    Constraint constraint = new Constraint(null, "Test Constraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(CoordinatesConditionType.ID, "match", "maven:g1:a2:v1"));
    policyG1A2V1.addConstraint(constraint);
    policyG1A2V1.setAction(DevelopStageType.ID, Action.ID_FAIL);
    policyG1A2V1.setAction(BuildStageType.ID, Action.ID_WARN);
    tempEntity.newPolicy(policyG1A2V1);

    Policy policy = new Policy("policyG1A2V2", "policyG1A2V2");
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

    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.TRANSITIVE_SOLVER_ENABLED, "false");
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

  private void mockLicenseFeature(boolean includeAdvancedStrategies) {
    lenient().when(productLicense.hasFeature(eq(LicensedFeature.ADVANCED_RECOMMENDATION_STRATEGIES)))
        .thenReturn(includeAdvancedStrategies);
  }

  private void enableTransitiveSolver() {
    setTransitiveSolverValue(true);
  }

  private void setTransitiveSolverValue(boolean turnOffTransitiveSolver) {
    boolean status = SystemConfigurationPropertyFeature.TRANSITIVE_SOLVER.isEnabled();
    if (turnOffTransitiveSolver && !status) {
      SystemConfigurationPropertyFeature.TRANSITIVE_SOLVER.setEnabled(true);
    }
    if (!turnOffTransitiveSolver && status) {
      SystemConfigurationPropertyFeature.TRANSITIVE_SOLVER.setEnabled(false);
    }
  }

  /*
   * --- Advanced strategies = false ---
   */

  /**
   * Test with advanced strategies flag as false to verify we are not getting back "with dependencies" remedies. Looking
   * up version a1v1, a1v2, and a1v3 with a1v1 being the current version. None of the versions have dependencies. a1v1
   * has failing alert, a1v2 has warning alert, and a1v3 has no alerts.
   */
  @Test
  public void testGetSuggestedRemediation_Advanced_NoDependencies_NoTransitive() {
    testGetSuggestedRemediation_NoDependencies(true, false);
  }

  @Test
  public void testGetSuggestedRemediation_NoAdvanced_NoDependencies_TransitiveEnabled() {
    testGetSuggestedRemediation_NoDependencies(false, true);
  }

  @Test
  public void testGetSuggestedRemediation_NoAdvanced_NoDependencies_TransitiveEnabled_IEREnabled() {
    testGetSuggestedRemediation_NoDependencies(false, true);
  }

  @Test
  public void testGetSuggestedRemediation_NoAdvanced_NoDependencies_NoTransitive() {
    testGetSuggestedRemediation_NoDependencies(false, false);
  }

  public void testGetSuggestedRemediation_NoDependencies(boolean advanced, boolean enableTransitiveSolver) {
    setTransitiveSolverValue(enableTransitiveSolver);
    ComponentDependenciesDTO returnDto = new ComponentDependenciesDTO(new HashMap<>(), new HashMap<>());
    List<ComponentDetailsDTO> allVersions = Arrays.asList(detailsDtoA1V1, detailsDtoA1V2, detailsDtoA1V3);
    mockHdsGetComponentDependencies(returnDto);
    mockLicenseFeature(advanced);
    ApiComponentRemediationValueDTO dto = componentRemediationService.getSuggestedRemediation(MAVEN_COORDINATES_A1_V1,
        allVersions, org, DevelopStageType.ID, componentDetailsLoaderFactory.newInstance(org),
        SourceEndpoint.API_COMPONENT_REMEDIATION);

    assertRemediations(dto, buildChangeDto(
        ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS, componentDtoA1V3));
    assertRemediations(dto, buildChangeDto(
        ApiVersionChangeOptionType.NEXT_NON_FAILING, componentDtoA1V2));
    assertThat(dto.versionChanges).hasSize(2);

    assertTelemetryData(org);
  }

  /**
   * Test with advanced strategies flag as false but also with dependencies included to verify we are not getting back
   * "with dependencies" remedies. Looking up versions a1v1, a1v2, a1v3, and a1v4 with a1v2 being the current version.
   * a1v1 has no dependencies a1v2 dependencies: a2v1, a2v2 a1v3 dependencies: a2v3, a2v4 a1v4 dependencies: a2v3, a2v4
   * a1v1 has failing alert, a1v2 has warning alert, a1v3 has no alerts, and a1v4 has no alerts. a2v1 has failing alert,
   * a2v2 has warning alert, a2v3 has no alerts, and a2v4 has no alerts.
   */
  @Test
  public void testGetSuggestedRemediation_Advanced_WithDependencies_NoTransitive() {
    testGetSuggestedRemediation_WithDependencies(true, false);
  }

  @Test
  public void testGetSuggestedRemediation_NoAdvanced_WithDependencies_TransitiveEnabled() {
    testGetSuggestedRemediation_WithDependencies(false, false);
  }

  @Test
  public void testGetSuggestedRemediation_NoAdvanced_WithDependencies_NoTransitive() {
    testGetSuggestedRemediation_WithDependencies(false, true);
  }

  public void testGetSuggestedRemediation_WithDependencies(boolean advanced, boolean transitiveSolverEnabled) {
    setTransitiveSolverValue(transitiveSolverEnabled);
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
    mockLicenseFeature(advanced);
    ApiComponentRemediationValueDTO dto = componentRemediationService.getSuggestedRemediation(MAVEN_COORDINATES_A1_V2,
        allVersions, org, DevelopStageType.ID, componentDetailsLoaderFactory.newInstance(org),
        SourceEndpoint.API_COMPONENT_REMEDIATION);

    assertRemediations(dto, buildChangeDto(
        ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS, componentDtoA1V3));
    assertRemediations(dto, buildChangeDto(
        ApiVersionChangeOptionType.NEXT_NON_FAILING, componentDtoA1V2));
    assertThat(dto.versionChanges).hasSize(2);
  }

  /*
   * --- Advanced strategies = true ---
   */

  /**
   * Test with advanced strategies flag as true with no dependencies. Looking up versions a1v1, a1v2, and a1v3 with a1v1
   * being the current version. None of the versions have dependencies. a1v1 has failing alert, a1v2 has warning alert,
   * and a1v3 has no alerts.
   */
  @Test
  public void testGetSuggestedRemediation_Advanced_NoDependencies() {
    enableTransitiveSolver();
    ComponentDependenciesDTO returnDto = new ComponentDependenciesDTO(new HashMap<>(), new HashMap<>());
    List<ComponentDetailsDTO> allVersions = Arrays.asList(detailsDtoA1V1, detailsDtoA1V2, detailsDtoA1V3);
    mockHdsGetComponentDependencies(returnDto);
    mockLicenseFeature(true);
    ApiComponentRemediationValueDTO dto = componentRemediationService.getSuggestedRemediation(MAVEN_COORDINATES_A1_V1,
        allVersions, org, DevelopStageType.ID, componentDetailsLoaderFactory.newInstance(org),
        SourceEndpoint.API_COMPONENT_REMEDIATION);

    assertRemediations(dto, buildChangeDto(
        ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES, componentDtoA1V3));
    assertRemediations(dto, buildChangeDto(
        ApiVersionChangeOptionType.NEXT_NON_FAILING_WITH_DEPENDENCIES, componentDtoA1V2));
    assertThat(dto.versionChanges).hasSize(2);
  }

  /**
   * Test with advanced strategies flag as true, with dependencies. Looking up versions a1v1, a1v2, a1v3, and a1v4 with
   * a1v1 being the current version. all versions have no dependencies a1v1 has failing alert, a1v2 has warning alert,
   * a1v3 has no alerts, and a1v4 has no alerts. a2v1 has failing alert, a2v2 has warning alert, a2v3 has no alerts, and
   * a2v4 has no alerts.
   */
  @Test
  public void testGetSuggestedRemediation_Advanced_EmptyDependencies() {
    enableTransitiveSolver();
    Map<PackageUrlIdentifier, Collection<PackageUrlIdentifier>> dependenciesMap = new HashMap<>();

    dependenciesMap.put(purlA1V1, Collections.emptyList());
    dependenciesMap.put(purlA1V2, Collections.emptyList());
    dependenciesMap.put(purlA1V3, Collections.emptyList());
    dependenciesMap.put(purlA1V4, Collections.emptyList());

    ComponentDependenciesDTO returnDto = new ComponentDependenciesDTO(dependenciesMap, new HashMap<>());
    List<ComponentDetailsDTO> allVersions = Arrays.asList(detailsDtoA1V1, detailsDtoA1V2, detailsDtoA1V3,
        detailsDtoA1V4);
    mockHdsGetComponentDependencies(returnDto);
    mockLicenseFeature(true);
    ApiComponentRemediationValueDTO dto = componentRemediationService.getSuggestedRemediation(MAVEN_COORDINATES_A1_V1,
        allVersions, org, DevelopStageType.ID, componentDetailsLoaderFactory.newInstance(org),
        SourceEndpoint.API_COMPONENT_REMEDIATION);

    assertRemediations(dto, buildChangeDto(
        ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES, componentDtoA1V3));
    assertRemediations(dto, buildChangeDto(
        ApiVersionChangeOptionType.NEXT_NON_FAILING_WITH_DEPENDENCIES, componentDtoA1V2));
    assertThat(dto.versionChanges).hasSize(2);
  }

  /**
   * Test with advanced strategies flag as true, with dependencies. Looking up NuGet versions a1, a2, and a3 with a1
   * being the current version. a1 dependencies: a2 a2 dependencies: a3 a3 dependencies: none a1 has a warn alert
   * (*non-failing), a2 has a warn alert, and a3 has no alerts (*non-violating).
   */
  @Test
  public void testGetSuggestedRemediation_Advanced_NonMaven() {
    enableTransitiveSolver();
    ComponentIdentifier compIdNgA1 = ComponentIdentifier.createNugetCoordinates("a1", "v");
    ComponentIdentifier compIdNgA2 = ComponentIdentifier.createNugetCoordinates("a2", "v");
    ComponentIdentifier compIdNgA3 = ComponentIdentifier.createNugetCoordinates("a3", "v");
    PackageUrlIdentifier purlNgA1 = PackageUrlIdentifier.fromComponentIdentifier(compIdNgA1);
    PackageUrlIdentifier purlNgA2 = PackageUrlIdentifier.fromComponentIdentifier(compIdNgA2);
    PackageUrlIdentifier purlNgA3 = PackageUrlIdentifier.fromComponentIdentifier(compIdNgA3);

    Map<PackageUrlIdentifier, Collection<PackageUrlIdentifier>> dependenciesMap = new HashMap<>();
    Map<PackageUrlIdentifier, ComponentDetails> detailsMap = new HashMap<>();
    dependenciesMap.put(purlNgA1, Collections.singletonList(purlNgA2));
    dependenciesMap.put(purlNgA2, Collections.singletonList(purlNgA3));
    dependenciesMap.put(purlNgA3, Collections.emptyList());

    ComponentDetails detailsNgA2 = buildComponentDetails(compIdNgA2, Collections.singletonList(warnAlert));
    ComponentDetails detailsNgA3 = buildComponentDetails(compIdNgA3, null);
    detailsMap.put(purlNgA2, detailsNgA2);
    detailsMap.put(purlNgA3, detailsNgA3);

    ComponentDependenciesDTO returnDto = new ComponentDependenciesDTO(dependenciesMap, detailsMap);
    ComponentDetailsDTO ngDtoA1 = new ComponentDetailsDTO();
    ngDtoA1.componentIdentifier = compIdNgA1;
    ngDtoA1.violatedPolicyCount = 1;
    ngDtoA1.breakingChangesCount = BREAKING_CHANGES_1;
    ComponentDetailsDTO ngDtoA2 = new ComponentDetailsDTO();
    ngDtoA2.componentIdentifier = compIdNgA2;
    ngDtoA2.violatedPolicyCount = 1;
    ngDtoA2.breakingChangesCount = BREAKING_CHANGES_2;
    ComponentDetailsDTO ngDtoA3 = new ComponentDetailsDTO();
    ngDtoA3.componentIdentifier = compIdNgA3;
    ngDtoA3.violatedPolicyCount = 0;
    ngDtoA3.breakingChangesCount = BREAKING_CHANGES_3;
    List<ComponentDetailsDTO> allVersions = Arrays.asList(ngDtoA1, ngDtoA2, ngDtoA3);

    mockHdsGetComponentDependencies(returnDto);
    mockLicenseFeature(true);
    ApiComponentRemediationValueDTO dto = componentRemediationService.getSuggestedRemediation(compIdNgA1,
        allVersions, org, DevelopStageType.ID, componentDetailsLoaderFactory.newInstance(org),
        SourceEndpoint.API_COMPONENT_REMEDIATION);

    ApiComponentDTOV2 ngApiDtoA1 = new ApiComponentDTOV2();
    ngApiDtoA1.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(compIdNgA1);
    ngApiDtoA1.packageUrl = "pkg:nuget/a1@v";
    ngApiDtoA1.breakingChangesCount = BREAKING_CHANGES_1;
    ApiComponentDTOV2 ngApiDtoA3 = new ApiComponentDTOV2();
    ngApiDtoA3.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(compIdNgA3);
    ngApiDtoA3.packageUrl = "pkg:nuget/a3@v";
    ngApiDtoA3.breakingChangesCount = BREAKING_CHANGES_3;

    assertRemediations(dto, buildChangeDto(
        ApiVersionChangeOptionType.NEXT_NON_FAILING, ngApiDtoA1));
    assertRemediations(dto, buildChangeDto(
        ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS, ngApiDtoA3));
    assertThat(dto.versionChanges).hasSize(2);
  }

  /**
   * Test with advanced strategies flag as true, with dependencies, no stage information. Looking up versions a1v1,
   * a1v2, a1v3, and a1v4 with a1v1 being the current version. a1v1 has no dependencies a1v2 dependencies: a2v1, a2v2
   * a1v3 dependencies: a2v2, a2v3 a1v4 dependencies: a2v3, a2v4 a1v1 has failing alert, a1v2 has warning alert, a1v3
   * has no alerts, and a1v4 has no alerts. a2v1 has failing alert, a2v2 has warning alert, a2v3 has no alerts, and a2v4
   * has no alerts.
   */
  @Test
  public void testGetSuggestedRemediation_Advanced_NoStage() {
    enableTransitiveSolver();
    Map<PackageUrlIdentifier, Collection<PackageUrlIdentifier>> dependenciesMap = new HashMap<>();
    Map<PackageUrlIdentifier, ComponentDetails> detailsMap = new HashMap<>();

    dependenciesMap.put(purlA1V1, Collections.emptyList());
    dependenciesMap.put(purlA1V2, Arrays.asList(purlA2V1, purlA2V2));
    dependenciesMap.put(purlA1V3, Arrays.asList(purlA2V2, purlA2V3));
    dependenciesMap.put(purlA1V4, Arrays.asList(purlA2V3, purlA2V4));

    detailsMap.put(purlA2V1, detailsA2V1);
    detailsMap.put(purlA2V2, detailsA2V2);
    detailsMap.put(purlA2V3, detailsA2V3);
    detailsMap.put(purlA2V4, detailsA2V4);

    ComponentDependenciesDTO returnDto = new ComponentDependenciesDTO(dependenciesMap, detailsMap);
    List<ComponentDetailsDTO> allVersions = Arrays.asList(detailsDtoA1V1, detailsDtoA1V2, detailsDtoA1V3,
        detailsDtoA1V4);
    mockHdsGetComponentDependencies(returnDto);
    mockLicenseFeature(true);
    ApiComponentRemediationValueDTO dto = componentRemediationService.getSuggestedRemediation(MAVEN_COORDINATES_A1_V1,
        allVersions, org, null, componentDetailsLoaderFactory.newInstance(org),
        SourceEndpoint.API_COMPONENT_REMEDIATION);

    assertRemediations(dto, buildChangeDto(
        ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS, componentDtoA1V3));
    assertRemediations(dto, buildChangeDto(
        ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES, componentDtoA1V4));
    assertThat(dto.versionChanges).hasSize(2);
  }

  /**
   * Test with advanced strategies flag as true, with dependencies. Looking up versions a1v1, a1v2, a1v3, a1v4 and a1v5
   * with a1v3 being the current version. a1v1 dependencies: a2v3, a2v4 a1v2 dependencies: a2v1, a2v2 a1v3 dependencies:
   * a2v2, a2v3 a1v4 dependencies: a2v4 a1v5 dependencies: a2v5 a1v1 has failing alert, a1v2 has warning alert, a1v3 has
   * no alerts, a1v4 has no alerts, and a1v5 has warning alert. a2v1 has failing alert, a2v2 has warning alert, a2v3 has
   * no alerts, a2v4 has no alerts, and a2v5 has warning alert.
   */
  @Test
  public void testGetSuggestedRemediation_Advanced_CurrentVersionNotFirstInAllVersions() {
    enableTransitiveSolver();
    Map<PackageUrlIdentifier, Collection<PackageUrlIdentifier>> dependenciesMap = new HashMap<>();
    Map<PackageUrlIdentifier, ComponentDetails> detailsMap = new HashMap<>();

    dependenciesMap.put(purlA1V1, Arrays.asList(purlA2V3, purlA2V4));
    dependenciesMap.put(purlA1V2, Arrays.asList(purlA2V1, purlA2V2));
    dependenciesMap.put(purlA1V3, Arrays.asList(purlA2V2, purlA2V3));
    dependenciesMap.put(purlA1V4, Collections.singletonList(purlA2V4));
    dependenciesMap.put(purlA1V5, Collections.singletonList(purlA2V5));

    detailsMap.put(purlA2V1, detailsA2V1);
    detailsMap.put(purlA2V2, detailsA2V2);
    detailsMap.put(purlA2V3, detailsA2V3);
    detailsMap.put(purlA2V4, detailsA2V4);
    detailsMap.put(purlA2V5, detailsA2V5);

    ComponentDependenciesDTO returnDto = new ComponentDependenciesDTO(dependenciesMap, detailsMap);
    List<ComponentDetailsDTO> allVersions = Arrays.asList(detailsDtoA1V1, detailsDtoA1V2, detailsDtoA1V3,
        detailsDtoA1V4, detailsDtoA1V5);
    mockHdsGetComponentDependencies(returnDto);
    mockLicenseFeature(true);
    ApiComponentRemediationValueDTO dto = componentRemediationService.getSuggestedRemediation(MAVEN_COORDINATES_A1_V3,
        allVersions, org, DevelopStageType.ID, componentDetailsLoaderFactory.newInstance(org),
        SourceEndpoint.API_COMPONENT_REMEDIATION);

    assertRemediations(dto, buildChangeDto(
        ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS, componentDtoA1V3));
    assertRemediations(dto, buildChangeDto(
        ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES, componentDtoA1V4));
    assertThat(dto.versionChanges).hasSize(2);
  }

  /**
   * Test with advanced strategies flag as true, with dependencies. Looking up versions a1v1, a1v2, a1v3, a1v4 and a1v5
   * with a1v5 being the current version. a1v1 dependencies: a2v1, a2v2 a1v2 dependencies: none a1v3 dependencies: a2v3
   * a1v4 dependencies: a2v4 a1v5 dependencies: a2v5 a1v1 has failing alert, a1v2 has warning alert, a1v3 has no alerts,
   * a1v4 has no alerts, and a1v5 has warning alert. a2v1 has failing alert, a2v2 has warning alert, a2v3 has no alerts,
   * a2v4 has no alerts, and a2v5 has warning alert.
   */
  @Test
  public void testGetSuggestedRemediation_Advanced_CurrentVersionLastInAllVersions() {
    enableTransitiveSolver();
    Map<PackageUrlIdentifier, Collection<PackageUrlIdentifier>> dependenciesMap = new HashMap<>();
    Map<PackageUrlIdentifier, ComponentDetails> detailsMap = new HashMap<>();

    dependenciesMap.put(purlA1V1, Arrays.asList(purlA2V1, purlA2V2));
    dependenciesMap.put(purlA1V2, Collections.emptyList());
    dependenciesMap.put(purlA1V3, Collections.singletonList(purlA2V3));
    dependenciesMap.put(purlA1V4, Collections.singletonList(purlA2V4));
    dependenciesMap.put(purlA1V5, Collections.singletonList(purlA2V5));

    detailsMap.put(purlA2V1, detailsA2V1);
    detailsMap.put(purlA2V2, detailsA2V2);
    detailsMap.put(purlA2V3, detailsA2V3);
    detailsMap.put(purlA2V4, detailsA2V4);
    detailsMap.put(purlA2V5, detailsA2V5);

    ComponentDependenciesDTO returnDto = new ComponentDependenciesDTO(dependenciesMap, detailsMap);
    List<ComponentDetailsDTO> allVersions = Arrays.asList(detailsDtoA1V1, detailsDtoA1V2, detailsDtoA1V3,
        detailsDtoA1V4, detailsDtoA1V5);
    mockHdsGetComponentDependencies(returnDto);
    mockLicenseFeature(true);
    ApiComponentRemediationValueDTO dto = componentRemediationService.getSuggestedRemediation(MAVEN_COORDINATES_A1_V5,
        allVersions, org, DevelopStageType.ID, componentDetailsLoaderFactory.newInstance(org),
        SourceEndpoint.API_COMPONENT_REMEDIATION);

    assertRemediations(dto, buildChangeDto(
        ApiVersionChangeOptionType.NEXT_NON_FAILING_WITH_DEPENDENCIES, componentDtoA1V5));
    assertThat(dto.versionChanges).hasSize(1);
  }

  /**
   * Test with advanced strategies flag as true, with dependencies. Looking up versions a1v3, and a1v4 with a1v3 being
   * the current version. a1v3 dependencies: a2v3 a1v4 dependencies: none a1v3 has no alerts, and a1v4 has no alerts.
   * a2v3 has no alerts.
   */
  @Test
  public void testGetSuggestedRemediation_Advanced_CurrentVersionNonViolatingWithDependencies() {
    enableTransitiveSolver();
    Map<PackageUrlIdentifier, Collection<PackageUrlIdentifier>> dependenciesMap = new HashMap<>();
    Map<PackageUrlIdentifier, ComponentDetails> detailsMap = new HashMap<>();

    dependenciesMap.put(purlA1V3, Collections.singletonList(purlA2V3));
    dependenciesMap.put(purlA1V4, Collections.emptyList());

    detailsMap.put(purlA2V3, detailsA2V3);

    ComponentDependenciesDTO returnDto = new ComponentDependenciesDTO(dependenciesMap, detailsMap);
    List<ComponentDetailsDTO> allVersions = Arrays.asList(detailsDtoA1V3, detailsDtoA1V4);
    mockHdsGetComponentDependencies(returnDto);
    mockLicenseFeature(true);
    ApiComponentRemediationValueDTO dto = componentRemediationService.getSuggestedRemediation(MAVEN_COORDINATES_A1_V3,
        allVersions, org, DevelopStageType.ID, componentDetailsLoaderFactory.newInstance(org),
        SourceEndpoint.API_COMPONENT_REMEDIATION);

    assertRemediations(dto, buildChangeDto(
        ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES, componentDtoA1V3));
    assertThat(dto.versionChanges).hasSize(1);
  }

  /**
   * Test with advanced strategies flag as true, with dependencies. Looking up versions a1v2 with a1v2 being the current
   * version. a1v2 dependencies: a2v2 a1v2 has warning alert. a2v2 has warning alert.
   */
  @Test
  public void testGetSuggestedRemediation_Advanced_CurrentVersionNonFailingWithDependencies() {
    enableTransitiveSolver();
    Map<PackageUrlIdentifier, Collection<PackageUrlIdentifier>> dependenciesMap = new HashMap<>();
    Map<PackageUrlIdentifier, ComponentDetails> detailsMap = new HashMap<>();

    dependenciesMap.put(purlA1V2, Collections.singletonList(purlA2V2));

    detailsMap.put(purlA2V2, detailsA2V2);

    ComponentDependenciesDTO returnDto = new ComponentDependenciesDTO(dependenciesMap, detailsMap);
    List<ComponentDetailsDTO> allVersions = Collections.singletonList(detailsDtoA1V2);
    mockHdsGetComponentDependencies(returnDto);
    mockLicenseFeature(true);
    ApiComponentRemediationValueDTO dto = componentRemediationService.getSuggestedRemediation(MAVEN_COORDINATES_A1_V2,
        allVersions, org, DevelopStageType.ID, componentDetailsLoaderFactory.newInstance(org),
        SourceEndpoint.API_COMPONENT_REMEDIATION);

    assertRemediations(dto, buildChangeDto(
        ApiVersionChangeOptionType.NEXT_NON_FAILING_WITH_DEPENDENCIES, componentDtoA1V2));
    assertThat(dto.versionChanges).hasSize(1);
  }

  /**
   * Test with advanced strategies flag as true, with dependencies. Looking up versions a1v1, a1v2 and a1v5 with a1v1
   * being the current version. a1v1 dependencies: a2v2 a1v2 dependencies: a2v1 a1v5 dependencies: a2v2, a2v5 a1v1 has
   * failing alert, a1v2 has warning alert, and a1v5 has warning alert. a2v1 has failing alert, a2v2 has warning alert,
   * and a2v5 has warning alert.
   */
  @Test
  public void testGetSuggestedRemediation_Advanced_AllViolating() {
    enableTransitiveSolver();
    Map<PackageUrlIdentifier, Collection<PackageUrlIdentifier>> dependenciesMap = new HashMap<>();
    Map<PackageUrlIdentifier, ComponentDetails> detailsMap = new HashMap<>();

    dependenciesMap.put(purlA1V1, Collections.singletonList(purlA2V2));
    dependenciesMap.put(purlA1V2, Collections.singletonList(purlA2V1));
    dependenciesMap.put(purlA1V5, Arrays.asList(purlA2V2, purlA2V5));

    detailsMap.put(purlA2V1, detailsA2V1);
    detailsMap.put(purlA2V2, detailsA2V2);
    detailsMap.put(purlA2V5, detailsA2V5);

    ComponentDependenciesDTO returnDto = new ComponentDependenciesDTO(dependenciesMap, detailsMap);
    List<ComponentDetailsDTO> allVersions = Arrays.asList(detailsDtoA1V1, detailsDtoA1V2, detailsDtoA1V5);
    mockHdsGetComponentDependencies(returnDto);
    mockLicenseFeature(true);
    ApiComponentRemediationValueDTO dto = componentRemediationService.getSuggestedRemediation(MAVEN_COORDINATES_A1_V1,
        allVersions, org, DevelopStageType.ID, componentDetailsLoaderFactory.newInstance(org),
        SourceEndpoint.API_COMPONENT_REMEDIATION);

    assertRemediations(dto, buildChangeDto(
        ApiVersionChangeOptionType.NEXT_NON_FAILING, componentDtoA1V2));
    assertRemediations(dto, buildChangeDto(
        ApiVersionChangeOptionType.NEXT_NON_FAILING_WITH_DEPENDENCIES, componentDtoA1V5));
    assertThat(dto.versionChanges).hasSize(2);
  }

  /**
   * Test with advanced strategies flag as true, with dependencies. Looking up versions a1v1, and a1v6 with a1v1 being
   * the current version. a1v1 dependencies: a2v3 a1v6 dependencies: none a1v1 has failing alert, and a1v6 has failing
   * alert. a2v3 has no alerts.
   */
  @Test
  public void testGetSuggestedRemediation_Advanced_AllFailing() {
    enableTransitiveSolver();
    Map<PackageUrlIdentifier, Collection<PackageUrlIdentifier>> dependenciesMap = new HashMap<>();
    Map<PackageUrlIdentifier, ComponentDetails> detailsMap = new HashMap<>();

    dependenciesMap.put(purlA1V1, Collections.singletonList(purlA2V3));
    dependenciesMap.put(purlA1V6, Collections.emptyList());

    detailsMap.put(purlA2V3, detailsA2V3);

    ComponentDependenciesDTO returnDto = new ComponentDependenciesDTO(dependenciesMap, detailsMap);
    List<ComponentDetailsDTO> allVersions = Arrays.asList(detailsDtoA1V1, detailsDtoA1V6);
    mockHdsGetComponentDependencies(returnDto);
    mockLicenseFeature(true);
    ApiComponentRemediationValueDTO dto = componentRemediationService.getSuggestedRemediation(MAVEN_COORDINATES_A1_V1,
        allVersions, org, DevelopStageType.ID, componentDetailsLoaderFactory.newInstance(org),
        SourceEndpoint.API_COMPONENT_REMEDIATION);

    assertRemediations(dto, null);
    assertThat(dto.versionChanges).isEmpty();
  }

  /**
   * Test with advanced strategies flag as true, with dependencies. Looking up single version a1v3 with a1v3 being the
   * current version. a1v3 dependencies: a2v1 a1v3 has no alerts, and a2v1 has failing alert.
   */
  @Test
  public void testGetSuggestedRemediation_Advanced_SingleVersion_DependenciesFailing() {
    enableTransitiveSolver();
    Map<PackageUrlIdentifier, Collection<PackageUrlIdentifier>> dependenciesMap = new HashMap<>();
    Map<PackageUrlIdentifier, ComponentDetails> detailsMap = new HashMap<>();

    dependenciesMap.put(purlA1V3, Collections.singletonList(purlA2V1));

    detailsMap.put(purlA2V1, detailsA2V1);

    ComponentDependenciesDTO returnDto = new ComponentDependenciesDTO(dependenciesMap, detailsMap);
    List<ComponentDetailsDTO> allVersions = Collections.singletonList(detailsDtoA1V3);
    mockHdsGetComponentDependencies(returnDto);
    mockLicenseFeature(true);
    ApiComponentRemediationValueDTO dto = componentRemediationService.getSuggestedRemediation(MAVEN_COORDINATES_A1_V3,
        allVersions, org, DevelopStageType.ID, componentDetailsLoaderFactory.newInstance(org),
        SourceEndpoint.API_COMPONENT_REMEDIATION);

    assertRemediations(dto, buildChangeDto(
        ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS, componentDtoA1V3));
    assertThat(dto.versionChanges).hasSize(1);
  }

  /**
   * Test with advanced strategies flag as true, with dependencies. Looking up versions a1v1, and a1v11 with a1v1 being
   * the current version, test with classifiers. a1v1 dependencies: a2v1, a2v2 a1v11 (with classifier) dependencies:
   * a2v11 (with classifier) a1v1 has failing alert, and a1v11 has no alert. a2v1 has failing alert, a2v2 has warning
   * alert, and a2v11 has no alert.
   */
  @Test
  public void testGetSuggestedRemediation_Advanced_WithClassifier() {
    enableTransitiveSolver();
    PackageUrlIdentifier purlA1V11WithClassifier = PackageUrlIdentifier.fromComponentIdentifier(
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v11", "c1", "jar"));
    PackageUrlIdentifier purlA2V11WithClassifier = PackageUrlIdentifier.fromComponentIdentifier(
        ComponentIdentifier.createMavenCoordinates("g1", "a2", "v11", "c1", "jar"));

    Map<PackageUrlIdentifier, Collection<PackageUrlIdentifier>> dependenciesMap = new HashMap<>();
    Map<PackageUrlIdentifier, ComponentDetails> detailsMap = new HashMap<>();

    dependenciesMap.put(purlA1V1, Arrays.asList(purlA2V1, purlA2V2));
    dependenciesMap.put(purlA1V11WithClassifier, Collections.singletonList(purlA2V11WithClassifier));

    detailsMap.put(purlA2V1, detailsA2V1);
    detailsMap.put(purlA2V2, detailsA2V2);
    detailsMap.put(purlA2V11WithClassifier, detailsA2V11);

    ComponentDependenciesDTO returnDto = new ComponentDependenciesDTO(dependenciesMap, detailsMap);
    List<ComponentDetailsDTO> allVersions = Arrays.asList(detailsDtoA1V1, detailsDtoA1V11);
    mockHdsGetComponentDependencies(returnDto);
    mockLicenseFeature(true);
    ApiComponentRemediationValueDTO dto = componentRemediationService.getSuggestedRemediation(MAVEN_COORDINATES_A1_V1,
        allVersions, org, DevelopStageType.ID, componentDetailsLoaderFactory.newInstance(org),
        SourceEndpoint.API_COMPONENT_REMEDIATION);

    assertRemediations(dto, buildChangeDto(
        ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES, componentDtoA1V11));
    assertThat(dto.versionChanges).hasSize(1);
  }

  /**
   * Test with advanced strategies flag as true, with dependencies. Looking up versions a1v1, a1v3, and a1v11 with a1v1
   * being the current version, test with classifiers. a1v1 dependencies: a2v1, a2v2 a1v11 (null classifier)
   * dependencies: a2v11 (null classifier) a1v3 dependencies: a2v3 a1v1 has failing alert, a1v3 has no alert, and a1v11
   * has no alert. a2v1 has failing alert, a2v2 has warning alert, a2v3 has no alert, and a2v11 has no alert.
   */
  @Test
  public void testGetSuggestedRemediation_Advanced_NullClassifier() {
    enableTransitiveSolver();
    PackageUrlIdentifier purlA1V11NullClassifier = PackageUrlIdentifier.fromComponentIdentifier(
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v11", null, "jar"));
    PackageUrlIdentifier purlA2V11NullClassifier = PackageUrlIdentifier.fromComponentIdentifier(
        ComponentIdentifier.createMavenCoordinates("g1", "a2", "v11", null, "jar"));

    Map<PackageUrlIdentifier, Collection<PackageUrlIdentifier>> dependenciesMap = new HashMap<>();
    Map<PackageUrlIdentifier, ComponentDetails> detailsMap = new HashMap<>();

    dependenciesMap.put(purlA1V1, Arrays.asList(purlA2V2, purlA2V1));
    dependenciesMap.put(purlA1V11NullClassifier, Collections.singletonList(purlA2V11NullClassifier));
    dependenciesMap.put(purlA1V3, Collections.singletonList(purlA2V3));

    detailsMap.put(purlA2V1, detailsA2V1);
    detailsMap.put(purlA2V2, detailsA2V2);
    detailsMap.put(purlA2V11NullClassifier, detailsA2V11);
    detailsMap.put(purlA2V3, detailsA2V3);

    ComponentDependenciesDTO returnDto = new ComponentDependenciesDTO(dependenciesMap, detailsMap);
    List<ComponentDetailsDTO> allVersions = Arrays.asList(detailsDtoA1V1, detailsDtoA1V11, detailsDtoA1V3);
    mockHdsGetComponentDependencies(returnDto);
    mockLicenseFeature(true);
    ApiComponentRemediationValueDTO dto = componentRemediationService.getSuggestedRemediation(MAVEN_COORDINATES_A1_V1,
        allVersions, org, DevelopStageType.ID, componentDetailsLoaderFactory.newInstance(org),
        SourceEndpoint.API_COMPONENT_REMEDIATION);

    assertRemediations(dto, buildChangeDto(
        ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES, componentDtoA1V11));
    assertThat(dto.versionChanges).hasSize(1);
  }

  /**
   * Test with advanced strategies flag as true, with dependencies. Looking up single version a1v3 with a1v3 being the
   * current version. a1v3 dependencies: a2v1 a1v3 has no alerts, and a2v1 has failing alert on transitive-only policy.
   */
  @Test
  public void testGetSuggestedRemediation_Advanced_DependencyTypePolicy() {
    enableTransitiveSolver();
    policyDAO.delete(policyG1A2V1);
    Policy policy = new Policy("policyG1A2V1", "policyG1A2V1");
    policy.setOwnerId(org.getParentOwnerId());
    policy.setThreatLevel(10);
    Constraint constraint = new Constraint(null, "Test Constraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(CoordinatesConditionType.ID, "match", "maven:g1:a2:v1"));
    constraint.addCondition(new Condition(DependencyTypeConditionType.ID, "is", "transitive"));
    policy.addConstraint(constraint);
    policy.setAction(DevelopStageType.ID, Action.ID_FAIL);
    policy.setAction(BuildStageType.ID, Action.ID_WARN);
    tempEntity.newPolicy(policy);

    Map<PackageUrlIdentifier, Collection<PackageUrlIdentifier>> dependenciesMap = new HashMap<>();
    Map<PackageUrlIdentifier, ComponentDetails> detailsMap = new HashMap<>();

    dependenciesMap.put(purlA1V3, Collections.singletonList(purlA2V1));

    detailsMap.put(purlA2V1, detailsA2V1);

    ComponentDependenciesDTO returnDto = new ComponentDependenciesDTO(dependenciesMap, detailsMap);
    List<ComponentDetailsDTO> allVersions = Collections.singletonList(detailsDtoA1V3);
    mockHdsGetComponentDependencies(returnDto);
    mockLicenseFeature(true);
    ApiComponentRemediationValueDTO dto = componentRemediationService.getSuggestedRemediation(MAVEN_COORDINATES_A1_V3,
        allVersions, org, DevelopStageType.ID, componentDetailsLoaderFactory.newInstance(org),
        SourceEndpoint.API_COMPONENT_REMEDIATION);

    assertRemediations(dto, buildChangeDto(
        ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS, componentDtoA1V3));
    assertThat(dto.versionChanges).hasSize(1);
  }

  private ApiVersionChangeOptionDTO buildChangeDto(ApiVersionChangeOptionType type, ApiComponentDTOV2 dto) {
    ApiVersionChangeOptionDTO changeDto = new ApiVersionChangeOptionDTO();
    changeDto.setType(type);

    ApiComponentChangeActionDTO data = new ApiComponentChangeActionDTO();
    ApiComponentDTOV2 component = new ApiComponentDTOV2();
    component.packageUrl = dto.packageUrl;
    component.componentIdentifier = dto.componentIdentifier;
    component.breakingChangesCount = dto.breakingChangesCount;
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
          final ApiComponentDTOV2 actualComponentDto = dto.getData().getComponent();
          final ApiComponentDTOV2 expectedComponentDto = expected.getData().getComponent();
          assertThat(actualComponentDto.packageUrl).isEqualTo(expectedComponentDto.packageUrl);
          assertThat(actualComponentDto.componentIdentifier.toComponentIdentifier())
              .isEqualTo(expectedComponentDto.componentIdentifier.toComponentIdentifier());
          assertThat(actualComponentDto.displayName)
              .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(expectedComponentDto.componentIdentifier
                  .toComponentIdentifier()).toString());
          assertThat(actualComponentDto.breakingChangesCount)
              .isEqualTo(expectedComponentDto.breakingChangesCount);
          found = true;
          break;
        }
      }
      // we expected to find that the remediationDto purl matches with expected purl, but did not find one.
      assertThat(found).as(expected.getType() + " does not exist in remediation result!").isTrue();
    }
  }

  @Test
  public void testGetSuggestedRemediation_Conan() {
    ComponentIdentifier cd1 = ComponentIdentifier.createConanCoordinates("bison", "3.5.3", null, null);
    ComponentIdentifier cd2 = ComponentIdentifier.createConanCoordinates("bison", "3.7.1", null, null);
    ComponentIdentifier cd3 = ComponentIdentifier.createConanCoordinates("bison", "3.7.6", null, null);

    ComponentDetailsDTO detailsDtoConan = new ComponentDetailsDTO();
    detailsDtoConan.violatedPolicyCount = 1;
    detailsDtoConan.componentIdentifier = cd1;
    detailsDtoConan.policyAlerts = Arrays.asList(warnAlert, failAlert);
    detailsDtoConan.breakingChangesCount = BREAKING_CHANGES_1;

    ComponentDetailsDTO detailsDtoConan1 = new ComponentDetailsDTO();
    detailsDtoConan1.violatedPolicyCount = 2;
    detailsDtoConan1.componentIdentifier = cd2;
    detailsDtoConan1.policyAlerts = Arrays.asList(warnAlert, failAlert);
    detailsDtoConan1.breakingChangesCount = BREAKING_CHANGES_2;

    ComponentDetailsDTO detailsDtoConan3 = new ComponentDetailsDTO();
    detailsDtoConan3.violatedPolicyCount = 0;
    detailsDtoConan3.componentIdentifier = cd3;
    detailsDtoConan3.breakingChangesCount = BREAKING_CHANGES_3;

    List<ComponentDetailsDTO> allVersions = Arrays.asList(detailsDtoConan, detailsDtoConan1, detailsDtoConan3);

    cd1.ensureComplete();
    ApiComponentRemediationValueDTO dto = componentRemediationService.getSuggestedRemediation(cd1,
        allVersions, org, DevelopStageType.ID, componentDetailsLoaderFactory.newInstance(org),
        SourceEndpoint.API_COMPONENT_REMEDIATION);

    ApiComponentDTOV2 conanDto = new ApiComponentDTOV2();
    conanDto.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(cd3);
    conanDto.packageUrl = PackageUrlIdentifier.toPackageUrl(cd3);
    conanDto.breakingChangesCount = BREAKING_CHANGES_3;

    assertThat(dto.versionChanges).hasSize(1);
    assertRemediations(dto, buildChangeDto(
        ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS, conanDto));
  }

  @Test
  public void testGetSuggestedRemediation_InvalidPackageURLException_BadRequestException() {
    enableTransitiveSolver();
    ComponentIdentifier currentComponent = ComponentIdentifier.createMavenCoordinates("g", "añ漢€", "1.0", null, null);
    ComponentDetailsDTO componentDetailsDTO = new ComponentDetailsDTO();
    componentDetailsDTO.componentIdentifier = currentComponent;
    componentDetailsDTO.violatedPolicyCount = 0;
    List<ComponentDetailsDTO> allVersions = Collections.singletonList(componentDetailsDTO);
    mockHdsGetComponentDependencies(new ComponentDependenciesDTO(new HashMap<>(), new HashMap<>()));
    mockLicenseFeature(true);

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> componentRemediationService.getSuggestedRemediation(currentComponent,
            allVersions, org, DevelopStageType.ID, componentDetailsLoaderFactory.newInstance(org),
            SourceEndpoint.API_COMPONENT_REMEDIATION));
  }

  @Test
  public void testGetSuggestedRemediation_InvalidComponentIdentifierException_BadRequestException() {
    ComponentIdentifier currentComponent = ComponentIdentifier.createConanCoordinates(null, null, null, "c");
    ComponentDetailsDTO componentDetailsDTO = new ComponentDetailsDTO();
    componentDetailsDTO.componentIdentifier = currentComponent;
    componentDetailsDTO.violatedPolicyCount = 0;
    List<ComponentDetailsDTO> allVersions = Collections.singletonList(componentDetailsDTO);
    mockHdsGetComponentDependencies(new ComponentDependenciesDTO(new HashMap<>(), new HashMap<>()));
    mockLicenseFeature(true);

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> componentRemediationService.getSuggestedRemediation(currentComponent,
            allVersions, org, DevelopStageType.ID, componentDetailsLoaderFactory.newInstance(org),
            SourceEndpoint.API_COMPONENT_REMEDIATION));
  }

  @Test
  public void testGetSuggestedRemediationForTransitive_Advanced() {
    enableTransitiveSolver();

    Map<PackageUrlIdentifier, Collection<PackageUrlIdentifier>> dependenciesMap = new HashMap<>();
    Map<PackageUrlIdentifier, ComponentDetails> detailsMap = new HashMap<>();

    dependenciesMap.put(purlA1V1, Collections.singletonList(purlA2V1));
    dependenciesMap.put(purlA1V5, Collections.singletonList(purlA2V3));
    dependenciesMap.put(purlA1V11, Collections.singletonList(purlA2V4));

    detailsMap.put(purlA2V1, detailsA2V1);
    detailsMap.put(purlA2V4, detailsA2V4);
    detailsMap.put(purlA2V3, detailsA2V3);

    ComponentDependenciesDTO returnDto = new ComponentDependenciesDTO(dependenciesMap, detailsMap);
    List<ComponentDetailsDTO> allVersions = Arrays.asList(detailsDtoA1V5, detailsDtoA1V11);
    mockHdsGetComponentDependencies(returnDto);
    mockLicenseFeature(true);

    Map<ComponentIdentifier, List<ComponentDetailsDTO>> dtoMap = new HashMap<>();
    dtoMap.put(detailsDtoA1V5.componentIdentifier, allVersions);

    ApiComponentIdentifierDTOV2 transitiveComponent =
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(detailsA2V1.getComponentIdentifier());

    ApiComponentRemediationValueDTO dto =
        componentRemediationService.getSuggestedRemediationForTransitive(dtoMap, transitiveComponent,
            app, BuildStageType.ID, componentDetailsLoaderFactory.newInstance(org));

    assertThat(dto.versionChanges).hasSize(2);
    ApiVersionChangeOptionDTO apiVersionChangeOptionDTO = dto.versionChanges.get(0);
    assertThat(apiVersionChangeOptionDTO.getData().getComponent().componentIdentifier).isEqualTo(transitiveComponent);
    assertThat(apiVersionChangeOptionDTO.getDirectDependency()).isFalse();
    assertThat(apiVersionChangeOptionDTO.getType()).isEqualTo(
        ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES);
    assertThat(apiVersionChangeOptionDTO.getDirectDependencyData()
        .get(0)
        .getComponent().componentIdentifier.toComponentIdentifier()).isEqualTo(detailsDtoA1V11.componentIdentifier);

    apiVersionChangeOptionDTO = dto.versionChanges.get(1);
    assertThat(apiVersionChangeOptionDTO.getData().getComponent().componentIdentifier).isEqualTo(transitiveComponent);
    assertThat(apiVersionChangeOptionDTO.getDirectDependency()).isFalse();
    assertThat(apiVersionChangeOptionDTO.getType()).isEqualTo(
        ApiVersionChangeOptionType.NEXT_NON_FAILING_WITH_DEPENDENCIES);
    assertThat(apiVersionChangeOptionDTO.getDirectDependencyData()
        .get(0)
        .getComponent().componentIdentifier.toComponentIdentifier()).isEqualTo(detailsDtoA1V5.componentIdentifier);
  }

  @Test
  public void testGetSuggestedRemediationForTransitive_NoAdvanced() {
    Map<PackageUrlIdentifier, Collection<PackageUrlIdentifier>> dependenciesMap = new HashMap<>();
    Map<PackageUrlIdentifier, ComponentDetails> detailsMap = new HashMap<>();

    dependenciesMap.put(purlA1V1, Collections.singletonList(purlA2V1));
    dependenciesMap.put(purlA1V5, Collections.singletonList(purlA2V3));
    dependenciesMap.put(purlA1V11, Collections.singletonList(purlA2V4));

    detailsMap.put(purlA2V1, detailsA2V1);
    detailsMap.put(purlA2V4, detailsA2V4);
    detailsMap.put(purlA2V3, detailsA2V3);

    ComponentDependenciesDTO returnDto = new ComponentDependenciesDTO(dependenciesMap, detailsMap);
    List<ComponentDetailsDTO> allVersions = Arrays.asList(detailsDtoA1V5, detailsDtoA1V11);
    mockHdsGetComponentDependencies(returnDto);

    Map<ComponentIdentifier, List<ComponentDetailsDTO>> dtoMap = new HashMap<>();
    dtoMap.put(detailsDtoA1V5.componentIdentifier, allVersions);

    ApiComponentIdentifierDTOV2 transitiveComponent =
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(detailsA2V1.getComponentIdentifier());

    ApiComponentRemediationValueDTO dto =
        componentRemediationService.getSuggestedRemediationForTransitive(dtoMap, transitiveComponent,
            app, BuildStageType.ID, componentDetailsLoaderFactory.newInstance(org));

    assertThat(dto.versionChanges).hasSize(0);
  }

  final ComponentDetailsDTO createComponentDetailsDTO(
      final String groupId,
      final String artifactId,
      final String version,
      final float highestSecuritySeverity)
  {
    final ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates(groupId, artifactId, version, null, "any-ext");
    final ComponentDetailsDTO componentDetailsDTO = new ComponentDetailsDTO();
    componentDetailsDTO.componentIdentifier = componentIdentifier;

    componentDetailsDTO.highestSecurityVulnerabilitySeverity = highestSecuritySeverity;

    final SecurityVulnerability vulnerability = new SecurityVulnerability();
    vulnerability.setSeverity(highestSecuritySeverity);
    vulnerability.setCwe("CWE-Any");

    componentDetailsDTO.securityVulnerabilities = Collections.singletonList(vulnerability);

    return componentDetailsDTO;
  }

  private static int getRandomRangedInt() {
    final int min = 0;
    final int max = 20;
    return (int) ((Math.random() * (max - min)) + min);
  }

  private void assertTelemetryData(final Organization org) {
    ArgumentCaptor<TelemetryData> argumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(mockTelemetrySender, times(1)).send(argumentCaptor.capture());

    TelemetryData telemetryData = argumentCaptor.getValue();

    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.COMPONENT_REMEDIATION);

    final Map<String, Object> attributes = telemetryData.getAttributes();
    assertThat(attributes.get(OWNER_ID_TELEMETRY_ATTR)).isEqualTo(HdsClientAnalytics.obfuscate(org.getId()));
    assertThat((String) attributes.get(OWNER_TYPE_TELEMETRY_ATTR)).isEqualTo(org.getType().toString());
    assertThat(attributes.containsKey(REAL_OWNER_ID_TELEMETRY_ATTR)).isTrue();
    assertThat(attributes.get(REAL_OWNER_ID_TELEMETRY_ATTR)).isEqualTo(org.getId());
  }

  private void mockVersionScoring_mockSortedVersions(String... versionsInOrder) {
    lenient().when(versionScoringServiceMock.getSortedNonBreakingVersionsNoAuth(anyCollection()))
        .thenReturn(Map.of(MAVEN_COORDINATES_A1_V1, Arrays.asList(versionsInOrder)));
  }

  private void setBreakingChangesCount(int n, ComponentDetailsDTO... details) {
    for (ComponentDetailsDTO detail : details) {
      detail.breakingChangesCount = n;
    }
  }

  /**
   * Without advanced strategies, we are unable to suggest RECOMMENDED_NON_BREAKING_WITH_DEPENDENCIES
   * Only RECOMMENDED_NON_BREAKING could be calculated.
   */
  @Test
  public void testGetSuggestedRemediation_suggestNonBreakingVersion_topOneHasFailAlert_noAdvanced() {
    setBreakingChangesCount(0,
        detailsDtoA1V1, detailsDtoA1V2, detailsDtoA1V3, detailsDtoA1V4, detailsDtoA1V5, detailsDtoA1V6);
    // Version appearing first has the highest score.
    // Version not appearing has no score, usually because it has breaking changes.
    mockVersionScoring_mockSortedVersions("v6", "v4", "v2");

    setTransitiveSolverValue(false);
    Map<PackageUrlIdentifier, Collection<PackageUrlIdentifier>> dependenciesMap = new HashMap<>();
    Map<PackageUrlIdentifier, ComponentDetails> detailsMap = new HashMap<>();

    ComponentDependenciesDTO returnDto = new ComponentDependenciesDTO(dependenciesMap, detailsMap);
    List<ComponentDetailsDTO> allVersions = Arrays.asList(detailsDtoA1V1, detailsDtoA1V2, detailsDtoA1V3,
        detailsDtoA1V4);
    mockHdsGetComponentDependencies(returnDto);
    mockLicenseFeature(false);
    ApiComponentRemediationValueDTO dto = componentRemediationService.getSuggestedRemediation(MAVEN_COORDINATES_A1_V1,
        allVersions, org, DevelopStageType.ID, componentDetailsLoaderFactory.newInstance(org),
        SourceEndpoint.API_COMPONENT_REMEDIATION);

    // v6 is the top-scored non-breaking version, however, it has a violation. So the next-best, v4, is suggested.
    assertThat(dto.suggestedVersionChange.getData().getComponent().packageUrl).isEqualTo(componentDtoA1V4.packageUrl);
    assertThat(dto.suggestedVersionChange.getType()).isEqualTo(
        ApiVersionChangeOptionType.RECOMMENDED_NON_BREAKING);
    assertThat(dto.suggestedVersionChange.getIsGolden()).isFalse();
  }

  /**
   * If we exhausted all the non-breaking versions, but they all have fail alerts, we should not suggest any version.
   */
  @Test
  public void testGetSuggestedRemediation_suggestNonBreakingVersion_allHaveFailAlert_noAdvanced() {
    setBreakingChangesCount(0,
        detailsDtoA1V1, detailsDtoA1V2, detailsDtoA1V3, detailsDtoA1V4, detailsDtoA1V5, detailsDtoA1V6);
    mockVersionScoring_mockSortedVersions("v6");

    setTransitiveSolverValue(false);
    Map<PackageUrlIdentifier, Collection<PackageUrlIdentifier>> dependenciesMap = new HashMap<>();
    Map<PackageUrlIdentifier, ComponentDetails> detailsMap = new HashMap<>();

    ComponentDependenciesDTO returnDto = new ComponentDependenciesDTO(dependenciesMap, detailsMap);
    List<ComponentDetailsDTO> allVersions = Arrays.asList(detailsDtoA1V1, detailsDtoA1V2, detailsDtoA1V3,
        detailsDtoA1V4, detailsDtoA1V6);
    mockHdsGetComponentDependencies(returnDto);
    mockLicenseFeature(false);
    ApiComponentRemediationValueDTO dto = componentRemediationService.getSuggestedRemediation(MAVEN_COORDINATES_A1_V1,
        allVersions, org, DevelopStageType.ID, componentDetailsLoaderFactory.newInstance(org),
        SourceEndpoint.API_COMPONENT_REMEDIATION);

    // v6 is the only non-breaking version (because it has version score from HDS)
    // However, it has a violation. So the suggested version is null.
    assertThat(dto.suggestedVersionChange).isNull();
  }

  /**
   * With advanced strategies, we are able to suggest RECOMMENDED_NON_BREAKING_WITH_DEPENDENCIES, if there are any.
   */
  @Test
  public void testGetSuggestedRemediation_suggestGoldenVersion_goldenAvailable_advanced() {
    setBreakingChangesCount(0,
        detailsDtoA1V1, detailsDtoA1V2, detailsDtoA1V3, detailsDtoA1V4, detailsDtoA1V5, detailsDtoA1V6);
    // Version appearing first has the highest score.
    // Version not appearing has no score, usually because it has breaking changes.
    mockVersionScoring_mockSortedVersions("v6", "v4", "v2");

    setTransitiveSolverValue(true);
    Map<PackageUrlIdentifier, Collection<PackageUrlIdentifier>> dependenciesMap = new HashMap<>();
    Map<PackageUrlIdentifier, ComponentDetails> detailsMap = new HashMap<>();

    dependenciesMap.put(purlA1V4, Arrays.asList(purlA2V3, purlA2V4));

    detailsMap.put(purlA2V3, detailsA2V3);
    detailsMap.put(purlA2V4, detailsA2V4);

    ComponentDependenciesDTO returnDto = new ComponentDependenciesDTO(dependenciesMap, detailsMap);
    List<ComponentDetailsDTO> allVersions = Arrays.asList(detailsDtoA1V1, detailsDtoA1V2, detailsDtoA1V3,
        detailsDtoA1V4);
    mockHdsGetComponentDependencies(returnDto);
    mockLicenseFeature(true);
    ApiComponentRemediationValueDTO dto = componentRemediationService.getSuggestedRemediation(MAVEN_COORDINATES_A1_V1,
        allVersions, org, DevelopStageType.ID, componentDetailsLoaderFactory.newInstance(org),
        SourceEndpoint.API_COMPONENT_REMEDIATION);

    // We are able to verify v4 has no violation, and its dependencies also have no violation. So it is golden.
    assertThat(dto.suggestedVersionChange.getData().getComponent().packageUrl).isEqualTo(componentDtoA1V4.packageUrl);
    assertThat(dto.suggestedVersionChange.getType()).isEqualTo(
        ApiVersionChangeOptionType.RECOMMENDED_NON_BREAKING_WITH_DEPENDENCIES);
    assertThat(dto.suggestedVersionChange.getIsGolden()).isTrue();
  }

  /**
   * If we are unable to find a RECOMMENDED_NON_BREAKING_WITH_DEPENDENCIES version after exhausting the list,
   * we suggest the best RECOMMENDED_NON_BREAKING version instead.
   */
  @Test
  public void testGetSuggestedRemediation_suggestGoldenVersion_goldenUnavailable_advanced() {
    setBreakingChangesCount(0,
        detailsDtoA1V1, detailsDtoA1V2, detailsDtoA1V3, detailsDtoA1V4, detailsDtoA1V5, detailsDtoA1V6);
    // Version appearing first has the highest score.
    // Version not appearing has no score, usually because it has breaking changes.
    mockVersionScoring_mockSortedVersions("v6", "v4", "v2", "v3");

    setTransitiveSolverValue(true);
    Map<PackageUrlIdentifier, Collection<PackageUrlIdentifier>> dependenciesMap = new HashMap<>();
    Map<PackageUrlIdentifier, ComponentDetails> detailsMap = new HashMap<>();

    dependenciesMap.put(purlA1V3, Arrays.asList(purlA2V1, purlA2V2));
    dependenciesMap.put(purlA1V4, Arrays.asList(purlA2V1, purlA2V3));

    detailsMap.put(purlA2V1, detailsA2V1);
    detailsMap.put(purlA2V2, detailsA2V2);
    detailsMap.put(purlA2V3, detailsA2V3);

    ComponentDependenciesDTO returnDto = new ComponentDependenciesDTO(dependenciesMap, detailsMap);
    List<ComponentDetailsDTO> allVersions = Arrays.asList(detailsDtoA1V1, detailsDtoA1V2, detailsDtoA1V3,
        detailsDtoA1V4);
    mockHdsGetComponentDependencies(returnDto);
    mockLicenseFeature(true);
    ApiComponentRemediationValueDTO dto = componentRemediationService.getSuggestedRemediation(MAVEN_COORDINATES_A1_V1,
        allVersions, org, DevelopStageType.ID, componentDetailsLoaderFactory.newInstance(org),
        SourceEndpoint.API_COMPONENT_REMEDIATION);

    // A2v1 has violation and is a dependency of both v3 and v4 so neither is golden candidate.
    // We suggest v4 as non-golden RECOMMENDED_NON_BREAKING out of the two because it has higher score.
    assertThat(dto.suggestedVersionChange.getData().getComponent().packageUrl).isEqualTo(componentDtoA1V4.packageUrl);
    assertThat(dto.suggestedVersionChange.getType()).isEqualTo(
        ApiVersionChangeOptionType.RECOMMENDED_NON_BREAKING);
    assertThat(dto.suggestedVersionChange.getIsGolden()).isFalse();
  }

  /**
   * | Version | Dependencies | Alerts |
   * |---------|--------------|--------|
   * | a1v1 | None | Failing|
   * | a1v2 | a2v1, a2v2 | Warning|
   * | a1v3 | a2v2, a2v3 | None |
   * | a1v4 | a2v3, a2v4 | None |
   *
   * | Dependency | Alerts |
   * |------------|--------|
   * | a2v1 | Failing|
   * | a2v2 | Warning|
   * | a2v3 | None |
   * | a2v4 | None |
   *
   * If we can find a golden version (v4 in this case),
   * even if it has a lower score than the non-golden non-breaking version, we suggest it.
   */
  @Test
  public void testGetSuggestedRemediation_suggestGoldenVersion_goldenTakesPriority_advanced() {
    setBreakingChangesCount(0,
        detailsDtoA1V1, detailsDtoA1V2, detailsDtoA1V3, detailsDtoA1V4, detailsDtoA1V5, detailsDtoA1V6);
    // Version appearing first has the highest score.
    // Version not appearing has no score, usually because it has breaking changes.
    mockVersionScoring_mockSortedVersions("v3", "v4", "v2");

    setTransitiveSolverValue(true);
    Map<PackageUrlIdentifier, Collection<PackageUrlIdentifier>> dependenciesMap = new HashMap<>();
    Map<PackageUrlIdentifier, ComponentDetails> detailsMap = new HashMap<>();

    dependenciesMap.put(purlA1V2, Arrays.asList(purlA2V1, purlA2V2));
    dependenciesMap.put(purlA1V3, Arrays.asList(purlA2V2, purlA2V3));
    dependenciesMap.put(purlA1V4, Arrays.asList(purlA2V3, purlA2V4));

    detailsMap.put(purlA2V1, detailsA2V1);
    detailsMap.put(purlA2V2, detailsA2V2);
    detailsMap.put(purlA2V3, detailsA2V3);
    detailsMap.put(purlA2V4, detailsA2V4);

    ComponentDependenciesDTO returnDto = new ComponentDependenciesDTO(dependenciesMap, detailsMap);
    List<ComponentDetailsDTO> allVersions = Arrays.asList(detailsDtoA1V1, detailsDtoA1V2, detailsDtoA1V3,
        detailsDtoA1V4);
    mockHdsGetComponentDependencies(returnDto);
    mockLicenseFeature(true);
    ApiComponentRemediationValueDTO dto = componentRemediationService.getSuggestedRemediation(MAVEN_COORDINATES_A1_V1,
        allVersions, org, DevelopStageType.ID, componentDetailsLoaderFactory.newInstance(org),
        SourceEndpoint.API_COMPONENT_REMEDIATION);

    // Both a1v3 and a1v4 are non-breaking versions with no alerts.
    // We suggest v4 as because it's "golden" (neither of its dependencies have alert)
    // even though it has a lower score than v3 (v3 not "golden" because one of its dependencies a2v2 has violation).
    assertThat(dto.suggestedVersionChange.getData().getComponent().packageUrl).isEqualTo(componentDtoA1V4.packageUrl);
    assertThat(dto.suggestedVersionChange.getType()).isEqualTo(
        ApiVersionChangeOptionType.RECOMMENDED_NON_BREAKING_WITH_DEPENDENCIES);
    assertThat(dto.suggestedVersionChange.getIsGolden()).isTrue();
  }

  @Test
  public void testSortAndDeduplicateVersionChanges_deduplicateShouldKeepDesirableOrder() {
    ApiVersionChangeOptionDTO v11NextNoViolations =
        buildChangeDto(
            ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS, componentDtoA1V11);
    ApiVersionChangeOptionDTO v11NextNoViolationsWithDependencies =
        buildChangeDto(
            ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES, componentDtoA1V11);
    ApiVersionChangeOptionDTO v11NextNonFailing =
        buildChangeDto(
            ApiVersionChangeOptionType.NEXT_NON_FAILING, componentDtoA1V11);
    ApiVersionChangeOptionDTO v11NextNonFailingWithDependencies =
        buildChangeDto(
            ApiVersionChangeOptionType.NEXT_NON_FAILING_WITH_DEPENDENCIES, componentDtoA1V11);

    assertThat(ComponentRemediationService.sortAndDeduplicateVersionChanges(
        List.of(v11NextNoViolations,
            v11NextNoViolationsWithDependencies,
            v11NextNonFailing,
            v11NextNonFailingWithDependencies)))
                .hasSize(1)
                .containsExactly(v11NextNoViolationsWithDependencies);

    assertThat(ComponentRemediationService.sortAndDeduplicateVersionChanges(
        List.of(v11NextNoViolations,
            v11NextNonFailing,
            v11NextNonFailingWithDependencies)))
                .hasSize(1)
                .containsExactly(v11NextNoViolations);

    assertThat(ComponentRemediationService.sortAndDeduplicateVersionChanges(
        List.of(v11NextNonFailing,
            v11NextNonFailingWithDependencies)))
                .hasSize(1)
                .containsExactly(v11NextNonFailingWithDependencies);
  }

  @Test
  public void testSortAndDeduplicateVersionChanges_deduplicateAndSortingShouldBothWork() {
    ApiVersionChangeOptionDTO v5NextNonFailing =
        buildChangeDto(
            ApiVersionChangeOptionType.NEXT_NON_FAILING, componentDtoA1V5);
    ApiVersionChangeOptionDTO v6NextNoViolations =
        buildChangeDto(
            ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS, componentDtoA1V6);
    ApiVersionChangeOptionDTO v11NextNoViolationsWithDependencies =
        buildChangeDto(
            ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES, componentDtoA1V11);
    ApiVersionChangeOptionDTO v11NextNonFailingWithDependencies =
        buildChangeDto(
            ApiVersionChangeOptionType.NEXT_NON_FAILING_WITH_DEPENDENCIES, componentDtoA1V11);

    assertThat(ComponentRemediationService.sortAndDeduplicateVersionChanges(
        Arrays.asList(v5NextNonFailing,
            v6NextNoViolations,
            v11NextNoViolationsWithDependencies,
            v11NextNonFailingWithDependencies)))
                .hasSize(3)
                .containsExactly(v11NextNoViolationsWithDependencies, v6NextNoViolations, v5NextNonFailing);
  }

  @Test
  public void testGetApplicableVersionChange_suggestedVersionChange_notNull() {
    ApiVersionChangeOptionDTO v11NextNoViolations =
        buildChangeDto(ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS, componentDtoA1V11);
    ApiVersionChangeOptionDTO v11NextNoViolationsWithDependencies =
        buildChangeDto(ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES, componentDtoA1V11);

    List<ApiVersionChangeOptionDTO> versionChanges =
        Arrays.asList(v11NextNoViolations, v11NextNoViolationsWithDependencies);

    ApiSuggestedVersionChangeOptionDTO suggestedVersionChange = new ApiSuggestedVersionChangeOptionDTO(
        ApiVersionChangeOptionType.RECOMMENDED_NON_BREAKING,
        true,
        new ApiComponentChangeActionDTO(componentDtoA1V6));

    Optional<ApiVersionChangeOptionDTO> result = componentRemediationService.getApplicableVersionChange(
        suggestedVersionChange, versionChanges);

    assertThat(result).isPresent();
    assertThat(result.get().getType()).isEqualTo(ApiVersionChangeOptionType.RECOMMENDED_NON_BREAKING);
    assertThat(result.get().getData().getComponent().packageUrl).isEqualTo(componentDtoA1V6.packageUrl);
  }

  @Test
  public void testGetApplicableVersionChange_suggestedVersionChange_null_withNextNoViolationsWithDependencies() {
    ApiVersionChangeOptionDTO v11NextNoViolations =
        buildChangeDto(ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS, componentDtoA1V11);
    ApiVersionChangeOptionDTO v11NextNoViolationsWithDependencies =
        buildChangeDto(ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES, componentDtoA1V11);
    ApiVersionChangeOptionDTO v11NextNonFailing =
        buildChangeDto(ApiVersionChangeOptionType.NEXT_NON_FAILING, componentDtoA1V5);

    List<ApiVersionChangeOptionDTO> versionChanges =
        Arrays.asList(v11NextNoViolations, v11NextNoViolationsWithDependencies, v11NextNonFailing);

    Optional<ApiVersionChangeOptionDTO> result = componentRemediationService.getApplicableVersionChange(
        null, versionChanges);

    assertThat(result).isPresent();
    assertThat(result.get().getType()).isEqualTo(ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES);
    assertThat(result.get().getData().getComponent().packageUrl).isEqualTo(componentDtoA1V11.packageUrl);
  }

  @Test
  public void testGetApplicableVersionChange_suggestedVersionChange_null_withNextNoViolations() {
    ApiVersionChangeOptionDTO v11NextNoViolations =
        buildChangeDto(ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS, componentDtoA1V11);
    ApiVersionChangeOptionDTO v11NextNonFailing =
        buildChangeDto(ApiVersionChangeOptionType.NEXT_NON_FAILING, componentDtoA1V5);

    List<ApiVersionChangeOptionDTO> versionChanges = Arrays.asList(v11NextNoViolations, v11NextNonFailing);

    Optional<ApiVersionChangeOptionDTO> result = componentRemediationService.getApplicableVersionChange(
        null, versionChanges);

    assertThat(result).isPresent();
    assertThat(result.get().getType()).isEqualTo(ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS);
    assertThat(result.get().getData().getComponent().packageUrl).isEqualTo(componentDtoA1V11.packageUrl);
  }

  @Test
  public void testGetApplicableVersionChange_suggestedVersionChange_null_noPreferredTypes() {
    ApiVersionChangeOptionDTO v11NextNonFailing =
        buildChangeDto(ApiVersionChangeOptionType.NEXT_NON_FAILING, componentDtoA1V11);

    List<ApiVersionChangeOptionDTO> versionChanges = Collections.singletonList(v11NextNonFailing);

    Optional<ApiVersionChangeOptionDTO> result = componentRemediationService.getApplicableVersionChange(
        null, versionChanges);

    assertThat(result).isEmpty();
  }

  @Test
  public void testGetApplicableVersionChange_suggestedVersionChange_null_emptyVersionChanges() {
    Optional<ApiVersionChangeOptionDTO> result = componentRemediationService.getApplicableVersionChange(
        null, Collections.emptyList());

    assertThat(result).isEmpty();
  }

  @Test
  public void testGetApplicableVersionChangeFromAllType_suggestedVersionChange_notNull() {
    ApiVersionChangeOptionDTO v11NextNoViolations =
        buildChangeDto(ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS, componentDtoA1V11);
    ApiVersionChangeOptionDTO v5NextNonFailing =
        buildChangeDto(ApiVersionChangeOptionType.NEXT_NON_FAILING, componentDtoA1V5);

    List<ApiVersionChangeOptionDTO> versionChanges = Arrays.asList(v11NextNoViolations, v5NextNonFailing);

    ApiSuggestedVersionChangeOptionDTO suggestedVersionChange = new ApiSuggestedVersionChangeOptionDTO(
        ApiVersionChangeOptionType.RECOMMENDED_NON_BREAKING,
        true,
        new ApiComponentChangeActionDTO(componentDtoA1V6));

    Optional<ApiVersionChangeOptionDTO> result = componentRemediationService.getApplicableVersionChangeFromAllType(
        suggestedVersionChange, versionChanges);

    assertThat(result).isPresent();
    assertThat(result.get().getType()).isEqualTo(ApiVersionChangeOptionType.RECOMMENDED_NON_BREAKING);
    assertThat(result.get().getData().getComponent().packageUrl).isEqualTo(componentDtoA1V6.packageUrl);
  }

  @Test
  public void testGetApplicableVersionChangeFromAllType_suggestedVersionChange_null_withPrioritizedOrder() {
    ApiVersionChangeOptionDTO v11NextNonFailing =
        buildChangeDto(ApiVersionChangeOptionType.NEXT_NON_FAILING, componentDtoA1V11);
    ApiVersionChangeOptionDTO v5NextNoViolations =
        buildChangeDto(ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS, componentDtoA1V5);
    ApiVersionChangeOptionDTO v6NextNoViolationsWithDependencies =
        buildChangeDto(ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES, componentDtoA1V6);
    ApiVersionChangeOptionDTO v3NextNonFailingWithDependencies =
        buildChangeDto(ApiVersionChangeOptionType.NEXT_NON_FAILING_WITH_DEPENDENCIES, componentDtoA1V3);

    // Order is intentionally shuffled to test sorting
    List<ApiVersionChangeOptionDTO> versionChanges =
        Arrays.asList(v11NextNonFailing, v3NextNonFailingWithDependencies, v5NextNoViolations,
            v6NextNoViolationsWithDependencies);

    Optional<ApiVersionChangeOptionDTO> result = componentRemediationService.getApplicableVersionChangeFromAllType(
        null, versionChanges);

    assertThat(result).isPresent();
    // Should select based on the preference order in PREFERABLE_TYPE_ORDER
    assertThat(result.get().getType()).isEqualTo(ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES);
    assertThat(result.get().getData().getComponent().packageUrl).isEqualTo(componentDtoA1V6.packageUrl);
  }

  @Test
  public void testGetApplicableVersionChangeFromAllType_suggestedVersionChange_null_emptyVersionChanges() {
    Optional<ApiVersionChangeOptionDTO> result = componentRemediationService.getApplicableVersionChangeFromAllType(
        null, Collections.emptyList());

    assertThat(result).isEmpty();
  }

  @Test
  public void testGetApplicableVersionChangeFromAllType_suggestedVersionChange_null_withSingleOption() {
    ApiVersionChangeOptionDTO v11NextNonFailing =
        buildChangeDto(ApiVersionChangeOptionType.NEXT_NON_FAILING, componentDtoA1V11);

    List<ApiVersionChangeOptionDTO> versionChanges = Collections.singletonList(v11NextNonFailing);

    Optional<ApiVersionChangeOptionDTO> result = componentRemediationService.getApplicableVersionChangeFromAllType(
        null, versionChanges);

    assertThat(result).isPresent();
    assertThat(result.get().getType()).isEqualTo(ApiVersionChangeOptionType.NEXT_NON_FAILING);
    assertThat(result.get().getData().getComponent().packageUrl).isEqualTo(componentDtoA1V11.packageUrl);
  }

  @Test
  public void testGetSuggestedRemediation_INNER_SOURCE_LATEST_NON_BREAKING() {
    Application application = tempEntity.newApplicationWithParent();

    ComponentIdentifier innerSourceComponent = ComponentIdentifier.createMavenCoordinates(
        "com.example", "innerSource", "1.2.3", "", "jar");

    PackageUrlIdentifier packageUrl = InnerSourceUtils.getVersionlessPackageUrl(innerSourceComponent);
    InnerSourceApplication innerSourceApp =
        tempEntity.newInnerSourceApplication(packageUrl.getPackageUrl(), application);
    tempEntity.newInnerSourceVersion(innerSourceApp, "1.3.0", StageTypes.RELEASE.getId());

    ApiComponentRemediationValueDTO remediationDto = componentRemediationService
        .getSuggestedRemediation(innerSourceComponent, Collections.emptyList(), app, StageTypes.RELEASE.getId(),
            componentDetailsLoaderFactory.newInstance(app), SourceEndpoint.API_COMPONENT_REMEDIATION);

    assertThat(remediationDto.suggestedVersionChange).isNotNull();
    assertThat(remediationDto.suggestedVersionChange.getType()).isEqualTo(
        ApiVersionChangeOptionType.INNER_SOURCE_LATEST_NON_BREAKING);
    assertThat(remediationDto.suggestedVersionChange.getData().getComponent().componentIdentifier.getCoordinates()
        .get("version")).isEqualTo("1.3.0");
  }

  @Test
  public void testGetSuggestedRemediation_INNER_SOURCE_LATEST() {
    Application application = tempEntity.newApplicationWithParent();

    ComponentIdentifier innerSourceComponent = ComponentIdentifier.createMavenCoordinates(
        "com.example", "innerSource", "1.2.3", "", "jar");

    PackageUrlIdentifier packageUrl = InnerSourceUtils.getVersionlessPackageUrl(innerSourceComponent);
    InnerSourceApplication innerSourceApp =
        tempEntity.newInnerSourceApplication(packageUrl.getPackageUrl(), application);
    tempEntity.newInnerSourceVersion(innerSourceApp, "2.0.0", StageTypes.RELEASE.getId());

    ApiComponentRemediationValueDTO remediationDto = componentRemediationService
        .getSuggestedRemediation(innerSourceComponent, Collections.emptyList(), app, StageTypes.RELEASE.getId(),
            componentDetailsLoaderFactory.newInstance(app), SourceEndpoint.API_COMPONENT_REMEDIATION);

    assertThat(remediationDto.suggestedVersionChange).isNotNull();
    assertThat(remediationDto.suggestedVersionChange.getType()).isEqualTo(
        ApiVersionChangeOptionType.INNER_SOURCE_LATEST);
    assertThat(remediationDto.suggestedVersionChange.getData().getComponent().componentIdentifier.getCoordinates()
        .get("version")).isEqualTo("2.0.0");
  }

  @Test
  public void testGetSuggestedRemediation_INNER_SOURCE_LATEST_buildStage() {
    Application application = tempEntity.newApplicationWithParent();

    ComponentIdentifier innerSourceComponent = ComponentIdentifier.createMavenCoordinates(
        "com.example", "innerSource", "1.2.3", "", "jar");

    PackageUrlIdentifier packageUrl = InnerSourceUtils.getVersionlessPackageUrl(innerSourceComponent);
    InnerSourceApplication innerSourceApp =
        tempEntity.newInnerSourceApplication(packageUrl.getPackageUrl(), application);
    // in the build stage
    tempEntity.newInnerSourceVersion(innerSourceApp, "2.0.0", StageTypes.BUILD.getId());

    ApiComponentRemediationValueDTO remediationDto = componentRemediationService
        .getSuggestedRemediation(innerSourceComponent, Collections.emptyList(), app, StageTypes.RELEASE.getId(),
            componentDetailsLoaderFactory.newInstance(app), SourceEndpoint.API_COMPONENT_REMEDIATION);

    assertThat(remediationDto.suggestedVersionChange).isNull();
  }

  @Test
  public void testGetSuggestedRemediation_InnerSource_SameVersion() {
    Application application = tempEntity.newApplicationWithParent();

    ComponentIdentifier innerSourceComponent = ComponentIdentifier.createMavenCoordinates(
        "com.example", "innerSource", "1.2.3", "", "jar");

    PackageUrlIdentifier packageUrl = InnerSourceUtils.getVersionlessPackageUrl(innerSourceComponent);
    InnerSourceApplication innerSourceApplication =
        tempEntity.newInnerSourceApplication(packageUrl.getPackageUrl(), application);
    tempEntity.newInnerSourceVersion(innerSourceApplication, "1.2.3", StageTypes.RELEASE.getId());

    ApiComponentRemediationValueDTO remediationDto = componentRemediationService
        .getSuggestedRemediation(innerSourceComponent, Collections.emptyList(), app, StageTypes.RELEASE.getId(),
            componentDetailsLoaderFactory.newInstance(app), SourceEndpoint.API_COMPONENT_REMEDIATION);

    assertThat(remediationDto.suggestedVersionChange).isNull();
  }

  @Test
  public void testGetSuggestedRemediation_goldenVersion_dependenciesWithoutViolationsAboveThreshold() {
    SystemConfigurationPropertyFeature.DEVELOPER_SUGGEST_NON_BREAKING_VERSION.setEnabled(true);
    setBreakingChangesCount(0,
        detailsDtoA1V1, detailsDtoA1V2, detailsDtoA1V3, detailsDtoA1V4);
    detailsDtoA1V3.policyMaxThreatLevelsByCategory = Collections.emptyMap();
    detailsDtoA1V4.policyMaxThreatLevelsByCategory = Collections.emptyMap();
    mockVersionScoring_mockSortedVersions("v4", "v3");

    setTransitiveSolverValue(true);
    Map<PackageUrlIdentifier, Collection<PackageUrlIdentifier>> dependenciesMap = new HashMap<>();
    Map<PackageUrlIdentifier, ComponentDetails> detailsMap = new HashMap<>();

    dependenciesMap.put(purlA1V3, Collections.singletonList(purlA2V1));
    dependenciesMap.put(purlA1V4, Collections.singletonList(purlA2V3));

    detailsMap.put(purlA2V1, detailsA2V1);
    detailsMap.put(purlA2V3, detailsA2V3);

    ComponentDependenciesDTO returnDto = new ComponentDependenciesDTO(dependenciesMap, detailsMap);
    List<ComponentDetailsDTO> allVersions = Arrays.asList(detailsDtoA1V1, detailsDtoA1V2, detailsDtoA1V3,
        detailsDtoA1V4);
    mockHdsGetComponentDependencies(returnDto);
    mockLicenseFeature(true);

    ApiComponentRemediationValueDTO dto = componentRemediationService.getSuggestedRemediation(MAVEN_COORDINATES_A1_V1,
        allVersions, org, DevelopStageType.ID, componentDetailsLoaderFactory.newInstance(org),
        SourceEndpoint.API_COMPONENT_REMEDIATION);

    assertThat(dto.suggestedVersionChange.getData().getComponent().packageUrl).isEqualTo(componentDtoA1V4.packageUrl);
    assertThat(dto.suggestedVersionChange.getType()).isEqualTo(
        ApiVersionChangeOptionType.RECOMMENDED_NON_BREAKING_WITH_DEPENDENCIES);
    assertThat(dto.suggestedVersionChange.getIsGolden()).isTrue();
  }

  @Test
  public void testGetSuggestedRemediation_goldenVersion_dependenciesWithViolationsAboveThreshold() {
    SystemConfigurationPropertyFeature.DEVELOPER_SUGGEST_NON_BREAKING_VERSION.setEnabled(true);
    setBreakingChangesCount(0,
        detailsDtoA1V1, detailsDtoA1V2, detailsDtoA1V3);
    mockVersionScoring_mockSortedVersions("v3");

    setTransitiveSolverValue(true);
    Map<PackageUrlIdentifier, Collection<PackageUrlIdentifier>> dependenciesMap = new HashMap<>();
    Map<PackageUrlIdentifier, ComponentDetails> detailsMap = new HashMap<>();

    dependenciesMap.put(purlA1V3, Collections.singletonList(purlA2V1));

    PolicyAlert highThreatAlert = new PolicyAlert(new PolicyFact("policyId", "High Threat", 5),
        Collections.singletonList(new Action(Action.ID_WARN)));
    ComponentDetails detailsA2V1WithHighThreat = buildComponentDetails(MAVEN_COORDINATES_A2_V1,
        Collections.singletonList(highThreatAlert));

    detailsMap.put(purlA2V1, detailsA2V1WithHighThreat);

    ComponentDependenciesDTO returnDto = new ComponentDependenciesDTO(dependenciesMap, detailsMap);
    List<ComponentDetailsDTO> allVersions = Arrays.asList(detailsDtoA1V1, detailsDtoA1V2, detailsDtoA1V3);
    mockHdsGetComponentDependencies(returnDto);
    mockLicenseFeature(true);

    ApiComponentRemediationValueDTO dto = componentRemediationService.getSuggestedRemediation(MAVEN_COORDINATES_A1_V1,
        allVersions, org, DevelopStageType.ID, componentDetailsLoaderFactory.newInstance(org),
        SourceEndpoint.API_COMPONENT_REMEDIATION);

    assertThat(dto.suggestedVersionChange.getData().getComponent().packageUrl).isEqualTo(componentDtoA1V3.packageUrl);
    assertThat(dto.suggestedVersionChange.getType()).isEqualTo(
        ApiVersionChangeOptionType.RECOMMENDED_NON_BREAKING);
    assertThat(dto.suggestedVersionChange.getIsGolden()).isFalse();
  }

  @Test
  public void testGetSuggestedRemediation_goldenVersion_mixedDependenciesWithViolationsAboveThreshold() {
    SystemConfigurationPropertyFeature.DEVELOPER_SUGGEST_NON_BREAKING_VERSION.setEnabled(true);
    setBreakingChangesCount(0,
        detailsDtoA1V1, detailsDtoA1V2, detailsDtoA1V3);
    mockVersionScoring_mockSortedVersions("v3");

    setTransitiveSolverValue(true);
    Map<PackageUrlIdentifier, Collection<PackageUrlIdentifier>> dependenciesMap = new HashMap<>();
    Map<PackageUrlIdentifier, ComponentDetails> detailsMap = new HashMap<>();

    dependenciesMap.put(purlA1V3, Arrays.asList(purlA2V1, purlA2V2));

    PolicyAlert highThreatAlert = new PolicyAlert(new PolicyFact("policyId1", "High Threat", 8),
        Collections.singletonList(new Action(Action.ID_WARN)));
    PolicyAlert lowThreatAlert = new PolicyAlert(new PolicyFact("policyId2", "Low Threat", 0),
        Collections.singletonList(new Action(Action.ID_WARN)));

    ComponentDetails detailsA2V1WithHighThreat = buildComponentDetails(MAVEN_COORDINATES_A2_V1,
        Collections.singletonList(highThreatAlert));
    ComponentDetails detailsA2V2WithLowThreat = buildComponentDetails(MAVEN_COORDINATES_A2_V2,
        Collections.singletonList(lowThreatAlert));

    detailsMap.put(purlA2V1, detailsA2V1WithHighThreat);
    detailsMap.put(purlA2V2, detailsA2V2WithLowThreat);

    ComponentDependenciesDTO returnDto = new ComponentDependenciesDTO(dependenciesMap, detailsMap);
    List<ComponentDetailsDTO> allVersions = Arrays.asList(detailsDtoA1V1, detailsDtoA1V2, detailsDtoA1V3);
    mockHdsGetComponentDependencies(returnDto);
    mockLicenseFeature(true);

    ApiComponentRemediationValueDTO dto = componentRemediationService.getSuggestedRemediation(MAVEN_COORDINATES_A1_V1,
        allVersions, org, DevelopStageType.ID, componentDetailsLoaderFactory.newInstance(org),
        SourceEndpoint.API_COMPONENT_REMEDIATION);

    assertThat(dto.suggestedVersionChange.getData().getComponent().packageUrl).isEqualTo(componentDtoA1V3.packageUrl);
    assertThat(dto.suggestedVersionChange.getType()).isEqualTo(
        ApiVersionChangeOptionType.RECOMMENDED_NON_BREAKING);
    assertThat(dto.suggestedVersionChange.getIsGolden()).isFalse();
  }

  @Test
  public void testPREFERABLE_TYPE_ORDER_containsCorrectTypes() {
    assertThat(ComponentRemediationService.PREFERABLE_TYPE_ORDER).containsExactly(
        ApiVersionChangeOptionType.RECOMMENDED_NON_BREAKING_WITH_DEPENDENCIES,
        ApiVersionChangeOptionType.RECOMMENDED_NON_BREAKING,
        ApiVersionChangeOptionType.INNER_SOURCE_LATEST_NON_BREAKING,
        ApiVersionChangeOptionType.INNER_SOURCE_LATEST,
        ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES,
        ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS,
        ApiVersionChangeOptionType.NEXT_NON_FAILING_WITH_DEPENDENCIES,
        ApiVersionChangeOptionType.NEXT_NON_FAILING);
  }
}
