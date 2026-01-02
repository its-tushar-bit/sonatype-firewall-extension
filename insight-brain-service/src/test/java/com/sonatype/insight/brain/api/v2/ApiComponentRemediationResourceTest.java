/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sonatype.clm.dto.model.ComponentSummary;
import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.clm.dto.model.component.ComponentDetailsList;
import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.api.v2.service.ComponentEvaluationV2Helper;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.hds.VersionScoringService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.dependency.ComponentDependenciesDTO;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.hds.VersionScoringService.HDS_BULK_SCORE_VERSIONING_PATH;
import static org.assertj.core.api.Assertions.assertThat;

@Category(SlowTest.class)
public class ApiComponentRemediationResourceTest
    extends AbstractResourceTest
{
  private static final ComponentIdentifier MAVEN_COORDINATES_V1 = ComponentIdentifier.createMavenCoordinates("g1", "a1",
      "v1", "", "jar");

  private static final ComponentIdentifier MAVEN_COORDINATES_V2 = ComponentIdentifier.createMavenCoordinates("g1", "a1",
      "v2", "", "jar");

  private static final ComponentIdentifier MAVEN_COORDINATES_V3 = ComponentIdentifier.createMavenCoordinates("g1", "a1",
      "v3", "", "jar");

  private ComponentEvaluationV2Helper componentEvaluationV2Helper;

  private Application app;

  private Organization org;

  @Before
  public void before() throws Exception {
    PolicyDAO policyDAO = lookup(PolicyDAO.class);
    componentEvaluationV2Helper = new ComponentEvaluationV2Helper(policyDAO);

    org = tempEntity.newOrganization("Org");
    app = tempEntity.newApplication(org.getId());
    setFeatures(LicensedFeature.ADVANCED_RECOMMENDATION_STRATEGIES, LicensedFeature.COMPONENT_EVALUATION);
    mockVersionScoring();
  }

  @Test
  public void testGetSuggestedRemediationForComponent_Application() throws Exception {
    ApiComponentDTOV2 component = componentEvaluationV2Helper.createComponent(MAVEN_COORDINATES_V1,null);
    assertRemediationApplication(
        component,
        ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES
    );
  }

  @Test
  public void testGetSuggestedRemediationForComponent_Application_includeParentRemediation_transitiveComponent()
      throws Exception
  {
    final String scanID = "scanID";
    createReportFile(app.getId(), scanID, "/ApiComponentRemediationResourceTest/extendReport");
    final ComponentIdentifier transitiveComponent =
        ComponentIdentifier.createMavenCoordinates("ch.qos.logback", "logback-core", "1.3.14", "", "jar");
    final ComponentIdentifier currentParentComponent =
        ComponentIdentifier.createMavenCoordinates("ch.qos.logback", "logback-classic", "1.3.14", "", "jar");
    final ComponentIdentifier newerVersionParentComponent1 =
        ComponentIdentifier.createMavenCoordinates("ch.qos.logback", "logback-classic", "1.3.15", "", "jar");
    final ComponentIdentifier newerVersionParentComponent2 =
        ComponentIdentifier.createMavenCoordinates("ch.qos.logback", "logback-classic", "1.3.16", "", "jar");

    mockComponentSummary(transitiveComponent, ComponentSummary.create(true));
    mockGetDependencies(new ComponentDependenciesDTO(new HashMap<>(), new HashMap<>()));

    ApiComponentDTOV2 transitiveComponentDTOV2 = componentEvaluationV2Helper.createComponent(transitiveComponent, null);

    createPolicyWithSecurityVulnerabilityConstraint(app.getId());

    ComponentEvaluationDataList componentEvaluationDataList = new ComponentEvaluationDataList();
    ComponentEvaluationData componentEvaluationData1 = new ComponentEvaluationData();
    componentEvaluationData1.declaredLicenses = Sets.newHashSet(new License("GPL-2.0", "GPL-2.0"));
    componentEvaluationData1.securityVulnerabilities =
        Collections.singletonList(new SecurityVulnerability("Test Ref Id", "Test Source", 7.5F));
    componentEvaluationData1.componentIdentifier = currentParentComponent;
    componentEvaluationDataList.components = new ArrayList<>();
    componentEvaluationDataList.components.add(componentEvaluationData1);
    ComponentEvaluationData componentEvaluationData2 = new ComponentEvaluationData();
    componentEvaluationData2.declaredLicenses = Sets.newHashSet(new License("GPL-2.0", "GPL-2.0"));
    componentEvaluationData2.securityVulnerabilities = Collections.emptyList();
    componentEvaluationData2.componentIdentifier = newerVersionParentComponent1;
    componentEvaluationDataList.components.add(componentEvaluationData2);
    ComponentEvaluationData componentEvaluationData3 = new ComponentEvaluationData();
    componentEvaluationData3.declaredLicenses = Sets.newHashSet(new License("GPL-2.0", "GPL-2.0"));
    componentEvaluationData3.securityVulnerabilities = Collections.emptyList();
    componentEvaluationData3.componentIdentifier = newerVersionParentComponent2;
    componentEvaluationDataList.components.add(componentEvaluationData3);

    mockComponentEvaluationData(componentEvaluationDataList);

    // Get the PURL without a version number
    PackageUrlIdentifier packageOnly = PackageUrlIdentifier
        .fromComponentIdentifier(currentParentComponent)
        .createAlternativeVersion(null);

    Map<String, List<String>> versionsByComponent = new LinkedHashMap<>();
    versionsByComponent.put(PackageUrlIdentifier.toPackageUrl(packageOnly.toComponentIdentifier()),
        Arrays.asList("1.3.14", "1.3.15", "1.3.16"));
    mockComponentVersionList(versionsByComponent);

    HttpResponse response = restRequest()
        .path(PublicApiPaths.COMPONENT_REMEDIATION_PATH_V2)
        .parameter(OwnerType.APPLICATION, app.getId())
        .query("scanId", scanID)
        .query("stageId", BuildStageType.ID)
        .query("includeParentRemediation", "true")
        .body(transitiveComponentDTOV2)
        .post();

    assertResponseStatus(200, response);
    ApiComponentRemediationDTO result = response.getBody(ApiComponentRemediationDTO.class);
    assertThat(result).isNotNull();
    assertThat(result.remediation.versionChanges).hasSize(2);

    //next-no-violations-with-dependencies should have newer version fix for parent component
    assertThat(result.remediation.versionChanges.get(0).getType()).isEqualTo(
        ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES);
    assertThat(result.remediation.versionChanges.get(0).getData()
        .getComponent().componentIdentifier.toComponentIdentifier()).isEqualTo(transitiveComponent);
    ApiVersionChangeOptionDTO transitiveVersionChangeOptionDTO = result.remediation.versionChanges.get(0);
    assertThat(transitiveVersionChangeOptionDTO.getDirectDependency()).isFalse();
    assertThat(transitiveVersionChangeOptionDTO.getDirectDependencyData().get(0)
        .getComponent().componentIdentifier.toComponentIdentifier()).isEqualTo(newerVersionParentComponent1);

    //next-no-failing-with-dependencies should have current version fix for parent component
    assertThat(result.remediation.versionChanges.get(1).getData()
        .getComponent().componentIdentifier.toComponentIdentifier()).isEqualTo(transitiveComponent);
    assertThat(result.remediation.versionChanges.get(1).getType()).isEqualTo(
        ApiVersionChangeOptionType.NEXT_NON_FAILING_WITH_DEPENDENCIES);
    transitiveVersionChangeOptionDTO = result.remediation.versionChanges.get(1);
    assertThat(transitiveVersionChangeOptionDTO.getDirectDependency()).isFalse();
    assertThat(transitiveVersionChangeOptionDTO.getDirectDependencyData().get(0)
        .getComponent().componentIdentifier.toComponentIdentifier()).isEqualTo(currentParentComponent);
  }

  @Test
  public void testGetSuggestedRemediationForComponent_Application_includeParentRemediation_directComponent()
      throws Exception
  {
    final String scanID = "scanID";
    createReportFile(app.getId(), scanID, "/ApiComponentRemediationResourceTest/extendReport");
    final ComponentIdentifier currentParentComponent =
        ComponentIdentifier.createMavenCoordinates("ch.qos.logback", "logback-classic", "1.3.14", "", "jar");
    final ComponentIdentifier newerVersionComponent1 =
        ComponentIdentifier.createMavenCoordinates("ch.qos.logback", "logback-classic", "1.3.15", "", "jar");
    final ComponentIdentifier newerVersionComponent2 =
        ComponentIdentifier.createMavenCoordinates("ch.qos.logback", "logback-classic", "1.3.16", "", "jar");

    ComponentDetails details1 = createComponentDetailsForSecurityViolation(currentParentComponent);
    ComponentDetails details2 = createComponentDetailsForSecurityViolation(newerVersionComponent1);
    ComponentDetails details3 = createComponentDetailsForNoViolation(newerVersionComponent2);
    List<ComponentDetails> list = Stream.of(details1, details2, details3).collect(Collectors.toList());
    ComponentDetailsList detailsList = new ComponentDetailsList();
    detailsList.setList(list);
    mockComponentDetails(detailsList);

    mockComponentSummary(currentParentComponent, ComponentSummary.create(true));
    mockGetDependencies(new ComponentDependenciesDTO(new HashMap<>(), new HashMap<>()));

    ApiComponentDTOV2 component = componentEvaluationV2Helper.createComponent(currentParentComponent, null);

    createPolicyWithSecurityVulnerabilityConstraint(app.getId());

    HttpResponse response = restRequest()
        .path(PublicApiPaths.COMPONENT_REMEDIATION_PATH_V2)
        .parameter(OwnerType.APPLICATION, app.getId())
        .query("scanId", scanID)
        .query("stageId", BuildStageType.ID)
        .query("includeParentRemediation", "true")
        .body(component)
        .post();

    assertResponseStatus(200, response);
    ApiComponentRemediationDTO result = response.getBody(ApiComponentRemediationDTO.class);
    assertThat(result).isNotNull();
    assertThat(result.remediation.versionChanges).hasSize(2);

    assertThat(result.remediation.versionChanges.get(0).getType()).isEqualTo(
        ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES);
    assertThat(result.remediation.versionChanges.get(0).getDirectDependency()).isTrue();
    assertThat(result.remediation.versionChanges.get(0).getDirectDependencyData()).isEmpty();
    assertThat(result.remediation.versionChanges.get(0).getData()
        .getComponent().componentIdentifier.toComponentIdentifier()).isEqualTo(newerVersionComponent2);

    assertThat(result.remediation.versionChanges.get(1).getType()).isEqualTo(
        ApiVersionChangeOptionType.NEXT_NON_FAILING_WITH_DEPENDENCIES);
    assertThat(result.remediation.versionChanges.get(1).getDirectDependency()).isTrue();
    assertThat(result.remediation.versionChanges.get(1).getDirectDependencyData()).isEmpty();
    assertThat(result.remediation.versionChanges.get(1).getData()
        .getComponent().componentIdentifier.toComponentIdentifier()).isEqualTo(currentParentComponent);
  }

  @Test
  public void testIncludeParentRemediation_WithoutDirectFlags_UsesTreeStructure()
      throws Exception
  {
    final String scanID = "scanID";
    createReportFile(app.getId(), scanID,
        "/ApiComponentRemediationResourceTest/parent-remediation-without-direct-flags");

    // pdfbox is a transitive dependency through easytable
    final ComponentIdentifier transitiveComponent =
        ComponentIdentifier.createMavenCoordinates("org.apache.pdfbox", "pdfbox", "2.0.19", "", "jar");

    // easytable is the direct dependency
    final ComponentIdentifier currentParentComponent =
        ComponentIdentifier.createMavenCoordinates("com.github.vandeseer", "easytable", "0.8.5", "", "jar");

    // Newer versions of easytable with updated pdfbox
    final ComponentIdentifier newerVersionParentComponent1 =
        ComponentIdentifier.createMavenCoordinates("com.github.vandeseer", "easytable", "0.8.6", "", "jar");
    final ComponentIdentifier newerVersionParentComponent2 =
        ComponentIdentifier.createMavenCoordinates("com.github.vandeseer", "easytable", "0.8.7", "", "jar");

    mockComponentSummary(transitiveComponent, ComponentSummary.create(true));
    mockGetDependencies(new ComponentDependenciesDTO(new HashMap<>(), new HashMap<>()));

    ApiComponentDTOV2 transitiveComponentDTOV2 = componentEvaluationV2Helper.createComponent(transitiveComponent, null);

    createPolicyWithSecurityVulnerabilityConstraint(app.getId());

    ComponentEvaluationDataList componentEvaluationDataList = new ComponentEvaluationDataList();
    ComponentEvaluationData componentEvaluationData1 = new ComponentEvaluationData();
    componentEvaluationData1.declaredLicenses = Set.of(new License("Apache-2.0", "Apache-2.0"));
    componentEvaluationData1.securityVulnerabilities =
        List.of(new SecurityVulnerability("Test Ref Id", "Test Source", 7.5F));
    componentEvaluationData1.componentIdentifier = currentParentComponent;
    componentEvaluationDataList.components = new ArrayList<>();
    componentEvaluationDataList.components.add(componentEvaluationData1);
    ComponentEvaluationData componentEvaluationData2 = new ComponentEvaluationData();
    componentEvaluationData2.declaredLicenses = Set.of(new License("Apache-2.0", "Apache-2.0"));
    componentEvaluationData2.securityVulnerabilities = Collections.emptyList();
    componentEvaluationData2.componentIdentifier = newerVersionParentComponent1;
    componentEvaluationDataList.components.add(componentEvaluationData2);
    ComponentEvaluationData componentEvaluationData3 = new ComponentEvaluationData();
    componentEvaluationData3.declaredLicenses = Set.of(new License("Apache-2.0", "Apache-2.0"));
    componentEvaluationData3.securityVulnerabilities = Collections.emptyList();
    componentEvaluationData3.componentIdentifier = newerVersionParentComponent2;
    componentEvaluationDataList.components.add(componentEvaluationData3);

    mockComponentEvaluationData(componentEvaluationDataList);

    // Get the PURL without a version number
    PackageUrlIdentifier packageOnly = PackageUrlIdentifier
        .fromComponentIdentifier(currentParentComponent)
        .createAlternativeVersion(null);

    Map<String, List<String>> versionsByComponent = new LinkedHashMap<>();
    versionsByComponent.put(PackageUrlIdentifier.toPackageUrl(packageOnly.toComponentIdentifier()),
        Arrays.asList("0.8.5", "0.8.6", "0.8.7"));
    mockComponentVersionList(versionsByComponent);

    HttpResponse response = restRequest()
        .path(PublicApiPaths.COMPONENT_REMEDIATION_PATH_V2)
        .parameter(OwnerType.APPLICATION, app.getId())
        .query("scanId", scanID)
        .query("stageId", BuildStageType.ID)
        .query("includeParentRemediation", "true")
        .body(transitiveComponentDTOV2)
        .post();

    // The feature works by using the dependency tree structure when 'direct' flags are missing.
    // ApiReportDataServiceV2 populates 'direct' flags based on tree position (root children = direct).
    assertResponseStatus(200, response);
    ApiComponentRemediationDTO result = response.getBody(ApiComponentRemediationDTO.class);
    assertThat(result).isNotNull();
    assertThat(result.remediation.versionChanges).hasSize(2);

    //next-no-violations-with-dependencies should have newer version fix for parent component
    assertThat(result.remediation.versionChanges.get(0).getType()).isEqualTo(
        ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES);
    assertThat(result.remediation.versionChanges.get(0).getData()
        .getComponent().componentIdentifier.toComponentIdentifier()).isEqualTo(transitiveComponent);
    ApiVersionChangeOptionDTO transitiveVersionChangeOptionDTO = result.remediation.versionChanges.get(0);
    assertThat(transitiveVersionChangeOptionDTO.getDirectDependency()).isFalse();
    assertThat(transitiveVersionChangeOptionDTO.getDirectDependencyData().get(0)
        .getComponent().componentIdentifier.toComponentIdentifier()).isEqualTo(newerVersionParentComponent1);

    //next-no-failing-with-dependencies should have current version fix for parent component
    assertThat(result.remediation.versionChanges.get(1).getData()
        .getComponent().componentIdentifier.toComponentIdentifier()).isEqualTo(transitiveComponent);
    assertThat(result.remediation.versionChanges.get(1).getType()).isEqualTo(
        ApiVersionChangeOptionType.NEXT_NON_FAILING_WITH_DEPENDENCIES);
    transitiveVersionChangeOptionDTO = result.remediation.versionChanges.get(1);
    assertThat(transitiveVersionChangeOptionDTO.getDirectDependency()).isFalse();
    assertThat(transitiveVersionChangeOptionDTO.getDirectDependencyData().get(0)
        .getComponent().componentIdentifier.toComponentIdentifier()).isEqualTo(currentParentComponent);
  }

  @Test
  public void testGetSuggestedRemediationForComponent_Application_ThirdParty_NoViolations() throws Exception {
    testGetSuggestedRemediationForComponent_Application_ThirdParty(1);
  }

  @Test
  public void testGetSuggestedRemediationForComponent_Application_ThirdParty_WithViolation() throws Exception {
    createPolicyWithSecurityVulnerabilityConstraint(org.getId());
    testGetSuggestedRemediationForComponent_Application_ThirdParty(1);
  }

  private void testGetSuggestedRemediationForComponent_Application_ThirdParty(
      final int expectedRemediationVersionsCount) throws Exception
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
    assertThat(versionChangeDTO.getData().getComponent().displayName)
        .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(versionChangeDTO.getData().getComponent().componentIdentifier
            .toComponentIdentifier()).toString());
  }

  @Test
  public void testGetSuggestedRemediationForComponent_Application_ThirdParty_ByPurl() throws Exception {
    String scanId = "ScanId";
    createReportFile(app.getId(), scanId, "/ApiComponentRemediationResourceTest/report");
    ComponentIdentifier tpComponentIdentifier = componentIdentifierFrom("debian-9", "glibc", "2.24-11+deb9u3");
    ApiComponentDTOV2 apiComponentDTOV2 = new ApiComponentDTOV2();
    apiComponentDTOV2.packageUrl = PackageUrlIdentifier.fromComponentIdentifier(tpComponentIdentifier).getPackageUrl();
    String identificationSource = IdentificationSource.CLAIR.getId();

    HttpResponse response = restRequest()
        .path(PublicApiPaths.COMPONENT_REMEDIATION_PATH_V2).parameter(OwnerType.APPLICATION, app.getId())
        .query("identificationSource", identificationSource).query("scanId", scanId)
        .body(apiComponentDTOV2).post();

    assertResponseStatus(200, response);
    String responseText = response.getBodyText();
    assertThat(responseText).doesNotContain("proprietary");
    ApiComponentRemediationDTO result = response.getBody(ApiComponentRemediationDTO.class);
    assertThat(result).isNotNull();
    assertThat(result.remediation.versionChanges).hasSize(1);
    ApiVersionChangeOptionDTO versionChangeDTO = result.remediation.versionChanges.get(0);
    assertThat(versionChangeDTO.getType()).isEqualTo(ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS);
    assertThat(versionChangeDTO.getData().getComponent().packageUrl).isEqualTo("pkg:debian-9/glibc@3.47-32%2Bdeb9u1");
    assertThat(versionChangeDTO.getData().getComponent().displayName)
        .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(versionChangeDTO.getData().getComponent().componentIdentifier
            .toComponentIdentifier()).toString());
  }

  @Test
  public void testGetSuggestedRemediationForComponent_Application_Purl() throws Exception {
    String purl = "pkg:maven/g1/a1@v1?type=jar";
    ApiComponentDTOV2 component = componentEvaluationV2Helper.createComponent(purl);
    assertRemediationApplication(
        component,
        ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES
    );
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
  public void testGetSuggestedRemediationForComponent_Organization() throws Exception {
    ApiComponentDTOV2 component = componentEvaluationV2Helper.createComponent(MAVEN_COORDINATES_V1, null);
    assertRemediationOrganization(
        component,
        ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES
    );
  }
  
  @Test
  public void testGetSuggestedRemediationForComponent_Organization_Purl() throws Exception {
    String purl = "pkg:maven/g1/a1@v1?type=jar";
    ApiComponentDTOV2 component = componentEvaluationV2Helper.createComponent(purl);
    assertRemediationOrganization(
        component,
        ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES
    );
  }

  @Test
  public void testGetSuggestedRemediationForComponent_Repository() throws Exception {
    Repository repo = tempEntity.newRepository();
    ApiComponentDTOV2 component = componentEvaluationV2Helper.createComponent(MAVEN_COORDINATES_V1, null);
    mockComponentSummary(MAVEN_COORDINATES_V1, ComponentSummary.create(true));
    mockGetDependencies(new ComponentDependenciesDTO(new HashMap<>(), new HashMap<>()));
    createPolicyWithSecurityVulnerabilityConstraint(RepositoryContainer.REPOSITORY_CONTAINER_ID);

    ComponentDetails componentDetails1 = createComponentDetailsForSecurityViolation(MAVEN_COORDINATES_V1);
    ComponentDetails componentDetails2 = createComponentDetailsForSecurityViolation(MAVEN_COORDINATES_V2);
    ComponentDetails componentDetails3 = createComponentDetailsForNoViolation(MAVEN_COORDINATES_V3);
    ComponentDetailsList componentDetailsList = new ComponentDetailsList();
    componentDetailsList.setList(Arrays.asList(componentDetails1, componentDetails2, componentDetails3));
    mockComponentDetails(componentDetailsList);

    // no violations / alerts - we expect component version 3
    ApiComponentDTOV2 expectedComponentNoViolations =
        componentEvaluationV2Helper.createComponent(MAVEN_COORDINATES_V3, null);
    // non-failing violations / alerts - we expect component version 1
    ApiComponentDTOV2 expectedComponentNonFailing =
        componentEvaluationV2Helper.createComponent(MAVEN_COORDINATES_V1, null);

    HttpResponse response = restRequest().path(PublicApiPaths.COMPONENT_REMEDIATION_PATH_V2)
        .parameter(OwnerType.REPOSITORY, repo.getId()).body(component).post();

    assertResponse(
        response,
        expectedComponentNoViolations,
        PackageUrlIdentifier.toPackageUrl(MAVEN_COORDINATES_V3),
        expectedComponentNonFailing,
        PackageUrlIdentifier.toPackageUrl(MAVEN_COORDINATES_V1),
        ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES,
        ApiVersionChangeOptionType.NEXT_NON_FAILING_WITH_DEPENDENCIES);
  }

  @Test
  public void testGetSuggestedRemediationForComponent_SpecifiedStage() throws Exception {
    ApiComponentDTOV2 component = componentEvaluationV2Helper.createComponent(MAVEN_COORDINATES_V1, null);
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
    ApiComponentDTOV2 expectedComponentNoViolations =
        componentEvaluationV2Helper.createComponent(MAVEN_COORDINATES_V3, null);
    // non-failing violations / alerts - we expect component version 1
    ApiComponentDTOV2 expectedComponentNonFailing =
        componentEvaluationV2Helper.createComponent(MAVEN_COORDINATES_V1, null);

    HttpResponse response = restRequest().path(PublicApiPaths.COMPONENT_REMEDIATION_PATH_V2)
        .parameter(OwnerType.APPLICATION, app.getId()).query("stageId", BuildStageType.ID).body(component).post();

    assertResponse(
        response,
        expectedComponentNoViolations,
        PackageUrlIdentifier.toPackageUrl(MAVEN_COORDINATES_V3),
        expectedComponentNonFailing,
        PackageUrlIdentifier.toPackageUrl(MAVEN_COORDINATES_V1),
        ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES,
        ApiVersionChangeOptionType.NEXT_NON_FAILING_WITH_DEPENDENCIES);
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

  private void mockComponentVersionList(final Map<String, List<String>> versionsByComponent) {
    hdsRespondWith(versionsByComponent).atUri("rest/component/versions/list");
  }

  private void mockComponentEvaluationData(final ComponentEvaluationDataList componentEvaluationDataList) {
    hdsRespondWith(componentEvaluationDataList).atUri("rest/component/details/integration");
  }

  private void mockVersionScoring() {
    hdsRespondWith(new VersionScoringService[] {}).atUri(HDS_BULK_SCORE_VERSIONING_PATH);
  }

  private void assertResponse(
      final HttpResponse response,
      final ApiComponentDTOV2 expectedComponent,
      final String expectedPackageUrl,
      final ApiVersionChangeOptionType... optionTypes)
  {
    assertResponse(response, expectedComponent, expectedPackageUrl, expectedComponent, expectedPackageUrl, optionTypes);
  }

  private void assertResponse(
      HttpResponse response,
      ApiComponentDTOV2 expectedComponentNoViolations,
      String expectedPackageUrlNoViolations,
      ApiComponentDTOV2 expectedComponentNonFailing,
      String expectedPackageUrlNonFailing,
      ApiVersionChangeOptionType... optionTypes)
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

      switch (optionTypes[i]) {
        case NEXT_NO_VIOLATIONS:
        case NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES:
          assertThat(versionChangeOption.getData().getComponent().componentIdentifier.toComponentIdentifier())
              .isEqualTo(expectedComponentNoViolations.componentIdentifier.toComponentIdentifier());
          assertThat(versionChangeOption.getData().getComponent().packageUrl).isEqualTo(expectedPackageUrlNoViolations);
          assertThat(versionChangeOption.getData().getComponent().displayName).isEqualTo(ComponentDisplayNameUtil
              .fromIdentifier(expectedComponentNoViolations.componentIdentifier.toComponentIdentifier()).toString());
          break;
        case NEXT_NON_FAILING:
        case NEXT_NON_FAILING_WITH_DEPENDENCIES:
          assertThat(versionChangeOption.getData().getComponent().componentIdentifier.toComponentIdentifier())
              .isEqualTo(expectedComponentNonFailing.componentIdentifier.toComponentIdentifier());
          assertThat(versionChangeOption.getData().getComponent().packageUrl).isEqualTo(expectedPackageUrlNonFailing);
          assertThat(versionChangeOption.getData().getComponent().displayName).isEqualTo(ComponentDisplayNameUtil
              .fromIdentifier(expectedComponentNonFailing.componentIdentifier.toComponentIdentifier()).toString());
          break;
        default:
          throw new RuntimeException("Unknown ApiVersionChangeOptionType:" + optionTypes[i]);
      }
    }
  }

  private ComponentIdentifier componentIdentifierFrom(final String format, final String name, final String version) {
    final HashMap<String, String> coords = new HashMap<>();
    coords.put("name", name);
    coords.put(ComponentIdentifier.VERSION, version);
    return new ComponentIdentifier(format, coords);
  }
}
