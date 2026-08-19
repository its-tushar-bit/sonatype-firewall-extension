/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

public class BaseUrlConfiguration
{
  private String baseUrl;

  private boolean forceBaseUrl;

  public BaseUrlConfiguration(String baseUrl, boolean forceBaseUrl) {
    this.baseUrl = baseUrl;
    this.forceBaseUrl = forceBaseUrl;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public boolean isForceBaseUrl() {
    return forceBaseUrl;
  }

  public void setForceBaseUrl(boolean forceBaseUrl) {
    this.forceBaseUrl = forceBaseUrl;
  }
}
