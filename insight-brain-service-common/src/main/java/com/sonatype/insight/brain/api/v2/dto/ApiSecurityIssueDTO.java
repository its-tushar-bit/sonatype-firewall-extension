/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

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
}
