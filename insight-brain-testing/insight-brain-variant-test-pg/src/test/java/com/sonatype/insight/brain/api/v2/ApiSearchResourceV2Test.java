/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.SearchTestHelper.ComponentInfo;
import com.sonatype.insight.brain.api.v2.SearchTestHelper.PolicyViolationInfo;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiSearchResultDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiSearchResultsDTOV2;
import com.sonatype.insight.brain.variant.IqPostgresTest;
import com.sonatype.insight.brain.variant.IqTestContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqPostgresTest
class ApiSearchResourceV2Test
{
  // Injected by IqPostgresServerExtension: the extension owns the shared, reused server.
  private IqTestContext ctx;

  private SearchTestHelper helper;

  private Map<String, List<ComponentInfo>> appToComponentMap;

  @BeforeEach
  void init() {
    helper = new SearchTestHelper(ctx.tempEntity());
    appToComponentMap = helper.createTestComponentInfoForTwoApps("search-app-1", "search-app-2");
  }

  private HttpRequest searchRequest(String stageId) {
    return ctx.restRequest().path(PublicApiPaths.SEARCH_RESOURCE_PATH_V2).query("stageId", stageId);
  }

  private Consumer<HttpRequest> hash(String hash) {
    return httpRequest -> httpRequest.query("hash", hash);
  }

  private Consumer<HttpRequest> coords(ComponentIdentifier componentIdentifier) {
    return httpRequest -> httpRequest.query("componentIdentifier", componentIdentifier);
  }

  private Consumer<HttpRequest> purl(String packageUrl) {
    return httpRequest -> httpRequest.query("packageUrl", packageUrl);
  }

  private void assertSearchResult(
      ApiSearchResultDTOV2 result,
      String appId,
      String appName,
      String hash,
      String packageUrl,
      ComponentIdentifier componentIdentifier,
      Integer threatLevel) throws Exception
  {
    assertThat(result.applicationId).isEqualTo(appId);
    assertThat(result.applicationName).isEqualTo(appName);
    assertThat(result.reportHtmlUrl).matches("ui/links/application/.+/report/[^\\s]+");
    assertThat(result.reportUrl).isNotNull();
    ctx.assertResponseStatus(200, HttpRequest.to(result.reportUrl).followRedirects().get());
    assertThat(result.hash).isEqualTo(hash);
    assertThat(result.packageUrl).isEqualTo(packageUrl);
    assertThat(ApiComponentIdentifierDTOV2.toComponentIdentifier(result.componentIdentifier))
        .isEqualTo(componentIdentifier);
    assertThat(result.threatLevel).isEqualTo(threatLevel);
  }

  private void sortResultsByAppIdAndHash(ApiSearchResultsDTOV2 resultsDTO) {
    resultsDTO.results.sort((o1, o2) -> {
      String applicationIdHash1 = o1.applicationId + o1.hash;
      String applicationIdHash2 = o2.applicationId + o2.hash;
      return applicationIdHash1.compareTo(applicationIdHash2);
    });
  }

  @Test
  void testSearchComponent_MissingStageId() throws Exception {
    HttpResponse response = searchRequest("").with(hash("12345678901234567890")).get();
    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Stage has not been specified.");
  }

  @Test
  void testSearchComponent_InvalidStageId() throws Exception {
    HttpResponse response = searchRequest("invalid").with(hash("12345678901234567890")).get();
    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Invalid stage: invalid.");
  }

  @Test
  void testSearchComponent_MissingHashAndCoordinates() throws Exception {
    HttpResponse response = searchRequest(Stage.ID_BUILD).get();
    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText())
        .isEqualTo("Neither hash nor coordinates of component to search for have been specified.");
  }

  @Test
  void testSearchComponent_InvalidHash() throws Exception {
    HttpResponse response = searchRequest(Stage.ID_BUILD).with(hash("invalid-hash")).get();
    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Invalid hash: invalid-hash.");
  }

  @Test
  void testSearchComponent_TooShortHash() throws Exception {
    HttpResponse response = searchRequest(Stage.ID_BUILD).with(hash("1249e25aebb15358bed")).get();
    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Invalid hash: 1249e25aebb15358bed.");
  }

  @Test
  void testSearchComponent_RestrictedToStage() throws Exception {
    helper.createAppWithScan("search-app-1", Stage.ID_BUILD, appToComponentMap.get("search-app-1"), "scanId1");
    ctx.mockReport("scanId1", "/" + getClass().getSimpleName() + "/report");
    helper.createAppWithScan("search-app-2", Stage.ID_RELEASE, appToComponentMap.get("search-app-2"), "scanId2");
    ctx.mockReport("scanId2", "/" + getClass().getSimpleName() + "/report");

    HttpResponse response = searchRequest(Stage.ID_BUILD).with(hash("1249e25aebb15358bedd")).get();
    ctx.assertResponseStatus(200, response);
    ApiSearchResultsDTOV2 results = response.getBody(ApiSearchResultsDTOV2.class);
    assertThat(results).isNotNull();
    assertThat(results.results).hasSize(1);
    assertSearchResult(results.results.get(0), "search-app-1", "SEARCH-APP-1", "1249e25aebb15358bedd",
        "pkg:maven/tomcat/tomcat-util@5.5.23?type=jar",
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "", "jar"), 8);
  }

  @Test
  void testSearchComponent_ByHash() throws Exception {
    helper.createAppWithScan("search-app-1", Stage.ID_BUILD, appToComponentMap.get("search-app-1"), "scanId1");
    ctx.mockReport("scanId1", "/" + getClass().getSimpleName() + "/report");
    helper.createAppWithScan("search-app-2", Stage.ID_BUILD, appToComponentMap.get("search-app-2"), "scanId2");
    ctx.mockReport("scanId2", "/" + getClass().getSimpleName() + "/report");

    HttpResponse response = searchRequest(Stage.ID_BUILD).with(hash("1249e25aebb15358bedd")).get();
    ctx.assertResponseStatus(200, response);
    ApiSearchResultsDTOV2 results = response.getBody(ApiSearchResultsDTOV2.class);
    assertThat(results).isNotNull();
    assertThat(results.results).hasSize(2);
    assertSearchResult(results.results.get(0), "search-app-1", "SEARCH-APP-1", "1249e25aebb15358bedd",
        "pkg:maven/tomcat/tomcat-util@5.5.23?type=jar",
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "", "jar"), 8);
    assertSearchResult(results.results.get(1), "search-app-2", "SEARCH-APP-2", "1249e25aebb15358bedd",
        "pkg:maven/tomcat/tomcat-util@5.5.23?type=jar",
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "", "jar"), 4);
  }

  @Test
  void testSearchComponent_ByHash_FullHashString() throws Exception {
    helper.createAppWithScan("search-app-1", Stage.ID_BUILD, appToComponentMap.get("search-app-1"), "scanId");
    ctx.mockReport("scanId", "/" + getClass().getSimpleName() + "/report");

    HttpResponse response = searchRequest(Stage.ID_BUILD).with(hash("1249E25aEbb15358bEdd00000000000000000000")).get();
    ctx.assertResponseStatus(200, response);
    ApiSearchResultsDTOV2 results = response.getBody(ApiSearchResultsDTOV2.class);
    assertThat(results).isNotNull();
    assertThat(results.results).hasSize(1);
    assertSearchResult(results.results.get(0), "search-app-1", "SEARCH-APP-1", "1249e25aebb15358bedd",
        "pkg:maven/tomcat/tomcat-util@5.5.23?type=jar",
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "", "jar"), 8);
  }

  @Test
  void testSearchComponent_ByHash_UnknownComponent() throws Exception {
    helper.createAppWithScan("search-app-1", Stage.ID_BUILD, appToComponentMap.get("search-app-1"), "scanId");
    ctx.mockReport("scanId", "/" + getClass().getSimpleName() + "/report");

    HttpResponse response = searchRequest(Stage.ID_BUILD).with(hash("69b58197caabec2e0d06")).get();
    ctx.assertResponseStatus(200, response);
    ApiSearchResultsDTOV2 results = response.getBody(ApiSearchResultsDTOV2.class);
    assertThat(results).isNotNull();
    assertThat(results.results).hasSize(1);
    assertSearchResult(results.results.get(0), "search-app-1", "SEARCH-APP-1", "69b58197caabec2e0d06", null, null,
        null);
  }

  @Test
  void testSearchComponent_ByGav_WithEmptyCoordinates() throws Exception {
    helper.createAppWithScan("search-app-1", Stage.ID_BUILD, appToComponentMap.get("search-app-1"), "scanId");
    ctx.mockReport("scanId", "/" + getClass().getSimpleName() + "/report");

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("", "tomcat-util", "");
    HttpResponse response = searchRequest(Stage.ID_BUILD).with(coords(componentIdentifier)).get();
    ctx.assertResponseStatus(200, response);
    ApiSearchResultsDTOV2 results = response.getBody(ApiSearchResultsDTOV2.class);
    assertThat(results).isNotNull();
    assertThat(results.results).hasSize(4);
    sortResultsByAppIdAndHash(results);
    assertSearchResult(results.results.get(0), "search-app-1", "SEARCH-APP-1", "1249e25aebb15358bedd",
        "pkg:maven/tomcat/tomcat-util@5.5.23?type=jar",
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "", "jar"), 8);
    assertSearchResult(results.results.get(1), "search-app-1", "SEARCH-APP-1", "2aa135385b1f449292e8",
        "pkg:maven/tomcat/tomcat-util@5.5.23?type=zip",
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "", "zip"), 8);
    assertSearchResult(results.results.get(2), "search-app-1", "SEARCH-APP-1", "a18da38b875b4658b4e9",
        "pkg:maven/tomcat/tomcat-util@5.5.23?classifier=sources&type=zip",
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "sources", "zip"), 8);
    assertSearchResult(results.results.get(3), "search-app-1", "SEARCH-APP-1", "c85713867bef4a3b91c9",
        "pkg:maven/tomcat/tomcat-util@5.5.23?classifier=sources&type=jar",
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "sources", "jar"), 8);
  }

  @Test
  void testSearchComponent_ByGav() throws Exception {
    helper.createAppWithScan("search-app-1", Stage.ID_BUILD, appToComponentMap.get("search-app-1"), "scanId1");
    ctx.mockReport("scanId1", "/" + getClass().getSimpleName() + "/report");
    helper.createAppWithScan("search-app-2", Stage.ID_BUILD, appToComponentMap.get("search-app-2"), "scanId2");
    ctx.mockReport("scanId2", "/" + getClass().getSimpleName() + "/report");

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("tomcat", "*", "*");
    HttpResponse response = searchRequest(Stage.ID_BUILD).with(coords(componentIdentifier)).get();
    assertSearchComponent_ByGav(response);
  }

  @Test
  void testSearchComponent_Purl_ByGav() throws Exception {
    helper.createAppWithScan("search-app-1", Stage.ID_BUILD, appToComponentMap.get("search-app-1"), "scanId1");
    ctx.mockReport("scanId1", "/" + getClass().getSimpleName() + "/report");
    helper.createAppWithScan("search-app-2", Stage.ID_BUILD, appToComponentMap.get("search-app-2"), "scanId2");
    ctx.mockReport("scanId2", "/" + getClass().getSimpleName() + "/report");

    String packageUrl = "pkg:maven/tomcat/*@*";
    HttpResponse response = searchRequest(Stage.ID_BUILD).with(purl(packageUrl)).get();
    assertSearchComponent_ByGav(response);
  }

  private void assertSearchComponent_ByGav(final HttpResponse response) throws Exception {
    ctx.assertResponseStatus(200, response);
    ApiSearchResultsDTOV2 results = response.getBody(ApiSearchResultsDTOV2.class);
    assertThat(results).isNotNull();
    assertThat(results.results).hasSize(9);
    sortResultsByAppIdAndHash(results);
    assertSearchResult(results.results.get(0), "search-app-1", "SEARCH-APP-1", "1249e25aebb15358bedd",
        "pkg:maven/tomcat/tomcat-util@5.5.23?type=jar",
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "", "jar"), 8);
    assertSearchResult(results.results.get(1), "search-app-1", "SEARCH-APP-1", "2aa135385b1f449292e8",
        "pkg:maven/tomcat/tomcat-util@5.5.23?type=zip",
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "", "zip"), 8);
    assertSearchResult(results.results.get(2), "search-app-1", "SEARCH-APP-1", "a18da38b875b4658b4e9",
        "pkg:maven/tomcat/tomcat-util@5.5.23?classifier=sources&type=zip",
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "sources", "zip"), 8);
    assertSearchResult(results.results.get(3), "search-app-1", "SEARCH-APP-1", "a397f601582e5ccd4b1a",
        "pkg:maven/tomcat/servlets-default@5.5.4?type=jar",
        ComponentIdentifier.createMavenCoordinates("tomcat", "servlets-default", "5.5.4", "", "jar"), null);
    assertSearchResult(results.results.get(4), "search-app-1", "SEARCH-APP-1", "c85713867bef4a3b91c9",
        "pkg:maven/tomcat/tomcat-util@5.5.23?classifier=sources&type=jar",
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "sources", "jar"), 8);
    assertSearchResult(results.results.get(5), "search-app-2", "SEARCH-APP-2", "1249e25aebb15358bedd",
        "pkg:maven/tomcat/tomcat-util@5.5.23?type=jar",
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "", "jar"), 4);
    assertSearchResult(results.results.get(6), "search-app-2", "SEARCH-APP-2", "2aa135385b1f449292e8",
        "pkg:maven/tomcat/tomcat-util@5.5.23?type=zip",
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "", "zip"), 4);
    assertSearchResult(results.results.get(7), "search-app-2", "SEARCH-APP-2", "a18da38b875b4658b4e9",
        "pkg:maven/tomcat/tomcat-util@5.5.23?classifier=sources&type=zip",
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "sources", "zip"), 4);
    assertSearchResult(results.results.get(8), "search-app-2", "SEARCH-APP-2", "c85713867bef4a3b91c9",
        "pkg:maven/tomcat/tomcat-util@5.5.23?classifier=sources&type=jar",
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "sources", "jar"), 4);
  }

  @Test
  void testSearchComponent_ByGavec_WithNonEmptyClassifier() throws Exception {
    helper.createAppWithScan("search-app-1", Stage.ID_BUILD, appToComponentMap.get("search-app-1"), "scanId1");
    ctx.mockReport("scanId1", "/" + getClass().getSimpleName() + "/report");
    helper.createAppWithScan("search-app-2", Stage.ID_BUILD, appToComponentMap.get("search-app-2"), "scanId2");
    ctx.mockReport("scanId2", "/" + getClass().getSimpleName() + "/report");

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("tomcat", "*", "*", "sources",
        "jar");
    HttpResponse response = searchRequest(Stage.ID_BUILD).with(coords(componentIdentifier)).get();
    assertSearchComponent_ByGavec_WithNonEmptyClassifier(response);
  }

  @Test
  void testSearchComponent_Purl_ByGavec_WithNonEmptyClassifier() throws Exception {
    helper.createAppWithScan("search-app-1", Stage.ID_BUILD, appToComponentMap.get("search-app-1"), "scanId1");
    ctx.mockReport("scanId1", "/" + getClass().getSimpleName() + "/report");
    helper.createAppWithScan("search-app-2", Stage.ID_BUILD, appToComponentMap.get("search-app-2"), "scanId2");
    ctx.mockReport("scanId2", "/" + getClass().getSimpleName() + "/report");

    String pacakageUrl = "pkg:maven/tomcat/*@*?classifier=sources&type=jar";
    HttpResponse response = searchRequest(Stage.ID_BUILD).with(purl(pacakageUrl)).get();
    assertSearchComponent_ByGavec_WithNonEmptyClassifier(response);
  }

  private void assertSearchComponent_ByGavec_WithNonEmptyClassifier(final HttpResponse response) throws Exception {
    ctx.assertResponseStatus(200, response);
    ApiSearchResultsDTOV2 results = response.getBody(ApiSearchResultsDTOV2.class);
    assertThat(results).isNotNull();
    assertThat(results.results).hasSize(2);
    sortResultsByAppIdAndHash(results);
    assertSearchResult(results.results.get(0), "search-app-1", "SEARCH-APP-1", "c85713867bef4a3b91c9",
        "pkg:maven/tomcat/tomcat-util@5.5.23?classifier=sources&type=jar",
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "sources", "jar"), 8);
    assertSearchResult(results.results.get(1), "search-app-2", "SEARCH-APP-2", "c85713867bef4a3b91c9",
        "pkg:maven/tomcat/tomcat-util@5.5.23?classifier=sources&type=jar",
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "sources", "jar"), 4);
  }

  @Test
  void testSearchComponent_ByGavec_WithEmptyClassifier() throws Exception {
    helper.createAppWithScan("search-app-1", Stage.ID_BUILD, appToComponentMap.get("search-app-1"), "scanId");
    ctx.mockReport("scanId", "/" + getClass().getSimpleName() + "/report");

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "*",
        "", "jar");
    HttpResponse response = searchRequest(Stage.ID_BUILD).with(coords(componentIdentifier)).get();
    ctx.assertResponseStatus(200, response);
    ApiSearchResultsDTOV2 results = response.getBody(ApiSearchResultsDTOV2.class);
    assertThat(results).isNotNull();
    assertThat(results.results).hasSize(1);
    sortResultsByAppIdAndHash(results);
    assertSearchResult(results.results.get(0), "search-app-1", "SEARCH-APP-1", "1249e25aebb15358bedd",
        "pkg:maven/tomcat/tomcat-util@5.5.23?type=jar",
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "", "jar"), 8);
  }

  @Test
  void testSearchComponent_Purl_ByGavec_WithEmptyClassifier() throws Exception {
    helper.createAppWithScan("search-app-1", Stage.ID_BUILD, appToComponentMap.get("search-app-1"), "scanId");
    ctx.mockReport("scanId", "/" + getClass().getSimpleName() + "/report");

    // With Purl we cannot represent an empty query param (which will be dropped/ignored by PackageURL constructor
    // (inline with purl-spec). So effectively this is treated it as no classifier (null)
    // which will be wildcarded for the search query.
    String packageUrl = "pkg:maven/tomcat/tomcat-util@*?type=jar&classifier=";
    HttpResponse response = searchRequest(Stage.ID_BUILD).with(purl(packageUrl)).get();
    ctx.assertResponseStatus(200, response);
    ApiSearchResultsDTOV2 results = response.getBody(ApiSearchResultsDTOV2.class);
    assertThat(results).isNotNull();
    assertThat(results.results).hasSize(2);
    sortResultsByAppIdAndHash(results);
    assertSearchResult(results.results.get(0), "search-app-1", "SEARCH-APP-1", "1249e25aebb15358bedd",
        "pkg:maven/tomcat/tomcat-util@5.5.23?type=jar",
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "", "jar"), 8);
    assertSearchResult(results.results.get(1), "search-app-1", "SEARCH-APP-1", "c85713867bef4a3b91c9",
        "pkg:maven/tomcat/tomcat-util@5.5.23?classifier=sources&type=jar",
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "sources", "jar"), 8);
  }

  @Test
  void testSearchComponent_ByGave_WithNullClassifier() throws Exception {
    helper.createAppWithScan("search-app-1", Stage.ID_BUILD, appToComponentMap.get("search-app-1"), "scanId1");
    ctx.mockReport("scanId1", "/" + getClass().getSimpleName() + "/report");
    helper.createAppWithScan("search-app-2", Stage.ID_BUILD, appToComponentMap.get("search-app-2"), "scanId2");
    ctx.mockReport("scanId2", "/" + getClass().getSimpleName() + "/report");

    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", null, "jar");
    HttpResponse response = searchRequest(Stage.ID_BUILD).with(coords(componentIdentifier)).get();
    ctx.assertResponseStatus(200, response);
    ApiSearchResultsDTOV2 results = response.getBody(ApiSearchResultsDTOV2.class);
    assertThat(results).isNotNull();
    assertThat(results.results).hasSize(4);
    sortResultsByAppIdAndHash(results);
    assertSearchResult(results.results.get(0), "search-app-1", "SEARCH-APP-1", "1249e25aebb15358bedd",
        "pkg:maven/tomcat/tomcat-util@5.5.23?type=jar",
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "", "jar"), 8);
    assertSearchResult(results.results.get(1), "search-app-1", "SEARCH-APP-1", "c85713867bef4a3b91c9",
        "pkg:maven/tomcat/tomcat-util@5.5.23?classifier=sources&type=jar",
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "sources", "jar"), 8);
    assertSearchResult(results.results.get(2), "search-app-2", "SEARCH-APP-2", "1249e25aebb15358bedd",
        "pkg:maven/tomcat/tomcat-util@5.5.23?type=jar",
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "", "jar"), 4);
    assertSearchResult(results.results.get(3), "search-app-2", "SEARCH-APP-2", "c85713867bef4a3b91c9",
        "pkg:maven/tomcat/tomcat-util@5.5.23?classifier=sources&type=jar",
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "sources", "jar"), 4);
  }

  @Test
  void testSearchComponent_ByNugetComponent() throws Exception {
    helper.createAppWithScan("search-app-1", Stage.ID_BUILD, appToComponentMap.get("search-app-1"), "scanId1");
    ctx.mockReport("scanId1", "/" + getClass().getSimpleName() + "/report");
    helper.createAppWithScan("search-app-2", Stage.ID_BUILD, appToComponentMap.get("search-app-2"), "scanId2");
    ctx.mockReport("scanId2", "/" + getClass().getSimpleName() + "/report");

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createNugetCoordinates("simplejson", "*");
    HttpResponse response = searchRequest(Stage.ID_BUILD).with(coords(componentIdentifier)).get();
    assertSearchComponent_ByNugetComponent(response);
  }

  @Test
  void testSearchComponent_Purl_ByNugetComponent() throws Exception {
    helper.createAppWithScan("search-app-1", Stage.ID_BUILD, appToComponentMap.get("search-app-1"), "scanId1");
    ctx.mockReport("scanId1", "/" + getClass().getSimpleName() + "/report");
    helper.createAppWithScan("search-app-2", Stage.ID_BUILD, appToComponentMap.get("search-app-2"), "scanId2");
    ctx.mockReport("scanId2", "/" + getClass().getSimpleName() + "/report");

    String packageUrl = "pkg:nuget/simplejson@*";
    HttpResponse response = searchRequest(Stage.ID_BUILD).with(purl(packageUrl)).get();
    assertSearchComponent_ByNugetComponent(response);
  }

  private void assertSearchComponent_ByNugetComponent(final HttpResponse response) throws Exception {
    ctx.assertResponseStatus(200, response);
    ApiSearchResultsDTOV2 results = response.getBody(ApiSearchResultsDTOV2.class);
    assertThat(results).isNotNull();
    assertThat(results.results).isNotNull();
    assertThat(results.results).hasSize(2);
    sortResultsByAppIdAndHash(results);
    assertSearchResult(results.results.get(0), "search-app-1", "SEARCH-APP-1", "2143b68270b82576110f",
        "pkg:nuget/simplejson@0.38.0", ComponentIdentifier.createNugetCoordinates("simplejson", "0.38.0"), 8);
    assertSearchResult(results.results.get(1), "search-app-2", "SEARCH-APP-2", "2143b68270b82576110f",
        "pkg:nuget/simplejson@0.38.0", ComponentIdentifier.createNugetCoordinates("simplejson", "0.38.0"), 4);
  }

  @Test
  void testSearchComponent_ByGavAndHash() throws Exception {
    helper.createAppWithScan("search-app-1", Stage.ID_BUILD, appToComponentMap.get("search-app-1"), "scanId1");
    ctx.mockReport("scanId1", "/" + getClass().getSimpleName() + "/report");
    helper.createAppWithScan("search-app-2", Stage.ID_BUILD, appToComponentMap.get("search-app-2"), "scanId2");
    ctx.mockReport("scanId2", "/" + getClass().getSimpleName() + "/report");

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("tomcat", "*", "*");
    HttpResponse response =
        searchRequest(Stage.ID_BUILD).with(hash("1249e25aebb15358bedd")).with(coords(componentIdentifier)).get();
    assertSearchComponent_ByGavAndHash(response);
  }

  @Test
  void testSearchComponent_Purl_ByGavAndHash() throws Exception {
    helper.createAppWithScan("search-app-1", Stage.ID_BUILD, appToComponentMap.get("search-app-1"), "scanId1");
    ctx.mockReport("scanId1", "/" + getClass().getSimpleName() + "/report");
    helper.createAppWithScan("search-app-2", Stage.ID_BUILD, appToComponentMap.get("search-app-2"), "scanId2");
    ctx.mockReport("scanId2", "/" + getClass().getSimpleName() + "/report");

    String packageUrl = "pkg:maven/tomcat/*@*";
    HttpResponse response =
        searchRequest(Stage.ID_BUILD).with(hash("1249e25aebb15358bedd")).with(purl(packageUrl)).get();
    assertSearchComponent_ByGavAndHash(response);
  }

  private void assertSearchComponent_ByGavAndHash(final HttpResponse response) throws Exception {
    ctx.assertResponseStatus(200, response);
    ApiSearchResultsDTOV2 results = response.getBody(ApiSearchResultsDTOV2.class);
    assertThat(results).isNotNull();
    assertThat(results.results).hasSize(2);
    sortResultsByAppIdAndHash(results);
    assertSearchResult(results.results.get(0), "search-app-1", "SEARCH-APP-1", "1249e25aebb15358bedd",
        "pkg:maven/tomcat/tomcat-util@5.5.23?type=jar",
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "", "jar"), 8);
    assertSearchResult(results.results.get(1), "search-app-2", "SEARCH-APP-2", "1249e25aebb15358bedd",
        "pkg:maven/tomcat/tomcat-util@5.5.23?type=jar",
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "", "jar"), 4);
  }

  @Test
  void testSearchComponent_ByGavAndHash_NoIntersection() throws Exception {
    helper.createAppWithScan("search-app-1", Stage.ID_BUILD, appToComponentMap.get("search-app-1"), "scanId1");
    ctx.mockReport("scanId1", "/" + getClass().getSimpleName() + "/report");
    helper.createAppWithScan("search-app-2", Stage.ID_BUILD, appToComponentMap.get("search-app-2"), "scanId2");
    ctx.mockReport("scanId2", "/" + getClass().getSimpleName() + "/report");

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("*", "tomcat-util", "*");
    HttpResponse response =
        searchRequest(Stage.ID_BUILD).with(hash("a397f601582e5ccd4b1a")).with(coords(componentIdentifier)).get();
    assertSearchComponent_EmptyResults(response);
  }

  @Test
  void testSearchComponent_Purl_ByGavAndHash_NoIntersection() throws Exception {
    helper.createAppWithScan("search-app-1", Stage.ID_BUILD, appToComponentMap.get("search-app-1"), "scanId1");
    ctx.mockReport("scanId1", "/" + getClass().getSimpleName() + "/report");
    helper.createAppWithScan("search-app-2", Stage.ID_BUILD, appToComponentMap.get("search-app-2"), "scanId2");
    ctx.mockReport("scanId2", "/" + getClass().getSimpleName() + "/report");
    String packageUrl = "pkg:maven/*/tomcat-util@*";
    HttpResponse response =
        searchRequest(Stage.ID_BUILD).with(hash("a397f601582e5ccd4b1a")).with(purl(packageUrl)).get();
    assertSearchComponent_EmptyResults(response);
  }

  @Test
  void testSearchComponent_EchoCriteria() throws Exception {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("gid", "aid", "1");
    HttpResponse response =
        searchRequest(Stage.ID_BUILD).with(hash("1249e25aebb15358bedd")).with(coords(componentIdentifier)).get();
    ctx.assertResponseStatus(200, response);
    ApiSearchResultsDTOV2 results = response.getBody(ApiSearchResultsDTOV2.class);
    assertThat(results).isNotNull();
    assertThat(results.results).isEmpty();
    assertThat(results.criteria).isNotNull();
    assertThat(results.criteria.stageId).isEqualTo(Stage.ID_BUILD);
    assertThat(results.criteria.hash).isEqualTo("1249e25aebb15358bedd");
    assertThat(ApiComponentIdentifierDTOV2.toComponentIdentifier(results.criteria.componentIdentifier))
        .isEqualTo(ComponentIdentifier.createMavenCoordinates("gid", "aid", "1", "*", "*"));
  }

  @Test
  void testSearchComponent_VerifyCriteria_ComponentIdentifier() throws Exception {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("gid", "aid", "1");
    HttpResponse response = searchRequest(Stage.ID_BUILD).with(coords(componentIdentifier)).get();
    ctx.assertResponseStatus(200, response);
    ApiSearchResultsDTOV2 results = response.getBody(ApiSearchResultsDTOV2.class);
    assertThat(ApiComponentIdentifierDTOV2.toComponentIdentifier(results.criteria.componentIdentifier))
        .isEqualTo(ComponentIdentifier.createMavenCoordinates("gid", "aid", "1", "*", "*"));
    assertThat(results.criteria.hash).isNull();
    assertThat(results.criteria.packageUrl).isNull();
  }

  @Test
  void testSearchComponent_VerifyCriteria_Hash() throws Exception {
    HttpResponse response = searchRequest(Stage.ID_BUILD).with(hash("1249e25aebb15358bedd")).get();
    ctx.assertResponseStatus(200, response);
    ApiSearchResultsDTOV2 results = response.getBody(ApiSearchResultsDTOV2.class);
    assertThat(results.criteria.hash).isEqualTo("1249e25aebb15358bedd");
    assertThat(results.criteria.componentIdentifier).isNull();
    assertThat(results.criteria.packageUrl).isNull();
  }

  @Test
  void testSearchComponent_VerifyCriteria_Purl() throws Exception {
    HttpResponse response = searchRequest(Stage.ID_BUILD).with(purl("pkg:maven/gid/aid@1?type=*&classifier=*")).get();
    ctx.assertResponseStatus(200, response);
    ApiSearchResultsDTOV2 results = response.getBody(ApiSearchResultsDTOV2.class);
    assertThat(results.criteria.packageUrl).isEqualTo("pkg:maven/gid/aid@1?type=*&classifier=*");
    assertThat(results.criteria.componentIdentifier).isNull();
    assertThat(results.criteria.hash).isNull();
  }

  @Test
  void testSearchComponent_NoHitsAmongAppComponents() throws Exception {
    helper.createAppWithScan("search-app-1", Stage.ID_BUILD, appToComponentMap.get("search-app-1"), "scanId");
    ctx.mockReport("scanId", "/" + getClass().getSimpleName() + "/report");

    HttpResponse response = searchRequest(Stage.ID_BUILD).with(hash("1249E25aEbb15358bEdf")).get();
    assertSearchComponent_EmptyResults(response);
  }

  @Test
  void testSearchComponent_PypiCaseInsensitive() throws Exception {
    List<ComponentInfo> pipyAppComponentInfos = new ArrayList<>();
    List<PolicyViolationInfo> appPolicyViolationInfos = new ArrayList<>();
    appPolicyViolationInfos.add(new PolicyViolationInfo("Test Policy", "Found red Label", 4));
    ComponentIdentifier pypiCoordinates = ComponentIdentifier.createPypiCoordinates(
        "PyYAML", "3.11", "WIN32-py3.2", "TAR.gz");
    pipyAppComponentInfos.add(new ComponentInfo("1249e25aebb15358bedd", pypiCoordinates, appPolicyViolationInfos));
    helper.createAppWithScan("search-app-3", Stage.ID_BUILD, pipyAppComponentInfos, "scanId");
    ctx.mockReport("scanId", "/" + getClass().getSimpleName() + "/report");

    String packageUrl = "pkg:pypi/pyyaml@3.11?qualifier=win*&extension=t*";
    HttpResponse response =
        searchRequest(Stage.ID_BUILD).with(hash("1249e25aebb15358bedd")).with(purl(packageUrl)).get();
    ctx.assertResponseStatus(200, response);
    ApiSearchResultsDTOV2 results = response.getBody(ApiSearchResultsDTOV2.class);
    assertThat(results).isNotNull();
    assertThat(results.results).hasSize(1);
    assertSearchResult(results.results.get(0), "search-app-3", "SEARCH-APP-3", "1249e25aebb15358bedd",
        "pkg:pypi/pyyaml@3.11?extension=TAR.gz&qualifier=WIN32-py3.2", pypiCoordinates, 4);
  }

  @Test
  void testSearchComponent_PypiNullExtension() throws Exception {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createPypiCoordinates(
        "PyYAML", "3.11", "WIN32-py3.2", null);
    HttpResponse response =
        searchRequest(Stage.ID_BUILD).with(hash("1249e25aebb15358bedd")).with(coords(componentIdentifier)).get();
    ctx.assertResponseStatus(200, response);
    ApiSearchResultsDTOV2 results = response.getBody(ApiSearchResultsDTOV2.class);
    assertThat(results).isNotNull();
    assertThat(results.results).isEmpty();
    assertThat(results.criteria).isNotNull();
    assertThat(results.criteria.stageId).isEqualTo(Stage.ID_BUILD);
    assertThat(results.criteria.hash).isEqualTo("1249e25aebb15358bedd");
    assertThat(ApiComponentIdentifierDTOV2.toComponentIdentifier(results.criteria.componentIdentifier))
        .isEqualTo(ComponentIdentifier.createPypiCoordinates("PyYAML", "3.11", "WIN32-py3.2", "*"));
  }

  @Test
  void testSearchComponent_AppWithoutAnyReports() throws Exception {
    ctx.tempEntity().newApplicationWithParent("search-app-1");

    HttpResponse response = searchRequest(Stage.ID_BUILD).with(hash("1249E25aEbb15358bEdd")).get();
    assertSearchComponent_EmptyResults(response);
  }

  @Test
  void testSearchComponent_Purl_InvalidPackageUrl() throws Exception {
    String packageUrl = "invalid package url";
    HttpResponse response =
        searchRequest(Stage.ID_BUILD).with(hash("a397f601582e5ccd4b1a")).with(purl(packageUrl)).get();

    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Invalid package url");
  }

  private void assertSearchComponent_EmptyResults(final HttpResponse response) {
    ctx.assertResponseStatus(200, response);
    ApiSearchResultsDTOV2 results = response.getBody(ApiSearchResultsDTOV2.class);
    assertThat(results).isNotNull();
    assertThat(results.results).isEmpty();
  }
}
