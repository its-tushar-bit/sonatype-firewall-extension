/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.List;

import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;

/**
 * @since 1.11.0
 */
public class PolicyViolationDTO
{
  public String applicationId;

  public String applicationName;

  public String artifactId;

  public String groupId;

  public String hash;

  public String id;

  public String policyEvaluationId;

  public String policyId;

  public String policyName;

  public PolicyThreatCategory threatCategory;

  public int threatLevel;

  public String version;

  public long time;

  public List<String> pathnames;
}
