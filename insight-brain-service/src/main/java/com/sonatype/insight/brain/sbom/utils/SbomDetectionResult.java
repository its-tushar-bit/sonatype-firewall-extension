/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.utils;

import java.util.List;

public class SbomDetectionResult
{
  public boolean isSbom = false;

  public boolean isBinary = false;

  public String errorMessage;

  public List<String> validationErrors;

  public String mimeType;

  public SbomSummary summary;
}
