/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.hds.EnvironmentResource;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kept in the {@code com.sonatype.insight.brain.variant} package; reproduces the {@code AbstractResourceAuthzTest}
 * fixture (org/app/repo + authorized/unauthorized users) and its {@code testAuthcGet} helper that the legacy
 * {@code EnvironmentResourceAuthzTest} inherited from its base class.
 */
@IqH2Test
class IqH2EnvironmentResourceAuthzTest
{
  private static final String QUERY_PARAMS = "p=eclipse&version=2.0.1.qualifier";

  private IqTestContext ctx;

  private User unauthorized;

  private User authorized;

  @BeforeEach
  void createEntities() {
    Organization org = ctx.tempEntity().newOrganization();
    ctx.tempEntity().newApplication(org.getId());
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    ctx.tempEntity().newRepository(repositoryManager, "test");
    unauthorized = ctx.tempEntity().newUser();
    authorized = ctx.tempEntity().newUser();
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().anon();
  }

  private void assertStatus(HttpResponse response, Integer status) {
    if (status == null) {
      assertThat(response.getStatusCode()).isGreaterThanOrEqualTo(200).isLessThan(400);
    }
    else {
      assertThat(response.getStatusCode()).isEqualTo(status);
    }
  }

  // Sometimes, simply being able to log in, is all the authorization you need...
  private HttpResponse testAuthcGet(HttpRequest request) throws Exception {
    HttpResponse response = request.anon().get();
    assertStatus(response, 401);

    response = request.auth(authorized).get();
    assertStatus(response, null);
    return response;
  }

  @Test
  void testSubmitClientEnvironment() throws Exception {
    ctx.hdsRespondWith("").atUri("session/environment?" + QUERY_PARAMS);
    testAuthcGet(restRequest().path(EnvironmentResource.RESOURCE_PATH).query(QUERY_PARAMS));
  }
}
