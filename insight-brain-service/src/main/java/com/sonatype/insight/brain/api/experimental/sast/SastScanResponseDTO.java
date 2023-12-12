/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.sast;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
public class SastScanResponseDTO
{
  public String sastScanId;

  public Date createdAt;

  public List<SastFindingResponseDTO> findings;

  @JsonInclude(NON_NULL)
  public static class SastFindingResponseDTO
  {
    public String sastFindingId;

    public Map<String, Object> coordinate;

    public Integer lineNumber;

    public String cwe;

    public String severity;

    public String confidence;

    public String ruleName;

    public String description;

    public List<SastRemediationResponseDTO> remediations;
  }

  @JsonInclude(NON_NULL)
  public static class SastRemediationResponseDTO
  {
    public String sastRemediationId;

    public String content;
  }
}


