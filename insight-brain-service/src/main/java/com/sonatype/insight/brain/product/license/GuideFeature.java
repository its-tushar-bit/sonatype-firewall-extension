/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import jakarta.inject.Named;

import org.sonatype.licensing.feature.AbstractFeature;

// Enables standalone Guide license validation in validateFeatures().
// Today Guide is an add-on to Lifecycle (license file includes SonatypeCLM),
// but the license-manager already uses a separate guide.* property namespace,
// so standalone Guide support is a planned evolution.
@Named(GuideFeature.ID)
public class GuideFeature
    extends AbstractFeature
{
  public static final String ID = "Guide";

  @Override
  public String getDescription() {
    return "Sonatype Guide";
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public String getName() {
    return "Sonatype Guide";
  }

  @Override
  public String getShortName() {
    return ID;
  }
}
