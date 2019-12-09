/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * @since 1.81
 */
public class ApiStalePolicyWaiverDTO
{
  public String policyWaiverId;

  public String policyId;

  public String policyName;

  public String comment;

  public String scopeOwnerType;

  public String scopeOwnerId;

  public String scopeOwnerName;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZZ")
  public Date createTime;

  public boolean isObsolete;

  public List<ApiConstraintFactDTO> constraintFacts;
}
