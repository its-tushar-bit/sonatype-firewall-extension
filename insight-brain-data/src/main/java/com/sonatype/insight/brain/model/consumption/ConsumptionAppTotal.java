/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.consumption;

import jakarta.annotation.Nullable;

/**
 * Top-consuming-apps row: internal appId + joined display fields + total count.
 *
 * @since 1.204
 */
public class ConsumptionAppTotal
{
  private final String appId;

  private final String publicId;

  private final String name;

  private final long componentCount;

  public ConsumptionAppTotal(String appId, @Nullable String publicId, @Nullable String name, long componentCount) {
    this.appId = appId;
    this.publicId = publicId;
    this.name = name;
    this.componentCount = componentCount;
  }

  public String getAppId() {
    return appId;
  }

  @Nullable
  public String getPublicId() {
    return publicId;
  }

  @Nullable
  public String getName() {
    return name;
  }

  public long getComponentCount() {
    return componentCount;
  }
}
