/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

/**
 * DTO that presents the data structure for the exported result file.
 * 
 * @since 1.9
 */
public class ResultData
{
  public String applicationId;

  public String scanId;

  public String reportHtmlUrl;

  public String reportPdfUrl;
}
