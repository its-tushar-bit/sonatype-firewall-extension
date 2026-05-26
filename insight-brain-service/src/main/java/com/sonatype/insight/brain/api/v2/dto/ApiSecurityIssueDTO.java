/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

public class ApiSecurityIssueDTO
{
  public String source;

  public String reference;

  public Float severity;

  @JsonInclude(Include.NON_NULL)
  public String status;

  /**
   * @since 1.13.0
   */
  public String url;

  /**
   * @since 1.13.0
   */
  public String threatCategory;

  /**
   * @since 1.43.0
   */
  @JsonInclude(Include.NON_NULL)
  public String cwe;

  @JsonInclude(Include.NON_NULL)
  public String cvssVector;

  @JsonInclude(Include.NON_NULL)
  public String cvssVectorSource;

  @JsonInclude(Include.NON_EMPTY)
  public List<String> vulnIds;

  /**
   * @since 1.168.0
   */
  @JsonInclude(Include.NON_NULL)
  public ApiSecurityIssueAnalysisDTO analysis;

  /**
   * Custom security vulnerability data (remediation / cweId / cvssVector / cvssSeverity) when the
   * caller requested {@code includeCustomSecurityVulnerabilityData=true} on the raw report endpoint
   * AND at least one override is configured at the owner hierarchy. Absent otherwise.
   *
   * @since 1.204.0
   */
  @JsonInclude(Include.NON_NULL)
  public SecurityVulnerabilityCustomDataDTO customData;
}
