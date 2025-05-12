/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.zscaler;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ZScalerCategory
{
  private String configuredName;

  private boolean customCategory;

  private String id;

  private List<String> urls;

  private int customUrlsCount;

  public String getConfiguredName() {
    return configuredName;
  }

  public void setConfiguredName(final String configuredName) {
    this.configuredName = configuredName;
  }

  public boolean isCustomCategory() {
    return customCategory;
  }

  public void setCustomCategory(final boolean customCategory) {
    this.customCategory = customCategory;
  }

  public String getId() {
    return id;
  }

  public void setId(final String id) {
    this.id = id;
  }

  public List<String> getUrls() {
    return urls;
  }

  public void setUrls(final List<String> urls) {
    this.urls = urls;
  }

  public void setCustomUrlsCount(final int customUrlsCount) {
    this.customUrlsCount = customUrlsCount;
  }

  public int getCustomUrlsCount() {
    return customUrlsCount;
  }
}
