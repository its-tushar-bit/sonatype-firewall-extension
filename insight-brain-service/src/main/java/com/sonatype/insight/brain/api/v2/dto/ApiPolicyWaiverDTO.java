/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.Date;

/**
 * @since 1.75
 */
public class ApiPolicyWaiverDTO
{
  public String policyWaiverId;

  public String comment;

  public Date createTime;
}
