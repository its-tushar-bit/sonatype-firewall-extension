/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.sast;

import java.util.List;
import java.util.Map;

public class SastScanRequestDTO
{
  public List<SastFindingRequestDTO> findings;

  public SastScmContextDTO scmContext;

  public static class SastFindingRequestDTO
  {
    public Map<String, Object> coordinate;

    public Integer lineNumber;

    public String cwe;

    public String severity;

    public String confidence;

    public String ruleName;

    public String description;

    public List<SastRemediationRequestDTO> remediations;
  }

  public static class SastRemediationRequestDTO
  {
    public String content;
  }
}
