/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.notifications;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
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

  @Inject
  public SaasProductNotificationService(final SaasClient saasClient) {
    this.saasClient = saasClient;
    this.expirationTime = new Date();
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
        ProductNotificationList productNotificationList = getNotificationsFromHds();
        if (productNotificationList != null) {
          notifications.clear();
          for (ProductNotification notification : productNotificationList.getProductNotifications()) {
            notifications.put(new Date(notification.getDateCreated()), notification);
          }
        }
      }
    }
    finally {
      readWriteLock.writeLock().unlock();
    }
  }

  @VisibleForTesting
  protected ProductNotificationList getNotificationsFromHds() {
    ProductNotificationList productNotificationList = null;
    try {
      productNotificationList = saasClient.get(ProductNotificationList.class,
          HDS_PRODUCT_NOTIFICATION_PATH, Collections.<String, String>emptyMap());
    }
    catch (IOException e) {
      log.error(e.getMessage(), e);
    }

    return productNotificationList;
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
