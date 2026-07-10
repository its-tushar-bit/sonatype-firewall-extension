/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.api.v2;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.api.AbstractIqApiTest;
import com.sonatype.clm.testing.api.categories.ApiRegressionTest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.ApiComponentsWithWaiversReportingResource;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

/**
 * API regression suite for {@code GET /api/v2/reports/components/waivers}
 * ({@link ApiComponentsWithWaiversReportingResource}).
 *
 * <p>
 * Endpoint responds with {@code {applicationWaivers: [...], repositoryWaivers: [...]}} — one
 * entry per waived component-in-context. The endpoint is server-wide (no owner filter), so
 * positive-path assertions target the specific package coordinates the test seeded rather
 * than assuming an empty starting state.
 *
 * <p>
 * The one query parameter, {@code format}, filters by component ecosystem (e.g. {@code maven},
 * {@code npm}) — not by ownerId as older docs implied.
 */
@Category(ApiRegressionTest.class)
public class ComponentsWithWaiversReportApiRegressionTest
    extends AbstractIqApiTest
{
  private static final String COMPONENTS_WITH_WAIVERS_PATH =
      PublicApiPaths.REPORTS_RESOURCE_PATH_V2 + ApiComponentsWithWaiversReportingResource.PATH;

  /**
   * Positive-path + shape guard. A waiver seeded against an application-scoped component
   * violation must surface in {@code applicationWaivers}, and the DTO must expose the
   * waiver metadata (via {@code componentPolicyViolations[*].waivedPolicyViolations[*]
   * .policyWaiver.policyWaiverId}) that downstream reporting consumers depend on. The
   * JSONPath filter on {@code artifactId} implicitly proves the seeded component appears —
   * if it weren't in the response, the filter would return empty and
   * {@code .contains(waiver.getId())} would fail.
   */
  @Test
  public void testGetComponentsWithWaivers_returnsExpectedFields() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-cww-shape"), Organization.ROOT_ORGANIZATION_ID);
    Policy policy = tempEntity.newPolicy(app);
    PolicyEvaluation eval =
        tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, uniqueId("api-cww-shape-scan"));

    String artifactId = uniqueId("api-cww-shape-artifact");
    String hash = tempEntity.newRandomHash();
    PolicyWaiver waiver = tempEntity.newWaiver(hash, policy.getId(), app.getId(), "shape check comment");
    tempEntity.newWaivedPolicyViolation(eval, policy,
        ComponentIdentifier.createMavenCoordinates("com.api.cww.shape", artifactId, "1.0"),
        hash, waiver);

    HttpResponse response = apiGet(COMPONENTS_WITH_WAIVERS_PATH);
    assertResponseStatus(200, response);

    // Filter the (nested) componentPolicyViolations array by our seeded artifactId, then walk
    // to the policyWaiver.waiverId to confirm waiver metadata is exposed on the DTO.
    String filter = "$.applicationWaivers[*].stages[*].componentPolicyViolations[?"
        + "(@.component.componentIdentifier.coordinates.artifactId == '" + artifactId + "')]";
    assertThatJson(response.getBodyText())
        .inPath(filter + ".waivedPolicyViolations[*].policyWaiver.policyWaiverId")
        .isArray()
        .contains(waiver.getId());
  }

  /**
   * The {@code format} query parameter filters by component ecosystem. Seeds a maven-scoped
   * waiver in-test and asserts it appears in the unfiltered response and is filtered out by a
   * bogus format value — proving the query param is actually wired to the filter (an
   * empty-array-only assertion is trivially true if the endpoint ignores the parameter and
   * the seed data hasn't landed yet).
   */
  @Test
  public void testGetComponentsWithWaivers_formatFilter_filtersOutNonMatchingEcosystem() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-cww-format"), Organization.ROOT_ORGANIZATION_ID);
    Policy policy = tempEntity.newPolicy(app);
    PolicyEvaluation eval =
        tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, uniqueId("api-cww-format-scan"));
    String artifactId = uniqueId("api-cww-format-artifact");
    String hash = tempEntity.newRandomHash();
    PolicyWaiver waiver = tempEntity.newWaiver(hash, policy.getId(), app.getId(), "format filter check");
    tempEntity.newWaivedPolicyViolation(eval, policy,
        ComponentIdentifier.createMavenCoordinates("com.api.cww.format", artifactId, "1.0"),
        hash, waiver);

    String seededArtifact = "$.applicationWaivers[*].stages[*].componentPolicyViolations[?"
        + "(@.component.componentIdentifier.coordinates.artifactId == '" + artifactId + "')]";

    HttpResponse unfiltered = apiGet(COMPONENTS_WITH_WAIVERS_PATH);
    assertResponseStatus(200, unfiltered);
    assertThatJson(unfiltered.getBodyText())
        .inPath(seededArtifact + ".waivedPolicyViolations[*].policyWaiver.policyWaiverId")
        .isArray()
        .contains(waiver.getId());

    HttpResponse filtered = apiGet(COMPONENTS_WITH_WAIVERS_PATH, "format", "no-such-format-42");
    assertResponseStatus(200, filtered);
    assertThatJson(filtered.getBodyText())
        .inPath(seededArtifact)
        .isArray()
        .isEmpty();
  }

  /** Auth contract: unauthenticated callers get 401. */
  @Test
  public void testGetComponentsWithWaivers_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(COMPONENTS_WITH_WAIVERS_PATH);
    assertResponseStatus(401, response);
  }
}
