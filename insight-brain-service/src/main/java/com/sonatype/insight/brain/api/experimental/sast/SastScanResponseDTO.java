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

  public SastScmScanContextResponseDTO sastScmScanContext;

  public SastScanResponseDTO() {
  }

  public SastScanResponseDTO(
      String sastScanId,
      Date createdAt,
      List<SastFindingResponseDTO> findings,
      SastScmScanContextResponseDTO sastScmScanContext)
  {
    this.sastScanId = sastScanId;
    this.createdAt = createdAt;
    this.findings = findings;
    this.sastScmScanContext = sastScmScanContext;
  }

  public static class Builder
  {
    public String sastScanId;

    public Date createdAt;

    public List<SastFindingResponseDTO> findings;

    public SastScmScanContextResponseDTO sastScmScanContext;

    public SastScanResponseDTO.Builder setSastScanId(final String sastScanId) {
      this.sastScanId = sastScanId;
      return this;
    }

    public SastScanResponseDTO.Builder setCreatedAt(final Date createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public SastScanResponseDTO.Builder setFindings(final List<SastFindingResponseDTO> findings) {
      this.findings = findings;
      return this;
    }

    public SastScanResponseDTO.Builder setSastScmScanContext(final SastScmScanContextResponseDTO sastScmScanContext) {
      this.sastScmScanContext = sastScmScanContext;
      return this;
    }

    public SastScanResponseDTO build() {
      return new SastScanResponseDTO(sastScanId, createdAt, findings, sastScmScanContext);
    }
  }

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

  @JsonInclude(NON_NULL)
  public static class SastScmScanContextResponseDTO
  {
    public String branchName;

    public String commitHash;

    public String sastPullRequestURL;

    public SastScmScanContextResponseDTO() {
    }

    public SastScmScanContextResponseDTO(String branchName, String commitHash, String sastPullRequestURL) {
      this.branchName = branchName;
      this.commitHash = commitHash;
      this.sastPullRequestURL = sastPullRequestURL;
    }
  }
}
