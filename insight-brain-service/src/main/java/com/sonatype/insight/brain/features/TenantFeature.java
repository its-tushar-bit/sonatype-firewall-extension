/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.features;

import com.sonatype.insight.license.model.Feature;

public enum TenantFeature
    implements
    Feature
{
  SINGLE_TENANT,
  MULTI_TENANT;

  @Override
  public String toString() {
    return getId();
  }
}
