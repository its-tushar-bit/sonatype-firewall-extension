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
import com.sonatype.insight.brain.model.Organization;

import java.util.Map;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

/**
 * Wide-and-shallow regression coverage for the {@code /api/v2/*} operational and
 * system-scoped resources — the "everything else" grab-bag from {@link PublicApiPaths}
 * that isn't policy/violation/waiver, firewall/repositories, source-control/CI, or
 * RBAC/config. Every family lands its path at a {@link PublicApiPaths} constant (so a
 * rename fails to compile here) and gets a 401-anon pin, plus a 200-happy pin where
 * the endpoint returns a stable default without heavy fixtures.
 *
 * <p>
 * <b>Auth ordering.</b> Every family rejects unauthenticated access with 401 upstream
 * of any license enforcement, feature-flag gate, or resource-body validation. That
 * ordering is what the 401 tests pin.
 *
 * <p>
 * <b>Templated {@link PublicApiPaths} constants</b> for owner-scoped families
 * (waiverExpirationNotification, malware-defense, reachability evidence, CPE) carry
 * Jersey path templates that leak through {@code HttpRequest#path()} URL-encoding —
 * un-templated prefixes are derived with {@code substring(0, indexOf("/{"))} at
 * class-init time, which additionally fails to build if the constant loses its
 * templated shape. One family — {@code /api/v2/lifecycle} — has no matching constant
 * and uses a literal path; adding {@code LIFECYCLE_RESOURCE_PATH} to
 * {@link PublicApiPaths} is a production-code change out of scope for this test-only
 * PR (tracked separately).
 *
 * <p>
 * <b>AC deviations.</b>
 * <ul>
 * <li>Happy-path GETs for owner-scoped resources need existing config rows and stay
 * at the resource tier ({@code ApiWaiverExpirationNotificationConfigResourceTest} et
 * al.); this class pins auth ordering only.</li>
 * <li>{@code /api/v2/component-change-detection}, {@code /api/v2/telemetry},
 * reachability-evidence, and developer-priorities endpoints all need heavy fixtures —
 * anon-401 only here.</li>
 * <li>{@code /api/v2/endpoints/public} intentionally serves the OpenAPI descriptor to
 * anonymous callers (third-party discovery), so its "anon" pin is a 200, not a 401.</li>
 * <li>{@code /api/v2/product/license} is POST/DELETE only — anon pin uses DELETE;
 * happy-path 200 needs a multipart license file, covered at the resource tier.</li>
 * </ul>
 */
@Category(ApiRegressionTest.class)
public class OperationalConfigApiRegressionTest
    extends AbstractIqApiTest
{

  /**
   * Un-templated prefix of {@link PublicApiPaths#WAIVER_EXPIRATION_NOTIFICATION_CONFIG_PATH_V2}
   * — the raw constant carries a Jersey {@code {ownerType:...}}/{@code {ownerId}}
   * template that leaks through {@code HttpRequest#path()} URL-encoding. Substring at
   * the first {@code /{} gives a compile-time-bound prefix that fails to build if the
   * constant is renamed or loses its templated shape.
   */
  private static final String WAIVER_EXPIRATION_CONFIG_BASE =
      PublicApiPaths.WAIVER_EXPIRATION_NOTIFICATION_CONFIG_PATH_V2.substring(
          0, PublicApiPaths.WAIVER_EXPIRATION_NOTIFICATION_CONFIG_PATH_V2.indexOf("/{"));

  /** Un-templated prefix of {@link PublicApiPaths#REACHABILITY_EVIDENCE_RESOURCE_PATH}. */
  private static final String REACHABILITY_EVIDENCE_BASE =
      PublicApiPaths.REACHABILITY_EVIDENCE_RESOURCE_PATH.substring(
          0, PublicApiPaths.REACHABILITY_EVIDENCE_RESOURCE_PATH.indexOf("/{"));

  /**
   * The CPE-matching resource path is anchored inside an {@code {ownerType}} template
   * ({@code /api/v2/{ownerType:...}/{internalOwnerId}/configuration/publicSource/cpe})
   * so no clean prefix exists — the {@code substring(0, indexOf("/{"))} pattern used
   * elsewhere in this class strips too much. Instead, substitute the placeholders
   * literally to preserve compile-time binding for the constant name, and validate
   * post-substitution that no {@code {...}} tokens remain so a template-shape change
   * (e.g. renamed capture group, adjusted regex) fails loudly at class-init rather
   * than silently no-op'ing through {@link String#replace}.
   */
  private static final String CPE_MATCHING_ROOT_ORG_PATH = requireFullyResolved(
      PublicApiPaths.CPE_MATCHING_CONFIGURATION_RESOURCE_PATH
          .replace("{ownerType: application|organization}", "organization")
          .replace("{internalOwnerId}", Organization.ROOT_ORGANIZATION_ID),
      "CPE_MATCHING_CONFIGURATION_RESOURCE_PATH");

  private static String requireFullyResolved(final String path, final String constantName) {
    if (path.contains("{")) {
      throw new IllegalStateException(
          "PublicApiPaths." + constantName + " template shape has drifted; unresolved placeholders in: " + path);
    }
    return path;
  }

  @Test
  public void testGetVersionEvaluationWindow_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(PublicApiPaths.VERSION_EVALUATION_WINDOW_RESOURCE_PATH);

    assertResponseStatus(401, response);
  }

  @Test
  public void testGetWaiverExpirationNotificationConfig_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(
        WAIVER_EXPIRATION_CONFIG_BASE + "/organization/" + Organization.ROOT_ORGANIZATION_ID);

    assertResponseStatus(401, response);
  }

  @Test
  public void testGetMalwareDefenseConfig_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(
        PublicApiPaths.MALWARE_DEFENSE_RESOURCE_PATH
            + "/organization/" + Organization.ROOT_ORGANIZATION_ID);

    assertResponseStatus(401, response);
  }

  @Test
  public void testGetLifecycle_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet("api/v2/lifecycle");

    assertResponseStatus(401, response);
  }

  @Test
  public void testGetAuditLogs_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(PublicApiPaths.AUDIT_LOGS_RESOURCE_PATH);

    assertResponseStatus(401, response);
  }

  /**
   * 401 pin — {@link ApiProductLicenseResource} exposes only {@code POST} (install-license,
   * multipart) and {@code DELETE} (uninstall) verbs. There is no {@code GET} — a happy-path
   * 200 pin at this level is intentionally omitted (verb doesn't exist and file-upload
   * fixtures belong at the resource-tier {@code ApiProductLicenseResourceTest}). The anon
   * pin uses {@code DELETE} so the request reaches the correct HTTP verb before Shiro
   * short-circuits on missing credentials.
   */
  @Test
  public void testDeleteProductLicense_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiDelete(PublicApiPaths.PRODUCT_LICENSE_RESOURCE_PATH);

    assertResponseStatus(401, response);
  }

  @Test
  public void testPostTelemetry_unauthenticated_returns401() throws Exception {
    HttpResponse response =
        anonApiPostJson(PublicApiPaths.EXTERNAL_TELEMETRY_PATH, Map.of());

    assertResponseStatus(401, response);
  }

  /**
   * Happy-path GET — licensed-solutions discovery returns 200 with a JSON <em>array</em>
   * of {@code {id, url}} entries, one per top-level solution granted by the current
   * license. Exact contents vary by license so only the array shape + non-emptiness +
   * {@code id} node presence is pinned; a wire-contract regression where the endpoint
   * starts returning a scalar, object, HTML/plaintext body, or an empty array would
   * fail one of these assertions. The {@code .isNotEmpty()} pin matches the module's
   * "bounds-checked arrays" AC convention.
   */
  @Test
  public void testGetLicensedSolutions_happyPath_returns200() throws Exception {
    HttpResponse response = apiGet(PublicApiPaths.LICENSED_SOLUTIONS_RESOURCE_PATH);

    assertResponseStatus(200, response);
    assertThatJson(response.getBodyText()).isArray().isNotEmpty();
    assertThatJson(response.getBodyText()).inPath("$[*].id").isArray().isNotEmpty();
  }

  @Test
  public void testGetLicensedSolutions_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(PublicApiPaths.LICENSED_SOLUTIONS_RESOURCE_PATH);

    assertResponseStatus(401, response);
  }

  /**
   * Happy-path GET — {@link ApiEndpointsResource} exposes GET only under the
   * {@code {apiType: public|experimental}} sub-resource template
   * ({@code ApiEndpointsResource.ENDPOINT_TYPE_RESOURCE_PATH}); the parent
   * {@code /api/v2/endpoints} path has no verbs and returns 404. Pin the {@code public}
   * variant which is guaranteed present on every IQ Server bootstrap. Body pin on
   * {@code openapi} (the OpenAPI 3 spec version identifier) matches the anon 200 test
   * below and would fail if the endpoint accidentally started returning a stub, an
   * empty object, or a different descriptor shape.
   */
  @Test
  public void testGetEndpoints_public_happyPath_returns200() throws Exception {
    HttpResponse response = apiGet(PublicApiPaths.ENDPOINTS_RESOURCE_PATH + "/public");

    assertResponseStatus(200, response);
    assertThatJson(response.getBodyText()).node("openapi").isString().isNotEmpty();
  }

  /**
   * <b>Anonymous access is intentional.</b> {@code /api/v2/endpoints/public} serves the
   * OpenAPI descriptor and is registered without Shiro authentication so third-party
   * clients can discover the API surface without needing a credential. This 200 pin
   * guards that contract — an accidental addition of {@code @AuthenticationRequired}
   * or a Shiro filter-chain change that removes the anon exception would surface here.
   * (This is one of the very few paths in {@code /api/v2/*} that intentionally lack a
   * 401 anon pin.)
   */
  @Test
  public void testGetEndpoints_public_anonymous_returns200() throws Exception {
    HttpResponse response = anonApiGet(PublicApiPaths.ENDPOINTS_RESOURCE_PATH + "/public");

    assertResponseStatus(200, response);
    assertThatJson(response.getBodyText()).node("openapi").isString().isNotEmpty();
  }

  @Test
  public void testComponentChangeDetection_unauthenticated_returns401() throws Exception {
    HttpResponse response =
        anonApiPostJson(PublicApiPaths.COMPONENT_CHANGE_DETECTION_RESOURCE_PATH, Map.of());

    assertResponseStatus(401, response);
  }

  @Test
  public void testReachabilityEvidence_unauthenticated_returns401() throws Exception {
    HttpResponse response =
        anonApiGet(REACHABILITY_EVIDENCE_BASE + "/" + uniqueId("app") + "/reports/"
            + uniqueId("scan") + "/vulnerabilities");

    assertResponseStatus(401, response);
  }

  @Test
  public void testDeveloperPriorities_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(
        PublicApiPaths.DEVELOPER_PATH + "/priorities/" + uniqueId("app") + "/" + uniqueId("scan"));

    assertResponseStatus(401, response);
  }

  @Test
  public void testCpeMatchingConfig_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(CPE_MATCHING_ROOT_ORG_PATH);

    assertResponseStatus(401, response);
  }
}
