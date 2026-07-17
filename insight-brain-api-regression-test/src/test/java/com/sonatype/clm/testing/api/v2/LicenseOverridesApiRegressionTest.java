/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.api.v2;

import com.sonatype.clm.testing.api.AbstractIqApiTest;
import com.sonatype.clm.testing.api.categories.ApiRegressionTest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.v2.ApiLicenseOverrideResource;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseOverrideDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.license.model.LicensedFeature;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * API regression suite for {@code /api/v2/licenseOverrides/...}
 * ({@link ApiLicenseOverrideResource}). Covers the CRUD triad — POST create/upsert, GET
 * hierarchy lookup, DELETE — plus auth and 404 contracts.
 *
 * <p>
 * Full JAX-RS path template lives at
 * {@code PublicApiPaths.LICENSE_OVERRIDE_RESOURCE_PATH_V2}. Because that constant embeds the
 * {@code {ownerType: application|organization|repository|repository_manager|repository_container}}
 * regex and {@code {ownerId}} template parameter, tests build URLs from
 * {@link #LICENSE_OVERRIDE_BASE} rather than reusing the constant directly.
 *
 * <p>
 * Owner-id conventions (from {@code ApiLicenseOverrideResourceTest}): {@code application}
 * uses the app <em>publicId</em>; {@code organization} / {@code repository} /
 * {@code repository_manager} use internal ids; {@code repository_container} uses a fixed
 * constant. Coverage here targets {@code application} — repo / org / manager permutations
 * are exercised by the resource-level integration tests.
 *
 * <p>
 * <b>JAX-RS methods on {@link ApiLicenseOverrideResource}:</b>
 * <ul>
 * <li>{@code POST addLicenseOverride} — covered (happy path, upsert, missing-identifier 400,
 * unknown owner 404, anon).</li>
 * <li>{@code DELETE deleteLicenseOverride} — covered (happy path, unknown id, anon).</li>
 * <li>{@code GET getAppliedLicenseOverrides} — covered (happy path, missing param, anon).</li>
 * <li>{@code GET getAppliedLicenseOverridesForLegalReviewer} on the {@code /legalReviewer}
 * sub-path — <b>intentionally deferred</b> to resource-level integration tests: it wraps
 * the same service method as the main GET plus an extra permission-check overlay, and
 * duplicating the JAX-RS wiring here without a distinct behavioural claim would just
 * shadow those tests.</li>
 * </ul>
 *
 * <p>
 * POST and DELETE are gated behind {@link LicensedFeature#POLICY_MANAGEMENT}; the GETs have
 * no license gate. {@link #enablePolicyManagement()} enables it for the whole class so 402s
 * never mask a 401/404.
 *
 * <p>
 * <b>404 fragments differ by code path.</b> The service methods
 * ({@code LicenseOverrideService.addLicenseOverride / deleteLicenseOverride /
 * getAppliedLicenseOverridesForRead}) carry {@code @Authorize @AuthzContext(Key.ID)} on
 * the {@code ownerId} parameter, so an unknown-application POST/GET/DELETE 404s from the
 * {@code AuthorizationChecker} interceptor with fragment {@code "Could not find an
 * application with public ID ..."} (before the service body runs). This is the
 * interceptor path — the {@code testCreateLicenseOverride_unknownApp} test is intentionally
 * kept as the single interceptor-path pin for this class. In
 * contrast, unknown-override-id at DELETE reaches the service body (auth passes because
 * the app <em>is</em> real) and resolves through {@code LicenseOverrideDAO.getByIdNotNull}
 * with fragment {@code "Cannot find a license override with ID ..."} — the
 * resource-specific pin. Matching against the specific fragment in each case is
 * intentional.
 *
 * <p>
 * <b>Fake ids in 401 tests.</b> {@code _unauthenticated_returns401} tests pass fabricated
 * ids via {@code uniqueId("any-app")} because Shiro's anonymous-filter 401s before any
 * owner-DAO lookup. If a future refactor moves the owner-existence check ahead of the auth
 * check, these tests would surface a 404 instead — a legitimate signal to update alongside
 * the refactor, not a bug in the test.
 *
 * <p>
 * <b>Row-cleanup.</b> {@code license_override.owner_id} is a plain varchar (not a FK to
 * {@code application}), so {@code TemporaryEntity}'s app deletion does <em>not</em> cascade
 * to override rows. Tests that create overrides via POST clean up in {@code try/finally}
 * via {@link #cleanupOverrideOrIgnore} so successive suite runs do not accumulate orphans.
 */
@Category(ApiRegressionTest.class)
public class LicenseOverridesApiRegressionTest
    extends AbstractIqApiTest
{
  private static final String LICENSE_OVERRIDE_BASE = "api/v2/licenseOverrides";

  @Before
  public void enablePolicyManagement() throws Exception {
    setFeatures(LicensedFeature.POLICY_MANAGEMENT);
  }

  private static String appPath(final String appPublicId) {
    return LICENSE_OVERRIDE_BASE + "/application/" + appPublicId;
  }

  private static String appOverridePath(final String appPublicId, final String overrideId) {
    return appPath(appPublicId) + "/" + overrideId;
  }

  /**
   * Canonical maven identifier shared by every test in this class. The upsert test relies
   * on two POSTs producing the same {@code (ownerId, format, coordinates)} unique-key so the
   * server merges into the same row, and the GET test relies on the query-string identifier
   * matching the DTO used in the preceding POST. Both requirements are met by returning a
   * single {@link ApiComponentIdentifierDTOV2} instance from one place — the POST body
   * builder and {@link #componentIdentifierQueryValue()} both call this. Cross-test
   * collisions are prevented by the fresh app owner-id per test rather than by varying
   * coordinates.
   *
   * <p>
   * <b>Cross-file duplication.</b> The same {@code com.example/sample/1.0.0/jar} maven
   * fixture is inlined in
   * {@link ComponentRemediationApiRegressionTest#mavenComponent()}. Both copies must stay in
   * lock-step; extracting a shared {@code ApiComponentIdentifierFixtures} helper is a
   * follow-up refactor tracked outside this PR.
   */
  private static ApiComponentIdentifierDTOV2 newSampleMavenIdentifier() {
    ApiComponentIdentifierDTOV2 identifier = new ApiComponentIdentifierDTOV2();
    identifier.setFormat("maven");
    Map<String, String> coordinates = new TreeMap<>();
    coordinates.put("groupId", "com.example");
    coordinates.put("artifactId", "sample");
    coordinates.put("version", "1.0.0");
    coordinates.put("extension", "jar");
    identifier.setCoordinates(coordinates);
    return identifier;
  }

  private static ApiLicenseOverrideDTO newOverrideBody(
      final String appPublicId,
      final String comment,
      final String licenseId)
  {
    ApiLicenseOverrideDTO body = new ApiLicenseOverrideDTO();
    body.ownerId = appPublicId;
    body.comment = comment;
    body.licenseIds = Collections.singleton(licenseId);
    body.status = LicenseOverrideStatus.OVERRIDDEN;
    body.componentIdentifier = newSampleMavenIdentifier();
    return body;
  }

  /**
   * Serializes {@link #newSampleMavenIdentifier()} to the JSON form expected by
   * {@code @QueryParam("componentIdentifier") ComponentIdentifier} on
   * {@code ApiLicenseOverrideResource.getAppliedLicenseOverrides}. Reusing the same DTO
   * instance the POST body uses eliminates the drift risk that a hand-rolled parallel Map
   * would introduce (see class-level Javadoc reasoning on the upsert / GET pairing).
   * Delegates to {@link #toJsonQueryParam(Object)} so serialization is centralised in the
   * base class rather than duplicated per test class.
   */
  private static String componentIdentifierQueryValue() {
    return toJsonQueryParam(newSampleMavenIdentifier());
  }

  /**
   * Best-effort cleanup for API-created overrides. Swallows errors so a cleanup failure
   * never masks the primary test assertion. Safe to call from {@code finally}.
   */
  private void cleanupOverrideOrIgnore(final Application app, final String overrideId) {
    if (overrideId == null || overrideId.isBlank()) {
      return;
    }
    try {
      apiDelete(appOverridePath(app.getPublicId(), overrideId));
    }
    catch (Exception ignore) {
      // best-effort — leaked rows are benign (varchar owner_id references a deleted app)
    }
  }

  @Test
  public void testCreateLicenseOverride_application_happyPath_returns200() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-lic-ov"), Organization.ROOT_ORGANIZATION_ID);

    String overrideId = null;
    try {
      HttpResponse response = apiPostJson(appPath(app.getPublicId()),
          newOverrideBody(app.getPublicId(), "initial override", "Apache-2.0"));

      assertResponseStatus(200, response);
      overrideId = response.getBody(ApiLicenseOverrideDTO.class).id;
      assertThat(overrideId).as("license-override create response must return a non-blank id").isNotBlank();
      assertThatJson(response.getBodyText()).node("comment").isEqualTo("initial override");
      assertThatJson(response.getBodyText()).node("licenseIds").isArray().containsExactly("Apache-2.0");
      assertThatJson(response.getBodyText()).node("componentIdentifier.format").isEqualTo("maven");
    }
    finally {
      cleanupOverrideOrIgnore(app, overrideId);
    }
  }

  /**
   * The POST endpoint is an <em>upsert</em>: sending a second body with the same
   * {@code ownerId} + {@code componentIdentifier} updates the existing override in place
   * (same {@code id}, refreshed {@code comment} / {@code licenseIds}). A future change that
   * silently created a duplicate row here would be a hard regression on the license-override
   * write path.
   */
  @Test
  public void testCreateLicenseOverride_upsertsExisting_sameId_updatedFields() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-lic-ov-upsert"), Organization.ROOT_ORGANIZATION_ID);

    String overrideId = null;
    try {
      HttpResponse first = apiPostJson(appPath(app.getPublicId()),
          newOverrideBody(app.getPublicId(), "first comment", "Apache-2.0"));
      assertResponseStatus(200, first);
      String firstId = first.getBody(ApiLicenseOverrideDTO.class).id;
      assertThat(firstId).as("first upsert must return a non-blank id").isNotBlank();
      overrideId = firstId;

      HttpResponse second = apiPostJson(appPath(app.getPublicId()),
          newOverrideBody(app.getPublicId(), "updated comment", "GPL-2.0"));
      assertResponseStatus(200, second);

      assertThatJson(second.getBodyText()).node("id").isEqualTo(firstId);
      assertThatJson(second.getBodyText()).node("comment").isEqualTo("updated comment");
      assertThatJson(second.getBodyText()).node("licenseIds").isArray().containsExactly("GPL-2.0");
    }
    finally {
      cleanupOverrideOrIgnore(app, overrideId);
    }
  }

  @Test
  public void testGetAppliedLicenseOverrides_afterCreate_returnsHierarchyWithOurOverride() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-lic-ov-get"), Organization.ROOT_ORGANIZATION_ID);

    String overrideId = null;
    try {
      HttpResponse created = apiPostJson(appPath(app.getPublicId()),
          newOverrideBody(app.getPublicId(), "for get", "Apache-2.0"));
      assertResponseStatus(200, created);
      overrideId = created.getBody(ApiLicenseOverrideDTO.class).id;
      assertThat(overrideId).as("create-for-get must return a non-blank id").isNotBlank();

      HttpResponse response =
          apiGet(appPath(app.getPublicId()), "componentIdentifier", componentIdentifierQueryValue());
      assertResponseStatus(200, response);
      assertThatJson(response.getBodyText()).node("licenseOverridesByOwner").isArray().isNotEmpty();
      assertThatJson(response.getBodyText())
          .inPath("$.licenseOverridesByOwner[*].licenseOverride.comment")
          .isArray()
          .contains("for get");
    }
    finally {
      cleanupOverrideOrIgnore(app, overrideId);
    }
  }

  @Test
  public void testGetAppliedLicenseOverrides_missingComponentIdentifier_returns400() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-lic-ov-nocid"), Organization.ROOT_ORGANIZATION_ID);

    HttpResponse response = apiGet(appPath(app.getPublicId()));
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).containsIgnoringCase("componentIdentifier is required");
  }

  /**
   * GET has no {@code @ProductLicenseEnforcementPoint}, so this test proves the URL is still
   * behind Shiro anon-filter auth — a regression here would be silently allowing anonymous
   * reads of an org's license overrides.
   */
  @Test
  public void testGetAppliedLicenseOverrides_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(appPath(uniqueId("any-app")));
    assertResponseStatus(401, response);
  }

  @Test
  public void testDeleteLicenseOverride_happyPath_returns204() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-lic-ov-del"), Organization.ROOT_ORGANIZATION_ID);

    String overrideId = null;
    try {
      HttpResponse created = apiPostJson(appPath(app.getPublicId()),
          newOverrideBody(app.getPublicId(), "will delete", "Apache-2.0"));
      assertResponseStatus(200, created);
      overrideId = created.getBody(ApiLicenseOverrideDTO.class).id;
      assertThat(overrideId).as("create-for-delete must return a non-blank id").isNotBlank();

      HttpResponse deleted = apiDelete(appOverridePath(app.getPublicId(), overrideId));
      assertResponseStatus(204, deleted);
    }
    finally {
      cleanupOverrideOrIgnore(app, overrideId);
    }
  }

  /**
   * Regression guard on the resource-body path: {@code LicenseOverrideDAO.getByIdNotNull}
   * must produce a resource-specific 404 ({@code Cannot find a license override with ID}),
   * not an interceptor-layer 404 masking the missing-override branch.
   */
  @Test
  public void testDeleteLicenseOverride_unknownId_returns404() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-lic-ov-del-unknown"), Organization.ROOT_ORGANIZATION_ID);

    HttpResponse response = apiDelete(appOverridePath(app.getPublicId(), uniqueId("no-override")));
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).containsIgnoringCase("cannot find a license override");
  }

  /**
   * AC-mandated 400 (invalid body) for POST addLicenseOverride. Sending a payload without
   * the required {@code componentIdentifier} field reaches
   * {@code ComponentIdentifierValidator.validate(null)} inside
   * {@code LicenseOverrideService.addLicenseOverride} and 400s with a resource-body fragment.
   * Distinct from
   * {@link #testGetAppliedLicenseOverrides_missingComponentIdentifier_returns400()} (that's
   * the GET's own 400 on the query parameter) and from
   * {@link #testCreateLicenseOverride_unknownApp_returns404()} (that's the interceptor-layer
   * auth path). The fragment {@code "component identifier cannot be null"} is unique to this
   * validator, so a regression that changed the check point would flip this to some other
   * error and fail the fragment assertion.
   */
  @Test
  public void testCreateLicenseOverride_missingComponentIdentifier_returns400() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-lic-ov-badbody"), Organization.ROOT_ORGANIZATION_ID);

    ApiLicenseOverrideDTO body = new ApiLicenseOverrideDTO();
    body.ownerId = app.getPublicId();
    body.comment = "missing identifier";
    body.licenseIds = Collections.singleton("Apache-2.0");
    body.status = LicenseOverrideStatus.OVERRIDDEN;
    // componentIdentifier intentionally left null — this is the invalid-body under test.

    HttpResponse response = apiPostJson(appPath(app.getPublicId()), body);
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).containsIgnoringCase("component identifier cannot be null");
  }

  @Test
  public void testCreateLicenseOverride_unknownApp_returns404() throws Exception {
    String missingAppId = uniqueId("nonexistent-app");

    HttpResponse response = apiPostJson(appPath(missingAppId),
        newOverrideBody(missingAppId, "n/a", "Apache-2.0"));
    assertResponseStatus(404, response);
    assertThat(response.getBodyText())
        .containsIgnoringCase("could not find an application")
        .containsIgnoringCase("public id");
  }

  @Test
  public void testCreateLicenseOverride_unauthenticated_returns401() throws Exception {
    String fakeAppId = uniqueId("any-app");

    HttpResponse response = anonApiPostJson(appPath(fakeAppId),
        newOverrideBody(fakeAppId, "n/a", "Apache-2.0"));
    assertResponseStatus(401, response);
  }

  @Test
  public void testDeleteLicenseOverride_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiDelete(appOverridePath(uniqueId("any-app"), uniqueId("any-override")));
    assertResponseStatus(401, response);
  }
}
