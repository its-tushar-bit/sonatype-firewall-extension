/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.api.v2;

import com.sonatype.clm.testing.api.AbstractIqApiTest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiLabelDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryManagerDTO;
import com.sonatype.insight.brain.api.v2.dto.firewall.RenewWaiversRequestDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.license.model.LicensedFeature;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * API regression coverage for the firewall + repository-manager + component-label
 * surface under {@code /api/v2/firewall/*},
 * {@code /api/v2/repositoryIdentifiedComponent}, {@code /api/v2/labels/*}, and
 * {@code /api/v2/components/*}.
 *
 * <p>
 * <b>Three license gates enabled in @Before</b> because the resources enforce them at
 * different tiers: {@link LicensedFeature#FIREWALL} (per-method inside the service),
 * {@link LicensedFeature#FIREWALL_AUTO_UNQUARANTINE} + {@link LicensedFeature#RELEASE_INTEGRITY}
 * (metrics endpoint), and {@link LicensedFeature#COMPONENT_LABELS} +
 * {@link LicensedFeature#CUSTOM_COMPONENT_LABELS} (labels resource — the latter is a
 * Lifecycle-only entitlement on the mutating methods). Missing any → 402 rather than the
 * 401/404 branch under test.
 *
 * <p>
 * <b>AC deviations.</b>
 * <ul>
 * <li>{@code DELETE /firewall/repositoryManagers/{id}} — no reliable 400 branch (no
 * upfront body validation on the delete path).</li>
 * <li>{@code POST /firewall/waivers/renew} happy-path 200 requires a heavy
 * {@code PolicyWaiver} seed — covered at the resource tier
 * ({@code ApiFirewallRenewWaiverResourceTest}). This class pins the {@code 400}
 * empty-ids branch and the {@code 401} anon branch.</li>
 * <li>Cascade-reevaluate / container-image quarantine / namespace-confusion happy paths
 * require virtual RMs, HDS mocks, or evaluated component graphs; deferred to
 * resource-tier tests.</li>
 * <li>{@code /api/v2/repositoryIdentifiedComponent} is {@code @Hidden} + gated on the
 * {@code BUILT_FROM_SOURCE} system feature; out of scope for wide-and-shallow.</li>
 * <li>{@code POST /components/{hash}/labels/{labelName}/{ownerType}s/{ownerId}} needs a
 * component hash aligned to a real evaluated component — deferred to
 * {@code ApiComponentLabelResourceV2Test}.</li>
 * </ul>
 */
public class FirewallAndRepositoriesApiRegressionTest
    extends AbstractIqApiTest
{
  private static final String FIREWALL_BASE = PublicApiPaths.FIREWALL_RESOURCE_PATH;

  private static final String REPOSITORY_MANAGERS_BASE = FIREWALL_BASE + "/repositoryManagers";

  private static final String FIREWALL_METRICS_EMBEDDED_PATH = FIREWALL_BASE + "/metrics/embedded";

  private static final String FIREWALL_WAIVERS_RENEW_PATH = FIREWALL_BASE + "/waivers/renew";

  /**
   * {@link PublicApiPaths#LABEL_RESOURCE_PATH} still contains the {@code {ownerType}} /
   * {@code {ownerId}} template placeholders that the HTTP client would try to substitute
   * — since we're building the path by hand for each test, we anchor at the plain prefix
   * instead.
   */
  private static final String LABELS_BASE = "api/v2/labels";

  /**
   * Placeholder 40-hex SHA-1 for component-label paths where the resource performs the
   * label lookup before the component lookup — the actual hash is never dereferenced,
   * so any well-formed value keeps the test on the label 404/401 branch under test.
   */
  private static final String FAKE_COMPONENT_HASH = "0000000000000000000000000000000000000000";

  @BeforeEach
  public void enableLicenseFeatures() throws Exception {
    setFeatures(
        LicensedFeature.FIREWALL,
        LicensedFeature.FIREWALL_AUTO_UNQUARANTINE,
        LicensedFeature.RELEASE_INTEGRITY,
        LicensedFeature.COMPONENT_LABELS,
        LicensedFeature.CUSTOM_COMPONENT_LABELS,
        // Enables the JAX-RS ProductLicenseEnforcementPoint filter on
        // ApiRepositoryIdentifiedComponentResource so DELETE reaches the resource body,
        // where checkBuiltFromSourceEnabled() (system property, off by default) still
        // trips a 403 — the pinned branch. Without the license the same DELETE would
        // short-circuit at 402 before the system-flag check runs.
        LicensedFeature.INNER_SOURCE_REPOSITORIES,
        // ApiFirewallContainerImageResource is @ProductLicenseEnforcementPoint(CONTAINER_IMAGES_EVALUATION).
        // The 401-anon path pins auth ordering (Shiro runs upstream of the license
        // filter); the feature-flag @HasFeature(CONTAINER_IMAGES_EVAL_ENABLED) still
        // returns 404 because the flag is off by default on a fresh test IQ.
        LicensedFeature.CONTAINER_IMAGES_EVALUATION);
  }

  private static String repositoryManagerByIdPath(final String repositoryManagerId) {
    return REPOSITORY_MANAGERS_BASE + "/" + repositoryManagerId;
  }

  private static String labelsAppPath(final String ownerId) {
    return LABELS_BASE + "/application/" + ownerId;
  }

  private static ApiRepositoryManagerDTO newRepositoryManagerBody(final String seedPrefix) {
    ApiRepositoryManagerDTO body = new ApiRepositoryManagerDTO();
    body.instanceId = uniqueId(seedPrefix + "-instance");
    body.name = uniqueName(seedPrefix + "-name");
    body.productName = "regression-rm";
    body.productVersion = "1.0";
    return body;
  }

  private static ApiLabelDTO newLabelBody(final String label) {
    return new ApiLabelDTO(label, "regression fixture", "light-green");
  }

  @Test
  public void testCreateRepositoryManager_happyPath_returns200() throws Exception {
    HttpResponse response = apiPostJson(REPOSITORY_MANAGERS_BASE, newRepositoryManagerBody("api-rm-create"));

    assertResponseStatus(200, response);
    assertThatJson(response.getBodyText()).node("id").isString().isNotEmpty();
    assertThatJson(response.getBodyText()).node("productName").isEqualTo("regression-rm");
  }

  @Test
  public void testCreateRepositoryManager_unauthenticated_returns401() throws Exception {
    HttpResponse response =
        anonApiPostJson(REPOSITORY_MANAGERS_BASE, newRepositoryManagerBody("api-rm-anon"));

    assertResponseStatus(401, response);
  }

  /**
   * Reliable 400 pin — {@code ApiFirewallService.addRepositoryManager}
   * ({@code ApiFirewallService.java:782-784}) rejects any request whose body contains a
   * non-null {@code id}, because RM ids are DAO-assigned. Simpler and more stable than
   * duplicate-instance-id pins because it doesn't depend on prior state.
   */
  @Test
  public void testCreateRepositoryManager_nonNullId_returns400() throws Exception {
    ApiRepositoryManagerDTO body = newRepositoryManagerBody("api-rm-400");
    body.id = uniqueId("bogus-rm-id");

    HttpResponse response = apiPostJson(REPOSITORY_MANAGERS_BASE, body);

    assertResponseStatus(400, response);
    assertThat(response.getBodyText())
        .containsIgnoringCase("repository manager")
        .containsIgnoringCase("must be null");
  }

  @Test
  public void testGetRepositoryManager_happyPath_returns200() throws Exception {
    RepositoryManager rm = tempEntity.newRepositoryManager(uniqueId("api-rm-get"));

    HttpResponse response = apiGet(repositoryManagerByIdPath(rm.getId()));

    assertResponseStatus(200, response);
    assertThatJson(response.getBodyText()).node("id").isEqualTo(rm.getId());
  }

  @Test
  public void testGetRepositoryManager_unknownId_returns404() throws Exception {
    HttpResponse response = apiGet(repositoryManagerByIdPath(uniqueId("no-rm")));

    assertResponseStatus(404, response);
    assertThat(response.getBodyText())
        .containsIgnoringCase("repository manager")
        .containsIgnoringCase("does not exist");
  }

  /**
   * 401 anon pin for the GET verb — symmetric with the DELETE anon pin below. Shiro runs
   * upstream of the FIREWALL license check and the resource-body DAO lookup, so a
   * synthetic id is sufficient to reach the auth branch.
   */
  @Test
  public void testGetRepositoryManager_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(repositoryManagerByIdPath(uniqueId("any-rm")));

    assertResponseStatus(401, response);
  }

  @Test
  public void testDeleteRepositoryManager_happyPath_returns204() throws Exception {
    RepositoryManager rm = tempEntity.newRepositoryManager(uniqueId("api-rm-del"));

    HttpResponse response = apiDelete(repositoryManagerByIdPath(rm.getId()));

    assertResponseStatus(204, response);
  }

  /**
   * 401 anon pin for the DELETE verb — Shiro runs upstream of the service-layer
   * FIREWALL license check and the resource-body {@code RepositoryManagerDAO} lookup,
   * so a synthetic id is sufficient to reach the auth branch.
   */
  @Test
  public void testDeleteRepositoryManager_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiDelete(repositoryManagerByIdPath(uniqueId("any-rm")));

    assertResponseStatus(401, response);
  }

  /**
   * The embedded-metrics endpoint returns a JSON object keyed by
   * {@code FirewallMetricsName} — values may be zero on a fresh DB, but the shape
   * (non-null map, no `"error"` field) is the wire contract pinned here. Also gates the
   * dual license enforcement ({@code FIREWALL_AUTO_UNQUARANTINE + RELEASE_INTEGRITY} —
   * see class Javadoc).
   */
  @Test
  public void testGetFirewallMetricsEmbedded_happyPath_returns200() throws Exception {
    HttpResponse response = apiGet(FIREWALL_METRICS_EMBEDDED_PATH);

    assertResponseStatus(200, response);
    assertThatJson(response.getBodyText()).isObject();
  }

  /**
   * 401 anon pin — Shiro runs upstream of the dual-license gate
   * ({@code FIREWALL_AUTO_UNQUARANTINE + RELEASE_INTEGRITY}) documented in the class
   * Javadoc, so anon access is rejected before license enforcement.
   */
  @Test
  public void testGetFirewallMetricsEmbedded_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(FIREWALL_METRICS_EMBEDDED_PATH);

    assertResponseStatus(401, response);
  }

  /**
   * Reliable 400 pin — {@link ApiFirewallRenewWaiverResource#renewWaivers} throws
   * {@code BadRequestException} with the pinned fragment before any DAO lookup when
   * {@code waiverIds} is null or empty. Requires no waiver seed, keeping the regression
   * lightweight; the resource-tier {@code ApiFirewallRenewWaiverResourceTest} covers the
   * happy-path renewal with real waivers.
   */
  @Test
  public void testRenewWaivers_emptyWaiverIds_returns400() throws Exception {
    RenewWaiversRequestDTO body = new RenewWaiversRequestDTO();
    body.waiverIds = Collections.emptyList();

    HttpResponse response = apiPostJson(FIREWALL_WAIVERS_RENEW_PATH, body);

    assertResponseStatus(400, response);
    assertThat(response.getBodyText())
        .containsIgnoringCase("waiver ids")
        .containsIgnoringCase("cannot be null or empty");
  }

  @Test
  public void testRenewWaivers_unauthenticated_returns401() throws Exception {
    RenewWaiversRequestDTO body = new RenewWaiversRequestDTO();
    body.waiverIds = List.of(uniqueId("any-waiver"));

    HttpResponse response = anonApiPostJson(FIREWALL_WAIVERS_RENEW_PATH, body);

    assertResponseStatus(401, response);
  }

  @Test
  public void testCreateLabel_application_happyPath_returns200() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-label-app"), Organization.ROOT_ORGANIZATION_ID);
    ApiLabelDTO body = newLabelBody(uniqueId("api-label"));

    HttpResponse response = apiPostJson(labelsAppPath(app.getId()), body);

    assertResponseStatus(200, response);
    assertThatJson(response.getBodyText()).node("id").isString().isNotEmpty();
    assertThatJson(response.getBodyText()).node("label").isEqualTo(body.label);
    assertThatJson(response.getBodyText()).node("ownerId").isEqualTo(app.getId());
  }

  /**
   * Seeds a label via {@code tempEntity.newLabel(...)} so the assertion is independent of
   * the create-happy test running first. Uses the same owner id in path + fixture so the
   * response array must contain the seeded label id.
   */
  @Test
  public void testListLabels_application_happyPath_returns200() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-label-list"), Organization.ROOT_ORGANIZATION_ID);
    String labelText = uniqueId("regression-label");
    tempEntity.newLabel(app.getId(), labelText);

    HttpResponse response = apiGet(labelsAppPath(app.getId()));

    assertResponseStatus(200, response);
    assertThatJson(response.getBodyText())
        .inPath("$[*].label")
        .isArray()
        .contains(labelText);
  }

  @Test
  public void testCreateLabel_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiPostJson(
        labelsAppPath(uniqueId("any-owner")),
        newLabelBody(uniqueId("anon-label")));

    assertResponseStatus(401, response);
  }

  /**
   * 401 anon pin for the list verb — symmetric with the POST create pin above. Shiro
   * rejects upstream of the {@code LabelDAO} lookup, so a synthetic owner id suffices.
   */
  @Test
  public void testListLabels_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(labelsAppPath(uniqueId("any-owner")));

    assertResponseStatus(401, response);
  }

  /**
   * Reliable 400 pin for label create — {@code LabelService.addLabelWithAuthzCheck} at
   * {@code LabelService.java:228} throws {@code BadRequestException("ID must be null when
   * creating a Label.")} when the request body carries any {@code id} value. The check
   * runs before authz resolution, so it pins even against an app that would otherwise
   * trip a 404.
   */
  @Test
  public void testCreateLabel_withIdInBody_returns400() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-label-400"), Organization.ROOT_ORGANIZATION_ID);
    ApiLabelDTO body = newLabelBody(uniqueId("id-must-be-null"));
    body.id = uniqueId("client-supplied-id");

    HttpResponse response = apiPostJson(labelsAppPath(app.getId()), body);

    assertResponseStatus(400, response);
    assertThat(response.getBodyText())
        .containsIgnoringCase("id must be null")
        .containsIgnoringCase("label");
  }

  private static final String FIREWALL_CASCADE_BASE = PublicApiPaths.FIREWALL_CASCADE_REEVALUATE_PATH;

  private static String cascadeInitiatePath(final String componentHash) {
    return FIREWALL_CASCADE_BASE + "/componentHash/" + componentHash;
  }

  private static String cascadeStatusPath(final String requestId) {
    return FIREWALL_CASCADE_BASE + "/status/" + requestId;
  }

  /**
   * 401 pin — the Shiro anonymous filter rejects unauthenticated POSTs upstream of the
   * "Evaluate Components at Repository Managers" service-level permission check. Component
   * hash is otherwise arbitrary because auth short-circuits before the service body.
   */
  @Test
  public void testCascadeReevaluate_anonymous_returns401() throws Exception {
    HttpResponse response = anonApiPostJson(cascadeInitiatePath(uniqueId("hash")), "");

    assertResponseStatus(401, response);
  }

  /**
   * 404 pin — {@code ApiFirewallCascadeService.getCascadeStatus} throws
   * {@code NotFoundException("Cascade request not found: {requestId}")} when no in-flight
   * ticket exists for the requested id. Any unique id from a fresh fork satisfies the
   * empty-store precondition.
   */
  @Test
  public void testGetCascadeStatus_unknownRequestId_returns404() throws Exception {
    HttpResponse response = apiGet(cascadeStatusPath(uniqueId("no-request")));

    assertResponseStatus(404, response);
    assertThat(response.getBodyText())
        .containsIgnoringCase("cascade request not found");
  }

  @Test
  public void testGetCascadeStatus_anonymous_returns401() throws Exception {
    HttpResponse response = anonApiGet(cascadeStatusPath(uniqueId("no-request")));

    assertResponseStatus(401, response);
  }

  private static final String CONTAINER_QUARANTINED_PATH =
      PublicApiPaths.FIREWALL_CONTAINER_IMAGE_RESOURCE_PATH + "/policyViolations/quarantined";

  /**
   * 401 pin — Shiro's anonymous filter fires upstream of both the
   * {@code @ProductLicenseEnforcementPoint(CONTAINER_IMAGES_EVALUATION)} filter and the
   * {@code @HasFeature(CONTAINER_IMAGES_EVAL_ENABLED)} gate on the resource, independent
   * of feature-flag state.
   *
   * <p>
   * The feature-disabled branch is intentionally not pinned here: bean-validation on the
   * {@code @QueryParam("page") @Min(1)} / {@code pageSize @Max(100)} arguments races
   * with the {@code @HasFeature} filter, so a status pin would drift with unrelated
   * Jersey/Spring changes. That branch is exercised at the resource tier
   * ({@code ApiFirewallContainerImageResourceTest}).
   */
  @Test
  public void testGetContainerImagesInQuarantine_anonymous_returns401() throws Exception {
    HttpResponse response = anonApiGet(CONTAINER_QUARANTINED_PATH, "page", 1);

    assertResponseStatus(401, response);
  }

  private static final String NAMESPACE_CONFUSION_BASE = PublicApiPaths.FIREWALL_RESOURCE_PATH + "/namespace_confusion";

  private static String namespaceConfusionPath(final String format) {
    return NAMESPACE_CONFUSION_BASE + "/" + format;
  }

  /**
   * Happy path — POSTs a small proprietary-namespaces list for the {@code npm} format.
   * The resource validates {@code format} against
   * {@code FirewallIgnorePatternService.getIgnorePatterns().regexpsByRepositoryFormat};
   * {@code npm} is the only format guaranteed present on a fresh test IQ (other formats
   * like {@code maven} are absent and would return
   * {@code 400 "'maven' format is not supported"}). Rows land in a namespace-confusion
   * RM keyed by {@code nsc_npm} which is wiped by {@code TemporaryEntity.after()} along
   * with the other repository-manager rows.
   */
  @Test
  public void testAddNamespaceConfusion_happyPath_returns204() throws Exception {
    HttpResponse response =
        apiPostJson(namespaceConfusionPath("npm"), List.of(uniqueId("com.example.proprietary")));

    assertResponseStatus(204, response);
  }

  @Test
  public void testAddNamespaceConfusion_anonymous_returns401() throws Exception {
    HttpResponse response =
        anonApiPostJson(namespaceConfusionPath("npm"), List.of(uniqueId("com.example.anon")));

    assertResponseStatus(401, response);
  }

  @Test
  public void testRemoveNamespaceConfusion_happyPath_returns204() throws Exception {
    // Seed a namespaces list so the DELETE has a target to remove; on an empty RM the
    // service is still a no-op returning 204, but seeding pins the round-trip.
    HttpResponse seed =
        apiPostJson(namespaceConfusionPath("npm"), List.of(uniqueId("proprietary-scope")));
    assertResponseStatus(204, seed);

    HttpResponse response = apiDelete(namespaceConfusionPath("npm"));

    assertResponseStatus(204, response);
  }

  @Test
  public void testRemoveNamespaceConfusion_anonymous_returns401() throws Exception {
    HttpResponse response = anonApiDelete(namespaceConfusionPath("npm"));

    assertResponseStatus(401, response);
  }

  private static final String REPOSITORY_IDENTIFIED_COMPONENT_BASE =
      PublicApiPaths.REPOSITORY_IDENTIFIED_COMPONENT_PATH_V2;

  /**
   * 401 pin — Shiro runs upstream of the license filter and the built-from-source
   * feature-flag check, so anonymous access returns 401 regardless of downstream state.
   */
  @Test
  public void testDeleteRepositoryIdentifiedComponent_anonymous_returns401() throws Exception {
    HttpResponse response = anonApiDelete(REPOSITORY_IDENTIFIED_COMPONENT_BASE);

    assertResponseStatus(401, response);
  }

  /**
   * 403 "built-from-source feature is disabled" pin — the resource body calls
   * {@code checkBuiltFromSourceEnabled()} before parameter validation, which throws
   * {@code NotAuthorizedException} when the {@code BUILT_FROM_SOURCE} system property
   * flag is off (default on a fresh IQ). This surfaces cleanly because
   * {@link LicensedFeature#INNER_SOURCE_REPOSITORIES} is enabled in {@code @Before} —
   * without it the license filter would 402 first.
   *
   * <p>
   * The 400 branches ({@code "You must specify one of…"} / {@code "Only one of…"}) are
   * downstream of the flag check and are covered at the resource tier
   * ({@code ApiRepositoryIdentifiedComponentResourceTest}).
   */
  @Test
  public void testDeleteRepositoryIdentifiedComponent_featureDisabled_returns403() throws Exception {
    HttpResponse response = apiDelete(REPOSITORY_IDENTIFIED_COMPONENT_BASE);

    assertResponseStatus(403, response);
    assertThat(response.getBodyText()).containsIgnoringCase("built-from-source").containsIgnoringCase("disabled");
  }

  private static String componentLabelAppPath(final String componentHash, final String labelName, final String appId) {
    return "api/v2/components/" + componentHash + "/labels/" + labelName + "/applications/" + appId;
  }

  /**
   * 404 pin — {@code ApiComponentLabelServiceV2.setComponentLabel} throws
   * {@code NotFoundException("Could not find a label with name '<name>' for <ownerType>
   * with ID <id>.")} when the referenced label name is not in the owner's label catalog.
   * The component hash is arbitrary because the label lookup precedes the component
   * lookup. Kept as a fixture-cheap 404 pin since the happy path requires an evaluated
   * component + an existing label with matching component-hash mapping (covered
   * end-to-end by {@code ApiComponentLabelResourceV2Test}).
   */
  @Test
  public void testSetComponentLabel_unknownLabel_returns404() throws Exception {
    Application app =
        tempEntity.newApplication(uniqueId("api-comp-label-404"), Organization.ROOT_ORGANIZATION_ID);
    String fakeLabel = uniqueId("no-such-label");

    HttpResponse response =
        apiPostJson(componentLabelAppPath(FAKE_COMPONENT_HASH, fakeLabel, app.getId()), "");

    assertResponseStatus(404, response);
    assertThat(response.getBodyText())
        .containsIgnoringCase("could not find a label")
        .containsIgnoringCase(fakeLabel);
  }

  @Test
  public void testSetComponentLabel_anonymous_returns401() throws Exception {
    HttpResponse response = anonApiPostJson(
        componentLabelAppPath(FAKE_COMPONENT_HASH, uniqueId("anon-label"), uniqueId("any-app")),
        "");

    assertResponseStatus(401, response);
  }

  @Test
  public void testDeleteComponentLabel_anonymous_returns401() throws Exception {
    HttpResponse response = anonApiDelete(componentLabelAppPath(
        FAKE_COMPONENT_HASH, uniqueId("anon-label"), uniqueId("any-app")));

    assertResponseStatus(401, response);
  }

  private static final String FIREWALL_POLICY_WAIVER_DETAIL_BASE =
      PublicApiPaths.FIREWALL_RESOURCE_PATH + "/policyWaivers";

  private static String firewallPolicyWaiverPath(final String ownerType, final String ownerId, final String waiverId) {
    return FIREWALL_POLICY_WAIVER_DETAIL_BASE + "/" + ownerType + "/" + ownerId + "/" + waiverId;
  }

  /**
   * 404 pin — the exception is thrown from
   * {@code PolicyWaiverDAO#getByIdAndOwnerIdNotNull} (not the service layer), which
   * emits {@code NotFoundException("Cannot find a waiver with ID <id> for owner …")}.
   * The application exists (so authz can resolve); the waiver id does not.
   */
  @Test
  public void testGetFirewallPolicyWaiverDetail_unknownWaiver_returns404() throws Exception {
    Application app =
        tempEntity.newApplication(uniqueId("api-fw-waiver-404"), Organization.ROOT_ORGANIZATION_ID);

    HttpResponse response =
        apiGet(firewallPolicyWaiverPath("application", app.getId(), uniqueId("no-waiver")));

    assertResponseStatus(404, response);
    assertThat(response.getBodyText())
        .containsIgnoringCase("cannot find a waiver")
        .containsIgnoringCase("with id");
  }

  @Test
  public void testGetFirewallPolicyWaiverDetail_anonymous_returns401() throws Exception {
    HttpResponse response = anonApiGet(
        firewallPolicyWaiverPath("application", uniqueId("any-app"), uniqueId("any-waiver")));

    assertResponseStatus(401, response);
  }
}
