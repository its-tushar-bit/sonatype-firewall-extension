/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.ApiThirdPartyScanResource;
import com.sonatype.insight.brain.model.security.User;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqPostgresTest
class IqPostgresApiThirdPartyScanResourceAuthzTest
{
  // Injected by IqPostgresServerExtension: the extension owns the shared, reused server.
  private IqTestContext ctx;

  private HttpRequest restRequest() {
    return ctx.restRequest();
  }

  @Test
  void testGetIdeUsersOverview_Requires_Authentication() throws Exception {
    String endpointPath = PublicApiPaths.THIRD_PARTY_SCAN_PATH + '/' + ApiThirdPartyScanResource.IDE_USER_OVERVIEW;
    testAuthcGet(restRequest().path(endpointPath));
  }

  // Sometimes, simply being able to log in, is all the authorization you need...
  private HttpResponse testAuthcGet(HttpRequest request) throws Exception {
    User authorized = ctx.tempEntity().newUser();

    HttpResponse response = request.anon().get();
    assertThat(response.getStatusCode()).isEqualTo(401);

    response = request.auth(authorized).get();
    assertThat(response.getStatusCode()).isGreaterThanOrEqualTo(200).isLessThan(400);
    return response;
  }
}
