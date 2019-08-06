/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;

/**
 * DTO that presents the data structure for the exported result file.
 * 
 * @since 1.9.1
 */
public class ResultData
{
  public String applicationId;

  public String scanId;

  public String reportHtmlUrl;

  public String reportPdfUrl;

  public String reportDataUrl;

  public String policyAction;

  public PolicyEvaluationResult policyEvaluationResult;
}
