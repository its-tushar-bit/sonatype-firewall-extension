/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.api.v2;

import com.sonatype.clm.testing.api.AbstractIqApiTest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiSourceControlDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.githubapp.GitHubApp;
import com.sonatype.insight.license.model.LicensedFeature;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * API regression coverage for source control ({@code /api/v2/sourceControl*}), the
 * composite validator, GitHub App registration ({@code /api/v2/githubApp*}), and CI
 * integration configuration ({@code /api/v2/config/ci/**}).
 *
 * <p>
 * <b>License setup.</b> {@link LicensedFeature#SOURCE_CONTROL} +
 * {@link LicensedFeature#AUTOMATION} are enabled in @Before — the first satisfies the
 * resource-level enforcement point on SCM/metrics/composite endpoints, the second
 * satisfies {@code IqForScmLicenseChecker#isIqForScmSupported} which checks for
 * {@code AUTOMATION} <em>or</em> {@code NOTIFICATIONS} (either is enough). GitHub App
 * and CI resources carry no license enforcement.
 *
 * <p>
 * {@code @HasFeature(SAAS_LIFECYCLE_SCM_ENABLED)} gates most SCM methods, but
 * {@code TenantUtil.isSingleTenant()} forces it {@code true} on self-hosted — the
 * "disabled → 404" branch is only observable on MTIQ and is intentionally not pinned
 * here.
 *
 * <p>
 * <b>AC deviations.</b>
 * <ul>
 * <li>The composite validator is a <b>GET</b>, not a POST (the ticket wording was
 * inaccurate). An incomplete configuration returns 200 with
 * {@code configurationComplete.valid == false} — no 400 branch.</li>
 * <li>GitHub App registration step 1 is {@code POST /githubApp/manifest} (local state
 * only); the redirect/setup callbacks drive live GitHub and stay at the resource
 * tier.</li>
 * <li>{@code ci_integrations_config} rows are <b>not</b> wiped by
 * {@code TemporaryEntity.after()} — the happy-path PUT test uses a try/finally to
 * DELETE. This is the only manual cleanup in the class.</li>
 * </ul>
 */
public class SourceControlAndCiApiRegressionTest
    extends AbstractIqApiTest
{
  private static final String SOURCE_CONTROL_BASE = PublicApiPaths.SOURCE_CONTROL_PATH_V2;

  private static final String SCM_METRICS_BASE = PublicApiPaths.SOURCE_CONTROL_METRICS_PATH_V2;

  /**
   * The {@link PublicApiPaths#COMPOSITE_SOURCE_CONTROL_CONFIG_VALIDATOR_PATH_V2} constant
   * carries the {@code /application/{applicationId}} template suffix; anchoring at the
   * literal prefix keeps the path builder responsible for owner substitution — same
   * pattern used elsewhere in this class for template-bearing constants.
   */
  private static final String SCM_VALIDATOR_BASE = "api/v2/compositeSourceControlConfigValidator";

  private static final String GITHUB_APP_BASE = PublicApiPaths.GITHUB_APP_RESOURCE_PATH;

  /**
   * The {@link PublicApiPaths#CI_CONFIG_RESOURCE_PATH_V2} constant carries the
   * {@code /{ownerType}/{ownerId}} template suffix; anchoring at the literal prefix
   * keeps the path builder responsible for owner substitution.
   */
  private static final String CI_CONFIG_BASE = "api/v2/config/ci";

  @BeforeEach
  public void enableLicenseFeatures() throws Exception {
    // Two layers of license check on SCM resources:
    // 1. @ProductLicenseEnforcementPoint(SOURCE_CONTROL) at the JAX-RS filter tier.
    // 2. ApiSourceControlService.checkLicense() -> IqForScmLicenseChecker.isIqForScmSupported()
    // which validates AUTOMATION *or* NOTIFICATIONS (see IqForScmLicenseChecker.java:44-46).
    // Without step 2's feature, every SCM read/write throws InvalidLicenseException -> 402
    // even though the JAX-RS filter passes on SOURCE_CONTROL alone. AUTOMATION is the canonical
    // choice — matches what the Lifecycle Cloud product license derives.
    setFeatures(LicensedFeature.SOURCE_CONTROL, LicensedFeature.AUTOMATION);
  }

  private static String scmAppPath(final String appInternalId) {
    return SOURCE_CONTROL_BASE + "/application/" + appInternalId;
  }

  private static String scmOrgPath(final String orgInternalId) {
    return SOURCE_CONTROL_BASE + "/organization/" + orgInternalId;
  }

  private static String scmMetricsAppPath(final String appInternalId) {
    return SCM_METRICS_BASE + "/application/" + appInternalId;
  }

  private static String scmValidatorAppPath(final String appInternalId) {
    return SCM_VALIDATOR_BASE + "/application/" + appInternalId;
  }

  private static String ciConfigAppPath(final String appInternalId) {
    return CI_CONFIG_BASE + "/application/" + appInternalId;
  }

  /**
   * Minimal well-formed SCM body for organization-scoped POST. {@code provider} is
   * required so {@code SourceControlDAO.getProvider} short-circuits the parent-hierarchy
   * lookup — without it the DAO tries to derive the provider from the root organization,
   * which is unset on a fresh test IQ Server and throws
   * {@code "The root organization source control provider is not set"}.
   * {@code repositoryUrl} is intentionally omitted: on org scope the DAO rejects any
   * URL ({@code "SourceControl repositoryUrl is not allowed for organization"}).
   */
  private static ApiSourceControlDTO newScmBody() {
    ApiSourceControlDTO body = new ApiSourceControlDTO();
    body.token = "regression-token";
    body.provider = "github";
    return body;
  }

  /**
   * Organization-scoped POST is the cleanest happy path — {@code SourceControlDAO} rejects
   * any {@code repositoryUrl} on the organization scope, and requires one on the
   * application scope with a valid derivable provider. Since the root organization has no
   * SCM provider configured on a fresh test IQ Server, the org scope lets us pin the
   * happy path with just a token.
   */
  @Test
  public void testPostSourceControl_organization_happyPath_returns200() throws Exception {
    Organization org = tempEntity.newOrganization();

    HttpResponse response = apiPostJson(scmOrgPath(org.getId()), newScmBody());

    assertResponseStatus(200, response);
    assertThatJson(response.getBodyText()).node("id").isString().isNotEmpty();
    assertThatJson(response.getBodyText()).node("ownerId").isEqualTo(org.getId());
  }

  /**
   * Seeds via {@code POST /api/v2/sourceControl/organization/{id}} (rather than
   * {@code tempEntity.newSourceControl(...)}) because
   * {@code ApiSourceControlAdapter.convertToDTO} derives the SCM provider from the
   * repository URL on GET — a raw DAO insert with a URL but no provider trips
   * {@code BadRequestException("The root organization source control provider is not
   * set.")}. Going through the POST path keeps the seed valid for the subsequent GET.
   */
  @Test
  public void testGetSourceControl_organization_happyPath_returns200() throws Exception {
    Organization org = tempEntity.newOrganization();
    HttpResponse seed = apiPostJson(scmOrgPath(org.getId()), newScmBody());
    assertResponseStatus(200, seed);

    HttpResponse response = apiGet(scmOrgPath(org.getId()));

    assertResponseStatus(200, response);
    assertThatJson(response.getBodyText()).node("ownerId").isEqualTo(org.getId());
  }

  @Test
  public void testPostSourceControl_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiPostJson(
        scmOrgPath(uniqueId("any-org")),
        newScmBody());

    assertResponseStatus(401, response);
  }

  /**
   * Reliable 400 pin — {@code ApiSourceControlAdapter.getSourceControlProvider} throws
   * {@code BadRequestException} listing valid provider strings when {@code provider}
   * doesn't match any enum. Runs after auth but before DAO, so it pins independently of
   * SCM row existence.
   */
  @Test
  public void testPostSourceControl_invalidProvider_returns400() throws Exception {
    Organization org = tempEntity.newOrganization();
    ApiSourceControlDTO body = newScmBody();
    body.provider = "invalid_scm_provider";

    HttpResponse response = apiPostJson(scmOrgPath(org.getId()), body);

    assertResponseStatus(400, response);
    assertThat(response.getBodyText())
        .containsIgnoringCase("sourcecontrol provider")
        .containsIgnoringCase("is invalid");
  }

  /**
   * 404 pin for the read path — {@code ApiSourceControlService.getSourceControlByOwner}
   * throws {@code NotFoundException("Cannot find SourceControl for <owner>")} when no row
   * exists for the owner. The application exists but has no SCM configured — this is the
   * canonical "empty SCM slot" 404 branch.
   */
  @Test
  public void testGetSourceControl_noConfig_returns404() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-scm-404"), Organization.ROOT_ORGANIZATION_ID);

    HttpResponse response = apiGet(scmAppPath(app.getId()));

    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).containsIgnoringCase("cannot find sourcecontrol");
  }

  /**
   * Validator is a <b>GET</b>, not POST. Incomplete configuration (no SCM row + no
   * hierarchy inheritance) returns 200 with {@code configurationComplete.valid == false}
   * rather than a 400 — the validator's contract is to report status, not reject. This
   * pins the 200 + JSON-shape contract; full green-path validation requires WireMock at
   * the resource-tier ({@code ApiCompositeSourceControlConfigValidatorResourceTest}).
   */
  @Test
  public void testGetCompositeSourceControlConfigValidator_incompleteConfig_returns200() throws Exception {
    Application app =
        tempEntity.newApplication(uniqueId("api-scm-validator"), Organization.ROOT_ORGANIZATION_ID);

    HttpResponse response = apiGet(scmValidatorAppPath(app.getId()));

    assertResponseStatus(200, response);
    assertThatJson(response.getBodyText()).node("configurationComplete").isPresent();
  }

  @Test
  public void testGetCompositeSourceControlConfigValidator_unknownApp_returns404() throws Exception {
    HttpResponse response = apiGet(scmValidatorAppPath(uniqueId("no-app")));

    assertResponseStatus(404, response);
    assertThat(response.getBodyText())
        .containsIgnoringCase("application")
        .containsIgnoringCase("does not exist");
  }

  /**
   * Metrics endpoint returns 200 with an empty {@code results} array when the application
   * has no PR results. Since we don't seed
   * {@code newSourceControlPullRequestResult(...)}, the contract pin is 200 + array
   * shape, not specific metric values.
   */
  @Test
  public void testGetSourceControlMetrics_application_returns200() throws Exception {
    Application app =
        tempEntity.newApplication(uniqueId("api-scm-metrics"), Organization.ROOT_ORGANIZATION_ID);

    HttpResponse response = apiGet(scmMetricsAppPath(app.getId()));

    assertResponseStatus(200, response);
    assertThatJson(response.getBodyText()).node("results").isArray();
  }

  /**
   * 401 anon pin — Shiro runs upstream of the class-level
   * {@code @ProductLicenseEnforcementPoint(SOURCE_CONTROL)} filter, so anon access is
   * rejected before license enforcement. A synthetic app id is sufficient to reach the
   * auth branch.
   */
  @Test
  public void testGetSourceControlMetrics_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(scmMetricsAppPath(uniqueId("any-app")));

    assertResponseStatus(401, response);
  }

  /**
   * Lists GitHub Apps for the given owner. Seeded via {@code tempEntity.newGitHubApp(...)}
   * — populates the DB row without touching GitHub. The response is a JSON array; the
   * assertion pins presence of the seeded app id (index-safe because
   * {@code tempEntity.after()} wipes {@code github_app} between tests).
   */
  @Test
  public void testListGitHubApps_seededOwner_returns200() throws Exception {
    Organization org = tempEntity.newOrganization();
    GitHubApp seeded = tempEntity.newGitHubApp(org.getId());

    HttpResponse response = apiGet(GITHUB_APP_BASE, "ownerId", org.getId());

    assertResponseStatus(200, response);
    assertThatJson(response.getBodyText())
        .inPath("$[*].id")
        .isArray()
        .contains(seeded.getId());
  }

  /**
   * 401 anon pin for the list verb — high-value coverage because CLAUDE.md §5 flags
   * GitHub App auth as the #1 bug-rate area (29 PRs, 2 reverts, 13+ Jira bugs since
   * Dec 2025). Shiro rejects upstream of query-parameter binding, so a synthetic
   * {@code ownerId} suffices.
   */
  @Test
  public void testListGitHubApps_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(GITHUB_APP_BASE, "ownerId", uniqueId("any-org"));

    assertResponseStatus(401, response);
  }

  /**
   * Registration step 1: {@code POST /api/v2/githubApp/manifest} generates a manifest
   * document + persists a registration-state row (no live GitHub call). The response
   * carries a CSRF {@code state} token used by the subsequent redirect callback (out of
   * scope — external GitHub).
   */
  @Test
  public void testPostGitHubAppManifest_happyPath_returns200() throws Exception {
    Organization org = tempEntity.newOrganization();

    HttpResponse response =
        apiPostJsonWithQuery(GITHUB_APP_BASE + "/manifest", Map.of(), "ownerId", org.getId());

    assertResponseStatus(200, response);
    // ApiGitHubAppManifestDTO exposes the CSRF token as `state` (record component),
    // not `stateToken` — see ApiGitHubAppManifestDTO.java.
    assertThatJson(response.getBodyText()).node("state").isString().isNotEmpty();
    assertThatJson(response.getBodyText()).node("manifest").isPresent();
  }

  /**
   * 401 anon pin for the manifest POST — high-value coverage on the GitHub App
   * registration entry point (see CLAUDE.md §5 on GitHub App auth churn). Shiro
   * rejects upstream of the manifest-generation logic and the registration-state
   * DB write, so a synthetic {@code ownerId} suffices.
   */
  @Test
  public void testPostGitHubAppManifest_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiPostJsonWithQuery(
        GITHUB_APP_BASE + "/manifest", Map.of(), "ownerId", uniqueId("any-org"));

    assertResponseStatus(401, response);
  }

  /**
   * Happy-path PUT — {@code parameterPriority: "CI"} is the minimal well-formed body.
   * Uses a raw {@code Map} rather than the {@code com.sonatype.clm.dto} DTO to keep the
   * regression module's dependency footprint focused. Explicit try/finally DELETE is
   * required because {@code ci_integrations_config} is not tracked by
   * {@code TemporaryEntity.after()} — the row would otherwise persist for later tests in
   * the same fork.
   */
  @Test
  public void testPutCiConfiguration_application_happyPath_returns200() throws Exception {
    Application app =
        tempEntity.newApplication(uniqueId("api-ci-cfg"), Organization.ROOT_ORGANIZATION_ID);
    String path = ciConfigAppPath(app.getId());
    try {
      HttpResponse response = apiPutJson(path, Map.of("parameterPriority", "CI"));

      assertResponseStatus(200, response);
      assertThatJson(response.getBodyText()).node("parameterPriority").isEqualTo("CI");
    }
    finally {
      apiDelete(path);
    }
  }

  /**
   * PUT updates an existing SCM row. Seeds via POST so the DAO row exists, then PUTs a
   * modified body (new token) — service returns 200 with the merged DTO. Row cleanup is
   * handled by {@code TemporaryEntity.after()} (see class Javadoc — {@code source_control}
   * table is wiped between tests).
   */
  @Test
  public void testPutSourceControl_organization_happyPath_returns200() throws Exception {
    Organization org = tempEntity.newOrganization();
    HttpResponse seed = apiPostJson(scmOrgPath(org.getId()), newScmBody());
    assertResponseStatus(200, seed);

    ApiSourceControlDTO updated = newScmBody();
    updated.token = "rotated-token";

    HttpResponse response = apiPutJson(scmOrgPath(org.getId()), updated);

    assertResponseStatus(200, response);
    assertThatJson(response.getBodyText()).node("ownerId").isEqualTo(org.getId());
  }

  @Test
  public void testPutSourceControl_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiPutJson(scmOrgPath(uniqueId("any-org")), newScmBody());

    assertResponseStatus(401, response);
  }

  @Test
  public void testDeleteSourceControl_organization_happyPath_returns204() throws Exception {
    Organization org = tempEntity.newOrganization();
    HttpResponse seed = apiPostJson(scmOrgPath(org.getId()), newScmBody());
    assertResponseStatus(200, seed);

    HttpResponse response = apiDelete(scmOrgPath(org.getId()));

    assertResponseStatus(204, response);
  }

  @Test
  public void testDeleteSourceControl_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiDelete(scmOrgPath(uniqueId("any-org")));

    assertResponseStatus(401, response);
  }

  /**
   * PAT ↔ GitHub App transition — the DAO enum value is {@code PAT} (not
   * {@code USER_TOKEN} as the ticket wording suggested). The initial POST implicitly
   * uses PAT; the PUT with {@code authenticationType="GITHUB_APP"} would trigger the
   * transition via {@code gitHubAppDeletionService.reactivateGitHubApps}, but that path
   * requires a real registered {@code github_app} row keyed to the owner. Without one
   * the service rejects the transition with a validation 400. This test pins the round
   * trip of setting {@code authenticationType="PAT"} explicitly on an existing PAT row —
   * the harmless no-op transition that still exercises the enum-parse + update code path.
   * The App→PAT and PAT→App transitions with a real seeded GitHub App are covered at the
   * resource tier ({@code ApiSourceControlResourceTest}).
   */
  @Test
  public void testPutSourceControl_authenticationTypePat_returns200() throws Exception {
    Organization org = tempEntity.newOrganization();
    HttpResponse seed = apiPostJson(scmOrgPath(org.getId()), newScmBody());
    assertResponseStatus(200, seed);

    ApiSourceControlDTO updated = newScmBody();
    updated.authenticationType = "PAT";

    HttpResponse response = apiPutJson(scmOrgPath(org.getId()), updated);

    assertResponseStatus(200, response);
  }

  private static final String SOURCE_CONTROL_CONFIG_BASE =
      PublicApiPaths.SOURCE_CONTROL_CONFIG_RESOURCE_PATH_V2;

  /**
   * <b>Happy-path 200 omitted.</b> The config-tier root SCM endpoint returns
   * {@code 404 "Source control not configured."} on a fresh IQ Server rather than a
   * default DTO — an initial happy-path pin would require seeding the config row (the
   * companion {@link #testPutAndDeleteSourceControlConfig_roundTrip_returns204()} below
   * exercises the PUT + DELETE round trip which is a stronger contract pin). Owner-tier
   * tests exercise the 200 branch after seeding; this class pins only the 401 anon
   * contract for the empty state.
   */
  @Test
  public void testGetSourceControlConfig_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(SOURCE_CONTROL_CONFIG_BASE);

    assertResponseStatus(401, response);
  }

  /**
   * Happy-path PUT + DELETE round trip — the endpoint takes a JsonNode body, so a
   * partial update (bumping the git timeout) round-trips through the service. The
   * following DELETE reverts to defaults so the test is idempotent across reruns.
   */
  @Test
  public void testPutAndDeleteSourceControlConfig_roundTrip_returns204() throws Exception {
    HttpResponse putResponse =
        apiPutJson(SOURCE_CONTROL_CONFIG_BASE, Map.of("gitTimeoutSeconds", 42));
    try {
      assertResponseStatus(204, putResponse);

      HttpResponse deleteResponse = apiDelete(SOURCE_CONTROL_CONFIG_BASE);
      assertResponseStatus(204, deleteResponse);
    }
    finally {
      // Best-effort cleanup only — assertions inside a finally block can suppress the
      // real failure from the try body. If the delete inside try already ran, this is
      // an idempotent no-op; if the try body threw before reaching it, this reverts
      // the seeded row for the next test.
      apiDelete(SOURCE_CONTROL_CONFIG_BASE);
    }
  }

  private static final String COMPOSITE_SOURCE_CONTROL_BASE = PublicApiPaths.COMPOSITE_SOURCE_CONTROL_PATH_V2;

  private static String compositeSourceControlAppPath(final String appInternalId) {
    return COMPOSITE_SOURCE_CONTROL_BASE + "/application/" + appInternalId;
  }

  /**
   * The composite endpoint is <b>GET-only</b> (the recon corrected an AC misread that
   * called it "CRUD"). On an app with no direct SCM + no inherited SCM, the service
   * returns a 200 with an empty DTO — the wire contract pinned here.
   */
  @Test
  public void testGetCompositeSourceControl_application_happyPath_returns200() throws Exception {
    Application app =
        tempEntity.newApplication(uniqueId("api-composite-scm"), Organization.ROOT_ORGANIZATION_ID);

    HttpResponse response = apiGet(compositeSourceControlAppPath(app.getId()));

    assertResponseStatus(200, response);
    assertThatJson(response.getBodyText()).isObject();
  }

  @Test
  public void testGetCompositeSourceControl_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(compositeSourceControlAppPath(uniqueId("any-app")));

    assertResponseStatus(401, response);
  }

  /**
   * 404 pin — {@code ApiGitHubAppService.deleteGitHubApp} throws
   * {@code NotFoundException("GitHub App not found for owner")} when no GitHub App row
   * matches the id + ownerId pair. The organization exists but has no GitHub App
   * registered.
   */
  @Test
  public void testDeleteGitHubApp_unknownId_returns404() throws Exception {
    Organization org = tempEntity.newOrganization();

    HttpResponse response =
        apiDelete(GITHUB_APP_BASE + "/" + uniqueId("no-app"), "ownerId", org.getId());

    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).containsIgnoringCase("github app").containsIgnoringCase("not found");
  }

  @Test
  public void testDeleteGitHubApp_anonymous_returns401() throws Exception {
    HttpResponse response = anonApiDelete(GITHUB_APP_BASE + "/" + uniqueId("any-app"));

    assertResponseStatus(401, response);
  }

  /**
   * GET happy-path after a PUT seed — pins the direct-config read shape. Uses
   * {@code ?direct=true} so the response doesn't include ancestor inheritance (which
   * would depend on root-org state). {@code try/finally} DELETE is required because
   * {@code ci_integrations_config} rows outlive {@code TemporaryEntity.after()}.
   */
  @Test
  public void testGetCiConfiguration_afterPut_happyPath_returns200() throws Exception {
    Application app =
        tempEntity.newApplication(uniqueId("api-ci-get"), Organization.ROOT_ORGANIZATION_ID);
    String path = ciConfigAppPath(app.getId());
    HttpResponse seed = apiPutJson(path, Map.of("parameterPriority", "CI"));
    assertResponseStatus(200, seed);
    try {
      HttpResponse response = apiGet(path, "direct", "true");

      assertResponseStatus(200, response);
      // ApiCiConfigurationResponseDto exposes the config under `data` (see
      // CiConfigurationService#getDirectConfiguration → response.setData(dto)), not
      // `configuration` — the outer envelope is `{"data": {...}, "source": ...}`.
      assertThatJson(response.getBodyText()).node("data").isPresent();
    }
    finally {
      apiDelete(path);
    }
  }

  @Test
  public void testDeleteCiConfiguration_happyPath_returns204() throws Exception {
    Application app =
        tempEntity.newApplication(uniqueId("api-ci-del"), Organization.ROOT_ORGANIZATION_ID);
    String path = ciConfigAppPath(app.getId());
    HttpResponse seed = apiPutJson(path, Map.of("parameterPriority", "CI"));
    assertResponseStatus(200, seed);

    HttpResponse response = apiDelete(path);

    assertResponseStatus(204, response);
  }

  /**
   * 401 anon pins for GET and DELETE — {@code ApiCiConfigurationResource} is not
   * {@code @UnlicensedPath} and carries no Shiro anon exception, so anonymous access
   * is rejected upstream of the service-layer permission check. Synthetic owner ids
   * are sufficient to reach the auth branch.
   */
  @Test
  public void testGetCiConfiguration_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(ciConfigAppPath(uniqueId("any-app")));

    assertResponseStatus(401, response);
  }

  @Test
  public void testDeleteCiConfiguration_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiDelete(ciConfigAppPath(uniqueId("any-app")));

    assertResponseStatus(401, response);
  }
}
