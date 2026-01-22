/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.notifications;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.notification.ProductNotification;
import com.sonatype.clm.dto.model.notification.ProductNotificationList;
import com.sonatype.insight.brain.dataaccess.notification.UserViewedProductNotificationDAO;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.notification.UserViewedProductNotification;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * @since 1.14.0
 */
@Singleton
@Named
public class HdsProductNotificationService
{
  public static final String HDS_PRODUCT_NOTIFICATION_PATH = "rest/productNotifications";

  private final HdsClient hdsClient;

  private final UserViewedProductNotificationDAO userViewedProductNotificationDAO;

  public boolean disableCacheForTesting;

  @Inject
  public HdsProductNotificationService(
      final HdsClient hdsClient,
      final UserViewedProductNotificationDAO userViewedProductNotificationDAO)
  {
    this.hdsClient = hdsClient;
    this.userViewedProductNotificationDAO = userViewedProductNotificationDAO;
  }

  public List<ProductNotification> getNotifications() {
    ProductNotificationList productNotificationList =
        hdsClient.get(ProductNotificationList.class, HDS_PRODUCT_NOTIFICATION_PATH, Collections.emptyMap());
    if (productNotificationList == null) {
      return Collections.emptyList();
    }
    List<ProductNotification> productNotifications = new ArrayList<>(
        productNotificationList.getProductNotifications() != null
            ? productNotificationList.getProductNotifications()
            : Collections.emptyList());

    // Add mock React2Shell notification for prototype
    String react2ShellCveIds = "CVE-2025-55182";
    String react2ShellUrl = "#/reports/react2shell?cveId=" + react2ShellCveIds;

    ProductNotification react2ShellNotification = new ProductNotification();
    react2ShellNotification.setId("react2shell-notification");
    react2ShellNotification.setSummaryText("NEW: React2Shell Impact Report");
    react2ShellNotification.setSummaryUrl(react2ShellUrl);
    react2ShellNotification.setDetailHtml(
        "<p>A new React2Shell Impact report is now available. This report provides detailed information about " +
        "the React2Shell vulnerability affecting your applications.</p>" +
        "<p><a href=\"" + react2ShellUrl + "\">View the React2Shell Impact Report</a></p>");
    react2ShellNotification.setType(com.sonatype.clm.dto.model.notification.ProductNotificationType.DEFAULT);
    react2ShellNotification.setDateCreated(System.currentTimeMillis());
    productNotifications.add(0, react2ShellNotification);

    productNotifications.sort((o1, o2) -> Long.compare(o2.getDateCreated(), o1.getDateCreated()));
    deleteOldUserViewedProductNotification(productNotifications.stream().map(ProductNotification::getId).collect(
        Collectors.toSet()));
    return productNotifications;
  }

  private void deleteOldUserViewedProductNotification(final Set<String> notificationIdsToKeep) {
    try (TransactionContext tx = userViewedProductNotificationDAO.createTransactionContext()) {
      tx.begin();
      for (UserViewedProductNotification notification : userViewedProductNotificationDAO.getAll(tx)) {
        if (!notificationIdsToKeep.contains(notification.getNotificationId())) {
          userViewedProductNotificationDAO.delete(tx, notification);
        }
      }
      tx.commit();
    }
  }
}
