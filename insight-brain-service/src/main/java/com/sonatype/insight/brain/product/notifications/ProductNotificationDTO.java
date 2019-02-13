/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.notifications;

import com.sonatype.clm.dto.model.notification.ProductNotificationType;

/**
 * @since 1.14.0
 */
public class ProductNotificationDTO
{
  public String id;

  public ProductNotificationType type;

  public String summaryText;

  public String summaryUrl;

  public String detailHtml;

  public long dateCreated;

  public Boolean viewed = false;
}
