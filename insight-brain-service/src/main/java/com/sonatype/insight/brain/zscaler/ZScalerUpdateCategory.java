/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.zscaler;

import java.util.List;

public class ZScalerUpdateCategory
{
  private String configuredName;

  private List<String> urls;

  public ZScalerUpdateCategory() {
    // empty
  }

  public ZScalerUpdateCategory(String configuredName, List<String> urls) {
    this.configuredName = configuredName;
    this.urls = urls;
  }

  public String getConfiguredName() {
    return configuredName;
  }

  public void setConfiguredName(String configuredName) {
    this.configuredName = configuredName;
  }

  public List<String> getUrls() {
    return urls;
  }

  public void setUrls(List<String> urls) {
    this.urls = urls;
  }
}
