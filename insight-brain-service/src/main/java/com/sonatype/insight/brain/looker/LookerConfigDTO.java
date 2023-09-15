/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.looker;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LookerConfigDTO
{
  public String baseUrl;

  public LookerConfigDTO() {
    //for jackson
  }

  public LookerConfigDTO(@JsonProperty("baseUrl") final String baseUrl) {
    this.baseUrl = baseUrl;
  }
}
