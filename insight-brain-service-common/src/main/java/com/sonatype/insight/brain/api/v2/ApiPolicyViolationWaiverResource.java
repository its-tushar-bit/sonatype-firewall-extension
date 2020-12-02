/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.api.v2.dto.ApiWaiverOptionsDTO;
import com.sonatype.insight.brain.model.OwnerType;

public interface ApiPolicyViolationWaiverResource
{
  /**
   * This is currently used in "request waiver"
   *
   * @deprecated Use
   * {@link ApiPolicyWaiverResource#addPolicyWaiverByPolicyViolationId(OwnerType, String, String, ApiWaiverOptionsDTO)}
   */
  @Deprecated
  void addPolicyWaiver(String policyViolationId, OwnerType ownerType, String comment);
}
