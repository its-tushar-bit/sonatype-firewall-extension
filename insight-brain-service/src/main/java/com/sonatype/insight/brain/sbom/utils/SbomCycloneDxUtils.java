/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.utils;

import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Metadata;

public class SbomCycloneDxUtils
{
  private SbomCycloneDxUtils() {
    //no-op
  }

  public static String getApplicationNameSafely(Bom bomDocument) {
    Metadata metadata = getMetadata(bomDocument);
    if (metadata != null && metadata.getComponent() != null) {
      return metadata.getComponent().getName();
    }
    return null;
  }

  public static String getApplicationVersionSafely(Bom bomDocument) {
    Metadata metadata = getMetadata(bomDocument);
    if (metadata != null && metadata.getComponent() != null) {
      return metadata.getComponent().getVersion();
    }
    return null;
  }

  private static Metadata getMetadata(Bom bomDocument) {
    if (bomDocument != null) {
      return bomDocument.getMetadata();
    }
    return null;
  }
}
