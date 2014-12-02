/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v1;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v1.dto.ApiSearchResultsDTO;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;
import com.sonatype.insight.test.RestAccess;

import com.ning.http.client.Response;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class ApiSearchResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  private SearchTestHelper helper;

  @Before
  public void init() {
    helper = new SearchTestHelper(tempEntity, getCLMServer());
  }

  @Test
  public void testSearchComponent() throws Exception {
    helper.createScanForApp(app.getId(), Stage.ID_BUILD, "search-app-1");

    grantReadPermission(app.getId());

    String url = getRestUrl(PublicApiPaths.SEARCH_SERVICE_PATH) + "?stageId=" + Stage.ID_BUILD + "&hash="
        + "1249e25aebb15358bedd";
    Response response = RestAccess.get(url, unauthorized.getUsername(), unauthorized.getPassword());
    assertResponseStatus(200, response);
    ApiSearchResultsDTO results = fromJson(response, ApiSearchResultsDTO.class);
    assertThat(results, is(notNullValue()));
    assertThat(results.results, is(empty()));

    response = RestAccess.get(url, authorized.getUsername(), authorized.getPassword());
    assertResponseStatus(200, response);
    results = fromJson(response, ApiSearchResultsDTO.class);
    assertThat(results, is(notNullValue()));
    assertThat(results.results, hasSize(1));
    assertThat(results.results.get(0).applicationId, is(app.getPublicId()));
  }
}
