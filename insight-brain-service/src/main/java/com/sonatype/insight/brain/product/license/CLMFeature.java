/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import jakarta.inject.Named;

import org.sonatype.licensing.feature.AbstractFeature;

@Named(CLMFeature.ID)
public class CLMFeature
    extends AbstractFeature
{
  public static final String ID = "SonatypeCLM";

  @Override
  public String getDescription() {
    return "Nexus IQ Server";
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public String getName() {
    return "Nexus IQ Server";
  }

  @Override
  public String getShortName() {
    return "IQ";
  }
}
