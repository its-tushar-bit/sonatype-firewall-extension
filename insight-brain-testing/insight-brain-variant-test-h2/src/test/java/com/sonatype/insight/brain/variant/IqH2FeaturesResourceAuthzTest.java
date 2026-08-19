/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.features.FeaturesResource;
import com.sonatype.insight.brain.model.security.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * H2 port of {@code FeaturesResourceAuthzTest}, reproducing the {@code AbstractResourceAuthzTest} fixture
 * (anonymous {@code restRequest()}, authorized user, and the {@code testAuthcGet} helper) that the legacy test
 * relied on.
 */
@IqH2Test
class IqH2FeaturesResourceAuthzTest
{
  private IqTestContext ctx;

  private User authorized;

  @BeforeEach
  void createEntities() {
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
  void testGetLicenses() throws Exception {
    testAuthcGet(restRequest().path(FeaturesResource.RESOURCE_PATH));
  }
}
