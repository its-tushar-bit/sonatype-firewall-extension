/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v1;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v1.SearchTestHelper.ComponentInfo;
import com.sonatype.insight.brain.api.v1.dto.ApiSearchResultDTO;
import com.sonatype.insight.brain.api.v1.dto.ApiSearchResultsDTO;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * @deprecated since 1.13.0, because the tested class is deprecated
 */
@Deprecated
public class ApiSearchResourceTest
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
    return restRequest().path(PublicApiPaths.SEARCH_RESOURCE_PATH).query("stageId", stageId);
  }

  private HttpRequest addHash(HttpRequest request, String hash) {
    return request.query("hash", hash);
  }

  private HttpRequest addCoords(HttpRequest request, String groupId, String artifactId, String version) {
    return request.query("groupId", groupId).query("artifactId", artifactId).query("version", version);
  }

  private void assertSearchResult(ApiSearchResultDTO result, String appId, String appName, String hash, String groupId,
      String artifactId, String version, Integer threatLevel) throws Exception
  {
    assertThat(result.applicationId, is(appId));
    assertThat(result.applicationName, is(appName));
    assertThat(result.reportUrl, is(notNullValue()));
    assertResponseStatus(200, HttpRequest.to(result.reportUrl).followRedirects().get());
    assertThat(result.hash, is(hash));
    assertThat(result.groupId, is(groupId));
    assertThat(result.artifactId, is(artifactId));
    assertThat(result.version, is(version));
    assertThat(result.threatLevel, is(threatLevel));
  }

  private void sortResultsByAppIdAndHash(ApiSearchResultsDTO resultsDTO) {
    Collections.sort(resultsDTO.results, new Comparator<ApiSearchResultDTO>() {
      @Override
      public int compare(final ApiSearchResultDTO o1, final ApiSearchResultDTO o2) {
        String applicationIdHash1 = o1.applicationId + o1.hash;
        String applicationIdHash2 = o2.applicationId + o2.hash;
        return applicationIdHash1.compareTo(applicationIdHash2);
      }
    });
  }

  @Test
  public void testSearchComponent_MissingStageId() throws Exception {
    HttpResponse response = addHash(searchRequest(""), "12345678901234567890").get();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText(), is("Stage has not been specified."));
  }

  @Test
  public void testSearchComponent_InvalidStageId() throws Exception {
    HttpResponse response = addHash(searchRequest("invalid"), "12345678901234567890").get();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText(), is("Invalid stage: invalid."));
  }

  @Test
  public void testSearchComponent_MissingHashAndCoordinates() throws Exception {
    HttpResponse response = searchRequest(Stage.ID_BUILD).get();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText(),
        is("Neither hash nor coordinates of component to search for have been specified."));
  }

  @Test
  public void testSearchComponent_InvalidHash() throws Exception {
    HttpResponse response = addHash(searchRequest(Stage.ID_BUILD), "invalid-hash").get();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText(), is("Invalid hash: invalid-hash."));
  }

  @Test
  public void testSearchComponent_TooShortHash() throws Exception {
    HttpResponse response = addHash(searchRequest(Stage.ID_BUILD), "1249e25aebb15358bed").get();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText(), is("Invalid hash: 1249e25aebb15358bed."));
  }

  @Test
  public void testSearchComponent_RestrictedToStage() throws Exception {
    helper.createAppWithScan("search-app-1", Stage.ID_BUILD, appToComponentMap.get("search-app-1"));
    helper.createAppWithScan("search-app-2", Stage.ID_RELEASE, appToComponentMap.get("search-app-2"));

    HttpResponse response = addHash(searchRequest(Stage.ID_BUILD), "1249e25aebb15358bedd").get();
    assertResponseStatus(200, response);
    ApiSearchResultsDTO results = response.getBody(ApiSearchResultsDTO.class);
    assertThat(results, is(notNullValue()));
    assertThat(results.results, is(notNullValue()));
    assertThat(results.results, hasSize(1));
    assertSearchResult(results.results.get(0), "search-app-1", "SEARCH-APP-1", "1249e25aebb15358bedd", "tomcat",
        "tomcat-util", "5.5.23", 8);
  }

  @Test
  public void testSearchComponent_ByHash() throws Exception {
    helper.createAppWithScan("search-app-1", Stage.ID_BUILD, appToComponentMap.get("search-app-1"));
    helper.createAppWithScan("search-app-2", Stage.ID_BUILD, appToComponentMap.get("search-app-2"));

    HttpResponse response = addHash(searchRequest(Stage.ID_BUILD), "1249e25aebb15358bedd").get();
    assertResponseStatus(200, response);
    ApiSearchResultsDTO results = response.getBody(ApiSearchResultsDTO.class);
    assertThat(results, is(notNullValue()));
    assertThat(results.results, is(notNullValue()));
    assertThat(results.results, hasSize(2));
    sortResultsByAppIdAndHash(results);
    assertSearchResult(results.results.get(0), "search-app-1", "SEARCH-APP-1", "1249e25aebb15358bedd", "tomcat",
        "tomcat-util", "5.5.23", 8);
    assertSearchResult(results.results.get(1), "search-app-2", "SEARCH-APP-2", "1249e25aebb15358bedd", "tomcat",
        "tomcat-util", "5.5.23", 4);
  }

  @Test
  public void testSearchComponent_ByHash_FullHashString() throws Exception {
    helper.createAppWithScan("search-app-1", Stage.ID_BUILD, appToComponentMap.get("search-app-1"));

    HttpResponse response = addHash(searchRequest(Stage.ID_BUILD), "1249E25aEbb15358bEdd00000000000000000000").get();
    assertResponseStatus(200, response);
    ApiSearchResultsDTO results = response.getBody(ApiSearchResultsDTO.class);
    assertThat(results, is(notNullValue()));
    assertThat(results.results, is(notNullValue()));
    assertThat(results.results, hasSize(1));
    assertSearchResult(results.results.get(0), "search-app-1", "SEARCH-APP-1", "1249e25aebb15358bedd", "tomcat",
        "tomcat-util", "5.5.23", 8);
  }

  @Test
  public void testSearchComponent_ByGav() throws Exception {
    helper.createAppWithScan("search-app-1", Stage.ID_BUILD, appToComponentMap.get("search-app-1"));
    helper.createAppWithScan("search-app-2", Stage.ID_BUILD, appToComponentMap.get("search-app-2"));

    HttpResponse response = addCoords(searchRequest(Stage.ID_BUILD), "tomcat", "*", "*").get();
    assertResponseStatus(200, response);
    ApiSearchResultsDTO results = response.getBody(ApiSearchResultsDTO.class);
    assertThat(results, is(notNullValue()));
    assertThat(results.results, is(notNullValue()));
    assertThat(results.results, hasSize(9));
    sortResultsByAppIdAndHash(results);
    assertSearchResult(results.results.get(0), "search-app-1", "SEARCH-APP-1", "1249e25aebb15358bedd", "tomcat",
        "tomcat-util", "5.5.23", 8); // jar
    assertSearchResult(results.results.get(1), "search-app-1", "SEARCH-APP-1", "2aa135385b1f449292e8",
        "tomcat", "tomcat-util", "5.5.23", 8); // zip
    assertSearchResult(results.results.get(2), "search-app-1", "SEARCH-APP-1", "a18da38b875b4658b4e9",
        "tomcat", "tomcat-util", "5.5.23", 8); // sources, zip
    assertSearchResult(results.results.get(3), "search-app-1", "SEARCH-APP-1", "a397f601582e5ccd4b1a", "tomcat",
        "servlets-default", "5.5.4", null); // jar
    assertSearchResult(results.results.get(4), "search-app-1", "SEARCH-APP-1", "c85713867bef4a3b91c9",
        "tomcat", "tomcat-util", "5.5.23", 8); // sources, jar

    assertSearchResult(results.results.get(5), "search-app-2", "SEARCH-APP-2", "1249e25aebb15358bedd", "tomcat",
        "tomcat-util", "5.5.23", 4); // jar
    assertSearchResult(results.results.get(6), "search-app-2", "SEARCH-APP-2", "2aa135385b1f449292e8", "tomcat",
        "tomcat-util", "5.5.23", 4); // zip
    assertSearchResult(results.results.get(7), "search-app-2", "SEARCH-APP-2", "a18da38b875b4658b4e9", "tomcat",
        "tomcat-util", "5.5.23", 4); // sources, zip
    assertSearchResult(results.results.get(8), "search-app-2", "SEARCH-APP-2", "c85713867bef4a3b91c9", "tomcat",
        "tomcat-util", "5.5.23", 4); // sources, jar
  }

  @Test
  public void testSearchComponent_ByGavAndHash() throws Exception {
    helper.createAppWithScan("search-app-1", Stage.ID_BUILD, appToComponentMap.get("search-app-1"));
    helper.createAppWithScan("search-app-2", Stage.ID_BUILD, appToComponentMap.get("search-app-2"));

    HttpResponse response = addCoords(addHash(searchRequest(Stage.ID_BUILD), "1249e25aebb15358bedd"), "tomcat", "*", "*")
        .get();
    assertResponseStatus(200, response);
    ApiSearchResultsDTO results = response.getBody(ApiSearchResultsDTO.class);
    assertThat(results, is(notNullValue()));
    assertThat(results.results, is(notNullValue()));
    assertThat(results.results, hasSize(2));
    sortResultsByAppIdAndHash(results);
    assertSearchResult(results.results.get(0), "search-app-1", "SEARCH-APP-1", "1249e25aebb15358bedd", "tomcat",
        "tomcat-util", "5.5.23", 8);
    assertSearchResult(results.results.get(1), "search-app-2", "SEARCH-APP-2", "1249e25aebb15358bedd", "tomcat",
        "tomcat-util", "5.5.23", 4);
  }

  @Test
  public void testSearchComponent_ByGavAndHash_NoIntersection() throws Exception {
    helper.createAppWithScan("search-app-1", Stage.ID_BUILD, appToComponentMap.get("search-app-1"));
    helper.createAppWithScan("search-app-2", Stage.ID_BUILD, appToComponentMap.get("search-app-2"));

    HttpResponse response = addCoords(addHash(searchRequest(Stage.ID_BUILD), "a397f601582e5ccd4b1a"), "*", "tomcat-util",
        "*").get();
    assertResponseStatus(200, response);
    ApiSearchResultsDTO results = response.getBody(ApiSearchResultsDTO.class);
    assertThat(results, is(notNullValue()));
    assertThat(results.results, is(notNullValue()));
    assertThat(results.results, hasSize(0));
  }

  @Test
  public void testSearchComponent_EchoCriteria() throws Exception {
    HttpResponse response = addCoords(addHash(searchRequest(Stage.ID_BUILD), "1249e25aebb15358bedd"), "gid", "aid", "1")
        .get();
    assertResponseStatus(200, response);
    ApiSearchResultsDTO results = response.getBody(ApiSearchResultsDTO.class);
    assertThat(results, is(notNullValue()));
    assertThat(results.results, is(notNullValue()));
    assertThat(results.results, hasSize(0));
    assertThat(results.criteria, is(notNullValue()));
    assertThat(results.criteria.stageId, is(Stage.ID_BUILD));
    assertThat(results.criteria.hash, is("1249e25aebb15358bedd"));
    assertThat(results.criteria.groupId, is("gid"));
    assertThat(results.criteria.artifactId, is("aid"));
    assertThat(results.criteria.version, is("1"));
  }

  @Test
  public void testSearchComponent_NoHitsAmongAppComponents() throws Exception {
    helper.createAppWithScan("search-app-1", Stage.ID_BUILD, appToComponentMap.get("search-app-1"));

    HttpResponse response = addHash(searchRequest(Stage.ID_BUILD), "1249E25aEbb15358bEdf").get();
    assertResponseStatus(200, response);
    ApiSearchResultsDTO results = response.getBody(ApiSearchResultsDTO.class);
    assertThat(results, is(notNullValue()));
    assertThat(results.results, is(notNullValue()));
    assertThat(results.results, hasSize(0));
  }

  @Test
  public void testSearchComponent_AppWithoutAnyReports() throws Exception {
    tempEntity.newApplicationWithParent("search-app-1");

    HttpResponse response = addHash(searchRequest(Stage.ID_BUILD), "1249E25aEbb15358bEdd").get();
    assertResponseStatus(200, response);
    ApiSearchResultsDTO results = response.getBody(ApiSearchResultsDTO.class);
    assertThat(results, is(notNullValue()));
    assertThat(results.results, is(notNullValue()));
    assertThat(results.results, hasSize(0));
  }
}
