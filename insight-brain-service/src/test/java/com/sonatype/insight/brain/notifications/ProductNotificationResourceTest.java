/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.notifications;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.sonatype.clm.dto.model.notification.ProductNotification;
import com.sonatype.clm.dto.model.notification.ProductNotificationType;
import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.notifications.dto.ProductNotificationDTO;
import com.sonatype.insight.brain.notifications.dto.ProductNotificationListDTO;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import com.ning.http.client.Response;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public class ProductNotificationResourceTest
    extends AbstractResourceTest
{
  @Before
  public void resetMock() {
    reset(mockHdsProductNotificationService);
  }

  @Test
  public void testGetNotifications() throws Exception {
    List<ProductNotification> notifications = createNotifications(2);
    when(mockHdsProductNotificationService.getNotifications()).thenReturn(notifications);

    // Get first page of notifications
    int pageSize = 10;
    Response response = AuthedRestAccess.get(getServiceURL(pageSize, 1));
    assertResponseStatus(200, response);
    ProductNotificationListDTO notificationListDTO = fromJson(response, ProductNotificationListDTO.class);
    assertThat(notificationListDTO, not(nullValue()));
    assertThat(notificationListDTO.notifications.size(), is(2));
    assertNotification(notificationListDTO.notifications.get(0), notifications.get(0), false);
    assertNotification(notificationListDTO.notifications.get(1), notifications.get(1), false);

    // Get second page, should be empty
    response = AuthedRestAccess.get(getServiceURL(pageSize, 2));
    assertResponseStatus(200, response);
    notificationListDTO = fromJson(response, ProductNotificationListDTO.class);
    assertThat(notificationListDTO, not(nullValue()));
    assertThat(notificationListDTO.notifications.size(), is(0));
  }

  @Test
  public void testPostNotificationViewed() throws Exception {
    List<ProductNotification> notifications = createNotifications(1);
    when(mockHdsProductNotificationService.getNotifications()).thenReturn(notifications);

    int pageSize = 10;
    Response response = AuthedRestAccess.get(getServiceURL(pageSize, 1));
    assertResponseStatus(200, response);
    ProductNotificationListDTO notificationListDTO = fromJson(response, ProductNotificationListDTO.class);
    assertThat(notificationListDTO, not(nullValue()));
    assertThat(notificationListDTO.notifications.size(), is(1));
    // test that the notification is what is expected with viewed = false
    assertNotification(notificationListDTO.notifications.get(0), notifications.get(0), false);

    // Now set the notification as viewed
    String url = getRestUrl(ProductNotificationResource.SERVICE_PATH + "/" + ProductNotificationResource.VIEWED_PATH);
    ProductNotificationDTO notificationDTO = new ProductNotificationDTO();
    notificationDTO.id = notifications.get(0).getId();
    response = AuthedRestAccess.post(url, toJson(notificationDTO));
    assertResponseStatus(200, response);

    // test the returned value has viewed flag set
    ProductNotificationDTO returnedValue = fromJson(response, ProductNotificationDTO.class);
    assertThat(returnedValue, notNullValue());
    assertThat(returnedValue.id, is(notificationDTO.id));
    assertThat(returnedValue.viewed, is(true));

    // Get the notifications again
    response = AuthedRestAccess.get(getServiceURL(pageSize, 1));
    assertResponseStatus(200, response);
    notificationListDTO = fromJson(response, ProductNotificationListDTO.class);
    assertThat(notificationListDTO, not(nullValue()));
    assertThat(notificationListDTO.notifications.size(), is(1));
    // test that the notification is what is expected with viewed = true
    assertNotification(notificationListDTO.notifications.get(0), notifications.get(0), true);
  }

  private String getServiceURL(final int pageSize, final int page) {
    return getRestBaseUrl() + ProductNotificationResource.SERVICE_PATH + "?pageSize=" + pageSize + "&page=" + page;
  }

  private List<ProductNotification> createNotifications(final int numNotifications) {
    List<ProductNotification> notifications = new ArrayList<>();
    for (int i = 1; i <= numNotifications; i++) {
      ProductNotification notification = new ProductNotification();
      notification.setId(UUID.randomUUID().toString());
      notification.setSummaryText("Test Summary-" + i);
      notification.setSummaryUrl("Test Summary Url-" + i);
      notification.setDetailHtml("Test Details-" + i);
      notification.setType(ProductNotificationType.DEFAULT);
      notification.setDateCreated(new Date().getTime());
      notifications.add(notification);
    }
    return notifications;
  }

  private void assertNotification(final ProductNotificationDTO notificationDTO, final ProductNotification notification,
      final boolean viewed)
  {
    assertThat(notificationDTO.id, is(notification.getId()));
    assertThat(notificationDTO.summaryText, is(notification.getSummaryText()));
    assertThat(notificationDTO.summaryUrl, is(notification.getSummaryUrl()));
    assertThat(notificationDTO.detailHtml, is(notification.getDetailHtml()));
    assertThat(notificationDTO.type, is(notification.getType()));
    assertThat(notificationDTO.dateCreated, is(notification.getDateCreated()));
    assertThat(notificationDTO.viewed, is(viewed));
  }
}
