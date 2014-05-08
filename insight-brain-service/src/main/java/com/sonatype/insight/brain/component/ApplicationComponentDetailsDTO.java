/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.organization.ApplicationDTO;

/**
 * @since 1.11
 */
public class ApplicationComponentDetailsDTO
{
  public ApplicationDTO application;

  public List<PolicyViolationSummaryDTO> policyViolations = new ArrayList<>();

  static class PolicyViolationSummaryDTO
  {
    public String policyId;

    public String policyName;

    public int threatLevel;

    public long time;

    public List<ReasonDTO> reasons;

    // TODO Do we need to include the policy action details by stage here? I would let the UI retrieve and display that
    // based on the policy id and stage ids provided here.
    public Set<String> stageTypeIds;

    static class ReasonDTO
    {
      public String constraintName;

      public List<String> reasons = new ArrayList<>();
    }
  }
}
