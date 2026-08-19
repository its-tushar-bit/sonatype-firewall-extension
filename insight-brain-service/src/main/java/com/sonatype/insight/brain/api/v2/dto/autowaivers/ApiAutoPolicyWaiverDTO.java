/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.autowaivers;

import java.util.Date;

import com.sonatype.insight.json.store.ApiDateFormat;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * @since 1.79
 */

public class ApiAutoPolicyWaiverDTO
{
  @JsonInclude(Include.NON_EMPTY)
  public String autoPolicyWaiverId;

  @JsonInclude(Include.NON_EMPTY)
  public String ownerId;

  @JsonInclude(Include.NON_EMPTY)
  public String ownerType;

  @JsonInclude(Include.NON_EMPTY)
  public String ownerName;

  @JsonInclude(Include.NON_EMPTY)
  public String publicId;

  @JsonInclude(Include.NON_EMPTY)
  public int threatLevel;

  public Boolean reachability;

  public Boolean pathForward;

  public String creatorId;

  public String creatorName;

  @JsonInclude(Include.NON_EMPTY)
  @ApiDateFormat
  public Date createTime;

  // Default to true, matching the default of AutoPolicyWaiver
  public boolean scopesOperatorAny = true;
}
