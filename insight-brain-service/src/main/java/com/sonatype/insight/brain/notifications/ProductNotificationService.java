/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.notifications;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.notification.ProductNotification;
import com.sonatype.insight.brain.dataaccess.notification.UserViewedProductNotificationDAO;
import com.sonatype.insight.brain.model.notification.UserViewedProductNotification;
import com.sonatype.insight.brain.notifications.dto.ProductNotificationDTO;
import com.sonatype.insight.brain.notifications.dto.ProductNotificationListDTO;
import com.sonatype.insight.brain.security.CurrentUser;

import com.google.common.annotations.VisibleForTesting;

/**
 * @since 1.14.0
 */
@Named
public class ProductNotificationService
{
  private final SaasProductNotificationService saasNotificationService;

  private final UserViewedProductNotificationDAO notificationViewedDAO;

  private final CurrentUser currentUser;

  @Inject
  public ProductNotificationService(final SaasProductNotificationService saasNotificationService,
      final UserViewedProductNotificationDAO notificationViewedDAO,
      final CurrentUser currentUser)
  {
    this.saasNotificationService = saasNotificationService;
    this.notificationViewedDAO = notificationViewedDAO;
    this.currentUser = currentUser;
  }

  public ProductNotificationListDTO getNotifications(final int pagesSize, final int page) {
    List<ProductNotification> notificationList = saasNotificationService.getNotifications();

    Set<String> viewedNotificationSet = getNotificationViewedIdSet();
    return convert(getPage(notificationList, pagesSize, page), viewedNotificationSet);
  }

  public ProductNotificationDTO setNotificationViewed(final ProductNotificationDTO notificationDTO) {
    if (notificationDTO == null) {
      throw new IllegalArgumentException("Notifications cannot be null");
    }

    String username = currentUser.getUsername();
    UserViewedProductNotification userViewedNotificationMapping = new UserViewedProductNotification();
    userViewedNotificationMapping.setNotificationId(notificationDTO.id);
    userViewedNotificationMapping.setUsername(username);
    notificationViewedDAO.insert(userViewedNotificationMapping);
    notificationDTO.viewed = true;
    return notificationDTO;
  }

  private Set<String> getNotificationViewedIdSet() {
    List<UserViewedProductNotification> viewedMappings = notificationViewedDAO.getByUsername(currentUser.getUsername());
    Set<String> notificationIdSet = new HashSet<>();
    for (UserViewedProductNotification viewedMapping : viewedMappings) {
      notificationIdSet.add(viewedMapping.getNotificationId());
    }
    return notificationIdSet;
  }

  private List<ProductNotification> getPage(final List<ProductNotification> notificationList, final int pageSize,
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
