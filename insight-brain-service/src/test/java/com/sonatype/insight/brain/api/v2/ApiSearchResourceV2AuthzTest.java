/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.SearchTestHelper.ComponentInfo;
import com.sonatype.insight.brain.api.v2.dto.ApiSearchResultsDTOV2;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class ApiSearchResourceV2AuthzTest
    extends AbstractResourceAuthzTest
{
  private SearchTestHelper helper;

  @Before
  public void init() {
    helper = new SearchTestHelper(tempEntity);
  }

  @Test
  public void testSearchComponent() throws Exception {
    List<ComponentInfo> app1ComponentInfos = new ArrayList<>();
    app1ComponentInfos.add(new ComponentInfo("1249e25aebb15358bedd", ComponentIdentifier.createMavenCoordinates(
        "tomcat", "tomcat-util", "5.5.23", "", "jar"), null));
    helper.createScanForApp(app.getId(), Stage.ID_BUILD, app1ComponentInfos);

    grantReadPermission(app.getId());

    HttpRequest request = restRequest().path(PublicApiPaths.SEARCH_RESOURCE_PATH_V2).query("stageId", Stage.ID_BUILD)
        .query("hash", "1249e25aebb15358bedd");
    HttpResponse response = request.auth(unauthorized.getUsername(), unauthorized.getPassword()).get();
    assertResponseStatus(200, response);
    ApiSearchResultsDTOV2 results = response.getBody(ApiSearchResultsDTOV2.class);
    assertThat(results, is(notNullValue()));
    assertThat(results.results, is(empty()));

    response = request.auth(authorized.getUsername(), authorized.getPassword()).get();
    assertResponseStatus(200, response);
    results = response.getBody(ApiSearchResultsDTOV2.class);
    assertThat(results, is(notNullValue()));
    assertThat(results.results, hasSize(1));
    assertThat(results.results.get(0).applicationId, is(app.getPublicId()));
  }
}
