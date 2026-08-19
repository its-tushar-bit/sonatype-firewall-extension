/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

/**
 * @since 1.164
 * @deprecated since 1.192
 *             Kept only for the legacy endpoint "/waiverRequests/{policyViolationId}"
 */
@Deprecated
public class ApiRequestPolicyWaiverDTO
{
  public String reasonId;

  public String comment;

  public String policyViolationLink;

  public String addWaiverLink;
}
