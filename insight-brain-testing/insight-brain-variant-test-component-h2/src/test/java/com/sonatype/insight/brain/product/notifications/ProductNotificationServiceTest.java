/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.notifications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.sonatype.clm.dto.model.notification.ProductNotification;
import com.sonatype.clm.dto.model.notification.ProductNotificationType;
import com.sonatype.insight.brain.dataaccess.notification.UserViewedProductNotificationDAO;
import com.sonatype.insight.brain.model.notification.UserViewedProductNotification;
import com.sonatype.insight.brain.security.InternalRealm;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

@ComponentH2Test
public class ProductNotificationServiceTest
    extends AbstractComponentH2Test
{
  @Inject
  private UserViewedProductNotificationDAO userViewedProductNotificationDAO;

  @Inject
  private ProductNotificationService notificationsService;

  @Mock
  private HdsProductNotificationService hdsNotificationService;

  @Test
  public void testGetNotifications_PageSizeGreaterThanNumItems() {
    List<ProductNotification> notifications = createNotification(1);
    when(hdsNotificationService.getNotifications()).thenReturn(notifications);

    int pageSize = 2;
    ProductNotificationListDTO notificationListDTO = notificationsService.getNotifications(pageSize, 1);
    assertThat(notificationListDTO).isNotNull();
    assertThat(notificationListDTO.notifications).hasSize(1);
    assertNotification(notificationListDTO.notifications.get(0), notifications.get(0), false);

    notificationListDTO = notificationsService.getNotifications(pageSize, 2);
    assertThat(notificationListDTO).isNotNull();
    assertThat(notificationListDTO.notifications).isEmpty();
  }

  @Test
  public void testGetNotifications_Pagination() {
    List<ProductNotification> notifications = createNotification(5);
    when(hdsNotificationService.getNotifications()).thenReturn(notifications);

    int pageSize = 2;
    for (int page = 1; page <= 2; page++) {
      ProductNotificationListDTO notificationListDTO = notificationsService.getNotifications(pageSize, page);
      assertThat(notificationListDTO).isNotNull();
      assertThat(notificationListDTO.notifications).hasSize(2);
      assertNotification(notificationListDTO.notifications.get(0), notifications.get((page * pageSize) - 2), false);
      assertNotification(notificationListDTO.notifications.get(1), notifications.get((page * pageSize) - 1), false);
    }

    ProductNotificationListDTO notificationListDTO = notificationsService.getNotifications(pageSize, 3);
    assertThat(notificationListDTO).isNotNull();
    assertThat(notificationListDTO.notifications).hasSize(1);
    assertNotification(notificationListDTO.notifications.get(0), notifications.get(notifications.size() - 1), false);
  }

  @Test
  public void testGetNotifications_SomeViewed() {
    List<ProductNotification> notifications = new ArrayList<>();
    List<String> viewedIds = new ArrayList<>();
    int numNotifications = 10;
    for (int i = 0; i < numNotifications; i++) {
      ProductNotification notification = new ProductNotification();
      notification.setId(UUID.randomUUID().toString());
      notifications.add(notification);
      if (i % 2 == 0) {
        viewedIds.add(notification.getId());
        tempEntity.newUserViewedProductNotification(USERNAME, InternalRealm.ID, notification.getId());
      }
    }
    when(hdsNotificationService.getNotifications()).thenReturn(notifications);

    int page = 1;
    ProductNotificationListDTO notificationListDTO = notificationsService.getNotifications(numNotifications, page);
    assertThat(notificationListDTO).isNotNull();
    assertThat(notificationListDTO.notifications).hasSize(numNotifications);
    for (int i = 0; i < numNotifications; i++) {
      ProductNotificationDTO notificationDTO = notificationListDTO.notifications.get(i);
      assertNotification(notificationDTO, notifications.get(i), viewedIds.contains(notificationDTO.id));
    }
  }

  @Test
  public void testSetNotificationViewed() {
    List<ProductNotification> notifications = createNotification(1);
    when(hdsNotificationService.getNotifications()).thenReturn(notifications);

    ProductNotificationListDTO notificationListDTO = notificationsService.convert(notifications,
        Collections.emptySet());
    ProductNotificationDTO returnedValue = notificationsService.setNotificationViewed(notificationListDTO.notifications
        .get(0));
    assertNotification(returnedValue, notifications.get(0), true);

    int pageSize = 2;
    ProductNotificationListDTO returnNotificationListDTO = notificationsService.getNotifications(pageSize, 1);
    assertThat(returnNotificationListDTO).isNotNull();
    assertThat(returnNotificationListDTO.notifications).hasSize(1);
    assertNotification(returnNotificationListDTO.notifications.get(0), notifications.get(0), true);
  }

  @Test
  public void testGetNotifications_LegacyViewedNotificationExists() {
    List<ProductNotification> notifications = createNotification(1);
    when(hdsNotificationService.getNotifications()).thenReturn(notifications);

    tempEntity.newUserViewedProductNotificationLegacy(USERNAME, notifications.get(0).getId());

    int page = 1;
    ProductNotificationListDTO notificationListDTO = notificationsService.getNotifications(1, page);
    assertThat(notificationListDTO).isNotNull();
    assertThat(notificationListDTO.notifications).hasSize(1);
    ProductNotificationDTO notificationDTO = notificationListDTO.notifications.get(0);
    assertNotification(notificationDTO, notifications.get(0), true);
  }

  @Test
  public void testSetNotificationViewed_LegacyViewedNotificationExists() {
    List<ProductNotification> notifications = createNotification(1);
    ProductNotificationListDTO notificationListDTO =
        notificationsService.convert(notifications, Collections.emptySet());

    UserViewedProductNotification userViewedProductNotificationLegacy =
        tempEntity.newUserViewedProductNotificationLegacy(USERNAME, notifications.get(0).getId());
    ProductNotificationDTO returnedValue =
        notificationsService.setNotificationViewed(notificationListDTO.notifications.get(0));
    assertNotification(returnedValue, notifications.get(0), true);

    assertThat(userViewedProductNotificationDAO.getById(userViewedProductNotificationLegacy.getId())).isNull();
    assertThat(userViewedProductNotificationDAO.getByUsernameAndRealmIdAndNotificationId(USERNAME,
        InternalRealm.ID, notifications.get(0).getId())).isNotNull();
  }

  @Test
  public void testSetNotificationViewed_SameNotificationTwice() {
    List<ProductNotification> notifications = createNotification(1);

    ProductNotificationListDTO notificationListDTO = notificationsService.convert(notifications,
        Collections.emptySet());
    ProductNotificationDTO returnedValue = notificationsService.setNotificationViewed(notificationListDTO.notifications
        .get(0));
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

  private void assertNotification(
      final ProductNotificationDTO notificationDTO,
      final ProductNotification notification,
      final boolean viewed)
  {
    assertThat(notificationDTO.id).isEqualTo(notification.getId());
    assertThat(notificationDTO.summaryText).isEqualTo(notification.getSummaryText());
    assertThat(notificationDTO.summaryUrl).isEqualTo(notification.getSummaryUrl());
    assertThat(notificationDTO.detailHtml).isEqualTo(notification.getDetailHtml());
    assertThat(notificationDTO.type).isEqualTo(notification.getType());
    assertThat(notificationDTO.dateCreated).isEqualTo(notification.getDateCreated());
    assertThat(notificationDTO.viewed).isEqualTo(viewed);
  }
}
