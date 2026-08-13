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
import com.sonatype.insight.brain.api.v2.dto.ApiLegacyViolationStatusDTO;
import com.sonatype.insight.brain.api.v2.dto.autowaivers.ApiAutoPolicyWaiverDTO;
import com.sonatype.insight.brain.api.v2.dto.autowaivers.ApiAutoPolicyWaiverExclusionRequestDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverExclusion;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * API regression coverage for the auto-policy-waiver and legacy-violation resources
 * under {@code /api/v2/autoPolicyWaivers*}, {@code /api/v2/legacyViolations*}, and
 * {@code /api/v2/config/legacyViolations*}. The corresponding
 * {@code Api*Resource}/{@code Api*ConfigResource} classes are the source of truth for
 * routing; this class pins the wire-level contract (status + error fragments).
 *
 * <p>
 * <b>Three license gates.</b> The @Before enables
 * {@link LicensedFeature#DEVELOPER_DASHBOARD},
 * {@link LicensedFeature#AUTO_WAIVER_MANAGEMENT}, and
 * {@link LicensedFeature#POLICY_GRANDFATHERING}, and stubs
 * {@code shouldEnableDeveloperProduct → true}. All three are needed because
 * {@code DefaultProductLicense#hasFeature(DEVELOPER_DASHBOARD)} delegates to the
 * developer-enablement service rather than the license feature set; without the stub,
 * every mutating call would 402 before reaching the 401/404 branch under test.
 *
 * <p>
 * <b>Feature-toggle 403 vs 401.</b> When
 * {@code SystemConfigurationPropertyFeature.AUTO_WAIVERS} is disabled,
 * {@code AutoPolicyWaiverUtil.validateAutoWaiversFeatureEnabled} throws
 * {@code UnauthorizedException} → HTTP 403 (not 401). The toggle test pins 403
 * accordingly; the env-var override caveat lives in-line on that test.
 */
@Category(ApiRegressionTest.class)
public class AutoWaiversAndLegacyViolationsApiRegressionTest
    extends AbstractIqApiTest
{
  private static final String AUTO_WAIVER_BASE = PublicApiPaths.AUTO_POLICY_WAIVER_PATH;

  private static final String AUTO_WAIVER_EXCLUSION_BASE = PublicApiPaths.AUTO_POLICY_WAIVER_EXCLUSION_PATH;

  private static final String LEGACY_VIOLATION_BASE = PublicApiPaths.LEGACY_VIOLATIONS_PATH_V2;

  private static final String LEGACY_VIOLATION_CONFIG_BASE = PublicApiPaths.LEGACY_VIOLATIONS_CONFIG_PATH_V2;

  @Before
  public void enableLicenseFeatures() throws Exception {
    // DefaultProductLicense#hasFeature(DEVELOPER_DASHBOARD) delegates to
    // developerEnablementService.shouldEnableDeveloperProduct() rather than checking the
    // license feature set. The API-regression base mock returns false by default (unlike
    // functional/playwright, which stub true in a static block), so setFeatures(..) alone
    // is not enough — the license enforcement filter would 402 every auto-waiver call.
    when(mockDeveloperEnablementService.shouldEnableDeveloperProduct()).thenReturn(true);
    setFeatures(
        LicensedFeature.DEVELOPER_DASHBOARD,
        LicensedFeature.AUTO_WAIVER_MANAGEMENT,
        LicensedFeature.POLICY_GRANDFATHERING);
  }

  /**
   * Defence-in-depth teardown for the {@code shouldEnableDeveloperProduct} stub above.
   * {@code AbstractBaseIntegrationTest#initTest} already resets this mock in {@code @Before}
   * of every test, so today no leak is possible — but the embedded server and its mock
   * graph are static across every {@code AbstractIqApiTest} subclass in a reused Failsafe
   * fork, and this module has a documented history of cross-fork stub leaks. Explicit
   * teardown here removes the latent fragility if the base {@code @Before} order changes.
   */
  @After
  public void resetDeveloperEnablementStub() {
    reset(mockDeveloperEnablementService);
  }

  private static String autoWaiverAppBase(final Application app) {
    return AUTO_WAIVER_BASE + "/application/" + app.getId();
  }

  private static String autoWaiverExclusionAppBase(final Application app) {
    return AUTO_WAIVER_EXCLUSION_BASE + "/application/" + app.getId();
  }

  private static String autoWaiverExclusionDeletePath(
      final Application app,
      final String waiverId,
      final String exclusionId)
  {
    return autoWaiverExclusionAppBase(app) + "/" + waiverId + "/" + exclusionId;
  }

  private static String legacyViolationAppPath(final String publicId) {
    return LEGACY_VIOLATION_BASE + "/application/" + publicId;
  }

  private static String legacyConfigAppPath(final String publicId) {
    return LEGACY_VIOLATION_CONFIG_BASE + "/application/" + publicId;
  }

  /**
   * Canonical body for auto-policy-waiver create. Populates the two scope bits
   * ({@code reachability = true}, {@code pathForward = false}) so a create request passes
   * the "at least one scope true" validator — mirroring the sibling
   * {@code ApiAutoPolicyWaiverResourceTest} defaults.
   */
  private static ApiAutoPolicyWaiverDTO newAutoWaiverBody() {
    ApiAutoPolicyWaiverDTO body = new ApiAutoPolicyWaiverDTO();
    body.threatLevel = 7;
    body.reachability = Boolean.TRUE;
    body.pathForward = Boolean.FALSE;
    return body;
  }

  @Test
  public void testCreateAutoPolicyWaiver_application_happyPath_returns200() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-auto-w-create"), Organization.ROOT_ORGANIZATION_ID);

    HttpResponse response = apiPostJson(autoWaiverAppBase(app), newAutoWaiverBody());

    assertResponseStatus(200, response);
    assertThatJson(response.getBodyText()).node("threatLevel").isEqualTo(7);
    assertThatJson(response.getBodyText()).node("reachability").isEqualTo(true);
    assertThatJson(response.getBodyText()).node("pathForward").isEqualTo(false);
    assertThatJson(response.getBodyText()).node("autoPolicyWaiverId").isString().isNotEmpty();
  }

  /**
   * Reliable 400 pin for the create verb — {@code AutoPolicyWaiverValidator.validateScopes}
   * throws when neither {@code reachability} nor {@code pathForward} is {@code true}, so
   * a waiver would apply to nothing. Alternative 400 candidates (threat level out of range;
   * duplicate scope on same owner) work but require more setup; this branch is a
   * self-contained validator throw.
   */
  @Test
  public void testCreateAutoPolicyWaiver_bothScopesFalse_returns400() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-auto-w-400"), Organization.ROOT_ORGANIZATION_ID);

    ApiAutoPolicyWaiverDTO body = newAutoWaiverBody();
    body.reachability = Boolean.FALSE;
    body.pathForward = Boolean.FALSE;

    HttpResponse response = apiPostJson(autoWaiverAppBase(app), body);

    assertResponseStatus(400, response);
    assertThat(response.getBodyText())
        .containsIgnoringCase("path forward")
        .containsIgnoringCase("reachability")
        .containsIgnoringCase("cannot both be false");
  }

  @Test
  public void testCreateAutoPolicyWaiver_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiPostJson(
        AUTO_WAIVER_BASE + "/application/" + uniqueId("any-app"),
        newAutoWaiverBody());

    assertResponseStatus(401, response);
  }

  @Test
  public void testCreateAutoPolicyWaiver_unknownApp_returns404() throws Exception {
    HttpResponse response = apiPostJson(
        AUTO_WAIVER_BASE + "/application/" + uniqueId("no-app"),
        newAutoWaiverBody());

    assertResponseStatus(404, response);
    assertThat(response.getBodyText())
        .containsIgnoringCase("application")
        .containsIgnoringCase("does not exist");
  }

  /**
   * <b>Feature-flag toggle pin (503-esque path).</b> When
   * {@code SystemConfigurationPropertyFeature.AUTO_WAIVERS} is disabled at runtime,
   * {@code AutoPolicyWaiverUtil.validateAutoWaiversFeatureEnabled} throws
   * {@code UnauthorizedException} which the JAX-RS mapper turns into <b>403</b> — not the
   * "401 disabled side" the AC template suggests, because this is a policy check inside
   * the service, not a Shiro anon filter. The paired "feature-enabled → 200" side is
   * covered by {@link #testCreateAutoPolicyWaiver_application_happyPath_returns200()}
   * (the default state, since {@code enabledWhenAbsent = true}).
   *
   * <p>
   * State is restored in {@code finally} so sibling tests within the same fork see the
   * feature enabled again — critical because {@code testCLMServer} is shared across
   * every subclass of {@code AbstractIqApiTest}.
   */
  @Test
  public void testCreateAutoPolicyWaiver_systemFeatureDisabled_returns403() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-auto-w-off"), Organization.ROOT_ORGANIZATION_ID);
    boolean wasEnabled = SystemConfigurationPropertyFeature.AUTO_WAIVERS.isEnabled();
    try {
      // NB: this in-process toggle is silently overridden if the agent has
      // {@code NXIQ_AUTO_WAIVERS} exported as an env var — the feature reads env
      // last-write-wins. If the 403 pin ever flips to 200 in CI, check the agent's
      // environment before assuming a service-layer regression.
      SystemConfigurationPropertyFeature.AUTO_WAIVERS.setEnabled(false);

      HttpResponse response = apiPostJson(autoWaiverAppBase(app), newAutoWaiverBody());

      assertResponseStatus(403, response);
      assertThat(response.getBodyText())
          .containsIgnoringCase("auto policy waivers")
          .containsIgnoringCase("not enabled");
    }
    finally {
      SystemConfigurationPropertyFeature.AUTO_WAIVERS.setEnabled(wasEnabled);
    }
  }

  /**
   * Pins the service-side "auto policy waiver not found for owner" branch. Sending an
   * exclusion request against an {@code autoPolicyWaiverId} that doesn't exist for the
   * owner returns 400 (not 404 — the service throws {@code BadRequestException} on this
   * branch because it's a body-validation failure, not a path-parent lookup). This is
   * more stable than pinning {@code "scanId is required"} because the resource resolves
   * the waiver before probing other body fields, so an unknown {@code autoPolicyWaiverId}
   * always wins the validator race.
   */
  @Test
  public void testCreateAutoWaiverExclusion_unknownWaiverId_returns400() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-auto-ex-400"), Organization.ROOT_ORGANIZATION_ID);

    ApiAutoPolicyWaiverExclusionRequestDTO body = new ApiAutoPolicyWaiverExclusionRequestDTO();
    body.applicationPublicId = app.getPublicId();
    body.ownerId = app.getId();
    body.scanId = uniqueId("any-scan");
    body.policyViolationId = uniqueId("any-viol");
    body.autoPolicyWaiverId = uniqueId("no-waiver");

    HttpResponse response = apiPostJson(autoWaiverExclusionAppBase(app), body);

    assertResponseStatus(400, response);
    assertThat(response.getBodyText())
        .containsIgnoringCase("auto policy waiver")
        .containsIgnoringCase("not found");
  }

  @Test
  public void testCreateAutoWaiverExclusion_unauthenticated_returns401() throws Exception {
    ApiAutoPolicyWaiverExclusionRequestDTO body = new ApiAutoPolicyWaiverExclusionRequestDTO();
    body.applicationPublicId = uniqueId("any-app");
    body.scanId = uniqueId("any-scan");

    HttpResponse response = anonApiPostJson(
        AUTO_WAIVER_EXCLUSION_BASE + "/application/" + uniqueId("any-app-owner"),
        body);

    assertResponseStatus(401, response);
  }

  /**
   * DELETE happy path — seeds both the parent auto-waiver and its exclusion via
   * {@code TemporaryEntity} (so no on-disk report fixture is required), then hits the
   * public DELETE endpoint. Verifies the resource-body {@code getByIdNotNull} path
   * actually removes the row (204). If a future refactor swaps the resource to a soft-
   * delete, this test would remain green on the 204 but the row cleanup guarantee moves —
   * a follow-up assertion on {@code AutoPolicyWaiverExclusionDAO.getById(...)} would be
   * needed at that point (deferred to resource-tier).
   */
  @Test
  public void testDeleteAutoWaiverExclusion_happyPath_returns204() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-auto-ex-del"), Organization.ROOT_ORGANIZATION_ID);
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverExclusion exclusion = tempEntity.newAutoPolicyWaiverExclusion(app.getId(), waiver.getId());

    HttpResponse response = apiDelete(
        autoWaiverExclusionDeletePath(app, waiver.getId(), exclusion.getId()));

    assertResponseStatus(204, response);
  }

  /**
   * 401 anon pin for the DELETE verb — the Shiro filter rejects unauthenticated
   * requests upstream of {@code AutoPolicyWaiverUtil.validateAutoWaiversFeatureEnabled}
   * and the resource-body {@code getByIdNotNull} lookup, so synthetic ids on both the
   * waiver and exclusion positions are enough to reach the auth branch.
   */
  @Test
  public void testDeleteAutoWaiverExclusion_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiDelete(
        AUTO_WAIVER_EXCLUSION_BASE + "/application/" + uniqueId("any-app-owner")
            + "/" + uniqueId("any-waiver") + "/" + uniqueId("any-excl"));

    assertResponseStatus(401, response);
  }

  /**
   * Happy path — a legacy-violations list against a real app returns 200 with a JSON
   * array. The array may be empty (no legacy violations have been granted yet); the pin
   * here is the wire-level 200 + array-shape contract, not violation content. Non-empty
   * content is exercised by {@code ApiLegacyViolationResourceTest} which drives the
   * grant flow end-to-end at the resource-integration tier.
   */
  @Test
  public void testListLegacyViolations_application_happyPath_returns200() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-legacy-list"), Organization.ROOT_ORGANIZATION_ID);

    HttpResponse response = apiGet(legacyViolationAppPath(app.getPublicId()));

    assertResponseStatus(200, response);
    assertThatJson(response.getBodyText()).isArray();
  }

  @Test
  public void testListLegacyViolations_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(legacyViolationAppPath(uniqueId("any-app")));

    assertResponseStatus(401, response);
  }

  @Test
  public void testListLegacyViolations_unknownApp_returns404() throws Exception {
    HttpResponse response = apiGet(legacyViolationAppPath(uniqueId("no-app")));

    assertResponseStatus(404, response);
    assertThat(response.getBodyText())
        .containsIgnoringCase("could not find an application")
        .containsIgnoringCase("public id");
  }

  @Test
  public void testSetLegacyViolationConfig_application_happyPath_returns200() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-legacy-cfg"), Organization.ROOT_ORGANIZATION_ID);

    ApiLegacyViolationStatusDTO body = new ApiLegacyViolationStatusDTO();
    body.enabled = Boolean.TRUE;

    HttpResponse response = apiPutJson(legacyConfigAppPath(app.getPublicId()), body);

    assertResponseStatus(200, response);
    assertThatJson(response.getBodyText()).node("enabled").isEqualTo(true);
  }

  /**
   * Reliable 400 pin for the config PUT — {@code ApiLegacyViolationConfigResource.setConfig}
   * inspects the deserialized body up-front and throws
   * {@code BadRequestException("Request body is required.")} on null. Sending a Java
   * {@code null} to {@code apiPutJson} serializes to the JSON literal {@code null} which
   * Jersey passes through as a null reference — matching what the resource guards
   * against. Distinct from a body deserialization failure (which would be a Jersey-layer
   * 400 with a different fragment).
   */
  @Test
  public void testSetLegacyViolationConfig_nullBody_returns400() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-legacy-cfg-400"), Organization.ROOT_ORGANIZATION_ID);

    HttpResponse response = apiPutJson(legacyConfigAppPath(app.getPublicId()), null);

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).containsIgnoringCase("request body is required");
  }

  @Test
  public void testSetLegacyViolationConfig_unauthenticated_returns401() throws Exception {
    ApiLegacyViolationStatusDTO body = new ApiLegacyViolationStatusDTO();
    body.enabled = Boolean.TRUE;

    HttpResponse response = anonApiPutJson(legacyConfigAppPath(uniqueId("any-app")), body);

    assertResponseStatus(401, response);
  }

  /**
   * Happy-path GET — PUTs the enabled state through {@link
   * #testSetLegacyViolationConfig_application_happyPath_returns200}'s sibling helper first
   * so the config row exists, then round-trips the GET. The resource returns a 200 with
   * {@code enabled} echoed back; without the PUT the resource 404s (mirrors the pattern
   * used for {@code testGetCiConfiguration_afterPut_happyPath_returns200} in
   * {@code SourceControlAndCiApiRegressionTest}).
   */
  @Test
  public void testGetLegacyViolationConfig_application_happyPath_returns200() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-legacy-cfg-get"), Organization.ROOT_ORGANIZATION_ID);
    ApiLegacyViolationStatusDTO body = new ApiLegacyViolationStatusDTO();
    body.enabled = Boolean.TRUE;
    apiPutJson(legacyConfigAppPath(app.getPublicId()), body);

    HttpResponse response = apiGet(legacyConfigAppPath(app.getPublicId()));

    assertResponseStatus(200, response);
    assertThatJson(response.getBodyText()).node("enabled").isEqualTo(true);
  }

  @Test
  public void testGetLegacyViolationConfig_unknownApp_returns404() throws Exception {
    HttpResponse response = apiGet(legacyConfigAppPath(uniqueId("no-app-cfg")));

    assertResponseStatus(404, response);
    assertThat(response.getBodyText())
        .containsIgnoringCase("could not find an application")
        .containsIgnoringCase("public id");
  }

  /**
   * 401 anon pin for GET — symmetric with {@code testSetLegacyViolationConfig_unauthenticated_returns401}.
   * Shiro rejects upstream of the {@code publicId → Application} resolution, so a
   * synthetic public id is sufficient to reach the auth branch.
   */
  @Test
  public void testGetLegacyViolationConfig_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(legacyConfigAppPath(uniqueId("any-app")));

    assertResponseStatus(401, response);
  }
}
