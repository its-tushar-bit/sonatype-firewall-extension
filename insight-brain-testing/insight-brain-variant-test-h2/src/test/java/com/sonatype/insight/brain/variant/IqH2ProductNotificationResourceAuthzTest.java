/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.util.UUID;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.product.notifications.ProductNotificationDTO;
import com.sonatype.insight.brain.product.notifications.ProductNotificationResource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kept in the {@code com.sonatype.insight.brain.variant} package; reproduces the {@code AbstractResourceAuthzTest}
 * fixture (authorized user) and its {@code testAuthcGet}/{@code testAuthcPost} helpers that the legacy
 * {@code ProductNotificationResourceAuthzTest} inherited from its base class.
 */
@IqH2Test
class IqH2ProductNotificationResourceAuthzTest
{
  private IqTestContext ctx;

  private User authorized;

  @BeforeEach
  void createEntities() {
    authorized = ctx.tempEntity().newUser();
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().anon().path(ProductNotificationResource.RESOURCE_PATH);
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

  private HttpResponse testAuthcPost(HttpRequest request) throws Exception {
    HttpResponse response = request.anon().post();
    assertStatus(response, 401);

    response = request.auth(authorized).post();
    assertStatus(response, null);
    return response;
  }

  @Test
  void testGetNotifications() throws Exception {
    testAuthcGet(restRequest());
  }

  @Test
  void testSetNotificationViewed() throws Exception {
    ProductNotificationDTO notificationDTO = new ProductNotificationDTO();
    notificationDTO.id = UUID.randomUUID().toString();
    testAuthcPost(restRequest().path(ProductNotificationResource.VIEWED_PATH).body(notificationDTO));
  }
}
