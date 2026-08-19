/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.sonatype.clm.dto.model.notification.ProductNotification;
import com.sonatype.clm.dto.model.notification.ProductNotificationList;
import com.sonatype.clm.dto.model.notification.ProductNotificationType;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.product.notifications.HdsProductNotificationService;
import com.sonatype.insight.brain.product.notifications.ProductNotificationDTO;
import com.sonatype.insight.brain.product.notifications.ProductNotificationListDTO;
import com.sonatype.insight.brain.product.notifications.ProductNotificationResource;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2ProductNotificationResourceTest
{
  private IqTestContext ctx;

  private HttpRequest restRequest() {
    return ctx.restRequest().path(ProductNotificationResource.RESOURCE_PATH);
  }

  private HttpRequest listRequest(int pageSize, int pageIndex) {
    return restRequest().query("pageSize", pageSize).query("page", pageIndex);
  }

  @Test
  void testGetNotifications() throws Exception {
    List<ProductNotification> notifications = createNotifications(2);
    ctx.hdsRespondWith(new ProductNotificationList(notifications))
        .atUri(HdsProductNotificationService.HDS_PRODUCT_NOTIFICATION_PATH);

    // Get first page of notifications
    int pageSize = 10;
    HttpResponse response = listRequest(pageSize, 1).get();
    ctx.assertResponseStatus(200, response);
    ProductNotificationListDTO notificationListDTO = response.getBody(ProductNotificationListDTO.class);
    assertThat(notificationListDTO).isNotNull();
    assertThat(notificationListDTO.notifications).hasSize(2);
    assertNotification(notificationListDTO.notifications.get(0), notifications.get(0), false);
    assertNotification(notificationListDTO.notifications.get(1), notifications.get(1), false);

    // Get second page, should be empty
    response = listRequest(pageSize, 2).get();
    ctx.assertResponseStatus(200, response);
    notificationListDTO = response.getBody(ProductNotificationListDTO.class);
    assertThat(notificationListDTO).isNotNull();
    assertThat(notificationListDTO.notifications).isEmpty();
  }

  @Test
  void testSetNotificationViewed() throws Exception {
    List<ProductNotification> notifications = createNotifications(1);
    ctx.hdsRespondWith(new ProductNotificationList(notifications))
        .atUri(HdsProductNotificationService.HDS_PRODUCT_NOTIFICATION_PATH);

    int pageSize = 10;
    HttpResponse response = listRequest(pageSize, 1).get();
    ctx.assertResponseStatus(200, response);
    ProductNotificationListDTO notificationListDTO = response.getBody(ProductNotificationListDTO.class);
    assertThat(notificationListDTO).isNotNull();
    assertThat(notificationListDTO.notifications).hasSize(1);
    assertNotification(notificationListDTO.notifications.get(0), notifications.get(0), false);

    // Now set the notification as viewed
    ProductNotificationDTO notificationDTO = new ProductNotificationDTO();
    notificationDTO.id = notifications.get(0).getId();
    response = restRequest().path(ProductNotificationResource.VIEWED_PATH).body(notificationDTO).post();
    ctx.assertResponseStatus(200, response);

    // test the returned value has viewed flag set
    ProductNotificationDTO returnedValue = response.getBody(ProductNotificationDTO.class);
    assertThat(returnedValue).isNotNull();
    assertThat(returnedValue.id).isEqualTo(notificationDTO.id);
    assertThat(returnedValue.viewed).isTrue();

    // Get the notifications again
    response = listRequest(pageSize, 1).get();
    ctx.assertResponseStatus(200, response);
    notificationListDTO = response.getBody(ProductNotificationListDTO.class);
    assertThat(notificationListDTO).isNotNull();
    assertThat(notificationListDTO.notifications).hasSize(1);
    assertNotification(notificationListDTO.notifications.get(0), notifications.get(0), true);
  }

  private List<ProductNotification> createNotifications(final int numNotifications) {
    long now = new Date().getTime();
    List<ProductNotification> notifications = new ArrayList<>();
    for (int i = 1; i <= numNotifications; i++) {
      ProductNotification notification = new ProductNotification();
      notification.setId(UUID.randomUUID().toString());
      notification.setSummaryText("Test Summary-" + i);
      notification.setSummaryUrl("Test Summary Url-" + i);
      notification.setDetailHtml("Test Details-" + i);
      notification.setType(ProductNotificationType.DEFAULT);
      notification.setDateCreated(now - i);
      notifications.add(notification);
    }
    return notifications;
  }

  private void assertNotification(
      final ProductNotificationDTO notificationDTO,
      final ProductNotification notification,
      final boolean viewed)
  {
    assertThat(notificationDTO).usingRecursiveComparison().ignoringFields("viewed").isEqualTo(notification);
    assertThat(notificationDTO.viewed).isEqualTo(viewed);
  }
}
