/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.development.prioritization.dto;

import java.util.Objects;

import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;

public class PrioritizationRemediationVersionDTO
{
  private final String version;

  private final ApiVersionChangeOptionType remediationType;

  public PrioritizationRemediationVersionDTO(
      final String version,
      final ApiVersionChangeOptionType remediationType)
  {
    this.version = version;
    this.remediationType = remediationType;
  }

  public String getVersion() {
    return version;
  }

  public ApiVersionChangeOptionType getRemediationType() {
    return remediationType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PrioritizationRemediationVersionDTO that = (PrioritizationRemediationVersionDTO) o;
    return Objects.equals(version, that.version) && remediationType == that.remediationType;
  }

  @Override
  public int hashCode() {
    return Objects.hash(version, remediationType);
  }

  @Override
  public String toString() {
    return "PrioritizationRemediationVersionDTO{" +
        "version='" + version + '\'' +
        ", remediationType=" + remediationType +
        '}';
  }
}
