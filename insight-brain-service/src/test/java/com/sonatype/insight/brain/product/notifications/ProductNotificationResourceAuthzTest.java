/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.notifications;

import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import java.util.UUID;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;

import org.junit.Test;

@Category(SlowTest.class)
public class ProductNotificationResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(ProductNotificationResource.RESOURCE_PATH);
  }

  @Test
  public void testGetNotifications() throws Exception {
    testAuthcGet(restRequest());
  }

  @Test
  public void testSetNotificationViewed() throws Exception {
    ProductNotificationDTO notificationDTO = new ProductNotificationDTO();
    notificationDTO.id = UUID.randomUUID().toString();
    testAuthcPost(restRequest().path(ProductNotificationResource.VIEWED_PATH).body(notificationDTO));
  }
}
