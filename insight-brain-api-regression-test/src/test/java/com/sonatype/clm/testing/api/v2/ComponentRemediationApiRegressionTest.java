/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.api.v2;

import com.sonatype.clm.testing.api.AbstractIqApiTest;
import com.sonatype.clm.testing.api.categories.ApiRegressionTest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.v2.ApiComponentRemediationResource;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.license.model.LicensedFeature;

import java.util.Map;
import java.util.TreeMap;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * API regression suite for
 * {@code POST /api/v2/components/remediation/{ownerType}/{ownerId}}
 * ({@link ApiComponentRemediationResource}). The endpoint is licensed under
 * {@link LicensedFeature#COMPONENT_EVALUATION} (class-level
 * {@code @ProductLicenseEnforcementPoint}) and gated by
 * {@code Permission.EVALUATE_COMPONENT} on the service method
 * ({@code ApiComponentRemediationService.getSuggestedRemediationForComponent} carries
 * {@code @Authorize(permission = Permission.EVALUATE_COMPONENT) @AuthzContext(Key.INTERNAL_ID)}
 * on the {@code ownerId} parameter). This is the only JAX-RS method on the resource.
 *
 * <p>
 * This suite pins the resource's <em>contracts</em> that fire before HDS traffic:
 * license / auth gating, JAX-RS input-parameter validation, and the resource-side
 * pre-checks in {@code ApiComponentRemediationService.getSuggestedRemediationForComponent}
 * (repository-specific rules on {@code stageId}/{@code scanId}, generic invalid-stage
 * message, invalid component identifier).
 *
 * <p>
 * <b>AC deviation — happy-path intentionally deferred.</b> The Jira AC (CLM-42445) calls
 * for a happy-path assertion on every mutating verb. This class covers 401, 400×3, and
 * 404 but does <em>not</em> add a 200 positive-path test. The endpoint aggregates data
 * across HDS ({@code /rest/vulnerability/details/json}, component-details, third-party
 * scan lookups), the {@code third_party_component} DAO, and the component-details
 * loader — a positive-path assertion that actually reaches 200 would need coordinated
 * HDS stubs across several endpoints plus a seeded scan/component, which would drift
 * from what the endpoint really returns in production. This positive-path coverage is
 * exercised end-to-end by {@code insight-brain-java-functional-test}
 * ({@code ApiComponentRemediationResourceIT} and siblings). Reviewers: this is the only
 * class in this PR that defers the happy-path (200) test — every other class carries a
 * positive-path assertion. The Part 6 PR carries one other AC deviation of a different
 * kind: {@code SecurityVulnerabilityOverrideApiRegressionTest} defers the 400
 * (invalid-body) test on its PUT because the resource has no reliable 400 branch to pin
 * (documented in that class's Javadoc).
 *
 * <p>
 * Full JAX-RS path template lives at
 * {@code PublicApiPaths.COMPONENT_REMEDIATION_PATH_V2}. Because that constant embeds the
 * {@code {ownerType: application|organization|repository}} regex plus an {@code {ownerId}}
 * template parameter, tests build URLs from the hardcoded {@link #REMEDIATION_BASE} rather
 * than reusing the constant directly — regex-in-template substitution is more error-prone
 * than a small literal.
 *
 * <p>
 * The three input-validation tests combine a JSON POST body with a query parameter — they
 * route through {@link #apiPostJsonWithQuery} on the base class so the standard breadcrumb
 * ({@code API POST <path?query> -> <status>}) still lands in the per-class Failsafe report.
 *
 * <p>
 * The {@code _unauthenticated_returns401} test uses a fake {@code uniqueId("any-app")} path
 * segment because Shiro's anonymous-filter 401s before any owner-DAO lookup. If a future
 * refactor moves the owner-existence check ahead of the auth check, this test would surface
 * a 404 instead — a legitimate signal to update the test alongside the refactor.
 */
@Category(ApiRegressionTest.class)
public class ComponentRemediationApiRegressionTest
    extends AbstractIqApiTest
{
  private static final String REMEDIATION_BASE = "api/v2/components/remediation";

  @Before
  public void enableComponentEvaluation() throws Exception {
    setFeatures(LicensedFeature.COMPONENT_EVALUATION);
  }

  /**
   * The service method's {@code @AuthzContext(Key.INTERNAL_ID)} treats the {@code ownerId}
   * path segment as the application's <em>internal</em> id, not its publicId. This differs
   * from the license-override / policy-waiver endpoints and is easy to get wrong; encode it
   * here so tests stay consistent.
   */
  private static String appPath(final String appInternalId) {
    return REMEDIATION_BASE + "/application/" + appInternalId;
  }

  private static String repositoryPath(final String repositoryInternalId) {
    return REMEDIATION_BASE + "/repository/" + repositoryInternalId;
  }

  private static ApiComponentDTOV2 mavenComponent() {
    ApiComponentDTOV2 component = new ApiComponentDTOV2();
    ApiComponentIdentifierDTOV2 identifier = new ApiComponentIdentifierDTOV2();
    identifier.setFormat("maven");
    Map<String, String> coordinates = new TreeMap<>();
    coordinates.put("groupId", "com.example");
    coordinates.put("artifactId", "sample");
    coordinates.put("version", "1.0.0");
    coordinates.put("extension", "jar");
    identifier.setCoordinates(coordinates);
    component.componentIdentifier = identifier;
    return component;
  }

  private Repository newSeededRepository(final String suffix) throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    return tempEntity.newRepository(repositoryManager, uniqueId("api-remed-" + suffix));
  }

  @Test
  public void testGetRemediation_invalidStageId_returns400() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-remed-stage"), Organization.ROOT_ORGANIZATION_ID);

    HttpResponse response = apiPostJsonWithQuery(appPath(app.getId()), mavenComponent(),
        "stageId", "not-a-real-stage");
    assertResponseStatus(400, response);
    // Fragment includes the ":" and echoed stageId so it distinguishes the generic
    // "Invalid stage ID: <id>." branch from the repository-specific "Invalid stage ID for
    // repositories: <id>." branch — matching the specific fragment prevents a future
    // wording tweak on the other branch from silently masking a regression here.
    assertThat(response.getBodyText()).containsIgnoringCase("invalid stage id: not-a-real-stage");
  }

  /**
   * Repository owners have a hard-coded stage ({@code proxy}) — passing any other stage id
   * (even a real one like {@code develop}) triggers a repository-specific 400. Guards the
   * repository-side branch in {@code ApiComponentRemediationService}.
   */
  @Test
  public void testGetRemediation_repositoryWithNonProxyStage_returns400() throws Exception {
    Repository repository = newSeededRepository("repo");

    HttpResponse response = apiPostJsonWithQuery(repositoryPath(repository.getId()), mavenComponent(),
        "stageId", "develop");
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).containsIgnoringCase("invalid stage id for repositories");
  }

  /**
   * Repository owners disallow {@code scanId} entirely — the parameter belongs to the
   * third-party-scan flow which only makes sense for applications/organizations.
   */
  @Test
  public void testGetRemediation_repositoryWithScanId_returns400() throws Exception {
    Repository repository = newSeededRepository("repo-scan");

    HttpResponse response = apiPostJsonWithQuery(repositoryPath(repository.getId()), mavenComponent(),
        "scanId", "some-scan-id");
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).containsIgnoringCase("scan id is not allowed for repositories");
  }

  /**
   * Unknown owner id — the service method's {@code @Authorize @AuthzContext(Key.INTERNAL_ID)}
   * causes {@code AuthorizationChecker} to resolve the owner and throw
   * {@code NotFoundException("Application with ID <id> does not exist.")} before the service
   * body runs. This is the interceptor path, not the resource-body path;
   * this test is intentionally kept as the single interceptor-path pin (labelled
   * {@code _unknownApp_}) and asserts the interceptor's specific 404 fragment so the
   * regression guard is not just "some 4xx" but "the correct 4xx from the correct code
   * path."
   */
  @Test
  public void testGetRemediation_unknownApp_returns404_authInterceptor() throws Exception {
    HttpResponse response = apiPostJson(appPath(uniqueId("no-such-app")), mavenComponent());
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).containsIgnoringCase("does not exist");
  }

  @Test
  public void testGetRemediation_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiPostJson(appPath(uniqueId("any-app")), mavenComponent());
    assertResponseStatus(401, response);
  }
}
