/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.api.v2;

import java.util.Collections;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.api.AbstractIqApiTest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.SearchTestHelper;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * API regression suite for {@code api/v2/search/component}.
 *
 * <p>
 * Covers: search by hash, by GAV coordinates, by package URL; validation errors when required
 * params are absent; and the unauthenticated auth contract (401).
 *
 * <p>
 * All seeding is performed through {@link SearchTestHelper} which delegates to
 * {@code tempEntity} — no manual DB cleanup is needed beyond what tempEntity provides.
 *
 * <p>
 * No {@code setFeatures(LicensedFeature.COMPONENT_SEARCH)} call is required — the test
 * harness enables all licensed features by default.
 */
public class SearchApiRegressionTest
    extends AbstractIqApiTest
{
  private static final String SEARCH_PATH = PublicApiPaths.SEARCH_RESOURCE_PATH_V2;

  private static final String PLACEHOLDER_HASH = "000000000000000000ff";

  private static final String MAVEN_GROUP = "tomcat";

  private static final String MAVEN_ARTIFACT = "tomcat-util";

  private static final String MAVEN_VERSION = "5.5.23";

  private SearchTestHelper helper;

  @BeforeEach
  public void setUp() {
    helper = new SearchTestHelper(tempEntity);
  }

  @Test
  public void testSearchByHash_returns200WithResult() throws Exception {
    String hash = tempEntity.newRandomHash();
    String appPublicId = uniqueId("search-hash");
    List<SearchTestHelper.ComponentInfo> components = Collections.singletonList(
        new SearchTestHelper.ComponentInfo(hash,
            ComponentIdentifier.createMavenCoordinates(MAVEN_GROUP, MAVEN_ARTIFACT, MAVEN_VERSION, "", "jar"),
            null));
    helper.createAppWithScan(appPublicId, BuildStageType.ID, components, uniqueId("scan-hash"));

    // apiRequest() required: two distinct query params — apiGet() overload supports only one param name
    HttpResponse response = apiRequest()
        .path(SEARCH_PATH)
        .query("stageId", BuildStageType.ID)
        .query("hash", hash)
        .get();

    assertResponseStatus(200, response);
    String body = response.getBodyText();
    assertThatJson(body).node("results").isArray().hasSize(1);
    assertThatJson(body).node("results[0].hash").isEqualTo(hash);
    assertThatJson(body).node("criteria.stageId").isEqualTo(BuildStageType.ID);
  }

  @Test
  public void testSearchByGavCoordinates_returns200WithResult() throws Exception {
    String hash = tempEntity.newRandomHash();
    String appPublicId = uniqueId("search-gav");
    ComponentIdentifier coords =
        ComponentIdentifier.createMavenCoordinates(MAVEN_GROUP, MAVEN_ARTIFACT, MAVEN_VERSION, "", "zip");
    List<SearchTestHelper.ComponentInfo> components =
        Collections.singletonList(new SearchTestHelper.ComponentInfo(hash, coords, null));
    helper.createAppWithScan(appPublicId, BuildStageType.ID, components, uniqueId("scan-gav"));

    // Raw JSON: componentIdentifier is passed as a query param requiring exact string form;
    // ComponentIdentifier serialises via SortedMap, producing this alphabetical key order.
    // Coordinates keys are sorted alphabetically (SortedMap) when serialized
    String gavJson =
        "{\"format\":\"maven\",\"coordinates\":{\"artifactId\":\"" + MAVEN_ARTIFACT + "\","
            + "\"classifier\":\"\",\"extension\":\"zip\",\"groupId\":\"" + MAVEN_GROUP
            + "\",\"version\":\"" + MAVEN_VERSION + "\"}}";

    // apiRequest() required: two distinct query params — apiGet() overload supports only one param name
    HttpResponse response = apiRequest()
        .path(SEARCH_PATH)
        .query("stageId", BuildStageType.ID)
        .query("componentIdentifier", gavJson)
        .get();

    assertResponseStatus(200, response);
    assertThatJson(response.getBodyText()).node("results").isArray().isNotEmpty();
    assertThatJson(response.getBodyText()).node("results[0].hash").isEqualTo(hash);
  }

  @Test
  public void testSearchByPackageUrl_returns200WithResult() throws Exception {
    String hash = tempEntity.newRandomHash();
    String appPublicId = uniqueId("search-purl");
    ComponentIdentifier coords =
        ComponentIdentifier.createMavenCoordinates(MAVEN_GROUP, MAVEN_ARTIFACT, MAVEN_VERSION, "sources", "jar");
    List<SearchTestHelper.ComponentInfo> components =
        Collections.singletonList(new SearchTestHelper.ComponentInfo(hash, coords, null));
    helper.createAppWithScan(appPublicId, BuildStageType.ID, components, uniqueId("scan-purl"));

    // apiRequest() required: two distinct query params — apiGet() overload supports only one param name
    HttpResponse response = apiRequest()
        .path(SEARCH_PATH)
        .query("stageId", BuildStageType.ID)
        .query("packageUrl",
            "pkg:maven/" + MAVEN_GROUP + "/" + MAVEN_ARTIFACT + "@" + MAVEN_VERSION + "?classifier=sources&type=jar")
        .get();

    assertResponseStatus(200, response);
    assertThatJson(response.getBodyText()).node("results").isArray().isNotEmpty();
    assertThatJson(response.getBodyText()).node("results[0].hash").isEqualTo(hash);
  }

  @Test
  public void testSearchMissingStageId_returns400() throws Exception {
    // 400 fires from missing stageId — hash value is irrelevant; use a format-valid placeholder
    HttpResponse response = apiGet(SEARCH_PATH, "hash", PLACEHOLDER_HASH);
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).containsIgnoringCase("Stage has not been specified");
  }

  @Test
  public void testSearchMissingHashAndCoordinates_returns400() throws Exception {
    HttpResponse response = apiGet(SEARCH_PATH, "stageId", BuildStageType.ID);
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).containsIgnoringCase("Neither hash nor coordinates");
  }

  @Test
  public void testSearchUnknownComponent_returns200EmptyResults() throws Exception {
    // PLACEHOLDER_HASH is a fixed valid-format hex hash guaranteed not to match any
    // randomly-generated tempEntity hash — no seeding required to produce 0 results.
    HttpResponse response = apiRequest()
        .path(SEARCH_PATH)
        .query("stageId", BuildStageType.ID)
        .query("hash", PLACEHOLDER_HASH)
        .get();

    assertResponseStatus(200, response);
    assertThatJson(response.getBodyText()).node("results").isArray().hasSize(0);
  }

  /** Auth contract: unauthenticated callers get 401, not 200/403/404. */
  @Test
  public void testSearchComponent_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(SEARCH_PATH);
    assertResponseStatus(401, response);
  }
}
