/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.dashboard.StageDetailDTO;
import com.sonatype.insight.brain.organization.ApplicationDTO;

/**
 * @since 1.11
 */
public class ApplicationComponentDetailsDTO
{
  public ApplicationDTO application;

  public List<StageDetailDTO> stageDetails = new ArrayList<>();

  public List<PolicyViolationSummaryDTO> policyViolations = new ArrayList<>();

  static class PolicyViolationSummaryDTO
  {
    public String policyId;

    public String policyName;

    public int threatLevel;

    public long time;

    public List<StageDetailDTO> stageDetails = new ArrayList<>();
  }
}
