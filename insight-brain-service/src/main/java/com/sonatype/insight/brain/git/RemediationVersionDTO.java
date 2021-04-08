/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;

public class RemediationVersionDTO
{
  private final String version;

  private final ApiVersionChangeOptionType remediationType;

  private final Integer breakingChangesCount;

  public RemediationVersionDTO(
      final String version,
      final ApiVersionChangeOptionType remediationType,
      final Integer breakingChangesCount)
  {
    this.version = version;
    this.remediationType = remediationType;
    this.breakingChangesCount = breakingChangesCount;
  }

  public RemediationVersionDTO(final String version, final ApiVersionChangeOptionType remediationType) {
    this(version, remediationType, null);
  }

  public String getVersion() {
    return version;
  }

  public ApiVersionChangeOptionType getRemediationType() {
    return remediationType;
  }

  public Integer getBreakingChangesCount() {
    return breakingChangesCount;
  }
}
