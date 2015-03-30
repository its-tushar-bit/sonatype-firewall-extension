/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.notifications;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.notification.ProductNotification;
import com.sonatype.clm.dto.model.notification.ProductNotificationList;
import com.sonatype.clm.dto.model.notification.ProductNotificationType;
import com.sonatype.insight.brain.dataaccess.notification.UserViewedProductNotificationDAO;
import com.sonatype.insight.brain.model.notification.UserViewedProductNotification;
import com.sonatype.insight.brain.saas.SaasClient;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import com.google.inject.Binder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SaasProductNotificationServiceTest
    extends AbstractComponentTest
{
  @Inject
  private SaasProductNotificationService saasNotificationService;

  @Inject
  private UserViewedProductNotificationDAO notificationViewedDAO;

  @Mock
  private SaasClient mockSaasClient;

  @Override
  public void configure(Binder binder) {
    super.configure(binder);
    binder.bind(SaasClient.class).toInstance(mockSaasClient);
  }

  @Test
  public void testGetNotifications() throws Exception {
    SaasProductNotificationService saasProductNotificationServiceSpy = spy(saasNotificationService);

    ProductNotificationList expectedProductNotificationList = createNotifications();
    List<ProductNotification> expectedNotifications = expectedProductNotificationList.getProductNotifications();
    expectedProductNotificationList.setProductNotifications(expectedNotifications);
    when(mockSaasClient.get(eq(ProductNotificationList.class),
        eq(SaasProductNotificationService.HDS_PRODUCT_NOTIFICATION_PATH),
        anyMapOf(String.class, String.class))).thenReturn(expectedProductNotificationList);

    List<ProductNotification> retrievedNotifications = saasProductNotificationServiceSpy.getNotifications();
    assertNotifications(retrievedNotifications, expectedNotifications);
    verify(saasProductNotificationServiceSpy, times(1)).isCacheExpired();
    verify(saasProductNotificationServiceSpy, times(1)).getNotificationsFromHds();

    reset(saasProductNotificationServiceSpy);

    // Now verify cached values used
    when(saasProductNotificationServiceSpy.isCacheExpired()).thenReturn(false);
    retrievedNotifications = saasProductNotificationServiceSpy.getNotifications();
    assertNotifications(retrievedNotifications, expectedNotifications);
    verify(saasProductNotificationServiceSpy, times(1)).isCacheExpired();
    verify(saasProductNotificationServiceSpy, times(0)).getNotificationsFromHds();
  }

  @Test
  public void testGetNotifications_NotificationsViewedUpdatedWhenCacheUpdated() throws Exception {
    SaasProductNotificationService saasProductNotificationServiceSpy = spy(saasNotificationService);

    ProductNotificationList expectedProductNotificationList = createNotifications();
    List<ProductNotification> expectedNotifications = expectedProductNotificationList.getProductNotifications();
    expectedProductNotificationList.setProductNotifications(expectedNotifications);
    when(mockSaasClient.get(eq(ProductNotificationList.class),
        eq(SaasProductNotificationService.HDS_PRODUCT_NOTIFICATION_PATH),
        anyMapOf(String.class, String.class))).thenReturn(expectedProductNotificationList);

    tempEntity.newUserViewedNotificationMapping(USERNAME, expectedNotifications.get(0).getId());

    List<ProductNotification> retrievedNotifications = saasProductNotificationServiceSpy.getNotifications();
    assertNotifications(retrievedNotifications, expectedNotifications);
    verify(saasProductNotificationServiceSpy, times(1)).isCacheExpired();
    verify(saasProductNotificationServiceSpy, times(1)).getNotificationsFromHds();

    List<UserViewedProductNotification> userViewedProductNotifications = notificationViewedDAO.getByUsername(USERNAME);
    assertThat(userViewedProductNotifications.size(), is(1));

    reset(saasProductNotificationServiceSpy);

    // Now verify cached is updated with new values
    expectedProductNotificationList = createNotifications();
    expectedNotifications = expectedProductNotificationList.getProductNotifications();
    expectedProductNotificationList.setProductNotifications(expectedNotifications);
    when(mockSaasClient.get(eq(ProductNotificationList.class),
        eq(SaasProductNotificationService.HDS_PRODUCT_NOTIFICATION_PATH),
        anyMapOf(String.class, String.class))).thenReturn(expectedProductNotificationList);
    when(saasProductNotificationServiceSpy.isCacheExpired()).thenReturn(true);

    retrievedNotifications = saasProductNotificationServiceSpy.getNotifications();
    assertNotifications(retrievedNotifications, expectedNotifications);
    verify(saasProductNotificationServiceSpy, times(1)).isCacheExpired();
    verify(saasProductNotificationServiceSpy, times(1)).getNotificationsFromHds();
    // This should have been removed as part of the cache update
    userViewedProductNotifications = notificationViewedDAO.getByUsername(USERNAME);
    assertThat(userViewedProductNotifications.size(), is(0));
  }


  private ProductNotificationList createNotifications() {
    List<ProductNotification> expectedNotifications = new ArrayList<>();
    ProductNotification notification = new ProductNotification();
    notification.setId(UUID.randomUUID().toString());
    notification.setSummaryText("Summary");
    notification.setSummaryUrl("Summary Url");
    notification.setDetailHtml("Details");
    notification.setType(ProductNotificationType.DEFAULT);
    notification.setDateCreated(new Date().getTime());
    expectedNotifications.add(notification);
    ProductNotificationList expectedProductNotificationList = new ProductNotificationList();
    expectedProductNotificationList.setProductNotifications(expectedNotifications);
    return expectedProductNotificationList;
  }

  private void assertNotifications(final List<ProductNotification> retrievedNotifications,
      final List<ProductNotification> expectedNotifications)
  {
    assertThat(retrievedNotifications.size(), is(expectedNotifications.size()));
    for (int i = 0; i < retrievedNotifications.size(); i++) {
      assertThat(retrievedNotifications.get(i).getId(), is(expectedNotifications.get(i).getId()));
      assertThat(retrievedNotifications.get(i).getSummaryText(), is(expectedNotifications.get(i).getSummaryText()));
      assertThat(retrievedNotifications.get(i).getSummaryUrl(), is(expectedNotifications.get(i).getSummaryUrl()));
      assertThat(retrievedNotifications.get(i).getDetailHtml(), is(expectedNotifications.get(i).getDetailHtml()));
      assertThat(retrievedNotifications.get(i).getType(), is(expectedNotifications.get(i).getType()));
      assertThat(retrievedNotifications.get(i).getDateCreated(), is(expectedNotifications.get(i).getDateCreated()));
    }
  }
}
