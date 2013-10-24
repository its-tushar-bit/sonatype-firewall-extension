/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.search.SearchResource.SearchResults;
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

public class SearchResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  private TestHelper helper;

  @Before
  public void init() {
    helper = new TestHelper(tempEntity, brain);
  }

  @Test
  public void testSearchComponent() throws Exception {
    helper.createScanForApp(app.getId(), Stage.ID_BUILD, "search-app-1");

    Role role = tempEntity.newRole(false, Permission.READ);
    tempEntity.newMembershipMapping(app.getId(), role.getId(), authorized.getUsername());

    String url = getRestUrl(SearchResource.SERVICE_PATH) + "?stageId=" + Stage.ID_BUILD + "&hash="
        + "1249e25aebb15358bedd";
    Response response = RestAccess.get(url, unauthorized.getUsername(), unauthorized.getPassword());
    assertResponseStatus(200, response);
    SearchResults results = fromJson(response, SearchResults.class);
    assertThat(results, is(notNullValue()));
    assertThat(results.results, is(empty()));

    response = RestAccess.get(url, authorized.getUsername(), authorized.getPassword());
    assertResponseStatus(200, response);
    results = fromJson(response, SearchResults.class);
    assertThat(results, is(notNullValue()));
    assertThat(results.results, hasSize(1));
    assertThat(results.results.get(0).applicationId, is(app.getPublicId()));
  }
}
