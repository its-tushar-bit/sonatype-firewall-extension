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
import com.sonatype.insight.brain.api.v2.ApiVulnerabilityDetailsResourceV2;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.vulnerability.model.SecurityVulnerabilityData;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

/**
 * API regression suite for {@code GET /api/v2/vulnerabilities/{refId}}
 * ({@link ApiVulnerabilityDetailsResourceV2}). The resource is a thin proxy that normalizes
 * the {@code refId} to upper case (CLM-38934), calls out to HDS
 * ({@code /rest/vulnerability/details/json/{refId}}), and returns the
 * {@code SecurityVulnerabilityDataDTO} projection. This is the only JAX-RS method on the
 * resource; there is no intentionally-deferred coverage.
 *
 * <p>
 * Auth model: {@code @AnonymousWithFeature(ENABLE_UNAUTHENTICATED_PAGES)}. The
 * unauthenticated-pages feature is enabled by default in the API-regression test harness, so
 * anonymous requests are <em>allowed</em>. Both sides of the toggle are exercised here:
 * <ul>
 * <li>{@link #testGetVulnerability_anonymousAllowedByFeatureFlag_returns200()} pins the
 * feature-<b>enabled</b> path (anon → 200) — if the annotation is ever swapped for
 * {@code @Authorize}, the anonymous request will 401 and the test will catch it.</li>
 * <li>{@link #testGetVulnerability_unauthenticated_featureDisabled_returns401()} pins the
 * feature-<b>disabled</b> path (anon → 401) — this is the AC-mandated 401 case for the
 * endpoint. The test flips the DB flag inside a try/finally so no residual state leaks
 * to sibling tests in the same fork.</li>
 * </ul>
 * There is no {@code @Authorize} annotation on this resource, so no per-owner permission gate
 * at the JAX-RS layer.
 *
 * <p>
 * HDS is mocked via {@link com.sonatype.insight.brain.service.HdsMockServerRule}; positive
 * paths stub the exact URI the client will hit ({@link #hdsUri(String)}), letting us assert
 * both the response payload and the upper-case-normalization contract without needing a live
 * HDS. Every test picks its own {@link #uniqueCveId() unique refId} so HDS stubs from other
 * tests in the same fork never leak in.
 *
 * <p>
 * <b>Fake / unique ids.</b> All tests here use {@code uniqueCveId()} to generate a fresh
 * refId per invocation. The anonymous test does not need a real vulnerability behind the id
 * because the anon-allow gate is checked before the HDS call; the HDS stub is set up
 * specifically so the response can also assert the payload flowed through.
 *
 * <p>
 * <b>Cross-file duplication.</b> {@link #uniqueCveId()} and {@link #newHdsSecurityVulnerabilityData(String)}
 * are structurally duplicated in {@code BulkVulnerabilitiesApiRegressionTest}. The pattern
 * is intentionally kept local to each suite for now — extracting a shared
 * {@code VulnerabilityApiTestFixtures} helper is a follow-up refactor tracked outside this
 * PR. Both copies must stay in lock-step; changes here should be mirrored to the bulk suite.
 */
@Category(ApiRegressionTest.class)
public class VulnerabilitiesApiRegressionTest
    extends AbstractIqApiTest
{
  private static final String STUB_RECOMMENDATION = "regression-recommendation";

  private static String vulnPath(final String refId) {
    return PublicApiPaths.VULNERABILITIES_RESOURCE_PATH_V2.replace("{refId}", refId);
  }

  private static String hdsUri(final String upperCaseRefId) {
    return "/rest/vulnerability/details/json/" + upperCaseRefId;
  }

  private static SecurityVulnerabilityData newHdsSecurityVulnerabilityData(final String refId) {
    SecurityVulnerabilityData data = new SecurityVulnerabilityData(refId);
    data.recommendationMarkdown = STUB_RECOMMENDATION;
    data.kevData = new KevData(false);
    return data;
  }

  /**
   * Fresh CVE-shaped refId. Delegates to {@link #uniqueId(String)} so uniqueness comes from
   * the harness's single generator rather than a hand-rolled {@code UUID.randomUUID().substring(...)}
   * (which would drift as the harness evolves). The upper-case wrap on the framework's
   * kebab output produces {@code CVE-2026-XXXXXXXX}, which lets the CLM-38934 normalization
   * test compare the upper-case canonical form against a lower-case variant.
   */
  private static String uniqueCveId() {
    return uniqueId("CVE-2026").toUpperCase();
  }

  @Test
  public void testGetVulnerability_knownRefId_returns200_withHdsPayload() throws Exception {
    String refId = uniqueCveId();
    getHdsServer().respondWith(newHdsSecurityVulnerabilityData(refId)).atUri(hdsUri(refId));

    HttpResponse response = apiGet(vulnPath(refId));
    assertResponseStatus(200, response);
    assertThatJson(response.getBodyText()).node("identifier").isEqualTo(refId);
    assertThatJson(response.getBodyText()).node("recommendationMarkdown").isEqualTo(STUB_RECOMMENDATION);
  }

  /**
   * CLM-38934 contract: CVE ids passed in mixed case must be normalized to upper case before
   * HDS lookup. Stub only the upper-case URI; if normalization regresses to pass-through, the
   * mock will not match and the response will be 200/empty (or a mismatch on {@code identifier}).
   */
  @Test
  public void testGetVulnerability_mixedCaseCveId_normalizedToUpperBeforeHdsLookup() throws Exception {
    String upperRefId = uniqueCveId();
    String mixedCaseRefId = "cve-" + upperRefId.substring("CVE-".length()).toLowerCase();
    getHdsServer().respondWith(newHdsSecurityVulnerabilityData(upperRefId)).atUri(hdsUri(upperRefId));

    HttpResponse response = apiGet(vulnPath(mixedCaseRefId));
    assertResponseStatus(200, response);
    assertThatJson(response.getBodyText()).node("identifier").isEqualTo(upperRefId);
  }

  /**
   * The resource is annotated {@code @AnonymousWithFeature(ENABLE_UNAUTHENTICATED_PAGES)},
   * and the flag is on in the API-regression harness — anonymous callers must be able to
   * hit this endpoint. If the annotation is removed or the resource is re-gated behind
   * {@code @Authorize}, the anonymous request will start returning 401 and this assertion
   * will catch it.
   */
  @Test
  public void testGetVulnerability_anonymousAllowedByFeatureFlag_returns200() throws Exception {
    String refId = uniqueCveId();
    getHdsServer().respondWith(newHdsSecurityVulnerabilityData(refId)).atUri(hdsUri(refId));

    HttpResponse response = anonApiGet(vulnPath(refId));
    assertResponseStatus(200, response);
    assertThatJson(response.getBodyText()).node("identifier").isEqualTo(refId);
  }

  /**
   * Feature-<b>disabled</b> side of the {@code @AnonymousWithFeature(ENABLE_UNAUTHENTICATED_PAGES)}
   * gate — this is the AC-mandated 401 case for the endpoint (CLM-42445). The
   * {@link com.sonatype.insight.brain.security.AnonymousWithFeatureMethodInterceptor} reads
   * {@code SystemConfigurationPropertyFeature.ENABLE_UNAUTHENTICATED_PAGES.isEnabled()} from
   * the DB on every request (see
   * {@code AnonymousWithFeatureMethodInterceptor#assertAnonymousWithFeature}); with the flag
   * flipped to {@code false} the anonymous principal path throws
   * {@code UnauthenticatedException} which Shiro maps to 401. The flag is restored in
   * {@code finally} so sibling tests in the same fork keep the harness-default (enabled).
   *
   * <p>
   * No HDS stub is set up because the interceptor short-circuits before the resource method
   * runs; the assertion is purely on the status code (401 responses don't carry a stable
   * body fragment because Shiro's anonymous-filter text isn't part of the API contract).
   */
  @Test
  public void testGetVulnerability_unauthenticated_featureDisabled_returns401() throws Exception {
    boolean wasEnabled = SystemConfigurationPropertyFeature.ENABLE_UNAUTHENTICATED_PAGES.isEnabled();
    try {
      // NB: setEnabled() is a silent no-op if the NXIQ_ENABLE_UNAUTHENTICATED_PAGES env var
      // is set (see SystemConfigurationPropertyFeature.ENABLE_UNAUTHENTICATED_PAGES). If this
      // test starts returning 200 in CI, check the runner's environment before assuming a
      // regression in the interceptor — no such override is set in any Jenkinsfile today.
      SystemConfigurationPropertyFeature.ENABLE_UNAUTHENTICATED_PAGES.setEnabled(false);

      HttpResponse response = anonApiGet(vulnPath(uniqueCveId()));
      assertResponseStatus(401, response);
    }
    finally {
      SystemConfigurationPropertyFeature.ENABLE_UNAUTHENTICATED_PAGES.setEnabled(wasEnabled);
    }
  }

  // Intentionally no HDS-not-stubbed / HDS-error test here: the endpoint is a thin proxy,
  // the exception-mapper behaviour for HDS non-200s is a Jersey/HDS-client interaction, and
  // pinning it via isBetween(400, 499) would hide the specific status the resource returns.
  // The bulk companion (see BulkVulnerabilitiesApiRegressionTest) covers the "unknown refId"
  // branch on its own well-defined 200-with-absent-entry contract.
}
