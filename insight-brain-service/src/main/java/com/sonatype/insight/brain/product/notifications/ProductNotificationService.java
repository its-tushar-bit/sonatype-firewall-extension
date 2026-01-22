/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.notifications;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.clm.dto.model.notification.ProductNotification;
import com.sonatype.insight.brain.dataaccess.notification.UserViewedProductNotificationDAO;
import com.sonatype.insight.brain.model.notification.UserViewedProductNotification;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.dataaccess.TransactionContext;

import com.google.common.annotations.VisibleForTesting;

/**
 * @since 1.14.0
 */
@Named
public class ProductNotificationService
{
  private final HdsProductNotificationService hdsNotificationService;

  private final UserViewedProductNotificationDAO userViewedProductNotificationDAO;

  private final CurrentUser currentUser;

  @Inject
  public ProductNotificationService(
      final HdsProductNotificationService hdsNotificationService,
      final UserViewedProductNotificationDAO userViewedProductNotificationDAO,
      final CurrentUser currentUser)
  {
    this.hdsNotificationService = hdsNotificationService;
    this.userViewedProductNotificationDAO = userViewedProductNotificationDAO;
    this.currentUser = currentUser;
  }

  public ProductNotificationListDTO getNotifications(final int pagesSize, final int page) {
    List<ProductNotification> notificationList = hdsNotificationService.getNotifications();

    Set<String> viewedNotificationSet = getViewedNotificationIdSet();
    return convert(getPage(notificationList, pagesSize, page), viewedNotificationSet);
  }

  public ProductNotificationDTO setNotificationViewed(final ProductNotificationDTO notificationDTO) {
    if (notificationDTO == null) {
      throw new IllegalArgumentException("Notifications cannot be null");
    }

    try (TransactionContext tx = userViewedProductNotificationDAO.createTransactionContext()) {
      tx.begin();

      String username = currentUser.getUsername();
      String realmId = currentUser.getRealmId();
      if (userViewedProductNotificationDAO.getByUsernameAndRealmIdAndNotificationId(tx, username, realmId,
          notificationDTO.id) == null) {
        if (userViewedProductNotificationDAO.getLegacyByUsernameAndNotificationId(tx, username,
            notificationDTO.id) != null) {
          userViewedProductNotificationDAO.deleteLegacyByUsername(tx, username);
        }
        UserViewedProductNotification userViewedProductNotification =
            new UserViewedProductNotification(username, realmId, notificationDTO.id);
        userViewedProductNotificationDAO.insert(tx, userViewedProductNotification);
      }

      tx.commit();

      notificationDTO.viewed = true;
      return notificationDTO;
    }
  }

  private Set<String> getViewedNotificationIdSet() {
    Set<String> notificationIdSet = new HashSet<>();
    for (UserViewedProductNotification viewedMapping : userViewedProductNotificationDAO
        .getByUsernameAndRealmId(currentUser.getUsername(), currentUser.getRealmId())) {
      notificationIdSet.add(viewedMapping.getNotificationId());
    }
    for (UserViewedProductNotification viewedMapping : userViewedProductNotificationDAO
        .getLegacyByUsername(currentUser.getUsername())) {
      notificationIdSet.add(viewedMapping.getNotificationId());
    }
    return notificationIdSet;
  }

  private List<ProductNotification> getPage(final List<ProductNotification> notificationList,
                                            final int pageSize,
                                            final int page)
  {
    if (pageSize >= notificationList.size()) {
      if (page == 1) {
        return notificationList;
      }
      else {
        return Collections.emptyList();
      }
    }

    int start = page * pageSize - pageSize;
    int end = start + pageSize;
    if (end >= notificationList.size()) {
      end = notificationList.size();
    }
    return notificationList.subList(start, end);
  }

  @VisibleForTesting
  protected ProductNotificationListDTO convert(final List<ProductNotification> notifications,
                                               final Set<String> viewedNotificationSet)
  {
    ProductNotificationListDTO notificationListDTO = new ProductNotificationListDTO();
    notificationListDTO.notifications = new ArrayList<>(notifications.size());

    for (ProductNotification notification : notifications) {
      ProductNotificationDTO notificationDTO = new ProductNotificationDTO();
      notificationDTO.id = notification.getId();
      notificationDTO.summaryText = notification.getSummaryText();
      notificationDTO.summaryUrl = notification.getSummaryUrl();
      notificationDTO.detailHtml = notification.getDetailHtml();
      notificationDTO.type = notification.getType();
      notificationDTO.dateCreated = notification.getDateCreated();
      if (viewedNotificationSet.contains(notification.getId())) {
        notificationDTO.viewed = true;
      }
      notificationListDTO.notifications.add(notificationDTO);
    }
    return notificationListDTO;
  }
}
