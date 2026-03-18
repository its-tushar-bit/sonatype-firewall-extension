/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.features;

import com.sonatype.insight.license.model.Feature;

/**
 * Denotes a feature that is not controlled by the product license.
 */
public enum NonLicensedFeature
    implements
    Feature
{
  ALLOW_EXTERNAL_HYPERLINKS,

  LABELS,

  POLICY,

  REEVALUATE_POLICY,

  RELEASE_GRAPH,

  REPORTS_LIST;

  @Override
  public String toString() {
    return getId();
  }
}
