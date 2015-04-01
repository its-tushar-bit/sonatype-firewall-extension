/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.notifications;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.notification.ProductNotification;
import com.sonatype.clm.dto.model.notification.ProductNotificationType;
import com.sonatype.insight.brain.notifications.dto.ProductNotificationDTO;
import com.sonatype.insight.brain.notifications.dto.ProductNotificationListDTO;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import com.google.inject.Binder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ProductNotificationServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ProductNotificationService notificationsService;

  @Mock
  private SaasProductNotificationService saasNotificationService;

  @Override
  public void configure(Binder binder) {
    super.configure(binder);
    binder.bind(SaasProductNotificationService.class).toInstance(saasNotificationService);
  }

  @Test
  public void testGetNotification_PageSizeGreaterThanNumItems() {
    List<ProductNotification> notifications = createNotification(1);
    when(saasNotificationService.getNotifications()).thenReturn(notifications);

    int pageSize = 2;
    ProductNotificationListDTO notificationListDTO = notificationsService.getNotifications(pageSize, 1);
    assertThat(notificationListDTO, not(nullValue()));
    assertThat(notificationListDTO.notifications.size(), is(1));
    assertNotification(notificationListDTO.notifications.get(0), notifications.get(0), false);

    notificationListDTO = notificationsService.getNotifications(pageSize, 2);
    assertThat(notificationListDTO, not(nullValue()));
    assertThat(notificationListDTO.notifications.size(), is(0));
  }

  @Test
  public void testGetNotification_Pagination() {
    List<ProductNotification> notifications = createNotification(5);
    when(saasNotificationService.getNotifications()).thenReturn(notifications);

    int pageSize = 2;
    for (int page = 1; page <= 2; page++) {
      ProductNotificationListDTO notificationListDTO = notificationsService.getNotifications(pageSize, page);
      assertThat(notificationListDTO, not(nullValue()));
      assertThat(notificationListDTO.notifications.size(), is(2));
      assertNotification(notificationListDTO.notifications.get(0), notifications.get((page * pageSize) - 2), false);
      assertNotification(notificationListDTO.notifications.get(1), notifications.get((page * pageSize) - 1), false);
    }

    ProductNotificationListDTO notificationListDTO = notificationsService.getNotifications(pageSize, 3);
    assertThat(notificationListDTO, not(nullValue()));
    assertThat(notificationListDTO.notifications.size(), is(1));
    assertNotification(notificationListDTO.notifications.get(0), notifications.get(notifications.size() - 1), false);
  }

  @Test
  public void testGetNotification_SomeViewed() {
    List<ProductNotification> notifications = new ArrayList<>();
    List<String> viewedIds = new ArrayList<>();
    int numNotifications = 10;
    for (int i = 0; i < numNotifications; i++) {
      ProductNotification notification = new ProductNotification();
      notification.setId(UUID.randomUUID().toString());
      notifications.add(notification);
      if (i % 2 == 0) {
        viewedIds.add(notification.getId());
        tempEntity.newUserViewedNotificationMapping(USERNAME, notification.getId());
      }
    }
    when(saasNotificationService.getNotifications()).thenReturn(notifications);

    int page = 1;
    ProductNotificationListDTO notificationListDTO = notificationsService.getNotifications(numNotifications, page);
    assertThat(notificationListDTO, not(nullValue()));
    assertThat(notificationListDTO.notifications.size(), is(numNotifications));
    for (int i = 0; i < numNotifications; i++) {
      ProductNotificationDTO notificationDTO = notificationListDTO.notifications.get(i);
      assertNotification(notificationDTO, notifications.get(i), viewedIds.contains(notificationDTO.id));
    }
  }

  @Test
  public void testSetNotificationViewed() {
    List<ProductNotification> notifications = createNotification(1);
    when(saasNotificationService.getNotifications()).thenReturn(notifications);

    ProductNotificationListDTO notificationListDTO = notificationsService.convert(notifications,
        Collections.<String>emptySet());
    ProductNotificationDTO returnedValue =
        notificationsService.setNotificationViewed(notificationListDTO.notifications.get(0));
    assertNotification(returnedValue, notifications.get(0), true);

    int pageSize = 2;
    ProductNotificationListDTO returnNotificationListDTO = notificationsService.getNotifications(pageSize, 1);
    assertThat(returnNotificationListDTO, not(nullValue()));
    assertThat(returnNotificationListDTO.notifications.size(), is(1));
    assertNotification(returnNotificationListDTO.notifications.get(0), notifications.get(0), true);
  }

  @Test
  public void testSetNotificationViewed_SameNotificationTwice() {
    List<ProductNotification> notifications = createNotification(1);
    when(saasNotificationService.getNotifications()).thenReturn(notifications);

    ProductNotificationListDTO notificationListDTO = notificationsService.convert(notifications,
        Collections.<String>emptySet());
    ProductNotificationDTO returnedValue =
        notificationsService.setNotificationViewed(notificationListDTO.notifications.get(0));
    assertNotification(returnedValue, notifications.get(0), true);

    returnedValue = notificationsService.setNotificationViewed(notificationListDTO.notifications.get(0));
    assertNotification(returnedValue, notifications.get(0), true);
  }

  private List<ProductNotification> createNotification(final int numberOfNotification) {
    List<ProductNotification> notifications = new ArrayList<>();
    for (int i = 0; i < numberOfNotification; i++) {
      ProductNotification notification = new ProductNotification();
      notification.setId(UUID.randomUUID().toString());
      notification.setSummaryText("Test Summary");
      notification.setSummaryUrl("Test Url");
      notification.setDetailHtml("Test Details");
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
