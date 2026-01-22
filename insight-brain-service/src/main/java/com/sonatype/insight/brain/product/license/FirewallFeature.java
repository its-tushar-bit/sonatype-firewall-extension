/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import jakarta.inject.Named;

import org.sonatype.licensing.feature.AbstractFeature;

/**
 * @since 1.18.0
 */
@Named(FirewallFeature.ID)
public class FirewallFeature
    extends AbstractFeature
{
  public static final String ID = "Firewall";

  @Override
  public String getDescription() {
    return "Nexus Firewall";
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public String getName() {
    return ID;
  }

  @Override
  public String getShortName() {
    return ID;
  }
}
