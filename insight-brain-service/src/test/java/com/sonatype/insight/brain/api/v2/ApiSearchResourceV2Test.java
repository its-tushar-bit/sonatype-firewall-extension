/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v1.SearchTestHelper;
import com.sonatype.insight.brain.api.v2.dto.ApiSearchResultDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiSearchResultsDTOV2;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class ApiSearchResourceV2Test
    extends AbstractResourceTest
{
  private SearchTestHelper helper;

  @Before
  public void init() {
    helper = new SearchTestHelper(tempEntity, getCLMServer());
  }

  private String getSearchUrl(String stageId, String hash) {
    return getRestBaseUrl() + PublicApiPaths.SEARCH_SERVICE_PATH_V2 + "?stageId=" + stageId + "&hash=" + hash;
  }

  private String getSearchUrl(String stageId, ComponentIdentifier componentIdentifier)
      throws UnsupportedEncodingException
  {
    return getRestBaseUrl() + PublicApiPaths.SEARCH_SERVICE_PATH_V2 + "?stageId=" + stageId +
        "&componentIdentifier=" + toQueryParam(componentIdentifier);
  }

  private String getSearchUrl(String stageId, String hash, ComponentIdentifier componentIdentifier)
      throws UnsupportedEncodingException
  {
    return getRestBaseUrl() + PublicApiPaths.SEARCH_SERVICE_PATH_V2 + "?stageId=" + stageId + "&hash=" + hash +
        "&componentIdentifier=" + toQueryParam(componentIdentifier);
  }

  private String toQueryParam(ComponentIdentifier componentIdentifier) throws UnsupportedEncodingException {
    return URLEncoder.encode(toJson(componentIdentifier), "UTF-8");
  }

  private void assertSearchResult(ApiSearchResultDTOV2 result, String appId, String appName, String hash,
      ComponentIdentifier componentIdentifier, Integer threatLevel) throws Exception
  {
    assertThat(result.applicationId, is(appId));
    assertThat(result.applicationName, is(appName));
    assertThat(result.reportUrl, is(notNullValue()));
    AsyncHttpClient.BoundRequestBuilder builder = AuthedRestAccess.getClient().prepareGet(result.reportUrl);
    builder.setFollowRedirects(true);
    assertResponseStatus(200, AuthedRestAccess.execute(builder));
    assertThat(result.hash, is(hash));
    assertThat(result.componentIdentifier.getFormat(), is(componentIdentifier.getFormat()));
    assertThat(result.componentIdentifier.getCoordinates(), is(componentIdentifier.getCoordinates()));
    assertThat(result.threatLevel, is(threatLevel));
  }

  @Test
  public void testSearchComponent_MissingStageId() throws Exception {
    Response response = AuthedRestAccess.get(getSearchUrl("", "12345678901234567890"));
    assertResponseStatus(400, response);
    assertThat(response.getResponseBody(), is("Stage has not been specified."));
  }

  @Test
  public void testSearchComponent_InvalidStageId() throws Exception {
    Response response = AuthedRestAccess.get(getSearchUrl("invalid", "12345678901234567890"));
    assertResponseStatus(400, response);
    assertThat(response.getResponseBody(), is("Invalid stage: invalid."));
  }

  @Test
  public void testSearchComponent_MissingHashAndGav() throws Exception {
    Response response = AuthedRestAccess.get(getSearchUrl("build", ""));
    assertResponseStatus(400, response);
    assertThat(response.getResponseBody(),
        is("Neither hash nor coordinates of component to search for have been specified."));
  }

  @Test
  public void testSearchComponent_InvalidHash() throws Exception {
    Response response = AuthedRestAccess.get(getSearchUrl("build", "invalid-hash"));
    assertResponseStatus(400, response);
    assertThat(response.getResponseBody(), is("Invalid hash: invalid-hash."));
  }

  @Test
  public void testSearchComponent_TooShortHash() throws Exception {
    Response response = AuthedRestAccess.get(getSearchUrl("build", "1249e25aebb15358bed"));
    assertResponseStatus(400, response);
    assertThat(response.getResponseBody(), is("Invalid hash: 1249e25aebb15358bed."));
  }

  @Test
  public void testSearchComponent_RestrictedToStage() throws Exception {
    helper.createAppWithScan("search-app-1", Stage.ID_BUILD);
    helper.createAppWithScan("search-app-2", Stage.ID_RELEASE);

    Response response = AuthedRestAccess.get(getSearchUrl(Stage.ID_BUILD, "1249e25aebb15358bedd"));
    assertResponseStatus(200, response);
    ApiSearchResultsDTOV2 results = fromJson(response, ApiSearchResultsDTOV2.class);
    assertThat(results, is(notNullValue()));
    assertThat(results.results, is(notNullValue()));
    assertThat(results.results, hasSize(1));
    assertSearchResult(results.results.get(0), "search-app-1", "SEARCH-APP-1", "1249e25aebb15358bedd",
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "", "jar"), 8);
  }

  @Test
  public void testSearchComponent_ByHash() throws Exception {
    helper.createAppWithScan("search-app-1", Stage.ID_BUILD);
    helper.createAppWithScan("search-app-2", Stage.ID_BUILD);

    Response response = AuthedRestAccess.get(getSearchUrl(Stage.ID_BUILD, "1249e25aebb15358bedd"));
    assertResponseStatus(200, response);
    ApiSearchResultsDTOV2 results = fromJson(response, ApiSearchResultsDTOV2.class);
    assertThat(results, is(notNullValue()));
    assertThat(results.results, is(notNullValue()));
    assertThat(results.results, hasSize(2));
    assertSearchResult(results.results.get(0), "search-app-1", "SEARCH-APP-1", "1249e25aebb15358bedd",
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "", "jar"), 8);
    assertSearchResult(results.results.get(1), "search-app-2", "SEARCH-APP-2", "1249e25aebb15358bedd",
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "", "jar"), 4);
  }

  @Test
  public void testSearchComponent_ByHash_FullHashString() throws Exception {
    helper.createAppWithScan("search-app-1", Stage.ID_BUILD);

    Response response = AuthedRestAccess.get(getSearchUrl(Stage.ID_BUILD, "1249E25aEbb15358bEdd00000000000000000000"));
    assertResponseStatus(200, response);
    ApiSearchResultsDTOV2 results = fromJson(response, ApiSearchResultsDTOV2.class);
    assertThat(results, is(notNullValue()));
    assertThat(results.results, is(notNullValue()));
    assertThat(results.results, hasSize(1));
    assertSearchResult(results.results.get(0), "search-app-1", "SEARCH-APP-1", "1249e25aebb15358bedd",
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "", "jar"), 8);
  }

  @Test
  public void testSearchComponent_ByGav() throws Exception {
    helper.createAppWithScan("search-app-1", Stage.ID_BUILD);
    helper.createAppWithScan("search-app-2", Stage.ID_BUILD);

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("tomcat", "*", "*");
    Response response = AuthedRestAccess.get(getSearchUrl(Stage.ID_BUILD, componentIdentifier));
    assertResponseStatus(200, response);
    ApiSearchResultsDTOV2 results = fromJson(response, ApiSearchResultsDTOV2.class);
    assertThat(results, is(notNullValue()));
    assertThat(results.results, is(notNullValue()));
    assertThat(results.results, hasSize(3));
    assertSearchResult(results.results.get(0), "search-app-1", "SEARCH-APP-1", "1249e25aebb15358bedd",
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "", "jar"), 8);
    assertSearchResult(results.results.get(1), "search-app-1", "SEARCH-APP-1", "a397f601582e5ccd4b1a",
        ComponentIdentifier.createMavenCoordinates("tomcat", "servlets-default", "5.5.4", "", "jar"), null);
    assertSearchResult(results.results.get(2), "search-app-2", "SEARCH-APP-2", "1249e25aebb15358bedd",
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "", "jar"), 4);
  }

  @Test
  public void testSearchComponent_ByGavAndHash() throws Exception {
    helper.createAppWithScan("search-app-1", Stage.ID_BUILD);
    helper.createAppWithScan("search-app-2", Stage.ID_BUILD);

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("tomcat", "*", "*");
    Response response = AuthedRestAccess.get(getSearchUrl(Stage.ID_BUILD, "1249e25aebb15358bedd", componentIdentifier));
    assertResponseStatus(200, response);
    ApiSearchResultsDTOV2 results = fromJson(response, ApiSearchResultsDTOV2.class);
    assertThat(results, is(notNullValue()));
    assertThat(results.results, is(notNullValue()));
    assertThat(results.results, hasSize(2));
    assertSearchResult(results.results.get(0), "search-app-1", "SEARCH-APP-1", "1249e25aebb15358bedd",
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "", "jar"), 8);
    assertSearchResult(results.results.get(1), "search-app-2", "SEARCH-APP-2", "1249e25aebb15358bedd",
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "", "jar"), 4);
  }

  @Test
  public void testSearchComponent_ByGavAndHash_NoIntersection() throws Exception {
    helper.createAppWithScan("search-app-1", Stage.ID_BUILD);
    helper.createAppWithScan("search-app-2", Stage.ID_BUILD);

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("*", "tomcat-util", "*");
    Response response = AuthedRestAccess.get(getSearchUrl(Stage.ID_BUILD, "a397f601582e5ccd4b1a", componentIdentifier));
    assertResponseStatus(200, response);
    ApiSearchResultsDTOV2 results = fromJson(response, ApiSearchResultsDTOV2.class);
    assertThat(results, is(notNullValue()));
    assertThat(results.results, is(notNullValue()));
    assertThat(results.results, hasSize(0));
  }

  @Test
  public void testSearchComponent_EchoCriteria() throws Exception {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("gid", "aid", "1");
    Response response = AuthedRestAccess.get(getSearchUrl(Stage.ID_BUILD, "1249e25aebb15358bedd", componentIdentifier));
    assertResponseStatus(200, response);
    ApiSearchResultsDTOV2 results = fromJson(response, ApiSearchResultsDTOV2.class);
    assertThat(results, is(notNullValue()));
    assertThat(results.results, is(notNullValue()));
    assertThat(results.results, hasSize(0));
    assertThat(results.criteria, is(notNullValue()));
    assertThat(results.criteria.stageId, is(Stage.ID_BUILD));
    assertThat(results.criteria.hash, is("1249e25aebb15358bedd"));
    assertThat(results.criteria.componentIdentifier.getFormat(), is(componentIdentifier.getFormat()));
    assertThat(results.criteria.componentIdentifier.getCoordinates(), is(componentIdentifier.getCoordinates()));
  }

  @Test
  public void testSearchComponent_NoHitsAmongAppComponents() throws Exception {
    helper.createAppWithScan("search-app-1", Stage.ID_BUILD);

    Response response = AuthedRestAccess.get(getSearchUrl(Stage.ID_BUILD, "1249E25aEbb15358bEdf"));
    assertResponseStatus(200, response);
    ApiSearchResultsDTOV2 results = fromJson(response, ApiSearchResultsDTOV2.class);
    assertThat(results, is(notNullValue()));
    assertThat(results.results, is(notNullValue()));
    assertThat(results.results, hasSize(0));
  }

  @Test
  public void testSearchComponent_AppWithoutAnyReports() throws Exception {
    tempEntity.newApplicationWithParent("search-app-1");

    Response response = AuthedRestAccess.get(getSearchUrl(Stage.ID_BUILD, "1249E25aEbb15358bEdd"));
    assertResponseStatus(200, response);
    ApiSearchResultsDTOV2 results = fromJson(response, ApiSearchResultsDTOV2.class);
    assertThat(results, is(notNullValue()));
    assertThat(results.results, is(notNullValue()));
    assertThat(results.results, hasSize(0));
  }
}
