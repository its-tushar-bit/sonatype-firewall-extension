/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.features;

import javax.inject.Named;
import javax.inject.Singleton;

/**
 * @since 1.23
 */
@Named
@Singleton
public class FeatureUtils
{
  // for testing
  public boolean hasMultipleLdapServersEnabled() {
    return false;
  }
}
