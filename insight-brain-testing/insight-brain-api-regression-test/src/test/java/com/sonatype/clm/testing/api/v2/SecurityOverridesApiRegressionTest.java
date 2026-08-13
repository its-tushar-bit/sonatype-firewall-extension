/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.api.v2;

import com.sonatype.clm.testing.api.AbstractIqApiTest;
import com.sonatype.clm.testing.api.categories.ApiRegressionTest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.ApiSecurityVulnerabilityOverrideResourceV2;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

/**
 * API regression suite for the v2 security-override read path
 * {@code GET /api/v2/securityOverrides} ({@link ApiSecurityVulnerabilityOverrideResourceV2}).
 *
 * <p>
 * The v2 mount point is <em>read-only</em>: it returns a filtered list keyed by
 * {@code refId} / {@code componentPurl} / {@code ownerId} query parameters. Unknown /
 * unauthorized {@code ownerId} does <em>not</em> 404 — the service returns HTTP 200 with
 * an empty {@code securityOverrides} array. That silent-filter behaviour is a client
 * contract (pre-provisioned poll patterns rely on it), so a regression to 404 needs to
 * fail loudly here.
 *
 * <p>
 * The write-side counterpart lives at the legacy
 * {@code PUT rest/securityVulnerabilityOverride/{ownerType}/{ownerId}} endpoint. Per the
 * module {@code CLAUDE.md} scope rule, {@code /rest/} coverage lives in the sibling
 * {@code …api.rest} package — see
 * {@code com.sonatype.clm.testing.api.rest.SecurityVulnerabilityOverrideApiRegressionTest}.
 *
 * <p>
 * <b>JAX-RS methods covered / deferred:</b>
 * <ul>
 * <li>{@code ApiSecurityVulnerabilityOverrideResourceV2#getSecurityOverrides} — covered
 * (happy path, cross-owner filter isolation, anonymous).</li>
 * <li>Legacy PUT / GET (single-lookup) — covered / deferred in the {@code api.rest}
 * sibling class linked above.</li>
 * </ul>
 *
 * <p>
 * Overrides for the happy-path GET are seeded directly via
 * {@code tempEntity.newSecurityVulnerabilityOverride(...)} so the read test does not
 * depend on the (separately covered) legacy PUT path — a regression in the writer cannot
 * masquerade as a regression in the reader here.
 *
 * <p>
 * Owner-id conventions: v2 GET filters on <em>internal</em> app id (matching what
 * {@code newSecurityVulnerabilityOverride} persists).
 *
 * @see PublicApiPaths#SECURITY_VIOLATION_OVERRIDE_PATH_V2
 */
@Category(ApiRegressionTest.class)
public class SecurityOverridesApiRegressionTest
    extends AbstractIqApiTest
{
  private static final String V2_LIST_PATH = PublicApiPaths.SECURITY_VIOLATION_OVERRIDE_PATH_V2;

  private static final String SOURCE_CVE = "cve";

  /**
   * Fresh CVE-shaped refId. Delegates to {@link #uniqueId(String)} so uniqueness comes
   * from the harness's single generator rather than a hand-rolled
   * {@code UUID.randomUUID().substring(...)}. {@code sv_override.reference_id} is
   * {@code VARCHAR(20)}; {@code "CVE-" + 8-char uuid} (12 chars total) fits well inside
   * that constraint.
   *
   * <p>
   * The same helper exists (verbatim) in the {@code api.rest} write-side test; extracting
   * a shared {@code SecurityOverrideFixtures} across the two packages is tracked as a
   * follow-up refactor outside this PR.
   */
  private static String uniqueCve() {
    return uniqueId("CVE").toUpperCase();
  }

  private static void assertOverrideListContainsRefId(final HttpResponse response, final String refId) {
    assertThatJson(response.getBodyText())
        .inPath("$.securityOverrides[*].referenceId")
        .isArray()
        .contains(refId);
  }

  private static void assertOverrideListDoesNotContainRefId(final HttpResponse response, final String refId) {
    assertThatJson(response.getBodyText())
        .inPath("$.securityOverrides[*].referenceId")
        .isArray()
        .doesNotContain(refId);
  }

  @Test
  public void testGetSecurityOverridesV2_seedForApp_returns200_containsIt() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-sec-ov-get"), Organization.ROOT_ORGANIZATION_ID);
    String hash = tempEntity.newRandomHash();
    String refId = uniqueCve();
    tempEntity.newSecurityVulnerabilityOverride(app.getId(), hash, SOURCE_CVE, refId,
        SecurityVulnerabilityOverrideStatus.NOT_APPLICABLE, "seeded for regression");

    HttpResponse response = apiGet(V2_LIST_PATH, "ownerId", app.getId());
    assertResponseStatus(200, response);
    assertThatJson(response.getBodyText()).node("securityOverrides").isArray().isNotEmpty();
    assertOverrideListContainsRefId(response, refId);
    assertThatJson(response.getBodyText())
        .inPath("$.securityOverrides[*].hash")
        .isArray()
        .contains(hash);
  }

  /**
   * v2 list does <em>not</em> 404 for an unknown or unauthorized {@code ownerId} — the
   * service silently filters the response. Guard against a future change that flips that
   * to 404 (which would break clients that poll pre-provisioned owner ids).
   *
   * <p>
   * To prove the filter <em>actually filters</em>, we seed a real override on a real
   * application and then query with a different owner id — asserting the seeded override
   * is absent. An {@code .isEmpty()} check on a random fake owner would pass trivially
   * even if the filter was silently returning every row.
   */
  @Test
  public void testGetSecurityOverridesV2_ownerIdFilter_excludesOtherOwners() throws Exception {
    Application seededApp = tempEntity.newApplication(uniqueId("api-sec-ov-seed"), Organization.ROOT_ORGANIZATION_ID);
    String seededRefId = uniqueCve();
    tempEntity.newSecurityVulnerabilityOverride(seededApp.getId(), tempEntity.newRandomHash(), SOURCE_CVE, seededRefId,
        SecurityVulnerabilityOverrideStatus.NOT_APPLICABLE, "seeded on a different owner");

    HttpResponse response = apiGet(V2_LIST_PATH, "ownerId", uniqueId("nonexistent-owner"));
    assertResponseStatus(200, response);
    assertOverrideListDoesNotContainRefId(response, seededRefId);
  }

  @Test
  public void testGetSecurityOverridesV2_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(V2_LIST_PATH);
    assertResponseStatus(401, response);
  }
}
