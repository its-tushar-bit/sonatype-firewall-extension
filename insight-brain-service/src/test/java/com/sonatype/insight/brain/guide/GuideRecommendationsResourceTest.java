/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sonatype.guide.api.dto.RecommendationResponse;
import com.sonatype.guide.api.request.RecommendationRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.guide.api.dto.GuideRecommendationResult;
import com.sonatype.insight.brain.guide.api.dto.RecommendedVersionInfo;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.jaxrs.JsonUtils;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GuideRecommendationsResourceTest
    extends AbstractResourceTest
{
  private static final String RECOMMENDATIONS_PATH = "api/v2/guide/recommendations";

  /**
   * Guide features (GUIDE, GUIDE_MCP, GUIDE_SEARCH) are HDS-controlled (see
   * CLMLicenseManager.populateLicenseCache hdsControlledFeatures). The default integration-test HDS
   * mock response in productLicenseDetails.json doesn't include them, so the Guide-feature-gated
   * resources under test would 403 on every happy-path test without an explicit setFeatures here.
   * <p>
   * Note: setFeatures replaces the entire feature set — tests in this class that require non-Guide
   * licensed features must call setFeatures/setMissingFeature explicitly in the test body.
   * <p>
   * Auth filtering runs before license-feature filtering in the Dropwizard filter chain, so
   * unauthenticatedRequest_returns401 is unaffected by this narrowed feature set.
   */
  @Before
  public void enableGuideFeatures() throws Exception {
    setFeatures(LicensedFeature.GUIDE, LicensedFeature.GUIDE_MCP, LicensedFeature.GUIDE_SEARCH);
  }

  @Test
  public void unauthenticatedRequest_returns401() throws Exception {
    HttpResponse response = restRequest()
        .path(RECOMMENDATIONS_PATH)
        .body(new RecommendationRequest("pkg:maven/org.example/lib@1.0.0", null, null))
        .anon()
        .post();

    assertThat(response.getStatusCode()).isEqualTo(401);
  }

  @Test
  public void missingGuideSearchFeature_returns403() throws Exception {
    // The Guide API admits on GUIDE_SEARCH or AI_DEVELOPER, so both must be absent to get a 403.
    setMissingFeatures(LicensedFeature.GUIDE_SEARCH, LicensedFeature.AI_DEVELOPER);

    HttpResponse response = restRequest()
        .path(RECOMMENDATIONS_PATH)
        .body(new RecommendationRequest("pkg:maven/org.example/lib@1.0.0", null, null))
        .post();

    assertThat(response.getStatusCode()).isEqualTo(403);
    assertThat(response.getBodyText()).contains("not available with the current license");
  }

  @Test
  public void insufficientPermissions_returns403() throws Exception {
    User userWithoutPermissions = tempEntity.newUser();

    HttpResponse response = restRequest()
        .auth(userWithoutPermissions)
        .path(RECOMMENDATIONS_PATH)
        .body(new RecommendationRequest("pkg:maven/org.example/lib@1.0.0", null, null))
        .post();

    assertThat(response.getStatusCode()).isEqualTo(403);
  }

  @Test
  public void blankPurl_returns400() throws Exception {
    HttpResponse response = restRequest()
        .path(RECOMMENDATIONS_PATH)
        .body(new RecommendationRequest("", null, null))
        .post();

    assertThat(response.getStatusCode()).isEqualTo(400);
    assertThat(response.getBodyText()).contains("Purl is required");
  }

  @Test
  public void nullPurl_returns400() throws Exception {
    HttpResponse response = restRequest()
        .path(RECOMMENDATIONS_PATH)
        .body(new RecommendationRequest(null, null, null))
        .post();

    assertThat(response.getStatusCode()).isEqualTo(400);
    assertThat(response.getBodyText()).contains("Purl is required");
  }

  @Test
  public void nullBody_returns400() throws Exception {
    // JAX-RS/Jackson hands the resource method `request=null` when the client sends an
    // empty body. Without an explicit `request == null` guard the resource NPEs before
    // the null-purl check runs, escaping GuideExceptionMapper and breaking the
    // {"success":false,"message":"..."} envelope contract. This test exercises the empty
    // body path explicitly so a future refactor that drops the guard fails loudly.
    HttpResponse response = restRequest()
        .path(RECOMMENDATIONS_PATH)
        .post();

    assertThat(response.getStatusCode()).isEqualTo(400);
    assertThat(response.getBodyText()).contains("\"success\":false");
    assertThat(response.getBodyText()).contains("Purl is required");
  }

  @Test
  public void validPurl_componentNotFound_returns404() throws Exception {
    HttpResponse response = restRequest()
        .path(RECOMMENDATIONS_PATH)
        .body(new RecommendationRequest("pkg:maven/org.nonexistent/fake@1.0.0", null, null))
        .post();

    assertThat(response.getStatusCode()).isEqualTo(404);
  }

  @Test
  public void validPurl_returnsRecommendations() throws Exception {
    GuideRecommendationResult hdsResponse = new GuideRecommendationResult(
        RecommendationResponse.Outcome.FOUND_RECOMMENDATIONS,
        new RecommendedVersionInfo("2.14.1", "0", Map.of("CVE-2021-44228", 10.0), Map.of(), Map.of(),
            List.of(), 85, 10.0, null),
        List.of(new RecommendedVersionInfo("2.21.1", "0", Map.of(), Map.of(), Map.of(), List.of(), 99, null, null)));
    hdsRespondWith(hdsResponse).atUri("/rest/search/recommendations");

    HttpResponse response = restRequest()
        .path(RECOMMENDATIONS_PATH)
        .body(new RecommendationRequest("pkg:maven/org.apache.logging.log4j/log4j-core@2.14.1", null, null))
        .post();

    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBodyText()).contains("FOUND_RECOMMENDATIONS");
    assertThat(response.getBodyText()).contains("2.21.1");
    assertThat(response.getBodyText()).contains("CVE-2021-44228");
  }

  @Test
  public void validPurl_hdsFailure_returns502() throws Exception {
    hdsRespondWith("").atUri("/rest/search/recommendations").andStatus(500);

    HttpResponse response = restRequest()
        .path(RECOMMENDATIONS_PATH)
        .body(new RecommendationRequest("pkg:maven/org.apache.logging.log4j/log4j-core@2.14.1", null, null))
        .post();

    assertThat(response.getStatusCode()).isEqualTo(502);
    assertThat(response.getBodyText()).contains("Bad Gateway");
  }

  // --- GUIDE-3045: ownerId/stage query params -------------------------------------------------------

  @Test
  public void unknownOwnerId_returnsUpstreamCandidatesUnfiltered() throws Exception {
    // Candidates missing from the compliance map are normally treated as compliant (kept) — see
    // GuideRecommendationsPolicyFilter — but an unknown ownerId must skip the filter entirely
    // (GuidePolicyService.filterRecommendations scoped overload), not just fall through to that
    // same "no data" branch. Both land on FOUND_RECOMMENDATIONS here, so this test's real
    // assertion is that the candidate survives — the corresponding GuidePolicyServiceTest case
    // (filterRecommendations_scoped_unknownOwner_returnsUpstreamUnfiltered) pins down the "skip the
    // filter, don't just no-op through it" distinction at the unit level.
    GuideRecommendationResult hdsResponse = new GuideRecommendationResult(
        RecommendationResponse.Outcome.FOUND_RECOMMENDATIONS,
        new RecommendedVersionInfo("2.14.1", "0", Map.of(), Map.of(), Map.of(), List.of(), 85, 10.0, null),
        List.of(new RecommendedVersionInfo("2.21.1", "0", Map.of(), Map.of(), Map.of(), List.of(), 99, null, null)));
    hdsRespondWith(hdsResponse).atUri("/rest/search/recommendations");

    HttpResponse response = restRequest()
        .path(RECOMMENDATIONS_PATH)
        .query("ownerId", "does-not-exist")
        .body(new RecommendationRequest("pkg:maven/org.apache.logging.log4j/log4j-core@2.14.1", null, null))
        .post();

    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBodyText()).contains("FOUND_RECOMMENDATIONS");
    assertThat(response.getBodyText()).contains("2.21.1");
  }

  @Test
  public void invalidStage_returns400() throws Exception {
    // Stage is validated up front (GuidePolicyService.requireValidStage) before the upstream HDS
    // fetch, so no HDS mock is needed here — an invalid stage 400s without ever calling out.
    HttpResponse response = restRequest()
        .path(RECOMMENDATIONS_PATH)
        .query("stage", "not-a-stage")
        .body(new RecommendationRequest("pkg:maven/org.apache.logging.log4j/log4j-core@2.14.1", null, null))
        .post();

    assertThat(response.getStatusCode()).isEqualTo(400);
    assertThat(response.getBodyText()).contains("not-a-stage");
  }

  // --- GUIDE-3174: artifact selector (extension/classifier) wire format -------------------------

  @Test
  public void extensionAndClassifier_forwardedToHds() throws Exception {
    GuideRecommendationResult hdsResponse = new GuideRecommendationResult(
        RecommendationResponse.Outcome.FOUND_RECOMMENDATIONS,
        new RecommendedVersionInfo("2.14.1", "0", Map.of(), Map.of(), Map.of(), List.of(), 85, 10.0, null),
        List.of(new RecommendedVersionInfo("2.21.1", "0", Map.of(), Map.of(), Map.of(), List.of(), 99, null, null)));
    hdsRespondWith(hdsResponse).atUri("/rest/search/recommendations");

    HttpResponse response = restRequest()
        .path(RECOMMENDATIONS_PATH)
        .body(new RecommendationRequest("pkg:maven/org.apache.commons/commons-lang3@3.12.0", "jar", "sources"))
        .post();

    assertThat(response.getStatusCode()).isEqualTo(200);
    // Verify HDS received the extension/classifier in the request body by parsing the JSON
    String capturedBody = hdsMockServer.getCapturedRequestBody("/rest/search/recommendations");
    Map<String, String> requestBody = JsonUtils.parse(capturedBody, new TypeReference<Map<String, String>>()
    {
    });
    assertThat(requestBody.get("purl")).isEqualTo("pkg:maven/org.apache.commons/commons-lang3@3.12.0");
    assertThat(requestBody.get("extension")).isEqualTo("jar");
    assertThat(requestBody.get("classifier")).isEqualTo("sources");
  }
}
