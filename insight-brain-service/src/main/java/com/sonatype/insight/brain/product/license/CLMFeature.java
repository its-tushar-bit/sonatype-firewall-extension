/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import javax.inject.Named;

import org.sonatype.licensing.feature.AbstractFeature;

@Named(CLMFeature.ID)
public class CLMFeature
    extends AbstractFeature
{
  public static final String ID = "SonatypeCLM";

  @Override
  public String getDescription() {
    return "Sonatype CLM Server";
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public String getName() {
    return "CLM";
  }

  @Override
  public String getShortName() {
    return "CLM";
  }

}
