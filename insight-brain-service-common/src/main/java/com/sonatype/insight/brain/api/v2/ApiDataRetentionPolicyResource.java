/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.api.v2.dto.ApiDataRetentionPoliciesDTO;

/**
 * Resource for API Date Retention Policy
 */
public interface ApiDataRetentionPolicyResource
{
  ApiDataRetentionPoliciesDTO getDataRetentionPolicies(String organizationId);

  ApiDataRetentionPoliciesDTO getParentDataRetentionPolicies(String organizationId);

  void setDataRetentionPolicies(String organizationId, ApiDataRetentionPoliciesDTO dto);
}
