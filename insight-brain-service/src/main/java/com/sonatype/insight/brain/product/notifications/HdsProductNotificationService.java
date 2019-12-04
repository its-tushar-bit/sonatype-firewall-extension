/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.notifications;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.clm.dto.model.notification.ProductNotification;
import com.sonatype.clm.dto.model.notification.ProductNotificationList;
import com.sonatype.insight.brain.dataaccess.notification.UserViewedProductNotificationDAO;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.notification.UserViewedProductNotification;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.14.0
 */
@Singleton
@Named
public class HdsProductNotificationService
{
  private static final Logger log = LoggerFactory.getLogger(HdsProductNotificationService.class);

  public static final String HDS_PRODUCT_NOTIFICATION_PATH = "rest/productNotifications";

  private static final long WAIT_TIME = TimeUnit.HOURS.toMillis(3);

  private final Date expirationTime;

  private final List<ProductNotification> notifications = new ArrayList<>();

  private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

  private final HdsClient hdsClient;

  private final UserViewedProductNotificationDAO userViewedProductNotificationDAO;

  public boolean disableCacheForTesting;

  @Inject
  public HdsProductNotificationService(
      final HdsClient hdsClient,
      final UserViewedProductNotificationDAO userViewedProductNotificationDAO)
  {
    this.hdsClient = hdsClient;
    this.expirationTime = new Date();
    this.userViewedProductNotificationDAO = userViewedProductNotificationDAO;
  }

  public List<ProductNotification> getNotifications() {
    updateCacheIfExpired();
    try {
      readWriteLock.readLock().lock();
      return new ArrayList<>(notifications);
    }
    finally {
      readWriteLock.readLock().unlock();
    }
  }

  private void updateCacheIfExpired() {
    try {
      readWriteLock.writeLock().lock();
      if (isCacheExpired()) {
        log.info("Updating notification cache from HDS");
        try {
          ProductNotificationList productNotificationList = hdsClient.get(ProductNotificationList.class,
              HDS_PRODUCT_NOTIFICATION_PATH, Collections.emptyMap());
          Set<String> notificationIds = new HashSet<>();
          notifications.clear();
          if (productNotificationList != null) {
            for (ProductNotification notification : productNotificationList.getProductNotifications()) {
              notifications.add(notification);
              notificationIds.add(notification.getId());
            }
            notifications.sort((o1, o2) -> {
              return Long.compare(o2.getDateCreated(), o1.getDateCreated());
            });
          }
          deleteOldUserViewedProductNotification(notificationIds);
        }
        catch (Exception e) {
          log.error(e.getMessage(), e);
        }
      }
    }
    finally {
      readWriteLock.writeLock().unlock();
    }
  }

  private void deleteOldUserViewedProductNotification(final Set<String> notificationIdsToKeep) {
    for (UserViewedProductNotification notification : userViewedProductNotificationDAO.getAll()) {
      if (!notificationIdsToKeep.contains(notification.getNotificationId())) {
        userViewedProductNotificationDAO.delete(notification);
      }
    }
  }

  @VisibleForTesting
  protected boolean isCacheExpired() {
    Date now = new Date();
    if (now.compareTo(expirationTime) >= 0 || disableCacheForTesting) {
      expirationTime.setTime(expirationTime.getTime() + WAIT_TIME);
      return true;
    }
    return false;
  }
}
