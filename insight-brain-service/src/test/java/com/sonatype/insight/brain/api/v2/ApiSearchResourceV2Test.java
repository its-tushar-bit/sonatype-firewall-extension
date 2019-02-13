/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.List;
import java.util.Map;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.SearchTestHelper.ComponentInfo;
import com.sonatype.insight.brain.api.v2.dto.ApiSearchResultDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiSearchResultsDTOV2;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiSearchResourceV2Test
    extends AbstractResourceTest
{
  private SearchTestHelper helper;

  private Map<String, List<ComponentInfo>> appToComponentMap;

  @Before
  public void init() {
    helper = new SearchTestHelper(tempEntity);
    appToComponentMap = helper.createTestComponentInfoForTwoApps("search-app-1", "search-app-2");
  }

  private HttpRequest searchRequest(String stageId) {
    return restRequest().path(PublicApiPaths.SEARCH_RESOURCE_PATH_V2).query("stageId", stageId);
  }

  private HttpRequest addHash(HttpRequest request, String hash) {
    return request.query("hash", hash);
  }

  private HttpRequest addCoords(HttpRequest request, ComponentIdentifier componentIdentifier) {
    return request.query("componentIdentifier", componentIdentifier);
  }

  private void assertSearchResult(ApiSearchResultDTOV2 result,
                                  String appId,
                                  String appName,
                                  String hash,
                                  ComponentIdentifier componentIdentifier,
                                  Integer threatLevel) throws Exception
  {
    assertThat(result.applicationId).isEqualTo(appId);
    assertThat(result.applicationName).isEqualTo(appName);
    assertThat(result.reportUrl).isNotNull();
    assertResponseStatus(200, HttpRequest.to(result.reportUrl).followRedirects().get());
    assertThat(result.hash).isEqualTo(hash);
    if (componentIdentifier != null) {
      assertThat(result.componentIdentifier).isNotNull();
      assertThat(result.componentIdentifier.getFormat()).isEqualTo(componentIdentifier.getFormat());
      assertThat(result.componentIdentifier.getCoordinates()).isEqualTo(componentIdentifier.getCoordinates());
    }
    else {
      assertThat(result.componentIdentifier).isNull();
    }
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
  public void testSearchComponent_MissingStageId() throws Exception {
    HttpResponse response = addHash(searchRequest(""), "12345678901234567890").get();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Stage has not been specified.");
  }

  @Test
  public void testSearchComponent_InvalidStageId() throws Exception {
    HttpResponse response = addHash(searchRequest("invalid"), "12345678901234567890").get();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Invalid stage: invalid.");
  }

  @Test
  public void testSearchComponent_MissingHashAndCoordinates() throws Exception {
    HttpResponse response = searchRequest(Stage.ID_BUILD).get();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText())
        .isEqualTo("Neither hash nor coordinates of component to search for have been specified.");
  }

  @Test
  public void testSearchComponent_InvalidHash() throws Exception {
    HttpResponse response = addHash(searchRequest(Stage.ID_BUILD), "invalid-hash").get();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Invalid hash: invalid-hash.");
  }

  @Test
  public void testSearchComponent_TooShortHash() throws Exception {
    HttpResponse response = addHash(searchRequest(Stage.ID_BUILD), "1249e25aebb15358bed").get();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Invalid hash: 1249e25aebb15358bed.");
  }

  @Test
  public void testSearchComponent_RestrictedToStage() throws Exception {
    helper.createAppWithScan("search-app-1", Stage.ID_BUILD, appToComponentMap.get("search-app-1"));
    helper.createAppWithScan("search-app-2", Stage.ID_RELEASE, appToComponentMap.get("search-app-2"));

    HttpResponse response = addHash(searchRequest(Stage.ID_BUILD), "1249e25aebb15358bedd").get();
    assertResponseStatus(200, response);
    ApiSearchResultsDTOV2 results = response.getBody(ApiSearchResultsDTOV2.class);
    assertThat(results).isNotNull();
    assertThat(results.results).hasSize(1);
    assertSearchResult(results.results.get(0), "search-app-1", "SEARCH-APP-1", "1249e25aebb15358bedd",
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "", "jar"), 8);
  }

  @Test
  public void testSearchComponent_ByHash() throws Exception {
    helper.createAppWithScan("search-app-1", Stage.ID_BUILD, appToComponentMap.get("search-app-1"));
    helper.createAppWithScan("search-app-2", Stage.ID_BUILD, appToComponentMap.get("search-app-2"));

    HttpResponse response = addHash(searchRequest(Stage.ID_BUILD), "1249e25aebb15358bedd").get();
    assertResponseStatus(200, response);
    ApiSearchResultsDTOV2 results = response.getBody(ApiSearchResultsDTOV2.class);
    assertThat(results).isNotNull();
    assertThat(results.results).hasSize(2);
    assertSearchResult(results.results.get(0), "search-app-1", "SEARCH-APP-1", "1249e25aebb15358bedd",
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "", "jar"), 8);
    assertSearchResult(results.results.get(1), "search-app-2", "SEARCH-APP-2", "1249e25aebb15358bedd",
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "", "jar"), 4);
  }

  @Test
  public void testSearchComponent_ByHash_FullHashString() throws Exception {
    helper.createAppWithScan("search-app-1", Stage.ID_BUILD, appToComponentMap.get("search-app-1"));

    HttpResponse response = addHash(searchRequest(Stage.ID_BUILD), "1249E25aEbb15358bEdd00000000000000000000").get();
    assertResponseStatus(200, response);
    ApiSearchResultsDTOV2 results = response.getBody(ApiSearchResultsDTOV2.class);
    assertThat(results).isNotNull();
    assertThat(results.results).hasSize(1);
    assertSearchResult(results.results.get(0), "search-app-1", "SEARCH-APP-1", "1249e25aebb15358bedd",
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "", "jar"), 8);
  }

  @Test
  public void testSearchComponent_ByHash_UnknownComponent() throws Exception {
    helper.createAppWithScan("search-app-1", Stage.ID_BUILD, appToComponentMap.get("search-app-1"));

    HttpResponse response = addHash(searchRequest(Stage.ID_BUILD), "69b58197caabec2e0d06").get();
    assertResponseStatus(200, response);
    ApiSearchResultsDTOV2 results = response.getBody(ApiSearchResultsDTOV2.class);
    assertThat(results).isNotNull();
    assertThat(results.results).hasSize(1);
    assertSearchResult(results.results.get(0), "search-app-1", "SEARCH-APP-1", "69b58197caabec2e0d06", null, null);
  }

  @Test
  public void testSearchComponent_ByGav_WithEmptyCoordinates() throws Exception {
    helper.createAppWithScan("search-app-1", Stage.ID_BUILD, appToComponentMap.get("search-app-1"));

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("", "tomcat-util", "");
    HttpResponse response = addCoords(searchRequest(Stage.ID_BUILD), componentIdentifier).get();
    assertResponseStatus(200, response);
    ApiSearchResultsDTOV2 results = response.getBody(ApiSearchResultsDTOV2.class);
    assertThat(results).isNotNull();
    assertThat(results.results).hasSize(4);
    sortResultsByAppIdAndHash(results);
    assertSearchResult(results.results.get(0), "search-app-1", "SEARCH-APP-1", "1249e25aebb15358bedd",
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "", "jar"), 8);
    assertSearchResult(results.results.get(1), "search-app-1", "SEARCH-APP-1", "2aa135385b1f449292e8",
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "", "zip"), 8);
    assertSearchResult(results.results.get(2), "search-app-1", "SEARCH-APP-1", "a18da38b875b4658b4e9",
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "sources", "zip"), 8);
    assertSearchResult(results.results.get(3), "search-app-1", "SEARCH-APP-1", "c85713867bef4a3b91c9",
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "sources", "jar"), 8);
  }

  @Test
  public void testSearchComponent_ByGav() throws Exception {
    helper.createAppWithScan("search-app-1", Stage.ID_BUILD, appToComponentMap.get("search-app-1"));
    helper.createAppWithScan("search-app-2", Stage.ID_BUILD, appToComponentMap.get("search-app-2"));

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("tomcat", "*", "*");
    HttpResponse response = addCoords(searchRequest(Stage.ID_BUILD), componentIdentifier).get();
    assertResponseStatus(200, response);
    ApiSearchResultsDTOV2 results = response.getBody(ApiSearchResultsDTOV2.class);
    assertThat(results).isNotNull();
    assertThat(results.results).hasSize(9);
    sortResultsByAppIdAndHash(results);
    assertSearchResult(results.results.get(0), "search-app-1", "SEARCH-APP-1", "1249e25aebb15358bedd",
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "", "jar"), 8);
    assertSearchResult(results.results.get(1), "search-app-1", "SEARCH-APP-1", "2aa135385b1f449292e8",
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "", "zip"), 8);
    assertSearchResult(results.results.get(2), "search-app-1", "SEARCH-APP-1", "a18da38b875b4658b4e9",
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "sources", "zip"), 8);
    assertSearchResult(results.results.get(3), "search-app-1", "SEARCH-APP-1", "a397f601582e5ccd4b1a",
        ComponentIdentifier.createMavenCoordinates("tomcat", "servlets-default", "5.5.4", "", "jar"), null);
    assertSearchResult(results.results.get(4), "search-app-1", "SEARCH-APP-1", "c85713867bef4a3b91c9",
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "sources", "jar"), 8);

    assertSearchResult(results.results.get(5), "search-app-2", "SEARCH-APP-2", "1249e25aebb15358bedd",
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "", "jar"), 4);
    assertSearchResult(results.results.get(6), "search-app-2", "SEARCH-APP-2", "2aa135385b1f449292e8",
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "", "zip"), 4);
    assertSearchResult(results.results.get(7), "search-app-2", "SEARCH-APP-2", "a18da38b875b4658b4e9",
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "sources", "zip"), 4);
    assertSearchResult(results.results.get(8), "search-app-2", "SEARCH-APP-2", "c85713867bef4a3b91c9",
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "sources", "jar"), 4);
  }

  @Test
  public void testSearchComponent_ByGavec_WithNonEmptyClassifier() throws Exception {
    helper.createAppWithScan("search-app-1", Stage.ID_BUILD, appToComponentMap.get("search-app-1"));
    helper.createAppWithScan("search-app-2", Stage.ID_BUILD, appToComponentMap.get("search-app-2"));

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("tomcat", "*", "*", "sources",
        "jar");
    HttpResponse response = addCoords(searchRequest(Stage.ID_BUILD), componentIdentifier).get();
    assertResponseStatus(200, response);
    ApiSearchResultsDTOV2 results = response.getBody(ApiSearchResultsDTOV2.class);
    assertThat(results).isNotNull();
    assertThat(results.results).hasSize(2);
    sortResultsByAppIdAndHash(results);
    assertSearchResult(results.results.get(0), "search-app-1", "SEARCH-APP-1", "c85713867bef4a3b91c9",
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "sources", "jar"), 8);
    assertSearchResult(results.results.get(1), "search-app-2", "SEARCH-APP-2", "c85713867bef4a3b91c9",
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "sources", "jar"), 4);
  }

  @Test
  public void testSearchComponent_ByGavec_WithEmptyClassifier() throws Exception {
    helper.createAppWithScan("search-app-1", Stage.ID_BUILD, appToComponentMap.get("search-app-1"));

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "*",
        "", "jar");
    HttpResponse response = addCoords(searchRequest(Stage.ID_BUILD), componentIdentifier).get();
    assertResponseStatus(200, response);
    ApiSearchResultsDTOV2 results = response.getBody(ApiSearchResultsDTOV2.class);
    assertThat(results).isNotNull();
    assertThat(results.results).hasSize(1);
    sortResultsByAppIdAndHash(results);
    assertSearchResult(results.results.get(0), "search-app-1", "SEARCH-APP-1", "1249e25aebb15358bedd",
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "", "jar"), 8);
  }
  
  @Test
  public void testSearchComponent_ByGave_WithNullClassifier() throws Exception {
    helper.createAppWithScan("search-app-1", Stage.ID_BUILD, appToComponentMap.get("search-app-1"));
    helper.createAppWithScan("search-app-2", Stage.ID_BUILD, appToComponentMap.get("search-app-2"));
    
    ComponentIdentifier componentIdentifier = 
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", null, "jar");
    HttpResponse response = addCoords(searchRequest(Stage.ID_BUILD), componentIdentifier).get();
    assertResponseStatus(200, response);
    ApiSearchResultsDTOV2 results = response.getBody(ApiSearchResultsDTOV2.class);
    assertThat(results).isNotNull();
    assertThat(results.results).hasSize(4);
    sortResultsByAppIdAndHash(results);
    assertSearchResult(results.results.get(0), "search-app-1", "SEARCH-APP-1", "1249e25aebb15358bedd",
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "", "jar"), 8);
    assertSearchResult(results.results.get(1), "search-app-1", "SEARCH-APP-1", "c85713867bef4a3b91c9",
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "sources", "jar"), 8);
    assertSearchResult(results.results.get(2), "search-app-2", "SEARCH-APP-2", "1249e25aebb15358bedd",
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "", "jar"), 4);
    assertSearchResult(results.results.get(3), "search-app-2", "SEARCH-APP-2", "c85713867bef4a3b91c9",
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "sources", "jar"), 4);
  }

  @Test
  public void testSearchComponent_ByNugetComponent() throws Exception {
    helper.createAppWithScan("search-app-1", Stage.ID_BUILD, appToComponentMap.get("search-app-1"));
    helper.createAppWithScan("search-app-2", Stage.ID_BUILD, appToComponentMap.get("search-app-2"));

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createNugetCoordinates("simplejson", "*");
    HttpResponse response = addCoords(searchRequest(Stage.ID_BUILD), componentIdentifier).get();
    assertResponseStatus(200, response);
    ApiSearchResultsDTOV2 results = response.getBody(ApiSearchResultsDTOV2.class);
    assertThat(results).isNotNull();
    assertThat(results.results).isNotNull();
    assertThat(results.results).hasSize(2);
    sortResultsByAppIdAndHash(results);
    assertSearchResult(results.results.get(0), "search-app-1", "SEARCH-APP-1", "2143b68270b82576110f",
        ComponentIdentifier.createNugetCoordinates("simplejson", "0.38.0"), 8);
    assertSearchResult(results.results.get(1), "search-app-2", "SEARCH-APP-2", "2143b68270b82576110f",
        ComponentIdentifier.createNugetCoordinates("simplejson", "0.38.0"), 4);
  }

  @Test
  public void testSearchComponent_ByGavAndHash() throws Exception {
    helper.createAppWithScan("search-app-1", Stage.ID_BUILD, appToComponentMap.get("search-app-1"));
    helper.createAppWithScan("search-app-2", Stage.ID_BUILD, appToComponentMap.get("search-app-2"));

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("tomcat", "*", "*");
    HttpResponse response = addCoords(addHash(searchRequest(Stage.ID_BUILD), "1249e25aebb15358bedd"),
        componentIdentifier).get();
    assertResponseStatus(200, response);
    ApiSearchResultsDTOV2 results = response.getBody(ApiSearchResultsDTOV2.class);
    assertThat(results).isNotNull();
    assertThat(results.results).hasSize(2);
    sortResultsByAppIdAndHash(results);
    assertSearchResult(results.results.get(0), "search-app-1", "SEARCH-APP-1", "1249e25aebb15358bedd",
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "", "jar"), 8);
    assertSearchResult(results.results.get(1), "search-app-2", "SEARCH-APP-2", "1249e25aebb15358bedd",
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "", "jar"), 4);
  }

  @Test
  public void testSearchComponent_ByGavAndHash_NoIntersection() throws Exception {
    helper.createAppWithScan("search-app-1", Stage.ID_BUILD, appToComponentMap.get("search-app-1"));
    helper.createAppWithScan("search-app-2", Stage.ID_BUILD, appToComponentMap.get("search-app-2"));

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("*", "tomcat-util", "*");
    HttpResponse response = addCoords(addHash(searchRequest(Stage.ID_BUILD), "a397f601582e5ccd4b1a"),
        componentIdentifier).get();
    assertResponseStatus(200, response);
    ApiSearchResultsDTOV2 results = response.getBody(ApiSearchResultsDTOV2.class);
    assertThat(results).isNotNull();
    assertThat(results.results).isEmpty();
  }

  @Test
  public void testSearchComponent_EchoCriteria() throws Exception {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("gid", "aid", "1");
    HttpResponse response = addCoords(addHash(searchRequest(Stage.ID_BUILD), "1249e25aebb15358bedd"),
        componentIdentifier).get();
    assertResponseStatus(200, response);
    ApiSearchResultsDTOV2 results = response.getBody(ApiSearchResultsDTOV2.class);
    assertThat(results).isNotNull();
    assertThat(results.results).isEmpty();
    assertThat(results.criteria).isNotNull();
    assertThat(results.criteria.stageId).isEqualTo(Stage.ID_BUILD);
    assertThat(results.criteria.hash).isEqualTo("1249e25aebb15358bedd");
    assertThat(results.criteria.componentIdentifier.getFormat()).isEqualTo(componentIdentifier.getFormat());
    assertThat(results.criteria.componentIdentifier.getCoordinates())
        .isEqualTo(ComponentIdentifier.createMavenCoordinates("gid", "aid", "1", "*", "*").getCoordinates());
  }

  @Test
  public void testSearchComponent_NoHitsAmongAppComponents() throws Exception {
    helper.createAppWithScan("search-app-1", Stage.ID_BUILD, appToComponentMap.get("search-app-1"));

    HttpResponse response = addHash(searchRequest(Stage.ID_BUILD), "1249E25aEbb15358bEdf").get();
    assertResponseStatus(200, response);
    ApiSearchResultsDTOV2 results = response.getBody(ApiSearchResultsDTOV2.class);
    assertThat(results).isNotNull();
    assertThat(results.results).isEmpty();
  }

  @Test
  public void testSearchComponent_AppWithoutAnyReports() throws Exception {
    tempEntity.newApplicationWithParent("search-app-1");

    HttpResponse response = addHash(searchRequest(Stage.ID_BUILD), "1249E25aEbb15358bEdd").get();
    assertResponseStatus(200, response);
    ApiSearchResultsDTOV2 results = response.getBody(ApiSearchResultsDTOV2.class);
    assertThat(results).isNotNull();
    assertThat(results.results).isEmpty();
  }
}
