/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

/**
 * DTO that presents the data structure for the exported result file.
 * 
 * @since 1.10
 */
class ResultData
{
  public String applicationId;

  public String scanId;

  public String reportHtmlUrl;

  public String reportPdfUrl;

  public String reportDataUrl;
}
