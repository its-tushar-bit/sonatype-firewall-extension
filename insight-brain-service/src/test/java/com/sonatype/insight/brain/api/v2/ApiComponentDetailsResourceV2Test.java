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
import com.sonatype.insight.brain.api.v2.service.ApiComponentDetailsServiceV2;
import com.sonatype.insight.brain.api.v2.service.ComponentEvaluationV2Helper;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

public class ApiComponentDetailsResourceV2Test
    extends AbstractResourceTest
{
  private ComponentEvaluationV2Helper componentEvaluationV2Helper = new ComponentEvaluationV2Helper();

  @Test
  public void testGetComponentDetails() throws Exception {
    ApiComponentEvaluationRequestDTOV2 request = new ApiComponentEvaluationRequestDTOV2();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1");
    ApiComponentDTOV2 component = componentEvaluationV2Helper.createComponent(componentIdentifier, null);
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
    assertThat(responseText, not(containsString("proprietary")));
    assertThat(responseText, not(containsString("overriddenLicenses")));
    assertThat(responseText, not(containsString("status")));
    assertThat(responseText, not(containsString("policyData")));

    ApiComponentDetailsResultDTOV2 result = response.getBody(ApiComponentDetailsResultDTOV2.class);
    assertThat(result, notNullValue());
    assertThat(result.componentDetails, notNullValue());
    assertThat(result.componentDetails, hasSize(1));
    ApiComponentDetailsDTOV2 componentDetails = result.componentDetails.get(0);
    assertThat(componentDetails, notNullValue());
    assertThat(componentDetails.component, notNullValue());
    ApiComponentIdentifierDTOV2 expectedComponentIdentifier = ApiComponentIdentifierDTOV2
        .fromComponentIdentifier(componentIdentifier);
    assertThat(componentDetails.component.componentIdentifier.getFormat(), is(expectedComponentIdentifier.getFormat()));
    assertThat(componentDetails.component.componentIdentifier.getCoordinates(),
        is(expectedComponentIdentifier.getCoordinates()));
    assertThat(componentDetails.component.hash, is("h1"));
    assertThat(componentDetails.licenseData.declaredLicenses, hasSize(1));
    assertThat(componentDetails.licenseData.declaredLicenses.get(0).licenseId, is("Apache-2.0"));
    assertThat(componentDetails.licenseData.observedLicenses, hasSize(1));
    assertThat(componentDetails.licenseData.observedLicenses.get(0).licenseId, is("GPL-2.0"));
    assertThat(componentDetails.securityData.securityIssues, hasSize(1));
    assertThat(componentDetails.securityData.securityIssues.get(0).reference, is("SOME-REFID"));
  }

  private ComponentEvaluationData createComponentEvaluationData(ComponentIdentifier componentIdentifier, String hash) {
    ComponentEvaluationData componentEvaluationData = new ComponentEvaluationData();
    componentEvaluationData.hash = hash;
    componentEvaluationData.componentIdentifier = componentIdentifier;
    componentEvaluationData.declaredLicenses = new HashSet<>();
    componentEvaluationData.observedLicenses = new HashSet<>();
    componentEvaluationData.securityVulnerabilities = new ArrayList<>();
    componentEvaluationData.matchState = MatchState.EXACT.getId();

    return componentEvaluationData;
  }

  private void mockComponentDetails(final ComponentEvaluationDataList componentEvaluationDataList) {
    setHdsResponseForURI(ApiComponentDetailsServiceV2.HDS_COMPONENT_DETAILS_PATH.replace(
        "{purpose: evaluation|integration}", ApiComponentDetailsServiceV2.PURPOSE_INTEGRATION),
        componentEvaluationDataList, 200);
  }
}
