/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.api.v2;

import com.sonatype.clm.dto.model.KevData;
import com.sonatype.clm.testing.api.AbstractIqApiTest;
import com.sonatype.clm.testing.api.categories.ApiRegressionTest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.ApiBulkVulnerabilityDetailsResourceV2;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.vulnerability.model.BulkSecurityVulnerabilityDataDTO;
import com.sonatype.insight.vulnerability.model.SecurityVulnerabilityData;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * API regression suite for {@code POST /api/v2/vulnerabilities}
 * ({@link ApiBulkVulnerabilityDetailsResourceV2}) — the bulk-lookup companion to
 * {@link VulnerabilitiesApiRegressionTest}. The endpoint accepts a raw JSON array of
 * refIds, normalizes each to upper case (CLM-38934), and delegates to HDS
 * ({@code POST /rest/vulnerability/details/json}). This is the only JAX-RS method on the
 * resource; there is no intentionally-deferred coverage.
 *
 * <p>
 * Auth is identical to the single-lookup endpoint —
 * {@code @AnonymousWithFeature(ENABLE_UNAUTHENTICATED_PAGES)} with the feature enabled by
 * default in the API-regression harness. Both sides of the toggle are exercised:
 * {@link #testGetBulkVulnerabilities_anonymousAllowedByFeatureFlag_returns200()} pins the
 * feature-<b>enabled</b> path (anon → 200), and
 * {@link #testGetBulkVulnerabilities_unauthenticated_featureDisabled_returns401()} pins the
 * feature-<b>disabled</b> path (anon → 401) — the AC-mandated 401 case for the POST verb
 * (CLM-42445). See {@link VulnerabilitiesApiRegressionTest} for the mirrored coverage on the
 * single-lookup GET.
 *
 * <p>
 * Unlike the single-lookup 404 contract, the bulk endpoint never 404s for missing refIds:
 * unknown ids are simply absent from the {@code vulnerabilities} map in the response.
 *
 * <p>
 * <b>Cross-file duplication.</b> {@link #uniqueCveId()} and the HDS-stub inner shape are
 * structurally duplicated in {@link VulnerabilitiesApiRegressionTest}. The pattern is
 * intentionally kept local to each suite for now — extracting a shared
 * {@code VulnerabilityApiTestFixtures} helper is a follow-up refactor tracked outside this
 * PR. Both copies must stay in lock-step; changes here should be mirrored to the
 * single-lookup suite.
 */
@Category(ApiRegressionTest.class)
public class BulkVulnerabilitiesApiRegressionTest
    extends AbstractIqApiTest
{
  private static final String BULK_PATH = PublicApiPaths.BULK_VULNERABILITIES_RESOURCE_PATH_V2;

  private static final String HDS_BULK_URI = "/rest/vulnerability/details/json";

  private static final String STUB_RECOMMENDATION = "bulk-regression";

  /**
   * Fresh CVE-shaped refId. Delegates to {@link #uniqueId(String)} so uniqueness comes from
   * the harness's single generator. See {@link VulnerabilitiesApiRegressionTest#uniqueCveId()}
   * for the full rationale — this copy exists only because {@link VulnerabilitiesApiRegressionTest}
   * and this class are intentionally not yet sharing a fixture helper (documented as a
   * follow-up in the class Javadoc above).
   */
  private static String uniqueCveId() {
    return uniqueId("CVE-2026").toUpperCase();
  }

  /**
   * Stub the HDS bulk endpoint with a response entry for each supplied refId, each mapped
   * to a minimal {@link SecurityVulnerabilityData}. Pass zero refIds to stub an empty
   * response.
   */
  private void stubHdsBulkResponse(final String... refIds) throws Exception {
    Map<String, SecurityVulnerabilityData> stub = new HashMap<>();
    for (String refId : refIds) {
      SecurityVulnerabilityData data = new SecurityVulnerabilityData(refId);
      data.recommendationMarkdown = STUB_RECOMMENDATION;
      data.kevData = new KevData(false);
      stub.put(refId, data);
    }
    getHdsServer().respondWith(new BulkSecurityVulnerabilityDataDTO(stub)).atUri(HDS_BULK_URI);
  }

  /**
   * Response-keying regression guard: the entry keyed by {@code refId} exists AND its
   * {@code identifier} node equals {@code refId} (pins both the upper-case normalization
   * and the map-key contract in one assertion).
   */
  private static void assertResponseContainsRefIdEntry(final HttpResponse response, final String refId) {
    assertThatJson(response.getBodyText())
        .node("vulnerabilities." + refId + ".identifier")
        .isEqualTo(refId);
  }

  @Test
  public void testGetBulkVulnerabilities_authenticated_returnsPayloadKeyedByRefId() throws Exception {
    String refId = uniqueCveId();
    stubHdsBulkResponse(refId);

    HttpResponse response = apiPostJson(BULK_PATH, Collections.singletonList(refId));
    assertResponseStatus(200, response);
    assertResponseContainsRefIdEntry(response, refId);
    assertThatJson(response.getBodyText())
        .node("vulnerabilities." + refId + ".recommendationMarkdown")
        .isEqualTo(STUB_RECOMMENDATION);
  }

  /**
   * CLM-38934: refIds sent lower-case must be normalized upper-case <b>before</b> dispatch
   * to HDS. Load-bearing assertion is on the captured HDS request body — a response-side
   * assertion would be vacuous here because {@code HdsMockServer} matches on path+query
   * only (never the request body) and
   * {@code VulnerabilityDetailsService.getBulkSecurityVulnerabilityDataDTO} returns the HDS
   * map verbatim, so the mock would return the upper-keyed entry even on a pass-through
   * regression. See {@link VulnerabilitiesApiRegressionTest} for the single-lookup mirror,
   * where the URI-path check is sound (refId is in the URI, not the body).
   */
  @Test
  public void testGetBulkVulnerabilities_mixedCaseRefId_normalizedBeforeHdsLookup() throws Exception {
    String upperRefId = uniqueCveId();
    String mixedRefId = "cve-" + upperRefId.substring("CVE-".length()).toLowerCase();
    stubHdsBulkResponse(upperRefId);

    HttpResponse response = apiPostJson(BULK_PATH, Collections.singletonList(mixedRefId));
    assertResponseStatus(200, response);

    assertThat(getHdsServer().getCapturedRequestBody(HDS_BULK_URI))
        .as("IQ must normalize refIds to upper-case before dispatching to HDS (CLM-38934)")
        .contains(upperRefId)
        .doesNotContain(mixedRefId);
  }

  @Test
  public void testGetBulkVulnerabilities_anonymousAllowedByFeatureFlag_returns200() throws Exception {
    String refId = uniqueCveId();
    stubHdsBulkResponse(refId);

    HttpResponse response = anonApiPostJson(BULK_PATH, Collections.singletonList(refId));
    assertResponseStatus(200, response);
    assertResponseContainsRefIdEntry(response, refId);
  }

  /**
   * Feature-<b>disabled</b> side of the {@code @AnonymousWithFeature(ENABLE_UNAUTHENTICATED_PAGES)}
   * gate — this is the AC-mandated 401 case for POST /api/v2/vulnerabilities (CLM-42445). The
   * {@link com.sonatype.insight.brain.security.AnonymousWithFeatureMethodInterceptor} reads
   * {@code SystemConfigurationPropertyFeature.ENABLE_UNAUTHENTICATED_PAGES.isEnabled()} from
   * the DB on every request; with the flag flipped to {@code false} the anonymous principal
   * path throws {@code UnauthenticatedException} which Shiro maps to 401. The flag is restored
   * in {@code finally} so sibling tests in the same fork keep the harness-default (enabled).
   *
   * <p>
   * No HDS stub is set up because the interceptor short-circuits before the resource method
   * runs; the assertion is purely on the status code (401 responses don't carry a stable
   * body fragment because Shiro's anonymous-filter text isn't part of the API contract).
   */
  @Test
  public void testGetBulkVulnerabilities_unauthenticated_featureDisabled_returns401() throws Exception {
    boolean wasEnabled = SystemConfigurationPropertyFeature.ENABLE_UNAUTHENTICATED_PAGES.isEnabled();
    try {
      // NB: setEnabled() is a silent no-op if the NXIQ_ENABLE_UNAUTHENTICATED_PAGES env var
      // is set (see SystemConfigurationPropertyFeature.ENABLE_UNAUTHENTICATED_PAGES). If this
      // test starts returning 200 in CI, check the runner's environment before assuming a
      // regression in the interceptor — no such override is set in any Jenkinsfile today.
      SystemConfigurationPropertyFeature.ENABLE_UNAUTHENTICATED_PAGES.setEnabled(false);

      HttpResponse response = anonApiPostJson(BULK_PATH, Collections.singletonList(uniqueCveId()));
      assertResponseStatus(401, response);
    }
    finally {
      SystemConfigurationPropertyFeature.ENABLE_UNAUTHENTICATED_PAGES.setEnabled(wasEnabled);
    }
  }

  /**
   * Empty input list is a valid request — IQ short-circuits and returns 200 with an empty
   * body. The {@code vulnerabilities} field is elided by
   * {@code @JsonInclude(Include.NON_EMPTY)} serialization, so {@code assertThatJson(...).isObject()}
   * is the shape assertion (not a {@code .node("vulnerabilities")} check).
   */
  @Test
  public void testGetBulkVulnerabilities_emptyRefIdList_returns200NoError() throws Exception {
    stubHdsBulkResponse();

    HttpResponse response = apiPostJson(BULK_PATH, Collections.emptyList());
    assertResponseStatus(200, response);
    assertThatJson(response.getBodyText()).isObject();
  }

  @Test
  public void testGetBulkVulnerabilities_unknownRefIdsAbsentFromResponse_notFatal() throws Exception {
    String presentRefId = uniqueCveId();
    String absentRefId = uniqueCveId();
    stubHdsBulkResponse(presentRefId);

    HttpResponse response = apiPostJson(BULK_PATH, List.of(presentRefId, absentRefId));
    assertResponseStatus(200, response);
    assertResponseContainsRefIdEntry(response, presentRefId);
    assertThatJson(response.getBodyText()).node("vulnerabilities").isObject().doesNotContainKey(absentRefId);
  }
}
