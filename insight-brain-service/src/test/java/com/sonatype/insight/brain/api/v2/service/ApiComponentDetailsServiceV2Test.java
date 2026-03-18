/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataRequestList.ComponentEvaluationDataRequest;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.ComponentProjectDetails;
import com.sonatype.clm.dto.model.component.HygieneRating;
import com.sonatype.clm.dto.model.component.IntegrityRating;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDetailsDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDetailsResultDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentEvaluationRequestDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.google.inject.Binder;
import org.assertj.core.groups.Tuple;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

public class ApiComponentDetailsServiceV2Test
    extends AbstractComponentTest
{
  private static final String MISSING_COORDINATES = "The following coordinates are missing for given format: ";

  private static final int CHUNK_SIZE = 5;

  @Inject
  private ApiComponentDetailsServiceV2 apiComponentDetailsServiceV2;

  @Inject
  private PolicyDAO policyDAO;

  @Mock
  private HdsClient client;

  private ComponentEvaluationV2Helper componentEvaluationV2Helper;

  @Override
  public void configure(Binder binder) {
    binder.bind(HdsClient.class).toInstance(client);
    super.configure(binder);
  }

  @Before
  public void before() {
    componentEvaluationV2Helper = new ComponentEvaluationV2Helper(policyDAO);
    apiComponentDetailsServiceV2.setChunkSize(CHUNK_SIZE);
  }

  private void mockHdsRequest(
      ComponentEvaluationDataRequestList hdsRequest,
      ComponentIdentifier componentIdentifier,
      String hash)
  {
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components = new ArrayList<>();
    hdsResult.components.add(createComponentEvaluationData(componentIdentifier, hash));
    mockHdsRequest(hdsRequest, hdsResult);
  }

  private void mockHdsRequest(ComponentEvaluationDataRequestList hdsRequest, ComponentEvaluationDataList hdsResult) {
    doReturn(hdsResult).when(client)
        .post(eq(ComponentEvaluationDataList.class),
            eq(ApiComponentDetailsServiceV2.HDS_COMPONENT_DETAILS_PATH), eq(hdsRequest),
            eq(ApiComponentDetailsServiceV2.PURPOSE_INTEGRATION));
  }

  @Test
  public void testGetComponentDetails_chunked() {
    Set<License> declaredLicenseSet = Collections.singleton(new License("Apache-2.0", "Apache-2.0"));
    Set<License> observedLicenseSet = Collections.singleton(new License("ATT", "ATT"));
    List<SecurityVulnerability> securityVulnerabilities = componentEvaluationV2Helper.createSecurityVulnerabilities();
    ComponentProjectDetails componentProjectDetails = componentEvaluationV2Helper.createComponentProjectDetails();
    HygieneRating hygieneRating = new HygieneRating(1, "Laggard");
    IntegrityRating integrityRating = new IntegrityRating(1, "Pending");

    ApiComponentEvaluationRequestDTOV2 request = new ApiComponentEvaluationRequestDTOV2();

    int numChunks = 2;
    for (int chunk = 0; chunk < numChunks; chunk++) {
      ApiComponentEvaluationRequestDTOV2 requestChunk = new ApiComponentEvaluationRequestDTOV2();
      ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
      for (int i = 0; i < CHUNK_SIZE; i++) {
        int componentIndex = request.components.size();
        ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g" + componentIndex,
            "a" + componentIndex, "v" + componentIndex, "", "e" + componentIndex);
        ApiComponentDTOV2 component = componentEvaluationV2Helper.createComponent(componentIdentifier,
            "h" + componentIndex);
        component.packageUrl = PackageUrlIdentifier.toPackageUrl(componentIdentifier);
        request.components.add(component);
        requestChunk.components.add(component);

        hdsResult.components.add(componentEvaluationV2Helper
            .createComponentEvaluationData(componentIdentifier, component.hash, MatchState.EXACT, i,
                declaredLicenseSet, observedLicenseSet, securityVulnerabilities, componentIndex /* popularity */,
                componentProjectDetails, hygieneRating, integrityRating));
      }
      mockHdsRequest(componentEvaluationV2Helper.toHdsRequest(requestChunk), hdsResult);
    }
    int numComponents = CHUNK_SIZE * 2;

    ApiComponentDetailsResultDTOV2 result = apiComponentDetailsServiceV2.getComponentDetails(request);

    assertThat(result).isNotNull();
    assertThat(result.componentDetails).hasSize(numComponents);
    int i = 0;
    for (ApiComponentDetailsDTOV2 componentDetailsDTOV2 : result.componentDetails) {
      Set<License> effectiveLicenseSet = new HashSet<>();
      effectiveLicenseSet.addAll(declaredLicenseSet);
      effectiveLicenseSet.addAll(observedLicenseSet);
      assertComponentDetails(componentDetailsDTOV2, request.components.get(i), MatchState.EXACT.getId(),
          declaredLicenseSet, observedLicenseSet, effectiveLicenseSet, securityVulnerabilities, i /* popularity */,
          componentProjectDetails, hygieneRating, integrityRating);
      i++;
    }
  }

  @Test
  public void testGetComponentDetails_invalidComponentIdentifier_noCoordinates() throws Exception {
    String jsonRequest =
        "{\"components\":[{\"hash\":\"h1\",\"componentIdentifier\":{\"format\":\"maven\"},\"proprietary\":false}]}";
    ApiComponentEvaluationRequestDTOV2 request = JsonUtils.parse(jsonRequest, ApiComponentEvaluationRequestDTOV2.class);
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> apiComponentDetailsServiceV2.getComponentDetails(request))
        .withMessage("A component identifier must have at least one coordinate.");
  }

  @Test
  public void testGetComponentDetails_invalidComponentIdentifier_noExtension() {
    ApiComponentEvaluationRequestDTOV2 request = new ApiComponentEvaluationRequestDTOV2();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    ApiComponentDTOV2 component = componentEvaluationV2Helper.createComponent(componentIdentifier, "h1");
    request.components.add(component);

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> apiComponentDetailsServiceV2.getComponentDetails(request))
        .withMessage(MISSING_COORDINATES + "[extension]");
  }

  private ComponentEvaluationData createComponentEvaluationData(ComponentIdentifier componentIdentifier, String hash) {
    ComponentEvaluationData componentEvaluationData = new ComponentEvaluationData();
    componentEvaluationData.hash = hash;
    componentEvaluationData.componentIdentifier = componentIdentifier;
    componentEvaluationData.declaredLicenses = Collections.emptySet();
    componentEvaluationData.observedLicenses = Collections.emptySet();
    componentEvaluationData.securityVulnerabilities = Collections.emptyList();
    componentEvaluationData.matchState = MatchState.EXACT.getId();

    return componentEvaluationData;
  }

  @Test
  public void testGetComponentDetails_validation_nullComponentIdentifierAndNullPackageUrl() {
    ApiComponentEvaluationRequestDTOV2 request = new ApiComponentEvaluationRequestDTOV2();
    ApiComponentDTOV2 component = new ApiComponentDTOV2();
    component.hash = "h1";
    request.components.add(component);

    ComponentIdentifier resultComponentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    mockHdsRequest(componentEvaluationV2Helper.toHdsRequest(request), resultComponentIdentifier, component.hash);

    ApiComponentDetailsResultDTOV2 result = apiComponentDetailsServiceV2.getComponentDetails(request);

    assertThat(result).isNotNull();
    assertThat(result.componentDetails).hasSize(1);
    assertComponentDetails(result.componentDetails.get(0), resultComponentIdentifier, "h1",
        PackageUrlIdentifier.toPackageUrl(resultComponentIdentifier));
  }

  @Test
  public void testGetComponentDetails_validation_nullHashAndNullPackageUrl() {
    ApiComponentEvaluationRequestDTOV2 request = new ApiComponentEvaluationRequestDTOV2();
    ApiComponentDTOV2 component = new ApiComponentDTOV2();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "", "e1");
    component.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier);
    request.components.add(component);

    mockHdsRequest(componentEvaluationV2Helper.toHdsRequest(request), componentIdentifier, "h1");

    ApiComponentDetailsResultDTOV2 result = apiComponentDetailsServiceV2.getComponentDetails(request);

    assertThat(result).isNotNull();
    assertThat(result.componentDetails).hasSize(1);
    assertComponentDetails(result.componentDetails.get(0), componentIdentifier, "h1",
        PackageUrlIdentifier.toPackageUrl(componentIdentifier));
  }

  @Test
  public void testGetComponentDetails_validation_nullComponentIdentifierAndNullHash() {
    ApiComponentEvaluationRequestDTOV2 request = new ApiComponentEvaluationRequestDTOV2();
    ApiComponentDTOV2 component = new ApiComponentDTOV2();

    PackageUrlIdentifier packageURLIdentifier = new PackageUrlIdentifier("pkg:maven/g1/a1@v1?type=e1");
    component.packageUrl = packageURLIdentifier.getPackageUrl();
    request.components.add(component);

    mockHdsRequest(componentEvaluationV2Helper.toHdsRequest(request), packageURLIdentifier.toComponentIdentifier(),
        component.hash);

    ApiComponentDetailsResultDTOV2 result = apiComponentDetailsServiceV2.getComponentDetails(request);

    assertThat(result).isNotNull();
    assertThat(result.componentDetails).hasSize(1);
    assertComponentDetails(result.componentDetails.get(0), packageURLIdentifier.toComponentIdentifier(), null,
        packageURLIdentifier.getPackageUrl());
  }

  @Test
  public void testGetComponentDetails_validation_nullComponentIdentifierAndNullHashAndNullPackageUrl() {
    ApiComponentEvaluationRequestDTOV2 request = new ApiComponentEvaluationRequestDTOV2();
    ApiComponentDTOV2 component = new ApiComponentDTOV2();
    request.components.add(component);

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> apiComponentDetailsServiceV2.getComponentDetails(request))
        .withMessage("One of either componentIdentifier, packageUrl, or hash must be supplied.");
  }

  @Test
  public void testGetComponentDetails_nullComponents() {
    ApiComponentEvaluationRequestDTOV2 request = new ApiComponentEvaluationRequestDTOV2();
    request.components = null;

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> apiComponentDetailsServiceV2.getComponentDetails(request))
        .withMessage("No components provided in the request");
  }

  @Test
  public void testGetComponentDetails_emptyComponents() {
    ApiComponentEvaluationRequestDTOV2 request = new ApiComponentEvaluationRequestDTOV2();
    request.components = Collections.emptyList();

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> apiComponentDetailsServiceV2.getComponentDetails(request))
        .withMessage("No components provided in the request");
  }

  @Test
  public void testGetComponentDetails_nullRequest() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> apiComponentDetailsServiceV2.getComponentDetails(null))
        .withMessage("No components provided in the request");
  }

  @Test
  public void testGetComponentDetails_matchByComponentIdentifier() {
    ApiComponentEvaluationRequestDTOV2 request = new ApiComponentEvaluationRequestDTOV2();

    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1");
    ApiComponentDTOV2 component1 = componentEvaluationV2Helper.createComponent(componentIdentifier1, null);
    request.components.add(component1);

    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "c2", "e2");
    ApiComponentDTOV2 component2 = componentEvaluationV2Helper.createComponent(componentIdentifier2, null);
    request.components.add(component2);

    mockHdsRequest(componentEvaluationV2Helper.toHdsRequest(request), componentIdentifier1, "h1");

    ApiComponentDetailsResultDTOV2 result = apiComponentDetailsServiceV2.getComponentDetails(request);

    assertThat(result).isNotNull();
    assertThat(result.componentDetails).isNotNull();
    assertThat(result.componentDetails).hasSize(1);
    assertComponentDetails(result.componentDetails.get(0), componentIdentifier1, "h1",
        PackageUrlIdentifier.toPackageUrl(componentIdentifier1));
  }

  @Test
  public void testGetComponentDetails_matchByPackageUrl() {
    ApiComponentEvaluationRequestDTOV2 request = new ApiComponentEvaluationRequestDTOV2();

    PackageUrlIdentifier packageURLIdentifier = new PackageUrlIdentifier("pkg:maven/g1/a1@v1?classifier=c1&type=e1");

    ApiComponentDTOV2 component = componentEvaluationV2Helper.createComponent(packageURLIdentifier.getPackageUrl());
    request.components.add(component);

    mockHdsRequest(componentEvaluationV2Helper.toHdsRequest(request), packageURLIdentifier.toComponentIdentifier(),
        "h1");

    ApiComponentDetailsResultDTOV2 result = apiComponentDetailsServiceV2.getComponentDetails(request);

    assertThat(result).isNotNull();
    assertThat(result.componentDetails).isNotNull();
    assertThat(result.componentDetails).hasSize(1);
    assertComponentDetails(result.componentDetails.get(0), packageURLIdentifier.toComponentIdentifier(), "h1",
        packageURLIdentifier.getPackageUrl());
  }

  @Test
  public void testGetComponentDetails_multipleMatchByHash() {
    ApiComponentEvaluationRequestDTOV2 request = new ApiComponentEvaluationRequestDTOV2();

    ApiComponentDTOV2 component1 = componentEvaluationV2Helper.createComponent(null, "h1");
    request.components.add(component1);

    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1");
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "c2", "e2");
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components = new ArrayList<>();
    hdsResult.components.add(createComponentEvaluationData(componentIdentifier1, "h1"));
    hdsResult.components.add(createComponentEvaluationData(componentIdentifier2, "h1"));
    mockHdsRequest(componentEvaluationV2Helper.toHdsRequest(request), hdsResult);

    ApiComponentDetailsResultDTOV2 result = apiComponentDetailsServiceV2.getComponentDetails(request);

    assertThat(result).isNotNull();
    assertThat(result.componentDetails).hasSize(2);
    assertComponentDetails(result.componentDetails.get(0), componentIdentifier1, "h1",
        PackageUrlIdentifier.toPackageUrl(componentIdentifier1));
    assertComponentDetails(result.componentDetails.get(1), componentIdentifier2, "h1",
        PackageUrlIdentifier.toPackageUrl(componentIdentifier2));
  }

  @Test
  public void testGetComponentDetails_matchByLongHash() {
    ApiComponentEvaluationRequestDTOV2 request = new ApiComponentEvaluationRequestDTOV2();

    String hash = "12345678901234567890";
    String longHash = hash + "a";
    // The CLM request uses long hash
    ApiComponentDTOV2 component = componentEvaluationV2Helper.createComponent(null, longHash);
    request.components.add(component);

    // The HDS request uses short hash
    ComponentEvaluationDataRequestList hdsRequest = componentEvaluationV2Helper.toHdsRequest(request);
    hdsRequest.components.get(0).hash = hash;
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1");
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components = new ArrayList<>();
    hdsResult.components.add(createComponentEvaluationData(componentIdentifier, hash));
    mockHdsRequest(hdsRequest, hdsResult);

    ApiComponentDetailsResultDTOV2 result = apiComponentDetailsServiceV2.getComponentDetails(request);

    assertThat(result).isNotNull();
    assertThat(result.componentDetails).hasSize(1);
    assertComponentDetails(result.componentDetails.get(0), componentIdentifier, hash,
        PackageUrlIdentifier.toPackageUrl(componentIdentifier));
  }

  private void assertComponentDetails(
      ApiComponentDetailsDTOV2 resultComponentDTO,
      ComponentIdentifier expectedComponentIdentifier,
      String expectedHash,
      String expectedPackageUrl)
  {
    assertThat(resultComponentDTO).isNotNull();
    assertThat(resultComponentDTO.component).isNotNull();
    assertThat(resultComponentDTO.component.componentIdentifier.toComponentIdentifier())
        .isEqualTo(expectedComponentIdentifier);
    assertThat(resultComponentDTO.component.hash).isEqualTo(expectedHash);
    assertThat(resultComponentDTO.component.packageUrl).isEqualTo(expectedPackageUrl);
    assertThat(resultComponentDTO.component.displayName)
        .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(expectedComponentIdentifier).toString());
  }

  private void assertComponentDetails(
      ApiComponentDetailsDTOV2 resultComponentDTO,
      ApiComponentDTOV2 requestComponentDTO,
      String matchState,
      Set<License> declaredLicenses,
      Set<License> observedLicenses,
      Set<License> effectiveLicenses,
      List<SecurityVulnerability> securityVulnerabilities,
      Integer relativePopularity,
      ComponentProjectDetails projectDetails,
      HygieneRating hygieneRating,
      IntegrityRating integrityRating)
  {
    ApiComponentIdentifierDTOV2 expectedComponentIdentifier = requestComponentDTO.componentIdentifier;
    String expectedHash = requestComponentDTO.hash;
    String expectedPackageUrl = requestComponentDTO.packageUrl;

    assertThat(resultComponentDTO).isNotNull();
    assertThat(resultComponentDTO.component).isNotNull();
    assertThat(resultComponentDTO.component.componentIdentifier.toComponentIdentifier())
        .isEqualTo(expectedComponentIdentifier.toComponentIdentifier());
    assertThat(resultComponentDTO.component.hash).isEqualTo(expectedHash);
    assertThat(resultComponentDTO.component.packageUrl).isEqualTo(expectedPackageUrl);
    assertThat(resultComponentDTO.component.displayName).isEqualTo(
        ComponentDisplayNameUtil.fromIdentifier(expectedComponentIdentifier.toComponentIdentifier()).toString());
    assertThat(resultComponentDTO.matchState).isEqualTo(matchState);
    assertThat(resultComponentDTO.relativePopularity).isEqualTo(relativePopularity);

    assertThat(resultComponentDTO.hygieneRating).isEqualTo(hygieneRating.getLabel());
    assertThat(resultComponentDTO.integrityRating).isEqualTo(integrityRating.getLabel());

    assertThat(resultComponentDTO.licenseData).isNotNull();
    assertThat(resultComponentDTO.licenseData.declaredLicenses).extracting(dto -> dto.licenseId, dto -> dto.licenseName)
        .containsExactly(declaredLicenses.stream()
            .map(license -> tuple(license.getLicenseId(), license.getLicenseName()))
            .toArray(Tuple[]::new));

    assertThat(resultComponentDTO.licenseData.observedLicenses).extracting(dto -> dto.licenseId, dto -> dto.licenseName)
        .containsExactly(observedLicenses.stream()
            .map(license -> tuple(license.getLicenseId(), license.getLicenseName()))
            .toArray(Tuple[]::new));

    assertThat(resultComponentDTO.licenseData.effectiveLicenses)
        .extracting(dto -> dto.licenseId, dto -> dto.licenseName)
        .containsExactlyInAnyOrder(effectiveLicenses.stream()
            .map(license -> tuple(license.getLicenseId(), license.getLicenseName()))
            .toArray(Tuple[]::new));

    assertThat(resultComponentDTO.licenseData.overriddenLicenses).isNull();

    assertThat(resultComponentDTO.securityData).isNotNull();
    assertThat(resultComponentDTO.securityData.securityIssues).hasSameSizeAs(securityVulnerabilities);
    for (int i = 0; i < securityVulnerabilities.size(); i++) {
      assertThat(resultComponentDTO.securityData.securityIssues.get(i).source)
          .isEqualTo(securityVulnerabilities.get(i).getSource());
      assertThat(resultComponentDTO.securityData.securityIssues.get(i).reference)
          .isEqualTo(securityVulnerabilities.get(i).getRefId());
      assertThat(resultComponentDTO.securityData.securityIssues.get(i).severity)
          .isEqualTo(securityVulnerabilities.get(i).getSeverity());
      assertThat(resultComponentDTO.securityData.securityIssues.get(i).url)
          .isEqualTo(securityVulnerabilities.get(i).getUrl());
    }

    componentEvaluationV2Helper
        .assertComponentProjectDetails(resultComponentDTO.projectData, projectDetails);

    assertThat(resultComponentDTO.policyData).isNull();
  }

  @Test
  public void testConvert_ComponentIdentifier() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", null, "e");

    ComponentEvaluationDataRequest result = apiComponentDetailsServiceV2.convert(componentIdentifier);

    assertThat(result).isNotNull();
    assertThat(result.componentIdentifier).isEqualTo(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v", "", "e"));
  }
}
