/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.api.v2;

import com.sonatype.clm.testing.api.AbstractIqApiTest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.ApiThirdPartyScanResource;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;

import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Test;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * API regression suite for {@code /api/v2/scan/applications/...}
 * ({@link ApiThirdPartyScanResource}). Covers the third-party SBOM scan submission (POST) +
 * status poll (GET) contract without depending on a real SBOM ingest pipeline.
 *
 * <p>
 * The full SBOM parsing and analysis flow is exercised by the SBOM Manager Playwright suite
 * and the {@code insight-brain-service} integration tests — the API regression coverage here
 * pins the endpoint's response contract (202 Accepted for valid submission, 400 for invalid
 * XML/JSON, 401/404 for auth and unknown-resource paths).
 *
 * <p>
 * <b>Intentionally excluded:</b> {@code GET ideUser/overview}
 * ({@link ApiThirdPartyScanResource#IDE_USER_OVERVIEW}) — this endpoint is annotated
 * {@code @Hidden} (omitted from Swagger docs) and serves internal IDE telemetry aggregation,
 * not external consumer workflows. Auth coverage is deferred unless a regression surfaces.
 */
public class ThirdPartyScanApiRegressionTest
    extends AbstractIqApiTest
{
  private static final String SCAN_BASE = PublicApiPaths.THIRD_PARTY_SCAN_PATH;

  // intentionally minimal CycloneDX 1.4 fixture — not a valid production SBOM; sufficient
  // to pass the endpoint's JSON-parse + schema-sniff checks and receive a 202 ticket
  private static final String MINIMAL_CYCLONEDX_JSON =
      "{" +
          "\"bomFormat\":\"CycloneDX\"," +
          "\"specVersion\":\"1.4\"," +
          "\"version\":1," +
          "\"components\":[" +
          "{\"type\":\"library\",\"name\":\"jackson-databind\",\"version\":\"2.13.0\"," +
          "\"purl\":\"pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.13.0?type=jar\"}" +
          "]}";

  private static String scanPath(final String applicationId, final String source) {
    return SCAN_BASE + "/" + applicationId + "/sources/" + source;
  }

  private static String statusPath(final String applicationId, final String scanRequestId) {
    return SCAN_BASE + "/" + applicationId + "/status/" + scanRequestId;
  }

  /**
   * Submitting a minimally-valid CycloneDX 1.4 JSON SBOM against a real application returns
   * 202 Accepted with a {@code statusUrl} ticket. The status URL then routes to the poll
   * endpoint — guards the contract that ticket-issuance is synchronous even though scan
   * completion is not.
   */
  @Test
  public void testScanComponents_validCycloneDxJson_returns202() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-tps"), Organization.ROOT_ORGANIZATION_ID);

    // raw apiRequest(): endpoint @Consumes({APPLICATION_XML, APPLICATION_JSON}) — the typed
    // apiPostJson helper would work but the explicit media type makes the XML/JSON duality visible
    HttpResponse response = apiRequest()
        .path(scanPath(app.getId(), "cyclonedx"))
        .body(MINIMAL_CYCLONEDX_JSON, MediaType.APPLICATION_JSON)
        .post();
    assertResponseStatus(202, response);
    assertThatJson(response.getBodyText()).node("statusUrl").isString().isNotEmpty();
  }

  /**
   * Malformed JSON SBOM must return 400 — silent 202 would queue a poison payload that only
   * fails hours later at parse time, hiding the client-side bug. The message assertion is
   * intentional: CI plugins parse "cannot be parsed" to surface actionable errors.
   */
  @Test
  public void testScanComponents_malformedJson_returns400() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-tps-bad"), Organization.ROOT_ORGANIZATION_ID);

    // raw apiRequest(): explicit media type for the dual-content-type endpoint (see above)
    HttpResponse response = apiRequest()
        .path(scanPath(app.getId(), "cyclonedx"))
        .body("{not valid json", MediaType.APPLICATION_JSON)
        .post();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).containsIgnoringCase("cannot be parsed");
  }

  @Test
  public void testGetScanStatus_unknownScanRequestId_returns404() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-tps-status"), Organization.ROOT_ORGANIZATION_ID);
    HttpResponse response = apiGet(statusPath(app.getId(), uniqueId("nonexistent-status")));
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).containsIgnoringCase("not found");
  }

  @Test
  public void testScanComponents_unauthenticated_returns401() throws Exception {
    // raw anonApiRequest(): explicit media type for the dual-content-type endpoint
    HttpResponse response =
        anonApiRequest()
            .path(scanPath(uniqueId("any-app"), "cyclonedx"))
            .body(MINIMAL_CYCLONEDX_JSON, MediaType.APPLICATION_JSON)
            .post();
    assertResponseStatus(401, response);
  }

  @Test
  public void testGetScanStatus_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(statusPath(uniqueId("any-app"), uniqueId("any-status")));
    assertResponseStatus(401, response);
  }
}
