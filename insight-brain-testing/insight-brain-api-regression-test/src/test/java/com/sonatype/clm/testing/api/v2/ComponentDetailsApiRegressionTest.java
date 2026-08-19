/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.api.v2;

import java.util.ArrayList;
import java.util.HashSet;

import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.api.AbstractIqApiTest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentEvaluationRequestDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.service.ApiComponentDetailsServiceV2;
import com.sonatype.insight.brain.model.component.MatchState;

import org.junit.jupiter.api.Test;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * API regression suite for {@code api/v2/components/details}.
 *
 * <p>
 * Covers: component details by GAV coordinates (license + security data returned, no
 * proprietary/policyData fields); by package URL; and the unauthenticated auth contract (401).
 *
 * <p>
 * This endpoint is stateless — no {@code tempEntity} seeding is required. All data comes
 * from the HDS stub at {@code rest/component/details/integration}.
 *
 * <p>
 * No {@code setFeatures(LicensedFeature.COMPONENT_EVALUATION)} call is required — the test
 * harness enables all licensed features by default.
 */
public class ComponentDetailsApiRegressionTest
    extends AbstractIqApiTest
{
  private static final String DETAILS_PATH = PublicApiPaths.COMPONENT_DETAILS_PATH_V2;

  private static final String HDS_DETAILS_URI = ApiComponentDetailsServiceV2.HDS_COMPONENT_DETAILS_PATH
      .replace("{purpose: evaluation|integration}", ApiComponentDetailsServiceV2.PURPOSE_INTEGRATION);

  private static final String GROUP_ID = "g1";

  private static final String ARTIFACT_ID = "a1";

  private static final String VERSION = "v1";

  private static final String EXTENSION = "e1";

  @Test
  public void testGetComponentDetails_byCoordinates_returns200() throws Exception {
    ComponentIdentifier coords =
        ComponentIdentifier.createMavenCoordinates(GROUP_ID, ARTIFACT_ID, VERSION, "c1", EXTENSION);

    ApiComponentEvaluationRequestDTOV2 request = new ApiComponentEvaluationRequestDTOV2();
    ApiComponentDTOV2 component = new ApiComponentDTOV2();
    component.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(coords);
    request.components.add(component);

    hdsRespondWith(buildHdsResult(coords)).atUri(HDS_DETAILS_URI);

    HttpResponse response = apiPostJson(DETAILS_PATH, request);

    assertResponseStatus(200, response);
    String body = response.getBodyText();
    assertThat(body).doesNotContain("\"proprietary\"", "\"overriddenLicenses\"", "\"policyData\"");
    assertThatJson(body).node("componentDetails").isArray().hasSize(1);
    assertThatJson(body).node("componentDetails[0].licenseData.declaredLicenses").isArray().isNotEmpty();
    assertThatJson(body).node("componentDetails[0].securityData.securityIssues").isArray().isNotEmpty();
  }

  @Test
  public void testGetComponentDetails_byPackageUrl_returns200() throws Exception {
    String packageUrl = "pkg:maven/" + GROUP_ID + "/" + ARTIFACT_ID + "@" + VERSION + "?type=" + EXTENSION;
    ComponentIdentifier coords =
        ComponentIdentifier.createMavenCoordinates(GROUP_ID, ARTIFACT_ID, VERSION, "", EXTENSION);

    ApiComponentEvaluationRequestDTOV2 request = new ApiComponentEvaluationRequestDTOV2();
    ApiComponentDTOV2 component = new ApiComponentDTOV2();
    component.packageUrl = packageUrl;
    request.components.add(component);

    hdsRespondWith(buildHdsResult(coords)).atUri(HDS_DETAILS_URI);

    HttpResponse response = apiPostJson(DETAILS_PATH, request);

    assertResponseStatus(200, response);
    assertThatJson(response.getBodyText()).node("componentDetails").isArray().hasSize(1);
    assertThatJson(response.getBodyText())
        .node("componentDetails[0].component.packageUrl")
        .isString()
        .isNotEmpty();
  }

  @Test
  public void testGetComponentDetails_emptyComponents_returns400() throws Exception {
    // Empty components list — ApiComponentDetailsServiceV2.validateRequest rejects with 400
    HttpResponse response = apiPostJson(DETAILS_PATH, new ApiComponentEvaluationRequestDTOV2());
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).containsIgnoringCase("No components provided");
  }

  /** Auth contract on POST: unauthenticated callers get 401 before the body is parsed. */
  @Test
  public void testGetComponentDetails_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiPostJson(DETAILS_PATH, new ApiComponentEvaluationRequestDTOV2());
    assertResponseStatus(401, response);
  }

  private ComponentEvaluationDataList buildHdsResult(ComponentIdentifier coords) {
    ComponentEvaluationData data = new ComponentEvaluationData();
    data.hash = "h1";
    data.componentIdentifier = coords;
    data.declaredLicenses = new HashSet<>();
    data.declaredLicenses.add(new License("Apache-2.0", "Apache-2.0"));
    data.observedLicenses = new HashSet<>();
    data.securityVulnerabilities = new ArrayList<>();
    data.securityVulnerabilities.add(new SecurityVulnerability("CVE-2024-00001", "NVD", 7.5F));
    data.matchState = MatchState.EXACT.getId();

    ComponentEvaluationDataList result = new ComponentEvaluationDataList();
    result.components = new ArrayList<>();
    result.components.add(data);
    return result;
  }
}
