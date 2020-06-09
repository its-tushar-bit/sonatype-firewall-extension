/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sonatype.clm.dto.model.ComponentSummary;
import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.clm.dto.model.component.ComponentDetailsList;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.api.v2.service.ComponentEvaluationV2Helper;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.dependency.ComponentDependenciesDTO;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiComponentRemediationResourceTest
    extends AbstractResourceTest
{
  private static final ComponentIdentifier MAVEN_COORDINATES_V1 = ComponentIdentifier.createMavenCoordinates("g1", "a1",
      "v1", "", "jar");

  private static final ComponentIdentifier MAVEN_COORDINATES_V2 = ComponentIdentifier.createMavenCoordinates("g1", "a1",
      "v2", "", "jar");

  private static final ComponentIdentifier MAVEN_COORDINATES_V3 = ComponentIdentifier.createMavenCoordinates("g1", "a1",
      "v3", "", "jar");

  private ComponentEvaluationV2Helper componentEvaluationV2Helper = new ComponentEvaluationV2Helper();

  private Application app;

  private Organization org;

  @Before
  public void before() throws Exception {
    org = tempEntity.newOrganization("Org");
    app = tempEntity.newApplication(org.getId());
    setFeatures(LicensedFeature.ADVANCED_RECOMMENDATION_STRATEGIES);
  }

  @Test
  public void testSuggestedRemediation_Application() throws Exception {
    ApiComponentDTOV2 component = componentEvaluationV2Helper.createComponent(MAVEN_COORDINATES_V1,null);
    assertRemediationApplication(component, ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS,
        ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES);
  }

  @Test
  public void testSuggestedRemediation_Application_ThirdParty_NoViolations() throws Exception {
    testSuggestedRemediation_Application_ThirdParty(1);
  }

  @Test
  public void testSuggestedRemediation_Application_ThirdParty_WithViolation() throws Exception {
    createPolicyWithSecurityVulnerabilityConstraint(org.getId());
    testSuggestedRemediation_Application_ThirdParty(1);
  }

  private void testSuggestedRemediation_Application_ThirdParty(final int expectedRemediationVersionsCount)
      throws Exception
  {
    final String scanId = "ScanId";
    createReportFile(app.getId(), scanId, "/ApiComponentRemediationResourceTest/report");
    final ComponentIdentifier tpComponentIdentifier = componentIdentifierFrom("debian-9", "glibc", "2.24-11+deb9u3");
    ApiComponentDTOV2 component = componentEvaluationV2Helper.createComponent(tpComponentIdentifier, null);

    String identificationSource = IdentificationSource.CLAIR.getId();

    HttpResponse response =
        restRequest().path(PublicApiPaths.COMPONENT_REMEDIATION_PATH_V2).parameter(OwnerType.APPLICATION, app.getId())
            .query("identificationSource", identificationSource).query("scanId", scanId)
            .body(component).post();

    assertResponseStatus(200, response);
    String responseText = response.getBodyText();
    assertThat(responseText).doesNotContain("proprietary");

    ApiComponentRemediationDTO result = response.getBody(ApiComponentRemediationDTO.class);
    assertThat(result).isNotNull();
    assertThat(result.remediation.versionChanges)
        .hasSize(expectedRemediationVersionsCount);

    ApiVersionChangeOptionDTO versionChangeDTO = result.remediation.versionChanges.get(0);
    assertThat(versionChangeDTO.getType()).isEqualTo(ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS);
    assertThat(versionChangeDTO.getData().getComponent().packageUrl).isEqualTo("pkg:debian-9/glibc@3.47-32%2Bdeb9u1");
  }

  @Test
  public void testSuggestedRemediation_Application_Purl() throws Exception {
    String purl = "pkg:maven/g1/a1@v1?type=jar";
    ApiComponentDTOV2 component = componentEvaluationV2Helper.createComponent(purl);
    assertRemediationApplication(component, ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS,
        ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES);
  }
  
  private void assertRemediationApplication(
      ApiComponentDTOV2 component,
      final ApiVersionChangeOptionType... optionTypes) throws Exception
  {
    mockComponentSummary(MAVEN_COORDINATES_V1, ComponentSummary.create(true));
    mockGetDependencies(new ComponentDependenciesDTO(new HashMap<>(), new HashMap<>()));
    createPolicyWithSecurityVulnerabilityConstraint(app.getId());

    ComponentDetails details1 = createComponentDetailsForSecurityViolation(MAVEN_COORDINATES_V1);
    ComponentDetails details2 = createComponentDetailsForSecurityViolation(MAVEN_COORDINATES_V2);
    ComponentDetails details3 = createComponentDetailsForNoViolation(MAVEN_COORDINATES_V3);
    List<ComponentDetails> list = Stream.of(details1, details2, details3).collect(Collectors.toList());
    ComponentDetailsList detailsList = new ComponentDetailsList();
    detailsList.setList(list);
    mockComponentDetails(detailsList);

    // no violations / alerts - we expect component version 3
    ApiComponentDTOV2 expectedComponent = componentEvaluationV2Helper.createComponent(MAVEN_COORDINATES_V3, null);

    HttpResponse response =
        restRequest().path(PublicApiPaths.COMPONENT_REMEDIATION_PATH_V2).parameter(OwnerType.APPLICATION, app.getId())
            .body(component).post();

    assertResponse(response, expectedComponent, PackageUrlIdentifier.toPackageUrl(MAVEN_COORDINATES_V3), optionTypes);
  }

  @Test
  public void testSuggestedRemediation_Organization() throws Exception {
    ApiComponentDTOV2 component = componentEvaluationV2Helper.createComponent(MAVEN_COORDINATES_V1, null);
    assertRemediationOrganization(component, ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS,
        ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES);
  }
  
  @Test
  public void testSuggestedRemediation_Organization_Purl() throws Exception {
    String purl = "pkg:maven/g1/a1@v1?type=jar";
    ApiComponentDTOV2 component = componentEvaluationV2Helper.createComponent(purl);
    assertRemediationOrganization(component, ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS,
        ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES);
  }
  
  private void assertRemediationOrganization(
      final ApiComponentDTOV2 component,
      final ApiVersionChangeOptionType... optionTypes) throws Exception
  {
    mockGetDependencies(new ComponentDependenciesDTO(new HashMap<>(), new HashMap<>()));
    mockComponentSummary(MAVEN_COORDINATES_V1, ComponentSummary.create(true));
    Organization org = tempEntity.newOrganization("testOrg");
    createPolicyWithSecurityVulnerabilityConstraint(org.getId());

    ComponentDetails details1 = createComponentDetailsForSecurityViolation(MAVEN_COORDINATES_V1);
    ComponentDetails details2 = createComponentDetailsForSecurityViolation(MAVEN_COORDINATES_V2);
    ComponentDetails details3 = createComponentDetailsForNoViolation(MAVEN_COORDINATES_V3);
    List<ComponentDetails> list = Stream.of(details1, details2, details3).collect(Collectors.toList());
    ComponentDetailsList detailsList = new ComponentDetailsList();
    detailsList.setList(list);
    mockComponentDetails(detailsList);

    // no violations / alerts - we expect component version 3
    ApiComponentDTOV2 expectedComponent = componentEvaluationV2Helper.createComponent(MAVEN_COORDINATES_V3, null);

    HttpResponse response =
        restRequest().path(PublicApiPaths.COMPONENT_REMEDIATION_PATH_V2).parameter(OwnerType.ORGANIZATION, org.getId())
            .body(component).post();

    assertResponse(response, expectedComponent, PackageUrlIdentifier.toPackageUrl(MAVEN_COORDINATES_V3), optionTypes);
  }

  private void createPolicyWithSecurityVulnerabilityConstraint(final String ownerId) {
    Policy policy = new Policy();
    policy.setName("Policy");
    policy.setThreatLevel(5);
    policy.setOwnerId(ownerId);
    Constraint constraint = new Constraint(null, "Test Constraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    policy.addConstraint(constraint);
    tempEntity.newPolicy(policy);
  }

  private ComponentDetails createComponentDetailsForNoViolation(final ComponentIdentifier componentIdentifier) {
    ComponentDetails componentDetails = new ComponentDetails();
    componentDetails.setComponentIdentifier(componentIdentifier);
    return componentDetails;
  }

  private ComponentDetails createComponentDetailsForSecurityViolation(final ComponentIdentifier componentIdentifier) {
    ComponentDetails componentDetails = createComponentDetailsForNoViolation(componentIdentifier);
    componentDetails.setLicenseThreatLevel(5);
    componentDetails
        .setSecurityVulnerabilities(Collections.singletonList(new SecurityVulnerability("ref", "source", 5f)));
    return componentDetails;
  }

  private void mockComponentDetails(final ComponentDetailsList componentEvaluationDataList) {
    hdsRespondWith(componentEvaluationDataList).atUri("rest/ci/componentDetails/list");
  }

  private void assertResponse(final HttpResponse response,
                              final ApiComponentDTOV2 expectedComponent,
                              final String expectedPackageUrl,
                              final ApiVersionChangeOptionType... optionTypes)
  {
    assertResponseStatus(200, response);
    String responseText = response.getBodyText();
    assertThat(responseText).doesNotContain("proprietary");

    ApiComponentRemediationDTO result = response.getBody(ApiComponentRemediationDTO.class);
    assertThat(result).isNotNull();
    assertThat(result.remediation.versionChanges).hasSize(optionTypes.length);
    for (int i = 0; i < optionTypes.length; i++) {
      ApiVersionChangeOptionDTO versionChangeOption = result.remediation.versionChanges.get(i);
      assertThat(versionChangeOption.getType()).isEqualTo(optionTypes[i]);
      assertThat(equalsMavenIdsIgnoringBlankClassifier(
          versionChangeOption.getData().getComponent().componentIdentifier.toComponentIdentifier(),
          expectedComponent.componentIdentifier.toComponentIdentifier())
      ).isTrue();
      assertThat(versionChangeOption.getData().getComponent().packageUrl).isEqualTo(expectedPackageUrl);
    }
  }

  private ComponentIdentifier componentIdentifierFrom(final String format, final String name, final String version) {
    final HashMap<String, String> coords = new HashMap<>();
    coords.put("name", name);
    coords.put(ComponentIdentifier.VERSION, version);
    return new ComponentIdentifier(format, coords);
  }

  private boolean equalsMavenIdsIgnoringBlankClassifier(ComponentIdentifier actual, ComponentIdentifier expected) {
    if (!actual.isMaven() || !expected.isMaven()) {
      return actual.equals(expected);
    }

    return actual.get(ComponentIdentifier.MAVEN_GROUP_ID).equals(expected.get(ComponentIdentifier.MAVEN_GROUP_ID))
        && actual.get(ComponentIdentifier.MAVEN_ARTIFACT_ID).equals(expected.get(ComponentIdentifier.MAVEN_ARTIFACT_ID))
        && actual.get(ComponentIdentifier.VERSION).equals(expected.get(ComponentIdentifier.VERSION))
        && actual.get(ComponentIdentifier.MAVEN_EXTENSION).equals(expected.get(ComponentIdentifier.MAVEN_EXTENSION))
        && ((StringUtils.isBlank(actual.get(ComponentIdentifier.MAVEN_CLASSIFIER))
              && StringUtils.isBlank(expected.get(ComponentIdentifier.MAVEN_CLASSIFIER)))
            || actual.get(ComponentIdentifier.MAVEN_CLASSIFIER).equals(
                expected.get(ComponentIdentifier.MAVEN_CLASSIFIER)));
  }
}
