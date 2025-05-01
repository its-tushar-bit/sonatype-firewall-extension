/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.zscaler;

import java.util.List;

public class ZScalerCreateCategory
{
  private String configuredName;

  private String superCategory;

  private List<String> urls;

  private String type;

  private boolean customCategory;

  public ZScalerCreateCategory() {
    // empty
  }

  public ZScalerCreateCategory(
      final String configuredName,
      final String superCategory,
      final List<String> urls,
      final String type,
      final boolean customCategory)
  {
    this.configuredName = configuredName;
    this.superCategory = superCategory;
    this.urls = urls;
    this.type = type;
    this.customCategory = customCategory;
  }

  public String getConfiguredName() {
    return configuredName;
  }

  public void setConfiguredName(final String configuredName) {
    this.configuredName = configuredName;
  }

  public String getSuperCategory() {
    return superCategory;
  }

  public void setSuperCategory(final String superCategory) {
    this.superCategory = superCategory;
  }

  public List<String> getUrls() {
    return urls;
  }

  public void setUrls(final List<String> urls) {
    this.urls = urls;
  }

  public String getType() {
    return type;
  }

  public void setType(final String type) {
    this.type = type;
  }

  public boolean isCustomCategory() {
    return customCategory;
  }

  public void setCustomCategory(final boolean customCategory) {
    this.customCategory = customCategory;
  }
}
