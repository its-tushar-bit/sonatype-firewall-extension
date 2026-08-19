/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.autowaivers;

import java.util.Date;

public class ApiAutoPolicyWaiverStatusDTO
{
  public boolean isAutoWaiverEnabled;

  // This field is a Boolean because if isAutoWaiverEnabled is false, all other fields will be null.
  public Boolean isInherited;

  public String autoPolicyWaiverId;

  public String autoPolicyWaiverOwnerId;

  public String autoPolicyWaiverOwnerName;

  public String autoPolicyWaiverOwnerType;

  public Date createTime;

  public Integer threatLevel;

  public Boolean hasNotReachable;

  public Boolean hasNoPathForward;

  public Boolean scopesOperatorAny;
}
