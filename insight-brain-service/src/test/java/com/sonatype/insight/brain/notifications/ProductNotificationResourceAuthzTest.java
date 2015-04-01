/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.notifications;

import java.util.UUID;

import com.sonatype.insight.brain.notifications.dto.ProductNotificationDTO;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;

import org.junit.Test;

public class ProductNotificationResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @Test
  public void testGetNotifications() throws Exception {
    String url = getRestUrl(ProductNotificationResource.SERVICE_PATH);
    testAuthcGet(url);
  }

  @Test
  public void testPostNotificationsViewed() throws Exception {
    String url = getRestUrl(ProductNotificationResource.SERVICE_PATH + "/" + ProductNotificationResource.VIEWED_PATH);
    ProductNotificationDTO notificationDTO = new ProductNotificationDTO();
    notificationDTO.id = UUID.randomUUID().toString();
    testAuthcPost(url, toJson(notificationDTO));
  }
}
