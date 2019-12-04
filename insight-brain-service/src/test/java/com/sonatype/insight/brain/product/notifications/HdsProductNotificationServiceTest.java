/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.notifications;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.notification.ProductNotification;
import com.sonatype.clm.dto.model.notification.ProductNotificationList;
import com.sonatype.clm.dto.model.notification.ProductNotificationType;
import com.sonatype.insight.brain.dataaccess.notification.UserViewedProductNotificationDAO;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.notification.UserViewedProductNotification;
import com.sonatype.insight.brain.security.InternalRealm;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.test.LogOutput;

import com.google.inject.Binder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

  @Override
  public void configure(Binder binder) {
    binder.bind(HdsClient.class).toInstance(mockHdsClient);
    super.configure(binder);
  }

  @Before
  public void resetMockHdsClient() {
    reset(mockHdsClient);
  }

  @Test
  public void testGetNotifications() throws Exception {
    HdsProductNotificationService hdsProductNotificationServiceSpy = spy(hdsNotificationService);

    ProductNotificationList expectedProductNotificationList = createNotifications();
    List<ProductNotification> expectedNotifications = expectedProductNotificationList.getProductNotifications();
    expectedProductNotificationList.setProductNotifications(expectedNotifications);
    when(mockHdsClient.get(eq(ProductNotificationList.class),
        eq(HdsProductNotificationService.HDS_PRODUCT_NOTIFICATION_PATH), anyMap()))
            .thenReturn(expectedProductNotificationList);

    List<ProductNotification> retrievedNotifications = hdsProductNotificationServiceSpy.getNotifications();
    assertNotifications(retrievedNotifications, expectedNotifications);
    verify(hdsProductNotificationServiceSpy, times(1)).isCacheExpired();
    verify(mockHdsClient, times(1)).get(eq(ProductNotificationList.class),
        eq(HdsProductNotificationService.HDS_PRODUCT_NOTIFICATION_PATH), anyMap());

    reset(hdsProductNotificationServiceSpy);
    reset(mockHdsClient);

    // Now verify cached values used
    when(hdsProductNotificationServiceSpy.isCacheExpired()).thenReturn(false);
    retrievedNotifications = hdsProductNotificationServiceSpy.getNotifications();
    assertNotifications(retrievedNotifications, expectedNotifications);
    verify(hdsProductNotificationServiceSpy, times(1)).isCacheExpired();
    verify(mockHdsClient, times(0)).get(eq(ProductNotificationList.class),
        eq(HdsProductNotificationService.HDS_PRODUCT_NOTIFICATION_PATH), anyMap());
  }

  @Test
  public void testGetNotifications_NotificationsViewedUpdatedWhenCacheUpdated() throws Exception {
    HdsProductNotificationService hdsProductNotificationServiceSpy = spy(hdsNotificationService);

    ProductNotificationList expectedProductNotificationList = createNotifications();
    List<ProductNotification> expectedNotifications = expectedProductNotificationList.getProductNotifications();
    expectedProductNotificationList.setProductNotifications(expectedNotifications);
    when(mockHdsClient.get(eq(ProductNotificationList.class),
        eq(HdsProductNotificationService.HDS_PRODUCT_NOTIFICATION_PATH), anyMap()))
            .thenReturn(expectedProductNotificationList);

    tempEntity.newUserViewedProductNotification(USERNAME, InternalRealm.ID, expectedNotifications.get(0).getId());

    List<ProductNotification> retrievedNotifications = hdsProductNotificationServiceSpy.getNotifications();
    assertNotifications(retrievedNotifications, expectedNotifications);
    verify(hdsProductNotificationServiceSpy, times(1)).isCacheExpired();
    verify(mockHdsClient, times(1)).get(eq(ProductNotificationList.class),
        eq(HdsProductNotificationService.HDS_PRODUCT_NOTIFICATION_PATH), anyMap());

    List<UserViewedProductNotification> userViewedProductNotifications =
        userViewedProductNotificationDAO.getByUsernameAndRealmId(USERNAME, InternalRealm.ID);
    assertThat(userViewedProductNotifications).hasSize(1);

    reset(hdsProductNotificationServiceSpy);
    reset(mockHdsClient);

    // Now verify cached is updated with new values
    expectedProductNotificationList = createNotifications();
    expectedNotifications = expectedProductNotificationList.getProductNotifications();
    expectedProductNotificationList.setProductNotifications(expectedNotifications);
    when(mockHdsClient.get(eq(ProductNotificationList.class),
        eq(HdsProductNotificationService.HDS_PRODUCT_NOTIFICATION_PATH), anyMap()))
            .thenReturn(expectedProductNotificationList);
    when(hdsProductNotificationServiceSpy.isCacheExpired()).thenReturn(true);

    retrievedNotifications = hdsProductNotificationServiceSpy.getNotifications();
    assertNotifications(retrievedNotifications, expectedNotifications);
    verify(hdsProductNotificationServiceSpy, times(1)).isCacheExpired();
    verify(mockHdsClient, times(1)).get(eq(ProductNotificationList.class),
        eq(HdsProductNotificationService.HDS_PRODUCT_NOTIFICATION_PATH), anyMap());
    // This should have been removed as part of the cache update
    userViewedProductNotifications =
        userViewedProductNotificationDAO.getByUsernameAndRealmId(USERNAME, InternalRealm.ID);
    assertThat(userViewedProductNotifications).isEmpty();
  }

  @Test
  public void testGetNotifications_ErrorOnHdsClient() throws Exception {
    HdsProductNotificationService hdsProductNotificationServiceSpy = spy(hdsNotificationService);

    ProductNotificationList expectedProductNotificationList = createNotifications();
    List<ProductNotification> expectedNotifications = expectedProductNotificationList.getProductNotifications();
    expectedProductNotificationList.setProductNotifications(expectedNotifications);
    when(mockHdsClient.get(eq(ProductNotificationList.class),
        eq(HdsProductNotificationService.HDS_PRODUCT_NOTIFICATION_PATH), anyMap()))
            .thenReturn(expectedProductNotificationList);

    List<ProductNotification> retrievedNotifications = hdsProductNotificationServiceSpy.getNotifications();
    assertNotifications(retrievedNotifications, expectedNotifications);

    // Now verify cached not cleared on hds client errors
    RuntimeException expectedException = new RuntimeException("Test Exception");
    when(mockHdsClient.get(eq(ProductNotificationList.class),
        eq(HdsProductNotificationService.HDS_PRODUCT_NOTIFICATION_PATH), anyMap())).thenThrow(expectedException);
    when(hdsProductNotificationServiceSpy.isCacheExpired()).thenReturn(true);

    retrievedNotifications = hdsProductNotificationServiceSpy.getNotifications();
    assertNotifications(retrievedNotifications, expectedNotifications);
    assertThat(logOutput).atErrorLevel().contains(expectedException.getMessage(), expectedException);
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

  private void assertNotifications(final List<ProductNotification> retrievedNotifications,
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
