/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.Date;

import com.sonatype.insight.json.store.ISODateSerializer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class ApiApplicationReportDTOV2
{
  public String stage;

  public String applicationId;

  @JsonSerialize(using = ISODateSerializer.class)
  public Date evaluationDate;

  /**
   * @since 1.79
   */
  public String latestReportHtmlUrl;

  public String reportHtmlUrl;

  /**
   * @since 1.16
   */
  public String embeddableReportHtmlUrl;

  public String reportPdfUrl;

  public String reportDataUrl;
}
