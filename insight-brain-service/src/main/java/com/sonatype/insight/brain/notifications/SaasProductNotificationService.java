/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.notifications;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.clm.dto.model.notification.ProductNotification;
import com.sonatype.clm.dto.model.notification.ProductNotificationList;
import com.sonatype.insight.brain.dataaccess.notification.UserViewedProductNotificationDAO;
import com.sonatype.insight.brain.model.notification.UserViewedProductNotification;
import com.sonatype.insight.brain.saas.SaasClient;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.14.0
 */
@Singleton
@Named
public class SaasProductNotificationService
{
  private static final Logger log = LoggerFactory.getLogger(SaasProductNotificationService.class);

  public static final String HDS_PRODUCT_NOTIFICATION_PATH = "rest/productNotifications";

  private static final long TWENTY_FOUR_HOURS = TimeUnit.DAYS.toMillis(1);

  private final Date expirationTime;

  // Provide comparator to sort newest first
  private final SortedMap<Date, ProductNotification> notifications = new TreeMap<>(new Comparator<Date>()
  {
    @Override
    public int compare(final Date o1, final Date o2) {
      return o2.compareTo(o1);
    }
  });

  private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

  private final SaasClient saasClient;

  private final UserViewedProductNotificationDAO notificationViewedDAO;

  @Inject
  public SaasProductNotificationService(final SaasClient saasClient,
      final UserViewedProductNotificationDAO notificationViewedDAO) {
    this.saasClient = saasClient;
    this.expirationTime = new Date();
    this.notificationViewedDAO = notificationViewedDAO;
  }

  public List<ProductNotification> getNotifications() {
    updateCacheIfExpired();
    try {
      readWriteLock.readLock().lock();
      return new ArrayList<>(notifications.values());
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
          ProductNotificationList productNotificationList = saasClient.get(ProductNotificationList.class,
              HDS_PRODUCT_NOTIFICATION_PATH, Collections.<String, String>emptyMap());
          Set<String> notificationIds = new HashSet<>();
          notifications.clear();
          if (productNotificationList != null) {
            for (ProductNotification notification : productNotificationList.getProductNotifications()) {
              notifications.put(new Date(notification.getDateCreated()), notification);
              notificationIds.add(notification.getId());
            }
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
    for (UserViewedProductNotification notification : notificationViewedDAO.getAll()) {
      if (!notificationIdsToKeep.contains(notification.getNotificationId())) {
        notificationViewedDAO.delete(notification);
      }
    }
  }

  @VisibleForTesting
  protected boolean isCacheExpired() {
    Date now = new Date();
    if (now.compareTo(expirationTime) >= 0) {
      expirationTime.setTime(expirationTime.getTime() + TWENTY_FOUR_HOURS);
      return true;
    }
    return false;
  }
}
