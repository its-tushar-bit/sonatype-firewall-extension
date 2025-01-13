/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.utils;

import java.util.List;

public class SbomDetectionResult
{
  public List<String> validationErrors;

  public SbomSummary summary;

  public String errorMessage;

  public String filename;

  public String mimeType;

  public Boolean isValidationErrorIgnorable;

  public Boolean isValid;

  // Is this file an SBOM, if not it is a binary file
  public boolean isSbom;
}
