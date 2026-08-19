/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.util.Date;

public class PolicyEvaluationTimestampsDTO
{
  public Date firstPolicyEvaluationTime;

  public Date latestPolicyEvaluationTime;

  public Date quarantineTime;

  public Date unquarantineTime;

  public Boolean autoUnquarantined;
}
