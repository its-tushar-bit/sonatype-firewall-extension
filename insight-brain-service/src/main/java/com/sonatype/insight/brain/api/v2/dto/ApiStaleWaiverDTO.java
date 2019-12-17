/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * @since 1.81
 */
public class ApiStaleWaiverDTO
{
  public String waiverId;

  public String policyId;

  public String policyName;

  public String comment;

  @JsonInclude(Include.NON_EMPTY)
  public String scopeOwnerType;

  @JsonInclude(Include.NON_EMPTY)
  public String scopeOwnerId;

  @JsonInclude(Include.NON_EMPTY)
  public String scopeOwnerName;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZZ")
  public Date createTime;

  public boolean isObsolete;

  @JsonInclude(Include.NON_EMPTY)
  public List<ApiConstraintFactDTO> constraintFacts = new ArrayList<>();
}
