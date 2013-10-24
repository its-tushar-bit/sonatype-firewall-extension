/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.TemporaryEntity;
import com.sonatype.insight.brain.search.SearchResource.SearchResult;
import com.sonatype.insight.brain.search.SearchResource.SearchResults;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import com.ning.http.client.Response;
import com.yammer.dropwizard.testing.JsonHelpers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class SearchResourceTest
    extends AbstractResourceTest
{
  private TestHelper helper;

  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  @Before
  public void init() {
    helper = new TestHelper(tempEntity, brain);
  }

  private String getSearchUrl(String stageId, String hash) {
    return getRestBaseUrl() + SearchResource.SERVICE_PATH + "?stageId=" + stageId + "&hash=" + hash;
  }

  private String getSearchUrl(String stageId, String groupId, String artifactId, String version) {
    return getRestBaseUrl() + SearchResource.SERVICE_PATH + "?stageId=" + stageId + "&groupId=" + groupId
        + "&artifactId=" + artifactId + "&version=" + version;
  }

  private String getSearchUrl(String stageId, String hash, String groupId, String artifactId, String version) {
    return getRestBaseUrl() + SearchResource.SERVICE_PATH + "?stageId=" + stageId + "&hash=" + hash + "&groupId="
        + groupId + "&artifactId=" + artifactId + "&version=" + version;
  }

  private void assertSearchResult(SearchResult result, String appId, String appName, String hash, String groupId,
      String artifactId, String version, Integer threatLevel) throws Exception
  {
    assertThat(result.applicationId, is(appId));
    assertThat(result.applicationName, is(appName));
    assertThat(result.reportUrl, is(notNullValue()));
    assertResponseStatus(200, AuthedRestAccess.get(result.reportUrl));
    assertThat(result.hash, is(hash));
    assertThat(result.groupId, is(groupId));
    assertThat(result.artifactId, is(artifactId));
    assertThat(result.version, is(version));
    assertThat(result.threatLevel, is(threatLevel));
  }

  @Test
  public void testSearchComponent_MissingStageId() throws Exception {
    Response response = AuthedRestAccess.get(getSearchUrl("", "12345678901234567890"));
    assertResponseStatus(400, response);
    assertThat(response.getResponseBody(), is("Stage has not been specified"));
  }

  @Test
  public void testSearchComponent_InvalidStageId() throws Exception {
    Response response = AuthedRestAccess.get(getSearchUrl("invalid", "12345678901234567890"));
    assertResponseStatus(400, response);
    assertThat(response.getResponseBody(), is("Invalid stage: invalid"));
  }

  @Test
  public void testSearchComponent_MissingHashAndGav() throws Exception {
    Response response = AuthedRestAccess.get(getSearchUrl("build", ""));
    assertResponseStatus(400, response);
    assertThat(response.getResponseBody(),
        is("Neither hash nor coordinates of component to search for have been specified"));
  }

  @Test
  public void testSearchComponent_InvalidHash() throws Exception {
    Response response = AuthedRestAccess.get(getSearchUrl("build", "invalid-hash"));
    assertResponseStatus(400, response);
    assertThat(response.getResponseBody(), is("Invalid hash: invalid-hash"));
  }

  @Test
  public void testSearchComponent_TooShortHash() throws Exception {
    Response response = AuthedRestAccess.get(getSearchUrl("build", "1249e25aebb15358bed"));
    assertResponseStatus(400, response);
    assertThat(response.getResponseBody(), is("Invalid hash: 1249e25aebb15358bed"));
  }

  @Test
  public void testSearchComponent_RestrictedToStage() throws Exception {
    helper.createAppWithScan("search-app-1", Stage.ID_BUILD);
    helper.createAppWithScan("search-app-2", Stage.ID_RELEASE);

    Response response = AuthedRestAccess.get(getSearchUrl(Stage.ID_BUILD, "1249e25aebb15358bedd"));
    assertResponseStatus(200, response);
    SearchResults results = JsonHelpers.fromJson(response.getResponseBody(), SearchResults.class);
    assertThat(results, is(notNullValue()));
    assertThat(results.results, is(notNullValue()));
    assertThat(results.results, hasSize(1));
    assertSearchResult(results.results.get(0), "search-app-1", "SEARCH-APP-1", "1249e25aebb15358bedd", "tomcat",
        "tomcat-util", "5.5.23", 8);
  }

  @Test
  public void testSearchComponent_ByHash() throws Exception {
    helper.createAppWithScan("search-app-1", Stage.ID_BUILD);
    helper.createAppWithScan("search-app-2", Stage.ID_BUILD);

    Response response = AuthedRestAccess.get(getSearchUrl(Stage.ID_BUILD, "1249e25aebb15358bedd"));
    assertResponseStatus(200, response);
    SearchResults results = JsonHelpers.fromJson(response.getResponseBody(), SearchResults.class);
    assertThat(results, is(notNullValue()));
    assertThat(results.results, is(notNullValue()));
    assertThat(results.results, hasSize(2));
    assertSearchResult(results.results.get(0), "search-app-1", "SEARCH-APP-1", "1249e25aebb15358bedd", "tomcat",
        "tomcat-util", "5.5.23", 8);
    assertSearchResult(results.results.get(1), "search-app-2", "SEARCH-APP-2", "1249e25aebb15358bedd", "tomcat",
        "tomcat-util", "5.5.23", 4);
  }

  @Test
  public void testSearchComponent_ByHash_FullHashString() throws Exception {
    helper.createAppWithScan("search-app-1", Stage.ID_BUILD);

    Response response = AuthedRestAccess.get(getSearchUrl(Stage.ID_BUILD, "1249E25aEbb15358bEdd00000000000000000000"));
    assertResponseStatus(200, response);
    SearchResults results = JsonHelpers.fromJson(response.getResponseBody(), SearchResults.class);
    assertThat(results, is(notNullValue()));
    assertThat(results.results, is(notNullValue()));
    assertThat(results.results, hasSize(1));
    assertSearchResult(results.results.get(0), "search-app-1", "SEARCH-APP-1", "1249e25aebb15358bedd", "tomcat",
        "tomcat-util", "5.5.23", 8);
  }

  @Test
  public void testSearchComponent_ByGav() throws Exception {
    helper.createAppWithScan("search-app-1", Stage.ID_BUILD);
    helper.createAppWithScan("search-app-2", Stage.ID_BUILD);

    Response response = AuthedRestAccess.get(getSearchUrl(Stage.ID_BUILD, "tomcat", "*", "*"));
    assertResponseStatus(200, response);
    SearchResults results = JsonHelpers.fromJson(response.getResponseBody(), SearchResults.class);
    assertThat(results, is(notNullValue()));
    assertThat(results.results, is(notNullValue()));
    assertThat(results.results, hasSize(3));
    assertSearchResult(results.results.get(0), "search-app-1", "SEARCH-APP-1", "1249e25aebb15358bedd", "tomcat",
        "tomcat-util", "5.5.23", 8);
    assertSearchResult(results.results.get(1), "search-app-1", "SEARCH-APP-1", "a397f601582e5ccd4b1a", "tomcat",
        "servlets-default", "5.5.4", null);
    assertSearchResult(results.results.get(2), "search-app-2", "SEARCH-APP-2", "1249e25aebb15358bedd", "tomcat",
        "tomcat-util", "5.5.23", 4);
  }

  @Test
  public void testSearchComponent_ByGavAndHash() throws Exception {
    helper.createAppWithScan("search-app-1", Stage.ID_BUILD);
    helper.createAppWithScan("search-app-2", Stage.ID_BUILD);

    Response response = AuthedRestAccess.get(getSearchUrl(Stage.ID_BUILD, "1249e25aebb15358bedd", "tomcat", "*", "*"));
    assertResponseStatus(200, response);
    SearchResults results = JsonHelpers.fromJson(response.getResponseBody(), SearchResults.class);
    assertThat(results, is(notNullValue()));
    assertThat(results.results, is(notNullValue()));
    assertThat(results.results, hasSize(2));
    assertSearchResult(results.results.get(0), "search-app-1", "SEARCH-APP-1", "1249e25aebb15358bedd", "tomcat",
        "tomcat-util", "5.5.23", 8);
    assertSearchResult(results.results.get(1), "search-app-2", "SEARCH-APP-2", "1249e25aebb15358bedd", "tomcat",
        "tomcat-util", "5.5.23", 4);
  }

  @Test
  public void testSearchComponent_ByGavAndHash_NoIntersection() throws Exception {
    helper.createAppWithScan("search-app-1", Stage.ID_BUILD);
    helper.createAppWithScan("search-app-2", Stage.ID_BUILD);

    Response response = AuthedRestAccess.get(getSearchUrl(Stage.ID_BUILD, "a397f601582e5ccd4b1a", "*", "tomcat-util",
        "*"));
    assertResponseStatus(200, response);
    SearchResults results = JsonHelpers.fromJson(response.getResponseBody(), SearchResults.class);
    assertThat(results, is(notNullValue()));
    assertThat(results.results, is(notNullValue()));
    assertThat(results.results, hasSize(0));
  }

  @Test
  public void testSearchComponent_EchoCriteria() throws Exception {
    Response response = AuthedRestAccess.get(getSearchUrl(Stage.ID_BUILD, "1249e25aebb15358bedd", "gid", "aid", "1"));
    assertResponseStatus(200, response);
    SearchResults results = JsonHelpers.fromJson(response.getResponseBody(), SearchResults.class);
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
    helper.createAppWithScan("search-app-1", Stage.ID_BUILD);

    Response response = AuthedRestAccess.get(getSearchUrl(Stage.ID_BUILD, "1249E25aEbb15358bEdf"));
    assertResponseStatus(200, response);
    SearchResults results = JsonHelpers.fromJson(response.getResponseBody(), SearchResults.class);
    assertThat(results, is(notNullValue()));
    assertThat(results.results, is(notNullValue()));
    assertThat(results.results, hasSize(0));
  }

  @Test
  public void testSearchComponent_AppWithoutAnyReports() throws Exception {
    createApplication("search-app-1");

    Response response = AuthedRestAccess.get(getSearchUrl(Stage.ID_BUILD, "1249E25aEbb15358bEdd"));
    assertResponseStatus(200, response);
    SearchResults results = JsonHelpers.fromJson(response.getResponseBody(), SearchResults.class);
    assertThat(results, is(notNullValue()));
    assertThat(results.results, is(notNullValue()));
    assertThat(results.results, hasSize(0));
  }
}
