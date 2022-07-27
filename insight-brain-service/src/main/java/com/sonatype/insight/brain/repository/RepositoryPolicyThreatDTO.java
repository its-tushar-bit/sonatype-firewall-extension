/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.util.List;

/**
 * @since 1.18.0
 */
public class RepositoryPolicyThreatDTO
{
  public List<DeprecatedRepositoryPolicyViolationDTO> activePolicyViolations;

  // Needed for de-serialization
  public RepositoryPolicyThreatDTO() {
  }

  public RepositoryPolicyThreatDTO(final List<DeprecatedRepositoryPolicyViolationDTO> activePolicyViolations) {
    this.activePolicyViolations = activePolicyViolations;
  }
}
