/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import com.google.gson.annotations.SerializedName;
import com.neuvector.model.ScanModule;

/**
 * Extends the NeuVector {@link ScanModule} with the pypi variant coordinate fields emitted by the Sonatype
 * container scanner, which the upstream model does not carry. They let a container-installed wheel resolve to
 * its single published variant instead of a bare name==version that HDS expands to every published variant.
 *
 * <p>
 * The upstream model does not declare {@code qualifier}/{@code extension} as of com.neuvector:scanner 1.13;
 * re-verify on upgrade. A same-named field in the upstream class would make Gson throw
 * {@link IllegalArgumentException} when it first inspects {@code ScanModuleWithQualifier}.
 */
class ScanModuleWithQualifier
    extends ScanModule
{
  @SerializedName("qualifier")
  private String qualifier;

  @SerializedName("extension")
  private String extension;

  public String getQualifier() {
    return qualifier;
  }

  public String getExtension() {
    return extension;
  }
}
