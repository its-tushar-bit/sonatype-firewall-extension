/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

// Vendored/copied from hosted-data-services/insight-license-util
package com.sonatype.insight.test.productlicense;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ProductLicenseConfig
{
  @JsonProperty
  private boolean bypass = false;

  @JsonProperty
  private String keyStorePath;

  @JsonProperty
  private String keyStoreAliasGroup;

  public boolean isBypass() {
    return bypass;
  }

  public void setBypass(final boolean bypass) {
    this.bypass = bypass;
  }

  public String getKeyStorePath() {
    return keyStorePath;
  }

  public String getKeyStoreAliasGroup() {
    return keyStoreAliasGroup;
  }

  public void setKeyStorePath(String keyStorePath) {
    this.keyStorePath = keyStorePath;
  }

  public void setKeyStoreAliasGroup(String keyStoreAliasGroup) {
    this.keyStoreAliasGroup = keyStoreAliasGroup;
  }
}
