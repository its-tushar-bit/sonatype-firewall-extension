/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import com.sonatype.insight.brain.model.OwnerType;

/**
 * Carries data that should be included in requests to the HDS for the purpose of analytics.
 * 
 * @since 1.19
 */
public class HdsClientAnalytics
{
  private OwnerType ownerType;

  private String ownerId;

  private HdsClientAnalytics() {
    // outside callers should use the factory methods
  }

  public static HdsClientAnalytics forApplication(String appId) {
    HdsClientAnalytics analytics = new HdsClientAnalytics();
    analytics.ownerType = OwnerType.APPLICATION;
    analytics.ownerId = appId;
    return analytics;
  }

  public OwnerType getOwnerType() {
    return ownerType;
  }

  public String getOwnerId() {
    return ownerId;
  }
}
