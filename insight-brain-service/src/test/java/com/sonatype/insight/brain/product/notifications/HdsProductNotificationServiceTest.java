/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.notifications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sonatype.clm.dto.model.notification.ProductNotification;
import com.sonatype.clm.dto.model.notification.ProductNotificationList;
import com.sonatype.clm.dto.model.notification.ProductNotificationType;
import com.sonatype.insight.brain.dataaccess.notification.UserViewedProductNotificationDAO;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.notification.UserViewedProductNotification;
import com.sonatype.insight.brain.security.InternalRealm;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.test.LogOutput;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

public class HdsProductNotificationServiceTest
    extends AbstractComponentTest
{
  @Rule
  public LogOutput logOutput = new LogOutput(HdsProductNotificationService.class);

  @Inject
  private HdsProductNotificationService hdsNotificationService;

  @Inject
  private UserViewedProductNotificationDAO userViewedProductNotificationDAO;

  @Mock
  private HdsClient mockHdsClient;

  @Before
  public void resetMockHdsClient() {
    reset(mockHdsClient);
  }

  @Test
  public void testGetNotifications() {
    HdsProductNotificationService hdsProductNotificationServiceSpy = spy(hdsNotificationService);

    ProductNotificationList expectedProductNotificationList = createNotifications();
    List<ProductNotification> expectedNotifications = expectedProductNotificationList.getProductNotifications();
    expectedProductNotificationList.setProductNotifications(expectedNotifications);
    when(mockHdsClient.get(eq(ProductNotificationList.class),
        eq(HdsProductNotificationService.HDS_PRODUCT_NOTIFICATION_PATH), anyMap()))
            .thenReturn(expectedProductNotificationList);

    List<ProductNotification> retrievedNotifications = hdsProductNotificationServiceSpy.getNotifications();
    assertNotifications(retrievedNotifications, expectedNotifications);
    verify(mockHdsClient, times(1)).get(eq(ProductNotificationList.class),
        eq(HdsProductNotificationService.HDS_PRODUCT_NOTIFICATION_PATH), anyMap());

    reset(hdsProductNotificationServiceSpy);
    reset(mockHdsClient);
  }

  @Test
  public void testGetNotifications_NotificationsViewedUpdated() {
    HdsProductNotificationService hdsProductNotificationServiceSpy = spy(hdsNotificationService);

    ProductNotificationList expectedProductNotificationList = createNotifications();
    List<ProductNotification> expectedNotifications = expectedProductNotificationList.getProductNotifications();
    when(mockHdsClient.get(eq(ProductNotificationList.class),
        eq(HdsProductNotificationService.HDS_PRODUCT_NOTIFICATION_PATH), anyMap()))
            .thenReturn(expectedProductNotificationList);

    tempEntity.newUserViewedProductNotification(USERNAME, InternalRealm.ID, expectedNotifications.get(0).getId());

    List<ProductNotification> retrievedNotifications = hdsProductNotificationServiceSpy.getNotifications();
    assertNotifications(retrievedNotifications, expectedNotifications);
    verify(mockHdsClient, times(1)).get(eq(ProductNotificationList.class),
        eq(HdsProductNotificationService.HDS_PRODUCT_NOTIFICATION_PATH), anyMap());

    List<UserViewedProductNotification> userViewedProductNotifications =
        userViewedProductNotificationDAO.getByUsernameAndRealmId(USERNAME, InternalRealm.ID);
    assertThat(userViewedProductNotifications).hasSize(1);

    reset(hdsProductNotificationServiceSpy);
    reset(mockHdsClient);

    // Now verify old user viewed product notifications are removed
    expectedProductNotificationList = createNotifications();
    expectedProductNotificationList.getProductNotifications().remove(0);
    expectedNotifications = expectedProductNotificationList.getProductNotifications();
    when(mockHdsClient.get(eq(ProductNotificationList.class),
        eq(HdsProductNotificationService.HDS_PRODUCT_NOTIFICATION_PATH), anyMap()))
            .thenReturn(expectedProductNotificationList);

    retrievedNotifications = hdsProductNotificationServiceSpy.getNotifications();
    assertNotifications(retrievedNotifications, expectedNotifications);
    verify(mockHdsClient, times(1)).get(eq(ProductNotificationList.class),
        eq(HdsProductNotificationService.HDS_PRODUCT_NOTIFICATION_PATH), anyMap());
    // This should have been removed as part of the cache update
    userViewedProductNotifications =
        userViewedProductNotificationDAO.getByUsernameAndRealmId(USERNAME, InternalRealm.ID);
    assertThat(userViewedProductNotifications).isEmpty();
  }

  private ProductNotificationList createNotifications() {
    List<ProductNotification> expectedNotifications = new ArrayList<>();
    long dateCreated = new Date().getTime();
    ProductNotification notification = new ProductNotification();
    notification.setId(UUID.randomUUID().toString());
    notification.setSummaryText("Summary1");
    notification.setSummaryUrl("Summary Url1");
    notification.setDetailHtml("Details1");
    notification.setType(ProductNotificationType.DEFAULT);
    notification.setDateCreated(dateCreated);
    expectedNotifications.add(notification);

    notification = new ProductNotification();
    notification.setId(UUID.randomUUID().toString());
    notification.setSummaryText("Summary2");
    notification.setSummaryUrl("Summary Url2");
    notification.setDetailHtml("Details2");
    notification.setType(ProductNotificationType.DEFAULT);
    notification.setDateCreated(dateCreated);
    expectedNotifications.add(notification);

    ProductNotificationList expectedProductNotificationList = new ProductNotificationList();
    expectedProductNotificationList.setProductNotifications(expectedNotifications);
    return expectedProductNotificationList;
  }

  private void assertNotifications(
      final List<ProductNotification> retrievedNotifications,
      final List<ProductNotification> expectedNotifications)
  {
    assertThat(retrievedNotifications).hasSameSizeAs(expectedNotifications);
    for (int i = 0; i < retrievedNotifications.size(); i++) {
      assertThat(retrievedNotifications.get(i).getId()).isEqualTo(expectedNotifications.get(i).getId());
      assertThat(retrievedNotifications.get(i).getSummaryText())
          .isEqualTo(expectedNotifications.get(i).getSummaryText());
      assertThat(retrievedNotifications.get(i).getSummaryUrl()).isEqualTo(expectedNotifications.get(i).getSummaryUrl());
      assertThat(retrievedNotifications.get(i).getDetailHtml()).isEqualTo(expectedNotifications.get(i).getDetailHtml());
      assertThat(retrievedNotifications.get(i).getType()).isEqualTo(expectedNotifications.get(i).getType());
      assertThat(retrievedNotifications.get(i).getDateCreated())
          .isEqualTo(expectedNotifications.get(i).getDateCreated());
    }
  }
}
