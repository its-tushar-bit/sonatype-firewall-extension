/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.ArrayList;
import java.util.HashSet;

import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDetailsDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDetailsResultDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentEvaluationRequestDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.service.ComponentEvaluationV2Helper;
import com.sonatype.insight.brain.api.v2.service.ApiComponentDetailsServiceV2;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class ApiComponentDetailsResourceV2Test
    extends AbstractResourceTest
{
  private ComponentEvaluationV2Helper componentEvaluationV2Helper;

  @Before
  public void setUp() {
    PolicyDAO policyDAO = lookup(PolicyDAO.class);
    componentEvaluationV2Helper = new ComponentEvaluationV2Helper(policyDAO);
  }

  @Test
  public void testGetComponentDetails() throws Exception {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1");
    String packageUrl = PackageUrlIdentifier.toPackageUrl(componentIdentifier);
    ApiComponentDTOV2 component = componentEvaluationV2Helper.createComponent(componentIdentifier, null);

    assertGetComponentDetails(componentIdentifier, component, packageUrl);
  }

  @Test
  public void testGetComponentDetails_Purl() throws Exception {
    PackageUrlIdentifier packageURLIdentifier = new PackageUrlIdentifier("pkg:maven/g1/a1@v1?type=e1");
    ApiComponentDTOV2 component =
        componentEvaluationV2Helper.createComponent(packageURLIdentifier.getPackageUrl());

    assertGetComponentDetails(packageURLIdentifier.toComponentIdentifier(), component,
        packageURLIdentifier.getPackageUrl());
  }

  private ComponentEvaluationData createComponentEvaluationData(ComponentIdentifier componentIdentifier, String hash) {
    ComponentEvaluationData componentEvaluationData = new ComponentEvaluationData();
    componentEvaluationData.hash = hash;
    componentEvaluationData.componentIdentifier = componentIdentifier;
    componentEvaluationData.declaredLicenses = new HashSet<>();
    componentEvaluationData.observedLicenses = new HashSet<>();
    componentEvaluationData.securityVulnerabilities = new ArrayList<>();
    componentEvaluationData.matchState = MatchState.EXACT.getId();
    componentEvaluationData.componentProjectDetails = componentEvaluationV2Helper.createComponentProjectDetails();

    return componentEvaluationData;
  }

  private void mockComponentDetails(final ComponentEvaluationDataList componentEvaluationDataList) {
    hdsRespondWith(componentEvaluationDataList).atUri(ApiComponentDetailsServiceV2.HDS_COMPONENT_DETAILS_PATH
        .replace("{purpose: evaluation|integration}", ApiComponentDetailsServiceV2.PURPOSE_INTEGRATION));
  }

  private void assertGetComponentDetails(
      final ComponentIdentifier componentIdentifier,
      final ApiComponentDTOV2 component,
      final String expectedPackageUrl) throws Exception
  {
    ApiComponentEvaluationRequestDTOV2 request = new ApiComponentEvaluationRequestDTOV2();
    request.components.add(component);

    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components = new ArrayList<>();
    ComponentEvaluationData componentData = createComponentEvaluationData(componentIdentifier, "h1");
    componentData.declaredLicenses.add(new License("Apache-2.0", "Apache-2.0"));
    componentData.observedLicenses.add(new License("GPL-2.0", "GPL-2.0"));
    componentData.securityVulnerabilities.add(new SecurityVulnerability("SOME-REFID", "Some source", 5F));
    hdsResult.components.add(componentData);
    mockComponentDetails(hdsResult);

    HttpResponse response = restRequest().path(PublicApiPaths.COMPONENT_DETAILS_PATH_V2).body(request).post();
    assertResponseStatus(200, response);

    String responseText = response.getBodyText();
    assertThat(responseText).doesNotContain("proprietary", "overriddenLicenses", "status", "policyData");

    ApiComponentDetailsResultDTOV2 result = response.getBody(ApiComponentDetailsResultDTOV2.class);
    assertThat(result).isNotNull();
    assertThat(result.componentDetails).hasSize(1);
    ApiComponentDetailsDTOV2 componentDetails = result.componentDetails.get(0);
    assertThat(componentDetails).isNotNull();
    assertThat(componentDetails.component).isNotNull();
    assertThat(ApiComponentIdentifierDTOV2.toComponentIdentifier(componentDetails.component.componentIdentifier))
        .isEqualTo(componentIdentifier);
    assertThat(componentDetails.component.hash).isEqualTo("h1");
    assertThat(componentDetails.component.packageUrl).isEqualTo(expectedPackageUrl);
    assertThat(componentDetails.component.displayName)
        .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(componentIdentifier).toString());
    assertThat(componentDetails.licenseData.declaredLicenses).extracting(dto -> dto.licenseId)
        .containsExactlyInAnyOrder("Apache-2.0");
    assertThat(componentDetails.licenseData.observedLicenses).extracting(dto -> dto.licenseId)
        .containsExactlyInAnyOrder("GPL-2.0");
    assertThat(componentDetails.securityData.securityIssues).extracting(dto -> dto.reference)
        .containsExactlyInAnyOrder("SOME-REFID");
    componentEvaluationV2Helper
        .assertComponentProjectDetails(componentDetails.projectData, componentData.componentProjectDetails);
    assertThat(componentDetails.projectData).isNotNull();
  }
}
