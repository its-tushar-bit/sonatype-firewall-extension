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
