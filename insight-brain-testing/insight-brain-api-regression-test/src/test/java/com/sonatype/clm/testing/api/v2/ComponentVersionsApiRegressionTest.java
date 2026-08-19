/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.api.v2;

import java.util.List;
import java.util.Map;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.api.AbstractIqApiTest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.service.ApiComponentVersionsServiceV2;

import org.junit.jupiter.api.Test;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * API regression suite for {@code api/v2/components/versions}.
 *
 * <p>
 * Covers: versions by GAV coordinates; versions by package URL; and the unauthenticated
 * auth contract (401).
 *
 * <p>
 * This endpoint is stateless — no {@code tempEntity} seeding is required. All data comes
 * from the HDS stub at {@code rest/component/versions}. The response is a plain JSON array of
 * version strings.
 */
public class ComponentVersionsApiRegressionTest
    extends AbstractIqApiTest
{
  private static final String VERSIONS_PATH = PublicApiPaths.COMPONENT_VERSIONS_PATH_V2;

  private static final List<String> TEST_VERSIONS = List.of("1.0.0", "1.1.0", "2.0.0");

  private static final String GROUP_ID = "g1";

  private static final String ARTIFACT_ID = "a1";

  private static final String VERSION = "v1";

  private static final String CLASSIFIER = "c1";

  private static final String EXTENSION = "e1";

  @Test
  public void testGetComponentVersions_byCoordinates_returns200() throws Exception {
    hdsRespondWith(TEST_VERSIONS)
        .atUri(ApiComponentVersionsServiceV2.HDS_COMPONENT_VERSIONS_LIST_PATH);

    ApiComponentIdentifierDTOV2 request = ApiComponentIdentifierDTOV2.fromComponentIdentifier(
        ComponentIdentifier.createMavenCoordinates(GROUP_ID, ARTIFACT_ID, VERSION, CLASSIFIER, EXTENSION));

    HttpResponse response = apiPostJson(VERSIONS_PATH, request);

    assertResponseStatus(200, response);
    assertThatJson(response.getBodyText()).isArray().hasSize(3);
  }

  @Test
  public void testGetComponentVersions_byPackageUrl_returns200() throws Exception {
    hdsRespondWith(TEST_VERSIONS)
        .atUri(ApiComponentVersionsServiceV2.HDS_COMPONENT_VERSIONS_LIST_PATH);

    Map<String, String> request = Map.of("packageUrl",
        "pkg:maven/" + GROUP_ID + "/" + ARTIFACT_ID + "@" + VERSION + "?classifier=" + CLASSIFIER + "&type="
            + EXTENSION);

    HttpResponse response = apiPostJson(VERSIONS_PATH, request);

    assertResponseStatus(200, response);
    assertThatJson(response.getBodyText()).isArray().hasSize(3);
  }

  @Test
  public void testGetComponentVersions_noIdentifier_returns400() throws Exception {
    // Empty body — ComponentIdentifierValidator rejects the empty identifier: "A component identifier must have a
    // format."
    HttpResponse response = apiPostJson(VERSIONS_PATH, Map.of());
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).containsIgnoringCase("must have a format");
  }

  /** Auth contract on POST: unauthenticated callers get 401 before the body is parsed. */
  @Test
  public void testGetComponentVersions_unauthenticated_returns401() throws Exception {
    HttpResponse response =
        anonApiPostJson(VERSIONS_PATH,
            Map.of("packageUrl", "pkg:maven/" + GROUP_ID + "/" + ARTIFACT_ID + "@" + VERSION));
    assertResponseStatus(401, response);
  }
}
